# 喜甜 SWEETEN 品牌规范

> **喜甜** 是吃到一口甜时的欢喜；**SWEETEN** 是让平常的日子甜一点。

| 项目 | 规范写法 |
|---|---|
| 中文名 | 喜甜 |
| 英文名 | SWEETEN（全大写） |
| 标语 | SWEETEN YOUR DAY.（含句点；中文：让每一天，甜一点。） |
| 品类 | DESSERT · BAKERY · DRINKS（甜品 · 烘焙 · 饮品） |

中文界面以「喜甜」为主名，「SWEETEN」作为英文名与之并列，不另起其它品牌名。
技术标识（项目目录、Java 包名、Maven 坐标、数据库名 `dessertshop`、接口路径、环境变量、订单号前缀 `DS`）保持不变。

---

## 1. 标志

### 1.1 构成

| 元素 | 说明 |
|---|---|
| 中文字标「喜甜」 | 思源宋体（Noto Serif SC）460 字重转曲后定制：「甜」右侧「甘」的底部改为圆润的杯形，杯中盛一颗略微倾斜的草莓腮红爱心；「喜」下方「口」的底横是一道微微上扬的笑弧 |
| 英文字标 SWEETEN | 古典比例的无衬线大写：粗主笔、细横笔、W 与 N 粗细交替，三个 E 完全一致 |
| 图形标志 | 两片可可棕叶子托起一颗草莓腮红爱心（「心芽」）：甜从心里长出来 |
| 分隔线 | 两条细线中间一株三瓣芽，只出现在主标志里 |
| 标语 / 品类行 | Montserrat Regular / 450，宽字距（0.20em / 0.33em），已转为路径 |

### 1.2 组合与使用场景

| 组合 | 内容 | 用在哪里 | 屏幕最小高度 |
|---|---|---|---|
| 主标志（竖版） | 喜甜 / SWEETEN / 标语 / 分隔线 / 品类 | 登录页面板、商城页脚品牌栏、海报与包装等品牌展示位 | 160px |
| 横版 | 喜甜 + SWEETEN 与居中的标语 | 高度充裕的页头、横幅、文档封面 | 48px |
| 单行 | 喜甜 + SWEETEN（无标语） | 商城导航、工作台侧栏、登录页照片左上角、会员卡、404 | 22px |
| 两行 | 喜甜 / SWEETEN | 打印小票（单色）、窄而高的位置 | 40px |
| 图形 | 两片叶子托起爱心 | 折叠侧栏、移动端工作台页头、会员中心空状态、首屏加载、商城首页眉题前的点缀 | 16px |
| 应用图标 | 浅草莓底圆角方块 + 喜甜 / SWEETEN | 手机主屏、应用列表 | — |
| 网站图标 | 暖奶油白圆角方块 + 简化图形 | 浏览器标签页、书签 | 16px |

按位置选择组合，不要把主标志缩到导航栏里用（标语与品类行会糊成一条线），也不要在 32px 以下的方块里放文字组合，改用图形。

### 1.3 界面里怎么用

界面一律使用 `src/components/brand/BrandLogo.vue`，不要用 `<img>` 引用 PNG：

```vue
<BrandLogo variant="inline" :size="30" />              <!-- 导航 / 侧栏 -->
<BrandLogo variant="primary" :size="168" />            <!-- 登录面板、页脚 -->
<BrandLogo variant="symbol" :size="28" />              <!-- 折叠侧栏、小尺寸 -->
<BrandLogo variant="inline" tone="inverse" :size="28" />   <!-- 照片或可可棕底 -->
<BrandLogo variant="stacked" tone="mono" :size="48" />     <!-- 单色打印 -->
```

- `variant`：`primary` / `horizontal` / `inline` / `stacked` / `symbol`；`size` 是高度（px），宽度按比例。
- `tone`：`default`（可可棕 + 草莓腮红爱心）、`inverse`（奶油白 + 草莓腮红爱心，深色或照片底）、`mono`（全部随 `currentColor`）。
- 路径数据来自 `src/assets/brand/logo.js`，与 `public/brand/` 的导出图同源；任意尺寸都清晰，不依赖字体文件。
- 门店工作台的深色外观「可可夜」下，`default` 会自动换成浅色字标，不需要改调用。
- 首屏加载：`index.html` 在应用挂载前显示图形（0.45 秒内加载完则不会出现），挂载后自动移除。

### 1.4 浅色与深色背景

| 背景 | 用法 |
|---|---|
| 暖奶油白、柔白、浅灰、奶茶色 | 标准色（可可棕字标 + 草莓腮红爱心） |
| 可可棕、深色、照片 | 反白版（奶油白字标，爱心保持草莓腮红）；照片上先加暗色渐变保证对比 |
| 草莓腮红纯色 | 不要直接放标准色或反白版：爱心与底色相同会消失。用应用图标的浅草莓底（`#F2DBD6`），或单色版 |
| 单色印刷（小票、盖章） | 单色版，爱心随文字色 |

### 1.5 安全区与禁止事项

- 安全区：四周至少留出 SWEETEN 大写字母的高度；图形四周至少留出其宽度的 1/4。
- 不要拉伸、旋转、改色、加描边 / 投影 / 渐变，不要调整各元素之间的比例与间距。
- 不要用字体重新排「喜甜」或 SWEETEN，一律使用标志文件或 `BrandLogo`。
- 不要使用参考展板里的 AI 渲染字形（见 1.7），也不要把参考展板截图当标志使用。

### 1.6 资源清单

标志文件在 `public/brand/`（构建后位于站点 `/brand/`），网站图标在 `public/` 根目录。SVG 为矢量母版，PNG 为常用尺寸导出；透明版边缘像素保持本色、只有透明度渐变，没有白边。

| 文件 | 尺寸 | 背景 | 用途 |
|---|---|---|---|
| `sweeten-logo.svg` | 矢量 | 透明 | 主标志母版（浅色底） |
| `sweeten-logo.png` | 1600 × 1525 | 暖奶油白 | 主标志，可直接放进文档 / 幻灯片 |
| `sweeten-logo-transparent.png` | 1600 × 1513 | 透明 | 主标志，浅色底 |
| `sweeten-logo-inverse.svg` | 矢量 | 透明 | 主标志反白母版（深色底） |
| `sweeten-logo-inverse.png` | 1600 × 1525 | 可可棕 | 主标志反白 |
| `sweeten-logo-inverse-transparent.png` | 1600 × 1513 | 透明 | 主标志反白，深色底 |
| `sweeten-logo-horizontal.svg` | 矢量 | 透明 | 横版母版 |
| `sweeten-logo-horizontal.png` | 2400 × 766 | 暖奶油白 | 横版 |
| `sweeten-logo-horizontal-transparent.png` | 2400 × 585 | 透明 | 横版，浅色底 |
| `sweeten-logo-horizontal-inverse.svg` | 矢量 | 透明 | 横版反白母版 |
| `sweeten-logo-horizontal-inverse.png` | 2400 × 766 | 可可棕 | 横版反白 |
| `sweeten-logo-horizontal-inverse-transparent.png` | 2400 × 585 | 透明 | 横版反白，深色底 |
| `sweeten-symbol.svg` / `.png` | 矢量 / 1024 × 1024 | 透明 | 图形，浅色底 |
| `sweeten-symbol-inverse.svg` / `.png` | 矢量 / 1024 × 1024 | 透明 | 图形反白，深色底 |
| `sweeten-icon.svg` | 矢量 | 浅草莓圆角方块 | 应用图标母版 |
| `sweeten-icon-512.png` / `sweeten-icon-192.png` | 512 / 192 | 浅草莓圆角方块，角外透明 | 应用图标；Web App Manifest（`any`） |
| `sweeten-icon-maskable-512.png` | 512 | 浅草莓满版 | Android 自适应图标（`maskable`，内容在中央 80% 安全区内） |

| 网站图标（`public/`） | 尺寸 | 说明 |
|---|---|---|
| `favicon.svg` | 矢量 | 现代浏览器优先使用 |
| `favicon.ico` | 16 / 32 / 48 | 16px 是单独绘制的简化版（图形放大、爱心加大），不是缩小图 |
| `favicon.png` | 32 × 32 | PNG 网站图标 |
| `apple-touch-icon.png` | 180 × 180 | iOS 主屏图标，满版不透明，系统自动切圆角 |
| `site.webmanifest` | — | 名称、主题色与 192 / 512 / maskable 图标；`display: browser`，不把站点声明为可安装应用 |

`index.html` 已引用以上图标与清单；遗留页面 `public/mobile.html` 同样使用图形与网站图标。

### 1.7 来源与校正

参考稿是 4 张 AI 生成的品牌展板和 1 张高清主标志图（`brand-reference/`，不入库）。以第 4 张《甜品品牌视觉识别指南展板》为主：主标志、横版、图形、应用图标、网站图标的构图与比例都按它来；第 1–3 张与高清主标志图作补充（「甜」的爱心细节、反白版、色板、字体规范、SWEETEN 的高清字形）。

AI 渲染里有字形错误，生产文件没有直接裁图或放大，而是重建：

- **「甜」缺了「口」**：所有展板里「舌」下方的「口」都被画成了一个托着爱心的大圆弧。生产版先用思源宋体的标准「甜」（舌 + 甘）保证字形正确，再把杯形与爱心移到「甘」的下半部分——「甘」本义就是口中含着美味，杯中一颗心正好对应。
- **「喜」**：用思源宋体标准字形，保留参考稿底横的笑弧。
- **SWEETEN**：拼写核对无误。按 1254px 高清主标志图实测（大写高 83.75px，主笔 15px，细笔 5.75px，斜笔 0.39 斜率）几何重建 E / T / N / W，三个 E 完全一致；S 由高清图平滑描摹。重建结果与参考字形的重合度（IoU）为 0.976。
- **图形与三瓣芽**：对第 4 张展板的图形和高清图的分隔芽做贝塞尔曲线拟合（重合度 0.98 / 0.96），左右叶严格对称，爱心改为端正对称并与叶子共用中轴。
- **颜色**：展板带光影与压纹效果，渲染色偏浅，生产文件统一使用下方色板的标准色值。

### 1.8 重新生成

全部标志文件、网站图标、组件路径数据和字体子集由 `scripts/brand/build_brand_assets.py` 生成（结果已提交，平时不需要运行）。

```bash
pip install fonttools brotli skia-pathops skia-python pillow numpy
python scripts/brand/build_brand_assets.py               # 全部重新生成
python scripts/brand/build_brand_assets.py --skip-fonts  # 只生成标志与图标
python scripts/brand/build_brand_assets.py --skip-logo   # 只重新生成字体子集
```

字体：思源宋体 / 思源黑体可变字体（Windows 字体目录），Montserrat 可变字体需从 Google Fonts 下载后用 `SWEETEN_MONTSERRAT_VF` 指定路径；三者均为 SIL Open Font License 1.1，允许转曲用于标志。脚本内保存了参考稿的实测数据，调整标志时改脚本里的参数后重新生成，不要手改导出文件。

## 2. 色彩

品牌标准色（与参考展板色板一致）：

| 名称 | 色值 | 用途 |
|---|---|---|
| 暖奶油白 Cream | `#F7F3EC` | 页面底色、网站图标底、反白字标 |
| 柔白 Paper | `#FFFEFC` | 卡片、表面 |
| 草莓腮红 Blush | `#E6A4A1` | 标志里的爱心、品牌点缀、选中底、进度条（**不作文字色**，对比度不足） |
| 奶茶 Milk tea | `#D9C5AD` | 辅助、分隔、悬停边框 |
| 可可棕 Cocoa | `#493B35` | 字标、叶子、主文字、主按钮（奶油底上 9.7:1） |
| 柔灰 Muted gray | `#8E8982` | 占位、禁用、装饰图标（不用于正文） |

派生色：

| 名称 | 色值 | 用途 |
|---|---|---|
| 浅草莓 Blush tint | `#F2DBD6` | 应用图标底色（30% 草莓腮红叠在暖奶油白上） |
| 可可浅 | `#6B5D55` | 次级文字（5.7:1） |
| 辅助灰 | `#736B64` | 说明文字（≥ 4.5:1） |
| 莓果 Berry | `#A9524D` | 需要可读的强调：优惠、低库存、眉题（4.7:1） |
| 樱桃 Cherry | `#C46A64` | 导航当前项圆点、进度节点等小面积点缀 |

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
| 标志 | 思源宋体 460（定制）/ 重建的 SWEETEN 字形 / Montserrat | 只存在于标志文件与 `logo.js` 的路径中，界面不加载这些字体 |

展示字体是子集：新增使用 `.sw-display` 的中文文案时，先写进 `src/config/brand.js`，再运行
`python scripts/brand/build_brand_assets.py --skip-logo` 重新生成（依赖与字体来源见 1.8，字体为 SIL OFL 1.1）。

金额与数量使用 `.sw-num`（等宽数字）。

## 4. 版式与组件

- 圆角：控件 10px、卡片 14px、对话框 / 底部面板 20px、胶囊按钮 999px。
- 阴影：极轻的暖色阴影（`--sw-shadow-*`），不用玻璃模糊、不用霓虹辉光。
- 动效：只做透明度与 8px 以内位移，时长 120–320ms；全部尊重 `prefers-reduced-motion`。
- 图标：`src/components/brand/SwIcon.vue`，24 网格、1.6 描边、圆角端点；不使用 emoji 作界面图标。
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
