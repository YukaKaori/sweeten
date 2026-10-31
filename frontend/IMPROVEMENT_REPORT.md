# 🍰 DessertShop 企业级改进报告 · Apple 风重构

> **一句话目标：** 把当前"暖色玫瑰 + 液态玻璃"的作品集式界面，重构为**克制、留白、近黑白中性**的企业级系统——对标 Apple 官网的简洁性与高级感，同时把后端与工程化补齐到可上线的企业标准。
>
> **本报告的写法：** 少而精彩。每一条改进 = `改什么 · 为什么(Apple 原则) · 落到代码哪一处`。不堆功能清单，只给能立刻动手、且能立竿见影的高杠杆项。
>
> 前一版 `DESSERTSHOP_MASTER_PLAN.md`（功能路线图）与本报告互补：那份讲"做多少功能"，本报告讲"做到什么品质"。可保留归档，也可删除。

---

## 进展 · Changelog

> 记录已落地的改进，与下方规划一一对应。

**2026-07-08 · 第一波启动**

- ✅ **A1–A5 设计令牌层重构**（`src/assets/base.css` 全量重写）
  中性灰阶 + 唯一 Apple 蓝 `#0071e3` 强调色；San Francisco 系统字体、删衬线；6 级彩色阴影 + glow → 2–3 级中性阴影；圆角收敛 8/12/16；动效统一 Apple 缓出、去弹跳；Element Plus 按钮/表格/输入/弹窗全部改扁平；`.glass-panel` 去光晕/扫光/聚光/涟漪；5 套彩色主题预设删除（回退浅/深两态）；新增全局 `:focus-visible` 焦点环。**变量名与 class 名全部保留，零破坏，`npm run build` 通过。**
- ✅ **B6 登录页惊艳版**（`src/views/login/index.vue` 重建）
  删双栏玫瑰渐变屏 + 4 浮球 + 鼠标视差 + 玻璃聚光 + 点击涟漪 + 渐变按钮；改为单栏居中窄卡 + 大留白 + 精排版 + 单一蓝色主按钮 + 6 元素逐级 fade-up 进场。
- ⏳ **下一步**：侧边栏/顶栏去暖色去玻璃（`views/layout/index.vue` scoped 样式仍硬编码玫瑰色）；仪表盘首屏 Hero 数字 count-up。

---

## 目录

- [0. 现状定性](#0-现状定性)
- [1. Apple 的四条底层规则](#1-apple-的四条底层规则)
- [A. 设计系统重构（视觉的地基）](#a-设计系统重构视觉的地基)
- [B. 界面与交互精简（做减法）](#b-界面与交互精简做减法)
- [C. 前端工程化到企业级](#c-前端工程化到企业级)
- [D. 后端到企业级](#d-后端到企业级)
- [E. 节奏与优先级](#e-节奏与优先级)
- [F. 验收标准（Apple 级 Checklist）](#f-验收标准apple-级-checklist)

---

## 0. 现状定性

**技术底子好，审美方向反了。**

| 维度 | 现状 | 判断 |
|------|------|------|
| 前端栈 | Vue3 + Element Plus + Pinia + ECharts + Vite | ✅ 主流、健康 |
| 设计语言 | 玫瑰珊瑚色 + 液态玻璃 + 光晕渐变 + Playfair 衬线 + 5 套主题 | ❌ 装饰过度，与 Apple 相反 |
| 视觉噪音 | `FloatingParticles.vue`、`useLiquidGlass.js`（spotlight/磁吸/涟漪）、`liquidStretch` 弹性动画、6 级彩色阴影 + glow | ❌ 全是"炫技"，削弱信息与高级感 |
| 后端栈 | Spring Boot 3.5 + MyBatis + JWT + BCrypt + OSS，多模块 Maven | ✅ 结构清晰 |
| 后端缺口 | 无 RBAC、无 Token 失效、无统一响应/错误码、无可观测性、无 CI/CD、无容器化 | ❌ 离"企业级"有距离 |

**核心矛盾：** Apple 的高级感来自**减法与精度**（留白、对齐、一种字体、极少的颜色、克制的动效）；当前项目的"高级感"来自**加法与特效**（玻璃、渐变、粒子、弹性）。方向必须调头。

---

## 1. Apple 的四条底层规则

写在最前，后面每条改进都由它们推导而来：

1. **克制即高级** — 颜色越少越贵。Apple 官网通篇近黑白，彩色只出现在产品图与唯一的功能强调色（蓝）上。
2. **留白是主角** — 内容之间的空气，比任何边框、卡片、阴影都更能表达层级。
3. **一种字体，一套网格** — SF Pro 一族撑起全站；8pt 网格让一切对齐。没有衬线体乱入，没有随意间距。
4. **动效服务功能** — 过渡只为解释"从哪来、到哪去"，200–300ms、缓出、无弹跳。特效本身不是卖点。

---

## A. 设计系统重构（视觉的地基）

> 全部落在 `src/assets/base.css` 的 `:root` / `[data-theme="dark"]` 令牌层。**改令牌，不改组件**——一处修改，全站生效。这是"少而精彩"的最高杠杆点。

### A1 · 调色板：从暖玫瑰改为 Apple 中性灰

**改什么：** 背景/文字/边框全部换成 Apple 官网的中性阶，品牌色降为**唯一功能强调色**且仅用于可点击元素。

**为什么：** Apple 官网正文区几乎只有 `#1D1D1F`（近黑）、`#6E6E73`（次要）、`#F5F5F7`（背景灰）三色。颜色的稀缺 = 高级。

**落到代码（替换 `base.css` `:root`）：**
```css
:root {
  /* 中性阶 — Apple 灰 */
  --color-text-primary:   #1D1D1F;  /* 近黑，不用纯黑 */
  --color-text-secondary: #6E6E73;
  --color-text-muted:     #86868B;
  --color-bg-primary:     #FFFFFF;  /* 内容底 */
  --color-bg-secondary:   #F5F5F7;  /* 分区/页面底，Apple 标志灰 */
  --color-bg-tertiary:    #FAFAFA;

  /* 边框 — 发丝级，永远低对比 */
  --color-border:       rgba(0,0,0,0.10);
  --color-border-light: rgba(0,0,0,0.06);

  /* 唯一强调色 — 只用于链接/主按钮/选中态 */
  --color-accent:       #0071E3;   /* Apple 蓝；若坚持品牌调，可用克制版玫瑰 #C94D63 */
  --color-accent-hover: #0077ED;
}
```
> 玫瑰色不是删掉，是**降级**：从"到处都是"变成"一年只出场几次"的强调色。

### A2 · 字体：删掉衬线，回归系统字体

**改什么：** 删除 `Playfair Display` 衬线标题字体，全站统一到 San Francisco 系统字体栈。

**为什么：** Apple 全站只用 SF Pro。衬线体在管理系统里是"廉价的仪式感"。系统字体还能省一次网络字体加载。

**落到代码：**
```css
--font-display: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', system-ui, sans-serif;
--font-body:    -apple-system, BlinkMacSystemFont, 'SF Pro Text',    'Segoe UI', system-ui, sans-serif;
```
配套：大标题收紧字距 `letter-spacing: -0.02em`、正文行高 `1.47`（Apple 正文比例），字重只用 400/500/600 三档。

### A3 · 层级：用留白和发丝线，替代阴影和玻璃

**改什么：** 6 级彩色阴影 + `--shadow-glow` + 全部 `--glass-*` 令牌 → 精简为 2 级中性阴影，删除 glow 与玻璃。

**为什么：** Apple 卡片几乎是平的，靠**背景色差 + 发丝边框 + 间距**分层，而不是投影。彩色阴影和光晕是"高级感杀手"。

**落到代码：**
```css
/* 保留两级，中性、极淡 */
--shadow-sm: 0 1px 2px rgba(0,0,0,0.04);
--shadow-md: 0 4px 16px rgba(0,0,0,0.06);
/* 删除：--shadow-lg/xl/glow、--glass-*、--depth-*、--transition-magnetic/ripple */
```
卡片默认**无阴影**，仅 `1px solid var(--color-border)`；hover 时才浮起 `--shadow-md`。

### A4 · 圆角与网格：收敛半径，锁死 8pt 间距

**改什么：** 圆角从 6/10/16/24 四档收敛到"控件 8 / 卡片 12"两档；间距严格 8 的倍数。

**为什么：** 半径越统一越精密。Apple 的克制感来自"所有圆角看起来像一个家族"。

**落到代码：** `--radius-control: 8px; --radius-card: 12px;` 间距沿用 `--space-*`（已是 8pt 体系），但删除组件里所有非 8 倍数的 magic number（如 `padding: 10px 14px` → `8px 16px`）。

### A5 · 动效：把"弹性液态"换成"缓出克制"

**改什么：** 删除 `liquidStretchDown/Up` 关键帧、磁吸、涟漪、粒子；过渡统一为一条 Apple 缓出曲线。

**为什么：** 弹跳/涟漪是消费类玩具的语言；企业工具要"快、稳、不抢戏"。

**落到代码：**
```css
--transition-base: 0.25s cubic-bezier(0.25, 0.1, 0.25, 1);  /* Apple ease */
/* 删除 --transition-spring / --transition-magnetic / --transition-ripple / 所有 @keyframes liquid* */
```
侧边栏选中指示器 `.glass-pill` 保留"滑动"这一个动作，去掉 stretch/scale 弹性，只做 `transform` 位移过渡。

---

## B. 界面与交互精简（做减法）

> 减法本身就是功能。以下每条都是"删/降/合"，不是"加"。

### B1 · 删除三个装饰组件与一个 composable

**改什么：** 移除 `components/FloatingParticles.vue`、`composables/useLiquidGlass.js`（spotlight/magneticTilt/ripple），以及仪表盘/布局里对它们的调用。

**为什么：** 粒子背景、鼠标磁吸、点击涟漪 = 分散注意力、增加渲染成本、拉低专业度。Apple 官网一个粒子都没有。

**落到代码：** `views/index/index.vue`、`views/layout/index.vue` 顶部 import 与模板中的 `<FloatingParticles/>`、`useGlassSpotlight/useMagneticTilt/useGlassRipple` 全部清理。

### B2 · 主题从 5 套收敛为 浅色 + 深色

**改什么：** `useTheme.js` 的 5 个 `presets` 配色方案 → 只留 Light / Dark 两个外观模式，去掉配色切换 UI。

**为什么：** 多主题是"给不了主张"的表现。Apple 只有浅色/深色，因为它对"什么最好看"有明确态度。Header 里那块 `theme-switcher__dropdown` 配色九宫格可整块删除，换成一个 太阳/月亮 图标切换。

### B3 · 表格：去边框、加行高、右对齐数字

**改什么：** 列表页（如 `views/dessert/index.vue` 的 `el-table`）改为**无竖线、发丝横线、行高 56px、金额右对齐等宽数字**。

**为什么：** Apple 风数据展示靠留白和对齐，不靠网格线。密集的斑马纹+竖线是"Excel 感"，不是"产品感"。

**落到代码：** `el-table` 去掉 `border`，全局 Element 覆盖 `--el-table-border-color: transparent`、`--el-table-row-hover-bg-color: var(--color-bg-secondary)`；金额列 `font-variant-numeric: tabular-nums`。

### B4 · 按钮：一主一次，主按钮全站唯一强调色

**改什么：** 页面里同一屏只允许**一个**实心强调色按钮（主操作），其余全部改为文字/次级按钮（灰底或描边）。

**为什么：** Apple 每屏只有一个"最想让你点"的蓝色按钮。满屏彩色按钮会让层级消失。

**落到代码：** 覆盖 Element `--el-color-primary: var(--color-accent)`；`el-button` 默认 plain，主操作才 `type="primary"`。

### B5 · 仪表盘：从"数据大屏"回到"信息卡片"

**改什么：** `views/index/index.vue` 现有玫瑰图/热力图/粒子/问候引擎——保留**核心 KPI 卡 + 一张趋势图 + 一个排行**，其余降权或移入二级页。

**为什么：** Apple 的首页是"一句话 + 一张图 + 一个 CTA"。信息越少，每一条越重要。KPI 卡去掉渐变底和 glow，改为白底 + 发丝边框 + 大号等宽数字 + 一行灰色标签。

### B6 · 登录页：一个输入框的呼吸感

**改什么：** `views/login/index.vue` 去掉玻璃卡+光晕，改为居中窄栏、大留白、纯白/极浅灰背景、一个 logo、两个输入框、一个蓝色按钮。

**为什么：** 登录页是"第一印象"。Apple 的登录/购买流程都是极窄居中、大量留白——高级感往往就在这一屏定调。

### B7 · 空态与骨架屏：安静、单色、有一句话

**改什么：** `EmptyState.vue` / `SkeletonCard.vue` / `SkeletonTable.vue` 统一为单色发丝风格；空态给一句**引导文案 + 一个主操作**，不放插画彩蛋。

**为什么：** Apple 的空状态是克制的引导，不是装饰。骨架屏用 `--color-bg-secondary` 的低对比脉冲即可。

---

## C. 前端工程化到企业级

> 视觉之外，代码质量决定"能不能交给团队维护"。

### C1 · 引入 TypeScript（渐进式）

**改什么：** 项目已有 `tsconfig.json` / `env.d.ts`，但 `package.json` 脚本仍是 `.vue,.js`。把 `api/`、`stores/`、`types/` 先 `.ts` 化，组件保持 `<script setup lang="ts">` 渐进迁移。

**为什么：** 企业级前端的第一道护栏是类型。API 返回结构、Store state 有类型后，重构才安全。

### C2 · 设计令牌单一数据源 + Element Plus 主题桥接

**改什么：** 把 `base.css` 的令牌作为唯一来源，用 CSS 变量桥接 Element Plus（`--el-color-primary`、`--el-border-radius-base`、`--el-font-family` 等），而不是散落覆盖。

**为什么：** "改一个变量，全站（含 Element 组件）一起变"是 Apple 级一致性的工程保障，也杜绝页面各自为政的 magic number。

### C3 · 可访问性（a11y）基线

**改什么：** 所有可点击 `div`（如 `logout-btn`、`nav-item`、`theme-switcher`）改为语义化 `<button>`/`<a>`；补 `:focus-visible` 焦点环、`aria-label`、对比度 ≥ 4.5:1。

**为什么：** Apple 是业界无障碍标杆。企业采购/合规会查 a11y。中性灰配色反而更容易达标（对比度可控）。

### C4 · 前端测试与质量门禁

**改什么：** 引入 Vitest（组件/composable 单测）+ 关键路径 Playwright e2e；`useForm/useTable/useDashboard` 这类逻辑优先覆盖。

**为什么：** 有 21 个后端单测，前端 0 测试是明显短板。逻辑集中在 composables，测试性价比高。

### C5 · 性能与体积

**改什么：** 路由级懒加载核对、Element Plus 按需引入、ECharts 按需 `use()` 模块、图片走 OSS 缩略图参数；`vite build` 后跑一次 `rollup-plugin-visualizer` 看包体。

**为什么：** 高级感也包括"秒开"。删掉粒子/玻璃/多主题后包体会自然瘦身，顺势固化。

---

## D. 后端到企业级

> 前端再美，后端不稳也上不了线。以下按"安全 → 规范 → 可观测 → 交付"排序。

### D1 · RBAC + Token 失效（安全闭环）

**改什么：** 落地角色-权限模型（`sys_role/permission/user_role`），`@RequirePermission` 注解 + AOP 拦截；登出把 JWT 存 Redis 黑名单，`TokenFilter` 查黑名单。

**为什么：** 当前"所有登录用户权限相同、登出后 token 仍有效"是企业级红线。这是上线前**必须**补的。（`DESSERTSHOP_MASTER_PLAN.md` 的 A2–A5 已有拆解，可直接执行。）

### D2 · 统一响应体 + 全局错误码

**改什么：** 固化 `Result<T>{ code, msg, data, traceId }`，配合已有 `GlobalExceptionHandler` 输出规范错误码；参数校验失败、业务异常、系统异常三类分明。

**为什么：** 前端要靠稳定契约做类型（见 C1）。`traceId` 贯穿前后端是排障的地基。

### D3 · 可观测性：日志 + 指标 + 链路

**改什么：** 引入 `spring-boot-starter-actuator` 暴露健康/指标；结构化日志（JSON + traceId，MDC 透传）；接入 Micrometer/Prometheus 采集接口耗时与错误率。

**为什么：** "企业级"和"课程作业"最大的分水岭就是可观测性。出问题时能不能定位，全靠这套。

### D4 · 数据层加固

**改什么：** 慢查询与索引复核（`sql/add_indexes.sql` 已起步，补订单/甜品高频筛选列的复合索引）；关键读接口接 Redis 缓存（Dashboard、甜品列表）并设失效策略；数据库连接池参数（HikariCP）显式化。

**为什么：** Dashboard 和列表是最高频读；缓存 + 索引直接决定体验流畅度。

### D5 · 配置与密钥治理

**改什么：** `application.yml` 里的 JWT secret、OSS AK/SK、DB 密码移出代码，走环境变量 / 配置中心；区分 dev/prod profile；secret 至少 256-bit。

**为什么：** README 里明文密钥示例是安全隐患。企业级要求密钥不进仓库。

### D6 · CI/CD + 容器化

**改什么：** `Dockerfile`（前端 nginx + 后端 JRE）+ `docker-compose.yml`（mysql/redis/backend/nginx）；GitHub Actions：`build → test → lint → 镜像`。

**为什么：** `docker-compose up` 一键起全栈 + 每次提交自动测试，是"可交付"的标志。（对应 MASTER_PLAN 的 D2，可复用。）

---

## E. 节奏与优先级

> 少而精彩：不并行铺开，按"看得见的品质提升"分三波推进，每波结束都能截图对比。

```
第一波 · 视觉调头（1–2 天，纯前端，零风险，冲击最大）
  A1 中性调色 → A2 系统字体 → A3 去阴影去玻璃 → A5 动效收敛
  → B1 删粒子/玻璃 → B2 主题收敛 → B6 登录页
  ✅ 验收：截图放在 Apple 官网旁边不违和

第二波 · 界面精修 + 前端工程化（3–5 天）
  B3 表格 → B4 按钮 → B5 仪表盘 → B7 空态
  C2 令牌桥接 → C3 a11y → C1 TS 渐进
  ✅ 验收：一处改令牌全站生效；键盘可完整操作

第三波 · 后端企业级（1–2 周，可与前端并行）
  D5 密钥治理 → D1 RBAC/登出 → D2 统一响应 → D3 可观测
  → D4 缓存索引 → D6 CI/CD 容器化
  ✅ 验收：docker-compose up 一键起全栈，越权返回 403
```

**为什么这个顺序：** 第一波成本最低、观感提升最大，先把"高级感"立住；第三波价值最高但耗时，放后面稳步推进。

---

## F. 验收标准（Apple 级 Checklist）

一条条打勾，就说明"对标 Apple + 企业级"落地了：

**视觉**
- [ ] 全站颜色 ≤ 中性阶 + 1 个强调色，截图看不出"第二种品牌色"
- [ ] 无衬线体、无玻璃、无光晕、无粒子、无彩色阴影
- [ ] 任一页每屏只有 1 个实心强调色按钮
- [ ] 所有圆角属于 8/12 两档；所有间距是 8 的倍数

**交互**
- [ ] 过渡 ≤ 300ms、缓出、无弹跳
- [ ] 键盘可完成登录→浏览→增删改查全流程，焦点环清晰
- [ ] 对比度全部 ≥ 4.5:1

**工程**
- [ ] `api/stores/types` 已 TS 化，改一个令牌全站（含 Element）生效
- [ ] 前端有单测 + 关键 e2e，CI 通过才能合并

**后端**
- [ ] 越权接口返回 403；登出后旧 token 失效
- [ ] 响应体统一含 `traceId`，可端到端排障
- [ ] 密钥不在仓库；`docker-compose up` 一键起全栈

---

> 📅 2026-07-08 · 原则：**减法、精度、留白、克制**。做少，但每一处都值得放大看。
