# 文档

| 文档 | 内容 |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | **现行架构**：运行形态、后端模块与依赖方向、订单生命周期与事件、前端分层、线上业务扩展点、标识与兼容性、下一步 |
| [DEPLOYMENT.md](DEPLOYMENT.md) | 部署说明：运行环境、环境变量、建库与迁移、升级注意事项、Nginx、就绪评估 |
| [ARCHITECTURE_PLAN.md](ARCHITECTURE_PLAN.md) | 2026-07 的模块边界与 RBAC 诊断、分阶段计划，以及历次改动的 Changelog |
| [runbooks/PASSWORD_MIGRATION.md](runbooks/PASSWORD_MIGRATION.md) | 运维手册：把早期库中的明文员工密码迁移为 BCrypt |
| [history/](history/) | 历史文档（DessertShop 时期的安全升级说明、界面改进报告），仅作参考 |

其它说明就近存放：数据库脚本见 [backend/sql/README.md](../backend/sql/README.md)，品牌规范见 [frontend/BRAND.md](../frontend/BRAND.md)，
未参与构建的设计素材见 [frontend/design-assets/README.md](../frontend/design-assets/README.md)。
