# LIMS 部署说明

## 1. 环境要求

- Java 21
- PostgreSQL 15 及以上版本
- Nginx 1.20 及以上版本
- 操作系统：Windows Server 或主流 Linux

## 2. 文件说明

- `backend/lims-backend.jar`：后端服务
- `frontend/`：前端静态文件，部署到 Nginx
- `config/lims.env.example`：后端环境变量模板
- `config/nginx-lims.conf`：Nginx 站点配置示例
- `scripts/start-backend.ps1`：Windows 后端启动脚本
- `scripts/start-backend.sh`：Linux 后端启动脚本
- `database/migrations/`：数据库迁移脚本备份；后端首次启动会由 Flyway 自动执行

## 3. 数据库准备

```sql
create user lims with password '请替换为强密码';
create database lims owner lims encoding 'UTF8';
```

无需手工逐个执行迁移脚本。配置数据库连接后，首次启动后端会自动初始化表结构。

## 4. 后端部署

1. 将 `config/lims.env.example` 复制为实际环境配置，并修改数据库密码、JWT 密钥和管理员初始密码。
2. 将环境变量加载到系统或服务管理器。
3. 执行：`java -jar backend/lims-backend.jar`。
4. 检查：`http://服务器地址:8080/actuator/health`，应返回 `{"status":"UP"}`。

生产环境建议使用 systemd、Windows 服务或进程守护工具运行，不要依赖命令窗口常驻。

## 5. 前端部署

1. 将 `frontend/` 目录复制到 Nginx Web 根目录。
2. 参考 `config/nginx-lims.conf` 修改目录和后端地址。
3. 检查并重载 Nginx 配置。
4. 浏览器访问服务器地址。

前端使用 HTML5 History 路由，Nginx 必须配置 `try_files $uri $uri/ /index.html`。

## 6. 安全要求

- 必须修改 `JWT_SECRET`，建议不少于 32 字节的随机字符串。
- 必须修改数据库默认密码。
- 首次登录后立即修改管理员密码。
- 正式环境建议启用 HTTPS，并限制 PostgreSQL、8080 端口仅内网访问。
- 定期备份 PostgreSQL 数据库及部署配置。

## 7. 升级步骤

1. 备份数据库和旧部署包。
2. 停止后端服务。
3. 替换后端 JAR 和前端静态文件。
4. 启动后端，Flyway 会自动执行新增迁移。
5. 检查健康接口并完成业务验收。

