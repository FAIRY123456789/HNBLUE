<template>
  <nav v-if="totalPages > 1" class="pagination-bar" aria-label="分页">
    <span>共 {{ total }} 条 · 第 {{ currentPage }} / {{ totalPages }} 页</span>
    <button type="button" :disabled="currentPage <= 1" @click="go(currentPage - 1)">上一页</button>
    <button
      v-for="page in pages"
      :key="page"
      type="button"
      :class="{ active: page === currentPage }"
      @click="go(page)"
    >
      {{ page }}
    </button>
    <button type="button" :disabled="currentPage >= totalPages" @click="go(currentPage + 1)">下一页</button>
  </nav>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 },
  offset: { type: Number, default: 0 },
});
const emit = defineEmits(["change"]);

const currentPage = computed(() => Math.floor(Number(props.offset || 0) / Number(props.pageSize || 10)) + 1);
const totalPages = computed(() => Math.max(1, Math.ceil(Number(props.total || 0) / Number(props.pageSize || 10))));
const pages = computed(() => {
  const start = Math.max(1, currentPage.value - 2);
  const end = Math.min(totalPages.value, start + 4);
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});

function go(page) {
  const target = Math.min(Math.max(1, page), totalPages.value);
  emit("change", (target - 1) * Number(props.pageSize || 10));
}
</script>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  color: var(--hn-muted);
}

.pagination-bar button {
  min-width: 34px;
  min-height: 34px;
  border: 1px solid var(--hn-border);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-text);
  cursor: pointer;
  font-weight: 900;
}

.pagination-bar button.active,
.pagination-bar button:hover:not(:disabled) {
  border-color: var(--hn-accent);
  background: var(--hn-accent);
  color: var(--hn-on-accent);
}

.pagination-bar button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .pagination-bar {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
