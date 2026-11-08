# UI 美术风格与素材规范（对齐美团风格 · iconfont 图标体系）

> 视觉基准：美团 App 黄色系电商风。图标一律走 [iconfont.cn](https://www.iconfont.cn/)，AI 生图只负责插画/banner/占位图。
> 工具适配：中文提示词适配即梦/通义万相/豆包；英文适配 Midjourney/Stable Diffusion。

## 1. 风格定位

| 端 | 风格 | 要点 |
|---|---|---|
| 用户端小程序（C端） | **美团式黄色电商风**：明黄主色 + 白底大圆角卡片 + 高密度信息流 + 面性图标 | 黄底黑字按钮是灵魂；列表左图右文；价格大号橙红强调 |
| 管理端（B端） | Element Plus **默认蓝色不覆盖**，保持克制 | B端专业感来自留白和对齐；品牌黄只做点缀（logo、强调标签） |

## 2. 色板

| 用途 | 色值 |
|---|---|
| 主色（美团黄） | `#FFC300` |
| 按钮黄底上的文字 | `#1F1F1F`（黑字，黄底白字对比度不够是新手常见错） |
| 促销/价格强调 | `#FF4D2E` |
| 成功 / 链接 | `#52C41A` / `#1677FF` |
| 页面背景 / 卡片 | `#F5F5F7` / `#FFFFFF` |
| 主文字 / 次要文字 / 分割线 | `#1F1F1F` / `#6B7280` / `#EEEEEE` |
| 卡片圆角 / 阴影 | 16–24rpx / `0 2rpx 12rpx rgba(0,0,0,0.04)` 微阴影 |

## 3. AI 风格母版（生成插画/banner 时拼在提示词最前面）

**母版 C（C端插画/banner）**
```
中文：扁平插画风格，明黄色系(#FFC300)与白色配色，圆润卡通造型，柔和光影，干净背景，构图留白充足，电商App视觉风格
EN  : flat illustration style, bright yellow (#FFC300) and white color scheme, rounded cartoon shapes, soft lighting, clean background, generous white space, e-commerce app visual style
```

**母版 B（B端登录页/空状态）**
```
中文：扁平几何插画，圆润造型，黄色与深灰点缀配色，极简现代商务风，干净纯色背景
EN  : flat geometric illustration, rounded shapes, yellow accents on light gray, minimal modern business style, clean solid background
```

**通用负面提示（SD 系）**：`text, watermark, blurry, low quality, distorted, realistic photo, human face`

## 4. 素材清单

### A1 品牌 Logo（图形部分；文字用设计字体拼）
- 512×512 白底 PNG；母版 C + 「一只碗与一双筷子的极简图形标志，单色黄色 #FFC300，圆形徽标构图，无文字」

### A2 管理端登录页插画（1200×800，16:9）
- 母版 B + 「餐厅经营场景：厨师托餐盘与订单卡片、数据图表元素，横向构图」

### A3 小程序首页 Banner 背景（1500×560，中央留白供排版）
- 母版 C + 「俯拍木质餐桌美食场景，食物元素布置在边缘，中央大面积留白」

### A4 分类图标 —— **走 iconfont，不用 AI**
iconfont 检索关键词：`奶茶` `火锅` `烧烤` `汉堡` `甜品` `寿司` `面条` `咖啡`
选图铁律：**同一合集内选满**（iconfont 搜索结果页勾选"合集"，一套图标一个设计师一个风格，混搭必丑）。

### A5 空状态插画（400×300，三张）
- 母版 B + 「空购物车」「空订单（纸与放大镜）」「空优惠券（虚线票券）」

### A6 商家默认封面占位图（1200×800）
- 母版 C + 「成套餐具美食俯拍，中央留白」

### A7 商品演示图 —— **不用 AI**
真实感照片用 Unsplash/Pexels，AI 生成的"假食物"观感差。

## 5. iconfont 使用流程（小程序）

### 5.1 检索关键词清单（M6 页面要用到的全套）

| 模块 | 关键词 |
|---|---|
| tabbar 四态 | `首页` `订单` `我的`（+备用 `分类`） |
| 通用 | `搜索` `定位` `时间` `优惠券` `购物车` `电话` `商家店铺` `五星评分` `右箭头` `加减号` |

### 5.2 选图与下载方式

| 用途 | 方式 | 说明 |
|---|---|---|
| tabbar 图标 | 下载 **PNG**（选中色 #1F1F1F 与激活色 #FFC300 各一张） | 81×81，微信官方建议尺寸；tabbar 只认图片不认字体 |
| 页面内单色图标 | 项目内下载 **font class**，把字体转 base64 内嵌 `app.wxss` | 改色只改 CSS；生成 base64 用 transfont 类工具或 iconfont 的"base64"选项 |
| 想偷懒 | 页面图标也全下 PNG 用 `<image>` | 零配置，代价是改色要重新下载——学习项目完全够用 |

### 5.3 版权注意
iconfont 图标分三种授权（免费商用/需授权/禁止商用），入库前看图标详情页授权标签；个人学习项目无碍，若将来开源/商用需替换或取得授权。

## 6. 文件夹结构与命名规范

```
miniprogram/images/
├── tabbar/    tab-home.png + tab-home-active.png ...
├── banner/    banner-home.png
├── category/  cat-tea.png ...（iconfont PNG 或 SVG 转换）
├── empty/     empty-cart.png ...
└── logo/      logo.png

admin-web/src/assets/
├── brand/     logo-*.png
├── images/    login-hero.png
└── empty/     empty-*.png
```

- 命名：kebab-case + 前缀（`logo-` `banner-` `cat-` `empty-` `tab-`），倍图 `@2x`
- AI 原图（4K）**不入库**：缩放压缩到 ≤100KB 再进工程
- 小程序单包 ≤2MB：图片必须压缩（TinyPNG / squoosh.app）

### 本机素材工作区（约定，不入 Git）

```
E:\下载\                     ← SVG 素材库：iconfont 下载的 SVG 统一放这里（根目录）
E:\下载\素材\svg2png.mjs     ← SVG→PNG 批量转换工具（Node + Edge 无头渲染，透明背景/调色）
E:\下载\素材\preview\        ← 调色预览产物
E:\下载\素材\原图\<日期>\    ← AI 生成 4K 原图暂存
```

转换命令示例：

```bash
node E:\下载\素材\svg2png.mjs E:\下载 miniprogram/images/tabbar --size 81 --color dark=#1F1F1F --color yellow=#FFC300
```

工程里只进压缩后的成品 PNG，SVG 与原图留在本机素材区。

## 7. 落地清单

- [ ] iconfont 建项目（免费账号即可），按 5.1 清单检索加购
- [ ] tabbar 四张 PNG（两态）落 `miniprogram/images/tabbar/`
- [ ] AI 生成 A1/A2/A3/A5/A6（母版拼接），压缩后入库
- [ ] 管理端不改 Element Plus 主色；品牌黄仅出现在 logo 与强调元素
