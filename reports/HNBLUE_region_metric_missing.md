# HNBLUE V2.0 区域指标缺失与计算阻塞报告

## 当前环境判断

本轮未写入 MySQL，未修改 DDL、后端、前端或旧库。

Python GIS 依赖：`geopandas`、`rasterio`、`fiona`、`shapely` 可用。

GEE 依赖：`earthengine-api` 未安装，GEE 授权未配置。

区域边界：仓库内未发现 `.shp`、`.geojson`、`.gpkg` 边界文件；仅发现前端展示 JSON，不能作为 25 个 `t_region` 精确边界。

## 数据源访问状态

已补齐 10 类数据源的 GEE Data Catalog 或官方数据页面 URL。当前未实际调用 GEE asset，无法验证资产读取权限。

若后续网络、账号或 GEE 项目权限受限，以下 asset 可能无法调用：`COPERNICUS/S2_SR_HARMONIZED`、`GOOGLE/DYNAMICWORLD/V1`、`ESA/WorldCover/v200`、`MODIS/061/MOD17A3HGF`、`MODIS/061/MOD13Q1`、`ECMWF/ERA5_LAND/HOURLY`、`UCSB-CHG/CHIRPS/DAILY`、`NOAA/VIIRS/DNB/ANNUAL_V22`、`JRC/GSW1_4/GlobalSurfaceWater`、`USGS/SRTMGL1_003`。

## 无法完成的计算任务

1. Sentinel-2 2018-2024 年 25 个 `t_region` 区域指数：缺少 GEE 授权和区域边界。
2. Dynamic World 2018-2024 年土地覆盖概率与主导类别面积：缺少 GEE 授权和区域边界。
3. MODIS MOD17 2001-2024 年 GPP/NPP：缺少 GEE 授权和区域边界。
4. ERA5-Land 2001-2024 年气候指标：缺少 GEE 授权、本地 ERA5 文件和区域边界。
5. VIIRS 2012-2024 年夜间灯光指标：缺少 GEE 授权和区域边界。
6. ESA WorldCover 2021 土地覆盖面积：缺少 GEE 授权或本地栅格，以及区域边界。
7. MODIS MOD13Q1 2001-2024 年 NDVI/EVI：缺少 GEE 授权和区域边界。
8. CHIRPS 2001-2024 年降水指标：缺少 GEE 授权或本地栅格，以及区域边界。
9. JRC Global Surface Water 静态水体指标：缺少 GEE 授权或本地栅格，以及区域边界。
10. SRTM 静态地形指标：缺少 GEE 授权或本地 DEM，以及区域边界。

## 需要人工确认的阈值

1. `viirs_ntl_lit_area_ha`：亮光面积阈值未确定。本轮将 `threshold_value` 标记为 `pending_threshold`，未计算。
2. `srtm_low_elevation_area_ha`：首批候选阈值为 10 m，需根据海南滨海地貌和潮间带研究口径校准。
3. `chirps_heavy_rain_days_50mm`：50 mm/d 为候选强降水阈值，需结合海南气象部门标准校准。
4. CHIRPS 雨季/旱季：当前候选为雨季 5-10 月、旱季 11-次年 4 月，需本地标准确认。

## 后续建议

1. 获取 25 个 `t_region`、海南省、市县和重点保护地边界文件，统一 CRS 与编码。
2. 配置 Earth Engine 账号和项目授权，安装 `earthengine-api`。
3. 先执行 high 优先级任务，生成可导入 `computed_records`。
4. 所有输出保留 `source_code`、`metric_code`、`quality_flag`、`is_proxy` 和单位换算说明。
