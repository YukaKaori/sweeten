# 喜甜 SWEETEN — 前端

**SWEETEN YOUR DAY.** 喜甜（甜品 · 烘焙 · 饮品）的前端应用，包含两部分：

| 入口 | 路径 | 用户 | 说明 |
|---|---|---|---|
| 顾客商城 | `/m` | 顾客（免登录） | 品牌首页、甜品菜单（分类 / 搜索 / 排序）、商品详情、购物车、结算下单、我的订单、会员中心；桌面 / 平板 / 手机自适应 |
| 门店工作台 | `/`（`/login` 登录） | 门店员工 | 经营概览、订单、客户与会员、甜品、价格、库存、数据报表、操作日志、员工、部门 |

Vue 3 + Pinia + Element Plus + ECharts + Vite。品牌规范见 [BRAND.md](./BRAND.md)。

- 前端（本仓库）：https://gitee.com/yuka-kaori/dessertshop-frontend
- 后端：https://gitee.com/yuka-kaori/newdessertshop

## 本地运行

**先按后端 README 建库并启动后端**（监听 `localhost:8080`），再启动前端。需要 Node 20.19+ 或 22.12+。

```bash
npm install
npm run dev
```

打开终端提示的地址（默认 `http://localhost:5173`）：`/m` 是顾客商城，`/login` 是员工登录（后端 `init_all.sql` 导入的默认账号 `admin / 123456`）。

开发环境下 `/api` 请求由 Vite 代理到 `http://localhost:8080`，后端地址不同时修改 `.env.development` 中的 `VITE_API_PROXY_TARGET`。

## 环境变量（`.env`）

| 变量 | 说明 |
|---|---|
| `VITE_APP_TITLE` | 默认浏览器标题（各页面为「页面 · 喜甜 SWEETEN」） |
| `VITE_PRODUCT_IMAGE_URL` | 全站统一商品图地址（当前为阿里云 OSS 上的草莓蛋糕图）；留空用仓库内置图 |
| `VITE_PRODUCT_IMAGE_UNIFORM` | `true` 全站统一展示上面的图；`false` 展示各甜品自己上传的图片 |
| `VITE_PRODUCT_IMAGE_FOCUS` | 可选，统一图的取景焦点（CSS `object-position`），默认 `50% 55%` |
| `VITE_DEV_MOCK` | 可选，仅开发环境：设为 `true` 时后端不可达的 **GET** 请求用演示数据兜底。默认关闭；写操作（下单、改状态等）永远不会被伪造成功 |

## 权限

登录响应携带当前员工的权限码（`模块:资源:操作`，与后端 `sql/rbac_init.sql` 一致）。
侧栏、⌘K 面板、路由与页面上的新增 / 编辑 / 删除 / 流转按钮都按权限显示；无权限的路由会被重定向到第一个有权限的页面。
**前端只负责不展示无权限的入口，鉴权以后端 `@RequirePermission` 为准。** 菜单配置在 `src/config/navigation.js`。

## 其他命令

```bash
npm run build                    # 构建生产版本（输出 dist/）
npx eslint src --ext .vue,.js    # 代码检查
npm run format                   # 格式化
python scripts/brand/build_brand_assets.py   # 重新生成字标路径与品牌字体子集（改了 config/brand.js 的展示文案后）
```

## 项目结构

```
src/
├── api/modules/        # 接口模块
├── assets/             # 设计系统 base.css（--sw-* 令牌）、品牌字体子集、字标路径
├── components/
│   ├── brand/          # BrandLogo 标志、SwIcon 线性图标
│   ├── store/          # 商城：ProductCard、CartPanel
│   └── …               # ProductImg、StatusBadge、QtyStepper、EmptyState、PageHeader …
├── composables/        # useCatalog、useDashboard、usePermission、useTheme …
├── config/             # brand.js 品牌文案、navigation.js 菜单、productImage.js 商品图唯一入口
├── router/             # 路由 + 守卫（登录、权限、标题）
├── stores/modules/     # user（含权限判断）、cart、storeUi、myOrders
├── utils/              # http、constants、format、echartsTheme、brandTokens
└── views/
    ├── mobile/         # 顾客商城（/m/*）
    ├── layout/         # 工作台外框
    └── …               # 工作台各页面
```

## 已知限制

- 顾客端没有登录体系：「我的订单」记录在本机浏览器，可凭订单号 + 下单手机号查回；会员中心按本机最近一次下单的手机号显示。上线前需要接入短信验证码或微信登录。
- 没有接入在线支付：订单提交后直接进入「待接单」，页面如实提示「在线支付暂未开通」。
- 优惠券目前不能在线下单时抵扣。
- 全站商品图暂时统一为一张草莓蛋糕图（见上文环境变量）。
