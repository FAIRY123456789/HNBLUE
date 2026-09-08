# HNBLUE V2.0 公开数据前端接入报告

生成时间：2026-05-27

## 接入结果

已按方案 C 接入 Vue 前端，新增静态专题页：

```text
http://localhost:8080/v2-public-data
```

页面直接读取前端 public 副本：

```text
frontend/public/data/hnblue_v2_public/normalized/
```

源数据目录保持不移动：

```text
data/results/hnblue_v2_public/normalized/
```

## 修改文件

| 文件 | 说明 |
|---|---|
| `frontend/src/router/index.js` | 新增 Vue Router，注册 `/v2-public-data`，保留 `/`、`/homepage`、`/about`、`/visual`、`/carbonseek`、`/login`、工具页入口 |
| `frontend/src/stores/counter.js` | 新增最小 Vuex store，满足 `main.js` 既有依赖 |
| `frontend/src/App.vue` | 精简根组件，移除对缺失组件的直接导入，交由 router 懒加载 |
| `frontend/src/components/PublicDataPage.vue` | 新增 HNBLUE V2.0 公开数据专题页 |
| `frontend/src/components/HomePage.vue` | 顶部导航新增“V2.0公开数据” |
| `frontend/src/components/AboutPage.vue` | 顶部导航新增“V2.0公开数据” |
| `frontend/src/components/CarbonSeek.vue` | 顶部导航新增“V2.0公开数据”，功能卡片新增公开数据入口 |
| `frontend/vue.config.js` | 新增 `devServer.historyApiFallback`，支持直接访问 `/v2-public-data` |
| `scripts/frontend/sync_hnblue_v2_public_data.py` | 新增 normalized 数据同步脚本 |
| `frontend/public/data/hnblue_v2_public/normalized/*` | 新增前端 public 数据副本与 `sync_manifest.json` |
| `frontend/src/assets/*` | 补齐旧页面编译所需最小占位图片资源 |

## 页面模块

`PublicDataPage.vue` 已实现四个展示模块：

| 模块 | 数据文件 | 展示内容 |
|---|---|---|
| 海南红树林面积时序 | `frontend_mangrove_area_timeseries.json` | 区域选择、面积折线图、面积时序点统计 |
| 海南岛遥感/土地利用/碳储 proxy 指标 | `frontend_remote_sensing_metrics.json` | 单位筛选、柱状图、指标表、proxy/direct 标签 |
| 文献碳储证据卡片 | `frontend_literature_carbon_cards.json` | 文献标题、年份、数值、单位、质量等级、来源链接、proxy/direct 标签 |
| 数据来源与质量等级 | `display_data_sources.csv` | 来源名称、来源类型、质量等级、记录数、proxy/direct 统计、来源链接 |

页面只调用：

```text
/data/hnblue_v2_public/normalized/frontend_mangrove_area_timeseries.json
/data/hnblue_v2_public/normalized/frontend_remote_sensing_metrics.json
/data/hnblue_v2_public/normalized/frontend_literature_carbon_cards.json
/data/hnblue_v2_public/normalized/display_data_sources.csv
/data/hnblue_v2_public/normalized/sync_manifest.json
```

`rg` 校验 `PublicDataPage.vue` 与 router 中无以下请求：

```text
localhost:8088
localhost:8880
api/devisual
literature-range
stream-carbon
stream?message
```

## 数据同步

执行命令：

```powershell
python scripts/frontend/sync_hnblue_v2_public_data.py
```

同步文件：

| 文件 | 记录数 |
|---|---:|
| `frontend_mangrove_area_timeseries.json` | 11 regions |
| `frontend_remote_sensing_metrics.json` | 147 metrics |
| `frontend_literature_carbon_cards.json` | 73 cards |
| `display_mangrove_area_timeseries.csv` | 172 |
| `display_remote_sensing_metrics.csv` | 147 |
| `display_literature_carbon_records.csv` | 73 |
| `display_species_allometry_records.csv` | 22 |
| `display_data_sources.csv` | 9 |
| `display_region_dictionary.csv` | 25 |

同步记录：

```text
frontend/public/data/hnblue_v2_public/normalized/sync_manifest.json
```

## 数据校验

对 `frontend/public/data/hnblue_v2_public/normalized/` 执行只读校验：

| 文件 | 记录数 | 区域数 | 指标数 | proxy | simulated 合计 |
|---|---:|---:|---:|---:|---:|
| `display_mangrove_area_timeseries.csv` | 172 | 12 | 13 | 0 | 0 |
| `display_remote_sensing_metrics.csv` | 147 | 10 | 49 | 30 | 0 |
| `display_literature_carbon_records.csv` | 73 | 14 | 31 | 19 | 0 |
| `display_species_allometry_records.csv` | 22 | 3 | 6 | 0 | 0 |
| `display_data_sources.csv` | 9 | 0 | 0 | 49 | 0 |

前端 JSON 校验：

| 文件 | `is_simulated` | 主记录数 |
|---|---:|---:|
| `frontend_mangrove_area_timeseries.json` | 0 | 11 regions |
| `frontend_remote_sensing_metrics.json` | 0 | 147 metrics |
| `frontend_literature_carbon_cards.json` | 0 | 73 cards |

结论：

- `is_simulated=0` 约束保留。
- proxy 记录未改写为真实观测，页面显式显示 `proxy` 标签。
- MODIS、ERA5、CHIRPS、GHG 清单等区域统计继续按 proxy/区域统计展示。

## 启动与访问验证

依赖安装：

```powershell
cd frontend
npm install
```

结果：

- 安装完成。
- 仅出现 Vue peer dependency 警告。

生产构建：

```powershell
cd frontend
npm run build
```

结果：

- 构建成功。
- 仅出现构建体积与 Browserslist 过期警告。

开发服务：

```powershell
cd frontend
npm run serve -- --host 127.0.0.1 --port 8080
```

结果：

- Vue CLI 编译成功。
- 输出 `App running at: http://127.0.0.1:8080/`。
- `http://127.0.0.1:8080/v2-public-data` 返回 200。
- 三个前端 JSON 与 `display_data_sources.csv` 返回 200。

Headless Edge 渲染验证：

```text
docs/v2/hnblue_v2_public_data_page.png
docs/v2/hnblue_v2_public_data_page_dom.html
```

DOM 校验结果：

| 检查项 | 结果 |
|---|---|
| `海南红树林蓝碳公开数据` | True |
| `面积时序点` | True |
| `遥感/土地利用指标` | True |
| `文献证据记录` | True |
| `数据来源与质量等级` | True |
| `proxy` | True |
| `模拟记录` | True |

## 最小占位说明

当前仓库缺失多个历史组件与静态资源。为保持 `/`、`/about`、`/visual`、`/carbonseek` 等入口不因缺失组件直接崩溃，本轮采用最小占位：

- `/visual` 使用路由内占位组件，未修复或重构地图页。
- `UserInfo`、`UserManage`、`StructurePredictor`、`VirtualPlotDesigner`、`ShapVisualizer`、`ResponseCurve`、`ParamSensitivity` 使用路由内占位组件。
- `frontend/src/assets/` 补齐 1x1 占位图片，避免旧页面构建期缺资源失败。

上述占位仅服务于前端启动链路，不改变 HNBLUE V2.0 公开数据专题页的数据逻辑。
