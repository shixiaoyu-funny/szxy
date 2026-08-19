# 角色 ↔ 权限 ↔ 接口映射（唯一事实源）

> 对应代码：`enums/RoleEnum.java`、`util/SecurityUtils.java`、`interceptor/AdminInterceptor.java`、`interceptor/LoginInterceptor.java`
> 权威表结构见 `db/schema.sql`；接口路径以本文件为准，`redesign.md` 的 §6.1 同步维护。

## 1. 角色体系

`user.role`：**1游客 / 2农户 / 3村长 / 4管理员**。语义唯一（`type` 已改名 `role`），由 `@EnumValue` 强类型落库、`@JsonValue` 序列化输出 int。

村长另由 `village_base.manage_id` 指向其 `user.id`，与 `role` 在事务内同步（`setVillageManager`）。

| code | 角色 | 谁授予 | 能做什么 | 联动表 |
|---|---|---|---|---|
| 1 | 游客 VISITOR | 注册即得 | 浏览/搜索农村景点、top10、详情、评论、点赞、收藏、个人中心 | - |
| 2 | 农户 FARMER | 管理端建档（`/fmr/create`） | 游客全部 + 在所属村新增景点（`/sc/rg`）、我的村（`/fmr/vlg`）、我的景点（`/fmr/sc`） | farm_user |
| 3 | 村长 CHIEF | 管理端任命（`/fmr/set-manager`） | 农户全部 + 本村农户管理（`/fmr/ls` 增改删） | farm_user + village_base.manage_id |
| 4 | 管理员 ADMIN | 种子数据预置（admin/admin123） | 全部管理端能力：农村CRUD、农户建档、村长任命、景点CRUD、报表 | - |

## 2. 鉴权机制（Redis + UUID 无状态，无 Spring Security）

1. **登录**：`/lg/pw`（或手机/邮箱验证码）→ `TokenUtil.issue` 生成 `IdUtil.simpleUUID()`，用户信息以 JSON 存入 Redis `login:token:{token}`，TTL 6h。前端后续请求头带 **`access_token`**。
2. **登录拦截**（`LoginInterceptor`，注册 `/**` 除 exclude-path）：校验 `access_token` → Redis 命中 → **每请求从 DB 刷新 role/status**（禁用即 401、任命/降级即时生效）→ 续期回写 → 存入 ThreadLocal。
3. **管理端校验**：
   - `AdminInterceptor` 统一拦截 `auth.admin-path`（当前 `/report/**`），role≠4 → 403。
   - 其余管理端方法在 Service 层首行 `SecurityUtils.requireAdmin()`（抛 `BusinessException(403)`），由 `GlobalExceptionHandler` 转 `Result{code:403}`。
4. 放行路径（`auth.exclude-path`）：`/lg/sdcode|ph|em|pw`、Swagger（`/doc.html`、`/webjars/**`、`/v3/api-docs/**`、`/swagger-ui.html`、`/swagger-ui/**`）、`/favicon.ico`、`/error`。

## 3. 接口契约

### 3.1 登录 `/lg`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/lg/sdcode` | 公开 | 发送验证码（手机/邮箱，IP 限流 + RabbitMQ 异步） |
| POST | `/lg/ph` | 公开 | 手机号验证码登录/注册 |
| POST | `/lg/em` | 公开 | 邮箱验证码登录/注册 |
| POST | `/lg/pw` | 公开 | 账密登录 |
| POST | `/lg/infoset` | 登录 | 修改个人信息（密码 BCrypt 加密） |
| POST | `/lg/lgout` | 登录 | 退出（删 Redis 会话） |

### 3.2 用户端 `/ur`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ur/info` | 登录 | 当前用户信息 |
| POST | `/ur/comment` | 登录 | 评论景点 |
| POST | `/ur/like/{id}` | 登录 | 点赞/取消（幂等） |
| POST | `/ur/collect/{id}` | 登录 | 收藏/取消（幂等） |
| POST | `/ur/isLike` | 登录 | 是否已赞 |
| POST | `/ur/isCollect` | 登录 | 是否已藏 |
| GET | `/ur/search` | 登录 | 搜索农村（多字段 LIKE + 分页） |
| GET | `/ur/like` | 登录 | 我赞过的景点 |
| GET | `/ur/comment` | 登录 | 我评论过的景点 |
| GET | `/ur/collection` | 登录 | 我收藏的景点 |

### 3.3 农户端 `/fmr`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/fmr/ls` | 村长 | 本村农户列表 |
| POST | `/fmr/new` | 村长 | 新增本村农户（默认密码 123456） |
| POST | `/fmr/modify/{id}` | 村长 | 修改本村农户 |
| POST | `/fmr/remote/{id}` | 村长 | 删除本村农户（档案删，账号降为游客） |
| GET | `/fmr/vlg` | 农户+ | 我的村 |
| GET | `/fmr/sc` | 农户+ | 我的景点 |

### 3.4 管理端-农户（Service `requireAdmin`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/fmr/all` | 全部农户列表 |
| POST | `/fmr/create` | 建档农户（user role=2 + farm_user，默认密码 123456） |
| POST | `/fmr/set-manager` | 任命/换村长（事务内同步 manage_id + 新旧村长 role） |

### 3.5 景点 `/sc`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/sc/rg` | 农户/村长 | 在所属村直接新增景点（无审核） |
| GET | `/sc/top10` | 公开 | 优质景点 top10（按点赞） |
| GET | `/sc/detail` | 公开 | 景点详情 |
| GET | `/sc/sccomments` | 公开 | 景点评论 |

### 3.6 管理端-景点（Service `requireAdmin`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/sc/ls` | 分页全部景点 |
| POST | `/sc/new` | 直接新增景点到指定村 |
| POST | `/sc/modify/{id}` | 修改景点 |
| POST | `/sc/del/{id}` | 删除景点 |

### 3.7 农村 `/vlg`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/vlg/ls` | 公开 | 分页农村信息 |
| GET | `/vlg/likes` | 公开 | 优质农村 top10（按下属景点点赞和，缓存 3h） |
| GET | `/vlg/collections` | 公开 | 优质农村 top10（按下属景点收藏和，缓存 3h） |

### 3.8 管理端-农村（Service `requireAdmin`）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/vlg/new` | 新增农村 |
| POST | `/vlg/modify/{id}` | 修改农村 |
| POST | `/vlg/del/{id}` | 删除农村 |

### 3.9 报表 `/report`（AdminInterceptor 统一拦截 role=4）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/report/farm` | 农户总数 |
| GET | `/report/uv` | 当天 UV（HyperLogLog） |
| GET | `/report/pv` | 当天 PV |
| GET | `/report/uvpv` | 粘性 PV/UV |
| GET | `/report/scenic` | 当日新增景点（DB create_time 计数） |
| GET | `/report/village` | 当日新增农村（DB create_time 计数） |
| GET | `/report/pv7` | 近 7 天 PV 走势 |
| GET | `/report/uv7` | 近 7 天 UV 走势 |

### 3.10 上传 `/upload`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/upload` | 登录 | 单文件上传 OSS，返回 URL |

## 4. 越权场景一览

| 请求 | 角色 | 结果 |
|---|---|---|
| `/fmr/all`、`/fmr/create`、`/fmr/set-manager` | 非 admin | 403 |
| `/sc/ls`、`/sc/new`、`/sc/modify/{id}`、`/sc/del/{id}` | 非 admin | 403 |
| `/vlg/new`、`/vlg/modify/{id}`、`/vlg/del/{id}` | 非 admin | 403 |
| `/report/**` | 非 admin | 403（拦截器） |
| `/fmr/ls`、`/fmr/new`、`/fmr/modify/{id}`、`/fmr/remote/{id}` | 非村长/不在本村 | 500（BusinessException 业务提示） |
| `/sc/rg` 指定村 ≠ 所属村 | 农户/村长 | 500 业务提示 |
| 全部 `/ur/**`、`/fmr/**` | 未登录/禁用 | 401 |
