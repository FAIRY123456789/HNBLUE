<template>
  <PageShell>
    <div class="gov-page">
      <UnifiedNav />
      <PageHero title="治理工作台" eyebrow="Governance" subtitle="面向自然资源、林业、生态环境、海洋、发改和地方管理人员，支持筛选、追溯、管理、研判和工单审批" :tags="['治理概览', '来源追溯', '工单审批']" />

      <section v-if="globalError" class="notice error">{{ globalError }}</section>
      <section v-if="!isLoggedIn" class="notice login-required">当前未检测到登录态。可继续浏览公开数据；如需进行治理工单审批，请先 <router-link to="/login">登录</router-link>。</section>
      <Transition name="feedback">
        <div v-if="feedback.message" :class="['feedback-toast', feedback.type]" role="status" aria-live="polite">
          <span aria-hidden="true">{{ feedback.type === 'success' ? '✓' : '!' }}</span>{{ feedback.message }}
        </div>
      </Transition>
      <section class="tabs" aria-label="治理工作台模块"><button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" type="button" @click="activeTab = tab.key">{{ tab.label }}</button></section>

      <section v-show="activeTab === 'overview'" class="panel">
        <SectionHeader title="治理概览" eyebrow="Governance Overview" subtitle="以关键数量、业务含义和治理动作说明当前数据状态"><ActionButton label="刷新" @click="loadSummary" /></SectionHeader>
        <div class="stat-grid"><StatCard v-for="item in summaryCards" :key="item.key" :label="item.label" :value="item.value" :unit="item.unit" :note="item.note" /></div>
        <div class="decision-grid"><article v-for="item in decisionCards" :key="item.title"><h3>{{ item.title }}</h3><p>{{ item.desc }}</p></article></div>
      </section>

      <section v-show="activeTab === 'map'" class="panel">
        <SectionHeader title="蓝碳一张图" eyebrow="One Map" subtitle="查看区域红树林覆盖与变化、区域指标、来源和文献依据，支持区域间比较与重点区域识别" />
        <SearchPanel :fields="coverFields" :model-value="coverFilters" @update:model-value="assign(coverFilters, $event)" @search="searchCover" @reset="resetCoverFilters" />
        <div class="workbench-layout map-layout"><div class="region-list"><button v-for="row in coverRows" :key="row.cover_id || row.record_id || row.region_id" type="button" class="record-card" @mouseenter="setPreview(row, 'area')" @focus="setPreview(row, 'area')" @click="openDetail(row)"><strong>{{ row.region_name || row.region_id || '未命名区域' }}</strong><span>{{ row.year || '-' }} · {{ formatValue(row.area_value) }} {{ row.unit || '' }}</span><SourceBadge :value="row.source_code" :type="row.source_type" /></button><EmptyState v-if="!coverRows.length" title="暂无区域记录" message="当前筛选条件下没有红树林面积记录" /></div><RecordPreview :record="previewRecord" eyebrow="Area Preview" :title="previewTitle" :description="previewDescription" :items="previewItems" /></div>
        <PaginationBar :total="Number(datasets.mangrove_cover.pagination.total || 0)" :offset="Number(coverFilters.offset || 0)" :page-size="COVER_PAGE_SIZE" @change="pageCover" />
      </section>

      <section v-show="activeTab === 'metrics'" class="panel">
        <SectionHeader title="区域指标" eyebrow="Metrics" subtitle="统一筛选区域、年份、指标类别和来源，表格行悬停可预览"><ActionButton label="导出 CSV" variant="secondary" @click="exportCsv('region_metrics')" /></SectionHeader>
        <SearchPanel :fields="metricFields" :model-value="metricFilters" @update:model-value="assign(metricFilters, $event)" @search="searchMetrics" @reset="resetMetricFilters" />
        <div class="workbench-layout table-layout"><DataTable :rows="metricRows" :columns="metricColumns" work-order @preview="setPreview($event, 'metric')" @detail="openDetail" @copy-source="copySource" @work-order="startWorkOrder('修改', '区域指标', $event)" /><RecordPreview :record="previewRecord" eyebrow="Metric Preview" :title="previewTitle" :description="previewDescription" :items="previewItems" /></div>
        <PaginationBar :total="Number(datasets.region_metrics.pagination.total || 0)" :offset="Number(metricFilters.offset || 0)" :page-size="PAGE_SIZE" @change="pageMetrics" />
      </section>

      <section v-show="activeTab === 'literature'" class="panel">
        <SectionHeader title="文献证据" eyebrow="Evidence" subtitle="筛选碳库、方法、区域和来源，评审时可快速追溯证据文本"><ActionButton label="导出 CSV" variant="secondary" @click="exportCsv('literature_carbon')" /></SectionHeader>
        <SearchPanel :fields="literatureFields" :model-value="literatureFilters" @update:model-value="assign(literatureFilters, $event)" @search="searchLiterature" @reset="resetLiteratureFilters" />
        <div class="workbench-layout table-layout"><DataTable :rows="literatureRows" :columns="literatureColumns" work-order @preview="setPreview($event, 'literature')" @detail="openDetail" @copy-source="copySource" @work-order="startWorkOrder('修改', '文献证据', $event)" /><RecordPreview :record="previewRecord" eyebrow="Evidence Preview" :title="previewTitle" :description="previewDescription" :items="previewItems" /></div>
        <PaginationBar :total="Number(datasets.literature_carbon.pagination.total || 0)" :offset="Number(literatureFilters.offset || 0)" :page-size="PAGE_SIZE" @change="pageLiterature" />
      </section>

      <section v-show="activeTab === 'sources'" class="panel">
        <SectionHeader title="来源追溯" eyebrow="Sources" subtitle="管理公开来源、引用信息和链接，来源以中文名称和可读类型展示"><ActionButton label="导出 CSV" variant="secondary" @click="exportCsv('sources')" /></SectionHeader>
        <SearchPanel :fields="sourceFields" :model-value="sourceFilters" @update:model-value="assign(sourceFilters, $event)" @search="searchSources" @reset="resetSourceFilters" />
        <div class="workbench-layout table-layout"><DataTable :rows="sourceRows" :columns="sourceColumns" work-order @preview="setPreview($event, 'source')" @detail="openDetail" @copy-source="copySource" @work-order="startWorkOrder('修改', '来源', $event)" /><RecordPreview :record="previewRecord" eyebrow="Source Preview" :title="previewTitle" :description="previewDescription" :items="previewItems" /></div>
        <PaginationBar :total="Number(datasets.sources.pagination.total || 0)" :offset="Number(sourceFilters.offset || 0)" :page-size="PAGE_SIZE" @change="pageSources" />
      </section>

      <section v-show="activeTab === 'workorders'" class="panel">
        <SectionHeader title="治理工单" eyebrow="Work Orders" subtitle="变更申请的受理、复核与日志追踪"><ActionButton label="新建变更申请" @click="startWorkOrder('新增', '来源', null)" /></SectionHeader>
        <p class="workflow-note">治理工单用于提交数据的新增、修改或删除申请；申请经受理与复核后形成可追踪记录，避免直接改库造成依据和责任链丢失。</p>
        <section v-if="workOrderApiError" class="notice workorder-api-note">{{ workOrderApiError }}</section>
        <div class="workorder-layout">
          <form class="workorder-form" @submit.prevent="saveWorkOrder">
            <label><span>工单标题</span><input v-model="workOrderForm.title" placeholder="例如：补充文昌红树林来源证据" /></label>
            <label><span>变更类型</span><select v-model="workOrderForm.changeType"><option>新增</option><option>修改</option><option>删除</option></select></label>
            <label><span>对象类型</span><select v-model="workOrderForm.tableType"><option>来源</option><option>红树林面积记录</option><option>区域指标</option><option>文献证据</option></select></label>
            <label><span>证据来源</span><input v-model="workOrderForm.sourceCode" placeholder="来源标识或来源说明" /></label>
            <label class="full"><span>变更原因</span><input v-model="workOrderForm.reason" placeholder="说明为什么需要变更" /></label>
            <label class="full"><span>变更内容</span><textarea v-model="workOrderForm.payload" rows="5" placeholder="请说明拟变更的内容、字段含义或依据"></textarea></label>
            <label class="full"><span>备注 / 评论</span><textarea v-model="workOrderForm.notes" rows="3" placeholder="经办意见、审核意见或补充说明"></textarea></label>
            <div class="form-actions"><ActionButton type="submit" :label="currentWorkOrder?.id ? '更新申请' : '提交申请'" /><ActionButton v-if="currentWorkOrder?.id" label="添加评论" variant="secondary" type="button" @click="commentCurrentWorkOrder" /></div>
          </form>
          <div class="workorder-list">
            <article v-for="order in workOrders" :key="order.id || order.code" :class="['workorder-card', { active: currentWorkOrder?.id === order.id }]" @click="selectWorkOrder(order)">
              <span>{{ statusLabel(order.status) }}</span>
              <h3>{{ order.title || `${order.changeType} / ${order.tableType}` }}</h3>
              <p>{{ order.reason || '未填写原因' }}</p>
              <div class="workorder-meta"><small>{{ order.code || order.id }}</small><small>{{ order.submitterName || '本地工单' }} -> {{ order.assignedAdminName || '未指派' }}</small><small>{{ formatDateTime(order.updatedAt || order.createdAt) }}</small></div>
              <div class="card-actions">
                <ActionButton v-if="canAccept(order)" size="sm" label="受理" variant="secondary" @click.stop="runWorkOrderAction(order, 'accept')" />
                <ActionButton v-if="canSubmitForApproval(order)" size="sm" label="提交终审" variant="secondary" @click.stop="runWorkOrderAction(order, 'submit-approval')" />
                <ActionButton v-if="canApprove(order)" size="sm" label="批准" variant="secondary" @click.stop="runWorkOrderAction(order, 'approve')" />
                <ActionButton v-if="canApprove(order)" size="sm" label="拒绝" variant="danger" @click.stop="runWorkOrderAction(order, 'reject')" />
                <ActionButton v-if="canReturn(order)" size="sm" label="退回" variant="ghost" @click.stop="runWorkOrderAction(order, 'return')" />
                <ActionButton v-if="canClose(order)" size="sm" label="关闭" variant="ghost" @click.stop="runWorkOrderAction(order, 'close')" />
              </div>
              <ol v-if="currentWorkOrder?.id === order.id && order.logs?.length" class="timeline">
                <li v-for="log in order.logs" :key="log.id"><strong>{{ actionLabel(log.action) }}</strong><span>{{ statusLabel(log.fromStatus) }} -> {{ statusLabel(log.toStatus) }}</span><small>{{ log.actorName }} - {{ formatDateTime(log.createdAt) }}</small></li>
              </ol>
            </article>
            <EmptyState v-if="!workOrders.length" title="暂无工单" message="可从表格记录发起修改工单，或在此新建请求" />
          </div>
        </div>
      </section>

      <DetailDrawer :open="Boolean(detailRecord)" title="记录详情" :record="detailRecord || {}" @close="detailRecord = null" />
    </div>
  </PageShell>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import PageShell from "@/components/common/PageShell.vue";
import UnifiedNav from "@/components/common/UnifiedNav.vue";
import PageHero from "@/components/common/PageHero.vue";
import SectionHeader from "@/components/common/SectionHeader.vue";
import SearchPanel from "@/components/common/SearchPanel.vue";
import StatCard from "@/components/common/StatCard.vue";
import EmptyState from "@/components/common/EmptyState.vue";
import SourceBadge from "@/components/common/SourceBadge.vue";
import RecordPreview from "@/components/common/RecordPreview.vue";
import DetailDrawer from "@/components/common/DetailDrawer.vue";
import ActionButton from "@/components/common/ActionButton.vue";
import DataTable from "@/components/common/DataTable.vue";
import PaginationBar from "@/components/common/PaginationBar.vue";
import { formatPublicSource, formatSourceType, getMetricRule } from "@/config/publicPresentationPolicy";
import { apiUrl } from "@/utils/urls";

const API_BASE = apiUrl("/api/v2");
const WORK_ORDER_API_BASE = apiUrl("/api/work-orders");
const PAGE_SIZE = 10;
const COVER_PAGE_SIZE = 12;
const WORK_ORDER_KEY = "hnblue_governance_work_orders";
const tabs = [{ key: "overview", label: "治理概览" }, { key: "map", label: "蓝碳一张图" }, { key: "metrics", label: "区域指标" }, { key: "literature", label: "文献证据" }, { key: "sources", label: "来源追溯" }, { key: "workorders", label: "治理工单" }];
const activeTab = ref("overview");
const isLoggedIn = ref(Boolean(localStorage.getItem("token")));
const actorRole = ref(localStorage.getItem("userType") || "User");
const actorName = ref(currentStoredUsername());
const globalError = ref("");
const workOrderApiError = ref("");
const feedback = reactive({ message: "", type: "success" });
let feedbackTimer = null;
const summary = ref(null);
const detailRecord = ref(null);
const previewRecord = ref(null);
const previewKind = ref("area");
const workOrders = ref(loadStoredWorkOrders());
const currentWorkOrder = ref(null);
const workOrderForm = reactive(defaultWorkOrder());
const datasets = reactive({ sources: emptyDataset(), mangrove_cover: emptyDataset(), region_metrics: emptyDataset(), literature_carbon: emptyDataset() });
const sourceFilters = reactive({ keyword: "", source_type: "", source_code: "", limit: PAGE_SIZE, offset: 0 });
const coverFilters = reactive({ keyword: "", region: "", region_id: "", year: "", source_code: "", limit: COVER_PAGE_SIZE, offset: 0 });
const metricFilters = reactive({ keyword: "", region: "", year: "", metric_category: "", indicator_code: "", source_code: "", quality_flag: "", limit: PAGE_SIZE, offset: 0 });
const literatureFilters = reactive({ keyword: "", region: "", carbon_pool: "", data_type: "", method_type: "", source_code: "", limit: PAGE_SIZE, offset: 0 });
const defaultSourceFilters = { keyword: "", source_type: "", source_code: "", limit: PAGE_SIZE, offset: 0 };
const defaultCoverFilters = { keyword: "", region: "", region_id: "", year: "", source_code: "", limit: COVER_PAGE_SIZE, offset: 0 };
const defaultMetricFilters = { keyword: "", region: "", year: "", metric_category: "", indicator_code: "", source_code: "", quality_flag: "", limit: PAGE_SIZE, offset: 0 };
const defaultLiteratureFilters = { keyword: "", region: "", carbon_pool: "", data_type: "", method_type: "", source_code: "", limit: PAGE_SIZE, offset: 0 };
const textField = (key, label, placeholder = "") => ({ key, label, placeholder });
const sourceFields = [textField("keyword", "关键词", "来源名称 / 引用"), textField("source_type", "来源类型"), textField("source_code", "来源标识")];
const coverFields = [textField("keyword", "关键词"), textField("region", "区域"), textField("region_id", "区域标识"), textField("year", "年份"), textField("source_code", "来源标识")];
const metricFields = [textField("keyword", "关键词"), textField("region", "区域"), textField("year", "年份"), textField("metric_category", "指标类别"), textField("indicator_code", "指标标识"), textField("source_code", "来源标识")];
const literatureFields = [textField("keyword", "关键词"), textField("region", "区域"), textField("carbon_pool", "碳库"), textField("data_type", "数据类型"), textField("method_type", "方法"), textField("source_code", "来源标识")];
const sourceColumns = [{ key: "source_code", label: "来源标识", width: "190px" }, { key: "source_name", label: "来源名称", width: "220px", format: (row) => formatPublicSource(row.source_name || row.source_code, row.source_type) }, { key: "source_type", label: "来源类型", width: "150px", format: (row) => formatSourceType(row.source_type) }, { key: "citation", label: "引用", width: "300px" }];
const metricColumns = [{ key: "region_name", label: "区域", width: "130px" }, { key: "indicator_name", label: "指标", width: "180px" }, { key: "metric_category", label: "类别", width: "120px" }, { key: "year", label: "年份", width: "90px" }, { key: "value", label: "数值", width: "110px" }, { key: "unit", label: "单位", width: "90px" }, { key: "source_code", label: "来源", width: "210px" }];
const literatureColumns = [{ key: "region_name", label: "区域", width: "120px" }, { key: "carbon_pool", label: "碳库", width: "120px" }, { key: "data_type", label: "类型", width: "120px", format: (row) => formatSourceType(row.data_type) }, { key: "method_type", label: "方法", width: "140px", format: (row) => formatSourceType(row.method_type) }, { key: "value", label: "数值", width: "110px" }, { key: "unit", label: "单位", width: "90px" }, { key: "quality_level", label: "质量", width: "90px" }, { key: "source_code", label: "来源", width: "190px" }];

const sourceRows = computed(() => (datasets.sources.rows || []).map(enrichSource));
const coverRows = computed(() => (datasets.mangrove_cover.rows || []).map(enrichSource));
const metricRows = computed(() => (datasets.region_metrics.rows || []).map(enrichSource));
const literatureRows = computed(() => (datasets.literature_carbon.rows || []).map(enrichSource));
const decisionCards = [{ title: "资源现状", desc: "展示红树林面积、区域指标和文献碳储证据" }, { title: "变化趋势", desc: "通过区域、年份和来源筛选查看记录变化" }, { title: "证据来源", desc: "保留来源标识、引用和链接，支持审阅追溯" }, { title: "治理闭环", desc: "新增、修改、删除先进入工单审批，再进入应用环节" }, { title: "AI 碳助手", desc: "问答入口统一为右下角全局悬浮组件，辅助解释数据和模型依据" }];
const workOrderStatusLabels = { SUBMITTED: "已提交", PROCESSING: "处理中", PENDING_APPROVAL: "待终审", APPROVED: "已批准", REJECTED: "已拒绝", RETURNED: "已退回", CLOSED: "已关闭", "草稿": "草稿", "待审核": "待审核", "已应用": "已应用" };
const workOrderActionLabels = { CREATE: "创建", UPDATE: "更新", ACCEPT: "受理", SUBMIT_APPROVAL: "提交终审", APPROVE: "批准", REJECT: "拒绝", RETURN: "退回", CLOSE: "关闭", COMMENT: "评论" };

const summaryCards = computed(() => {
  const counts = summary.value?.data?.counts || {};
  return ["data_source", "region_metric", "indicator_dictionary", "literature_carbon"].map((key) => {
    const rule = getMetricRule(key);
    return { key, label: rule.title, value: counts[key] ?? "-", unit: rule.unit, note: rule.description };
  });
});
const previewTitle = computed(() => titleFor(previewRecord.value, previewKind.value));
const previewDescription = computed(() => descriptionFor(previewRecord.value, previewKind.value));
const previewItems = computed(() => itemsFor(previewRecord.value, previewKind.value));

watch(workOrders, persistWorkOrders, { deep: true });

onMounted(async () => {
  window.addEventListener("hnblue-auth-changed", syncAuthState);
  await Promise.allSettled([loadSummary(), loadSources(), loadCover(), loadMetrics(), loadLiterature()]);
  await loadWorkOrders();
  previewRecord.value = coverRows.value[0] || metricRows.value[0] || literatureRows.value[0] || sourceRows.value[0] || null;
});
onBeforeUnmount(() => {
  window.removeEventListener("hnblue-auth-changed", syncAuthState);
  if (feedbackTimer) window.clearTimeout(feedbackTimer);
});

function emptyDataset() { return { rows: [], pagination: { total: 0, limit: PAGE_SIZE, offset: 0, has_next: false }, boundary_notes: [] }; }
function assign(target, value) { Object.assign(target, value); }
async function request(path, params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => { if (value !== undefined && value !== null && String(value).trim() !== "") search.set(key, value); });
  const url = `${API_BASE}${path}${search.toString() ? `?${search.toString()}` : ""}`;
  const response = await fetch(url, { cache: "no-cache" });
  if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
  return response.json();
}
async function safeLoad(task) { try { return await task(); } catch (error) { globalError.value = `接口请求失败：${error.message}`; return null; } }
function assignDataset(key, result) { datasets[key] = result?.data ? { ...datasets[key], rows: result.data.rows || [], pagination: result.data.pagination || emptyDataset().pagination, boundary_notes: result.data.boundary_notes || [] } : emptyDataset(); }
async function loadSummary() { globalError.value = ""; summary.value = await safeLoad(() => request("/dashboard/summary")); }
async function loadSources() { globalError.value = ""; sourceFilters.limit = PAGE_SIZE; sourceFilters.offset = Number(sourceFilters.offset || 0); assignDataset("sources", await safeLoad(() => request("/sources", sourceFilters))); }
async function loadCover() { globalError.value = ""; coverFilters.limit = COVER_PAGE_SIZE; coverFilters.offset = Number(coverFilters.offset || 0); assignDataset("mangrove_cover", await safeLoad(() => request("/mangrove-cover", coverFilters))); }
async function loadMetrics() { globalError.value = ""; metricFilters.limit = PAGE_SIZE; metricFilters.offset = Number(metricFilters.offset || 0); assignDataset("region_metrics", await safeLoad(() => request("/region-metrics", metricFilters))); }
async function loadLiterature() { globalError.value = ""; literatureFilters.limit = PAGE_SIZE; literatureFilters.offset = Number(literatureFilters.offset || 0); assignDataset("literature_carbon", await safeLoad(() => request("/literature-carbon", literatureFilters))); }
function searchSources() { sourceFilters.offset = 0; loadSources(); }
function pageSources(offset) { sourceFilters.offset = offset; loadSources(); scrollToPanelTop(); }
function searchCover() { coverFilters.offset = 0; loadCover(); }
function pageCover(offset) { coverFilters.offset = offset; loadCover(); scrollToPanelTop(); }
function searchMetrics() { metricFilters.offset = 0; loadMetrics(); }
function pageMetrics(offset) { metricFilters.offset = offset; loadMetrics(); scrollToPanelTop(); }
function searchLiterature() { literatureFilters.offset = 0; loadLiterature(); }
function pageLiterature(offset) { literatureFilters.offset = offset; loadLiterature(); scrollToPanelTop(); }
function resetSourceFilters() { Object.assign(sourceFilters, defaultSourceFilters); loadSources(); }
function resetCoverFilters() { Object.assign(coverFilters, defaultCoverFilters); loadCover(); }
function resetMetricFilters() { Object.assign(metricFilters, defaultMetricFilters); loadMetrics(); }
function resetLiteratureFilters() { Object.assign(literatureFilters, defaultLiteratureFilters); loadLiterature(); }
function openDetail(row) { detailRecord.value = row; }
function setPreview(row, kind) { previewRecord.value = row; previewKind.value = kind; }
async function copySource(row) {
  const value = row?.url || row?.source_url || row?.source_code || row?.source_record_id || row?.source_name;
  if (!value) { showFeedback("该记录暂没有可复制的来源信息", "error"); return; }
  try {
    if (navigator.clipboard?.writeText && window.isSecureContext) {
      await navigator.clipboard.writeText(String(value));
    } else {
      const textarea = document.createElement("textarea");
      textarea.value = String(value);
      textarea.setAttribute("readonly", "");
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      if (!document.execCommand("copy")) throw new Error("copy command rejected");
      textarea.remove();
    }
    showFeedback(row?.url || row?.source_url ? "来源地址已复制" : "来源信息已复制");
  } catch {
    showFeedback("复制失败，请使用“查看来源”打开链接", "error");
  }
}
function showFeedback(message, type = "success") {
  feedback.message = message;
  feedback.type = type;
  if (feedbackTimer) window.clearTimeout(feedbackTimer);
  feedbackTimer = window.setTimeout(() => { feedback.message = ""; }, 2600);
}
function scrollToPanelTop() { nextTick(() => document.querySelector(".panel:not([style*='display: none'])")?.scrollIntoView({ block: "start", behavior: "smooth" })); }
function displayValue(row, key) { if (!row) return "-"; if (key === "value") return row.value ?? row.value_mg_ha ?? row.area_value ?? "-"; if (key === "quality_flag") return row.quality_flag ?? row.quality_level ?? "-"; if (key === "citation") return row.citation ?? row.reference ?? row.source_name ?? "-"; return row[key] ?? "-"; }
function titleFor(row, kind) { if (!row) return "记录摘要"; if (kind === "source") return formatPublicSource(row.source_name || row.source_code, row.source_type); if (kind === "literature") return row.region_name || row.site_name || "文献证据"; if (kind === "metric") return row.indicator_name || row.indicator_code || "区域指标"; return row.region_name || row.region_id || "区域记录"; }
function descriptionFor(row, kind) { if (!row) return ""; if (kind === "source") return row.description || row.citation || "公开来源记录。"; if (kind === "literature") return row.evidence_text || row.notes || row.description || "文献碳储证据摘要。"; if (kind === "metric") return row.description || "区域指标记录摘要。"; return [row.year, `${formatValue(row.area_value)} ${row.unit || ""}`.trim(), row.quality_flag].filter(Boolean).join(" · ") || "红树林面积记录摘要。"; }
function itemsFor(row, kind) {
  if (!row) return [];
  const common = [{ label: "来源", value: sourceDisplay(row) }, { label: "区域", value: row.region_name || row.region_id }];
  if (kind === "source") return [{ label: "类型", value: formatSourceType(row.source_type) }, { label: "引用", value: row.citation }, { label: "链接", value: row.url }];
  if (kind === "literature") return [...common, { label: "碳库", value: row.carbon_pool }, { label: "数值", value: `${displayValue(row, "value")} ${row.unit || ""}` }, { label: "质量", value: row.quality_level }];
  if (kind === "metric") return [...common, { label: "年份", value: row.year }, { label: "数值", value: `${displayValue(row, "value")} ${row.unit || ""}` }, { label: "类别", value: row.metric_category }];
  return [...common, { label: "年份", value: row.year }, { label: "面积", value: `${formatValue(row.area_value)} ${row.unit || ""}` }];
}
function exportCsv(key) {
  const rows = datasets[key]?.rows || [];
  if (!rows.length) return;
  const headers = [...new Set(rows.flatMap((row) => Object.keys(row)))];
  const csv = [headers.join(","), ...rows.map((row) => headers.map((header) => csvCell(row[header])).join(","))].join("\n");
  const link = document.createElement("a");
  link.href = `data:text/csv;charset=utf-8,%EF%BB%BF${encodeURIComponent(csv)}`;
  link.download = `hnblue_governance_${key}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  showFeedback("CSV 已开始下载");
}
function csvCell(value) { return `"${String(value ?? "").replaceAll('"', '""')}"`; }
function formatValue(value) { const number = Number(value); return Number.isFinite(number) ? number.toLocaleString("zh-CN", { maximumFractionDigits: 4 }) : value ?? "-"; }
function defaultWorkOrder() { return { id: "", code: "", title: "", changeType: "新增", tableType: "来源", sourceCode: "", reason: "", payload: "", notes: "", status: "SUBMITTED", createdAt: "" }; }
function loadStoredWorkOrders() { try { return JSON.parse(localStorage.getItem(WORK_ORDER_KEY) || "[]"); } catch { return []; } }
function persistWorkOrders() { localStorage.setItem(WORK_ORDER_KEY, JSON.stringify(workOrders.value)); }
function currentStoredUsername() { try { const info = JSON.parse(localStorage.getItem("hnblue_user_info") || "{}"); return info.name || info.username || ""; } catch { return ""; } }
function syncAuthState() { isLoggedIn.value = Boolean(localStorage.getItem("token")); actorRole.value = localStorage.getItem("userType") || "User"; actorName.value = currentStoredUsername(); loadWorkOrders(); }
function authHeaders(extra = {}) { const token = localStorage.getItem("token"); return token ? { ...extra, Authorization: `Bearer ${token}` } : { ...extra }; }
async function workOrderRequest(path, options = {}) {
  const response = await fetch(`${WORK_ORDER_API_BASE}${path}`, { cache: "no-cache", ...options, headers: authHeaders({ "Content-Type": "application/json", ...(options.headers || {}) }) });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.message || `${response.status} ${response.statusText}`);
  return payload;
}
async function loadWorkOrders() {
  if (!localStorage.getItem("token")) return;
  try {
    const payload = await workOrderRequest("");
    workOrders.value = payload.data || [];
    workOrderApiError.value = "";
  } catch (error) {
    workOrderApiError.value = `工单接口暂不可用：${error.message}。当前保留本地工单草稿，接口恢复后可继续提交。`;
  }
}
function startWorkOrder(changeType, tableType, row) {
  activeTab.value = "workorders";
  Object.assign(workOrderForm, defaultWorkOrder(), { title: row ? `申请${changeType}${tableType}记录` : "", changeType, tableType, sourceCode: row ? sourceDisplay(row) : "", reason: row ? `基于当前记录提交${changeType}申请` : "", payload: row ? descriptionFor(row, previewKind.value) : "" });
  currentWorkOrder.value = null;
}
async function saveWorkOrder() {
  const orderPayload = { ...workOrderForm, title: workOrderForm.title || `${workOrderForm.changeType}${workOrderForm.tableType}工单` };
  try {
    const isUpdate = Boolean(currentWorkOrder.value?.id);
    const path = isUpdate ? `/${currentWorkOrder.value.id}` : "";
    const method = isUpdate ? "PUT" : "POST";
    const payload = await workOrderRequest(path, { method, body: JSON.stringify(orderPayload) });
    upsertWorkOrder(payload.data);
    selectWorkOrder(payload.data);
    workOrderApiError.value = "";
    showFeedback(isUpdate ? "变更申请已更新" : "变更申请已提交");
  } catch (error) {
    workOrderApiError.value = `工单接口暂不可用：${error.message}。本次内容已保存到本地草稿。`;
    saveLocalWorkOrder(orderPayload);
    showFeedback("接口暂不可用，申请已保存为本地草稿", "error");
  }
}
function saveLocalWorkOrder(orderPayload) {
  const order = { ...orderPayload, id: orderPayload.id || `LOCAL-WO-${Date.now()}`, code: orderPayload.code || `LOCAL-WO-${Date.now()}`, status: orderPayload.status || "SUBMITTED", createdAt: orderPayload.createdAt || new Date().toISOString(), updatedAt: new Date().toISOString(), logs: orderPayload.logs || [] };
  upsertWorkOrder(order);
  selectWorkOrder(order);
}
async function commentCurrentWorkOrder() {
  if (!currentWorkOrder.value?.id) return;
  try {
    const payload = await workOrderRequest(`/${currentWorkOrder.value.id}/comments`, { method: "POST", body: JSON.stringify({ comment: workOrderForm.notes || "补充评论" }) });
    upsertWorkOrder(payload.data);
    selectWorkOrder(payload.data);
  } catch (error) { workOrderApiError.value = `工单操作失败：${error.message}`; }
}
function enrichSource(row) { return { ...row, source_display: sourceDisplay(row) }; }
function sourceDisplay(row) {
  if (!row) return "-";
  if (row.source_code || row.source_name) return formatPublicSource(row.source_name || row.source_code, row.source_type);
  const recordId = String(row.source_record_id || "").trim();
  if (recordId.startsWith("HN_LOW_CARBON_ISLAND_PLAN_2025")) return "海南低碳岛建设政策目标（2025）";
  if (recordId.startsWith("hn_plan_2035_")) return "海南省红树林资源保护专项规划（2024—2035年）";
  if (row.method_note?.startsWith("《")) return row.method_note.split("》")[0] + "》";
  return recordId || "来源待关联（已保留记录编号）";
}
async function runWorkOrderAction(order, action) {
  try {
    const payload = await workOrderRequest(`/${order.id}/${action}`, { method: "POST", body: JSON.stringify({ comment: workOrderForm.notes || "" }) });
    upsertWorkOrder(payload.data);
    selectWorkOrder(payload.data);
  } catch (error) { workOrderApiError.value = `评论提交失败：${error.message}`; }
}
function upsertWorkOrder(order) { const index = workOrders.value.findIndex((item) => item.id === order.id); if (index >= 0) workOrders.value[index] = order; else workOrders.value.unshift(order); }
function selectWorkOrder(order) { currentWorkOrder.value = order; Object.assign(workOrderForm, defaultWorkOrder(), order); }
function isAdminRole() { return actorRole.value === "Admin"; }
function isFirstLevelAdmin() { return isAdminRole() && actorName.value === "Admin_100000"; }
function canAccept(order) { return isAdminRole() && ["SUBMITTED", "RETURNED"].includes(order.status); }
function canSubmitForApproval(order) { return isAdminRole() && !isFirstLevelAdmin() && ["PROCESSING", "RETURNED"].includes(order.status); }
function canApprove(order) { return isFirstLevelAdmin() && ["SUBMITTED", "PROCESSING", "PENDING_APPROVAL"].includes(order.status); }
function canReturn(order) { return isFirstLevelAdmin() && ["SUBMITTED", "PROCESSING", "PENDING_APPROVAL"].includes(order.status); }
function canClose(order) { return isFirstLevelAdmin() && order.status !== "CLOSED"; }
function statusLabel(status) { return workOrderStatusLabels[status] || status || "补充评论"; }
function actionLabel(action) { return workOrderActionLabels[action] || action || "动作"; }
function formatDateTime(value) { if (!value) return "-"; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false }); }
</script>

<style scoped>
.gov-page { min-height: 100vh; padding-bottom: 52px; transition: background 0.25s ease, color 0.25s ease; }
.notice, .panel, .tabs { width: var(--hn-page-wide); margin-left: auto; margin-right: auto; }
.notice { margin-top: 12px; padding: 14px 16px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-card); color: var(--hn-text); }
.notice.login-required { margin-top: 4px; }
.notice.error { border-color: #f1c6c6; background: color-mix(in srgb, var(--hn-danger) 10%, var(--hn-card)); color: var(--hn-danger); }
.notice.login-required a { color: var(--hn-accent); font-weight: 900; }
.workorder-card span { color: var(--hn-accent); font-size: 12px; font-weight: 900; }
.decision-grid p, .asset-card p, .workorder-card p { margin: 0; color: var(--hn-muted); line-height: 1.65; }
.tabs { display: flex; gap: 8px; overflow-x: auto; padding: 10px; border: 1px solid var(--hn-border); border-radius: 999px; background: var(--hn-surface); box-shadow: var(--hn-shadow-soft); }
.tabs button { min-height: 38px; padding: 0 13px; border: 1px solid transparent; border-radius: 999px; background: transparent; color: var(--hn-muted); cursor: pointer; font-weight: 900; white-space: nowrap; }
.tabs button.active { border-color: var(--hn-accent); background: var(--hn-accent); color: var(--hn-on-accent); }
.panel { margin-top: 18px; padding: 24px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-surface); box-shadow: var(--hn-shadow-soft); }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 12px; }
.decision-grid, .asset-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 14px; margin-top: 18px; }
.decision-grid article, .asset-card, .workorder-card { padding: 16px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-card); }
.decision-grid h3, .asset-card strong, .workorder-card h3 { margin: 0 0 8px; color: var(--hn-text); }
.workbench-layout { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 18px; margin-top: 16px; }
.map-layout { align-items: start; }
.map-layout :deep(.record-preview) { min-height: 0; max-height: 398px; padding: 18px; overflow-y: auto; }
.map-layout :deep(.record-preview .description) { line-height: 1.5; }
.map-layout :deep(.record-preview dl) { gap: 9px; margin-top: 14px; }
.table-layout { grid-template-columns: minmax(0, 1.45fr) 340px; }
.region-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); grid-auto-rows: minmax(126px, auto); align-content: start; gap: 10px; }
.record-card { position: relative; display: grid; min-height: 126px; grid-template-rows: auto auto auto; align-content: space-between; gap: 8px; overflow: hidden; padding: 16px; text-align: left; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-control-bg); color: var(--hn-text); cursor: pointer; transition: box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease; }
.record-card::before { content: ""; position: absolute; inset: 0 0 auto; height: 3px; background: linear-gradient(90deg, var(--hn-accent), color-mix(in srgb, var(--hn-accent) 28%, transparent)); transform: scaleX(0); transform-origin: left center; transition: transform 0.22s ease; }
.record-card:hover, .record-card:focus-visible { border-color: rgba(13, 107, 87, 0.42); background: color-mix(in srgb, var(--hn-soft) 36%, var(--hn-control-bg)); box-shadow: var(--hn-shadow-soft); }
.record-card:hover::before, .record-card:focus-visible::before { transform: scaleX(1); }
.record-card strong { font-size: 16px; line-height: 1.4; }
.record-card span { color: var(--hn-muted); font-size: 14px; line-height: 1.45; }
.workorder-layout { display: grid; grid-template-columns: minmax(300px, 0.85fr) minmax(0, 1.15fr); gap: 18px; }
.workorder-form { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; padding: 16px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-card); }
.workorder-form label { display: grid; gap: 6px; }
.workorder-form label.full, .form-actions { grid-column: 1 / -1; }
.workorder-form label span { color: var(--hn-muted); font-size: 12px; font-weight: 900; }
.workorder-form input, .workorder-form select, .workorder-form textarea { width: 100%; border: 1px solid var(--hn-border-strong); border-radius: 7px; background: var(--hn-control-bg); color: var(--hn-text); padding: 10px 12px; font: inherit; }
.form-actions, .card-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.workorder-list { display: grid; gap: 10px; }
.workorder-card { cursor: pointer; transition: transform 0.18s ease, border-color 0.18s ease; }
.workorder-card:hover, .workorder-card.active { transform: translateY(-2px); border-color: var(--hn-accent); }
.workorder-card small { display: block; color: var(--hn-muted); }
.workorder-api-note { width: 100%; margin: 0 0 14px; box-shadow: none; }
.workflow-note { margin: 0 0 14px; padding: 12px 14px; border-left: 4px solid var(--hn-accent); border-radius: 0 7px 7px 0; background: var(--hn-soft); color: var(--hn-muted); line-height: 1.65; }
.feedback-toast { position: fixed; top: 84px; right: 24px; z-index: 1200; display: inline-flex; align-items: center; gap: 9px; max-width: min(420px, calc(100vw - 32px)); padding: 12px 16px; border: 1px solid rgba(31, 138, 122, 0.32); border-radius: 8px; background: #edf8f4; color: #0d6b57; box-shadow: var(--hn-shadow-strong); font-weight: 900; }
.feedback-toast span { display: inline-grid; width: 22px; height: 22px; place-items: center; border-radius: 50%; background: #198754; color: #fff; }
.feedback-toast.error { border-color: rgba(161, 58, 58, 0.3); background: #fff2f2; color: var(--hn-danger); }
.feedback-toast.error span { background: var(--hn-danger); }
.feedback-enter-active, .feedback-leave-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.feedback-enter-from, .feedback-leave-to { opacity: 0; transform: translateY(-8px); }
.workorder-meta { display: flex; flex-wrap: wrap; gap: 8px 12px; margin: 8px 0 12px; color: var(--hn-muted); }
.timeline { margin: 12px 0 0; padding: 10px 0 0 18px; border-top: 1px solid var(--hn-border); color: var(--hn-muted); }
.timeline li { margin-bottom: 8px; line-height: 1.45; }
.timeline strong { color: var(--hn-text); margin-right: 8px; }
.timeline span { margin-right: 8px; }
.timeline small { color: var(--hn-muted); }

@media (max-width: 980px) { .workbench-layout, .table-layout, .workorder-layout { grid-template-columns: 1fr; } .map-layout :deep(.record-preview) { max-height: none; } }
@media (max-width: 640px) { .panel { width: calc(100% - 24px); padding: 16px; } .tabs { width: calc(100% - 24px); border-radius: 8px; } .workorder-form { grid-template-columns: 1fr; } }
</style>
