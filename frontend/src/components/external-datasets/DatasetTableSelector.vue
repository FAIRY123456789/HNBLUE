<template>
  <div class="table-selector">
    <template v-if="isGwm">
      <label>
        <span>生态类型</span>
        <select v-model="selectedEcosystem">
          <option v-for="item in ecosystems" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>统计层级</span>
        <select v-model="selectedLevel">
          <option value="country">国家或地区</option>
          <option value="global">全球汇总</option>
        </select>
      </label>
    </template>
    <label v-else>
      <span>数据表</span>
      <select :value="modelValue" @change="emit('update:modelValue', $event.target.value)">
        <option v-for="table in tables" :key="table.tableName" :value="table.tableName">
          {{ table.displayName }} · {{ formatCount(table.recordCount) }} 条
        </option>
      </select>
    </label>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  modelValue: { type: String, default: "" },
  datasetId: { type: String, default: "" },
  tables: { type: Array, default: () => [] },
});
const emit = defineEmits(["update:modelValue"]);

const ecosystemLabels = {
  Mangrove: "Mangrove · 红树林",
  Seagrass: "Seagrass · 海草床",
  Saltmarsh: "Saltmarsh · 盐沼",
  Coral_Reef: "Coral Reef · 珊瑚礁",
  Cold_Coral: "Cold Coral · 冷水珊瑚",
};
const ecosystemOrder = ["Mangrove", "Seagrass", "Saltmarsh", "Coral_Reef", "Cold_Coral"];
const isGwm = computed(() => props.datasetId.toLowerCase() === "gwm");
const gwmTables = computed(() => props.tables.map((table) => ({ ...table, ...parseGwmName(table.tableName) })).filter((table) => table.ecosystem));
const ecosystems = computed(() => ecosystemOrder
  .filter((ecosystem) => gwmTables.value.some((table) => table.ecosystem === ecosystem))
  .map((value) => ({ value, label: ecosystemLabels[value] })));
const selectedEcosystem = computed({
  get: () => parseGwmName(props.modelValue).ecosystem || ecosystems.value[0]?.value || "Mangrove",
  set: (value) => selectGwm(value, selectedLevel.value),
});
const selectedLevel = computed({
  get: () => parseGwmName(props.modelValue).level || "country",
  set: (value) => selectGwm(selectedEcosystem.value, value),
});

function parseGwmName(tableName) {
  const match = String(tableName || "").match(/^(Cold_Coral|Coral_Reef|Mangrove|Saltmarsh|Seagrass)_(country|global)\.csv$/i);
  if (!match) return {};
  const ecosystem = ecosystemOrder.find((item) => item.toLowerCase() === match[1].toLowerCase());
  return { ecosystem, level: match[2].toLowerCase() };
}
function selectGwm(ecosystem, level) {
  const table = gwmTables.value.find((item) => item.ecosystem === ecosystem && item.level === level);
  if (table) emit("update:modelValue", table.tableName);
}
function formatCount(value) {
  return new Intl.NumberFormat("zh-CN").format(Number(value || 0));
}
</script>

<style scoped>
.table-selector { display: grid; min-width: min(100%, 360px); grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; color: var(--hn-muted); font-size: 12px; }
.table-selector > label { display: grid; gap: 6px; }
.table-selector > label:only-child { grid-column: 1 / -1; }
select { width: 100%; min-height: 42px; padding: 0 36px 0 12px; border: 1px solid var(--hn-border-strong); border-radius: 8px; background: var(--hn-control-bg); color: var(--hn-text); font: inherit; font-size: 14px; }
@media (max-width: 620px) { .table-selector { grid-template-columns: 1fr; } }
</style>