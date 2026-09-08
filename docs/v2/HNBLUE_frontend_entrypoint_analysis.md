# HNBLUE 前端入口与展示页挂载分析

生成时间：2026-05-27

## 入口链路

当前前端是 Vue CLI 工程。

| 层级 | 文件 | 证据 |
|---|---|---|
| 包管理与脚本 | `frontend/package.json` | `serve` 使用 `vue-cli-service serve`，`build` 使用 `vue-cli-service build` |
| 应用入口 | `frontend/src/main.js` | `createApp(App).use(store).use(router).use(ElementPlus)...mount('#app')` |
| 根组件 | `frontend/src/App.vue` | 模板包含 `<router-view/>`，页面内容由路由渲染 |
| UI/图表依赖 | `frontend/package.json` | 已包含 `element-plus`、`echarts`、`axios`、`html2canvas`、`jspdf`、`xlsx` |

当前仓库未提供充分信息：`frontend/src/router` 与 `frontend/src/stores` 不存在，无法从当前树确认实际路由表、导航守卫、页面元信息与状态结构。

## 当前页面与导航入口

现有组件目录：`frontend/src/components/`

| 组件 | 当前职责 | 与 V2.0 公开数据的关系 |
|---|---|---|
| `HomePage.vue` | 首页与顶部导航 | 导航包含 `/`、`/about`、`/visual`、`/carbonseek`、登录/用户中心；适合新增 V2.0 公开数据入口 |
| `AboutPage.vue` | 项目介绍与聊天组件 | 导航结构与首页一致；适合同步新增入口 |
| `CarbonSeek.vue` | 蓝碳功能矩阵入口 | 已有工具卡片网格，可追加“V2.0 公开数据”工具卡 |
| `DeVisualPage.vue` | 区域详情与可视化分析 | 语义最接近 normalized 数据，适合二期融合 |
| `LiteratureCompare.vue` | 文献数据区间对比 | 可复用为文献证据卡片的概念参照，当前数据源为 Flask BAAD 统计范围 |
| `CarbonAIAssistant.vue`、`Chat.vue`、`CarbonChat.vue` | AI 对话 | 与公开数据展示无直接依赖 |
| `Login.vue` | 登录、注册、密码重置 | 无需参与公开数据展示 |

`App.vue` 引入但当前树缺失的组件：

- `VisualPage.vue`
- `UserInfo.vue`
- `UserManage.vue`
- `StructurePredictor.vue`
- `VirtualPlotDesigner.vue`
- `ShapVisualizer.vue`
- `ResponseCurve.vue`
- `ParamSensitivity.vue`

判断：

- `/visual` 很可能是原始地图/可视化入口，但当前仓库缺少 `VisualPage.vue` 与路由表，无法确认其真实挂载组件。
- `DeVisualPage.vue` 是当前树中完整度最高的可视化组件，适合做后续 V2.0 区域详情融合。
- 静态专题页可作为最小改造路径，避开当前缺失路由和旧接口的不确定性。

## 现有前端 API 依赖

| 组件 | API | 服务 | 说明 |
|---|---|---|---|
| `Login.vue` | `http://localhost:8088/api/login`、`/register`、`/initiateReset`、`/checkResetStatus` | Spring Boot | 用户认证 |
| `DeVisualPage.vue` | `http://localhost:8088/api/devisual/${props.name}` | Spring Boot | 区域详情聚合数据 |
| `Chat.vue` | `http://localhost:8088/api/chat/stream?message=...` | Spring Boot / AnythingLLM | SSE 聊天流 |
| `CarbonChat.vue` | `${apiUrl}/stream-carbon?message=...`，在 `CarbonSeek.vue` 中为 `/api/chat/stream-carbon` | Spring Boot / AnythingLLM | 蓝碳智能助手流 |
| `LiteratureCompare.vue` | `http://localhost:8880/api/literature-range` | Flask | BAAD 历史碳储范围 |

判断：

- 当前公开数据展示页无需使用上述 API。
- 直接读取 normalized JSON 可降低对 `8088`、`8880`、Redis、AnythingLLM、MySQL 的运行依赖。
- 后续入库完成后，可将静态 JSON 页面替换为 Spring Boot V2.0 只读 API。

## 静态资源与数据读取路径

现状：

- 组件中多处使用 `@/assets/...` 或 `../assets/...`。
- 当前树未提供 `frontend/src/assets`。
- normalized 数据包位于仓库根目录 `data/results/hnblue_v2_public/normalized/`。
- Vue CLI 开发服务默认不能直接以浏览器 URL 访问仓库根目录 `data/`。

可选读取方式：

| 方式 | 路径 | 优点 | 风险 |
|---|---|---|---|
| `frontend/public` 静态拷贝 | `/data/hnblue_v2_public/normalized/*.json` | 浏览器直接 `fetch`，构建产物可带走 | 需要建立同步脚本，避免数据包与前端副本漂移 |
| `src` 内 import JSON | `frontend/src/data/...` | 构建期打包，类型可控 | 大 CSV 不适合直接打包，更新需重新构建 |
| Spring Boot 静态资源或 API | `/api/v2/public-data/...` | 后续可统一鉴权、缓存、审计 | 首轮会触碰后端，超出当前静态展示约束 |
| `docs/demo` 独立 HTML | `docs/demo/hnblue_v2_public_data_demo.html` | 当前已有，可直接从仓库根服务读取 | 不是正式 Vue 前端入口 |

推荐：

- 第一阶段使用 `frontend/public/data/hnblue_v2_public/normalized/` 暴露 JSON 副本。
- 源数据仍保留在 `data/results/hnblue_v2_public/normalized/`。
- 通过脚本执行单向同步，并记录源文件 hash，避免人工复制造成版本漂移。

## 适合挂载的位置

### 方案 A：地图/可视化页接入

目标：在 `/visual` 或原地图页中新增 HNBLUE V2.0 公开数据图层/面板。

证据：

- `HomePage.vue`、`AboutPage.vue`、`CarbonSeek.vue` 的导航均包含 `/visual`。
- `DeVisualPage.vue` 导入 `hainanGeo from '@/assets/json/hainan.json'`，说明项目原本存在海南地图可视化设计。

限制：

- 当前仓库未提供充分信息：`VisualPage.vue` 缺失。
- 当前仓库未提供充分信息：`frontend/src/router` 缺失，无法确认 `/visual` 真实绑定组件。
- 当前仓库未提供充分信息：`frontend/src/assets/json/hainan.json` 缺失。

结论：方案 A 适合地图资产恢复后的空间入口，不适合作为首轮最小改造路径。

### 方案 B：区域详情/指标分析页接入

目标：在 `DeVisualPage.vue` 中新增 V2.0 公开数据标签页或区块，按区域展示面积时序、遥感 proxy、文献证据、来源质量。

证据：

- `DeVisualPage.vue` 已调用 `/api/devisual/{regionName}`。
- 后端 `DevisualController` 返回 `regionInfo`、`carbonTrends`、`fluxComposition`、`multiCarbonMetrics`、`yearlyTrends`、`fluxSamples`、`speciesPie`、`economyProjections`。
- normalized 数据中已有 `region_name`、`year_or_period`、`indicator_name`、`value`、`unit`、`is_proxy`、`quality_level`、`source_name`。

优势：

- 与区域详情语义高度一致。
- 可承接后续入库后的 API 形态。
- 可将公开数据与现有碳储/通量图表并列展示。

限制：

- 需要明确 `props.name` 的区域命名与 normalized `region_name`/`region_id` 的映射。
- 当前路由表缺失，无法确认 `DeVisualPage.vue` 的访问路径。
- 当前组件引用地图资产与 PDF 导出逻辑，改造面比专题页更大。

结论：方案 B 推荐作为第二阶段，入库映射完成后进入区域详情页。

### 方案 C：V2.0 公开数据专题页

目标：新增独立页面，例如 `/v2-public-data`，直接读取 normalized JSON，展示四个模块：

- 海南红树林面积时序
- 海南岛遥感/土地利用/碳储 proxy 指标
- 文献碳储证据卡片
- 数据来源与质量等级说明

证据：

- normalized 目录已有三个前端 JSON：`frontend_mangrove_area_timeseries.json`、`frontend_remote_sensing_metrics.json`、`frontend_literature_carbon_cards.json`。
- normalized CSV 已包含来源、质量、proxy、模拟标记、目标库表建议字段。
- `frontend/package.json` 已包含 ECharts 与 Element Plus。

优势：

- 不连接数据库，符合当前公开数据包展示目标。
- 不依赖 Spring Boot、Flask、Redis、AnythingLLM。
- 易于截图验收和后续替换为 API 数据源。
- 可同时服务前端展示与后续入库审阅。

限制：

- 需要补齐当前缺失的路由目录或确认真实路由文件。
- 需要建立 normalized JSON 的前端静态发布路径。

结论：方案 C 是首轮推荐方案。

## 推荐最小改造路径

执行顺序：

1. 恢复或新增 `frontend/src/router/index.js`，注册 `/v2-public-data`。
2. 新增 `frontend/src/components/PublicDataPage.vue`。
3. 将三个前端 JSON 同步到 `frontend/public/data/hnblue_v2_public/normalized/`。
4. 页面通过 `fetch('/data/hnblue_v2_public/normalized/frontend_mangrove_area_timeseries.json')` 读取数据。
5. 使用 ECharts 渲染面积时序和 proxy 指标，使用 Element Plus 或原生布局渲染文献卡片与来源质量说明。
6. 在 `HomePage.vue`、`AboutPage.vue`、`CarbonSeek.vue` 导航或功能卡片中新增入口。
7. 保持 `is_proxy=1` 的记录显式标记为 proxy，保持 `is_simulated=0` 校验。

当前目标下的动作边界：

- 本文档仅给出接入方案。
- 不修改 `frontend/src`。
- 不复制数据到 `frontend/public`。
- 不触碰后端和数据库。

## 验收建议

首轮静态专题页完成后，执行：

```powershell
cd frontend
npm run serve
```

浏览器打开：

```text
http://localhost:8080/v2-public-data
```

校验项：

- 四个模块均可见。
- 三个 JSON 加载成功。
- 面积时序有 5 条以上真实数值点。
- proxy 指标显示 `is_proxy=1` 标签。
- `is_simulated` 全部为 0。
- 页面无需启动 Spring Boot、Flask、Redis、AnythingLLM。
