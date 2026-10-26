# 呼和浩特实验室信息管理系统（LIMS）

本项目依据《呼和浩特实验室信息管理系统建设》文档推进研发，目标是建设符合 CMA、CNAS、ISO/IEC 17025 管理要求的实验室信息管理平台。

## 当前状态

- 已完成：原始文档解读、需求范围分层、一期路线规划、技术架构草案。
- 已确认：一期采用建议范围；系统部署在内网并要求国产化适配；规模不超过 100 名用户，按不少于 200 并发访问设计；一期无外部签章、短信、邮件、统一登录或财务系统对接。
- 当前阶段：阶段 3——工程骨架与权限基础模块。
- 已实现：前后端工程、数据库迁移、JWT 登录、RBAC 权限加载、登录审计、权限菜单和用户列表最小闭环。
- 下一里程碑：配置 JDK 21、Maven 和数据库后完成联调，并实现组织用户与角色授权的完整 CRUD。

## 文档导航

- [需求规格说明](docs/01-requirements.md)
- [研发路线图](docs/02-roadmap.md)
- [技术架构草案](docs/03-architecture.md)
- [待确认问题](docs/04-open-questions.md)
- [产品与流程设计](docs/05-product-and-workflow-design.md)
- [权限设计](docs/06-permission-design.md)
- [领域数据模型](docs/07-domain-data-model.md)
- [API 目录](docs/08-api-catalog.md)
- [开发环境与启动指南](docs/09-development-guide.md)
- [工程实现状态](docs/10-implementation-status.md)

## 工程结构

```text
LIMS/
├─ backend/                 # Java 21 / Spring Boot 后端
├─ frontend/                # Vue 3 / TypeScript 管理端
├─ deploy/                  # 开发数据库与部署配置
└─ docs/                    # 需求、设计、接口和开发文档
```

## 建议的一期范围

一期优先形成可上线闭环：组织与权限、财务收费、物料库存、文件管理、报告模板与报告流转、审计日志、通知预警。任务/样品全流程、仪器采集和高级质量控制在完成详细调研后进入二期，避免在业务规则未确认前过度开发。
