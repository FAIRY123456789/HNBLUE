<template>
  <aside class="record-preview">
    <template v-if="record">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p v-if="description" class="description">{{ description }}</p>
      <dl>
        <div v-for="item in items" :key="item.label">
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value || '-' }}</dd>
        </div>
      </dl>
    </template>
    <EmptyState v-else title="悬停预览" message="鼠标移到左侧记录上，即可在这里预览摘要；点击记录可查看完整详情" />
  </aside>
</template>

<script setup>
import EmptyState from "./EmptyState.vue";

defineProps({
  record: { type: Object, default: null },
  eyebrow: { type: String, default: "Preview" },
  title: { type: String, default: "记录摘要" },
  description: { type: String, default: "" },
  items: { type: Array, default: () => [] },
});
</script>

<style scoped>
.record-preview {
  position: sticky;
  top: 92px;
  min-height: 340px;
  padding: 20px;
  border: 1px solid var(--hn-border);
  border-radius: var(--hn-radius);
  background: linear-gradient(180deg, var(--hn-card), var(--hn-surface));
  box-shadow: var(--hn-shadow-soft);
  overflow: hidden;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

h3 {
  margin: 0;
  color: var(--hn-text);
  font-size: 23px;
  overflow-wrap: anywhere;
}

.description {
  margin: 10px 0 0;
  color: var(--hn-muted);
  line-height: 1.65;
  overflow-wrap: anywhere;
}

dl {
  display: grid;
  gap: 12px;
  margin: 18px 0 0;
}

dt {
  color: var(--hn-muted);
  font-size: 12px;
}

dd {
  margin: 3px 0 0;
  color: var(--hn-text);
  font-weight: 800;
  overflow-wrap: anywhere;
  word-break: break-word;
}
</style>
