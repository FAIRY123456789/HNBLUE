<template>
  <section class="records-panel">
    <form class="record-tools" @submit.prevent="applySearch">
      <label class="search"><span>关键词查询</span><input v-model="draftKeyword" type="search" placeholder="在当前表全部字段中查询" /></label>
      <button type="submit">查询</button>
      <label><span>排序字段</span><select v-model="sortField"><option value="">源记录顺序</option><option v-for="column in columns" :key="column" :value="column">{{ column }}</option></select></label>
      <label><span>方向</span><select v-model="sortDirection" :disabled="!sortField"><option value="asc">升序</option><option value="desc">降序</option></select></label>
      <label><span>每页</span><select v-model.number="size"><option :value="20">20 条</option><option :value="50">50 条</option><option :value="100">100 条</option></select></label>
    </form>

    <div v-if="error" class="state error" role="alert">{{ error }}</div>
    <div v-else class="record-table-shell" :aria-busy="loading">
      <table>
        <thead><tr><th class="ordinal">序号</th><th v-for="column in columns" :key="column">{{ column }}</th></tr></thead>
        <tbody>
          <tr v-for="(record, rowIndex) in records" :key="`${page}-${rowIndex}`">
            <td class="ordinal">{{ (page - 1) * size + rowIndex + 1 }}</td>
            <td v-for="column in columns" :key="column">
              <button
                v-if="isLong(record[column])"
                type="button"
                :class="['cell-value', { expanded: expandedCell === `${rowIndex}:${column}` }]"
                :title="String(record[column])"
                @click="toggleCell(rowIndex, column)"
              >{{ displayValue(record[column]) }}</button>
              <span v-else class="cell-value">{{ displayValue(record[column]) }}</span>
            </td>
          </tr>
          <tr v-if="!loading && !records.length"><td :colspan="columns.length + 1" class="empty">当前条件下没有数据记录</td></tr>
        </tbody>
      </table>
      <div v-if="loading" class="loading-layer" aria-live="polite">正在读取真实记录…</div>
    </div>

    <div class="pagination" aria-label="记录分页">
      <div class="page-summary">共 {{ formatCount(totalElements) }} 条 · {{ formatCount(totalPages) }} 页</div>
      <div class="page-actions">
        <button type="button" :disabled="page <= 1 || loading" @click="go(1)">第一页</button>
        <button type="button" :disabled="page <= 1 || loading" @click="go(page - 1)">上一页</button>
        <label><span class="sr-only">页码</span><input v-model.number="jumpPage" type="number" min="1" :max="Math.max(totalPages, 1)" @keyup.enter="jump" /></label>
        <button type="button" :disabled="loading" @click="jump">跳转</button>
        <button type="button" :disabled="page >= totalPages || loading" @click="go(page + 1)">下一页</button>
        <button type="button" :disabled="page >= totalPages || loading" @click="go(totalPages)">最后一页</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from "vue";
import { fetchExternalDatasetRecords } from "@/services/externalDatasetService";

const props = defineProps({
  datasetId: { type: String, required: true },
  tableName: { type: String, required: true },
});

const records = ref([]);
const columns = ref([]);
const page = ref(1);
const size = ref(20);
const totalElements = ref(0);
const totalPages = ref(0);
const jumpPage = ref(1);
const draftKeyword = ref("");
const keyword = ref("");
const sortField = ref("");
const sortDirection = ref("asc");
const loading = ref(false);
const error = ref("");
const expandedCell = ref("");
let requestSequence = 0;

async function load() {
  if (!props.tableName) return;
  const sequence = ++requestSequence;
  loading.value = true;
  error.value = "";
  expandedCell.value = "";
  try {
    const response = await fetchExternalDatasetRecords(props.datasetId, {
      tableName: props.tableName,
      page: page.value,
      size: size.value,
      keyword: keyword.value,
      sortField: sortField.value,
      sortDirection: sortDirection.value,
    });
    if (sequence !== requestSequence) return;
    records.value = response.records || [];
    columns.value = response.columns || [];
    totalElements.value = Number(response.totalElements || 0);
    totalPages.value = Number(response.totalPages || 0);
    jumpPage.value = page.value;
  } catch (requestError) {
    if (sequence !== requestSequence) return;
    error.value = requestError.message || "数据记录读取失败";
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

function applySearch() { keyword.value = draftKeyword.value.trim(); page.value = 1; load(); }
function go(target) { page.value = Math.min(Math.max(Number(target) || 1, 1), Math.max(totalPages.value, 1)); load(); }
function jump() { go(jumpPage.value); }
function toggleCell(row, column) { const key = `${row}:${column}`; expandedCell.value = expandedCell.value === key ? "" : key; }
function isLong(value) { return String(value ?? "").length > 28; }
function displayValue(value) { return value === null || value === undefined || value === "" ? "—" : value; }
function formatCount(value) { return new Intl.NumberFormat("zh-CN").format(Number(value || 0)); }

watch(() => [props.datasetId, props.tableName], () => {
  page.value = 1;
  sortField.value = "";
  keyword.value = "";
  draftKeyword.value = "";
  load();
}, { immediate: true });
watch(size, () => { page.value = 1; load(); });
watch([sortField, sortDirection], () => { page.value = 1; load(); });
</script>

<style scoped>
.records-panel { min-width: 0; }
.record-tools { display: grid; grid-template-columns: minmax(240px, 1fr) auto minmax(150px, 0.55fr) 110px 100px; align-items: end; gap: 10px; margin-bottom: 14px; }
label { display: grid; gap: 6px; color: var(--hn-muted); font-size: 12px; }
input, select, button { min-height: 40px; border: 1px solid var(--hn-border-strong); border-radius: 8px; background: var(--hn-control-bg); color: var(--hn-text); }
input, select { min-width: 0; padding: 0 11px; }
button { padding: 0 12px; font-weight: 700; cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: 0.45; }
.record-tools > button { background: var(--hn-accent); color: #fff; }
.record-table-shell { position: relative; max-height: 54vh; overflow: auto; border: 1px solid var(--hn-border); border-radius: 9px; }
table { width: max-content; min-width: 100%; border-collapse: collapse; font-size: 12px; }
th { position: sticky; top: 0; z-index: 2; max-width: 240px; padding: 11px 12px; background: var(--hn-panel-solid); color: var(--hn-muted); text-align: left; white-space: nowrap; }
td { max-width: 260px; padding: 8px 12px; border-top: 1px solid var(--hn-border); color: var(--hn-text); vertical-align: top; }
tbody tr:hover { background: var(--hn-table-hover); }
.ordinal { position: sticky; left: 0; z-index: 1; width: 72px; background: var(--hn-panel-solid); color: var(--hn-muted); }
th.ordinal { z-index: 3; }
.cell-value { display: block; max-width: 240px; overflow: hidden; padding: 0; border: 0; background: transparent; color: inherit; font: inherit; font-weight: inherit; text-align: left; text-overflow: ellipsis; white-space: nowrap; }
button.cell-value { min-height: 0; cursor: zoom-in; }
.cell-value.expanded { overflow: visible; white-space: normal; overflow-wrap: anywhere; cursor: zoom-out; }
.empty { padding: 34px; color: var(--hn-muted); text-align: center; }
.loading-layer { position: absolute; inset: 42px 0 0; display: grid; place-items: center; background: color-mix(in srgb, var(--hn-panel-solid) 84%, transparent); color: var(--hn-muted); }
.pagination { display: flex; justify-content: space-between; align-items: center; gap: 14px; margin-top: 12px; }
.page-summary { color: var(--hn-muted); font-size: 12px; }
.page-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 6px; }
.page-actions input { width: 76px; }
.state { padding: 18px; border-radius: 8px; background: var(--hn-soft); }
.error { color: var(--hn-danger); }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); }
@media (max-width: 920px) { .record-tools { grid-template-columns: 1fr 100px; } .record-tools .search { grid-column: 1; } .pagination { align-items: flex-start; flex-direction: column; } }
</style>
