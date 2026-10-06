<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="数据资产"
      eyebrow="DataAssets"
      subtitle="使用完全合成的测试记录演示来源追溯、证据卡片、区域指标和面积时序的数据产品能力"
      :tags="['合成数据', '来源追溯', '证据状态', '字段契约']"
    />

    <section class="stat-row">
      <StatCard label="合成数据来源" :value="dataSources.length" unit="项" note="只演示来源字段、许可字段和追溯链路" />
      <StatCard label="测试指标记录" :value="remoteMetrics.length" unit="条" note="仅用于界面、筛选和图表联调" />
      <StatCard label="标准指标" :value="indicatorRows.length" unit="项" note="统一指标名称、单位、统计口径和来源说明" />
      <StatCard label="证据卡片" :value="literatureCards.length" unit="条" note="全部为虚构记录，不构成科研或业务依据" />
    </section>

    <section v-if="loadError" class="content-panel">
      <EmptyState title="数据包读取失败" :message="loadError" />
    </section>

    <section v-else-if="isLoading" class="content-panel">
      <EmptyState title="正在读取数据" message="正在读取公开数据资产包" />
    </section>

    <template v-else>
      <section class="content-panel dataset-panel">
        <SectionHeader title="外部数据集" eyebrow="External Datasets" subtitle="使用小型合成 CSV 演示字段结构、来源信息和分页浏览" />
        <ExternalDatasetGrid />
      </section>

      <section class="content-panel">
        <SectionHeader title="红树林面积" eyebrow="Area Records" subtitle="按区域浏览面积时序，图表容器保持稳定高度">
          <label class="select-pill">
            <span>区域</span>
            <select v-model="selectedRegionId">
              <option v-for="region in areaRegions" :key="region.region_id" :value="region.region_id">
                {{ region.region_name }} · {{ region.points?.length || 0 }} 条
              </option>
            </select>
          </label>
        </SectionHeader>
        <div class="chart-shell">
          <div ref="areaChartRef" class="chart"></div>
          <EmptyState v-if="!areaRegions.length" title="暂无面积记录" message="当前数据范围未覆盖红树林面积时序" />
        </div>
      </section>

      <section class="content-panel">
        <SectionHeader title="合成区域指标" eyebrow="Regional Metrics" subtitle="展示虚构指标记录，说明字段、单位、类型和来源契约">
          <label class="select-pill">
            <span>单位</span>
            <select v-model="selectedRemoteUnit">
              <option v-for="unit in remoteUnits" :key="unit" :value="unit">{{ unit }}</option>
            </select>
          </label>
        </SectionHeader>
        <div class="chart-shell wide">
          <div ref="remoteChartRef" class="chart"></div>
          <EmptyState v-if="!remoteMetrics.length" title="暂无指标记录" message="当前数据范围未覆盖区域指标" />
        </div>
        <div class="metric-table" v-if="visibleRemoteMetrics.length">
          <div class="metric-row head">
            <span>区域</span><span>年份</span><span>指标</span><span>数值</span><span>类型</span>
          </div>
          <div v-for="metric in visibleRemoteMetrics" :key="metric.record_id" class="metric-row">
            <span>{{ metric.region_name || '-' }}</span>
            <span>{{ metric.year_or_period || '-' }}</span>
            <span>{{ metric.indicator_label || metric.indicator_code || '-' }}</span>
            <span>{{ formatNumber(metric.value) }} {{ metric.unit || '' }}</span>
            <span><b :class="Number(metric.is_proxy) === 1 ? 'ref-tag' : 'direct-tag'">{{ Number(metric.is_proxy) === 1 ? '参考记录' : '直接证据' }}</b></span>
          </div>
        </div>
      </section>

      <section class="content-panel">
        <SectionHeader title="证据卡片" eyebrow="Evidence" subtitle="使用虚构证据记录演示引用、质量等级与追溯界面" />
        <div v-if="visibleLiteratureCards.length" class="card-grid">
          <article v-for="card in visibleLiteratureCards" :key="card.record_id" class="evidence-card">
            <div class="card-top">
              <span class="quality">质量 {{ card.quality_level || 'NA' }}</span>
              <span :class="Number(card.is_proxy) === 1 ? 'ref-tag' : 'direct-tag'">{{ Number(card.is_proxy) === 1 ? '参考记录' : '直接证据' }}</span>
            </div>
            <h3 :title="card.title || '未命名文献记录'">{{ card.title || '未命名文献记录' }}</h3>
            <p>{{ card.subtitle || card.notes || '暂无摘要' }}</p>
            <div class="value-line"><strong>{{ formatNumber(card.value) }}</strong><span>{{ card.unit || '' }}</span></div>
            <a v-if="card.source_url" :href="card.source_url" target="_blank" rel="noreferrer">查看来源</a>
          </article>
        </div>
        <EmptyState v-else title="暂无文献证据" message="当前数据范围未覆盖文献碳储卡片" />
      </section>
      <section class="content-panel source-panel">
        <SectionHeader title="来源追溯" eyebrow="Sources" subtitle="展示公开来源名称、类型、用途和记录数量，便于第三方查验" />
        <div v-if="dataSources.length" class="source-grid">
          <article v-for="source in dataSources" :key="source.source_id || source.source_name" class="source-card">
            <h3 :title="sourceDisplayName(source)">{{ sourceDisplayName(source) }}</h3>
            <p class="official-name" :title="source.source_name">{{ officialName(source) }}</p>
            <span class="source-type">{{ formatSourceType(source.source_type) }}</span>
            <dl class="source-meta">
              <div class="source-meta-row"><dt>主要用途</dt><dd :title="sourceUsage(source)">{{ sourceUsage(source) }}</dd></div>
              <div class="source-meta-row"><dt>覆盖范围</dt><dd :title="sourceCoverage(source)">{{ sourceCoverage(source) }}</dd></div>
            </dl>
            <div class="source-actions">
              <span>记录 {{ source.record_count || 0 }} 条</span>
              <span>来源 {{ source.direct_count || 0 }} 条</span>
              <a v-if="source.source_url" :href="source.source_url" target="_blank" rel="noreferrer" aria-label="打开来源链接">查看来源</a>
              <span v-else class="disabled-link">暂无链接</span>
            </div>
          </article>
        </div>
        <EmptyState v-else title="暂无来源记录" message="当前数据范围未覆盖来源清单" />
      </section>
    </template>
  </PageShell>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import PageShell from "@/components/common/PageShell.vue";
import UnifiedNav from "@/components/common/UnifiedNav.vue";
import PageHero from "@/components/common/PageHero.vue";
import SectionHeader from "@/components/common/SectionHeader.vue";
import StatCard from "@/components/common/StatCard.vue";
import EmptyState from "@/components/common/EmptyState.vue";
import ExternalDatasetGrid from "@/components/external-datasets/ExternalDatasetGrid.vue";
import { formatPublicSource, formatSourceType } from "@/config/publicPresentationPolicy";
import { publicUrl } from "@/utils/urls";

const DATA_BASE = publicUrl("/data/demo");
const isLoading = ref(true);
const loadError = ref("");
const areaData = ref({ regions: [] });
const remoteData = ref({ metrics: [] });
const literatureData = ref({ cards: [] });
const dataSources = ref([]);
const indicatorRows = ref([]);
const selectedRegionId = ref("");
const selectedRemoteUnit = ref("");
const areaChartRef = ref(null);
const remoteChartRef = ref(null);
let areaChart = null;
let remoteChart = null;

const areaRegions = computed(() => areaData.value.regions || []);
const remoteMetrics = computed(() => remoteData.value.metrics || []);
const literatureCards = computed(() => literatureData.value.cards || []);
const remoteUnits = computed(() => [...new Set(remoteMetrics.value.map((item) => item.unit).filter(Boolean))].sort((a, b) => a.localeCompare(b)));
const visibleRemoteMetrics = computed(() => remoteMetrics.value.filter((item) => !selectedRemoteUnit.value || item.unit === selectedRemoteUnit.value).slice(0, 12));
const visibleLiteratureCards = computed(() => literatureCards.value.slice(0, 9));

async function fetchJson(name) {
  const response = await fetch(`${DATA_BASE}/${name}`, { cache: "no-cache" });
  if (!response.ok) throw new Error(`读取 ${name} 失败：HTTP ${response.status}`);
  return response.json();
}

async function fetchText(name) {
  const response = await fetch(`${DATA_BASE}/${name}`, { cache: "no-cache" });
  if (!response.ok) throw new Error(`读取 ${name} 失败：HTTP ${response.status}`);
  return response.text();
}

function parseCsv(text) {
  const rows = [];
  let cell = "";
  let row = [];
  let quoted = false;
  const normalized = text.replace(/\r\n/g, "\n").replace(/\r/g, "\n");
  for (let i = 0; i < normalized.length; i += 1) {
    const char = normalized[i];
    const next = normalized[i + 1];
    if (char === '"' && quoted && next === '"') { cell += '"'; i += 1; }
    else if (char === '"') quoted = !quoted;
    else if (char === "," && !quoted) { row.push(cell); cell = ""; }
    else if (char === "\n" && !quoted) { row.push(cell); rows.push(row); row = []; cell = ""; }
    else cell += char;
  }
  if (cell || row.length) { row.push(cell); rows.push(row); }
  const [headers, ...body] = rows.filter((item) => item.length > 1 || item[0]);
  if (!headers) return [];
  return body.map((item) => Object.fromEntries(headers.map((header, index) => [header, item[index] ?? ""])));
}

function themeTextColor() {
  return getComputedStyle(document.documentElement).getPropertyValue('--hn-text').trim() || '#123f37';
}

function themeMutedColor() {
  return getComputedStyle(document.documentElement).getPropertyValue('--hn-muted').trim() || '#5b746d';
}

function emptyChart(instance, title) {
  instance.setOption({
    title: { text: title, left: "center", top: "middle", textStyle: { color: themeMutedColor(), fontSize: 15, fontWeight: 500 } },
    xAxis: { show: false },
    yAxis: { show: false },
    series: [],
  }, true);
}

function renderAreaChart() {
  if (!areaChartRef.value) return;
  if (!areaChart) areaChart = echarts.init(areaChartRef.value);
  const region = areaRegions.value.find((item) => item.region_id === selectedRegionId.value);
  const points = [...(region?.points || [])].sort((a, b) => Number(a.year) - Number(b.year));
  if (!points.length) { emptyChart(areaChart, "暂无面积时序"); return; }
  areaChart.setOption({
    tooltip: { trigger: "axis" },
    grid: { left: 56, right: 22, top: 34, bottom: 42 },
    xAxis: { type: "category", data: points.map((item) => item.year), axisLabel: { color: themeTextColor() } },
    yAxis: { type: "value", name: region?.unit || "数值", axisLabel: { color: themeTextColor() }, nameTextStyle: { color: themeTextColor() } },
    series: [{ name: region?.region_name || "面积", type: "line", smooth: true, symbolSize: 8, lineStyle: { width: 3, color: "#0d6b57" }, itemStyle: { color: "#0d6b57" }, areaStyle: { color: "rgba(13, 107, 87, 0.16)" }, data: points.map((item) => Number(item.value)) }],
  }, true);
}

function renderRemoteChart() {
  if (!remoteChartRef.value) return;
  if (!remoteChart) remoteChart = echarts.init(remoteChartRef.value);
  const rows = visibleRemoteMetrics.value;
  if (!rows.length) { emptyChart(remoteChart, "暂无区域指标"); return; }
  const unit = selectedRemoteUnit.value || rows[0]?.unit || "数值";
  remoteChart.setOption({
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: (params) => {
        const item = rows[params?.[0]?.dataIndex] || {};
        return `${shortRegionName(item.region_name)}<br/>指标：${item.indicator_label || item.indicator_code || '-'}<br/>年份：${item.year_or_period || '-'}<br/>数值：${formatNumber(item.value)} ${item.unit || unit}`;
      },
    },
    grid: { left: 78, right: 36, top: 42, bottom: 94, containLabel: true },
    xAxis: { type: "category", data: rows.map((item) => `${shortRegionName(item.region_name)}\n${item.year_or_period || '-'}`), axisLabel: { interval: 0, rotate: 30, color: themeTextColor() } },
    yAxis: { type: "value", name: unit, axisLabel: { color: themeTextColor() }, nameTextStyle: { color: themeTextColor() } },
    series: [{
      name: unit,
      type: "bar",
      barMaxWidth: 34,
      label: { show: true, position: "top", color: themeTextColor(), formatter: (params) => formatNumber(params.value) },
      data: rows.map((item) => ({ value: Number(item.value), itemStyle: { color: Number(item.is_proxy) === 1 ? "#d99b35" : "#1f8a7a" } })),
    }],
  }, true);
}

function shortRegionName(value) {
  return String(value || '-').replace(/^海南岛[-—·\s]*/, '');
}

function resizeCharts() { areaChart?.resize(); remoteChart?.resize(); }

async function loadData() {
  try {
    const [area, remote, literature, sourcesCsv, indicatorsCsv] = await Promise.all([
      fetchJson("frontend_mangrove_area_timeseries.json"),
      fetchJson("frontend_remote_sensing_metrics.json"),
      fetchJson("frontend_literature_carbon_cards.json"),
      fetchText("display_data_sources.csv"),
      fetchText("display_region_dictionary.csv"),
    ]);
    areaData.value = area;
    remoteData.value = remote;
    literatureData.value = literature;
    dataSources.value = parseCsv(sourcesCsv);
    indicatorRows.value = parseCsv(indicatorsCsv);
    selectedRegionId.value = [...areaRegions.value].sort((a, b) => (b.points?.length || 0) - (a.points?.length || 0))[0]?.region_id || "";
    selectedRemoteUnit.value = remoteUnits.value.includes("ha") ? "ha" : remoteUnits.value[0] || "";
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : String(error);
  } finally {
    isLoading.value = false;
    await nextTick();
    renderAreaChart();
    renderRemoteChart();
  }
}

function formatNumber(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return value ?? '-';
  return number.toLocaleString('zh-CN', { maximumFractionDigits: Math.abs(number) >= 10 ? 1 : 2 });
}
function sourceDisplayName(source) {
  const byId = {
    SRC_LIT_BAMEN_SOC_2022: '八门湾红树林土壤有机碳研究',
    SRC_LIT_FORESTS_HI_MANGROVE_RS_2023: '海南岛红树林遥感变化研究',
    SRC_LIT_FRONTIERS_HI_LULC_INVEST_2024: '海南岛土地利用变化与碳储情景研究',
    SRC_LIT_HI_GHG_2022: '沿海蓝碳温室气体清单研究',
    SRC_ALLO_KOMIYAMA_2005: '红树林生物量异速生长方程研究',
    SRC_BAAD_BIOMASS_AND_ALLOMETRY_DATABASE: '全球植物异速生长数据库（BAAD）',
    SRC_GOV_HI_BLUE_CARBON_2023: '海南蓝碳事业发展公开资料',
    SRC_IPCC_WETLANDS_SUPPLEMENT: 'IPCC 湿地补充指南',
    SRC_TALLO_GLOBAL_TREE_ALLOMETRY_DATABASE: '全球树木尺度与异速生长数据集（Tallo）',
  };
  return byId[source.source_id] || formatPublicSource(source.source_name || source.source_id, source.source_type);
}

function officialName(source) {
  const name = source.source_name || source.source_id || '官方名称待补充';
  return sourceDisplayName(source) === name ? formatSourceType(source.source_type) : name;
}

function sourceUsage(source) {
  const type = formatSourceType(source.source_type);
  if (type.includes('遥感')) return '支持区域覆盖、变化识别和遥感指标查验';
  if (type.includes('模型') || type.includes('异速')) return '支持生物量估算和模型参数说明';
  if (type.includes('管理')) return '支持政策资料和治理背景核验';
  return '支持文献证据、来源核验和参数查证';
}

function sourceCoverage(source) {
  if (source.source_id?.includes('TALLO') || source.source_id?.includes('BAAD')) return '全球公开记录';
  if (source.source_id?.includes('IPCC')) return '国际方法指南';
  if (source.source_id?.includes('HI') || source.source_id?.includes('BAMEN')) return '海南相关区域';
  return '多来源记录';
}

watch(selectedRegionId, () => nextTick(renderAreaChart));
watch(selectedRemoteUnit, () => nextTick(renderRemoteChart));
watch(visibleRemoteMetrics, () => nextTick(renderRemoteChart));

onMounted(() => {
  document.body.style.overflow = "auto";
  document.documentElement.style.overflow = "auto";
  loadData();
  window.addEventListener("resize", resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  areaChart?.dispose();
  remoteChart?.dispose();
});
</script>

<style scoped>
.stat-row,
.content-panel {
  width: var(--hn-page);
  margin: 0 auto 18px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.content-panel {
  padding: 24px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.select-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 220px;
  color: var(--hn-muted);
  font-size: 12px;
  font-weight: 900;
}

.select-pill select {
  height: 40px;
  min-width: 160px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-card);
  color: var(--hn-text);
  padding: 0 10px;
}

.chart-shell {
  position: relative;
  min-height: 360px;
  border: 1px solid rgba(18, 63, 56, 0.08);
  border-radius: 8px;
  background: var(--hn-surface);
  padding: 12px;
}

.chart-shell.wide { min-height: 408px; }
.chart { width: 100%; height: 340px; }
.wide .chart { height: 378px; }

.dataset-grid,
.card-grid,
.source-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 14px;
}

.dataset-card,
.evidence-card,
.source-card {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  padding: 16px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
}

.dataset-card h3,
.evidence-card h3,
.source-card h3 {
  height: 28px;
  margin: 0 0 8px;
  color: var(--hn-text);
  font-size: 17px;
  line-height: 28px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dataset-card p,
.evidence-card p,
.official-name {
  margin: 0;
  color: var(--hn-muted);
  line-height: 1.55;
}

.dataset-card p {
  min-height: 50px;
}

.dataset-status {
  min-height: 32px;
  display: flex;
  align-items: center;
  margin-top: 12px;
}

.dataset-status span,
.source-type,
.quality,
.direct-tag,
.ref-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 6px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}

.dataset-meta,
.source-meta {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  margin: auto 0 0;
}

.dataset-meta div,
.source-meta div {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.metric-table {
  margin-top: 16px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  overflow: hidden;
}

.metric-row {
  display: grid;
  grid-template-columns: 1.2fr 0.7fr 1.5fr 0.9fr 0.8fr;
  gap: 12px;
  padding: 12px 14px;
  border-top: 1px solid rgba(18, 63, 56, 0.08);
  color: var(--hn-text);
}

.metric-row.head {
  border-top: 0;
  background: var(--hn-soft);
  color: var(--hn-text);
  font-weight: 900;
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  min-height: 28px;
  margin-bottom: 12px;
}

.direct-tag { background: #e7f0fb; color: #1f5f8f; }
.ref-tag { background: var(--hn-soft); color: var(--hn-amber); }

.evidence-card p {
  min-height: 48px;
}

.value-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: auto 0 12px;
}

.value-line strong {
  color: var(--hn-accent);
  font-size: 27px;
}

a {
  color: var(--hn-accent);
  font-weight: 900;
}

.source-type {
  width: fit-content;
  margin: 12px 0;
}

.source-meta {
  margin-top: 0;
}

.source-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  align-items: center;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--hn-border);
}

.source-actions span,
.source-actions a {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
  text-align: center;
  text-decoration: none;
}

.source-actions .disabled-link {
  color: var(--hn-muted);
  opacity: 0.72;
}

dt { color: var(--hn-muted); font-size: 12px; }
dd { margin: 0; color: var(--hn-text); font-weight: 900; line-height: 1.45; }

:global(:root[data-hn-theme="dark"] .dataset-card),
:global(:root.theme-dark .dataset-card),
:global(:root[data-hn-theme="dark"] .evidence-card),
:global(:root.theme-dark .evidence-card),
:global(:root[data-hn-theme="dark"] .source-card),
:global(:root.theme-dark .source-card) {
  background: var(--hn-card);
}

:global(:root[data-hn-theme="dark"] .dataset-card p),
:global(:root.theme-dark .dataset-card p),
:global(:root[data-hn-theme="dark"] .evidence-card p),
:global(:root.theme-dark .evidence-card p),
:global(:root[data-hn-theme="dark"] .official-name),
:global(:root.theme-dark .official-name),
:global(:root[data-hn-theme="dark"] .metric-row),
:global(:root.theme-dark .metric-row) {
  color: var(--hn-muted);
}

@media (max-width: 820px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
  .metric-row { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .stat-row { grid-template-columns: 1fr; }
  .select-pill { align-items: stretch; flex-direction: column; }
  .evidence-card h3 { height: 48px; line-height: 24px; white-space: normal; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
  .source-actions { grid-template-columns: 1fr; }
}

/* Delivery fixed-card layout */
.dataset-card {
  min-height: 304px;
  display: grid;
  grid-template-rows: 32px 58px 42px 158px;
  gap: 10px;
}

.dataset-card h3 { margin: 0; }
.dataset-card p {
  min-height: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.dataset-status { min-height: 0; margin-top: 0; align-items: start; }
.dataset-meta {
  display: grid;
  grid-template-rows: minmax(58px, 58px) minmax(42px, 42px);
  row-gap: 10px;
  margin: 0;
}
.dataset-meta-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
}
.dataset-meta-row dd {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  -webkit-line-clamp: 2;
  font-weight: 500;
}
.dataset-meta-row:last-child dd { -webkit-line-clamp: 2; }

.source-grid {
  grid-template-columns: repeat(auto-fit, minmax(270px, 1fr));
  align-items: stretch;
}
.source-card {
  min-height: 286px;
  display: flex;
  flex-direction: column;
  padding: 14px;
}
.source-card h3 {
  min-height: 48px;
  margin: 0;
  font-size: 18px;
  line-height: 24px;
  white-space: normal;
  overflow-wrap: anywhere;
}
.official-name {
  min-height: 38px;
  margin-top: 2px;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}
.source-type {
  width: fit-content;
  max-width: 100%;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  margin: 10px 0 0;
}
.source-meta {
  display: grid;
  grid-template-rows: none;
  row-gap: 8px;
  margin: 14px 0 12px;
}
.source-meta-row {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
}
.source-meta-row dt {
  font-weight: 500;
}
.source-meta-row dd {
  font-size: 13px;
  font-weight: 500;
  overflow-wrap: anywhere;
}
.source-actions {
  margin-top: auto;
  min-height: 44px;
  align-self: stretch;
}
.source-actions span,
.source-actions a {
  padding: 0 6px;
  font-weight: 700;
}


</style>
