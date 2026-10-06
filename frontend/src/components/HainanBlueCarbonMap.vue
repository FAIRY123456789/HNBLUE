<template>
  <section class="map-explorer">
    <div class="map-board" aria-label="海南蓝碳市县地图">
      <div class="map-toolbar">
        <div class="segmented" role="group" aria-label="行政层级">
          <button type="button" :class="{ active: adminLevel === 'province' }" @click="setAdminLevel('province')">省级</button>
          <button type="button" :class="{ active: adminLevel === 'county' }" @click="setAdminLevel('county')">市县级</button>
        </div>
        <div class="segmented" role="group" aria-label="地图范围">
          <button type="button" :class="{ active: mapScope === 'main' }" @click="setMapScope('main')">海南主岛</button>
          <button type="button" :class="{ active: mapScope === 'sansha' }" @click="setMapScope('sansha')">三沙市</button>
        </div>
      </div>
      <div v-if="adminLevel === 'county'" class="metric-strip" role="group" aria-label="地图指标">
        <button
          v-for="metric in availableMetrics"
          :key="metric.key"
          type="button"
          :class="{ active: metricKey === metric.key }"
          @click="metricKey = metric.key"
        >
          {{ metric.label }}
        </button>
      </div>
      <div ref="chartRef" class="map-chart"></div>
      <p class="boundary-note">行政区名称与代码依据民政部公开资料整理；地图边界用于空间定位和市县资料查询。</p>
    </div>

    <article class="region-panel region-panel--compact">
      <span class="eyebrow">{{ panelEyebrow }}</span>
      <h3>{{ activeProfile.region_name }}</h3>
      <div class="profile-pills" aria-label="行政单元信息">
        <span>{{ displayValue(activeProfile.region_code) }}</span>
        <span>{{ regionTypeLabel(activeProfile.region_type) }}</span>
        <span>{{ coastalLabel(activeProfile.coastal_status) }}</span>
      </div>
      <p class="summary">{{ activeProfile.basic_description }}</p>

      <section class="info-section">
        <h4>生态与蓝碳线索</h4>
        <dl class="info-list compact-list">
          <div><dt>生态特征</dt><dd>{{ displayValue(activeProfile.main_ecosystem_features) }}</dd></div>
          <div><dt>湿地或保护地</dt><dd>{{ displayValue(activeProfile.wetland_or_protected_area) }}</dd></div>
          <div><dt>红树林状态</dt><dd>{{ displayValue(activeProfile.mangrove_status) }}</dd></div>
          <div v-if="hasVerifiedMangroveArea(activeProfile)"><dt>已核验红树林面积</dt><dd>{{ verifiedMangroveText(activeProfile) }}</dd></div>
          <div class="wide"><dt>研究或项目线索</dt><dd>{{ researchClueText(activeProfile) }}</dd></div>
        </dl>
      </section>

      <section class="info-section">
        <h4>来源与状态</h4>
        <dl class="info-list compact-list">
          <div><dt>主要来源</dt><dd>{{ displayValue(activeProfile.primary_source_title) }}</dd></div>
          <div><dt>来源机构</dt><dd>{{ displayValue(activeProfile.primary_source_institution) }}</dd></div>
          <div><dt>资料年份</dt><dd>{{ sourceYearText(activeProfile) }}</dd></div>
          <div><dt>数据状态</dt><dd>{{ dataStatusText(activeProfile) }}</dd></div>
        </dl>
        <div class="source-actions">
          <a v-if="activeProfile.primary_source_url" class="source-link" :href="activeProfile.primary_source_url" target="_blank" rel="noreferrer">查看主要来源</a>
          <a v-if="activeProfile.secondary_source_url" class="source-link ghost" :href="activeProfile.secondary_source_url" target="_blank" rel="noreferrer">查看市县来源</a>
        </div>
      </section>

    </article>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';
import hainanGeo from '@/assets/geo/hainan.json';
import { publicUrl } from '@/utils/urls';

const chartRef = ref(null);
const adminLevel = ref('county');
const mapScope = ref('main');
const metricKey = ref('basic');
const cityCountyGeo = ref(null);
const profiles = ref([]);
const activeCode = ref('469005');
let chart = null;
let themeObserver = null;

function isDarkTheme() {
  return document.documentElement.dataset.hnTheme === 'dark' || document.documentElement.classList.contains('theme-dark');
}

function mapPalette() {
  return isDarkTheme()
    ? {
        low: '#15221f', mid: '#28473f', high: '#47796f', missing: '#11191a',
        label: '#aebbb8', legend: '#7f918d', border: '#05090a', emphasis: '#765f3c',
        emphasisLabel: '#e0e5e3', tooltip: '#080e10', tooltipBorder: '#2a3d39',
      }
    : {
        low: '#d8f1eb', mid: '#6bb9a8', high: '#0d6b57', missing: '#d8dedb',
        label: '#123f37', legend: '#31554d', border: '#ffffff', emphasis: '#d99b35',
        emphasisLabel: '#072c27', tooltip: '#ffffff', tooltipBorder: '#c7ddd7',
      };
}

const metrics = [
  { key: 'basic', label: '基础信息', unit: '', field: null },
  { key: 'mangrove_area_ha', label: '红树林面积', unit: 'ha', field: 'mangrove_area_ha' },
  { key: 'database_record_count', label: '区域记录数', unit: '条', field: 'database_record_count' },
  { key: 'indicator_record_count', label: '指标记录数', unit: '条', field: 'indicator_record_count' },
  { key: 'literature_record_count', label: '文献记录数', unit: '条', field: 'literature_record_count' },
  { key: 'source_count', label: '来源数量', unit: '条', field: 'source_count' },
  { key: 'latest_source_year', label: '最近年份', unit: '年', field: 'latest_source_year' },
];

const profileByCode = computed(() => Object.fromEntries(profiles.value.map((item) => [String(item.region_code), item])));
const availableMetrics = computed(() => metrics.filter((metric) => !metric.field || profiles.value.some((item) => isNumeric(item[metric.field]))));
const selectedMetric = computed(() => metrics.find((item) => item.key === metricKey.value) || metrics[0]);
const panelEyebrow = computed(() => adminLevel.value === 'county' ? '市县级蓝碳资料' : '省级地图概览');

const activeProfile = computed(() => {
  if (adminLevel.value === 'province') {
    return {
      region_code: '460000',
      region_name: '海南省',
      region_type: 'province',
      coastal_status: 'coastal',
      basic_description: '省级视图保留海南主岛和三沙市范围切换，用于查看整体空间位置。市县级资料、来源证据和蓝碳相关字段请切换至市县级后点击具体行政单元。',
      main_ecosystem_features: '热带海岛、海岸带、河口湿地、红树林和海洋生态系统',
      wetland_or_protected_area: '省域多处滨海湿地、红树林保护地和近岸生态空间',
      mangrove_status: '省域存在红树林分布，市县级数值需按已核验资料查看',
      blue_carbon_evidence: '省域蓝碳研究与项目线索已在市县级资料中索引',
      primary_source_title: '海南省人民政府门户网站',
      primary_source_institution: '海南省人民政府',
      primary_source_url: 'https://www.hainan.gov.cn/',
      source_count: 1,
      data_quality_grade: 'A for government source',
    };
  }
  return profileByCode.value[String(activeCode.value)] || profiles.value[0] || { region_name: '暂无数据', basic_description: '市县资料正在读取。' };
});

const countyGeoForScope = computed(() => {
  const source = cityCountyGeo.value;
  if (!source?.features) return null;
  const features = source.features
    .filter((feature) => mapScope.value === 'sansha' ? String(feature.properties?.regionCode) === '460300' : String(feature.properties?.regionCode) !== '460300')
    .map((feature) => ({
      ...feature,
      properties: {
        ...feature.properties,
        name: feature.properties?.regionName || feature.properties?.name,
      },
    }));
  return { ...source, features };
});

const provinceGeoForScope = computed(() => {
  const features = (hainanGeo.features || []).filter((feature) => {
    const name = feature.properties?.name || feature.properties?.fullname || '';
    return mapScope.value === 'sansha' ? name.includes('三沙') : !name.includes('三沙');
  });
  return { ...hainanGeo, features };
});

const currentGeo = computed(() => adminLevel.value === 'county' ? countyGeoForScope.value : provinceGeoForScope.value);
const mapName = computed(() => `${adminLevel.value}-${mapScope.value}-hainan-blue-carbon`);

function isNumeric(value) {
  return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value));
}

function displayValue(value) {
  return value === null || value === undefined || value === '' ? '暂无数据' : value;
}

function countText(value) {
  return isNumeric(value) ? `${Number(value).toLocaleString('zh-CN')} 条` : '暂无数据';
}

function areaText(profile) {
  return isNumeric(profile.administrative_area_km2) ? `${Number(profile.administrative_area_km2).toLocaleString('zh-CN', { maximumFractionDigits: 2 })} km²` : '暂无数据';
}

function mangroveAreaText(profile) {
  return isNumeric(profile.mangrove_area_ha) ? `${Number(profile.mangrove_area_ha).toLocaleString('zh-CN', { maximumFractionDigits: 2 })} ha` : '暂无已核验面积';
}

function coastalLabel(value) {
  if (value === 'coastal') return '沿海';
  if (value === 'inland') return '内陆';
  return displayValue(value);
}

function regionTypeLabel(value) {
  const labels = {
    province: '省级',
    prefecture_city: '地级市',
    direct_county_level_city: '省直辖县级市',
    direct_county_level_county: '省直辖县',
    direct_county_level_autonomous_county: '省直辖自治县',
  };
  return labels[value] || displayValue(value);
}

function matchLabel(value) {
  const labels = { exact: '正式名称匹配', alias: '别名匹配', unmatched: '暂无既有库匹配' };
  return labels[value] || displayValue(value);
}

function verifiedMangroveText(profile) {
  if (!hasVerifiedMangroveArea(profile)) return '暂无已核验面积';
  const value = Number(profile.mangrove_area_ha).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  const year = profile.latest_source_year || profile.administrative_area_year;
  return year ? `${value} ha?${year}?` : `${value} ha`;
}

function hasVerifiedMangroveArea(profile) {
  return isNumeric(profile?.mangrove_area_ha);
}

function researchClueText(profile) {
  return displayValue(profile.blue_carbon_project_or_study || profile.blue_carbon_evidence);
}

function sourceYearText(profile) {
  return displayValue(profile.latest_source_year || profile.administrative_area_year);
}

function dataStatusText(profile) {
  if (profile?.data_quality_grade) return profile.data_quality_grade;
  if (profile?.primary_source_title || profile?.primary_source_institution) return '已有来源，需按用途复核';
  return '暂无可用来源';
}

function metricValue(profile) {
  const metric = selectedMetric.value;
  if (!metric.field) return 1;
  return isNumeric(profile?.[metric.field]) ? Number(profile[metric.field]) : null;
}

function mapData() {
  if (adminLevel.value === 'province') {
    return (currentGeo.value?.features || []).map((feature) => ({ name: feature.properties?.name || feature.properties?.fullname, value: 1 }));
  }
  return (currentGeo.value?.features || []).map((feature) => {
    const code = String(feature.properties?.regionCode || '');
    const profile = profileByCode.value[code];
    const value = metricValue(profile);
    return {
      name: feature.properties?.regionName,
      value: value === null ? undefined : value,
      regionCode: code,
      itemStyle: value === null ? { areaColor: mapPalette().missing } : undefined,
    };
  });
}

function valueRange() {
  const values = mapData().map((item) => item.value).filter((value) => Number.isFinite(Number(value))).map(Number);
  if (!values.length || metricKey.value === 'basic') return null;
  return { min: Math.min(...values), max: Math.max(...values) };
}

function setAdminLevel(level) {
  adminLevel.value = level;
  nextTick(renderMap);
}

function setMapScope(scope) {
  mapScope.value = scope;
  if (adminLevel.value === 'county') activeCode.value = scope === 'sansha' ? '460300' : '469005';
  nextTick(renderMap);
}

function tooltip(params) {
  if (adminLevel.value === 'province') return `${params.name}<br/>切换至市县级查看资料`;
  const code = params.data?.regionCode;
  const profile = profileByCode.value[String(code)] || {};
  const metric = selectedMetric.value;
  const value = metric.field ? metricValue(profile) : null;
  const metricLine = metric.field ? `${metric.label}：${value === null ? '暂无数据' : `${value} ${metric.unit}`}` : '点击查看市县资料';
  return `${displayValue(profile.region_name || params.name)}<br/>代码：${displayValue(code)}<br/>${metricLine}`;
}

function renderMap() {
  if (!chartRef.value || !currentGeo.value?.features?.length) return;
  if (!chart) {
    chart = echarts.init(chartRef.value);
    chart.on('click', (params) => {
      if (adminLevel.value === 'county' && params.data?.regionCode) activeCode.value = String(params.data.regionCode);
    });
  }
  echarts.registerMap(mapName.value, currentGeo.value);
  const range = valueRange();
  const palette = mapPalette();
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item', formatter: tooltip,
      backgroundColor: palette.tooltip,
      borderColor: palette.tooltipBorder,
      textStyle: { color: palette.label },
    },
    visualMap: range ? {
      min: range.min,
      max: range.max === range.min ? range.min + 1 : range.max,
      text: [selectedMetric.value.unit || '高', '低'],
      calculable: false,
      left: 14,
      bottom: 54,
      inRange: { color: [palette.low, palette.mid, palette.high] },
      textStyle: { color: palette.legend },
    } : undefined,
    series: [{
      name: adminLevel.value === 'county' ? '海南市县级蓝碳资料' : '海南省级地图',
      type: 'map',
      map: mapName.value,
      data: mapData(),
      roam: true,
      scaleLimit: { min: 0.85, max: 8 },
      selectedMode: false,
      layoutCenter: ['55%', adminLevel.value === 'county' && mapScope.value === 'sansha' ? '56%' : '58%'],
      layoutSize: mapScope.value === 'sansha' ? '88%' : '96%',
      labelLayout: { hideOverlap: true },
      label: { show: adminLevel.value === 'county', color: palette.label, fontSize: 11, fontWeight: 800 },
      itemStyle: { borderColor: palette.border, borderWidth: 1.2, areaColor: palette.low },
      emphasis: {
        label: { show: true, color: palette.emphasisLabel, fontWeight: 900 },
        itemStyle: { areaColor: palette.emphasis, borderColor: palette.border, borderWidth: 2 },
      },
    }],
  }, true);
}

async function loadAssets() {
  const [geoResponse, profileResponse] = await Promise.all([
    fetch(publicUrl('/data/hainan-map/hainan_city_county_web.geojson'), { cache: 'no-cache' }),
    fetch(publicUrl('/data/hainan-map/hainan_city_county_profile.json'), { cache: 'no-cache' }),
  ]);
  cityCountyGeo.value = await geoResponse.json();
  profiles.value = await profileResponse.json();
  await nextTick();
  renderMap();
}

function resizeMap() {
  chart?.resize();
}

watch([adminLevel, mapScope, metricKey, cityCountyGeo, profiles], () => nextTick(renderMap));

onMounted(() => {
  loadAssets();
  themeObserver = new MutationObserver(() => renderMap());
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'data-hn-theme'] });
  window.addEventListener('resize', resizeMap);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeMap);
  themeObserver?.disconnect();
  chart?.dispose();
});
</script>

<style scoped>
.map-explorer {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(340px, 0.85fr);
  gap: 16px;
  align-items: stretch;
}

.map-board {
  position: relative;
  min-height: 570px;
  overflow: hidden;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: linear-gradient(145deg, var(--hn-bg-soft), var(--hn-soft) 52%, var(--hn-bg-soft));
  box-shadow: var(--hn-shadow-soft);
}

.map-board::before {
  content: '海南蓝碳市县地图';
  position: absolute;
  left: 16px;
  top: 14px;
  z-index: 2;
  color: var(--hn-muted);
  font-size: 12px;
  font-weight: 900;
}

.map-toolbar {
  position: absolute;
  right: 14px;
  top: 12px;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.segmented,
.metric-strip {
  display: inline-flex;
  gap: 6px;
  padding: 5px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-control-bg);
  box-shadow: var(--hn-shadow-soft);
}

.segmented button,
.metric-strip button {
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: var(--hn-text);
  cursor: pointer;
  font-weight: 900;
}

.segmented button.active,
.metric-strip button.active {
  border-color: var(--hn-accent);
  background: var(--hn-accent);
  color: var(--hn-on-accent);
}

.metric-strip {
  position: absolute;
  left: 14px;
  right: 14px;
  top: 60px;
  z-index: 3;
  overflow-x: auto;
  justify-content: flex-start;
}

.metric-strip button { white-space: nowrap; }

.map-chart {
  position: absolute;
  inset: 108px 18px 58px;
}

.boundary-note {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 12px;
  margin: 0;
  color: var(--hn-muted);
  font-size: 12px;
  line-height: 1.5;
}

.region-panel {
  min-height: auto;
  padding: 14px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel-solid);
  box-shadow: var(--hn-shadow-soft);
  overflow: hidden;
}

.eyebrow {
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 800;
}

h3 {
  margin: 6px 0 7px;
  color: var(--hn-text);
  font-size: 20px;
  line-height: 1.25;
}

.profile-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.profile-pills span {
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--hn-border);
  border-radius: 7px;
  background: var(--hn-soft);
  color: var(--hn-muted);
  font-size: 12px;
  font-weight: 700;
}

.summary,
.status-note {
  margin: 0;
  color: var(--hn-muted);
  line-height: 1.52;
  font-size: 13px;
  font-weight: 400;
}

.info-section {
  margin-top: 11px;
  padding-top: 10px;
  border-top: 1px solid var(--hn-border);
}

.info-section h4 {
  margin: 0 0 5px;
  color: var(--hn-text);
  font-size: 13px;
  font-weight: 800;
}

.info-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
  margin: 0;
}

.info-list div {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--hn-border) 68%, transparent);
}

.info-list .wide { grid-column: 1 / -1; }

.info-list div:last-child { border-bottom: 0; }

dt {
  color: var(--hn-muted);
  font-size: 11px;
  font-weight: 700;
}

dd {
  min-width: 0;
  margin: 0;
  color: var(--hn-text);
  font-size: 13px;
  line-height: 1.42;
  font-weight: 400;
  overflow-wrap: anywhere;
}

.source-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.source-link {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 7px;
  background: var(--hn-accent);
  color: var(--hn-on-accent);
  text-decoration: none;
  font-weight: 800;
  font-size: 12px;
}

.source-link.ghost {
  border: 1px solid var(--hn-border);
  background: var(--hn-soft);
  color: var(--hn-accent);
}

.status-note {
  margin-top: 14px;
  padding: 10px;
  border-radius: 8px;
  background: var(--hn-soft);
  font-size: 12px;
}

:global(:root[data-hn-theme="dark"] .segmented button.active),
:global(:root.theme-dark .segmented button.active),
:global(:root[data-hn-theme="dark"] .metric-strip button.active),
:global(:root.theme-dark .metric-strip button.active),
:global(:root[data-hn-theme="dark"] .source-link),
:global(:root.theme-dark .source-link) {
  background: var(--hn-surface-strong);
  color: var(--hn-text);
}

@media (max-width: 980px) {
  .map-explorer { grid-template-columns: 1fr; }
  .map-board { min-height: 560px; }
}

@media (max-width: 620px) {
  .map-board { min-height: 620px; }
  .map-toolbar { left: 10px; right: 10px; justify-content: flex-start; }
  .metric-strip { top: 104px; }
  .map-chart { inset: 158px 8px 68px; }
  .info-list div { grid-template-columns: 1fr; gap: 4px; }
}
</style>
