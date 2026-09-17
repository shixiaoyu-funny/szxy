# 角色 ↔ 权限 ↔ 接口映射（唯一事实源）

> 对应代码：`enums/RoleEnum.java`、`utils/SecurityUtil.java`、`interceptor/LoginInterceptor.java`、`interceptor/AdminInterceptor.java`、`common/properties/PathProperties.java`
> 权威数据库脚本见 `szxy-backend/src/main/resources/db/xiangyue_full.sql`。

## 1. 角色体系

`user.role`：**1游客 / 2农户 / 3村长 / 4管理员**。由 `@EnumValue` 强类型落库、`@JsonValue` 序列化输出 int。

村长另由 `village_base.manage_id` 指向其 `user.id`，与 `role` 在事务内同步（`FarmerService.setVillageManager`，新增/修改村时也经此复用）。

| code | 角色 | 谁授予 | 能做什么 | 联动表 |
|---|---|---|---|---|
| 1 | 游客 VISITOR | 注册即得 | 浏览/搜索农村景点、top10、详情、评论、点赞、收藏、AI 聊天、个人中心、申请农户 | - |
| 2 | 农户 FARMER | 管理端建档 `/fmr/create` | 游客全部 + 本村新增景点 `/sc/rg`、我的村 `/fmr/vlg`、我的景点 `/fmr/sc`、卖家订单、申请村长 | farm_user |
| 3 | 村长 CHIEF | 管理端任命 `/fmr/set-manager` | 农户全部 + 本村农户管理 `/fmr/ls` 增改删、村长审批 `/fmr/vghead/pending-chief` | farm_user + village_base.manage_id |
| 4 | 管理员 ADMIN | 种子数据预置（admin） | 全部管理端能力：农村/景点 CRUD、农户建档、村长任命、准入审批、用户管理、报表 | - |

## 2. 鉴权机制（Redis + UUID 无状态，无 Spring Security）

### 2.1 登录与 token

- 登录 `/lg/pw`（或手机 `/lg/ph` / 邮箱 `/lg/em` 验证码）→ `TokenUtil` 生成 `IdUtil.simpleUUID()`，用户信息以 JSON 存 Redis `login:token:{token}`，TTL 6h。前端后续请求头带 **`access_token`**。
- 退出 `/lg/lgout` 删除 Redis 会话。

### 2.2 三档路径（`application.yml` 的 `auth.*`）

| 档 | 配置 | 行为 |
|---|---|---|
| `exclude-path` | 登录/Swagger/静态资源 | 完全不鉴权 |
| `optional-path` | 浏览类接口 | 有 token 则尽力解析身份并写入上下文；无 token 也放行（供游客浏览） |
| 其余（默认） | — | 强制登录，未登录/无效返回 401 |

- `exclude-path`：`/lg/sdcode`、`/lg/ph`、`/lg/em`、`/lg/pw`、`/doc.html`、`/webjars/**`、`/v3/api-docs/**`、`/swagger-ui.html`、`/swagger-ui/**`、`/favicon.ico`、`/error`。
- `optional-path`：`/sc/top10`、`/sc/detail`、`/sc/sccomments`、`/vlg/ls`、`/vlg/detail`、`/vlg/likes`、`/vlg/collections`、`/ur/search`、`/product/**`、`/pos/getPos`、`/pos/report`、`/ai/chat/anonymous`、`/upload`。

### 2.3 登录拦截（`LoginInterceptor`）

校验 `access_token` → Redis 命中 → **每请求从 DB 刷新 role/status**（禁用即 401、任命/降级即时生效）→ 续期回写 → 存入 ThreadLocal（`UserHolder`）。

### 2.4 管理端校验

- `AdminInterceptor` 统一拦截 `auth.admin-path`（当前 `/report/**`），role≠4 → HTTP 403。
- 其余管理端方法在 Service 层首行 `SecurityUtil.requireAdmin()`（抛 `BusinessException(403)`），由 `GlobalExceptionHandler` 转 `Result{code:403}`。

## 3. 接口契约

### 3.1 登录 `/lg`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/lg/sdcode` | 公开 | 发送验证码（手机/邮箱，RabbitMQ 异步） |
| POST | `/lg/ph` | 公开 | 手机号验证码登录/注册 |
| POST | `/lg/em` | 公开 | 邮箱验证码登录/注册 |
| POST | `/lg/pw` | 公开 | 账密登录 |
| POST | `/lg/lgout` | 登录 | 退出（删 Redis 会话） |

### 3.2 用户 `/ur`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/ur/infoset` | 登录 | 修改个人信息（password 明文服务端加密） |
| GET | `/ur/info` | 登录 | 当前用户信息 |
| POST | `/ur/comment` | 登录 | 评论景点（支持父评论回复） |
| POST | `/ur/like/{id}` | 登录 | 点赞/取消（幂等） |
| POST | `/ur/collect/{id}` | 登录 | 收藏/取消（幂等） |
| POST | `/ur/isLike` | 登录 | 是否已赞 |
| POST | `/ur/isCollect` | 登录 | 是否已藏 |
| GET | `/ur/search` | 可选登录 | 搜索（type=1 农村 / 2 景点，多字段模糊+分页） |
| GET | `/ur/like` | 登录 | 我赞过的景点 |
| GET | `/ur/comment` | 登录 | 我评论过的景点 |
| GET | `/ur/collection` | 登录 | 我收藏的景点 |
| GET | `/ur/msg/likes-received` | 登录 | 收到的点赞动态（消息页） |
| GET | `/ur/ls` | admin | 管理端用户分页 |

### 3.3 农户/村长 `/fmr`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/fmr/ls` | 村长 | 本村农户列表 |
| POST | `/fmr/new` | 村长 | 新增本村农户（默认密码 123456） |
| POST | `/fmr/modify/{id}` | 村长 | 修改本村农户 |
| POST | `/fmr/remote/{id}` | 村长 | 删除本村农户（档案删，账号降为游客） |
| GET | `/fmr/vlg` | 登录 | 我的所属村 |
| GET | `/fmr/sc` | 登录 | 我创建的景点 |
| GET | `/fmr/all` | admin | 全部农户列表 |
| POST | `/fmr/create` | admin | 建档农户（user role=2 + farm_user，默认密码 123456） |
| POST | `/fmr/set-manager` | admin | 任命/换村长（事务内同步 manage_id + 新旧村长 role） |

### 3.4 农户准入 `/fmr/access`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/fmr/access/apply` | 游客 | 提交/改提农户申请 |
| GET | `/fmr/access/mine` | 登录 | 本人最新申请 |
| GET | `/fmr/access/list` | admin | 申请分页 |
| POST | `/fmr/access/{id}/approve` | admin | 通过 |
| POST | `/fmr/access/{id}/reject` | admin | 拒绝 |

### 3.5 村长准入 `/fmr/vghead`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/fmr/vghead/apply` | 农户 | 申请成为村长 |
| GET | `/fmr/vghead/mine` | 登录 | 本人最新申请 |
| GET | `/fmr/vghead/my-list` | 登录 | 本人全部申请 |
| GET | `/fmr/vghead/pending-chief` | 村长 | 待审列表 |
| GET | `/fmr/vghead/pending-chief/count` | 村长 | 待审数量 |
| GET | `/fmr/vghead/list` | admin | 申请分页 |
| GET | `/fmr/vghead/{id}` | 本人/村长/admin | 申请详情 |
| POST | `/fmr/vghead/{id}/chief-approve` | 村长 | 村长初审通过 |
| POST | `/fmr/vghead/{id}/chief-reject` | 村长 | 村长初审拒绝 |
| POST | `/fmr/vghead/{id}/admin-approve` | admin | 终审通过 |
| POST | `/fmr/vghead/{id}/admin-reject` | admin | 终审拒绝 |

### 3.6 景点 `/sc`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/sc/rg` | 农户/村长 | 在所属村直接新增景点（无审核） |
| GET | `/sc/top10` | 可选登录 | 优质景点 top10（按点赞） |
| GET | `/sc/detail` | 可选登录 | 景点详情 |
| GET | `/sc/sccomments` | 可选登录 | 景点评论 |
| GET | `/sc/ls` | admin | 分页全部景点 |
| POST | `/sc/new` | admin | 新增景点（含创建门票/住宿商品） |
| POST | `/sc/modify/{id}` | admin | 修改景点（同步商品价格） |
| POST | `/sc/del/{id}` | admin | 删除景点 |

### 3.7 农村 `/vlg`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/vlg/ls` | 可选登录 | 分页农村信息 |
| GET | `/vlg/detail` | 可选登录 | 农村详情（含下属景点） |
| GET | `/vlg/likes` | 可选登录 | 优质农村 top10（按点赞，缓存 3h） |
| GET | `/vlg/collections` | 可选登录 | 优质农村 top10（按收藏，缓存 3h） |
| POST | `/vlg/new` | admin | 新增农村（可指定村长） |
| POST | `/vlg/modify/{id}` | admin | 修改农村（可改村长） |
| POST | `/vlg/del/{id}` | admin | 删除农村 |

### 3.8 报表 `/report`（AdminInterceptor 统一拦截 role=4）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/report/farm` | 农户总数 |
| GET | `/report/uv` | 当天 UV（HyperLogLog） |
| GET | `/report/pv` | 当天 PV |
| GET | `/report/uvpv` | 粘性 PV/UV |
| GET | `/report/scenic` | 当日新增景点 |
| GET | `/report/village` | 当日新增农村 |
| GET | `/report/pv7` | 近 7 天 PV 走势 |
| GET | `/report/uv7` | 近 7 天 UV 走势 |

### 3.9 交易模块
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/product/scenic/{scenicId}` | 可选登录 | 按景点查上架商品 |
| GET | `/cart/ls` | 登录 | 购物车列表 |
| POST | `/cart/add` | 登录 | 加入购物车 |
| POST | `/cart/alter/{id}` | 登录 | 修改数量/勾选 |
| POST | `/cart/del/{id}` | 登录 | 删除单项 |
| POST | `/cart/clear` | 登录 | 清空购物车 |
| GET | `/order/ls` | 登录 | 买家历史订单分页 |
| GET | `/order/{id}` | 登录 | 订单详情 |
| POST | `/order/preview` | 登录 | 确认页预览 |
| POST | `/order/add` | 登录 | 生成待支付订单（15min 延迟取消） |
| POST | `/order/alter` | 登录 | 修改待支付订单地址/备注 |
| POST | `/order/cancel/{id}` | 登录 | 取消订单 |
| POST | `/order/refund/{id}` | 登录 | 申请退款 |
| POST | `/order/confirm/{id}` | 登录 | 确认收货 |
| POST | `/order/use/{id}` | 登录 | 核销虚拟订单 |
| POST | `/order/del/{id}` | 登录 | 软删除终态订单 |
| GET | `/order/seller/ls` | 登录 | 卖家订单分页 |
| POST | `/order/seller/ship/{id}` | 登录 | 卖家发货 |
| POST | `/order/seller/refund/{id}` | 登录 | 卖家处理退款 |
| POST | `/order/admin/deliver/{id}` | admin | 管理员模拟送达 |
| GET | `/wallet/balance` | 登录 | 余额 |
| GET | `/wallet/records` | 登录 | 钱包流水 |
| POST | `/wallet/recharge` | 登录 | 模拟充值 |
| POST | `/wallet/pay/{orderId}` | 登录 | 余额支付（CAS） |
| GET | `/address/ls` | 登录 | 地址列表 |
| POST | `/address/add` | 登录 | 新增地址 |
| POST | `/address/alter` | 登录 | 修改地址 |
| POST | `/address/default/{id}` | 登录 | 设默认 |
| POST | `/address/del/{id}` | 登录 | 删除地址 |

### 3.10 AI `/ai`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/ai/chat/{id}` | 登录 | 发送消息（文本+图片多模态，持久化） |
| POST | `/ai/chat/anonymous` | 可选登录 | 游客匿名聊天（不落库，上下文由前端传） |
| GET | `/ai/gcBySID/{id}` | 登录 | 查询会话消息 |
| GET | `/ai/gssByUID` | 登录 | 会话列表 |
| POST | `/ai/new` | 登录 | 新建会话 |
| POST | `/ai/rename/{id}` | 登录 | 重命名会话 |

### 3.11 定位 `/pos` 与上传 `/upload`
| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/pos/getPos` | 可选登录 | 查省市区经纬度缓存 |
| POST | `/pos/savePos` | 登录 | 保存经纬度到 Redis |
| POST | `/pos/report` | 可选登录 | 上报用户定位（触发推荐） |
| POST | `/upload` | 可选登录 | 文件上传 OSS，返回 URL |

## 4. 越权场景一览

| 请求 | 角色 | 结果 |
|---|---|---|
| `/report/**` | 非 admin | 403（拦截器） |
| `/fmr/all`、`/fmr/create`、`/fmr/set-manager`、`/fmr/access/*`、`/fmr/vghead/admin-*` | 非 admin | 403 |
| `/sc/ls`、`/sc/new`、`/sc/modify/{id}`、`/sc/del/{id}` | 非 admin | 403 |
| `/vlg/new`、`/vlg/modify/{id}`、`/vlg/del/{id}` | 非 admin | 403 |
| `/ur/ls`、`/order/admin/deliver/{id}` | 非 admin | 403 |
| `/fmr/ls`、`/fmr/new`、`/fmr/modify/{id}`、`/fmr/remote/{id}` | 非村长/不在本村 | 500（BusinessException 业务提示） |
| `/sc/rg` 指定村 ≠ 所属村 | 农户/村长 | 500 业务提示 |
| 需登录接口（非 optional/exclude） | 未登录/禁用 | 401 |

## 5. 数据库

- 17 张表：`user`、`village_base`、`farm_user`、`village_scenic`、`user_comment`、`user_like`、`user_collect`、`farmer_access`、`vghead_access`、`chat_session`、`chat_message`、`user_address`、`shop_product`、`cart_item`、`trade_order`、`order_item`、`wallet_record`。
- 完整建库脚本：`szxy-backend/src/main/resources/db/xiangyue_full.sql`（含结构与种子数据，`DROP TABLE IF EXISTS` 幂等可重建）。
