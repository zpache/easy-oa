# easy-oa

轻量级 OA（办公自动化）系统，技术栈与 [easy-mall](../easy-mall) 一致。

- **后端**：Java 25 + Spring Boot 4.1.2 + MyBatis-Plus + MySQL，Spring Security + JWT 无状态认证
- **前端**：Vue3 + Vben Admin（Element Plus），动态菜单/按钮权限
- **端口**：admin 接口服务 `8091`，context-path `/oa`

## 模块

```
backend/easy-oa-admin   # 管理后台接口服务
db/                     # easy_oa.sql 建库脚本 + migrations/ 幂等增量迁移（不入库，仅本地）
docs/FEATURES.md        # 功能清单与分期计划
web-admin/              # 管理后台前端（规划中）
```

## 快速开始（后端）

```bash
# 1. 初始化数据库
mysql -uroot -p < db/easy_oa.sql

# 2. 配置本机密码
cp backend/easy-oa-admin/src/main/resources/application-local.yml.example \
   backend/easy-oa-admin/src/main/resources/application-local.yml
#    填入 spring.datasource.password

# 3. 编译并启动
cd backend && mvn -pl easy-oa-admin spring-boot:run
# 或 java -jar easy-oa-admin/target/easy-oa-admin-1.0.0.jar

# 4. 访问
# 接口文档 http://localhost:8091/oa/swagger-ui.html
# 默认账号 admin / admin123
```

功能分期与实现进度见 [docs/FEATURES.md](docs/FEATURES.md)。
