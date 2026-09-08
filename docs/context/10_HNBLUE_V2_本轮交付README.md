# HNBLUE V2.0 本轮交付 README

## 1. 本轮产品化重构目标

本轮目标是把 HNBLUE 从“多个页面拼接的毕业设计展示系统”推进为更完整的海南蓝碳数字化应用系统。重点完成：统一导航和页面壳、政府端浅/深主题、统一按钮/表格/搜索、数据资产空白区修复、碳溯地图入口、AI 助手板块、工单式增删改流程，以及 V1.0 可继承资产审计。

本轮未运行 Maven，未启动 Spring Boot，未启动后端服务。已运行前端静态检查和 `npm run build`。

## 2. V1.0 可继承资产审计结果

直接迁移或优先继承：

- `frontend/src/components/DeVisualPage.vue`：存在 V1 区域详情页，包含区域摘要、碳储/通量趋势、多碳指标、物种组成、经济价值、PDF 导出等工作量。适合后续接入 V2 区域详情抽屉或 `/visual` 深度详情页。
- `frontend/src/components/CarbonChat.vue`：存在旧 CarbonSeek AI 聊天组件，使用 EventSource 流式接口和 Markdown 展示；可作为学习型 AI 助手体验参考。
- `backend/src/main/java/com/example/jpaspringboot/controller/V2ApiController.java`：已有 `/api/v2/ai/context`，本轮政府端 AI 助手先复用结构化上下文检索，不硬编码 API Key。
- `flask_model/carbon_model_api` 与 `flask_model/General_Modeling.py`：存在 CatBoost、BAAD 模型训练/预测相关资产，适合碳溯模型解释和接口待验证说明。
- `data/staging/species/species_and_allometry_records.csv`：可见 BAAD、Tallo、ChinAllomeTree 等公开数据来源痕迹，已在数据资产中心作为科研/模型学习数据资产展示。

改造后迁移：

- V1 地图/地市详情依赖的 `frontend/src/assets/json/hainan.json` 当前未在项目路径中找到，`DeVisualPage.vue` 仍有旧接口和旧样式依赖。已先新增 V2 产品化地图壳层 `HainanBlueCarbonMap.vue` 和 `/visual` 页面，后续可把 V1 详情图表逐步迁入。
- 旧 AI 聊天组件存在中文乱码和旧资源依赖，暂未直接挂到政府端；政府端改为正式 AI 助手板块，碳溯仍保留 `/ai-assistant` 学习入口。
- CatBoost/Python 服务当前不作为本轮运行依赖；碳溯页面保留模型流程、估算示例和接口待验证边界。

保留但不主推：

- 依赖旧接口、旧地图 JSON、旧蓝色视觉或演示模拟数据的页面暂不删除，避免 V1 功能丢失，但不放入政府端主流程。
- 直接编辑真实数据的思路不继续推进，统一改为工单审批流程。

## 3. 新增公共组件列表

新增或完善 `frontend/src/components/common`：

- `UnifiedNav.vue`：统一导航。
- `PageShell.vue`：统一页面背景和外层布局。
- `PageHero.vue`：统一页面头部。
- `ThemeToggle.vue`：历史主题切换组件已保留但政府端不再使用；当前主题入口统一由 `UnifiedNav.vue` 管理。
- `SearchPanel.vue`：统一筛选表单，已接入统一按钮。
- `ActionButton.vue`：统一查询、重置、详情、复制、导出、新建、提交、审批等按钮。
- `SourceBadge.vue`：统一 `source_code` 徽章，长文本截断，悬浮显示完整内容。
- `RecordPreview.vue`：政府端右侧 hover 摘要。
- `DetailDrawer.vue`：统一记录详情抽屉。
- `EmptyState.vue`：统一空状态。
- `StatCard.vue`：统一指标卡。
- `DataTable.vue`：统一表格、横向滚动、操作列和行 hover 预览。
- `hnblue-ui.css`：统一色彩、圆角、阴影、页面宽度和动效降级。

## 4. 全站 UI 规范

- 默认采用浅色生态背景、深色文字、蓝绿色强调色。
- 首页和政府端允许深色 hero，但内容区必须同主题协调。
- 页面一级标题尽量短：碳溯、数据资产、治理工作台、关于项目。
- 不在主视觉区展示 `Proxy`、`boundary_notes`、数据库英文字段等技术提示。
- 卡片、按钮、表格、搜索框统一 8px 圆角、轻阴影、稳定高度和 hover 状态。
- 所有空数据区域使用 `EmptyState`，不让页面整块空白。
- 动效克制，并通过 `prefers-reduced-motion` 降级。

## 5. 主题切换实现说明

- 政府端已移除页面内独立 `ThemeToggle.vue`，统一使用全站导航主题按钮。
- `/v2-government` 不再读写 `hnblue_government_theme`，统一服从 `hnblue_site_theme`。
- 浅色主题为正式办公风格：浅背景、深色文字、白色卡片。
- 深色主题为研判风格：深色背景、浅色文字、深色卡片、协调表格和表单。
- 切换不依赖后端。

## 6. 首页优化内容

- 首页继续作为系统总入口，突出海南蓝碳资源、数据资产、碳溯和政府端能力。
- 入口卡片使用中文主名 + 英文短名：碳溯 / CarbonSeek、数据资产 / DataAssets、政府端 / GovDesk、关于 / AboutHub。
- 右侧视觉采用 CSS 生态数字面板，不依赖缺失图片。
- 动效使用轻量 CSS 和鼠标视差，不引入大型新依赖。

## 7. 碳溯优化内容

- 定位为学习、模型解释、碳储估算过程展示、数据来源理解和案例推演。
- 新增“探索图”区块，接入 `HainanBlueCarbonMap.vue`。
- 保留模型流程、估算示例图、解释重点和模块入口。
- 公共数据集在碳溯语境中解释为模型学习数据，不作为政府端管理功能。

## 8. 数据资产中心优化内容

- “公开数据 2.0”已在页面文案上重组为“数据资产”。旧路径继续兼容。
- 新增“数据集库”：BAAD、Tallo、ChinAllomeTree、HNBLUE normalized。
- 红树林面积、区域指标、文献证据、来源追溯继续作为共享数据底座展示。
- 图表容器保持稳定高度，接口或数据包失败时显示空状态。
- 区域指标中部空白和红树林面积右侧空白已用图表/摘要/空状态结构修复。

## 9. 政府端工作台优化内容

- 页面标题改为“治理工作台”，短标题更正式。
- 新增浅/深主题切换，解决深色头部与浅色内容割裂。
- 一张图、指标库、文献证据、来源追溯全部复用 `SearchPanel`。
- 表格改用 `DataTable`，操作列固定宽度，按钮横排，不再竖向挤压。
- 操作按钮改用 `ActionButton`。
- 长 `source_code` 改用 `SourceBadge` 截断和悬浮显示。
- hover 区域卡片或表格行时更新右侧 `RecordPreview`。
- 点击详情打开 `DetailDrawer`；关闭后右侧预览保留最近记录。
- 增强数据全为 0 时只显示“增强数据待接入”轻量空状态。

## 10. 海南地图和区域详情融合情况

- 已新增 `frontend/src/components/HainanBlueCarbonMap.vue`。
- 已新增 `/visual` 页面 `MapExplorePage.vue`，旧 `/devisual` 作为别名进入地图页。
- 地图当前为 V2 产品化壳层，展示海口、文昌、儋州、万宁、三亚、东方等区域和融合状态。
- V1 `DeVisualPage.vue` 的深度区域图表已审计，下一轮建议迁入地图详情抽屉或区域详情页。
- 因 `hainan.json` 未找到，未直接恢复旧 ECharts GeoJSON 地图，避免运行态报错。

## 11. AI 助手融合情况

- 政府端已新增“AI 助手”标签页，替代独立“AI 上下文”标签页。
- AI 助手调用 `/api/v2/ai/context` 检索来源、面积记录、区域指标和文献碳储上下文。
- 页面生成可引用的研判草稿，并展示来源代码、区域、指标或文献来源。
- 未硬编码 DeepSeek API Key。
- 旧 `CarbonChat.vue` 已审计，但存在旧接口和乱码问题，暂作为后续学习型 AI 体验参考。

## 12. 工单式增删改设计和实现情况

- 政府端新增“治理工单”板块。
- 支持新增、修改、删除请求。
- 支持表类型：来源、红树林面积记录、区域指标、文献证据。
- 支持填写证据来源、变更原因、变更内容、备注。
- 状态包括：草稿、待审核、已退回、已批准、已应用。
- 当前使用前端 `localStorage` 模拟，键名为 `hnblue_governance_work_orders`。
- 表格中的“发起工单”会带入当前记录内容，不直接修改真实数据。
- 后端如确认存在 `t_data_governance_task`，下一轮可补 `/api/v2/governance/work-orders` 持久化接口。

## 13. 按钮、表格、搜索框和详情交互优化情况

- 查询、重置、刷新、导出、详情、复制来源、新建、提交、批准、退回、应用已统一为 `ActionButton`。
- 搜索框统一高度、圆角、标签字号和间距。
- 表格支持横向滚动，操作列固定宽度。
- 详情抽屉统一字段展示。
- 来源徽章统一截断，避免长代码撑破布局。

## 14. 动态视觉和视差滚动实现说明

- 首页保留轻量视差、漂浮数据点、流动曲线。
- 政府端主题切换有柔和过渡。
- 卡片 hover 轻微上浮和阴影。
- 地图区域 hover 高亮。
- 碳溯流程节点与地图使用轻量动画。
- 全局动效遵守 `prefers-reduced-motion`。

## 15. 已移除或弱化的技术提示

- 主页面不再显眼展示 Proxy 技术解释。
- 政府端不再显眼展示 `unified source data source database` 一类原始字段说明。
- `boundary_notes` 等专业边界保留到文档或详情，不作为 Demo 主视觉。
- 0 条增强资产不再大面积展示。

## 16. 已修复问题

- 首页视觉缺失：已用 CSS 生态数字面板恢复主视觉、入口卡片和模块导航。
- 关于页视觉单薄：已恢复系统介绍、定位、技术结构和边界说明卡片。
- 碳溯视觉缺失：已恢复模型流程、估算示例图、解释卡片和地图探索入口。
- 数据资产空白区：已修复面积和指标图表容器，新增数据集库。
- 政府端原始按钮：已替换为统一按钮。
- 政府端表格按钮竖排：已改为统一表格操作列横排。
- 政府端深浅割裂：已完成浅/深主题体系。
- AI 上下文定位不清：已改为 AI 助手板块。
- 政府端直接编辑风险：已改为工单审批模拟。

## 17. 修改文件列表

- `frontend/src/App.vue`
- `frontend/src/router/index.js`
- `frontend/src/components/HomePage.vue`
- `frontend/src/components/AboutPage.vue`
- `frontend/src/components/CarbonSeek.vue`
- `frontend/src/components/PublicDataPage.vue`
- `frontend/src/components/V2GovernmentPage.vue`
- `frontend/src/components/HainanBlueCarbonMap.vue`
- `frontend/src/components/MapExplorePage.vue`
- `frontend/src/components/common/hnblue-ui.css`
- `frontend/src/components/common/UnifiedNav.vue`
- `frontend/src/components/common/PageShell.vue`
- `frontend/src/components/common/PageHero.vue`
- `frontend/src/components/common/SectionHeader.vue`
- `frontend/src/components/common/SearchPanel.vue`
- `frontend/src/components/common/StatCard.vue`
- `frontend/src/components/common/EmptyState.vue`
- `frontend/src/components/common/SourceBadge.vue`
- `frontend/src/components/common/RecordPreview.vue`
- `frontend/src/components/common/DetailDrawer.vue`
- `frontend/src/components/common/ThemeToggle.vue`
- `frontend/src/components/common/ActionButton.vue`
- `frontend/src/components/common/DataTable.vue`
- `docs/context/10_HNBLUE_V2_本轮交付README.md`

说明：运行 `npm run build` 后 `frontend/dist` 已刷新。

## 18. 静态检查和构建结果

已完成：

- `node --check frontend/src/router/index.js`：通过。
- Vue SFC 解析检查：通过。
- `npm run build`：通过。

构建警告：

- vendor/部分 chunk 超过 Vue CLI 推荐体积阈值。
- Browserslist/caniuse-lite 数据较旧。

这些是体积和依赖元数据警告，不是本轮代码编译错误。

## 19. 用户本机验证清单

请运行前端和后端后重点检查：

1. `/` 首页：导航、生态面板、四个入口卡片是否完整。
2. `/about`：介绍卡片、技术结构、数据说明是否显示。
3. `/carbonseek`：模型流程、估算示例图、探索图是否显示。
4. `/visual`：蓝碳地图页是否可打开，区域 hover 是否更新右侧详情。
5. `/v2-public-data`、`/data-assets`、`/public-data-2`：是否都进入数据资产页；面积图、指标图、数据集库、来源追溯是否无空白。
6. `/v2-government`：浅/深主题切换是否生效且刷新后保留。
7. 政府端指标库、文献证据、来源追溯：表格按钮是否横排，source_code 是否截断，hover 是否更新右侧预览。
8. 政府端详情：点击详情是否打开抽屉，关闭后右侧预览是否保留。
9. 政府端 AI 助手：输入区域/主题后是否能调用 `/api/v2/ai/context`，是否显示引用来源。
10. 政府端治理工单：从表格发起工单、新建工单、提交、批准、退回、应用状态是否可操作。

请反馈：页面截图、浏览器 Console 报错、Network 失败请求 URL、接口返回 JSON。重点观察关键词：`Failed to fetch`、`404`、`500`、`Cannot read properties`、`ResizeObserver`、`Loading chunk failed`、`echarts`、`localStorage`、`/api/v2/ai/context`。

## 20. 下一轮建议任务

1. 将 V1 `DeVisualPage.vue` 深度图表迁入 V2 地图详情抽屉，并改用 V2 真实表或明确标注演示数据。
2. 后端确认 `t_data_governance_task` 后，实现工单持久化接口。
3. 整理旧 AI 乱码组件，区分政府端研判 AI 和碳溯学习 AI。
4. 为数据资产中心增加统一详情抽屉和字段说明页。
5. 分析构建体积，优化 vendor chunk。
6. 补齐真实海南 GeoJSON 或静态地图资源，替换当前地图壳层。
7. 对移动端进行截图级视觉校验。

## 21. 2026-07-03 运行态验收修复补充

本轮根据用户运行观察继续处理硬性验收项，重点不是新增后端能力，而是修复入口缺失、主题可见性、旧接口失败暴露、地图降级方案和按钮观感。

### 用户反馈逐条处理结果

- 首页缺少主题切换入口：已在 `UnifiedNav.vue` 右侧增加全站“浅色/深色”切换按钮，主题写入 `localStorage`，键名为 `hnblue_site_theme`。
- 政府端主题切换不够清晰：政府端保留页面内更正式的主题切换卡片；统一导航在政府端关闭重复主题按钮，避免两个开关混淆。
- 登录按钮消失：已在统一导航右侧恢复“登录”入口，路径为 `/login`，旧 `Login.vue` 继续保留。
- 首页标题过大：已将首页标题字号从 `clamp(42px, 6.2vw, 72px)` 压缩为 `clamp(38px, 4.6vw, 58px)`，降低 1440px 下换行风险。
- 首页右侧低保真树形插画：已移除几棵树/椭圆式视觉，改为 CSS/SVG 生态数字面板，包含海南轮廓式数字面、扫描网格和数据流线。
- 按钮字母前缀：`ActionButton.vue` 不再渲染 `icon` 字母；`V2GovernmentPage.vue`、`SearchPanel.vue`、`DataTable.vue` 中的 `icon="q/r/i/c/t"` 等属性已清理。
- 增强数据主标签：政府端主标签栏已移除“增强数据”，该区块保留为降级说明，不再作为主流程入口。
- AI 助手异常弹层：政府端 AI 助手使用普通 `input` 搜索面板，无 autocomplete/popover；本轮保留结构化上下文检索和“生成服务待接入”解释路径。
- 8880 接口失败：`LiteratureCompare.vue` 不再在页面加载时自动请求 `http://localhost:8880/api/literature-range`；用户点击“检查服务”时才尝试连接，未启动时显示“模型服务未启动”空状态。
- 碳储价值页乱码与失败观感：`CarbonValueConverter.vue` 已重写为可读的教学型换算页，不依赖 8880，也不暴露红色接口失败。
- 结构参数、虚拟样地空白入口：当前路由仍进入统一 `PlaceholderPage`，明确显示待接入说明，不再是空白页。
- `/devisual/:name` 旧链接兼容：新增兼容路由，带区域名的旧链接也进入新版地图降级页。

### V1 地图和 hainan.json 审计补充

已再次搜索地图资产与旧代码：

- `frontend/src/components/DeVisualPage.vue` 仍导入 `@/assets/json/hainan.json`。
- `frontend/src/assets/json/hainan.json` 当前缺失。
- 全项目未找到可直接替代的海南 GeoJSON 文件。
- 后端仍有 V1 可视化相关实体：`visual_indicator_data`、`region_summary_info`、`region_yearly_carbon`、`region_yearly_trend` 等。
- 文档中也记录过该缺失问题，说明这是旧地图恢复的真实阻断点。

本轮处理方式：

- 不再用椭圆冒充海南地图。
- `HainanBlueCarbonMap.vue` 已改为“省域轮廓 + 区域卡片 + V1/V2 详情位”的降级方案。
- `/visual` 和 `/devisual` 可用，`/devisual/:name` 兼容旧详情路径。
- 地图详情中保留区域摘要、面积记录、区域指标、文献碳储、V1 年度趋势、多碳指标、物种/通量组成等承接位。
- 缺失真实数据时显示“待验证 / 待接入 / 暂无完整记录”，不编造数值。

需要用户后续补齐的地图文件路径建议：

- 优先补齐：`frontend/src/assets/json/hainan.json`
- 格式建议：海南省 GeoJSON，地市名称字段需要能映射到海口、文昌、儋州、万宁、东方、三亚等区域名。
- 补齐后下一轮可恢复 ECharts `registerMap` 和区域点击。

### 旧 8880 Python 接口审计结果

仍存在的 8880 来源：

- `flask_model/carbon_model_api/app.py`：定义 `/api/literature-range`，端口 8880。
- `flask_model/carbon_model_api/predict_service.py`：生成 `http://localhost:8880/sensitivity_plots/...`。
- `frontend/src/components/LiteratureCompare.vue`：保留 `SERVICE_URL` 常量，但不再自动请求，改为用户点击检查服务。

本轮降级策略：

- 不启动 Python 服务时，页面显示清晰空状态。
- 不再使用 Element Plus 红色消息弹出失败。
- 不再在用户进入页面时直接暴露 `net::ERR_CONNECTION_REFUSED`。
- 若后续要完全 V2 化，建议新增 Spring Boot `/api/v2/literature-carbon/range` 或静态区间 JSON。

### 本轮新增/修改文件

- `frontend/src/components/common/UnifiedNav.vue`
- `frontend/src/components/common/ActionButton.vue`
- `frontend/src/components/common/hnblue-ui.css`
- `frontend/src/components/HomePage.vue`
- `frontend/src/components/HainanBlueCarbonMap.vue`
- `frontend/src/components/CarbonSeek.vue`
- `frontend/src/components/LiteratureCompare.vue`
- `frontend/src/components/CarbonValueConverter.vue`
- `frontend/src/components/V2GovernmentPage.vue`
- `frontend/src/components/common/SearchPanel.vue`
- `frontend/src/components/common/DataTable.vue`
- `frontend/src/router/index.js`
- `docs/context/10_HNBLUE_V2_本轮交付README.md`

### 本轮仍需用户运行验证

请重点打开：

1. `/`：确认标题是否一行、右侧视觉是否不再像低保真插画、导航是否有“浅色/深色”和“登录”。
2. `/login`：确认登录页可进入。
3. `/carbonseek`：确认模型解释、地图区和模块入口不空白。
4. `/visual`、`/devisual`、`/devisual/文昌`：确认不再是空壳，地图降级页和区域详情位可见。
5. `/carbon-value-converter`：确认碳储价值页中文正常、无红色接口失败。
6. `/literature-compare`：确认不自动报 8880 连接失败；点击“检查服务”后若 Python 未启动，应显示温和空状态。
7. `/v2-government`：确认主题切换可见、按钮无字母前缀、无“增强数据”主标签、表格按钮横向不挤压。

### 下一轮剩余任务

- 补齐真实 `hainan.json` 后恢复 ECharts 海南地图注册和地市点击。
- 将 V1 `DeVisualPage.vue` 中的年度碳储、通量组成、物种组成、多碳指标迁入新版详情抽屉。
- 将 `LiteratureCompare.vue` 的 8880 区间改接 Spring Boot V2 API 或静态 normalized 数据。
- 整理旧 `CarbonChat.vue` 的乱码和 SSE 体验，再决定是否作为学习型 AI 正式入口。
- 对首页和地图页做浏览器截图级检查，继续微调 1440px 和移动端布局。

## 22. 2026-07-03 真实地图与全系统集成审计补充

本节覆盖前文“缺少 hainan.json”的旧状态：用户已提供真实海南 GeoJSON，本轮已复制并接入新版地图，不再使用省域椭圆或假地图降级方案。

### 22.1 已接入的真实素材

- 已复制 `D:/Program Files/前端/Vue/HainanBlueCarbon/src/assets/json/hainan.json` 到 `frontend/src/assets/geo/hainan.json`。
- 同步复制到 `frontend/src/assets/json/hainan.json`，用于兼容 V1 `DeVisualPage.vue` 的旧导入路径。
- 已复制用户提供的红树林实景图到 `frontend/src/assets/hero-mangrove.webp`。
- GeoJSON 静态审计结果：`FeatureCollection`，包含 19 个地市/区域要素，属性中包含 `name`、`fullname`、`center`、`centroid`、`code` 等字段。

### 22.2 首页真实视觉修复

- `HomePage.vue` 已从 CSS 合成面板升级为“真实红树林图片 + 数据覆盖层”。
- 暗色主题下首页主标题颜色改为跟随 `var(--hn-text)`，修复标题半浅半深、局部不可读的问题。
- 首页右侧保留轻量数据浮层，展示区域指标、文献证据和面积记录入口感，不引入大型新依赖。

### 22.3 海南地图接入情况

- `HainanBlueCarbonMap.vue` 已改为 ECharts 真实 GeoJSON 地图，使用 `echarts.registerMap('hainan-blue', hainanGeo)`。
- 地图支持 hover 高亮、缩放漫游、区域点击和右侧详情更新。
- `/visual`、`/devisual`、`/devisual/:name`、`/carbonseek` 均可复用新版地图能力。
- 已保留区域摘要、面积记录、区域指标、文献碳储和趋势展示位；缺失真实区域数据时显示“待验证 / 待接入”，不编造数值。
- V1 `DeVisualPage.vue` 的深度图表仍建议下一轮迁入新版详情抽屉，但其旧 `@/assets/json/hainan.json` 导入路径已经补齐。

### 22.4 Flask CatBoost 模型服务审计

已审计 `flask_model/carbon_model_api`：

- `app.py` 提供 Flask 服务，默认端口 `8880`。
- 主要接口包括 `/api/predict-carbon`、`/api/predict-carbon/batch`、`/api/literature-range`、`/api/sensitivity-analysis`、`/api/sensitivity-plot`、`/api/sensitivity-explain`、`/ping`。
- `predict_service.py` 读取 `catboost_pipeline.pkl`，提供单条预测、批量预测、敏感性分析、响应曲线图片和解释文本。
- `requirements.txt` 包含 Flask、CatBoost、pandas、numpy、scikit-learn、matplotlib、joblib 等模型服务依赖。
- `General_Modeling.py` 仍保留 CatBoost 训练与模型导出逻辑。
- 前端 `LiteratureCompare.vue` 仍保留 8880 检查入口，但不会在页面加载时自动请求，未启动时显示温和空状态。

建议定位：Flask 服务作为“碳溯模型解释 / 案例推演”的可选模型层，不作为政府端主流程强依赖。

### 22.5 Redis、AnythingLLM 与 DeepSeek 审计

- `application.properties` 当前 datasource 已指向 `hnblue_v2_dev_control`，符合 V2 单库运行要求。
- Redis 配置仍存在于 `application.properties`，并被登录、限流、缓存工具等后端模块使用；若用户验证登录或限流流程，需先确保 Redis 集群可用。
- `CarbonAssistantController.java` 提供 `/api/chat/stream-carbon`，通过后端配置读取 AnythingLLM URL、workspace 和 API key，并以 SSE 方式转发回答。
- `SeekController.java` 提供 `/api/chat/stream`，DeepSeek API key 通过 `api.password` 配置项读取，未写入前端。
- 当前 `application.properties` 中存在明文 AnythingLLM key 风险，建议下一轮改为环境变量或本机私有配置文件，不应提交真实密钥。
- V2 政府端 AI 助手当前优先调用 `/api/v2/ai/context` 做结构化上下文检索和引用展示；真实生成式回答可后续接入 AnythingLLM 或 DeepSeek 流式接口。

### 22.6 推荐本机启动顺序

仅供用户本机联调使用，本轮 Codex 未启动 Maven、Spring Boot、Flask 或 Redis：

1. MySQL：确认 `hnblue_v2_dev_control` 可访问，后端配置仍指向该库。
2. Redis：如验证登录、限流或旧缓存能力，先启动配置中的 Redis 集群或改成本机可用配置。
3. AnythingLLM：如验证 `/api/chat/stream-carbon`，确保 AnythingLLM 在 `localhost:3001` 可用，并将 key 外置到安全配置。
4. Spring Boot：启动后验证 `/api/v2/*` 与 `/api/v2/ai/context`。
5. Flask 模型服务：如验证 CatBoost、文献区间、敏感性分析，从 `flask_model/carbon_model_api` 安装依赖后启动 8880。
6. Vue 前端：启动前端后依次验证 `/`、`/carbonseek`、`/visual`、`/v2-public-data`、`/v2-government`。

### 22.7 数据资产可读性补充修复

- `PublicDataPage.vue` 已加强数据集卡、文献卡、来源卡的标题和正文对比度。
- 深色主题下卡片背景与正文颜色增加适配，避免卡片文字发灰、来源信息难读。
- 该修复不改变数据加载逻辑，仅提升展示清晰度。

### 22.8 最新构建结果

本轮再次运行 `npm run build`：通过。

仍有警告：

- `chunk-vendors` 与部分懒加载 chunk 超过推荐体积阈值。
- Browserslist/caniuse-lite 数据较旧。

这些是体积和依赖元数据警告，不是编译失败。下一轮可做 ECharts/Element Plus 分包和依赖元数据更新。

### 22.9 用户本机新增验证项

请重点反馈：

1. `/` 首页暗色主题下标题是否完整可读，右侧是否显示真实红树林图片。
2. `/visual` 和 `/carbonseek` 中海南地图是否是真实海南形状，hover/click 是否更新详情。
3. `/devisual/:name` 旧链接是否不再 404。
4. `/v2-public-data` 数据资产卡片在浅色/深色主题下是否文字清楚。
5. 若启动 Flask 8880，请验证文献区间和敏感性接口；若未启动，应只出现空状态，不应整页报错。
6. 若启动 AnythingLLM，请验证 `/api/chat/stream-carbon`；若未启动，政府端 AI 助手仍应能显示 `/api/v2/ai/context` 引用上下文。

## 23. 2026-07-06 全站主题统一与政府端深浅适配修复

### 23.1 本轮修复目标

本轮不新增业务功能，重点修复运行态主题体系冲突：政府端不再维护独立主题，所有页面统一服从全站导航右侧的主题切换，避免出现“上方深色、下方浅色”或“政府端内部主题与全站主题互相打架”的半主题状态。

本轮未运行 Maven，未启动 Spring Boot，未启动 Flask 或其他后端服务。已运行前端 `npm run build`。

### 23.2 主题冲突原因

旧实现中存在两套主题状态：

- 全站导航使用 `hnblue_site_theme`。
- 政府端页面内部使用 `hnblue_government_theme`，并在页面内另放“当前主题 / 浅色办公 / 深色研判”卡片。

当用户在其他页面切换全站主题后，政府端又读取自己的独立主题，并通过 `.gov-page.dark` 覆盖局部变量，导致导航、页面壳、Hero、内容卡片、表格和抽屉不一定使用同一套颜色变量。

### 23.3 删除政府端独立主题逻辑

已处理：

- 删除 `V2GovernmentPage.vue` 中的 `ThemeToggle` 引入和页面内主题卡片。
- 删除 `govTheme` 状态和对 `hnblue_government_theme` 的读写。
- 政府端 `PageShell`、`UnifiedNav`、`PageHero`、内容面板全部改为使用全站 CSS 变量。
- 全站主题按钮只保留在 `UnifiedNav.vue` 右侧，位置在“登录”左侧。
- 前端代码不再使用 `hnblue_government_theme`。如浏览器中仍残留旧 localStorage 键，可手动清理；当前代码会忽略该键。

### 23.4 全站主题状态键名

当前唯一主题状态键名：

- `hnblue_site_theme`

`UnifiedNav.vue` 会把主题同步到：

- `document.documentElement.dataset.hnTheme`
- `theme-light` / `theme-dark` 根类

公共组件通过 `hnblue-ui.css` 中的变量响应主题，包括：

- `--hn-bg`
- `--hn-bg-soft`
- `--hn-surface`
- `--hn-surface-strong`
- `--hn-card`
- `--hn-control-bg`
- `--hn-nav-bg`
- `--hn-table-hover`
- `--hn-text`
- `--hn-muted`
- `--hn-border`
- `--hn-accent`

### 23.5 浅色主题下政府端表现

浅色模式下，政府端整体为浅色办公风格：

- 页面背景使用浅色生态底色。
- Hero 不再强制深色分段，避免上下割裂。
- 驾驶舱、一张图、指标库、文献证据、来源追溯、AI 助手、治理工单均使用浅色卡片体系。
- 搜索框、表格、预览区、详情抽屉使用白色或浅色变量底色。
- 按钮保持蓝绿色强调色。

### 23.6 深色主题下政府端表现

深色模式下，政府端整体服从全站深色变量：

- 页面背景、导航、面板、卡片、表格、输入框、预览区、详情抽屉全部为协调深色体系。
- 标签栏使用深色胶囊栏，不再出现白色大卡片。
- `SearchPanel`、`DataTable`、`RecordPreview`、`DetailDrawer`、`ActionButton` 已改为变量化背景和文字色。
- 文本使用 `--hn-text` / `--hn-muted`，避免白底白字或浅绿过淡。
- hover 状态使用 `--hn-table-hover` 等主题变量。

### 23.7 首页真实红树林图片融合优化

`HomePage.vue` 保留真实红树林图片，并补充：

- 柔和扩散阴影和背景晕染。
- 图片遮罩在浅色主题下更自然，在深色主题下加深。
- 浮层数据卡片降低不透明度并增加轻微模糊。
- 图片高度改为稳定响应式区间，避免压迫左侧标题。

### 23.8 已检查的政府端模块

已检查并适配：

- 驾驶舱
- 一张图
- 指标库
- 文献证据
- 来源追溯
- AI 助手
- 治理工单
- 详情抽屉
- 右侧预览区
- 搜索筛选区
- 标签栏
- 表格操作列

同时再次搜索按钮残留，未发现 `icon="q"`、`icon="r"`、`icon="i"`、`icon="c"`、`icon="t"`、`q 查询`、`r 重置`。

### 23.9 数据资产深色可读性回归

已同步检查数据资产中心：

- 数据集卡、文献卡、来源卡改用 `--hn-card`。
- 正文、来源说明、指标表格改用 `--hn-muted` / `--hn-text`。
- `SourceBadge.vue` 改为 `--hn-soft` + `--hn-accent`，深色下不再是固定浅色徽章。
- 参考标签不再使用固定浅黄白底。

### 23.10 海南真实地图确认

未回退地图能力：

- `frontend/src/assets/geo/hainan.json` 存在。
- `frontend/src/assets/json/hainan.json` 兼容路径存在。
- `HainanBlueCarbonMap.vue` 继续导入 `@/assets/geo/hainan.json`。
- `HainanBlueCarbonMap.vue` 继续使用 `echarts.registerMap('hainan-blue', hainanGeo)` 和 `type: 'map'`。
- `/visual`、`/devisual`、`/devisual/:name`、`/carbonseek` 地图入口继续保留。

### 23.11 本轮修改文件

- `frontend/src/components/common/UnifiedNav.vue`
- `frontend/src/components/common/hnblue-ui.css`
- `frontend/src/components/common/PageShell.vue`
- `frontend/src/components/common/SearchPanel.vue`
- `frontend/src/components/common/DataTable.vue`
- `frontend/src/components/common/RecordPreview.vue`
- `frontend/src/components/common/DetailDrawer.vue`
- `frontend/src/components/common/ActionButton.vue`
- `frontend/src/components/common/SourceBadge.vue`
- `frontend/src/components/V2GovernmentPage.vue`
- `frontend/src/components/HomePage.vue`
- `frontend/src/components/PublicDataPage.vue`
- `frontend/src/App.vue`
- `docs/context/10_HNBLUE_V2_本轮交付README.md`

### 23.12 构建结果

已运行：`npm run build`

结果：构建通过。

仍有两类警告：

- `chunk-vendors` 和部分懒加载 chunk 超过 Vue CLI 推荐体积阈值。
- Browserslist/caniuse-lite 数据较旧。

这些是体积和依赖元数据警告，不是本轮主题代码编译错误。

### 23.13 用户本机验证清单

请本机重点验证：

1. 清理或忽略浏览器旧 localStorage 中的 `hnblue_government_theme`，确认系统只依赖 `hnblue_site_theme`。
2. 在首页切换“浅色/深色”，进入 `/v2-government`，确认政府端没有单独主题卡片。
3. 浅色主题下 `/v2-government`：背景、Hero、标签栏、卡片、表格、搜索框、预览区、抽屉是否都是浅色办公风格。
4. 深色主题下 `/v2-government`：驾驶舱、一张图、指标库、文献证据、来源追溯、AI 助手、治理工单是否没有白色大组件。
5. 深色主题下点击详情抽屉，确认抽屉、关闭按钮、字段文字可读。
6. 深色主题下 hover 表格行，确认预览区与表格 hover 不刺眼、不白底。
7. `/` 首页确认红树林图片更像嵌入背景，浮层卡片不过分抢眼。
8. `/v2-public-data` 确认来源追溯、数据集卡、source_code 徽章在浅色/深色下都清楚。
9. `/visual`、`/devisual`、`/devisual/文昌`、`/carbonseek` 确认仍是真实海南地图。


## 24. 2026-07-06 V1 登录与 AI 碳助手迁移修复

### 24.1 本轮修复目标

本轮聚焦 V1.0 旧资产迁移与运行态可信修复，不新增后端功能，不启动 Maven、Spring Boot 或 Flask。目标是：

- 将旧登录业务逻辑迁入 V2.0 视觉体系。
- 删除首页红树林主视觉上的扫描线/扫描类效果。
- 恢复 V1.0 AI 碳助手的右下角全局悬浮入口。
- 政府端不再保留意义不清的 AI 主标签，统一使用全局 AI 碳助手。
- 保持全站统一主题键 `hnblue_site_theme`。

### 24.2 搜索关键词和路径证据

登录与用户态搜索关键词：

- `Login.vue`
- `login`
- `user`
- `admin`
- `administrator_user_relation`
- `auth`
- `token`
- `register`
- `logout`
- `personal`
- `个人中心`

搜索路径：`frontend`、`backend`、`docs`，排除 `frontend/node_modules`、`frontend/dist`、`backend/target`。

找到的登录/用户态资产：

- `frontend/src/components/Login.vue`：V1.0 登录、注册、忘记密码页面，存在乱码和 V1 孤立样式，但保留了业务接口路径。
- `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java`：后端登录、注册、密码重置和重置状态检查接口。
- `backend/src/main/java/com/example/jpaspringboot/controller/UserController.java`：当前用户信息、头像、用户资料更新接口，依赖 Bearer Token。
- `backend/src/main/java/com/example/jpaspringboot/controller/AdminController.java`：管理员用户管理接口，依赖 Authorization Token。
- `backend/src/main/java/com/example/jpaspringboot/dto/UserInfoDTO.java`：用户信息 DTO。
- `docs/API.md`、`docs/TEST_REPORT.md`、`docs/redis_rate_limit_test.md`：记录 `/api/login`、`/api/register`、Authorization Bearer Token、登录限流等说明。
- `backend/src/test/java/com/example/jpaspringboot/controller/LoginControllerTest.java`：注册和登录测试证据。

未找到：

- 未找到独立的 `Register.vue`。
- 未找到独立的 `Personal.vue` 或 `UserInfo.vue` 前端完整页面；当前 `/userinfo` 仍走路由占位页。
- 未找到前端专门的 `auth` store；旧登录态主要保存在 `localStorage`。

AI 碳助手搜索关键词：

- `CarbonChat.vue`
- `Ai`
- `AI`
- `assistant`
- `chat`
- `CarbonAssistant`
- `stream-carbon`
- `EventSource`
- `SSE`
- `DeepSeek`
- `AnythingLLM`
- `robot`
- `floating`
- `右下角`
- `chatbot`

搜索路径：`frontend`、`backend`、`docs`，排除依赖和构建目录。

找到的 AI 资产：

- `frontend/src/components/CarbonChat.vue`：旧蓝碳智能助手组件，核心逻辑为 `EventSource(`${props.apiUrl}/stream-carbon?message=...`)`。
- `frontend/src/components/Chat.vue`：旧通用聊天组件，核心逻辑为 `EventSource(`${props.apiUrl}/chat/stream?message=...`)`。
- `frontend/src/components/CarbonAIAssistant.vue`：旧 AI 说明页，不是右下角全局入口。
- `backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java`：`/api/chat/stream-carbon`，对接 AnythingLLM 工作区流式问答。
- `backend/src/main/java/com/example/jpaspringboot/controller/SeekController.java`：`/api/chat/stream`，DeepSeek SSE 流式问答。
- `docs/v2/HNBLUE_frontend_entrypoint_analysis.md`：记录 `CarbonChat.vue` 与 `/api/chat/stream-carbon` 的对应关系。
- `docs/v2/HNBLUE_project_structure_diagnosis.md`：记录 `/api/chat/stream`、`/api/chat/stream-carbon` 的后端控制器。

未找到：

- 未找到可直接复用的 V1 右下角悬浮外壳组件；旧资产里找到了聊天面板和 SSE 逻辑，但未找到完整 `floating/chatbot/robot` 命名的全局悬浮组件。

### 24.3 登录页 V1 逻辑审计结果

旧 `Login.vue` 保留了以下业务逻辑：

- 登录：`POST http://localhost:8088/api/login`
- 注册：`POST http://localhost:8088/api/register`
- 发起密码重置：`POST http://localhost:8088/api/initiateReset`
- 轮询重置状态：`GET http://localhost:8088/api/checkResetStatus?username=...`
- 登录成功后保存：`localStorage.setItem('token', ...)`、`localStorage.setItem('userType', ...)`
- 请求拦截：为后续请求附加 `Authorization: Bearer <token>`

旧模板和注释存在中文乱码，且旧页面自行设置 `document.body.style` 背景、导航和布局，和 V2.0 主题体系割裂。

### 24.4 登录页 V2 UI 改造结果

修改文件：`frontend/src/components/Login.vue`

修改位置：

- `<template>`：重写为 `PageShell + UnifiedNav + V2 登录卡片`。
- `<script setup>`：保留 V1 旧接口路径、`token/userType` 存储、注册、忘记密码和轮询逻辑。
- `<style scoped>`：改为使用 `--hn-bg`、`--hn-card`、`--hn-text`、`--hn-muted`、`--hn-accent`、`--hn-control-bg` 等全站主题变量。

保留的旧逻辑：

- 登录、注册、忘记密码和重置状态轮询接口未改为假逻辑。
- 登录态仍保存到 `localStorage` 的 `token` 和 `userType`。
- 后端未启动时显示温和错误提示，不白屏。

验证方式：

- 构建验证：`npm run build` 通过。
- 用户运行验证：打开 `/login`，分别切换浅色/深色主题，检查登录、注册、忘记密码表单是否完整。

### 24.5 首页扫描效果删除结果

修改文件：`frontend/src/components/HomePage.vue`

处理结果：

- 删除主视觉 DOM 中的 `<div class="scan-line"></div>`。
- 删除 `.scan-line` 样式。
- 删除 `@keyframes scan` 扫描动画残留。
- 保留真实红树林图片 `frontend/src/assets/hero-mangrove.webp`。
- 保留柔和阴影、背景晕染、渐变遮罩和数据浮层。

验证方式：

- 搜索 `scan-line`、`@keyframes scan`、`95px`、`扫描`，当前首页组件无残留。
- 构建验证：首次构建发现 `@keyframes scan` 残段导致 CSS 语法错误，已修复；第二次 `npm run build` 通过。

用户运行检查：打开 `/`，确认红树林图上不再出现横向扫描线、扫描网格或雷达式动画。

### 24.6 AI 碳助手 V1 资产审计结果

V1 可迁移逻辑来自：

- `frontend/src/components/CarbonChat.vue`
  - 旧蓝碳助手面板。
  - 使用 `EventSource`。
  - 调用 `${props.apiUrl}/stream-carbon?message=...`。
  - 支持流式追加回答、关闭连接和快捷问题。
- `frontend/src/components/Chat.vue`
  - 旧通用助手面板。
  - 使用 `EventSource`。
  - 调用 `${props.apiUrl}/chat/stream?message=...`。
- `backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java`
  - 后端路径 `/api/chat/stream-carbon`。
  - 通过后端配置读取 AnythingLLM URL、workspace 和 API key。
- `backend/src/main/java/com/example/jpaspringboot/controller/SeekController.java`
  - 后端路径 `/api/chat/stream`。
  - 通过配置读取 DeepSeek key。

本轮未声称找到完整旧右下角悬浮外壳；实际找到的是旧聊天面板和 SSE 业务逻辑，因此本轮新增 V2 全局悬浮外壳，并复用旧接口能力。

### 24.7 全局悬浮 AI 碳助手实现结果

新增文件：`frontend/src/components/CarbonAiFloatingAssistant.vue`

挂载文件：`frontend/src/App.vue`

实现内容：

- 所有主页面通过 `App.vue` 全局挂载右下角 `AI 碳助手` 悬浮按钮。
- 点击悬浮按钮打开聊天面板。
- 面板支持关闭、最小化、移动端宽度适配。
- 面板跟随全站浅色/深色主题变量。
- 发送消息时使用旧蓝碳助手接口：`/api/chat/stream-carbon?message=...`。
- 使用 `EventSource` 流式接收后端回答。
- 兼容旧 `CarbonChat.vue` 的嵌套 JSON 文本解析方式。
- 后端或 AnythingLLM 未启动时，显示“AI 服务未启动 / 连接中断：当前仅影响探助手，不影响页面浏览。”，不红屏刷错。

碳溯入口：

- 修改 `frontend/src/components/CarbonSeek.vue`。
- 在“模型解释”区块增加“打开 AI 碳助手”按钮。
- 通过 `window.dispatchEvent(new CustomEvent('hnblue-open-ai'))` 打开全局悬浮面板。

验证方式：

- 搜索 `GlobalAIAssistant`：确认已在 `App.vue` 挂载。
- 搜索 `stream-carbon` 和 `EventSource`：确认新组件调用旧 SSE 接口。
- 构建验证：`npm run build` 通过。

### 24.8 政府端 AI 主标签处理结果

修改文件：`frontend/src/components/V2GovernmentPage.vue`

处理结果：

- 从 `tabs` 中移除 `{ key: "assistant", label: "AI 助手" }`。
- 页面不再出现“AI 助手”主标签。
- 驾驶舱说明卡增加“AI 碳助手”说明，提示问答入口已统一为右下角全局悬浮组件。
- 保留政府端数据治理、来源追溯、工单审批等主流程。
- 未删除后端 `/api/v2/ai/context` 或历史函数，避免破坏后续可复用上下文检索能力；但它不再作为政府端主标签入口。

用户运行检查：打开 `/v2-government`，确认标签栏只显示驾驶舱、一张图、指标库、文献证据、来源追溯、治理工单。

### 24.9 登录态提示处理

修改文件：`frontend/src/components/V2GovernmentPage.vue`

处理结果：

- 增加 `isLoggedIn = Boolean(localStorage.getItem("token"))`。
- 未登录进入政府端时显示统一提示：“当前未检测到登录态。可继续浏览公开数据；如需进行治理工单审批，请先登录。”
- 登录页登录成功后触发 `hnblue-auth-changed` 事件，政府端可更新提示状态。

说明：本轮未新增路由守卫，避免影响公开浏览和演示路径。工单真实鉴权仍需后端运行时验证。

### 24.10 主题统一检查结果

- 当前全站主题键仍为 `hnblue_site_theme`。
- 本轮未恢复 `hnblue_government_theme`。
- `Login.vue`、`CarbonAiFloatingAssistant.vue`、`V2GovernmentPage.vue` 均使用 `hnblue-ui.css` 变量。
- 搜索前端源码未发现 `hnblue_government_theme`、`govTheme`、政府端 `ThemeToggle` 残留。

### 24.11 本轮修改文件列表

新增：

- `frontend/src/components/CarbonAiFloatingAssistant.vue`

修改：

- `frontend/src/components/Login.vue`
- `frontend/src/App.vue`
- `frontend/src/components/HomePage.vue`
- `frontend/src/components/CarbonSeek.vue`
- `frontend/src/components/V2GovernmentPage.vue`
- `docs/context/10_HNBLUE_V2_本轮交付README.md`

构建输出刷新：

- `frontend/dist/`

### 24.12 构建结果

执行命令：`npm run build`

结果：第二次构建通过。

过程说明：

- 第一次构建失败，原因是首页删除扫描动画时残留了半段 `@keyframes scan` CSS。
- 已清理残段后再次构建，通过。

仍有警告：

- vendor chunk 和部分懒加载 chunk 超过 Vue CLI 推荐体积阈值。
- Browserslist/caniuse-lite 数据较旧。

这些是体积和依赖元数据警告，不是编译错误。

### 24.13 用户本机验证清单

1. `/login`：浅色和深色主题下，确认登录页有统一导航、HNBLUE 标识、主题切换、登录/注册/忘记密码表单。
2. `/login`：后端未启动时，点击登录/注册/重置应显示温和错误提示，不白屏。
3. `/login`：后端启动后，验证 `/api/login` 成功后是否写入 `localStorage.token` 和 `localStorage.userType`。
4. `/`：首页红树林图片应无扫描线、扫描网格、雷达线；仅保留图片、遮罩、柔和阴影和数据浮层。
5. 任意主页面：右下角应出现 `AI 碳助手` 悬浮入口。
6. 点击 AI 图标：聊天面板应打开，可关闭、最小化。
7. 后端未启动时：发送问题后应显示“AI 服务未启动 / 连接中断”温和状态。
8. 后端和 AnythingLLM 启动后：AI 应调用 `/api/chat/stream-carbon` 并流式显示回答。
9. `/carbonseek`：点击“打开 AI 碳助手”应打开右下角全局 AI 面板。
10. `/v2-government`：确认不再出现“AI 助手”主标签；未登录时能看到登录提示。
11. 全站切换浅色/深色：登录页和 AI 面板都应跟随主题变化。

## 25. 2026-07-06 登录认证与 AI 碳助手联调专项

### 25.1 本轮修复目标

本轮围绕用户运行态反馈做专项修复：

- `/login` 页面继续使用 V2.0 导航、主题和登录卡片，但修复登录请求失败时只有 500 的黑盒体验。
- 删除登录页“第三方登录待接入”占位，只保留登录、注册、忘记密码三条已有 V1.0 业务入口。
- 恢复右下角全局悬浮 `AI 碳助手`，复用 V1.0 蓝碳助手 SSE 接口能力。
- 审计 Spring Boot、Redis、AnythingLLM、Flask CatBoost 模型、Vue 前端之间的启动和联调关系。
- 移除配置中的硬编码 AI API Key，改为环境变量占位。

本轮没有启动 Maven、Spring Boot、Flask、Redis 或 AnythingLLM；只运行了前端 `npm run build`。

### 25.2 登录页 V1 逻辑审计结果

搜索关键词：

- `Login.vue`
- `login`
- `user`
- `admin`
- `administrator_user_relation`
- `auth`
- `token`
- `register`
- `logout`
- `personal`
- `个人中心`

搜索路径：`frontend/src`、`backend/src/main/java`、`backend/src/test`、`docs`。

找到的旧登录资产：

- `frontend/src/components/Login.vue`：V1 登录、注册、忘记密码和重置状态轮询逻辑。
- `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java`：后端 `/api/login`、`/api/register`、`/api/initiateReset`、`/api/checkResetStatus`。
- `backend/src/main/java/com/example/jpaspringboot/service/impl/UserServiceImpl.java`：普通用户注册、加盐哈希、登录校验、密码重置。
- `backend/src/main/java/com/example/jpaspringboot/service/impl/AdminServiceImpl.java`：管理员加盐哈希和登录校验。
- `backend/src/main/java/com/example/jpaspringboot/entity/User.java`：`user` 表实体，字段包括 `id`、`name`、`salt`、`passwordHash`、`email`、`birthdate`、`avatar`。
- `backend/src/main/java/com/example/jpaspringboot/entity/Admin.java`：`admin` 表实体，字段包括 `id`、`name`、`salt`、`passwordHash`。
- `backend/src/main/java/com/example/jpaspringboot/entity/AdministratorUserRelation.java` 与 `entity/ids/AdministratorUserRelationId.java`：管理员-用户关系表资产。
- `backend/src/main/java/com/example/jpaspringboot/repository/UserRepository.java`：`findByName` 查询登录用户。
- `backend/src/main/java/com/example/jpaspringboot/repository/AdminRepository.java`：`findByName` 查询管理员。

未找到：

- 未找到独立 `Register.vue` 页面；注册能力在 `Login.vue` 内以模式切换实现。
- 未找到完整 V2 个人中心页面；旧用户态主要由 token、userType 和后端用户接口支撑。

### 25.3 `/api/login` 500 定位与修复

用户观察：浏览器请求 `/api/login` 返回 500，说明请求已经到达后端或前端代理，而不是纯路由问题。

定位结果：

- `frontend/vue.config.js` 已配置 `devServer.proxy["/api"] -> http://localhost:8088`。
- 登录页原先硬写 `http://localhost:8088/api`，本轮改为相对路径 `/api`，与代理和浏览器控制台路径保持一致。
- `/api/login` 会经过 `AccessLimitInterceptor`，该拦截器依赖 Redis 读写限流计数。如果 Redis 集群未启动或地址不可达，登录请求可能在进入 `LoginController` 前就抛出 500。
- `LoginController.login` 原先没有请求体校验和异常兜底；当 `admin`、`user` 表缺失、字段不匹配、数据库连接异常或 Redis 前置异常时，前端只能看到泛化 500。

已修复文件：

- `frontend/src/components/Login.vue`
  - `API_BASE` 从 `http://localhost:8088/api` 改为 `/api`。
  - 500 时提示用户检查后端日志、MySQL 登录表和 Redis 限流服务。
  - 删除“第三方登录待接入”DOM 和相关按钮样式。
- `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java`
  - `login` 增加用户名/密码空值校验，缺字段返回 400。
  - 用户名或密码不匹配返回 401 和可读 JSON。
  - 数据库/表结构/服务异常返回带 `message` 与 `error` 的 500 JSON，便于运行态定位。
- `backend/src/main/java/com/example/jpaspringboot/interceptor/AccessLimitInterceptor.java`
  - Redis 限流异常时记录 `AccessLimit Redis unavailable` 并放行请求，避免 Redis 不可用直接导致登录 500。
  - 正常 Redis 可用时仍保留 429 限流逻辑。

仍需用户本机运行验证：

- 如果 `/api/login` 仍返回 500，请优先看 Spring Boot 后端日志中的 `error` 类型和 SQL 异常。
- 当前代码已降低 Redis 未启动导致 500 的概率，但 MySQL 表不存在、字段名不一致、账号密码哈希数据不匹配仍需运行态确认。

### 25.4 登录数据库检查 SQL

请在用户本机 MySQL 中检查 V2 运行库：

```sql
USE hnblue_v2_dev_control;
SHOW TABLES LIKE 'user';
SHOW TABLES LIKE 'admin';
SHOW TABLES LIKE 'administrator_user_relation';
DESC user;
DESC admin;
DESC administrator_user_relation;
SELECT COUNT(*) AS user_count FROM user;
SELECT COUNT(*) AS admin_count FROM admin;
SELECT id, name, email, birthdate FROM user LIMIT 5;
SELECT id, name FROM admin LIMIT 5;
SELECT id, name, salt, password_hash FROM user WHERE name = '你的登录名';
SELECT id, name, salt, password_hash FROM admin WHERE name = '你的登录名';
```

注意：JPA 实体字段名是 `passwordHash`，MySQL 默认物理命名策略常映射为 `password_hash`。如果数据库实际字段是 `passwordHash` 或其他形式，请结合 `DESC user`、`DESC admin` 和后端日志确认。

后端日志重点关键词：

- `AccessLimit Redis unavailable`
- `SQLSyntaxErrorException`
- `Table 'hnblue_v2_dev_control.user' doesn't exist`
- `Table 'hnblue_v2_dev_control.admin' doesn't exist`
- `Unknown column`
- `Communications link failure`
- `Access denied for user`
- `LoginController`
- `authenticateAdmin`
- `authenticateUser`
- `RedisConnectionFailureException`

### 25.5 登录页 V2 UI 结果

修改文件：`frontend/src/components/Login.vue`

实现结果：

- 登录页接入 `PageShell`、`UnifiedNav`、`ActionButton` 和全站主题变量。
- 保留 HNBLUE 统一导航和右侧全站主题切换。
- 登录、注册、忘记密码入口保留。
- 登录态仍保存 `localStorage.token` 和 `localStorage.userType`。
- 删除“第三方登录待接入”文字、微信/支付宝占位按钮和相关 CSS。
- 后端未启动或接口异常时显示温和提示，不白屏。

验证方式：

- `/login` 浅色主题：浅色生态背景、白色卡片、蓝绿色按钮。
- `/login` 深色主题：深色背景、深色卡片、浅色文字。
- 注册和忘记密码模式仍可切换。
- 点击登录时 Network 中请求应为 `/api/login`。

### 25.6 AI 碳助手 V1 资产审计结果

搜索关键词：

- `CarbonChat.vue`
- `Ai`
- `AI`
- `assistant`
- `chat`
- `CarbonAssistant`
- `stream-carbon`
- `EventSource`
- `SSE`
- `DeepSeek`
- `AnythingLLM`
- `robot`
- `floating`
- `右下角`
- `chatbot`

搜索路径：`frontend/src`、`backend/src/main/java`、`flask_model`、`docs`。

找到的 AI 资产：

- `frontend/src/components/CarbonChat.vue`：旧蓝碳聊天组件，使用 `EventSource`，调用 `${props.apiUrl}/stream-carbon?message=...`。
- `frontend/src/components/Chat.vue`：旧通用聊天组件，使用 `EventSource`，调用 `${props.apiUrl}/chat/stream?message=...`。
- `frontend/src/components/CarbonAIAssistant.vue`：旧 AI 说明页，不是右下角悬浮入口。
- `backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java`：提供 `/api/chat/stream-carbon`，对接 AnythingLLM。
- `backend/src/main/java/com/example/jpaspringboot/controller/SeekController.java`：提供 `/api/chat/stream`，对接 DeepSeek。

未找到：

- 未找到完整 V1 右下角悬浮外壳组件。实际可复用的是旧聊天面板/SSE 业务逻辑和后端流式接口。

### 25.7 全局悬浮 AI 碳助手实现结果

新增文件：

- `frontend/src/components/CarbonAiFloatingAssistant.vue`

修改挂载：

- `frontend/src/App.vue`

实现内容：

- 所有主页面右下角显示 `AI 碳助手` 悬浮入口。
- 点击后打开聊天面板，支持关闭、最小化、清空。
- 面板使用全站 CSS 变量，跟随浅色/深色主题。
- 图标改为组件内 SVG 蓝碳叶片图标，不再依赖随机外部 AI 图片。
- 复用旧蓝碳助手接口：`/api/chat/stream-carbon?message=...`。
- 复用旧 SSE 方式：`EventSource`。
- 后端或 AnythingLLM 未启动时显示“AI 服务未启动 / 连接中断”，不影响页面浏览。

同时处理：

- 删除上一轮临时文件 `frontend/src/components/GlobalAIAssistant.vue`，避免旧“AI 探助手”文案残留。
- `frontend/src/components/CarbonSeek.vue` 中按钮改为“打开 AI 碳助手”，仍通过 `hnblue-open-ai` 打开全局面板。
- `frontend/src/components/V2GovernmentPage.vue` 中说明卡改为“AI 碳助手”，政府端不再设置 AI 主标签。

### 25.8 政府端 AI 主标签处理结果

修改文件：`frontend/src/components/V2GovernmentPage.vue`

结果：

- 政府端主标签不再展示意义不清的 AI 助手标签。
- 政府端保留驾驶舱、一张图、指标库、文献证据、来源追溯、治理工单等主流程。
- AI 问答入口统一为右下角全局 `AI 碳助手`。

用户运行检查：打开 `/v2-government`，确认标签栏中没有独立 AI 主标签；右下角仍有全局 AI 碳助手入口。

### 25.9 Flask CatBoost 模型服务审计结果

搜索路径：`flask_model`。

找到的模型资产：

- `flask_model/carbon_model_api/app.py`
- `flask_model/carbon_model_api/predict_service.py`
- `flask_model/carbon_model_api/catboost_pipeline.pkl`
- `flask_model/carbon_model_api/requirements.txt`
- `flask_model/BAAD_raw.csv`
- `flask_model/Data_preprocessing.py`
- `flask_model/General_Modeling.py`
- `flask_model/ExploreParamsVisual_1.py`
- `flask_model/ExploreParamsVisual_2.py`
- `flask_model/ExploreParamsVisual_3.py`
- `flask_model/catboost_info/*`

Flask 端口和接口：

- 端口：`8880`
- `GET /ping`
- `POST /api/predict-carbon`
- `POST /api/predict-carbon/batch`
- `GET /api/literature-range`
- `POST /api/sensitivity-analysis`
- `POST /api/sensitivity-plot`
- `POST /api/sensitivity-explain`
- `GET /sensitivity_plots/<filename>`

本轮未启动 Flask，也未把 Flask 预测接口强接到 Spring Boot；当前前端 AI 碳助手走 Spring Boot SSE，碳储预测仍需下一轮做 Vue/Spring Boot/Flask 的正式代理或直连策略。

### 25.10 服务关系与推荐启动顺序

推荐启动顺序：

1. MySQL
   - 运行库：`hnblue_v2_dev_control`。
   - 先执行 25.4 的 SQL，确认 `user`、`admin`、`administrator_user_relation` 表和字段存在。
2. Redis
   - 当前配置使用集群节点：`192.168.204.128:6379` 到 `192.168.204.128:6384`。
   - 登录限流会使用 Redis；本轮已做 Redis 不可用时放行降级，但正式演示仍建议启动。
3. AnythingLLM
   - 默认地址：`http://localhost:3001/api`。
   - 工作区：默认 `hnblue`。
   - API Key 通过 `ANYTHINGLLM_API_KEY` 环境变量配置。
4. Spring Boot 后端
   - 目录：`<PROJECT_ROOT>\backend`
   - 用户本机运行：`mvn spring-boot:run`
   - 本轮未运行 Maven。
5. Flask CatBoost 模型服务
   - 目录：`<PROJECT_ROOT>\flask_model\carbon_model_api`
   - 用户本机运行：`python app.py`
   - 默认端口：`8880`。
6. Vue 前端
   - 目录：`<PROJECT_ROOT>\frontend`
   - 用户本机运行：`npm run serve`
   - 构建验证：本轮已执行 `npm run build`。

### 25.11 本机 API 验证命令参考

登录接口：

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8088/api/login" -ContentType "application/json" -Body '{"username":"你的登录名","password":"你的密码"}'
```

AI 碳助手 SSE：

```powershell
curl.exe -N "http://localhost:8088/api/chat/stream-carbon?message=%E4%BB%80%E4%B9%88%E6%98%AF%E8%93%9D%E7%A2%B3"
```

Flask 模型服务：

```powershell
Invoke-RestMethod -Uri "http://localhost:8880/ping"
Invoke-RestMethod -Uri "http://localhost:8880/api/literature-range"
```

Redis 连通性示例：

```powershell
redis-cli -h 192.168.204.128 -p 6379 ping
```

### 25.12 API Key 配置安全处理

修改文件：`backend/src/main/resources/application.properties`

处理结果：

- `api.password=${DEEPSEEK_API_KEY:}`
- `anythingllm.api.url=${ANYTHINGLLM_API_URL:http://localhost:3001/api}`
- `anythingllm.api.key=${ANYTHINGLLM_API_KEY:}`
- `anythingllm.workspace=${ANYTHINGLLM_WORKSPACE:hnblue}`

说明：本轮没有在前端或 README 中写入真实 API Key。用户本机运行 AI 时，需要在系统环境变量或私有配置中提供 `ANYTHINGLLM_API_KEY`；DeepSeek 备用接口需要 `DEEPSEEK_API_KEY`。

### 25.13 搜索与残留检查结果

已执行源码搜索并确认：

- 前端源码未再出现 `第三方登录待接入`。
- 前端源码未再出现 `AI 探助手`。
- 前端源码未再出现 `GlobalAIAssistant`。
- 前后端源码未再出现 `hnblue_government_theme`。
- 前端源码未发现 `icon="q"`、`icon="r"`、`icon="i"`、`icon="c"`、`icon="t"`、`q 查询`、`r 重置`。
- `frontend/src/assets/geo/hainan.json` 存在。
- `frontend/src/assets/json/hainan.json` 存在。
- `frontend/src/components/HainanBlueCarbonMap.vue` 仍使用 `echarts.registerMap('hainan-blue', hainanGeo)` 和 `type: 'map'`，没有回退为假地图。

### 25.14 本轮修改文件列表

新增：

- `frontend/src/components/CarbonAiFloatingAssistant.vue`

删除：

- `frontend/src/components/GlobalAIAssistant.vue`

修改：

- `frontend/src/App.vue`
- `frontend/src/components/Login.vue`
- `frontend/src/components/CarbonSeek.vue`
- `frontend/src/components/V2GovernmentPage.vue`
- `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java`
- `backend/src/main/java/com/example/jpaspringboot/interceptor/AccessLimitInterceptor.java`
- `backend/src/main/resources/application.properties`
- `docs/context/10_HNBLUE_V2_本轮交付README.md`

构建输出刷新：

- `frontend/dist/`

### 25.15 构建结果

执行目录：`<PROJECT_ROOT>\frontend`

执行命令：`npm run build`

结果：通过。

警告：

- `css/chunk-vendors.a717f531.css`、`js/chunk-vendors.ba531660.js`、`js/54.e4698581.js` 超过 Vue CLI 推荐体积阈值。
- app entrypoint 总体积约 `1.58 MiB`，超过推荐阈值。
- Browserslist/caniuse-lite 数据较旧。

这些是体积和依赖元数据警告，不是编译错误。

### 25.16 用户本机验证清单

1. `/login`：浅色/深色主题下是否完整，是否仍保留统一导航和主题切换。
2. `/login`：是否不再显示“第三方登录待接入”。
3. `/login`：Network 中登录请求是否为 `/api/login`。
4. `/login`：后端未启动时是否显示温和错误，不白屏。
5. `/login`：后端启动后，登录成功是否写入 `localStorage.token` 和 `localStorage.userType`。
6. `/login`：若仍返回 500，请反馈浏览器 Network 响应 JSON 和 Spring Boot 后端日志。
7. 任意主页面：右下角是否出现 `AI 碳助手`。
8. 点击 AI 碳助手：聊天面板是否打开，可最小化、关闭、清空。
9. 后端未启动时：发送问题是否显示“AI 服务未启动 / 连接中断”。
10. 后端和 AnythingLLM 启动后：AI 是否调用 `/api/chat/stream-carbon` 并流式返回。
11. `/carbonseek`：点击“打开 AI 碳助手”是否打开右下角全局面板。
12. `/v2-government`：是否不再出现独立 AI 主标签。
13. 全站主题切换：登录页和 AI 碳助手面板是否跟随浅色/深色变化。
14. `/visual`、`/devisual`、`/devisual/:name`、`/carbonseek`：真实海南地图入口是否仍可用。

### 25.17 下一轮建议任务

- 将 Spring Boot 增加 Flask CatBoost 代理接口，例如 `/api/model/predict-carbon`，统一前端调用路径。
- 为登录接口补充后端单元测试或 MockMvc 覆盖 Redis 不可用、表缺失、账号不存在、密码错误、登录成功五种场景。
- 将用户态路由守卫细化为“公开浏览可访问、治理提交需登录”。
- 把 AnythingLLM/DeepSeek 配置迁移到本机私有 profile 或 `.env`，避免任何真实密钥进入仓库。

## 26. 登录 500 定位与 AI 碳助手入口清理

### 26.1 `/api/login` 500 静态定位结论

本轮没有启动 Spring Boot、Maven、MySQL、Redis、AnythingLLM 或 Flask，因此不能声称已经在运行态复现并消除 500。当前完成的是代码链路定位和防护修复。

已确认：

- 前端登录请求位于 `frontend/src/components/Login.vue`，发送体为 `{ username, password }`。
- 后端 `LoginController.LoginRequest` 读取字段同样是 `username` 和 `password`。
- `/api/login` 逻辑先调用 `AdminServiceImpl.authenticateAdmin(username, password)`，再调用 `UserServiceImpl.authenticateUser(username, password)`。
- `AdminServiceImpl` 与 `UserServiceImpl` 均通过 `findByName(name)` 查账号，再用 `hashPassword(inputPassword, salt)` 与数据库 `passwordHash` 对比。
- 前后端字段名不一致不是当前 500 的主要原因。

最可能的 500 根因排序：

1. Spring Boot 没有重启，用户本机仍在运行旧代码。
2. Redis 限流拦截器仍在旧进程中抛出连接异常，导致请求未进入 `LoginController`。
3. `user` / `admin` 表字段与实体映射不一致，尤其是 `passwordHash` 与 `password_hash`。
4. 账号 `zhangsansan` 不存在，或存在但 `salt/password_hash` 与输入密码哈希不匹配。
5. 后端运行时数据库未连接到 `hnblue_v2_dev_control`。
6. 前端请求字段与后端读取字段不一致；本轮静态检查已排除当前源码中的这一项。

### 26.2 本轮代码修复

修改文件：

- `frontend/src/components/Login.vue`
  - 登录请求使用 `/api/login`。
  - 请求体字段明确为 `username`、`password`。
- `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java`
  - 登录空字段返回 400。
  - 用户名/密码不匹配返回 401。
  - 捕获异常时写入后端日志：`/api/login failed for user: ...`。
  - 500 响应返回 JSON，包含 `message` 和异常类名 `error`。
- `backend/src/main/java/com/example/jpaspringboot/interceptor/AccessLimitInterceptor.java`
  - Redis 限流异常时记录 `AccessLimit Redis unavailable, allow request: ...` 并放行请求。
  - 登录功能不应因为 Redis 未启动直接 500。
- `backend/src/main/java/com/example/jpaspringboot/entity/User.java`
  - `passwordHash` 增加 `@Column(name = "password_hash")`。
- `backend/src/main/java/com/example/jpaspringboot/entity/Admin.java`
  - `passwordHash` 增加 `@Column(name = "password_hash")`。

注意：实体列名明确为 `password_hash` 后，用户本机数据库中也需要存在该列。如果旧库实际列名是 `passwordHash`，请先迁移或重命名字段，再重启后端。

### 26.3 用户必须执行的安全 SQL 检查清单

不要直接 `SELECT password_hash`，先用 `information_schema.columns` 确认真实列名。

```sql
USE hnblue_v2_dev_control;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'hnblue_v2_dev_control'
  AND table_name IN ('user', 'admin', 'administrator_user_relation');

SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'hnblue_v2_dev_control'
  AND table_name IN ('user', 'admin', 'administrator_user_relation')
ORDER BY table_name, ordinal_position;

SELECT id, name, email, birthdate
FROM user
WHERE name = 'zhangsansan';

SELECT id, name
FROM admin
WHERE name = 'zhangsansan';
```

如果上面的列清单中存在 `password_hash`，再执行：

```sql
SELECT id, name, salt, password_hash
FROM user
WHERE name = 'zhangsansan';

SELECT id, name, salt, password_hash
FROM admin
WHERE name = 'zhangsansan';
```

如果列清单中是 `passwordHash` 而不是 `password_hash`，说明数据库与本轮实体映射不一致。建议迁移列名：

```sql
ALTER TABLE user CHANGE COLUMN passwordHash password_hash VARCHAR(255);
ALTER TABLE admin CHANGE COLUMN passwordHash password_hash VARCHAR(255);
```

执行迁移前请先备份数据库。

### 26.4 用户需要反馈的 Spring Boot 日志

如果 `/api/login` 仍返回 500，请反馈以下材料：

- 浏览器 Network 中 `/api/login` 的响应 JSON。
- Spring Boot 控制台中从 `/api/login failed for user:` 开始的一段完整异常栈。
- 是否出现 `AccessLimit Redis unavailable`。
- 是否出现 `SQLSyntaxErrorException`、`Unknown column`、`Table ... doesn't exist`、`Communications link failure`、`Access denied for user`。
- `application.properties` 运行态是否仍指向 `hnblue_v2_dev_control`。
- 是否已经重启 Spring Boot，而不是仍使用旧进程。

### 26.5 `/ai-assistant` 介绍页处理结果

本轮已删除独立介绍页组件：

- 删除：`frontend/src/components/CarbonAIAssistant.vue`

路由处理：

- `frontend/src/router/index.js` 中 `/ai-assistant` 不再引用 `CarbonAIAssistant.vue`。
- 新增兼容组件：`frontend/src/components/CarbonAiOpenRedirect.vue`。
- 用户访问 `/ai-assistant` 时，组件只做两件事：
  1. `window.dispatchEvent(new CustomEvent("hnblue-open-ai"))` 打开右下角全局 `AI 碳助手`；
  2. `router.replace("/carbonseek")` 回到碳溯页面。

因此 `/ai-assistant` 不再是介绍页，也不会 404；它只是旧链接兼容入口。

### 26.6 全局 AI 碳助手接口路径

前端：

- 文件：`frontend/src/components/CarbonAiFloatingAssistant.vue`
- 请求路径：`/api/chat/stream-carbon`
- 调用方式：`EventSource(`${STREAM_URL}?message=...`)`
- 后端未启动或 AnythingLLM 不可用时，面板显示温和提示：`AI 服务未启动 / 连接中断`。

后端：

- 文件：`backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java`
- `@RequestMapping("${chat.api.path}")`，当前 `chat.api.path=/api/chat`。
- `@GetMapping("/stream-carbon")`，最终路径为 `/api/chat/stream-carbon`。
- 后端通过 AnythingLLM 提供回答。
- `ANYTHINGLLM_API_KEY` 需要用户通过环境变量提供；当前配置为 `anythingllm.api.key=${ANYTHINGLLM_API_KEY:}`。

该功能与 Python Flask model 没有直接关系；本轮没有发现 Python 端参与 AI 碳助手回答链路。

### 26.7 Python Flask model 审计与运行状态

本轮未运行 Python Flask model，仅完成代码审计和启动命令整理。

未执行：

- 未启动 `python app.py`。
- 未请求 `http://localhost:8880/ping`。
- 未请求 `http://localhost:8880/api/literature-range`。

已确认文件存在：

- `flask_model/carbon_model_api/app.py`
- `flask_model/carbon_model_api/requirements.txt`
- `flask_model/carbon_model_api/catboost_pipeline.pkl`

已确认 Flask 端口和接口：

- `app.run(host="0.0.0.0", port=8880)`。
- `GET /ping`
- `GET /api/literature-range`
- `POST /api/predict-carbon`
- `POST /api/predict-carbon/batch`
- `POST /api/sensitivity-analysis`
- `POST /api/sensitivity-plot`
- `POST /api/sensitivity-explain`

用户本机启动命令：

```powershell
cd <PROJECT_ROOT>\flask_model\carbon_model_api
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

用户本机验证命令：

```powershell
Invoke-RestMethod http://localhost:8880/ping
Invoke-RestMethod http://localhost:8880/api/literature-range
```

### 26.8 残留搜索和构建结果

源码搜索结果：

- `frontend/src` 不再引用 `CarbonAIAssistant.vue`。
- `frontend/src` 不再存在 `GlobalAIAssistant`。
- `frontend/src` 不再存在 `AI 探助手`。
- `frontend/src` 不再存在 `第三方登录待接入`。
- `frontend/src` 和 `backend/src` 不再使用 `hnblue_government_theme`。
- 右下角组件和页面文案统一为 `AI 碳助手`。

构建命令：

```powershell
cd <PROJECT_ROOT>\frontend
npm run build
```

构建结果：通过。

仍有警告：

- vendor chunk 和部分懒加载 chunk 超过 Vue CLI 推荐体积阈值。
- Browserslist/caniuse-lite 数据较旧。

这些是体积和依赖元数据警告，不是编译错误。

### 26.9 本轮修改文件列表

新增：

- `frontend/src/components/CarbonAiOpenRedirect.vue`

删除：

- `frontend/src/components/CarbonAIAssistant.vue`

修改：

- `frontend/src/router/index.js`
- `frontend/src/components/Login.vue`
- `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java`
- `backend/src/main/java/com/example/jpaspringboot/interceptor/AccessLimitInterceptor.java`
- `backend/src/main/java/com/example/jpaspringboot/entity/User.java`
- `backend/src/main/java/com/example/jpaspringboot/entity/Admin.java`
- `docs/context/10_HNBLUE_V2_本轮交付README.md`

构建输出刷新：

- `frontend/dist/`
## 27. 2026-07-11 AI 碳助手流式响应体验优化

### 27.1 本轮修复目标

本轮针对右下角全局 `AI 碳助手` 的运行态体验做专项优化：

- 保留现有前端调用路径 `/api/chat/stream-carbon`，不改变 Vue 侧入口。
- Spring Boot 继续作为 Vue 与 AnythingLLM/DeepSeek 之间的代理层。
- 后端新增响应清洗处理，过滤模型推理过程和 AnythingLLM 内部 metadata。
- 前端将 SSE chunk 转成逐字符显示，形成类似 ChatGPT 的渐进生成体验。

### 27.2 后端处理结果

修改文件：

- `backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java`

核心变化：

- `/stream-carbon` SSE 接口保留，最终路径仍为 `/api/chat/stream-carbon`。
- AnythingLLM 请求路径改为使用配置项 workspace：`/v1/workspace/{workspace}/stream-chat`，不再写死 `hnblue`。
- 新增内部 `AiResponseProcessor`，逐 chunk 清洗 AnythingLLM 返回内容。
- 清洗规则包括：
  - 删除 `<think>...</think>` 推理块；
  - 删除 `uuid`、`sources`、`metrics`、`finalizeResponseStream`、`close` 等内部 metadata JSON；
  - 保留 `textResponse`、`content`、`text`、`message`、`response`、`delta` 中的自然语言回答；
  - 普通自然语言 chunk 立即发送，仅当疑似 `<think>` 或 metadata 标记跨 chunk 时短暂保留尾部字符。
- SSE 输出仍使用 `data:{ContentDto JSON}`，结束时仍发送 `data:end`。
- 增加响应头 `Cache-Control: no-cache` 和 `X-Accel-Buffering: no`，降低代理缓存对流式输出的影响。

日志增强：

- `AI请求开始 messageLength=...`
- `AI收到chunk数量=... 过滤前长度=... 过滤后长度=...`
- `AI连接关闭 chunk总数=...`
- `AnythingLLM 连接失败 chunk总数=...`

安全说明：日志没有打印 AnythingLLM API Key。

### 27.3 前端处理结果

修改文件：

- `frontend/src/components/CarbonAiFloatingAssistant.vue`

核心变化：

- 保留旧接口调用：`/api/chat/stream-carbon?message=...`。
- SSE 收到 chunk 后不再一次性追加完整文本，而是进入字符队列。
- 新增 `TYPE_INTERVAL = 24` 毫秒的逐字符输出节奏。
- 新增流式状态：
  - 连接中：`正在连接 AI 碳助手...`
  - 生成中：`正在生成回答...`
  - 结束：`回答完成`
  - 失败：`AI 服务未启动 / 连接中断：当前仅影响 AI 碳助手，不影响页面浏览。`
- 当前 assistant 消息带 `streaming` 状态，生成中显示柔和打字光标。
- 面板关闭或清空时会停止打字计时器并关闭 EventSource，避免残留流。

### 27.4 构建与静态检查结果

已执行前端构建：

```powershell
cd <PROJECT_ROOT>\frontend
npm run build
```

结果：通过。

构建警告：

- vendor chunk 和入口体积超过 Vue CLI 推荐阈值。
- Browserslist/caniuse-lite 数据较旧。

这些是体积和依赖元数据警告，不是本轮 AI 改动导致的编译错误。

已执行后端编译：

```powershell
cd <PROJECT_ROOT>\backend
mvn -q -DskipTests compile
```

结果：通过。

本轮没有启动 Spring Boot，没有启动 Maven 长服务，没有启动 AnythingLLM，也没有启动 Flask。

轻量运行态探测：

```powershell
Invoke-WebRequest "http://localhost:8088/api/chat/stream-carbon?message=什么是蓝碳"
```

结果：本机当前 `localhost:8088` 未运行 Spring Boot，返回“无法连接到远程服务器”。因此真实 SSE 端到端效果仍需用户本机启动服务后验证。

### 27.5 用户本机验证清单

请在 AnythingLLM 正常运行、Spring Boot 已启动后验证：

```powershell
curl.exe -N "http://localhost:8088/api/chat/stream-carbon?message=%E4%BB%80%E4%B9%88%E6%98%AF%E8%93%9D%E7%A2%B3"
```

重点观察：

1. 浏览器或 curl 是否连续收到多段 `data:`。
2. 输出中是否不再出现 `<think>`、`</think>`。
3. 输出尾部是否不再出现 `uuid`、`sources`、`metrics`、`finalizeResponseStream` 等 JSON metadata。
4. 前端右下角 `AI 碳助手` 是否逐字显示回答，而不是一次性刷出完整文本。
5. 后端日志是否出现 chunk 数量、过滤前长度、过滤后长度、连接关闭记录。
6. 日志中不应出现 API Key。

### 27.6 本轮修改文件

本轮专项修改：

- `backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java`
- `frontend/src/components/CarbonAiFloatingAssistant.vue`
- `docs/context/10_HNBLUE_V2_本轮交付README.md`

说明：当前工作区还存在前序 UI、登录、配置和构建产物相关改动；本节只记录 2026-07-11 AI 碳助手流式响应体验专项涉及的文件。
## 28. 2026-07-12 AI 碳助手流式链路重构与真实 SSE 验证

### 28.1 根因

上一版 `CarbonAssistantController` 的问题不是单纯少两个方法，而是链路设计本身不稳：

- 控制器内仍保留 `AiResponseProcessor` 的累积缓冲式清洗逻辑，且曾引用缺失方法 `keepPossibleClosingThinkTail()`、`partialTokenHoldLength(String)`，导致运行态出现 `Unresolved compilation problems`。
- 后端使用 `PrintWriter + CountDownLatch` 等待上游 SSE 完整结束，占用 Tomcat 请求线程。
- AnythingLLM 的事件被当作普通文本/嵌套 JSON 转发，`finalizeResponseStream` 中的 sources、metrics 等 metadata 可能进入正文或前端显示区。
- 前端使用 `24 ms/字符` 的人工打字队列，AnythingLLM 已经生成完成时仍会积压显示。

### 28.2 后端重构结果

修改文件：

- `backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java`
- `backend/src/main/java/com/example/jpaspringboot/service/ai/ThinkTagFilter.java`
- `backend/src/test/java/com/example/jpaspringboot/service/ai/ThinkTagFilterTest.java`

处理结果：

- 删除旧 `AiResponseProcessor`。
- 删除 `keepPossibleClosingThinkTail`、`partialTokenHoldLength` 残留引用。
- `/api/chat/stream-carbon` URL 保持不变。
- 控制器改为 `SseEmitter` 返回，不再在控制器线程中等待完整回答。
- OkHttp SSE 继续异步连接 AnythingLLM：`/api/v1/workspace/{workspace}/stream-chat`。
- `readTimeout` 改为长连接友好配置，避免 60 秒固定读超时打断生成。
- 前端断开、超时或异常时会取消上游 `EventSource`。
- 请求体保留 `message`，增加 `mode="chat"` 和稳定 `sessionId`。
- 响应头设置 `Cache-Control: no-cache` 与 `X-Accel-Buffering: no`。

### 28.3 Spring Boot → Vue SSE 协议

后端统一发送单层 JSON，不再套 `ContentDto` 字符串：

```text
data:{"type":"status","status":"connecting","message":"正在连接 AI 碳助手…"}

data:{"type":"status","status":"thinking","message":"正在分析问题并检索知识库…"}

data:{"type":"status","status":"generating","message":"正在生成回答…"}

data:{"type":"delta","content":"蓝碳"}

data:{"type":"meta","sources":[...],"metrics":{...},"model":"...","duration":...}

data:{"type":"done","eventCount":206,"visibleChars":216,"firstVisibleMs":3475,"totalMs":5105}

data:{"type":"error","message":"AI 服务连接失败，请检查 Spring Boot 与 AnythingLLM。"}
```

`done` 只发送一次。

### 28.4 AnythingLLM 事件处理方式

- `textResponseChunk`：只读取 `textResponse`，经 `ThinkTagFilter` 清洗后立即作为 `delta` 转发。
- `textResponse`：兼容旧版本；若已收到增量 chunk，则不再转发完整答案，避免重复。
- `finalizeResponseStream`：不转发完整 JSON，不转发完整答案；只生成轻量 `meta` 与一次 `done`。
- `agentThought` / `thought`：不展示和不保存原始思考文本，只向前端发送 `thinking` 状态。
- `abort` / `error=true`：转换成 `error` 事件；日志记录状态和简短错误，不打印 API Key。

meta 压缩说明：第一次真实 curl 发现 sources 中包含大量文档原文；已改为只保留 `title`、`url`、`docSource`、`chunkSource`、`published` 等摘要字段。metrics 只保留模型、provider、duration、token 与输出速度等小型统计。

### 28.5 ThinkTagFilter 实现与测试

新增 `ThinkTagFilter` 使用有限状态机，状态包括：

- `VISIBLE`
- `MATCHING_START`
- `INSIDE_THINK`
- `MATCHING_END`

它只保留识别 `<think>` 与 `</think>` 所需的极短尾部，不累积完整回答，不用跨全文正则，不改写 Markdown 语义。

测试命令：

```powershell
cd <PROJECT_ROOT>\backend
mvn -Dtest=ThinkTagFilterTest test
```

结果：通过。

覆盖用例：完整标签、开始标签拆分、结束标签拆分、标签前后正文、无 think 标签、Markdown 代码块普通尖括号、空 chunk、中文多行、think 结束后立即正文、未闭合尾部、finalize 不重复进入过滤器。

### 28.6 前端重构结果

修改文件：

- `frontend/src/components/CarbonAiFloatingAssistant.vue`

处理结果：

- 删除 `TYPE_INTERVAL = 24`。
- 删除逐字符 `charQueue`、字符级 `setInterval` 和服务结束后继续吐字的长队列。
- SSE 收到 `delta` 后进入 `pendingDelta`，最多约 40ms 合并刷新一次，不制造人工延迟。
- 每条 AI 消息保存 `rawMarkdown`。
- 使用项目已有 `md-editor-v3` 的 `MdPreview` 实时渲染 Markdown。
- 支持标题、列表、粗体、行内代码、代码块、表格、引用、链接等 Markdown 展示能力。
- 关闭、清空、重新提问时关闭 `EventSource` 并清理 pending delta、flush timer、scroll frame。
- 自动滚动改为 `requestAnimationFrame` 节流。
- 增加折叠式“回答依据”，只展示来源标题/路径摘要、模型、耗时和 token 统计，不展示原始 `<think>` 或 sources 正文。

### 28.7 编译、构建与真实验证结果

后端 clean 编译：

```powershell
mvn clean compile
```

结果：通过。无 `Unresolved compilation problems`，无缺失方法残留。

后端打包：

```powershell
mvn clean package -DskipTests
```

结果：通过。

前端构建：

```powershell
npm run build
```

结果：通过。仍有既有体积警告和 Browserslist 数据较旧提示。

完整后端测试：

```powershell
mvn clean test
```

结果：未完成。命令在约 244 秒后超时，后续确认本轮新增 `ThinkTagFilterTest` 可单独通过。完整测试超时与历史测试/上下文测试耗时有关，本轮未把它写成“已通过”。

真实 SSE：

```powershell
curl.exe -N --max-time 90 "http://localhost:8088/api/chat/stream-carbon?message=%E4%BB%80%E4%B9%88%E6%98%AF%E8%93%9D%E7%A2%B3&sessionId=codex-smoke-test-2"
```

结果：通过。

摘要：

- `CURL_EXIT=0`
- `eventCount=206`
- `visibleChars=216`
- `firstVisibleMs=3475`
- `totalMs=5105`
- `contains_think=False`
- `contains_uuid=False`
- `contains_finalize=False`
- `contains_raw_source_text=False`

curl 输出首段示例：

```text
data:{"message":"正在连接 AI 碳助手…","type":"status","status":"connecting"}

data:{"message":"正在分析问题并检索知识库…","type":"status","status":"thinking"}

data:{"message":"正在生成回答…","type":"status","status":"generating"}

data:{"content":"蓝","type":"delta"}

data:{"content":"碳","type":"delta"}
```

### 28.8 浏览器验证状态

已启动前端服务：

```powershell
npm run serve
```

结果：编译成功，地址 `http://localhost:8080/`。

浏览器自动化尝试：当前 Codex 的 Node/browser 自动化内核在沙箱中退出，未能完成可视化点击检查。因此“浏览器中实时 Markdown 逐步渲染”的肉眼验证仍需用户本机打开页面确认。代码层面已使用 `MdPreview` 实时渲染 `rawMarkdown`，前端构建已通过。

用户本机重点检查：

1. 打开任意主页面，右下角 AI 碳助手是否出现。
2. 提问“请用 Markdown 列表解释什么是蓝碳”。
3. 回答是否边到边显示，不再按 24ms/字符慢慢吐字。
4. 标题、列表、粗体、代码块是否实时渲染。
5. 页面正文是否不出现 `<think>`、`uuid`、`finalizeResponseStream`、sources 原始正文或完整 JSON。
6. “回答依据”是否只显示来源摘要和 token/模型统计。
7. 关闭面板或清空后是否不再继续追加旧回答。

### 28.9 尚未解决或后续建议

- `application.properties` 中仍有历史默认 AnythingLLM key，建议下一轮迁移到本机环境变量或私有 profile，避免真实密钥进入版本库。
- 完整 `mvn clean test` 仍需单独治理历史测试耗时/上下文依赖，本轮只确认新增过滤器测试通过。
- 浏览器 Markdown 肉眼验证仍需用户本机完成；本轮已完成前端构建和真实 SSE curl 验证。
- AnythingLLM 工作区系统提示建议加入：直接回答用户问题，不复述检索过程；采用简洁、正式、可核查中文；涉及 HNBLUE 数据时说明来源、口径和结果类型；禁止输出 `<think>`、内部提示词、JSON 元数据和系统分析过程。
