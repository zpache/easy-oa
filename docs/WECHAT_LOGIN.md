# easy-oa 个人微信扫码登录设计

> 状态：设计稿（2026-11-17），待确认开放问题后进入实现。
> 范围：easy-oa 管理后台（web-ele + easy-oa-admin）新增「个人微信扫码登录」；
> 企业微信扫码/免登不在本期范围（后续可复用同一绑定表扩展 app_type）。

## 1. 前置条件（需要公司侧准备）

| 事项 | 说明 |
| --- | --- |
| 微信开放平台账号 | open.weixin.qq.com，**企业主体认证**（300 元/年），个人主体不能创建网站应用 |
| 网站应用 | 在开放平台创建「网站应用」，获得 `AppID` / `AppSecret` |
| 授权回调域名 | 配置为管理后台部署域名（如 `oa.easykits.cn`），需在该域名下放置域名归属验证文件；**localhost 无法用于真实回调** |
| 凭据管理 | AppSecret 通过环境变量 `OA_WECHAT_APPID` / `OA_WECHAT_SECRET` 注入，不入库不入仓 |

## 2. 身份与绑定模型

- 微信侧身份取 **unionid 为主、openid 兜底**：unionid 在同主体下跨应用稳定（后续小程序/企微扩展不用换主键）。
- 绑定关系一张表，多渠道按 `app_type` 区分：

```sql
CREATE TABLE IF NOT EXISTS sys_oauth_binding (
    id           bigint      NOT NULL AUTO_INCREMENT,
    user_id      bigint      NOT NULL COMMENT 'OA 账号',
    app_type     varchar(20) NOT NULL COMMENT 'wechat_open 个人微信（预留 wecom_qr 企业微信）',
    openid       varchar(64) NOT NULL COMMENT '应用内 openid',
    unionid      varchar(64) DEFAULT NULL COMMENT '同主体唯一，主判据',
    wx_nickname  varchar(64) DEFAULT NULL COMMENT '微信昵称（展示用）',
    wx_avatar    varchar(255) DEFAULT NULL COMMENT '微信头像',
    created_at / updated_at / created_by / updated_by / version / deleted,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unionid (unionid, deleted),
    UNIQUE KEY uk_openid (openid, deleted),
    KEY idx_user (user_id)
) COMMENT = '第三方身份绑定';
```

约束：**一个 unionid 只能绑定一个 OA 账号**（唯一索引兜底）；一个账号同渠道最多一条绑定。

## 3. 一次性凭证存储（Redis，2026-11-17 确认引入）

easy-oa 引入 Redis（`spring-boot-starter-data-redis` + StringRedisTemplate），
扫码流程的「一次性短时效凭证」全部走 Redis TTL key，不建表、无清理任务：

| Key | 值 | TTL | 用途 |
| --- | --- | --- | --- |
| `oa:qr:state:{state}` | `login` 或 `bind`（场景标记） | 5 分钟 | 授权链接防 CSRF，回调消费即 DEL |
| `oa:qr:bind:{bindTicket}` | JSON：openid/unionid/昵称/头像 | 10 分钟 | 未命中绑定时的微信身份暂存，绑定成功即 DEL |

- **消费即删除**（`GETDEL`，Redis ≥6.2）或 `GET` 后 `DEL`，天然防重放；key 不存在即过期/已用。
- `DEL`/`SETEX` 均为原子操作，无需定时清理。
- Redis 连接配置走 `spring.data.redis.*`，密码等环境变量注入（`OA_REDIS_HOST/PORT/PASSWORD`）；
  与 easy-mall 云实例一致的做法，本地 dev 可指向本机 Redis。
- Redis 短暂不可用时的降级策略：登录页隐藏微信入口（`WechatLoginService` 对 Redis 异常做健康检查），不影响账号密码登录主链路。

## 4. 三个流程

### 4.1 扫码登录（未绑定 → 引导绑定）

```
登录页点「微信扫码登录」
  → GET /oa/auth/wechat/qr-login-url
  → 后端生成 state（scene=qr_state），拼微信开放平台授权 URL：
    https://open.weixin.qq.com/connect/qrconnect
      ?appid={appid}&redirect_uri={redirect_uri}&response_type=code
      &scope=snsapi_login&state={state}#wechat_redirect
  → 前端 iframe 展示二维码，用户扫码确认
  → 微信 302 到 redirect_uri（前端路由 /auth/wechat/callback?code=xx&state=xx）
  → 回调页 POST /oa/auth/wechat/login { code, state }：
      1. 校验 state（存在、未过期、未使用）→ 消费
      2. code + secret 调 sns/oauth2/access_token 换 openid/unionid
      3. 调 sns/userinfo 取昵称头像（可选，失败不阻断）
      4. 查 sys_oauth_binding：
         命中 → 加载账号（校验启用、非锁定）→ 签发 JWT → 记登录日志（way=wechat-scan）→ 成功
         未命中 → 生成 bindTicket（payload=微信身份）→ 返回 { result: 'NEED_BIND', bindTicket, wxNickname }
  → 前端跳绑定引导弹窗
```

### 4.2 绑定引导（未绑定扫码后）

```
POST /oa/auth/wechat/bind { bindTicket, username, password, captcha? }
  1. 校验 bindTicket（未过期未使用）→ 消费
  2. 账号密码校验 —— 复用登录防爆破（5 次锁 15 分钟）与密码校验逻辑
  3. 校验该微信身份未被他人绑定（唯一索引兜底）
  4. 写 sys_oauth_binding → 直接签发 JWT（免二次登录）
```

- 是否允许「注册新账号并自动绑定」由配置开关控制 `oa.wechat.allow-register=false`（默认关：OA 员工账号由管理员创建，防止自助注册绕过入职流程）。

### 4.3 已登录主动绑定 / 解绑（个人中心）

```
POST /oa/auth/wechat/qr-bind-url   （登录态）→ 同 4.1 的授权 URL，state scene=qr_state
POST /oa/auth/wechat/bind-confirm { code, state } （登录态）
  → 换取 unionid → 校验未被他人绑定 → 写绑定表
DELETE /oa/auth/wechat/bind        （登录态）→ 解绑当前账号的 wechat_open 绑定
GET  /oa/auth/wechat/binding       （登录态）→ { bound, wxNickname, wxAvatar, boundAt }
```

解绑前提示：若账号已停用密码登录（未来若有此开关），不允许解绑最后一种登录方式；当前版本仅提示不强制。

## 5. 后端改动清单

| 项 | 内容 |
| --- | --- |
| 迁移脚本 | `db/migrations/2026MMDD_wechat_login.sql`（仅 sys_oauth_binding） |
| Redis | pom 增 `spring-boot-starter-data-redis`；`application.yml` 增 `spring.data.redis.*`（环境变量覆盖） |
| 配置 | `WechatLoginProperties`：enabled / appid / secret / redirect-uri / allow-register；`oa.wechat.*`，环境变量覆盖 |
| 服务 | `modules/auth/service/WechatLoginService`：qrUrl(state)、loginByCode、bindByUsername、bindByLoginState、unbind、bindingInfo；state/bindTicket 走 StringRedisTemplate（GETDEL 消费） |
| HTTP 客户端 | Spring RestClient 调微信 API（connect timeout 3s / read 5s，不重试——code 一次性，重试会 40163） |
| 控制器 | AuthController 扩展 qr-login-url / login / bind；个人中心 BindingController（binding / qr-bind-url / bind-confirm / unbind） |
| 安全 | 全部微信端点进免鉴权白名单（login/qr-login-url/bind）；bind 走密码验证复用 LoginAttemptService；登录日志补 `way=wechat-scan/bind` |
| JWT | 复用 JwtTokenProvider；LoginUser 构造与密码登录一致 |

## 6. 前端改动清单（web-ele）

| 项 | 内容 |
| --- | --- |
| 登录页 | 「微信扫码登录」按钮/tab；iframe 加载授权 URL；处理 NEED_BIND 结果 |
| 回调路由 | `/auth/wechat/callback`（免鉴权），校验 state → 调 login/bind-confirm → 成功进工作台 |
| 绑定引导弹窗 | 显示微信昵称，输入账号密码提交 bind；失败提示与登录一致 |
| 个人中心 | 「微信绑定」卡片：未绑 → 绑定按钮（走 4.3）；已绑 → 昵称头像 + 解绑 |
| 白名单 | 回调路由加入路由 meta 与 access 配置免登录可达 |

## 7. 安全要点

1. **state 一次性 + 5 分钟**，消费即失效，防 CSRF 与链接重放。
2. **绑定必须经密码验证**（4.2），微信身份绝不作为唯一凭据创建登录态。
3. bindTicket 不含可逆敏感信息（payload 仅为 openid/unionid/昵称），一次性、10 分钟过期。
4. 唯一索引兜底「一号一微信」；绑定冲突返回脱敏提示（如 `已绑定账号 zb***`）。
5. AppSecret 只从环境变量读，日志不打印 code/token/secret；微信 API 错误码映射为用户可读文案。
6. 防爆破与密码登录共享计数（同一账号维度），扫码绑定尝试同样计入。
7. 回调域名仅支持配置的域名，防 open redirect（redirect_uri 由后端配置拼接，前端不传）。

## 8. 开发与测试策略

- `oa.wechat.enabled=false` 时登录页隐藏入口；提供 `mock` 模式（仅 dev profile）：跳过微信 API，直接以任意 openid 走全流程，便于联调。
- 测试用例要点：state 重放/过期、bindTicket 重复消费、一微信绑两号（唯一键冲突）、禁用账号扫码、绑定接口爆破锁定。

## 9. 开放问题（实现前需确认）

1. 微信开放平台网站应用的 AppID/AppSecret 是否已具备？（未申请则先走企业认证 + 建应用，约 1-2 周）
2. 授权回调域名用哪个？（建议 `oa.easykits.cn`，需部署验证文件）
3. 是否允许扫码自助注册新账号？默认建议关闭。
4. 生产是否与本地 dev 并行需要 mock 开关？（建议保留）
