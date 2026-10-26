# API 目录（V0.1）

## 1. 通用约定

- 基础路径：`/api/v1`。
- JSON 字段采用 `camelCase`，时间使用 ISO 8601 并明确时区。
- 列表统一支持页码、每页数量、排序和受控筛选。
- 写操作使用请求校验、乐观锁版本和统一错误码。
- 创建、提交、审批、收款、库存出入库等关键写操作支持幂等键。
- 文件上传下载、导出和高风险操作全部鉴权并写审计。

## 2. 身份与权限

| 方法 | 路径 | 用途 | 权限码 |
| --- | --- | --- | --- |
| POST | /auth/login | 登录 | 公开 |
| POST | /auth/logout | 退出 | 已登录 |
| GET | /auth/me | 当前用户、菜单和权限 | 已登录 |
| PUT | /auth/password | 修改本人密码 | 已登录 |
| GET/POST | /system/users | 查询/新增用户 | system:user:view/create |
| GET/PUT | /system/users/{id} | 查看/修改用户 | system:user:view/update |
| POST | /system/users/{id}/enable | 启停用户 | system:user:enable |
| PUT | /system/users/{id}/roles | 分配角色 | system:user:grant |
| GET/POST/PUT | /system/roles | 角色管理 | system:role:* |
| PUT | /system/roles/{id}/permissions | 角色授权 | system:role:grant |
| PUT | /system/roles/{id}/data-scopes | 数据范围 | system:role:grant |
| GET/POST/PUT | /system/departments | 部门管理 | system:department:* |
| GET | /system/audit-logs | 审计查询 | audit:operation:view |

## 3. 财务

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST | /finance/price-lists | 价目列表/新建版本 |
| GET/PUT | /finance/price-lists/{id} | 查看/编辑价目草稿 |
| DELETE | /finance/price-lists/{id} | 删除未被收费单引用的价目；已引用时返回业务校验错误 |
| POST | /finance/price-lists/{id}/publish | 发布价目版本 |
| POST | /finance/charge-calculations | 根据项目和数量试算 |
| GET/POST | /finance/charge-orders | 查询/新建收费单 |
| GET | /finance/charge-orders/{id}/items | 查询收费单检测项目明细 |
| PUT/DELETE | /finance/charge-orders/{id} | 修改/删除待确认收费单 |
| POST | /finance/charge-orders/{id}/confirm | 确认收费单 |
| POST | /finance/charge-orders/{id}/return | 将未收款的待收款收费单退回待确认 |
| POST | /finance/charge-orders/{id}/void | 作废收费单 |
| GET/POST | /finance/charge-orders/{id}/payments | 查询/登记收款 |
| GET | /finance/receivables | 欠款查询 |
| POST | /finance/exports | 异步生成财务导出 |

## 4. 物料

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST/PUT | /inventory/materials | 物料档案管理 |
| GET/POST/PUT | /inventory/suppliers | 供应商管理 |
| GET/POST/PUT | /inventory/warehouses | 仓库和库位管理 |
| GET/POST | /inventory/purchase-requests | 采购申请查询/创建 |
| POST | /inventory/purchase-requests/{id}/submit | 提交采购申请 |
| DELETE | /inventory/purchase-requests/{id} | 删除草稿采购申请 |
| POST | /inventory/purchase-requests/{id}/review | 审批采购申请 |
| POST | /inventory/purchase-requests/{id}/receipt-submit | 提交采购验收入库审核 |
| POST | /inventory/purchase-requests/{id}/receipt-review | 通过或退回采购验收入库 |
| GET/POST | /inventory/receipts | 入库单查询/创建 |
| POST | /inventory/receipts/{id}/accept | 验收并入库 |
| GET/POST | /inventory/issues | 领用单查询/创建 |
| POST | /inventory/issues/{id}/submit | 提交领用申请 |
| POST | /inventory/issues/{id}/review | 审批领用申请 |
| POST | /inventory/issues/{id}/post | 确认出库 |
| GET | /inventory/stocks | 库存查询 |
| GET | /inventory/stock-transactions | 库存流水 |
| GET/POST | /inventory/stocktakes | 盘点单查询/创建 |
| POST | /inventory/stocktakes/{id}/approve | 审批并过账盘点差异 |
| GET | /inventory/alerts | 库存及效期预警 |

## 5. 文件

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST | /documents | 文件查询/新建 |
| GET/PUT | /documents/{id} | 查看/修改文件草稿 |
| POST | /documents/{id}/versions | 发起新版本 |
| POST | /document-versions/{id}/submit | 提交审核 |
| POST | /document-versions/{id}/review | 审核 |
| POST | /document-versions/{id}/publish | 发布 |
| POST | /documents/{id}/void | 作废 |
| GET | /documents/{id}/versions | 版本历史 |
| GET | /document-versions/{id}/download | 授权下载 |

## 6. 报告

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST | /report-templates | 模板查询/创建 |
| POST | /report-templates/{id}/versions | 导入模板版本 |
| POST | /report-template-versions/{id}/test-render | 测试渲染 |
| GET/POST | /reports | 报告查询/创建 |
| GET/PUT | /reports/{id} | 查看/编辑报告草稿 |
| POST | /reports/{id}/preview | 生成预览 |
| POST | /reports/{id}/submit | 提交审核 |
| POST | /reports/{id}/review | 审核 |
| POST | /reports/{id}/approve | 批准并固化版本 |
| POST | /reports/{id}/void | 发起/执行作废 |
| POST | /reports/{id}/corrections | 发起更正版本 |
| POST | /report-versions/{id}/seal-records | 登记用印 |
| POST | /report-versions/{id}/distributions | 登记发放 |
| POST | /report-versions/{id}/archive | 归档 |
| GET | /report-versions/{id}/download | 下载受控报告 |

## 7. 平台能力

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | /files | 上传附件 |
| GET | /files/{id}/download | 授权下载附件 |
| GET | /workbench/todos | 当前用户待办 |
| GET | /notifications | 站内通知 |
| POST | /notifications/{id}/read | 标记已读 |
| GET/POST/PUT | /system/dictionaries | 字典管理 |
| GET/POST/PUT | /system/sequence-rules | 编号规则 |
| GET | /jobs/{id} | 异步任务状态 |
| GET | /health | 受控健康检查 |

## 8. 错误码分类

- `AUTH_*`：认证、会话和密码问题。
- `FORBIDDEN_*`：功能权限、数据范围或职责分离拒绝。
- `VALIDATION_*`：字段、状态或业务规则校验失败。
- `CONFLICT_*`：乐观锁、重复编号、库存并发或幂等冲突。
- `FILE_*`：上传、格式、存储、哈希或渲染问题。
- `SYSTEM_*`：不可预期错误，响应只返回追踪号，不暴露内部堆栈。

后续由代码生成 OpenAPI 文档，并以契约测试保证实现与本目录一致。
