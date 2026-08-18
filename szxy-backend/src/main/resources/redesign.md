# 「数智乡约」项目重构设计文档（全量决策记录）

> 本文档完整记录 2026-08-18 会话中针对本项目重构的全部讨论、代码审查结论与最终决策。
> 目的：为本次重构（后端 MVP → 前端 → 阶段 B AI 重构）保留完整上下文。
>
> 会话参与人：项目作者（李瑞琦），AI 助手（opencode）
> 项目当前分支：`master`；远端含 `lrq / change1 / temp-save` 等分支

---

## 目录

1. [项目现状全梳理（重构前代码审查）](#1-项目现状全梳理重构前代码审查)
   - 1.1 项目定位与技术栈
   - 1.2 目录结构
   - 1.3 数据模型
   - 1.4 认证与请求链路
   - 1.5 业务模块明细
   - 1.6 中间件与定时任务
2. [问题清单（重构必须解决）](#2-问题清单重构必须解决)
   - 2.1 数据模型级（最严重）
   - 2.2 架构级
   - 2.3 代码卫生
3. [重构目标与总体决策（已锁定）](#3-重构目标与总体决策已锁定)
   - 3.1 用户决策记录
   - 3.2 MVP 边界（砍什么 / 留什么）
   - 3.3 阶段划分
4. [角色模型与约束力设计](#4-角色模型与约束力设计)
   - 4.1 角色体系（唯一事实源）
   - 4.2 枚举强类型化（@EnumValue / @JsonValue）
   - 4.3 权限规则映射
   - 4.4 角色/权限约束的三层防线
   - 4.5 注释规范
5. [新数据模型（7 张表）](#5-新数据模型7-张表)
   - 5.1 表结构
   - 5.2 删除的表
   - 5.3 核心业务流程（重写后）
6. [接口契约（后端先行）](#6-接口契约后端先行)
   - 6.1 接口清单
   - 6.2 删除的接口
7. [代码结构变更](#7-代码结构变更)
   - 7.1 新增
   - 7.2 重写
   - 7.3 删除
   - 7.4 pom 依赖调整
8. [已知 bug 一并修复](#8-已知-bug-一并修复)
9. [前端架构（后端完成后实施）](#9-前端架构后端完成后实施)
   - 9.1 管理端
   - 9.2 用户端
   - 9.3 用户端 UI 策略（Element Plus 桌面感问题的结论）
   - 9.4 为什么不做单前端角色路由
10. [验证与测试计划](#10-验证与测试计划)
11. [执行顺序](#11-执行顺序)
12. [阶段 B / 阶段 C（AI 重构与 Sentinel，后置）](#12-阶段-b--阶段-cai-重构与-sentinel后置)
13. [技术要点备忘](#13-技术要点备忘)
14. [附：会话中回答过的问题记录](#14-附会话中回答过的问题记录)

---

## 1. 项目现状全梳理（重构前代码审查）

### 1.1 项目定位与技术栈

**项目名**：数智乡约 —— AI 驱动乡村振兴服务平台（后端单体）。
服务对象：游客（浏览/互动）、农户（入驻/经营）、村长（本村管理）、管理员（审核/数据维护/报表）。
核心卖点：AI 行程推荐、智能问答、农产品图片识别（果蔬识别）。

**技术栈（重构前）**：

| 类别 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 框架 | Spring Boot | 3.5.11 | Java 17 |
| ORM | MyBatis-Plus | 3.5.16 | + mybatis-plus-jsqlparser |
| 数据库 | MySQL | - | `com.mysql.cj.jdbc.Driver` |
| 缓存 | Redis | - | StringRedisTemplate |
| 消息 | RabbitMQ | - | 验证码异步发送 |
| 搜索 | Elasticsearch | 7.11.2 | **RestHighLevelClient（官方已废弃）** |
| 实时 | WebSocket | - | `/ws/{sid}` 群发通知 |
| AI | Spring AI | 1.0.0-M6 | **旧版/里程碑版，计划弃用** |
| AI 模型 | DeepSeek | - | base-url 指向 `https://api.deepseek.com` |
| AI 附加 | dashscope-sdk-java | 2.22.13 | **实际未用**（同时引了 spring-ai） |
| 密码 | Spring Security | - | 只用 BCryptPasswordEncoder |
| 文档 | Knife4j / OpenAPI | 4.5.0 | `doc.html` |
| 上传 | 阿里云 OSS | 3.18.4 | 签名 V4 |
| 短信 | 阿里云 dypnsapi | 2.0.0 | 验证码 |
| 识别 | 百度云果蔬识别 | - | OKHttp 调用 |
| 定位 | 高德 IP API | - | RestTemplate 调用 |
| 工具 | hutool | 5.8.42 | BeanUtil/JSONUtil 等 |
| JSON | fastjson2 / org.json | - | 冗余并存 |

### 1.2 目录结构

```
com.shixiaoyu.xiangyueproject
├── config        AIConfiguration / AliyunProperties / ESConfiguration / GaodeProperties /
│                 MvcConfiguration / MvcPathProperties / MybatisPlusConfiguration /
│                 RabbitConfiguration / RecognizeProperties / RestTemplateConfiguration /
│                 ScheduleTask / SecurityConfiguration / SwaggerConfiguration /
│                 UploadConfiguration / WebSocketConfiguration / WebSocketServerConfigurator
├── constants     CommonConstants(186行,大量AI提示词) / ErrorConstants / RedisConstants
├── consumer      MailQueueConsumer / PhoneQueueConsumer  (RabbitMQ 验证码消费者)
├── controller    AdminFarmerController / AIController / FarmerController / LoginController /
│                 ReportController / ScenicController / UploadController / UserController / VillageController
├── entity
│   ├── dto       AIDTO / FarmerAccessDTO / FarmerUserDTO / ManagerAccessDTO / PageResultDTO /
│   │             ScenicAccessDTO / UserCommentDTO / UserDTO / UserSetInfoDTO / VillageBaseDTO / VillageSortDTO
│   ├── po        AITripRecommend / FarmerAccess / FarmerUser / GaodeIP / ManagerAccess / ScenicAccess /
│   │             User / UserCollect / UserComment / UserLike / VillageBase / VillageScenic
│   ├── result    ExternalMultiResult / InnerMultiResult / Result
│   └── vo        AIRecommendVO / FarmerAccessVO / FarmerUserVO / LCCVO / LocationVO / ManagerAccessVO /
│                 PageResultVO / ScenicVO / UserCommentVO / UserVO / VillageBaseVO
├── enums         AIGenerateEnum / FarmerTypeEnum / FeedbackEnum / ReviewStatusEnum / ScenicTypeEnum /
│                 UserStatusEnum / UserTypeEnum / VillageTypeEnum
├── exception     CommonException / UnauthorizedException
├── interceptor   FreshInterceptor / LoginInterceptor
├── mapper        AIMapper / FarmerAccessMapper / FarmerMapper / LoginMapper / ManagerAccessMapper /
│                 ReportMapper / ScenicAccessMapper / ScenicMapper / UserCollectMapper /
│                 UserCommentMapper / UserLikeMapper / UserMapper / VillageMapper / VillageScenicMapper
├── server        WebSocketServer
├── service
│   ├── Impl      AIServiceImpl(277) / FarmerServiceImpl(383) / LoginServiceImpl(222) /
│   │             ReportServiceImpl(18) / ScenicServiceImpl(190) / UploadServiceImpl(30) /
│   │             UserServiceImpl(353) / VillageServiceImpl(144)
│   └── (接口)     AIService / FarmerService / LoginService / ReportService / ScenicService /
│                 UploadService / UserService / VillageService
└── util          CacheUtils / ClientUtils / FlowUtils / LockUtils / MainUtils / PhoneUtils /
                  PVUVUtils / RecognizeUtils / UploadUtils / UserHolder / VerifyUtils
```

### 1.3 数据模型（实体推出来的表）

| 表 | 作用 | 关键字段 |
|---|---|---|
| `user` | 全角色统一账号 | `type`(1普通/2农户/3管理员), `status` |
| `village_base` | 农村基础信息 | `manage_id`(村长user_id), name/province/city/county/longitude/latitude/type/intro/image/best_time/activity/contact |
| `farm_user` | 农户扩展表 | `user_id`, `village_id`, `farm_name`, `id_card`, `type` ← **问题重灾区** |
| `farmer_access` | 农户资质申请表 | 用户→农户，status(0待审/1通过/2拒绝/3已过无村) |
| `manager_access` | 村长资质申请表 | 农户→村长 |
| `scenic_access` | 景点注册申请表 | 私人/公共景点申请 |
| `village_scenic` | 已通过景点表 | `likes`, `collections`, `has_accommodation`, `accommodation_info` |
| `user_comment` | 评论 | target_type(1景点), target_id, score, comment_img, is_show |
| `user_like` | 点赞 | user_id, target_id, create_time |
| `user_collect` | 收藏 | user_id, target_id, create_time |
| `ai_trip_recommend` | AI行程记录 | 定义了但**代码里从没写过** |

### 1.4 认证与请求链路

1. **发验证码** `/common/login/sendcode`：识别手机/邮箱 → IP 限流 60s（`VerifyUtils` + `FlowUtils.limitOnceCheck`）→ 验证码入 Redis 3min（`VERIFY_EMAIL_CODE_PREFIX` / `VERIFY_PHONE_CODE_PREFIX`）→ **RabbitMQ** 异步发短信（阿里云）/邮件（JavaMail）。消费者在 `consumer/MailQueueConsumer`、`PhoneQueueConsumer`。
2. **三种登录**（`LoginServiceImpl`）：
   - 手机验证码 / 邮箱验证码：不存在则自动注册（type=1, status=1, 默认头像, 用户名 `szxy用户-{手机/邮箱}`）。
   - 账密登录：BCrypt `matches` 校验。
   - 成功后生成 UUID token，`login:token:{token}` = UserDTO JSON 存 Redis，TTL 30min。
3. **两个拦截器**（`MvcConfiguration` 注册，include `/**`，exclude 登录/验证码/swagger/ws）：
   - `LoginInterceptor`：取 `authorization` header → 查 Redis token → 黑名单检查 → **status=0 自动解封为 1（BUG）** → `UserHolder.saveUser` → 刷新 TTL。
   - `FreshInterceptor`：刷新 token TTL + 记 PV/UV（排除 type=3）。
4. `UserHolder`（ThreadLocal）承载当前用户 `UserDTO`，业务层直接 `UserHolder.getUser()` 取。

### 1.5 业务模块明细

#### 登录 `/common/login`
sendCode / phone_login / email_login / pw_login / info_set（BCrypt 加密后 update）/ logout（删 AI 会话记忆 key）。

#### 用户端 `/user`
- info（查自己信息转 UserVO）
- comment / like / isLike / collect / isCollect（点赞收藏为 check-then-act + `±1` 更新 `village_scenic.likes/collections`；写前删 Redis 缓存 key）
- getLikes / getComments / getCollections（历史列表，走 `getVOList` 泛型方法：缓存 → 锁 → 双检 → 重建，随机 TTL 防雪崩）
- location（高德 IP 定位，**代码里硬编码测试 IP `111.53.227.84`**）
- search（ES multiMatchQuery 搜 name/intro/activity/province/city/county/managerName）
- apply_farmer（**直接写在 Controller 里**：查待审记录 → 插 farmer_access → WebSocket 群发 `farmer_access`）

#### 农户端 `/farmer`
- applyManager（申请村长：必须是 farm_user 且 type=1 → 插 manager_access）
- applyFarmer（绑定村落：先查待审申请 → 校验姓名/身份证/类型一致 → 更新 village_id + status=0）
- list（本村农户列表：`getVillageIdByChiefId` 找村长村 → `selectFarmersByVillageAndType` 查 type=1）
- addFarmer（村长新增农户：建 user（默认密码 123456，type=2）+ farm_user）
- updateFarmer / deleteFarmer（校验"只能操作本村"）
- set-manager（设/换村长：降旧村长 → 升新村长，跨 4 张表）
- village（我的村信息，`fillManagerName` N+1 查库拼村长名）
- scenic（我的景点，`fillScenicVillageName` 拼村名）

#### 管理端
- `/admin/farmer`：所有农户列表、农户/村长/景点资质申请列表与审核（solveFarmer / solveManager / solveScenic）
- `/admin/village`：农村 CRUD + top10 排行（**CRUD 完全没有管理员校验**）

#### 景点 `/scenic`
- registerPrivateScenic（农户）/ registerPublicScenic（`farm_user.type==2` 村长）→ 插 scenic_access status=0 → WebSocket 群发
- list（全部申请列表）
- scenic（top10 按 likes）
- detail（拼村名）
- sc_comments（评论，Redis 缓存含空值防穿透）

#### AI `/common/ai`（AIServiceImpl，全项目最复杂）
- recommend（行程推荐）：白名单话术走 Redis 缓存 → DeepSeek「村落AI」筛村落 ID → 并行双线程（景点AI筛景点 + 特产AI推特产，CountDownLatch 70s 超时）→ 组装 AIRecommendVO
- inquire（智能问答）：`commonChatClient` 带 `InMemoryChatMemory`（conversationId=userId）+ 手动写 Redis 列表 `ai:memory{userId}`
- multimodal（多模态识别）：校验图片 OSS 前缀 → 百度云果蔬识别 → 取置信度最高 → DeepSeek 生成介绍 → 按果蔬名缓存 72h
- memory（读 Redis 列表返回会话记忆）

#### 报表 `/report`
farmCnt（SQL count）/ uv / pv（Redis）/ uvpv（粘性）/ scenic（**HyperLogLog `new:scenic`，但写入是 `new:village`，键对不上 = BUG**）/ village（HyperLogLog）/ pv7 / uv7。

#### 上传 `/upload`
MultipartFile → 阿里云 OSS（UUID 随机文件名）→ 返回 URL。

### 1.6 中间件与定时任务

- **RabbitMQ**：只有验证码异步发送一条链路（direct 交换机 `szxy.direct`，email.verify / phone.verify）。
- **WebSocket** `/ws/{sid}`：全局 sessionMap 群发，只要有"新申请"就群发字符串，管理端收到后刷新列表。
- **定时任务**（`ScheduleTask`）：
  - 每 15s 重算两个 top10 排行榜（全量查库 → 清 ZSet → 重建）。
  - 每 15min **全量重建 ES**（删光 → 分页重灌 1000/页）。

---

## 2. 问题清单（重构必须解决）

### 2.1 数据模型级（最严重）

1. **`farm_user.type` 一列两义**：`addFarmer` 当"经营类型"(1民宿/2农产品/3文旅) 存；`getVillageIdByChiefId` / `promoteNewManager` / `registerPublicScenic` 又当"角色"(1农户/2村长) 用。**同一列两种含义，数据互相污染，权限判断错误。**
2. **`user.type=3` 语义冲突**：`setVillageManager` 把新村长 `user.type` 设为 3，而 `isAdminUser()` 也用 type==3 判断"平台管理员"。**村长 = 管理员，村长能过所有管理端校验。**
3. **枚举三套口径全不一致**：
   - `UserTypeEnum`：ADMIN=1 / USER=2 / FARMER=3（与实体注释相反）
   - 实体注释：1普通/2农户/3管理员
   - 实际代码 `isAdminUser()`：type==3 判管理员
   - `FarmerTypeEnum`：FARMER=1 / VILLAGE_MANAGER=2
4. **资质审核状态机混乱**：`farmer_access` 状态码出现 0/1/2/3（3="通过但没绑村"）；`solveFarmer` 通过后有时 `deleteById` 有时留 status=3。
5. **枚举与实体注释不一致**：`VillageTypeEnum` 9 个值 vs 实体注释 4 个；`ScenicTypeEnum` 5 个值 vs 注释 4 个。

### 2.2 架构级

6. **Controller 直接操作 Mapper/业务逻辑**：`UserController`、`FarmerController`、`ScenicController` 里散落查询/校验/insert/WebSocket 调用。
7. **没有全局异常处理器**：`CommonException`/`UnauthorizedException` 定义了但全项目无 `@RestControllerAdvice`，未捕获异常直接 500。
8. **鉴权靠手写**：`/admin/**` 无统一拦截器/注解，靠各 Service 手动 `isAdminUser()`；`VillageController` CRUD 完全没校验。
9. **`solveManager` 明显 bug**：`managerAccess.setId(null)` 后 `updateById`，等于自断主键。

### 2.3 代码卫生

10. 硬编码泄漏：测试 IP（`UserServiceImpl.java:177`）、百度云 `Authorization` 头明文（`RecognizeUtils.java:35`）、ES 地址 `"es", 9200` 写死、短信签名/模板写死。
11. `application.yml` 引用 `${mysql.url}` 等占位符，但仓库无 `application-dev.yml`/`application-local.yml`（历史 commit 说"需自建"）。
12. N+1 查询：`fillManagerName`、`getSorted` 等每行循环查库。
13. 依赖脏：es 7.11 RestHighLevelClient（官方废弃）、spring-ai 1.0.0-M6 里程碑版、javax.xml.bind/jaxb 旧包、okhttp 与 RestTemplate 混用、dashscope 与 spring-ai 双 AI 客户端并存（实际只用 spring-ai）。
14. 实体缺 `@TableName`（VillageBase、ScenicAccess 等靠默认驼峰转换碰巧对上）。
15. `MainUtils` 拼写错误（应是 MailUtils）；`LCCVO`/`VillageSortDTO`/`UserDTO` 里 `@TableId` 等 MP 注解出现在 DTO/VO 上是拷贝残留。
16. 定时任务每 15s 全量重建排行、每 15min 全量重建 ES，数据量大后吃力。
17. **`type` 字段裸 Integer，无约束力**：值写错编译器不拦，这是"开发时自己也晕"的根因之一。另一个根因是**几乎没有注释，已有注释还互相矛盾**。

---

## 3. 重构目标与总体决策（已锁定）

### 3.1 用户决策记录（逐条）

1. **AI 重构方向**：弃用旧版 Spring AI，改 Python 技术栈（LangChain/LangGraph Agent + Milvus 向量库存文旅信息）；Java 与 Py 之间用 OpenFeign 远程调用，**单开一个 `api` 包**存放 Py 相关接口；利用 langchain 生态的链式/管道符注入 + 提示词模板。→ **阶段 B 实施，不进 MVP。**
2. **Sentinel**：现阶段（Python 服务尚未存在）**不引入**。等阶段 B 有 Python 服务后，**OpenFeign 自带 fallback + 连接/读超时 + Redis 兜底缓存**已足够覆盖"Python 挂掉"场景。try...catch 只能解决"报错"、解决不了"挂起 70s 占线程"。真正上 Sentinel 需等到有真实部署流量 → **阶段 C 再议，MVP 不装。**
3. **数据模型梳理**：本次重构重点。→ 全新建库 + 种子数据，不迁移旧数据。
4. **ES 暂时去掉** → 改 MySQL 多字段 LIKE + 分页模糊查询。
5. **先缩 MVP**：先保证最小 MVP 功能完全正确，再谈 AI 等。→ 采纳。
6. **申请/审核业务整体撤销**：撤掉整个申请业务，不搞"黏在一起的混乱状态机"，后续有更好方案再议。→ 农户/村长由管理端直接建档，景点由农户/村长直接创建，**全程零审核状态机**。
7. **前端两套保留**：管理端（Element Plus 纯 PC）+ 用户端（游客/农户/村长三角色，同一项目内角色差异化，移动端+PC 双适配）。
8. **管理端账号**：种子数据预置 admin，不开放注册，管理员记住密码即可。
9. **开发顺序**：后端先行 → 前端对接（用户端先做，管理端后做）。

### 3.2 MVP 边界（砍什么 / 留什么）

| 模块 | 决定 | 理由 |
|---|---|---|
| 登录/注册（手机/邮箱/账密） | ✅ 保留 | 一切的基础 |
| RabbitMQ 异步发验证码 | ✅ 保留 | 已隔离、可用 |
| 上传 OSS | ✅ 保留 | 图片展示必需 |
| 农村/景点浏览 + top10 + 详情 | ✅ 保留 | 核心内容 |
| 点赞/收藏/评论 | ✅ 保留 | 核心交互 |
| 管理端农村 CRUD / 农户管理 / 报表 | ✅ 保留 | 报表改 DB 计数 |
| 管理端农户建档 / 村长任命 | ✅ 保留 | 角色体系（重写） |
| **AI 全系列**（推荐/问答/多模态/记忆） | ❌ 阶段 B | 与数据模型深度耦合 |
| **ES** | ❌ 移除 | 改 MySQL LIKE 分页 |
| **WebSocket 通知** | ❌ 移除 | 管理端改手动刷新 |
| **高德 IP 定位** | ❌ 移除 | 硬编码测试 IP，MVP 不需要 |
| **申请/审核业务**（farmer_access/manager_access/scenic_access） | ❌ 移除 | 状态机混乱，管理端直接建档 |
| 定时任务（top10 + ES 同步） | ❌ 移除 | top10 改按需查 + Redis 缓存 |

### 3.3 阶段划分

- **阶段 A（本次实施）**：后端 MVP 重构（数据模型重建、五层重写、鉴权统一、删 ES/AI/WS/定位、修 bug、验证）。
- **阶段 B（后续）**：Python FastAPI + LangGraph Agent + Milvus + Java `api` 包 OpenFeign 调用；DeepSeek 走 langchain-openai；多模态复用百度识别。前提：阶段 A 完全跑通。
- **阶段 C（可选）**：Sentinel 熔断降级。仅阶段 B 上线且有真实负载才评估；否则 OpenFeign 超时 + fallback + Redis 兜底足够。

---

## 4. 角色模型与约束力设计

### 4.1 角色体系（唯一事实源）

`user.role`：**1游客 / 2农户 / 3村长 / 4管理员**。语义唯一，`type` 改名 `role`。
村长另由 `village_base.manage_id` 指向其 user.id，与 `role` 在事务内同步。

| code | 角色 | 谁授予 | 能做什么 | 联动表 |
|---|---|---|---|---|
| 1 | 游客 VISITOR | 注册即得 | 浏览/搜索农村景点、top10、详情、评论、点赞、收藏 | - |
| 2 | 农户 FARMER | 管理端建档 | 游客全部 + 在所属村新增景点、查看我的村/我的景点 | farm_user |
| 3 | 村长 CHIEF | 管理端 set_manager 任命 | 农户全部 + 本村农户管理（增改删） | farm_user + village_base.manage_id |
| 4 | 管理员 ADMIN | 种子数据预置 | `/admin/**` 全部：农村 CRUD、农户建档、村长任命、景点 CRUD、报表 | - |

### 4.2 枚举强类型化（@EnumValue / @JsonValue）

**背景**：项目 `application.yml` 已配置 `default-enum-type-handler: MybatisEnumTypeHandler`，但实体全用裸 `Integer`，值写错编译器不拦。

**方案**：用 MyBatis-Plus `@EnumValue` 把枚举落库为 int，字段类型改为强类型枚举。

```java
public enum RoleEnum {
    VISITOR(1, "游客"),
    FARMER(2, "农户"),
    CHIEF(3, "村长"),
    ADMIN(4, "管理员");

    @EnumValue          // 存库/查库用这个字段（int）
    @JsonValue          // JSON 序列化输出 code（否则默认输出枚举名字符串）
    private final Integer code;
    private final String desc;

    RoleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

实体用法：
```java
@TableName("user")
public class User {
    private Long id;
    private RoleEnum role;   // 强类型，杜绝 ==3 魔法数
}
```

**要点**：
- `@EnumValue` 只影响 MyBatis 读写；JSON 序列化需额外加 `@JsonValue`（否则返回 `"role": "CHIEF"` 而不是 3）。
- `IEnum` 接口是老写法，`@EnumValue` 是新写法，选 `@EnumValue` + `@JsonValue`。
- `business_type`、景区 type、农村 type、评论 is_show 一律换强类型枚举。

**需要强类型化的枚举**：`RoleEnum`（新）、`BusinessTypeEnum`（新，1民宿/2农产品/3文旅）、`ScenicTypeEnum`（统一保留一份为准）、`VillageTypeEnum`（统一保留一份为准）、`CommentShowEnum`（0隐藏/1展示）。
**废弃的枚举**：UserTypeEnum、FarmerTypeEnum、ReviewStatusEnum、AIGenerateEnum、FeedbackEnum、UserStatusEnum。

### 4.3 权限规则映射

| 角色 | 能做什么 |
|---|---|
| 游客(1) | 浏览农村/景点/top10、评论、点赞、收藏、搜索 |
| 农户(2) | 游客全部 + 查看我的村/我的景点、**在所属村直接新增景点** |
| 村长(3) | 农户全部 + **本村农户管理**（增/改/删）、本村景点管理 |
| 管理员(4) | `/admin/**` 全部：农村 CRUD、农户建档、**村长任命**（事务内同步 manage_id + 新旧村长 role）、景点 CRUD、报表 |

### 4.4 角色/权限约束的三层防线

1. **编译期（强类型枚举）**：`RoleEnum role` 字段，非法值编译不过。
2. **应用层（统一出口）**：所有权限判断收敛到 `security/SecurityUtils`（`currentUser/isVisitor/isFarmer/isChief/isAdmin`），**禁止**业务代码写 `getType()==3`；`/admin/**` 由 `AdminInterceptor` 统一拦截（role≠4 → 401），Service 层再查一遍纵深防御。
3. **DB 层（CHECK）**：`role TINYINT CHECK (role IN (1,2,3,4))` 兜底绕过应用直写库。

### 4.5 注释规范

- 每个实体字段带 `@Schema(description)`（现有惯例，缺失补齐）。
- 枚举每个常量带完整注释：语义、code、谁授予、能做什么、联动表。
- Service 公开方法带 JavaDoc 说明权限前置条件（如"仅村长可调用，否则抛 UnauthorizedException"）。
- 状态/类型枚举注释与 `docs/roles.md` 权威说明保持一致，**杜绝注释与代码相反**。
- 建议新增 `docs/roles.md`：角色 ↔ 权限 ↔ 接口映射表。

---

## 5. 新数据模型（7 张表）

> 原则：**没有"申请→审批"路径**。农户/村长由管理端直接建档，景点由农户/村长直接创建。角色权限全靠 `user.role` + `village_base.manage_id` 判定。

### 5.1 表结构

**`user`**（全角色统一账号）
```sql
id BIGINT PK AUTO_INCREMENT
username VARCHAR
password VARCHAR        -- BCrypt
phone VARCHAR
email VARCHAR
avatar VARCHAR          -- 原 avator 拼写，可保留或修正
role TINYINT            -- 1游客/2农户/3村长/4管理员，CHECK(role IN (1,2,3,4))
status TINYINT          -- 1正常/0禁用
create_time DATETIME
update_time DATETIME
```

**`village_base`**（农村基础信息）
```sql
id BIGINT PK AUTO_INCREMENT
manage_id BIGINT        -- 村长 user.id
name VARCHAR
province VARCHAR
city VARCHAR
county VARCHAR
longitude DECIMAL
latitude DECIMAL
type TINYINT            -- 强类型枚举
intro TEXT
image VARCHAR           -- 多张逗号分隔
best_time VARCHAR
activity VARCHAR
contact VARCHAR
create_time DATETIME
update_time DATETIME
```

**`farm_user`**（纯农户档案，零审核字段）
```sql
id BIGINT PK AUTO_INCREMENT
user_id BIGINT UNIQUE   -- 与 user 一一对应
village_id BIGINT
farm_name VARCHAR
id_card VARCHAR
business_type TINYINT   -- 1民宿/2农产品/3文旅（强类型枚举，不再兼当角色）
create_time DATETIME
update_time DATETIME
-- status 删除（禁用走 user.status）
```

**`village_scenic`**（景点，直接上架无 status）
```sql
id BIGINT PK AUTO_INCREMENT
user_id BIGINT          -- 创建人
village_id BIGINT
name VARCHAR
intro TEXT
image VARCHAR
price INT
type TINYINT            -- 强类型枚举
has_accommodation TINYINT
accommodation_info VARCHAR
likes INT
collections INT
create_time DATETIME
update_time DATETIME
```

**`user_comment`**（只评景点）
```sql
id BIGINT PK AUTO_INCREMENT
user_id BIGINT
scenic_id BIGINT        -- 原 target_id + target_type 收敛为 scenic_id
content VARCHAR
score INT
comment_img VARCHAR
is_show TINYINT         -- 0隐藏/1展示
create_time DATETIME
```

**`user_like` / `user_collect`**（幂等）
```sql
id BIGINT PK AUTO_INCREMENT
user_id BIGINT
scenic_id BIGINT
create_time DATETIME
UNIQUE(user_id, scenic_id)
```

### 5.2 删除的表

`farmer_access`、`manager_access`、`scenic_access`、`ai_trip_recommend`（共 4 张）。
对应枚举、状态机、`solveXxx` 审批逻辑全部从代码移除。

### 5.3 核心业务流程（重写后）

1. **农户产生**：管理端建档 = 事务内建 `user`（role=2，默认密码 123456）+ `farm_user`。
2. **村长任命/换任**：事务内 `village_base.manage_id=新村长` + 旧村长 `role→2` + 新村长 `role→3` + 申请已删。
3. **景点创建**：农户/村长在所属村直接新增 `village_scenic`（无审核）；管理员可增删改。

---

## 6. 接口契约（后端先行）

### 6.1 接口清单

```
/common/login  sendcode | phone_login | email_login | pw_login | info_set | logout
/user          info | comment | like | isLike | collect | isCollect | search | likes | comments | collection
/farmer        list(本村农户) | add | update | delete(仅村长,role=3) | village(我的村) | scenic(我的景点)
/scenic        register(农户/村长在所属村新增) | top10 | detail | sc_comments
/admin/village list | add | update | delete | likes(top10) | collections(top10)
/admin/farmer  list | create(建 user+farm_user,默认密码123456) | set_manager(任命/换村长)
/admin/scenic  list | add | update | delete            ← 新增 AdminScenicController
/report        farmCnt | uv | pv | uvpv | pv7 | uv7 | village | scenic(当天新增,按 create_time 查库)
/upload        (保留)
```

### 6.2 删除的接口

`/user/apply_farmer`、`/user/location`、`/farmer/apply_manager`、`/farmer/apply_farmer`、`/scenic/private_scenic`、`/scenic/public_scenic`、`/scenic/list`(申请列表)、`/common/ai/*`（全部）、`/admin/farmer/farmer_access/*`、`/admin/farmer/manager_access/*`、`/admin/farmer/scenic_access/*`。

---

## 7. 代码结构变更

### 7.1 新增

- `entity/enums`：`RoleEnum`、`BusinessTypeEnum`、`CommentShowEnum`
- `controller/AdminScenicController`
- `exception/GlobalExceptionHandler`（`@RestControllerAdvice`，统一处理 CommonException/UnauthorizedException/参数校验/兜底 500）
- `interceptor/AdminInterceptor`（`/admin/**` 强制 role=4）+ `security/SecurityUtils`
- `db/schema.sql`、`db/data.sql`（种子：admin + 1村长 + 2农户 + 若干农村/景点）
- 各 service 按需查询（搜索 LIKE、top10 按需+缓存 3h）
- `docs/roles.md`（角色-权限-接口映射）

### 7.2 重写

- Controller：`UserController`、`FarmerController`、`ScenicController`、`AdminFarmerController`、`VillageController`、`ReportController`（逻辑全部下沉 Service）
- Service 层：`UserService`（search 改 LIKE 分页、like/collect 幂等+计数+缓存）、`FarmerService`（建档/任命/本村管理）、`ScenicService`（直接注册）、`VillageService`（CRUD/top10 按需）、`LoginService`、`ReportService`
- `interceptor/LoginInterceptor`：**修 status=0 自动解封 bug**
- `config/MvcConfiguration`：注册登录拦截 + admin 拦截，去 ws 静态资源
- `application.yml`：删 ai/gaode/baidu/ws 配置
- `mapper/FarmerMapper.xml`（去申请相关）、`mapper/UserMapper.xml`（保留+清理）

### 7.3 删除

- **config**：AIConfiguration、ESConfiguration、ScheduleTask、WebSocketConfiguration、WebSocketServerConfigurator、GaodeProperties、RecognizeProperties、RestTemplateConfiguration
- **controller**：AIController
- **service**：AIService、AIServiceImpl
- **server**：WebSocketServer
- **po**：FarmerAccess、ManagerAccess、ScenicAccess、AITripRecommend、GaodeIP
- **vo**：FarmerAccessVO、ManagerAccessVO、AIRecommendVO、LCCVO、LocationVO
- **dto**：AIDTO、FarmerAccessDTO、ManagerAccessDTO、ScenicAccessDTO、VillageSortDTO
- **result**：ExternalMultiResult、InnerMultiResult
- **enums**：UserTypeEnum、FarmerTypeEnum、ReviewStatusEnum、AIGenerateEnum、FeedbackEnum、UserStatusEnum
- **mapper**：LoginMapper（并入 UserMapper）、VillageScenicMapper（并入 ScenicMapper）、FarmerAccessMapper、ManagerAccessMapper、ScenicAccessMapper、AIMapper
- **util**：RecognizeUtils、CacheUtils（AI 专用）
- **xml**：mapper/ManagerAccessMapper.xml

### 7.4 pom 依赖调整

**删除**：es-rest-high-level-client、spring-ai-openai-spring-boot-starter、dashscope-sdk-java、okhttp、org.json、spring-boot-starter-websocket、spring-security-rsa（含 jaxb 相关）、fastjson2（如确认未用）。
**补充**：`spring-security-crypto`（BCrypt 来源，原依赖来自 spring-security-rsa 传递）。
**保留**：spring-boot-starter-web / mybatis-plus / mysql / redis / amqp / mail / knife4j / hutool / aliyun-oss / dypnsapi。

---

## 8. 已知 bug 一并修复

1. `LoginInterceptor` status=0（禁用）被自动解封为 1 → 改为：status=0 拒绝登录。
2. 报表 `new:scenic`（读） vs 写入 `new:village`（键不一致）→ 改 DB 当天 create_time 计数。
3. 新增农村/景点 HyperLogLog 计数随申请接口删除 → 用当天 create_time 查库。
4. 测试硬编码 IP、明文百度 AK 随模块删除自然消失。
5. `solveManager` 的 `setId(null)` bug 随审批业务删除消失。

---

## 9. 前端架构（后端完成后实施）

### 9.1 管理端

- **独立项目**，Vue3 + Vite + Element Plus + Pinia + Vue Router。
- 纯 PC Dashboard：登录（预置 admin）→ 农村 CRUD、农户列表/建档、村长任命、景点 CRUD、报表（PV/UV/排行/新增）。
- 不建议用若依等重型框架（除非要后台模板），自建即可。

### 9.2 用户端

- **单项目**（游客/农户/村长三角色），登录后按 `role` **动态路由 + 菜单过滤 + 路由守卫**。
- 双端适配：移动 H5 + PC 响应式。
- 三角色差异集中在"菜单 + 路由"，页面组件按业务模块划分，权限只在路由守卫和菜单过滤做。
  - **游客**：浏览/搜索农村景点、top10、详情、评论点赞收藏、个人信息
  - **农户**：游客全部 + 我的村/我的景点 + 新增景点
  - **村长**：农户全部 + 本村农户管理（增改删）+ 本村景点管理

### 9.3 用户端 UI 策略（Element Plus 桌面感问题的结论）

**不是二选一，而是分工**：

| 层 | 用谁 | 理由 |
|---|---|---|
| 展示/品牌页（首页、卡片流、详情、top10、搜索页） | 自定义组件 + 自定义 CSS | "用户端感觉"都在这里，手写才不像后台 |
| 交互/表单控件（表单、下拉、弹窗、上传、分页、评分、消息） | 复用 Element Plus + 全局改 CSS 变量 | 自己写又慢又易 bug |

落地要点：
1. **一套 design token**：颜色/圆角/间距/字号走 CSS 变量（`--el-color-primary` 等 EP 官方变量 + 自定义），移动端调断点+密度，PC 调宽度容器。
2. **展示层自定义、交互层复用 EP**：卡片/详情/列表手写；`el-form/el-dialog/el-upload/el-pagination/el-rate/ElMessage` 直接引。
3. **不建议** Vant(移动)+Element Plus(PC) 双组件库——双端适配+三角色再叠两套组件库，维护成本翻倍。

### 9.4 为什么不做单前端角色路由

讨论过"一套前端 + 角色路由"的可行性（集中式动态路由/守卫/菜单过滤，代码量增加是"一处"），但最终决策**保留两套**，理由：
- 管理端是纯桌面 Dashboard、用户端含移动端 H5，视觉与交互差异大。
- 你们已有两套前端代码，复用现有更省事（重复代码靠抽共享请求/组件包缓解）。
- 团队不想为单一工程引入复杂 Layout 切换。

---

## 10. 验证与测试计划

1. `mvn -q compile` 通过。
2. 集成测试（`xiangyue_test` 库 + test profile）：
   - 登录三式 + infoSet + 禁用用户被拒。
   - 管理端建档农户 → role=2；set_manager 换村长（role 3↔2 + manage_id 同步）。
   - 农户注册景点 → 归属正确；管理员改删。
   - 点赞/收藏/评论：幂等切换 + likes/collections 计数正确 + 缓存失效。
   - 搜索 LIKE 分页、top10 排序与缓存。
   - 报表各计数。
3. 全接口 smoke：起服务后 Knife4j `/doc.html` 走一遍接口契约。

---

## 11. 执行顺序

1. `db/schema.sql` + `db/data.sql`（建库 + 种子数据）。
2. entity/dto/vo/枚举层（强类型枚举）。
3. Mapper + XML。
4. Service 层（含搜索/排行/幂等）。
5. Controller + Admin 拦截 + 异常处理。
6. 删除上述模块与依赖、改 pom、清 application.yml。
7. `mvn compile` 修错 → 集成测试 → smoke。
8. 前端：用户端 → 管理端。

---

## 12. 阶段 B / 阶段 C（AI 重构与 Sentinel，后置）

### 阶段 B：AI 重构（MVP 跑通后再规划细节）

- Python FastAPI + LangChain/LangGraph Agent 智能体。
- Milvus 向量数据库存储文旅信息（农村/景点向量）。
- Java 侧单开 `api` 包存放 Py 相关 OpenFeign 接口。
- DeepSeek 走 langchain-openai。
- 多模态复用百度云果蔬识别。
- 天然使用 langchain 生态链式/管道符注入 + 提示词模板。

### 阶段 C：Sentinel（可选）

- 仅阶段 B 上线且有真实流量才评估。
- 否则 OpenFeign 连接/读超时 + fallback + Redis 兜底缓存即可。
- **结论：MVP 不装，大概率不装。**

---

## 13. 技术要点备忘

- **`@EnumValue`**：MyBatis-Plus（MyBatis 3.5.x 自带）注解，标记枚举中对应数据库列的字段；配合已配置的 `MybatisEnumTypeHandler` 自动转换。只影响 MyBatis 读写。
- **`@JsonValue`**：Jackson 注解，与 `@EnumValue` 同放一个字段，让接口返回 int 而非枚举名字符串。
- **admin.auth.enabled 配置开关**（讨论过）：生产 true；开发/演示可临时 false 跳过 AdminInterceptor。MVP 先用种子数据预置 admin，暂不加开关。
- **安全结论**：管理端必须登录（不能免登录），复用现有 token 鉴权基础设施，仅多套 AdminInterceptor 检查 role==4，成本几乎为零。
- **管理员账号**：种子数据预置 `admin`（BCrypt 密码），不开放注册；登录后 `info_set` 可改密码。

---

## 14. 附：会话中回答过的问题记录

| # | 问题 | 结论 |
|---|---|---|
| 1 | MVP 阶段哪些模块保留？ | 高德定位去掉、RabbitMQ 保留、OSS 保留、报表保留、WebSocket 去掉 |
| 2 | 角色与申请模型方案？ | 撤掉整个申请业务，后续再说，不做黏合状态机 |
| 3 | 现有数据如何处理？ | 全新建库 + 种子数据 |
| 4 | 前端是否同步改造？ | 后端先写好，前端后改 |
| 5 | 农户/村长如何产生？ | 管理端建档 |
| 6 | 景点由谁创建？ | 农户/村长直接新增 |
| 7 | 枚举落库方式？ | 用 MP 的 @EnumValue |
| 8 | 管理端需要登录吗？ | 需要，走同一套 token 鉴权 + AdminInterceptor |
| 9 | 前端一套还是两套？ | 两套：管理端(EP PC) + 用户端(三角色,双端适配) |
| 10 | 用户端技术栈？ | Element Plus（展示自研 + 交互复用 EP，全局 CSS 变量去桌面化） |
| 11 | 管理端脚手架？ | Vue3 + Element Plus |
| 12 | Sentinel 有必要吗？ | MVP 不装，Feign fallback 足够，阶段 C 再议 |
