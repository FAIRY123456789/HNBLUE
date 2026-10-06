<template>
  <section class="external-dataset-module">
    <div v-if="loading" class="module-state" aria-live="polite">正在审计外部数据集…</div>
    <div v-else>
      <div class="external-grid">
        <ExternalDatasetCard v-for="dataset in datasets" :key="dataset.datasetId" :dataset="dataset" @open="activeDataset = $event" />
      </div>
    </div>
    <ExternalDatasetDetailModal :dataset="activeDataset" @close="activeDataset = null" />
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import ExternalDatasetCard from "@/components/external-datasets/ExternalDatasetCard.vue";
import ExternalDatasetDetailModal from "@/components/external-datasets/ExternalDatasetDetailModal.vue";
import { fetchExternalDatasets } from "@/services/externalDatasetService";
import { EXTERNAL_DATASET_CATALOG } from "@/data/externalDatasetCatalog";

const datasets = ref([]);
const activeDataset = ref(null);
const loading = ref(true);

async function load() {
  loading.value = true;
  try {
    const response = await fetchExternalDatasets();
    datasets.value = response.datasets || [];
    if (datasets.value.length !== 4) throw new Error(`接口返回 ${datasets.value.length} 个数据集，预期为 4 个`);
  } catch {
    datasets.value = EXTERNAL_DATASET_CATALOG.map((dataset) => ({ ...dataset, catalogFallback: true }));
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.external-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.module-state { display: grid; min-height: 220px; place-items: center; gap: 8px; padding: 28px; border: 1px dashed var(--hn-border-strong); border-radius: 10px; color: var(--hn-muted); text-align: center; }
.module-state strong { color: var(--hn-text); font-size: 18px; }
.module-state button { min-height: 40px; padding: 0 14px; border: 0; border-radius: 8px; background: var(--hn-accent); color: #fff; font-weight: 700; cursor: pointer; }
.module-state.error { color: var(--hn-danger); }
@media (max-width: 1180px) { .external-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .external-grid { grid-template-columns: 1fr; } }
</style>
