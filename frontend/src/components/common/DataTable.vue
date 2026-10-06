<template>
  <div class="data-table-wrap">
    <table class="data-table">
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.key || column[0]" :style="columnStyle(column)">{{ column.label || column[1] }}</th>
          <th class="actions-head">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!rows.length">
          <td :colspan="columns.length + 1" class="empty-cell">暂无可用记录</td>
        </tr>
        <tr v-for="row in rows" :key="row[rowKey] || row.record_id || row.metric_id || row.cover_id || row.source_code" @mouseenter="$emit('preview', row)" @focusin="$emit('preview', row)">
          <td v-for="column in columns" :key="column.key || column[0]" :title="stringValue(row, column)">
            <a v-if="(column.key || column[0]) === 'url' && stringValue(row, column) !== '-'" :href="stringValue(row, column)" target="_blank" rel="noopener noreferrer">查看来源</a>
            <SourceBadge v-else-if="(column.key || column[0]) === 'source_code'" :value="sourceDisplay(row, column)" :type="row.source_type" />
            <span v-else>{{ stringValue(row, column) }}</span>
          </td>
          <td class="row-actions">
            <ActionButton size="sm" variant="secondary" label="详情" @click="$emit('detail', row)" />
            <a v-if="sourceUrl(row)" class="action-link" :href="sourceUrl(row)" target="_blank" rel="noopener noreferrer">查看来源</a>
            <ActionButton size="sm" variant="ghost" :label="sourceUrl(row) ? '复制地址' : '复制来源'" :disabled="!sourceCopyValue(row)" @click="$emit('copy-source', row)" />
            <ActionButton v-if="workOrder" size="sm" variant="ghost" label="申请变更" @click="$emit('work-order', row)" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import ActionButton from '@/components/common/ActionButton.vue';
import SourceBadge from '@/components/common/SourceBadge.vue';
import { formatPublicSource, formatPublicValue } from '@/config/publicPresentationPolicy';

const props = defineProps({
  rows: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  rowKey: { type: String, default: 'id' },
  workOrder: { type: Boolean, default: false },
});

defineEmits(['preview', 'detail', 'copy-source', 'work-order']);

function columnKey(column) {
  return column.key || column[0];
}

function rawValue(row, column) {
  const key = columnKey(column);
  let value = typeof column.format === 'function' ? column.format(row) : row[key];
  if (key === 'value') value = row.value ?? row.value_mg_ha ?? row.area_value;
  if (key === 'citation') value = row.citation ?? row.reference ?? row.source_name;
  return value;
}

function stringValue(row, column) {
  const key = columnKey(column);
  const value = rawValue(row, column);
  if (value === undefined || value === null || value === '') return '-';
  if (key === 'source_code') return formatPublicSource(value, row.source_type);
  return formatPublicValue(key, value);
}

function sourceDisplay(row, column) {
  return row.source_display || rawValue(row, column) || row.source_name || row.source_record_id || '';
}

function sourceUrl(row) {
  return row.url || row.source_url || '';
}

function sourceCopyValue(row) {
  return sourceUrl(row) || row.source_code || row.source_record_id || row.source_name || '';
}

function columnStyle(column) {
  return column.width ? { width: column.width } : undefined;
}
</script>

<style scoped>
.data-table-wrap {
  width: 100%;
  overflow-x: auto;
  border: 1px solid var(--hn-border);
  border-radius: var(--hn-radius);
  background: var(--hn-card);
}

.data-table {
  width: 100%;
  min-width: 840px;
  border-collapse: collapse;
  table-layout: fixed;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid var(--hn-border);
  text-align: left;
  vertical-align: middle;
}

th {
  background: var(--hn-soft);
  color: var(--hn-text);
  font-size: 13px;
  font-weight: 900;
}

td {
  color: var(--hn-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

tr:hover td {
  background: var(--hn-table-hover);
}

a {
  color: var(--hn-accent);
  font-weight: 900;
}

.action-link {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-soft);
  text-decoration: none;
  font-size: 12px;
}

.actions-head,
.row-actions {
  width: 330px;
}

.row-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  white-space: nowrap;
}

.empty-cell {
  height: 96px;
  color: var(--hn-muted);
  text-align: center;
}
</style>
