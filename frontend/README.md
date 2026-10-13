# 🍰 甜品店管理系统 — 前端

Vue 3 + Pinia + Element Plus + ECharts + Vite 的甜品店后台管理 SPA。

- 前端（本仓库）：https://gitee.com/yuka-kaori/dessertshop-frontend
- 后端：https://gitee.com/yuka-kaori/newdessertshop

## 本地运行

**先按后端 README 建库并启动后端**（监听 `localhost:8080`），再启动前端。需要 Node 20.19+ 或 22.12+。

```bash
npm install
npm run dev
```

打开终端里提示的地址（默认 `http://localhost:5173`），用后端导入的默认账号 `admin / 123456` 登录。

开发环境下 `/api` 请求由 Vite 代理到 `http://localhost:8080`，后端地址不同时修改 `.env.development` 中的 `VITE_API_PROXY_TARGET`。

## 其他命令

```bash
npm run build                    # 构建生产版本
npx eslint src --ext .vue,.js    # 代码检查
npm run format                   # 格式化
```

## 项目结构

```
src/
├── api/modules/      # API 接口模块
├── assets/           # 全局样式（设计系统 + 暗色模式）
├── components/       # 通用 UI 组件
├── composables/      # 可复用逻辑（useTable/useForm/useDashboard...）
├── config/           # 环境配置
├── router/           # 路由 + 导航守卫
├── stores/modules/   # Pinia 状态管理
├── utils/            # Axios 封装 + 常量
└── views/            # 页面组件
```
