<template>
  <aside class="guide-sidebar" aria-label="系统使用手册章节">
    <button
      v-for="section in sections"
      :key="section.id"
      type="button"
      :class="['guide-nav-item', { active: section.id === activeId }]"
      @click="$emit('select', section.id)"
    >
      <span>{{ section.title }}</span>
      <small>{{ section.audience }}</small>
    </button>
  </aside>
</template>

<script setup>
defineProps({
  sections: { type: Array, required: true },
  activeId: { type: String, default: '' },
});
defineEmits(['select']);
</script>

<style scoped>
.guide-sidebar {
  display: grid;
  align-content: start;
  gap: 8px;
  min-width: 252px;
  max-height: 100%;
  overflow: auto;
  padding: 8px 8px 8px 0;
}

.guide-nav-item {
  width: 100%;
  display: grid;
  gap: 4px;
  padding: 12px 13px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--hn-muted);
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.guide-nav-item span {
  color: inherit;
  font-size: 14px;
  font-weight: 900;
  line-height: 1.35;
}

.guide-nav-item small {
  color: inherit;
  opacity: 0.78;
  font-size: 12px;
  line-height: 1.35;
}

.guide-nav-item:hover,
.guide-nav-item.active {
  border-color: rgba(31, 138, 122, 0.28);
  background: var(--hn-soft);
  color: var(--hn-text);
}

@media (max-width: 860px) {
  .guide-sidebar {
    min-width: 0;
    grid-auto-flow: column;
    grid-auto-columns: minmax(166px, 1fr);
    overflow-x: auto;
    padding: 0 0 10px;
  }
}
</style>
