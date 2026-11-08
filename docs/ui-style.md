# UI 美术风格与 AI 素材提示词库

> 用法：把「风格母版」拼在每条提示词最前面再生成，保证全套素材风格一致。
> 工具适配：中文提示词适配即梦/通义万相/豆包；英文适配 Midjourney/Stable Diffusion（SD 需配负面词）。

## 1. 风格定位

| 端 | 风格 | 原因 |
|---|---|---|
| 用户端小程序（C端） | **3D 黏土美食风**（图标/装饰）+ 暖橙色调 | 餐饮行业经典心智色，圆润有食欲，区分度高 |
| 管理端（B端） | **扁平几何插画 + 大量留白**，克制用色 | 数据密集型界面，装饰只出现在登录页和空状态 |

## 2. 统一色板与字体

| 用途 | 色值 |
|---|---|
| 主色（品牌橙） | `#FF6B00` |
| 辅助黄 | `#FFC53D` |
| 促销红 / 成功绿 / 链接蓝 | `#FF4D4F` / `#52C41A` / `#1677FF` |
| 背景 / 卡片 | `#F5F7FA` / `#FFFFFF` |
| 正文 / 次要文字 | `#1F2937` / `#6B7280` |

字体：小程序默认系统字体；管理端标题用默认字体加粗即可，**不要引入额外字体包**。

## 3. 风格母版（每条提示词的前缀）

**母版 C（C端黏土风，用于图标/banner 装饰）**
```
中文：3D黏土渲染风格，圆润卡通造型，柔和摄影棚光影，暖橙色系配色，白色干净背景，居中构图，C4D质感，电商App视觉风格，高细节
EN  : 3D clay render style, cute rounded cartoon shape, soft studio lighting, warm orange color palette, clean white background, centered composition, C4D quality, e-commerce app visual style, high detail
```

**母版 B（B端扁平插画，用于登录页/空状态）**
```
中文：扁平几何插画，圆润造型，橙色与奶油色配色，极简现代商务风，干净纯色背景，构图留白充足
EN  : flat geometric illustration, rounded shapes, orange and cream color palette, minimal modern business style, clean solid background, generous white space
```

**通用负面提示（SD 系）**
```
text, watermark, logo text, blurry, low quality, distorted, realistic photo, human face, extra limbs
```

## 4. 资产清单与提示词

### A1 品牌 Logo（图形部分；文字用设计字体拼，AI 生文字必翻车）
- 尺寸 512×512，白底 PNG → 后期抠透明
- 母版 C + 「一只碗与一双筷子的极简图形标志，单色橙色，圆形徽标构图，无文字」
- EN: minimal emblem logo of a bowl and chopsticks, single orange color, circular badge composition, no text

### A2 管理端登录页插画（16:9 横幅，1200×800 起）
- 母版 B + 「餐厅经营场景：厨师托着餐盘，旁边漂浮订单卡片和数据图表元素，现代餐厅内景，横向构图」
- EN: restaurant operations scene, chef holding a tray with floating order cards and chart elements, modern restaurant interior, wide composition

### A3 小程序首页 Banner 背景（1500×560，含留白供排版）
- 母版 C + 「俯拍木质餐桌美食场景，边缘布置食物元素，中央大面积留白用于标题文字，横幅构图」
- EN: top-down wooden table food scene, food elements placed at edges, large empty center area for title text, banner composition

### A4 分类图标 ×8（200×200，同一母版批量生成，只换主体）
主体清单：珍珠奶茶 / 火锅 / 烧烤串 / 汉堡薯条 / 甜品蛋糕 / 寿司 / 汤面 / 咖啡
- 母版 C + 「主体名，单件商品居中，无背景杂物」
- **一致性技巧**：同一工具、同一模型、同一次会话连续生成；生成后进像素级统一调色

### A5 空状态插画（400×300，三张一套）
- 母版 B + 「空购物车」「空订单列表（一张纸和放大镜）」「空优惠券（虚线票券）」
- EN 例: empty shopping cart, flat illustration

### A6 商家默认封面占位图（1200×800）
- 母版 C + 「成套餐具与美食俯拍场景，中央留白」
- EN: tableware and food flat-lay scene, center space

### A7 吉祥物（可选，不做也行）
- 母版 C + 「戴厨师帽的小圆子吉祥物，微笑挥手，全身」

### A8 商品演示图（不用 AI！）
- 真实感商品照片用 **Unsplash/Pexels 免费图库** 下载，比 AI 生成的"假食物"观感好得多；AI 只负责插画/图标/装饰类

## 5. 文件夹结构与命名规范

```
admin-web/src/assets/
├── brand/     logo-*.png
├── images/    login-hero.png
└── empty/     empty-cart.png / empty-order.png / empty-coupon.png

miniprogram/images/
├── tabbar/    tab-home.png + tab-home-active.png（81×81，两态）
├── banner/    banner-home.png
├── category/  cat-tea.png / cat-hotpot.png / cat-bbq.png / ...
├── empty/     empty-cart.png
└── logo/      logo.png
```

命名规则：
- **kebab-case + 前缀表用途**：`logo-` / `banner-` / `cat-`（分类）/ `empty-` / `tab-`
- 小程序倍图后缀 `@2x`（分类图标 200×200 显示 100rpx）；tabbar 官方建议 81×81
- AI 原图（4K 大图）**不入库**：先缩放+压缩（TinyPNG / squoosh.app）到目标尺寸，单图 ≤100KB 再进工程
- 生成原图统一存项目外：`E:\作业\素材\原图\<日期>\`，工程里只放成品

## 6. 管理端主色落地（Element Plus 变量覆盖）

`admin-web/src/styles/index.css` 追加：

```css
:root {
  --el-color-primary: #ff6b00;
  --el-color-primary-light-3: #ff9a4d;
  --el-color-primary-light-5: #ffb87a;
  --el-color-primary-light-7: #ffd6ad;
  --el-color-primary-light-8: #ffe3c2;
  --el-color-primary-light-9: #fff1e5;
  --el-color-primary-dark-2: #cc5600;
}
```

管理端除登录页插画与空状态外**不加装饰性素材**——B 端界面的专业感来自留白和对齐，不来自图片。
