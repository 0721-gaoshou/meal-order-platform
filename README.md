# 本地生活点餐与优惠券平台

> 单体架构的本地生活点餐平台：用户端微信小程序 + 管理端 Vue3 + 后端 Spring Boot。
> 完整交易闭环：商品 → 购物车 → 下单 → 优惠券 → 支付 → 取消/退款。
> 重点打磨并发扣减、支付幂等、订单状态机、消息队列等企业级问题，并为后续 AI 扩展（推荐/搜索/客服）预留数据与模块接口。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5.16 · Java 21 · MyBatis-Plus 3.5.18 · MySQL 8 · Redis 7 · RabbitMQ 3.13 · Knife4j 4.5 |
| 管理端 | Vue 3.5 · Vite · Element Plus · Pinia · Vue Router · Axios · TypeScript |
| 用户端 | 微信小程序（原生） |
| 部署 | Docker Compose · Nginx（M9） |

## 快速启动（M0 已验证）

```bash
# 1. 依赖容器：MySQL(3306) / Redis(6379) / RabbitMQ(5672,15672)
wsl docker compose -f docker/docker-compose.yml up -d

# 2. 后端：http://localhost:8080/api/doc.html
mvn -f backend/pom.xml package -DskipTests
java -jar backend/target/meal-order-backend.jar

# 3. 管理端：http://localhost:5173
cd admin-web && npm install && npm run dev

# 4. 小程序：微信开发者工具导入 miniprogram/
#    勾选 详情 → 本地设置 → 不校验合法域名，首页按钮可一键健康检查
```

## 文档

- [docs/项目计划书.md](docs/项目计划书.md) — 选型对比、功能范围、12 个核心技术难点（方案对比→定案→验证）
- [docs/任务进度书.md](docs/任务进度书.md) — 里程碑 M0–M9 进度与决策记录
- [docs/git-standard.md](docs/git-standard.md) — 分支模型 / Conventional Commits / 防 Git 污染红线
- [docker/README.md](docker/README.md) — 容器环境端口与账号速查

## 里程碑

- [x] M0 环境与三端脚手架（三端贯通冒烟通过）`v0.1-m0`
- [x] M1 核心库表 + 商品模块 + 管理端 RBAC `v0.2-m1`
- [x] M2 购物车 + 下单 + 库存扣减 + 防重复提交 `v0.3-m2`
- [x] M3 优惠券 + 分摊算法 `v0.4-m3`
- [x] M4 支付回调幂等 + 订单取消/退款 + 管理端订单页 `v0.5-m4`
- [x] M5 延迟取消主通道 + 埋点管道 + 对账任务 `v0.6-m5`
- [ ] M6 微信小程序用户端
- [ ] M7 秒杀 + 压测报告
- [ ] M9 部署 + 文档定稿

---

仅供学习交流使用。
