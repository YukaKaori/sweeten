# 🍰 甜品店管理系统 — 前端

基于 **Vue 3 + Pinia + Element Plus + ECharts + Vite** 的甜品店后台管理 SPA。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.x | Composition API + `<script setup>` |
| Vite | 7.x | 构建工具 |
| Pinia | 3.x | 状态管理 |
| Vue Router | 4.x | 路由（History 模式） |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 6.x | 数据可视化 |
| Axios | 1.x | HTTP 客户端 |

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint

# 格式化
npm run format
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

## 环境变量

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 约定

- 所有 API 通过 `@/utils/http` (Axios) 统一调用
- 状态管理使用 Pinia，位于 `stores/modules/`
- 通用逻辑抽取为 composable
- 全局样式定制通过 CSS 自定义属性
