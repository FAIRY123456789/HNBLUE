<template>
  <section class="schema-panel">
    <div class="schema-tools">
      <label><span>字段搜索</span><input v-model.trim="keyword" type="search" placeholder="英文名、中文名或含义" /></label>
      <label><span>字段类别</span><select v-model="category"><option value="">全部类别</option><option v-for="item in categories" :key="item">{{ item }}</option></select></label>
    </div>
    <div v-if="error" class="state error" role="alert">{{ error }}</div>
    <div v-else class="table-shell" :aria-busy="loading">
      <table>
        <thead><tr><th>英文字段</th><th>中文名称</th><th>类型</th><th>单位</th><th>字段含义</th><th>类别</th><th>缺失值</th></tr></thead>
        <tbody>
          <tr v-for="column in visibleColumns" :key="column.columnName">
            <td><code>{{ column.columnName }}</code></td>
            <td>{{ column.displayNameCn }}</td>
            <td>{{ column.dataType }}</td>
            <td>{{ column.unit || "—" }}</td>
            <td class="meaning">{{ column.description }}</td>
            <td><span class="category">{{ column.category }}</span></td>
            <td>{{ column.nullable ? "可能存在" : "未发现" }}</td>
          </tr>
          <tr v-if="!loading && !visibleColumns.length"><td colspan="7" class="empty">没有匹配字段</td></tr>
        </tbody>
      </table>
      <div v-if="loading" class="loading-layer" aria-live="polite">正在读取字段结构…</div>
    </div>
    <p class="summary">共 {{ filteredColumns.length }} 个字段；关键字段优先显示，完整字段名保持与源表一致。</p>
  </section>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { fetchExternalDatasetSchema } from "@/services/externalDatasetService";

const props = defineProps({
  datasetId: { type: String, required: true },
  tableName: { type: String, required: true },
});

const columns = ref([]);
const loading = ref(false);
const error = ref("");
const keyword = ref("");
const category = ref("");
const categories = computed(() => [...new Set(columns.value.map((item) => item.category).filter(Boolean))].sort());
const filteredColumns = computed(() => columns.value.filter((column) => {
  const matchesCategory = !category.value || column.category === category.value;
  const needle = keyword.value.toLocaleLowerCase();
  const matchesKeyword = !needle || [column.columnName, column.displayNameCn, column.description]
    .some((value) => String(value || "").toLocaleLowerCase().includes(needle));
  return matchesCategory && matchesKeyword;
}).sort((a, b) => Number(a.displayPriority || 100) - Number(b.displayPriority || 100)));
const visibleColumns = computed(() => filteredColumns.value);

async function load() {
  if (!props.tableName) return;
  loading.value = true;
  error.value = "";
  try {
    const response = await fetchExternalDatasetSchema(props.datasetId, { tableName: props.tableName, page: 1, size: 100 });
    columns.value = response.columns || [];
  } catch (requestError) {
    error.value = requestError.message || "字段结构读取失败";
  } finally {
    loading.value = false;
  }
}

watch(() => [props.datasetId, props.tableName], load, { immediate: true });
</script>

<style scoped>
.schema-panel { min-width: 0; }
.schema-tools { display: grid; grid-template-columns: minmax(220px, 1fr) minmax(160px, 240px); gap: 12px; margin-bottom: 14px; }
label { display: grid; gap: 6px; color: var(--hn-muted); font-size: 12px; }
input, select { min-height: 40px; padding: 0 12px; border: 1px solid var(--hn-border-strong); border-radius: 8px; background: var(--hn-control-bg); color: var(--hn-text); }
.table-shell { position: relative; max-height: 52vh; overflow: auto; border: 1px solid var(--hn-border); border-radius: 9px; }
table { width: 100%; min-width: 900px; border-collapse: collapse; font-size: 13px; }
th { position: sticky; top: 0; z-index: 1; padding: 11px 12px; background: var(--hn-panel-solid); color: var(--hn-muted); text-align: left; }
td { max-width: 320px; padding: 10px 12px; border-top: 1px solid var(--hn-border); color: var(--hn-text); vertical-align: top; }
tbody tr:hover { background: var(--hn-table-hover); }
code { color: var(--hn-accent); font-weight: 700; }
.meaning { min-width: 260px; line-height: 1.55; }
.category { padding: 4px 7px; border-radius: 999px; background: var(--hn-soft); white-space: nowrap; }
.empty { padding: 30px; color: var(--hn-muted); text-align: center; }
.loading-layer { position: absolute; inset: 42px 0 0; display: grid; place-items: center; background: color-mix(in srgb, var(--hn-panel-solid) 82%, transparent); color: var(--hn-muted); }
.state { padding: 18px; border-radius: 8px; background: var(--hn-soft); }
.error { color: var(--hn-danger); }
.summary { margin: 10px 0 0; color: var(--hn-muted); font-size: 12px; }
@media (max-width: 720px) { .schema-tools { grid-template-columns: 1fr; } }
</style>
