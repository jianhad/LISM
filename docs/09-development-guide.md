# 开发环境与启动指南

## 1. 环境要求

- JDK 21
- Maven 3.9+
- Node.js 22+（当前机器已有 Node.js 24）
- pnpm 10+（当前机器已有 pnpm 11）
- PostgreSQL 16 或兼容的 openGauss 开发实例

当前机器尚未安装 JDK、Maven和 Docker，因此前端可以直接构建，后端需补充 Java 环境后运行。

## 2. 数据库

开发环境可进入 `deploy` 目录，将 `.env.example` 复制为 `.env` 并修改密码，然后执行：

```powershell
docker compose -f compose.dev.yml up -d
```

如没有 Docker，可直接创建 PostgreSQL 数据库和用户。应用首次启动时 Flyway 自动执行 `backend/src/main/resources/db/migration` 中的脚本。

## 3. 后端启动

不得在源码或提交记录中保存正式密码。PowerShell 示例：

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/lims'
$env:DB_USERNAME='lims'
$env:DB_PASSWORD='开发数据库密码'
$env:JWT_SECRET='至少32字节的随机开发密钥'
$env:BOOTSTRAP_ADMIN_USERNAME='admin'
$env:BOOTSTRAP_ADMIN_PASSWORD='仅首次启动使用的临时密码'
mvn spring-boot:run
```

仅当数据库中不存在同名用户且配置了 `BOOTSTRAP_ADMIN_PASSWORD` 时，系统才创建初始管理员。创建后应清除该环境变量并立即修改初始密码。

主要地址：

- API：`http://localhost:8080/api/v1`
- 健康检查：`http://localhost:8080/actuator/health`
- OpenAPI UI：`http://localhost:8080/swagger-ui.html`（需登录并具有接口文档权限）

## 4. 前端启动

```powershell
Set-Location frontend
pnpm install
pnpm run dev
```

访问 `http://localhost:5173`。开发代理会将 `/api` 转发到后端的 8080 端口。

## 5. 构建和测试

```powershell
Set-Location backend
mvn clean verify

Set-Location ../frontend
pnpm run build
pnpm run test
```

后续将增加 Testcontainers 数据库集成测试、权限越权测试和 Playwright 登录流程测试。

## 6. 国产化切换规则

- 开发阶段使用 PostgreSQL 兼容环境，生产优先 openGauss。
- 确定达梦或人大金仓后，新增独立配置和迁移兼容验证，不直接修改既有迁移历史。
- 确定麒麟/统信和国产 CPU 版本后，建立对应构建、部署、压力与恢复测试环境。

