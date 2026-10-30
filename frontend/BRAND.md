# 喜甜 SWEETEN 品牌规范

> **喜甜** 是吃到一口甜时的欢喜；**SWEETEN** 是让平常的日子甜一点。
> 标语：**SWEETEN YOUR DAY.**（中文：让每一天，甜一点。）
> 品类：甜品 · 烘焙 · 饮品（Dessert, Bakery & Drinks）

中文界面以「喜甜」为主名，「SWEETEN」作为英文名与之并列，不另起其它品牌名。
技术标识（项目目录 `dessertshop`、Java 包名、Maven 坐标、数据库名、接口路径、环境变量、订单号前缀 `DS`）保持不变。

---

## 1. 标志

| 资源 | 位置 | 说明 |
|---|---|---|
| 标志组件 | `src/components/brand/BrandLogo.vue` | `variant`：`mark` 图形 / `icon` 应用图标 / `horizontal` 单行 / `stacked` 两行 / `wordmark` 仅字标；`tone`：`default` / `inverse`（深色底）/ `mono` |
| 字标轮廓 | `src/assets/brand/wordmark.js` | 「喜甜」思源宋体 700、「SWEETEN」思源黑体 500 宽字距，已转为 SVG 路径，不依赖字体 |
| 网站图标 | `public/favicon.svg`、`favicon.ico`、`apple-touch-icon.png` | 可可底方块 + 奶油微笑 + 腮红樱桃 |

**图形含义**：一道微笑弧线托起一颗樱桃。微笑是「喜」，樱桃是让一件好事更好的那一点甜（cherry on top）—— 正是 SWEETEN。

使用要点：
- 页头用 `horizontal`；登录页、页脚用 `stacked`；侧栏收起或 32px 以下用 `icon`。
- 深色或照片背景用 `tone="inverse"`；单色印刷（小票）用 `tone="mono"`。
- 不要拉伸、改色、加阴影或描边，不要把字标重新用字体排版。

## 2. 色彩

| 名称 | 色值 | 用途 |
|---|---|---|
| 暖奶油 Cream | `#F7F3EC` | 页面底色 |
| 柔白 Paper | `#FFFEFC` | 卡片、表面 |
| 可可 Cocoa | `#493B35` | 主文字、主按钮（奶油底上 9.7:1） |
| 可可浅 | `#6B5D55` | 次级文字（5.7:1） |
| 辅助灰 | `#736B64` | 说明文字（≥ 4.5:1） |
| 柔灰 | `#8E8982` | 占位、禁用、装饰图标（不用于正文） |
| 草莓腮红 Blush | `#E6A4A1` | 品牌点缀、选中底、进度条（**不作文字色**，对比度不足） |
| 莓果 Berry | `#A9524D` | 需要可读的强调：优惠、低库存、眉题（4.7:1） |
| 樱桃 Cherry | `#C46A64` | 标志里的樱桃、导航当前项圆点 |
| 奶茶 Milk tea | `#D9C5AD` | 辅助、分隔、悬停边框 |

状态色（文字 / 浅底，文字均 ≥ 4.5:1）：成功 `#4F6E4B`、提醒 `#8A6420`、危险 `#A44743`、信息 `#4E6A8C`。
订单状态统一用 `StatusBadge`：待接单＝提醒，制作中＝品牌，配送中＝信息，已完成＝成功，已取消＝弱化。

门店工作台另有深色外观「可可夜」（`[data-theme="dark"]`），只在工作台内生效；顾客商城、登录页始终是品牌浅色。

全部颜色以 `src/assets/base.css` 的 `--sw-*` 令牌为准；图表色通过 `--chart-*` 令牌 + `utils/brandTokens.js` 读取。

## 3. 字体

| 角色 | 字体 | 用途 |
|---|---|---|
| 正文 | 系统字体栈（苹方 / 微软雅黑 / Segoe UI …） | 所有动态内容：商品名、人名、金额、表格 |
| 展示 | `SWEETEN Display`（思源宋体 600 子集，18KB） | 只用于 `src/config/brand.js` 中的固定品牌文案，类名 `.sw-display` |
| 英文眉题 | `SWEETEN Latin`（思源黑体 500 拉丁子集，7KB） | `.sw-eyebrow`：SWEETEN YOUR DAY. / BESTSELLERS |

展示字体是子集：新增使用 `.sw-display` 的中文文案时，先写进 `src/config/brand.js`，再运行
`python scripts/brand/build_brand_assets.py` 重新生成（依赖与字体来源见脚本头部注释，字体为 SIL OFL 1.1）。

金额与数量使用 `.sw-num`（等宽数字）。

## 4. 版式与组件

- 圆角：控件 10px、卡片 14px、对话框 / 底部面板 20px、胶囊按钮 999px。
- 阴影：极轻的暖色阴影（`--sw-shadow-*`），不用玻璃模糊、不用霓虹辉光。
- 动效：只做透明度与 8px 以内位移，时长 120–320ms；全部尊重 `prefers-reduced-motion`。
- 图标：`src/components/brand/SwIcon.vue`，24 网格、1.6 描边、圆角端点，与标志笔触一致；不使用 emoji 作界面图标。
- 触控目标 ≥ 44px（`QtyStepper`、底部标签栏、购物车按钮）。
- 键盘：全站 `:focus-visible` 莓果色外环；抽屉 / 面板支持 Esc 关闭。

## 5. 商品摄影

图片来源统一由 `src/config/productImage.js` 决定，组件只用 `<ProductImg>`：

- 卡片 1:1、人气推荐 4:5、详情 1:1；`object-fit: cover`，取景焦点 `VITE_PRODUCT_IMAGE_FOCUS`（默认 `50% 55%`）。
- 阿里云 OSS 图按显示宽度缩放并转 webp，宽度档位 160–1536px，`srcset + sizes` 响应式加载。
- 加载前显示奶油色占位并淡入；远程图失败换本地图，本地图也失败显示品牌线稿占位。
- 当前全站临时统一使用草莓蛋糕图（`VITE_PRODUCT_IMAGE_UNIFORM=true`）。接入真实摄影时：在工作台为每个甜品上传图片，再把该变量设为 `false`，组件无需改动。
- 品牌主视觉（商城首页、登录页）使用打包内置的招牌甜品图 `BRAND_HERO_IMAGE`，不随商品图切换。

建议拍摄：暖色自然光、浅色台面、主体居中略偏下、正方形或 4:5 构图，长边 ≥ 1200px。

## 6. 文案语气

温暖、克制、具体。说清楚正在发生什么，不夸大、不催促（不用「欲购从速」「爆款」之类）。
对顾客如实说明现状：在线支付暂未开通，订单提交后由门店确认制作。
固定文案集中在 `src/config/brand.js`。
