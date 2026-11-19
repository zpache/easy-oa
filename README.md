# easy-oa

轻量级 OA（办公自动化）系统，技术栈与 [easy-mall](../easy-mall) 一致。

- **后端**：Java 25 + Spring Boot 4.1.2 + MyBatis-Plus + MySQL，Spring Security + JWT 无状态认证
- **前端**：Vue3 + Vben Admin（Element Plus），动态菜单/按钮权限
- **端口**：后端接口服务 `8091`（context-path `/`），前端 dev server `5778`

## 模块

```
backend/easy-oa-admin   # 管理后台接口服务
web-admin/apps/web-ele  # 管理后台前端（vben web-ele，包名 @easy-oa/web-ele）
db/                     # easy_oa.sql 建库脚本 + migrations/ 幂等增量迁移（不入库，仅本地）
docs/FEATURES.md        # 功能清单与分期计划
```

## 快速开始

```bash
# 1. 初始化数据库
mysql -uroot -p < db/easy_oa.sql

# 2. 后端：配置本机密码
cp backend/easy-oa-admin/src/main/resources/application-local.yml.example \
   backend/easy-oa-admin/src/main/resources/application-local.yml
#    填入 spring.datasource.password

# 3. 启动后端
cd backend && mvn -pl easy-oa-admin spring-boot:run
# 接口文档 http://localhost:8091/swagger-ui.html

# 4. 前端（pnpm workspace，依赖已在 node_modules 就绪）
cd web-admin && pnpm -F @easy-oa/web-ele run dev
# 打开 http://localhost:5778

# 默认账号 admin / admin123
```

功能分期与实现进度见 [docs/FEATURES.md](docs/FEATURES.md)。
