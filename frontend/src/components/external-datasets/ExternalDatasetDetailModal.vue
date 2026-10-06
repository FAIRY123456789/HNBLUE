<template>
  <Teleport to="body">
    <div v-if="dataset" class="modal-backdrop" @click.self="$emit('close')" @keydown.esc="$emit('close')">
      <article ref="dialog" class="dataset-dialog" role="dialog" aria-modal="true" :aria-labelledby="titleId" tabindex="-1">
        <header>
          <div>
            <span class="eyebrow">{{ dataset.name }} · EXTERNAL DATASET</span>
            <h2 :id="titleId">{{ dataset.fullName }}</h2>
            <p>{{ dataset.description }}</p>
          </div>
          <button type="button" class="close" aria-label="关闭外部数据集详情" @click="$emit('close')">×</button>
        </header>

        <div v-if="error" class="state degraded" role="status">
          <strong>记录分页暂时未连接</strong>
          <span>{{ error }}</span>
          <a :href="dataset.sourceUrl" target="_blank" rel="noopener noreferrer">访问权威来源 ↗</a>
        </div>
        <div v-else-if="loading" class="state" aria-live="polite">正在读取数据表清单…</div>
        <template v-else>
          <div class="dialog-toolbar">
            <DatasetTableSelector v-model="selectedTableName" :dataset-id="dataset.datasetId" :tables="tables" />
            <nav class="detail-tabs" aria-label="外部数据集详情导航">
              <button v-for="tab in tabs" :key="tab.id" type="button" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">{{ tab.label }}</button>
            </nav>
          </div>

          <section v-if="activeTab === 'overview'" class="overview-panel">
            <div class="overview-stats">
              <div><span>数据表</span><strong>{{ formatCount(dataset.tableCount) }}</strong></div>
              <div><span>真实记录</span><strong>{{ formatCount(dataset.recordCount) }}</strong></div>
              <div><span>字段规模</span><strong>{{ formatCount(dataset.fieldCount) }}</strong></div>
            </div>
            <div class="overview-grid">
              <section>
                <h3>数据表清单</h3>
                <ul><li v-for="table in tables" :key="table.tableName"><span>{{ table.displayName }}</span><b>{{ formatCount(table.recordCount) }} 条 · {{ table.fieldCount }} 字段</b></li></ul>
              </section>
              <section>
                <h3>在平台中的用途</h3>
                <p>{{ dataset.usage }}</p>
                <p v-if="dataset.datasetId === 'baad'">BAAD 支持理解树木尺寸与生物量关系及模型变量含义，不等同于海南红树林实测结果。</p>
                <p v-else-if="dataset.datasetId === 'gwm'">各生态类型保持独立统计口径；珊瑚礁和冷水珊瑚不作为红树林蓝碳记录。</p>
              </section>
            </div>
            <a class="source-link" :href="dataset.sourceUrl" target="_blank" rel="noopener noreferrer">访问 {{ dataset.source }} 权威来源</a>
          </section>

          <DatasetSchemaTable v-else-if="activeTab === 'schema' && selectedTableName" :dataset-id="dataset.datasetId" :table-name="selectedTableName" />
          <DatasetRecordTable v-else-if="activeTab === 'records' && selectedTableName" :dataset-id="dataset.datasetId" :table-name="selectedTableName" />
        </template>
      </article>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from "vue";
import DatasetRecordTable from "@/components/external-datasets/DatasetRecordTable.vue";
import DatasetSchemaTable from "@/components/external-datasets/DatasetSchemaTable.vue";
import DatasetTableSelector from "@/components/external-datasets/DatasetTableSelector.vue";
import { fetchExternalDatasetTables } from "@/services/externalDatasetService";

const props = defineProps({ dataset: { type: Object, default: null } });
defineEmits(["close"]);

const titleId = `external-dataset-title-${Math.random().toString(36).slice(2)}`;
const tabs = [
  { id: "overview", label: "数据集概览" },
  { id: "schema", label: "字段结构" },
  { id: "records", label: "真实记录分页" },
];
const dialog = ref(null);
const tables = ref([]);
const selectedTableName = ref("");
const activeTab = ref("overview");
const loading = ref(false);
const error = ref("");

async function loadTables() {
  if (!props.dataset) return;
  loading.value = true;
  error.value = "";
  activeTab.value = "overview";
  try {
    const response = await fetchExternalDatasetTables(props.dataset.datasetId);
    tables.value = response.tables || [];
    selectedTableName.value = tables.value[0]?.tableName || "";
  } catch {
    error.value = "当前仍可查看上方简介与权威来源；记录服务恢复后可继续浏览表结构和分页记录。";
  } finally {
    loading.value = false;
  }
}

watch(() => props.dataset, async (value) => {
  document.body.style.overflow = value ? "hidden" : "";
  if (value) {
    await loadTables();
    await nextTick();
    dialog.value?.focus();
  }
}, { immediate: true });

onBeforeUnmount(() => { document.body.style.overflow = ""; });
function formatCount(value) { return new Intl.NumberFormat("zh-CN").format(Number(value || 0)); }
</script>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; z-index: 1200; display: grid; place-items: center; padding: 20px; background: rgba(3, 22, 25, 0.72); backdrop-filter: blur(8px); }
.dataset-dialog { width: min(1480px, 96vw); max-height: 94vh; overflow: auto; padding: 24px; border: 1px solid var(--hn-border-strong); border-radius: 14px; background: var(--hn-panel-solid); color: var(--hn-text); box-shadow: 0 28px 90px rgba(0, 0, 0, 0.34); }
header { display: flex; justify-content: space-between; align-items: flex-start; gap: 24px; padding-bottom: 18px; border-bottom: 1px solid var(--hn-border); }
.eyebrow { color: var(--hn-accent); font-size: 12px; font-weight: 800; letter-spacing: 0.12em; }
h2 { margin: 8px 0 6px; font-size: clamp(22px, 3vw, 34px); line-height: 1.2; }
header p { max-width: 900px; margin: 0; color: var(--hn-muted); line-height: 1.6; }
.close { flex: 0 0 42px; width: 42px; height: 42px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-control-bg); color: var(--hn-text); font-size: 28px; cursor: pointer; }
.dialog-toolbar { display: flex; justify-content: space-between; align-items: end; gap: 18px; padding: 18px 0; }
.detail-tabs { display: flex; flex-wrap: wrap; gap: 6px; }
.detail-tabs button { min-height: 40px; padding: 0 13px; border: 1px solid var(--hn-border-strong); border-radius: 8px; background: var(--hn-control-bg); color: var(--hn-text); font-weight: 700; cursor: pointer; }
.detail-tabs button.active { border-color: var(--hn-accent); background: var(--hn-accent); color: #fff; }
.overview-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.overview-stats div { padding: 14px; border: 1px solid var(--hn-border); border-radius: 9px; background: var(--hn-bg-soft); }
.overview-stats span { display: block; color: var(--hn-muted); font-size: 12px; }
.overview-stats strong { display: block; margin-top: 6px; font-size: 20px; }
.overview-grid { display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 14px; margin: 14px 0; }
.overview-grid section { padding: 18px; border: 1px solid var(--hn-border); border-radius: 9px; background: var(--hn-card); }
.overview-grid h3 { margin: 0 0 12px; }
.overview-grid p { color: var(--hn-muted); line-height: 1.7; }
ul { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
li { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--hn-border); }
li b { color: var(--hn-muted); font-size: 12px; white-space: nowrap; }
.source-link { display: inline-flex; min-height: 42px; align-items: center; padding: 0 14px; border-radius: 8px; background: var(--hn-soft); color: var(--hn-accent); font-weight: 800; text-decoration: none; }
.state { margin-top: 18px; padding: 24px; border-radius: 9px; background: var(--hn-soft); color: var(--hn-muted); }
.state.degraded { display: grid; gap: 10px; }
.state.degraded strong { color: var(--hn-text); }
.state.degraded a { width: fit-content; color: var(--hn-accent); font-weight: 800; text-decoration: none; }
.error { color: var(--hn-danger); }
@media (max-width: 860px) { .modal-backdrop { padding: 0; } .dataset-dialog { width: 100vw; min-height: 100vh; max-height: 100vh; border-radius: 0; } .dialog-toolbar, header { align-items: stretch; flex-direction: column; } .close { position: absolute; top: 14px; right: 14px; } .overview-stats { grid-template-columns: repeat(2, 1fr); } .overview-grid { grid-template-columns: 1fr; } }
</style>
