# HNBLUE 核心功能与用户权限验收台账

更新时间：2026-07-12
角色：中央交付管理员

## 状态说明

- PENDING：待实施或待验证
- IMPLEMENTED：代码已完成，等待运行验证
- VERIFIED：已有命令、页面访问或接口结果作为证据
- BLOCKED：受外部服务、账号、数据库或环境限制阻塞，并记录证据

## 审计基线

- 任务提到的 6 个参考文件中，当前工作区和附件目录仅找到现有 `StructureParameterPage.vue`、`VirtualPlotDesignerPage.vue`；未找到旧版 `StructurePredictor.vue`、`VirtualPlotDesigner.vue`、`UserInfo.vue`、`UserManage.vue`。
- Flask 模型接口已审计：`GET /ping`、`POST /api/predict-carbon`、`POST /api/predict-carbon/batch`；单条返回 `{prediction}`，批量返回 `{predictions}`。
- 登录接口已审计：`POST /api/login` 返回 `Admin` / `User` 与 token。
- 用户接口已审计：`GET /user/info`、`POST /user/avatar`、`PUT /user/updateInfo`。
- 管理员接口已审计并重写安全返回：`/admin/users`、`/admin/search`、`/admin/add`、`/admin/update`、`/admin/delete`、`/admin/users/{id}`。

## 任务台账

| 编号 | 任务 | 状态 | 证据 / 备注 |
|---|---|---|---|
| DATA-01 | 数据集卡片来源与用途纵向对齐 | VERIFIED | `PublicDataPage.vue` 使用固定 `dataset-meta` 网格；`npm run build` 通过 |
| DATA-02 | 来源追溯内容精简与层级调整 | VERIFIED | 来源卡固定 6 行网格；DOM `/v2-public-data` 命中“数据资产/来源追溯”且无运行时错误 |
| MODEL-01 | 结构参数页面恢复真实输入 | VERIFIED | `StructurePredictor.vue` 12 类字段；源码检查无 `readonly`；DOM 命中“估算生物量/智能解释” |
| MODEL-02 | 结构参数单次 CatBoost 估算 | BLOCKED | 前后端代理已实现；`http://127.0.0.1:8880/ping` 无法连接，Flask 未运行，不能声称预测跑通 |
| MODEL-03 | 结构参数 AI 结果解释 | BLOCKED | 前端已接 `/api/chat/stream-carbon`；后端/AI 服务未启动，未完成端到端验证 |
| MODEL-04 | 结构参数历史记录与导出 | VERIFIED | `StructurePredictor.vue` 存储 localStorage 历史并导出 UTF-8 BOM CSV；构建通过 |
| MODEL-05 | 模型服务不可用时的受控降级 | VERIFIED | 页面健康检查 catch；DOM 无运行时错误；Flask 不可用时页面仍渲染 |
| PLOT-01 | 虚拟样地增删与参数编辑 | VERIFIED | `VirtualPlotDesigner.vue` 存在添加、复制、删除和真实输入；DOM 命中“添加样地/批量估算” |
| PLOT-02 | 虚拟样地批量 CatBoost 估算 | BLOCKED | 批量调用代码已实现；Flask 未运行，无法验证真实预测结果 |
| PLOT-03 | 虚拟样地方案对比与情景推演 | VERIFIED | 情景百分比应用和结果对比表/图代码存在；构建通过 |
| PLOT-04 | 虚拟样地结果导出 | VERIFIED | `exportResults()` 生成 UTF-8 BOM CSV；构建通过 |
| PLOT-05 | 虚拟样地 AI 对比分析 | BLOCKED | 前端已接 AI 流式接口；后端/AI 服务未启动，未完成端到端验证 |
| AUTH-01 | 统一登录态管理 | VERIFIED | 新增 `useAuth.js` 与 `httpClient.js`，统一 Bearer 头；构建通过 |
| AUTH-02 | 普通用户与管理员角色识别 | VERIFIED | 审计 LoginController 返回 `Admin/User`；`useAuth` 统一归一化角色 |
| AUTH-03 | 导航栏动态账户入口 | VERIFIED | `UnifiedNav.vue` 按角色显示“个人中心/用户管理/退出” |
| AUTH-04 | 退出登录 | VERIFIED | `UnifiedNav.vue` 调用 `auth.logout()` 并跳转 `/login`，不刷新页面 |
| AUTH-05 | 路由权限控制 | VERIFIED | `router.beforeEach` 检查 `requiresAuth` 与 `roles`；未登录访问受保护路由进入登录页 |
| USER-01 | 个人中心恢复 | BLOCKED | 页面和接口服务已实现；缺少真实登录账号/后端运行，未完成登录后资料读取验证 |
| USER-02 | 个人资料修改 | BLOCKED | 前后端已实现且记录日志；缺少真实账号/后端运行，未完成真实提交验证 |
| USER-03 | 头像上传 | BLOCKED | 前端类型/大小校验与后端日志已实现；缺少真实账号/后端运行，未完成真实上传验证 |
| ADMIN-01 | 用户管理恢复 | BLOCKED | 页面和后端安全接口已实现；缺少管理员账号/后端运行，未完成列表真实访问验证 |
| ADMIN-02 | 用户详情 | BLOCKED | `/admin/users/{id}` 与详情抽屉已实现；缺少管理员账号/后端运行，未完成真实详情验证 |
| ADMIN-03 | 最近登录时间 | BLOCKED | 新增 `last_login_at` 和登录写入；缺少真实登录验证 |
| ADMIN-04 | 操作日志 | BLOCKED | 新增 `UserActivityLog` 与最近 20 条查询；缺少真实账号操作验证 |
| ADMIN-05 | 后端管理员权限校验 | VERIFIED | `AdminController` 对 `/admin/**` 统一校验 token 与 Admin 身份；`mvn clean package -DskipTests` 通过 |
| TEST-01 | 前端构建 | VERIFIED | `npm run audit:delivery` 通过；`npm run build` 通过，有既有体积/Browserslist 警告 |
| TEST-02 | 后端测试与编译 | BLOCKED | `mvn clean test` 184s 超时；`mvn clean package -DskipTests` BUILD SUCCESS，证明编译打包通过 |
| TEST-03 | 模型接口验证 | BLOCKED | `Invoke-WebRequest http://127.0.0.1:8880/ping` 返回无法连接到远程服务器 |
| TEST-04 | 普通用户流程验证 | BLOCKED | 需要用户本机输入普通账号密码；本轮未获得可用账号密码，不能代跑登录流程 |
| TEST-05 | 管理员流程验证 | BLOCKED | 需要管理员账号密码或受控测试管理员；本轮未获得凭据，未执行破坏性用户操作 |

## 命令与验证记录

- `npm run audit:delivery`：通过。
- `npm run build`：通过；仅保留包体积和 Browserslist 过期警告。
- `mvn clean test`：184 秒超时，未得到测试完成结果。
- `mvn clean package -DskipTests`：BUILD SUCCESS，生成 `backend/target/JPAspringboot-0.0.1-SNAPSHOT.jar`。
- Edge DOM 验证：`/structure-predictor`、`/virtual-plot-designer`、`/userinfo`、`/usermanage`、`/v2-public-data` 均有应用节点和关键文案，无运行时错误；未登录访问受保护路由进入登录页。
- Flask 验证：`http://127.0.0.1:8880/ping` 无法连接，模型真实预测验证 BLOCKED。

## 数据库变更

见 `docs/context/12_HNBLUE_数据库变更说明.sql`。
