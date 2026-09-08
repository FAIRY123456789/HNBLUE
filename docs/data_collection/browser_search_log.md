# HNBLUE V2.0 公开数据浏览器检索日志

检索时间：2026-05-25
访问时区：Asia/Shanghai
检索目标：海南蓝碳、海南红树林、文昌示范区、红树林碳储量、遥感产品、通量或替代通量、物种与异速生长方程、蓝碳政策和方法学来源。

## 计划检索关键词

| 关键词 | 优先站点 | 目标数据类型 |
|---|---|---|
| 海南 红树林 碳储量 | hainan.gov.cn, cnki, mdpi, frontiers | 文献碳储量、政府统计 |
| 海南 红树林 土壤有机碳 | mdpi, jorae.cn, 期刊官网 | SOC、样方或区域均值 |
| 文昌 红树林 碳储量 | wenchang.hainan.gov.cn, hainan.gov.cn, 学术站点 | 文昌或八门湾数据 |
| 海南 蓝碳 数据 | hainan.gov.cn, hnlyj.hainan.gov.cn | 蓝碳试点、调查数据 |
| 海南 红树林 遥感 | GMW, GEE, NASA, USGS | 红树林分布、NDVI、GPP/NPP |
| Hainan mangrove carbon stock | Google Scholar, Frontiers, MDPI | 碳储量论文 |
| Hainan mangrove biomass carbon | 期刊官网, ResearchGate 摘要页 | 生物量碳、模型参考 |
| Hainan mangrove soil organic carbon | MDPI, PubMed, 期刊官网 | 土壤有机碳 |
| Wenchang mangrove carbon | Hainan government, MDPI | 文昌区域碳储量 |
| Global Mangrove Watch Hainan | globalmangrovewatch.org, Zenodo | 红树林分布产品 |
| Sentinel-2 mangrove Hainan | GEE Data Catalog, ESA/Copernicus | 遥感影像产品 |
| MODIS GPP Hainan mangrove | GEE Data Catalog, NASA LP DAAC | 替代通量指标 |
| China mangrove allometric equation | 论文页面, 数据库页面 | 异速生长方程 |
| mangrove carbon flux Hainan | 期刊官网, 数据中心 | CO2/CH4/N2O、NEP/GPP |
| blue carbon China Hainan dataset | Frontiers, 政府站点, 数据平台 | 省级清单、方法学 |

## 已打开和核验页面

| 序号 | URL | 页面标题或来源 | 是否找到原始数据 | 是否有下载链接 | 是否有许可证说明 | 是否进入来源目录 | 排除或限制原因 |
|---|---|---|---|---|---|---|---|
| 1 | https://www.forestry.gov.cn/ | 国家林草局检索到《海南省红树林保护规划》相关页面 | 是，政策规划摘要 | 否 | 未见明确开放许可 | 是 | 作为政策和区域背景，不作为核心数值入库 |
| 2 | https://www.hainan.gov.cn/hainan/zmgyshj/202307/3f612bd40e1941a080bf7abef3b71c69.shtml | 海南省政府英文网，海南蓝碳试点与调查信息 | 是，披露样方、沉积物柱样、无人机遥感调查信息 | 否 | 未见数据再分发许可 | 是 | 只记录项目级事实和待接入线索 |
| 3 | https://en.hainan.gov.cn/hainan/zdjsxm/202510/d9dd6ae78ac74071aa8995d56dd633e7.shtml | 海南省政府英文网，红树林保护与修复项目 | 是，披露红树林保护修复项目信息 | 否 | 未见数据再分发许可 | 是 | 用于政策和修复工程背景 |
| 4 | https://globalmangrovewatch.org/ | Global Mangrove Watch | 是，全球红树林分布平台 | 是 | 按数据集页面许可复核 | 是 | 大体量空间数据不下载入仓库 |
| 5 | https://zenodo.org/records/6894273 | Global Mangrove Watch 1996-2020 Version 3.0 | 是，正式数据集 | 是 | Zenodo 页面提供许可和 DOI | 是 | 只登记元数据和冷数据路径 |
| 6 | https://developers.google.com/earth-engine/datasets/catalog/COPERNICUS_S2_SR_HARMONIZED | GEE Sentinel-2 SR Harmonized | 是，影像产品元数据 | 可通过 GEE 使用 | 页面说明使用条款 | 是 | 不下载影像，后续 GEE 计算区域统计 |
| 7 | https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MOD17A3HGF | GEE MODIS MOD17A3HGF GPP/NPP | 是，年度 GPP/NPP 产品 | 可通过 GEE 使用 | 页面说明 NASA LP DAAC 数据政策 | 是 | 作为替代通量指标，标记 is_proxy=1 |
| 8 | https://www.usgs.gov/landsat-missions/landsat-collection-2-level-2-science-products | USGS Landsat Collection 2 Level-2 | 是，Landsat 地表反射率产品 | 是 | USGS/NASA 开放数据政策 | 是 | 不下载影像，记录元数据 |
| 9 | https://daac.ornl.gov/ | NASA ORNL DAAC 全球红树林生物量或冠层高度数据检索页 | 是，数据平台级来源 | 是 | ORNL DAAC 数据引用要求 | 是 | 具体产品需下一轮按 DOI 定位 |
| 10 | https://www.mdpi.com/2073-4441/14/20/3278 | MDPI Water，文昌八门湾红树林 SOC 论文 | 是，论文摘要和开放正文 | 是 | CC BY 许可 | 是 | 可录入文献记录，页码或表号需二次人工核对 |
| 11 | https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2022.932984/full | Frontiers，海南省级蓝碳温室气体清单论文 | 是，开放论文 | 是 | CC BY 许可 | 是 | 可作为省域清单和方法来源 |
| 12 | https://www.jorae.cn/CN/10.5814/j.issn.1674-764x.2022.03.010 | Journal of Resources and Ecology，海南红树林碳储量评估论文 | 是，论文摘要页 | 视期刊页面 | 期刊版权限制 | 是 | 不复制全文，仅记录题录和摘要级信息 |
| 13 | https://www.ipcc-nggip.iges.or.jp/public/wetlands/ | IPCC 2013 Wetlands Supplement | 是，方法学 | 是 | IPCC 使用条款 | 是 | 作为知识库和方法标准，不作为数值入库 |
| 14 | https://www.thebluecarboninitiative.org/manual | Blue Carbon Initiative Coastal Blue Carbon Manual | 是，方法手册 | 是 | 页面许可需复核 | 是 | 作为测量方法参考 |
| 15 | https://verra.org/methodologies/vm0033-methodology-for-tidal-wetland-and-seagrass-restoration-v2-1/ | Verra VM0033 | 是，蓝碳核证方法学 | 是 | Verra 文档条款 | 是 | 作为核证框架参考 |
| 16 | https://github.com/dfalster/baad | BAAD 数据和代码仓库 | 是，生物量异速生长数据库 | 是 | 仓库许可和论文引用需保留 | 是 | 可做模型参考，不直接作为海南实测数据 |
| 17 | https://zenodo.org/ | Tallo 全球树高和冠层数据集检索页 | 是，公开数据平台 | 是 | 具体记录许可需复核 | 是 | 需下一轮锁定版本 DOI |
| 18 | https://github.com/ | 公开 GEE 与红树林处理脚本检索 | 否，方法线索 | 视仓库 | 视仓库许可 | 否 | 未经核验数值不进入 seed 数据 |
