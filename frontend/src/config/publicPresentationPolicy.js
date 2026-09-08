export const publicPageLabels = {
  home: "海南蓝碳应用系统",
  carbonSeek: "碳溯",
  publicData: "数据资产",
  governance: "治理工作台",
  about: "关于项目",
};

export const metricDisplayRules = {
  data_source: { title: "可信数据来源", unit: "项", description: "汇聚公开遥感、政府或权威机构资料、同行评审文献和模型基础数据" },
  region_metric: { title: "区域观察与评价指标", unit: "条", description: "用于区域比较、变化识别、生态状态分析和治理研判" },
  indicator_dictionary: { title: "标准化指标体系", unit: "项", description: "统一指标名称、单位、统计口径和来源说明，降低跨来源误读" },
  mangrove_cover: { title: "红树林覆盖记录", unit: "条", description: "用于查看区域红树林面积、年份变化和来源依据" },
  literature_carbon: { title: "文献证据", unit: "条", description: "为碳密度、碳储和模型参数提供可追溯依据" },
  area_points: { title: "面积观测记录", unit: "条", description: "按区域和年份组织红树林面积时序记录" },
  foundation: { title: "多源数据协同底座", unit: "", description: "支撑数据查询、来源核验、模型分析与辅助解释" },
};

export const sourceTypeLabels = {
  "peer-reviewed study": "同行评审研究",
  "peer_reviewed_study_area_text": "同行评审研究",
  "peer-reviewed remote sensing": "同行评审遥感研究",
  "peer-reviewed remote sensing table": "同行评审遥感研究",
  "peer_reviewed_remote_sensing_table": "同行评审遥感研究",
  "peer_reviewed_remote_sensing_model_table": "同行评审遥感模型研究",
  "peer_reviewed_inventory_table": "同行评审清单研究",
  "study area record": "研究区域记录",
  "study_area_record": "研究区域记录",
  "allometry method literature": "异速生长模型文献",
  "allometry_method_literature": "异速生长模型文献",
  "remote sensing table": "遥感观测资料",
  "remote sensing dataset": "遥感数据产品",
  "remote_sensing_dataset": "遥感数据产品",
  "normalized table": "标准化数据记录",
  "government record": "管理部门资料",
  "government_record": "管理部门资料",
  "public dataset": "公开数据集",
  "public_dataset": "公开数据集",
  "model-derived": "模型计算结果",
  "model_derived": "模型计算结果",
  "field observation": "实地观测资料",
  "field_observation": "实地观测资料",
  "literature parameter": "文献参数依据",
  "literature_parameter": "文献参数依据",
  literature: "学术文献",
  dataset: "公开数据集",
  agency: "权威机构资料",
  government: "管理部门资料",
  model: "模型计算结果",
};

export const sourceNameLabels = {
  GMW: "全球红树林监测数据（GMW）",
  WDPA: "世界保护地数据库（WDPA）",
  BAAD: "全球植物异速生长数据库（BAAD）",
  Tallo: "全球树木尺度与异速生长数据集（Tallo）",
  ChinAllomeTree: "中国树木异速生长数据库",
  "NASA SRTM": "NASA 航天雷达地形数据",
  "Copernicus DEM": "哥白尼全球高程数据",
  PSMSL: "国际平均海平面长期观测服务（PSMSL）",
  "BlueCarbon normalized": "标准化蓝碳公开数据包",
  SRC_LIT_BAMEN_SOC_2022: "八门湾红树林土壤有机碳研究",
  SRC_LIT_FORESTS_HI_MANGROVE_RS_2023: "海南岛红树林遥感变化研究",
  SRC_LIT_FRONTIERS_HI_LULC_INVEST_2024: "海南岛土地利用变化与碳储情景研究",
  SRC_LIT_HI_GHG_2022: "沿海蓝碳温室气体清单研究",
  SRC_ALLO_KOMIYAMA_2005: "红树林生物量异速生长方程研究",
  SRC_BAAD_BIOMASS_AND_ALLOMETRY_DATABASE: "全球植物异速生长数据库（BAAD）",
  SRC_GOV_HI_BLUE_CARBON_2023: "海南蓝碳事业发展公开资料",
  SRC_IPCC_WETLANDS_SUPPLEMENT: "IPCC 湿地补充指南",
  SRC_TALLO_GLOBAL_TREE_ALLOMETRY_DATABASE: "全球树木尺度与异速生长数据集（Tallo）",
};

export const dataStatusLabels = {
  audited: "已核验",
  connected: "已接入",
  pending: "来源信息待补充",
  empty: "暂无可用记录",
  proxy: "参考记录",
  direct: "直接证据",
  simulated: "数据属性需核验",
};

export const forbiddenPublicTerms = [
  "V1.0",
  "V2.0",
  "继承 V1",
  "数据来源待补充",
  "下一轮",
  "待迁入",
  "hnblue_v2_dev_control",
  "驾驶舱",
  "peer-reviewed remote sensing table",
  "allometry method literature",
  "data/staging",
  "flask_model",
];

export const publicFieldLabels = {
  source_code: "来源标识",
  source_id: "来源标识",
  source_name: "来源名称",
  source_type: "来源类型",
  source_url: "来源链接",
  url: "来源链接",
  citation: "引用信息",
  reference: "引用信息",
  record_count: "记录数量",
  proxy_count: "参考记录",
  direct_count: "直接证据",
  boundary_notes: "数据范围说明",
  is_simulated: "数据属性",
  indicator_code: "指标标识",
  indicator_name: "指标名称",
  indicator_label: "指标名称",
  indicator_dictionary: "标准指标目录",
  data_source: "数据来源目录",
  satellite_mangrove_cover: "红树林覆盖记录",
  region_metric: "区域观察指标",
  literature_carbon_record: "文献碳数据",
  metric_category: "指标类别",
  quality_flag: "质量说明",
  quality_level: "质量等级",
  method_type: "方法类型",
  data_type: "数据类型",
  carbon_pool: "碳库",
  region_name: "区域",
  region_id: "区域标识",
  year: "年份",
  year_or_period: "年份或时期",
  value: "数值",
  area_value: "面积",
  value_mg_ha: "数值",
  unit: "单位",
  notes: "说明",
  description: "说明",
};

const hiddenPublicFields = new Set([
  "id",
  "offset",
  "limit",
  "schema",
  "table",
  "table_name",
  "database",
  "db_name",
  "database_target",
  "locked_high_value_source",
  "payload",
  "raw",
  "raw_json",
]);

export function formatPublicSource(value, fallbackType = "") {
  const raw = String(value || "").trim();
  if (!raw) return fallbackType ? `未命名来源（${formatSourceType(fallbackType)}）` : "来源待补充";
  const known = sourceNameLabels[raw] || sourceNameLabels[raw.toUpperCase?.()] || sourceNameLabels[raw.replace(/\s+/g, " ").trim()];
  if (known) return known;
  return fallbackType ? `${raw}（${formatSourceType(fallbackType)}）` : raw;
}

export function formatSourceType(value) {
  const raw = String(value || "").trim();
  if (!raw) return "来源类型待补充";
  return sourceTypeLabels[raw] || sourceTypeLabels[raw.toLowerCase()] || raw;
}

export function formatMetricValue(value, unit = "") {
  if (value === undefined || value === null || value === "") return "-";
  const display = typeof value === "number" ? value.toLocaleString("zh-CN") : String(value);
  return unit ? `${display} ${unit}` : display;
}

export function getMetricDescription(key) {
  return metricDisplayRules[key]?.description || "用于支撑来源核验、区域比较和治理研判";
}

export function getMetricRule(key) {
  return metricDisplayRules[key] || { title: key, unit: "", description: getMetricDescription(key) };
}

export function formatPublicValue(key, value) {
  if (value === undefined || value === null || value === "") return "-";
  if (key === "source_type" || key === "data_type" || key === "method_type") return formatSourceType(value);
  if (key === "source_code" || key === "source_id") return formatPublicSource(value);
  if (key === "is_simulated") return Number(value) === 1 || value === true ? "模型或情景记录" : "可追溯记录";
  if (typeof value === "object") return "";
  return String(value);
}

export function formatRecordForPublic(record = {}) {
  return Object.entries(record)
    .filter(([key, value]) => !hiddenPublicFields.has(key) && value !== undefined && value !== null && value !== "")
    .map(([key, value]) => ({
      key,
      label: publicFieldLabels[key] || key.replaceAll("_", " "),
      value: formatPublicValue(key, value),
    }))
    .filter((item) => item.value !== "");
}
