# HNBLUE V2.0 红树林面积事实数据缺失与阻塞报告

## 当前产物

已生成 `data/staging/mangrove_cover/` 下 5 个文件和本报告。未写入 MySQL，未修改 DDL、后端、前端或旧库。

## 无法完成的下载与计算

1. CASEarth 30 m A 类：页面可访问，显示下载入口；本轮未捕获稳定直链。面积 seed 使用已知论文表格和 CASEarth 元数据登记。
2. CASEarth GF-2 1 m B 类：页面可访问，显示下载入口和 shp 元数据；当前未捕获稳定直链，未下载 shp，未计算 2015、2017、2019 面积。
3. GMW v3.0 C 类：Zenodo 和 GEE asset 信息已登记；未下载全球 GeoTIFF/vector，未配置 GEE 授权，未发现海南 25 个 `t_region` 边界文件。
4. GMW v4.0.19 D 类：Zenodo 国家面积 xlsx 直链已登记；本轮未下载并解析 xlsx，未提取 China 行。GEE 2020 raster/vector asset 已登记。
5. 中国 2019 G 类：OpenAIRE 页面可核验论文题名、DOI、方法和精度；未找到海南面积表和可直接下载的空间数据文件。

## 无法核验或需人工确认的面积值

1. B 类 GF-2 2015、2017、2019 不存在页面面积表，需下载 shp 后计算。
2. C 类 GMW v3 海南省、市县、25 个 `t_region` 面积需基于边界和栅格/矢量计算。
3. D 类 GMW v4.0.19 海南省、市县 2020 面积需基于 10 m 栅格/矢量和边界计算。
4. G 类中国 2019 海南面积未找到明确表格，需数据下载后计算。

## 环境与数据边界阻塞

本地 Python 环境存在 `geopandas`、`rasterio`、`fiona`、`shapely`。当前仓库未发现 `.shp`、`.geojson`、`.gpkg` 行政区或保护地边界文件。GIS 依赖可用，输入空间数据缺失。

GEE 任务需要 Earth Engine 授权、项目配置和海南区域边界。当前环境未配置 GEE 授权。

## 地名与口径人工确认

1. `Maniaogang` / `Maliaogang` 与“马袅港”需统一拼写。seed_records 暂用用户种子中的 `Maniaogang`，备注保留 `Maliaogang` 论文拼写。
2. `Xinyingwan` 新盈湾与论文数据集组成中出现的 `Yangpugang` 洋浦港需核对。seed_records 暂按用户种子录入新盈湾，不做名称替换。
3. A 类 `Total` 为七个样区合计，需在前端和入库层禁用“海南省总面积”解释。

## 后续执行建议

1. 获取海南省、市县、保护地边界文件，并写入独立 boundary staging 目录。
2. 下载 CASEarth B 类 GF-2 shp 后，统一投影至等面积 CRS，按省、市县、重点样区计算面积。
3. 使用 GEE `projects/earthengine-legacy/assets/projects/sat-io/open-datasets/GMW/extent/GMW_V3` 对 11 期执行 `reduceRegions`。
4. 使用 GMW v4.0.19 `projects/sat-io/open-datasets/GMW/annual-extent/GMW_MNG_2020` 或 vector asset 计算 2020 年海南面积。
5. 找到中国 2019 数据下载页后，按海南省界计算面积，再写入 `hainan_mangrove_cover_computed_records.csv`。
