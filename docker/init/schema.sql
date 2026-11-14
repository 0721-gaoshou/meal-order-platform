-- ============================================================================
-- meal-order-platform 核心库表 DDL（M1）
-- 唯一建表源：docker/init/schema.sql（docker 首次建卷自动执行；本地 MySQL 手动应用）
-- 对应 docs/项目计划书.md §4.11（幂等总表）/ §4.12（埋点）/ §5（表设计）
--
-- 全局约定：
--   1. 主键一律 BIGINT UNSIGNED，由应用层雪花 ID（MyBatis-Plus ASSIGN_ID）生成，
--      不用 AUTO_INCREMENT——分库分表/多写入点时 ID 全局唯一不冲突；
--   2. 金额一律 BIGINT，单位【分】，杜绝浮点精度问题；
--   3. 状态一律 VARCHAR 字符串枚举，可读性好，状态流转用
--      UPDATE ... WHERE status=旧值 的 CAS 兜底（见计划书 §4.2）；
--   4. created_at/updated_at 交给数据库默认值维护，应用不显式赋值；
--   5. 逻辑删除仅用于可删的业务数据（spu/sku/category），订单/支付/流水只增不改不删（审计要求）；
--   6. 幂等唯一索引是灵魂：uk_order_no / uk_request_id / uk_out_trade_trade /
--      uk_refund_no / uk_user_coupon（对应计划书 §4.11 接口幂等设计总表）。
--
-- 重建语义：本文件是 dev 阶段全量 DDL，DROP+CREATE，重复应用 = 重置数据。
-- 上线后表结构演进另起增量迁移脚本，不在本文件上继续 DROP。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `meal_order`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `meal_order`;

-- ----------------------------------------------------------------------------
-- 1. 商品分类（M1 分类管理；小程分类导航共用）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`         BIGINT UNSIGNED  NOT NULL COMMENT '雪花ID',
  `name`       VARCHAR(64)      NOT NULL COMMENT '分类名',
  `icon`       VARCHAR(512)     NULL     COMMENT '分类图标URL',
  `sort`       INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status`     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `deleted`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删 1已删(逻辑删除)',
  `created_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品分类';

-- ----------------------------------------------------------------------------
-- 2. 商品 SPU（标准产品单元；tags 为 AI 推荐预留的结构化标签）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `spu`;
CREATE TABLE `spu` (
  `id`          BIGINT UNSIGNED  NOT NULL COMMENT '雪花ID',
  `name`        VARCHAR(128)     NOT NULL COMMENT '商品名',
  `category_id` BIGINT UNSIGNED  NOT NULL COMMENT '分类ID -> category.id',
  `merchant_id` BIGINT UNSIGNED  NULL     COMMENT '商家ID -> admin_user.id·预留多商家数据隔离',
  `main_image`  VARCHAR(512)     NULL     COMMENT '主图URL(RustFS)',
  `tags`        VARCHAR(512)     NULL     COMMENT '结构化标签·AI推荐预留(逗号分隔, 如 川菜,微辣,招牌)',
  `description` VARCHAR(2048)    NULL     COMMENT '商品图文详情',
  `status`      VARCHAR(20)      NOT NULL DEFAULT 'OFF_SALE' COMMENT 'ON_SALE在售 / OFF_SALE下架',
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删 1已删(逻辑删除)',
  `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品SPU';

-- ----------------------------------------------------------------------------
-- 3. 商品 SKU（库存单元；下单锁的是 SKU 的 stock）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sku`;
CREATE TABLE `sku` (
  `id`         BIGINT UNSIGNED  NOT NULL COMMENT '雪花ID',
  `spu_id`     BIGINT UNSIGNED  NOT NULL COMMENT 'SPU ID -> spu.id',
  `spec`       VARCHAR(128)     NOT NULL COMMENT '规格名(大杯/少糖/微辣)',
  `price`      BIGINT           NOT NULL COMMENT '售价·分',
  `stock`      INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '库存·UNSIGNED兜底防负数；M2下单DB条件扣减，M7上Redis预扣',
  `version`    INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '乐观锁版本号(MyBatis-Plus @Version)',
  `deleted`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删 1已删(逻辑删除)',
  `created_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_spu` (`spu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品SKU';

-- ----------------------------------------------------------------------------
-- 4. 订单主表（状态机见计划书 §4.2；request_id 是下单幂等锚点）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`              BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `order_no`        VARCHAR(32)     NOT NULL COMMENT '业务单号(展示用)',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID(小程序openid映射)',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING_PAY' COMMENT 'PENDING_PAY待支付/PAID已支付/MAKING制作中/READY待取餐/COMPLETED已完成/CLOSED已取消/REFUNDING退款中/REFUNDED已退款',
  `total_amount`    BIGINT          NOT NULL COMMENT '商品总额·分',
  `discount_amount` BIGINT          NOT NULL DEFAULT 0 COMMENT '优惠总额·分',
  `pay_amount`      BIGINT          NOT NULL COMMENT '实付金额·分 = total - discount',
  `coupon_id`       BIGINT UNSIGNED NULL COMMENT '使用的用户券ID -> user_coupon.id',
  `remark`          VARCHAR(255)    NULL COMMENT '订单备注',
  `request_id`      VARCHAR(64)     NOT NULL COMMENT '下单幂等键(前端UUID)，重复提交靠 uk_request_id 挡',
  `expire_time`     DATETIME        NOT NULL COMMENT '支付截止时间，M5延迟队列按它自动取消',
  `pay_time`        DATETIME        NULL COMMENT '支付时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_user_status_created` (`user_id`, `status`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单主表';

-- ----------------------------------------------------------------------------
-- 5. 订单明细（商品信息下单时快照，防改价/改名污染历史单；coupon_share_amount 是 §4.8 分摊结果）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`                  BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `order_id`            BIGINT UNSIGNED NOT NULL COMMENT '订单ID -> orders.id',
  `spu_id`              BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID(下单时快照)',
  `sku_id`              BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID(下单时快照)',
  `spu_name`            VARCHAR(128)    NOT NULL COMMENT '商品名快照',
  `sku_spec`            VARCHAR(128)    NOT NULL COMMENT '规格快照',
  `sku_image`           VARCHAR(512)    NULL COMMENT '图片快照',
  `quantity`            INT UNSIGNED    NOT NULL COMMENT '数量',
  `unit_price`          BIGINT          NOT NULL COMMENT '成交单价·分(快照)',
  `coupon_share_amount` BIGINT          NOT NULL DEFAULT 0 COMMENT '本行分摊到的优惠·分(§4.8，最后一行兜底尾差)',
  `refund_status`       VARCHAR(20)     NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PARTIAL/REFUNDING/REFUNDED',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_sku` (`sku_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单明细';

-- ----------------------------------------------------------------------------
-- 6. 支付流水（回调幂等双保险：uk_out_trade_no 防重复发起，uk_out_trade_trade 兜底渠道重试）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_log`;
CREATE TABLE `pay_log` (
  `id`           BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `order_id`     BIGINT UNSIGNED NOT NULL COMMENT '订单ID -> orders.id',
  `out_trade_no` VARCHAR(32)     NOT NULL COMMENT '商户支付单号(发起支付时生成)',
  `trade_no`     VARCHAR(64)     NULL COMMENT '渠道支付单号(回调后回填)，未回调前为NULL',
  `pay_channel`  VARCHAR(20)     NOT NULL DEFAULT 'MOCK' COMMENT 'MOCK/WECHAT',
  `amount`       BIGINT          NOT NULL COMMENT '支付金额·分',
  `status`       VARCHAR(20)     NOT NULL DEFAULT 'PAYING' COMMENT 'PAYING/PAID/CLOSED/FAILED',
  `callback_body` TEXT           NULL COMMENT '回调报文原文(幂等比对+排查留证)',
  `pay_time`     DATETIME        NULL COMMENT '支付成功时间',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  UNIQUE KEY `uk_out_trade_trade` (`out_trade_no`, `trade_no`),
  KEY `idx_trade_no` (`trade_no`),
  KEY `idx_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付流水';

-- ----------------------------------------------------------------------------
-- 7. 退款单（refund_no 唯一索引 = 退款回调幂等锚点；状态 REFUNDING→REFUNDED/FAILED CAS）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `refund_order`;
CREATE TABLE `refund_order` (
  `id`            BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `refund_no`     VARCHAR(32)     NOT NULL COMMENT '退款单号',
  `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '订单ID -> orders.id',
  `refund_amount` BIGINT          NOT NULL COMMENT '退款金额·分',
  `status`        VARCHAR(20)     NOT NULL DEFAULT 'REFUNDING' COMMENT 'REFUNDING/REFUNDED/FAILED',
  `reason`        VARCHAR(255)    NULL COMMENT '退款原因',
  `refund_time`   DATETIME        NULL COMMENT '退款成功时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '退款单';

-- ----------------------------------------------------------------------------
-- 8. 优惠券（remain 条件扣减防超发 §4.7；UNSIGNED + WHERE remain>0 双保险）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id`               BIGINT UNSIGNED  NOT NULL COMMENT '雪花ID',
  `name`             VARCHAR(128)     NOT NULL COMMENT '券名',
  `type`             VARCHAR(20)      NOT NULL DEFAULT 'FULL_REDUCTION' COMMENT 'FULL_REDUCTION满减 / DISCOUNT折扣',
  `threshold_amount` BIGINT           NOT NULL DEFAULT 0 COMMENT '使用门槛·分，0=无门槛',
  `discount_amount`  BIGINT           NOT NULL DEFAULT 0 COMMENT '优惠金额·分(满减券用)',
  `discount_rate`    SMALLINT UNSIGNED NULL COMMENT '折扣率，90=9折(折扣券用)',
  `total_count`      INT UNSIGNED     NOT NULL COMMENT '发行总量',
  `remain`           INT UNSIGNED     NOT NULL COMMENT '剩余可领·UNSIGNED兜底，领取用 WHERE remain>0 条件扣减',
  `per_user_limit`   INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT '每人限领(当前uk(user_id,coupon_id)锁1张，字段预留放宽)',
  `start_time`       DATETIME         NOT NULL COMMENT '生效时间',
  `end_time`         DATETIME         NOT NULL COMMENT '失效时间',
  `created_at`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '优惠券';

-- ----------------------------------------------------------------------------
-- 9. 用户券（uk_user_coupon = 领券幂等锚点 §4.11；核销用状态 CAS）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id`            BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `coupon_id`     BIGINT UNSIGNED NOT NULL COMMENT '券ID -> coupon.id',
  `status`        VARCHAR(20)     NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
  `used_order_id` BIGINT UNSIGNED NULL COMMENT '核销订单ID -> orders.id',
  `used_time`     DATETIME        NULL COMMENT '核销时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`),
  KEY `idx_used_order` (`used_order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户优惠券';

-- ----------------------------------------------------------------------------
-- 10. 行为埋点（AI 推荐原料，append-only：只写不更不删，无 updated_at；
--     采集链路 §4.12：AOP -> Spring事件 -> @Async 缓冲 -> 批量 insert，允许丢失不阻塞交易）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_behavior`;
CREATE TABLE `user_behavior` (
  `id`         BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `item_id`    BIGINT UNSIGNED NULL COMMENT '商品ID -> spu.id(搜索等无商品场景为NULL)',
  `action`     VARCHAR(20)     NOT NULL COMMENT 'EXPOSED曝光/CLICK点击/ADD_CART加购/ORDER下单/PAY支付/COUPON_RECEIVE领券',
  `scene`      VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '场景: home/detail/search/recommend',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_action` (`user_id`, `action`),
  KEY `idx_item_action` (`item_id`, `action`),
  KEY `idx_created` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户行为埋点';

-- ----------------------------------------------------------------------------
-- 11. 管理端用户（JWT 登录主体；RBAC 用 role 区分 ADMIN/MERCHANT，M1 不做角色表）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id`         BIGINT UNSIGNED  NOT NULL COMMENT '雪花ID',
  `username`   VARCHAR(64)      NOT NULL COMMENT '登录名',
  `password`   VARCHAR(100)     NOT NULL COMMENT 'BCrypt散列(应用侧Seeding)',
  `nickname`   VARCHAR(64)      NULL COMMENT '显示名',
  `role`       VARCHAR(20)      NOT NULL DEFAULT 'MERCHANT' COMMENT 'ADMIN管理员 / MERCHANT商家',
  `status`     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理端用户';

-- ----------------------------------------------------------------------------
-- 种子数据：小程序首页分类导航（INSERT IGNORE 保证幂等；雪花ID上线后由应用生成，
-- 种子用人工号段 1xxx 不会与应用雪花段冲突）
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `category` (`id`, `name`, `sort`, `status`) VALUES
  (1001, '热销推荐', 1, 1),
  (1002, '主食',     2, 1),
  (1003, '小吃',     3, 1),
  (1004, '饮品',     4, 1),
  (1005, '甜点',     5, 1);
