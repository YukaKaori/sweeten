# DessertShop 前端项目改进方案（2026-06 更新）

---

## 一、当前项目状态评估

### 1.1 整体完成度：~75%

原 `OPTIMIZATION_PLAN.md` 中列出的大部分问题已得到解决。以下为当前状态：

| 维度 | 状态 | 完成度 |
|------|------|--------|
| 页面覆盖 | 10+ 个页面全部有内容 | 95% |
| 工程化基础设施 | Pinia / 路由守卫 / 环境变量 / ESLint | 85% |
| UI 一致性 | 玻璃态设计系统 + 统一组件 | 80% |
| 数据真实感 | 首页大量硬编码数据 | 50% |
| 代码质量 | JS 无类型，部分页面代码风格不统一 | 60% |
| 测试覆盖 | 零测试 | 0% |

### 1.2 已完成的改进

- ✅ Pinia 状态管理（user + app + cart）
- ✅ 全局路由守卫 + NProgress
- ✅ 全部路由懒加载
- ✅ 4 个空白页面（order / price / report / log）已全部填充
- ✅ emp 页面 UI 已重写
- ✅ 首页仪表盘 ECharts + CountUp 数字动画
- ✅ 统一组件：PageHeader / Breadcrumb / EmptyState / SkeletonTable / SkeletonCard
- ✅ request.js 错误处理已修复（ElMessage.error）
- ✅ 环境变量配置文件（.env / .env.development / .env.production）
- ✅ 常量管理（utils/constants.js）
- ✅ ESLint flat config
- ✅ 移动端路由 + 购物车 store
- ✅ 404 页面 + 命令面板 + 通知面板 + 用户面板

---

## 二、当前存在的核心问题

### 问题 1：首页仪表盘大量硬编码数据（优先级高）

**dashboard/index.vue** 中有以下硬编码内容：

- `salesDataMap` — 三个时间维度的营收/利润率数据全部写死（第 93-109 行）
- 顶部 micro KPIs — "今日边际毛利额 ¥12,860.00" "冷链配送准时率 99.4%" 等全部静态
- 活动运营卡片（campaign）— "初夏覆盆子狂欢节" "芒里偷闲" 两个卡片完全写死
- 图表底部图例 "¥12,860.00 / 64.8% / ¥69.20" 硬编码
- 欢迎横幅中的"空气湿度 42%"文案硬编码

**影响**：作为系统最重要的入口页面，打开后看到的全部是假数据，严重削弱商业项目可信度。

### 问题 2：useDashboard composable 与页面逻辑重复

- `useDashboard.js` 中定义了 `initChart` / `chartRef` / `chartRange` 等图管理逻辑
- 但 dashboard/index.vue 中又重新定义了 `salesChartRef` / `chartRange` / `renderSalesChart` / `salesDataMap`
- composable 的 `loadData` 和页面 `loadDashboardData` 逻辑高度重叠

**影响**：代码重复，维护两套仪表盘逻辑，且 composable 版本的 chart 初始化逻辑更简洁但未被主页面使用。

### 问题 3：部分页面未使用统一组件

| 页面 | 问题 |
|------|------|
| dept/index.vue | 未使用 PageHeader 组件，使用原始 div 布局 |
| dessert/detail.vue | 需确认设计一致性 |
| login/index.vue | 设计语言独立，与管理系统风格不同 |
| inventory/index.vue | 需检查是否使用统一骨架屏/空状态 |

### 问题 4：零测试覆盖

- 无 Vitest 单元测试
- 无组件测试
- 无 E2E 测试
- 无 API mock 策略

### 问题 5：缺少 TypeScript

- 全部为 `.js` / `.vue` 文件，无类型安全
- API 响应结构、store 状态、路由 meta 均无类型定义
- Element Plus 的 `FormInstance`、`ElTable` 等类型无法使用

### 问题 6：样式系统分散

- 全局变量在 `base.css` 中（需确认是否存在）
- 各页面 scoped style 中重复定义相似的 color tokens
- 部分页面使用硬编码色值而非 CSS 变量

---

## 三、改进方案（按优先级）

### P0 — 立即改进（影响可信度/用户体验）

#### 1. 首页仪表盘去硬编码

**目标**：让仪表盘数据从 API 动态加载，保留降级数据作为 fallback。

具体改动：
- 新增 `/api/dashboard/summary` 接口调用，返回顶部 micro KPIs 数据
- 新增 `/api/dashboard/campaigns` 接口调用，返回营销活动列表
- 图表数据统一走 API 加载，`salesDataMap` 降级为 API 失败时的后备数据
- 图表底部图例数据改为响应式 computed 计算
- `useDashboard` composable 增加 chart 渲染逻辑（`renderChart()`），减少页面端重复代码

**产出物**：
- 修改 `src/composables/useDashboard.js`：合并图表渲染逻辑
- 修改 `src/views/index/index.vue`：移除硬编码 salesDataMap，API 失败时走 fallback
- 新增 API 函数：`querySummaryApi()` / `queryCampaignsApi()`（已存在，确保调用）

#### 2. dept 页面统一化

**目标**：dept 页面使用 PageHeader + EmptyState 等统一组件。

具体改动：
- 引入 PageHeader 替换当前自定义标题区域
- 引入 EmptyState 替换无数据时的空白
- 引入 SkeletonTable 替换 loading 文字

#### 3. 增加 API Mock 策略

**目标**：确保前端在没有后端的情况下也能完整运行。

方案：使用 `vite-plugin-mock` 或在 `public/mock/` 下放 JSON fixture 文件。

**产出物**：
- `mock/handlers.js` — MSW 风格的请求处理器
- 或者直接在 `useDashboard.js` fallback 中补充完整的 mock 数据

---

### P1 — 短期改进（提升工程质量）

#### 4. TypeScript 渐进式迁移

**目标**：核心模块先迁移，不要求一次性全部改完。

迁移顺序：
1. `src/utils/http.js` → `http.ts`（Axios 类型 + 拦截器类型）
2. `src/utils/constants.js` → `constants.ts`（enum / const assertions）
3. `src/config/index.js` → `index.ts`（环境变量类型）
4. `src/stores/modules/user.js` → `user.ts`（Pinia 类型推断）
5. `src/api/modules/*.js` → `*.ts`（请求/响应接口定义）
6. 组件逐步迁移（.vue 文件添加 `<script setup lang="ts">`）

**产出物**：
- `tsconfig.json` + `tsconfig.node.json`
- `src/types/` 目录：`api.d.ts` / `store.d.ts` / `global.d.ts`
- 核心模块 .ts 迁移

#### 5. 单元测试框架搭建

**目标**：基础工具函数和 composable 有测试覆盖。

**产出物**：
- `vitest.config.js`
- `src/__tests__/` 目录
- 优先覆盖：`utils/constants.js` / `stores/modules/user.js` / `composables/useDashboard.js`
- 配置 `@vue/test-utils` + `@pinia/testing`

#### 6. CSS 变量体系统一

**目标**：消除各页面 scoped style 中重复的色值/间距/阴影定义。

方案：
- 在 `base.css` 中补全所有 design tokens
- 页面 scoped style 中只引用变量，不自定义重复 token
- 将 `var(--color-*)` 体系覆盖到全部页面

---

### P2 — 中期改进（提升专业度）

#### 7. 错误边界组件

**目标**：防止单个组件崩溃导致整个页面白屏。

**产出物**：
- `src/components/ErrorBoundary.vue`（使用 `onErrorCaptured`）

#### 8. ECharts 性能优化

**目标**：避免频繁 resize / 重绘。

方案：
- 添加 ResizeObserver 的 debounce（已有时序简单，可加 200ms debounce）
- 图表数据更新使用 `notMerge: false` 增量更新而非擦除重绘
- 页面不可见时暂停图表动画

#### 9. 表格虚拟滚动

**目标**：大数据量列表渲染性能。

方案：Element Plus 的 `el-table-v2` 虚拟化表格组件（适用于 order / log / emp 页面）。

#### 10. 基础可访问性（A11y）

**目标**：满足 WCAG 2.1 AA 级基础要求。

方案：
- 表单控件添加 aria-label
- 表格添加 role/summary
- 模态框 focus trap
- 颜色对比度检查

---

### P3 — 长期改进（可选项）

#### 11. PWA 离线支持

使用 `vite-plugin-pwa` 实现 Service Worker 缓存策略。

#### 12. i18n 国际化

使用 `vue-i18n` 抽取所有中文文案，为多语言扩展做准备。

#### 13. CI/CD 流水线

GitHub Actions / GitLab CI：lint → test → build → deploy。

#### 14. 组件文档（Storybook）

对通用组件（Breadcrumb / PageHeader / EmptyState / SkeletonTable / SkeletonCard / CountUp）编写 Storybook 文档。

---

## 四、改进清单总览

| 序号 | 优先级 | 任务 | 预估工作量 | 影响范围 |
|------|--------|------|-----------|---------|
| 1 | P0 | 仪表盘去硬编码 | 2-3h | index/index.vue + useDashboard.js |
| 2 | P0 | dept 页面统一化 | 1h | dept/index.vue |
| 3 | P0 | API Mock 策略 | 2h | 全局 |
| 4 | P1 | TypeScript 迁移 | 8-12h | 全局（渐进式） |
| 5 | P1 | 单元测试框架 | 4h | 全局（基础设施） |
| 6 | P1 | CSS 变量体系统一 | 2h | base.css + 各页面 |
| 7 | P2 | 错误边界组件 | 1h | App.vue |
| 8 | P2 | ECharts 性能优化 | 1h | 图表相关页面 |
| 9 | P2 | 表格虚拟滚动 | 2h | 列表页面 |
| 10 | P2 | 基础 A11y | 3h | 全局 |
| 11 | P3 | PWA 支持 | 2h | 全局 |
| 12 | P3 | i18n 国际化 | 4h | 全局 |
| 13 | P3 | CI/CD | 3h | 全局 |
| 14 | P3 | Storybook | 4h | 组件库 |

---

## 五、技术债务追踪

以下问题记录在此，不阻塞新功能开发但应在合适时机修复：

1. **API 命名不统一** — 历史遗留的 `querryAllApi`（多了一个 r）需全局搜索替换
2. **dept 页面未使用 PageHeader 组件** — 与其他页面视觉不统一
3. **useDashboard composable 与 dashboard 页面逻辑重复** — 应合并为一套实现
4. **部分页面 loading 策略不统一** — 有的用 `v-loading` 指令，有的用 Skeleton 组件
5. **ECharts 实例未统一管理** — 建议 createECharts() composable 统一创建/销毁/响应式 resize

---

## 六、推荐执行顺序

```
第1周：P0-1 仪表盘去硬编码 → P0-2 dept统一化 → P0-3 API Mock
第2周：P1-4 TS迁移（核心模块） → P1-6 CSS变量统一
第3周：P1-5 测试框架 → P2-7 错误边界 → P2-8 ECharts优化
第4周：P2-9 虚拟滚动 → P2-10 A11y
```

按此节奏，4 周内可将完成度从 75% 提升至 ~90%。
