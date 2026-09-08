<template>
  <article class="stat-card">
    <span v-if="label">{{ label }}</span>
    <strong :class="{ compact: isCompactValue }"><b>{{ value }}</b><em v-if="unit">{{ unit }}</em></strong>
    <p v-if="cleanNote">{{ cleanNote }}</p>
    <ul v-if="tags.length" class="stat-tags">
      <li v-for="tag in tags" :key="tag">{{ tag }}</li>
    </ul>
  </article>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  label: { type: String, default: "" },
  value: { type: [String, Number], default: "-" },
  unit: { type: String, default: "" },
  note: { type: String, default: "" },
  tags: { type: Array, default: () => [] },
});

const isCompactValue = computed(() => String(props.value || '').length > 12);
const cleanNote = computed(() => String(props.note || '').trim().replace(/[。.]$/, ''));
</script>

<style scoped>
.stat-card {
  min-height: 148px;
  display: flex;
  flex-direction: column;
  padding: 18px;
  border: 1px solid var(--hn-border);
  border-radius: var(--hn-radius);
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

span {
  display: block;
  min-height: 18px;
  color: var(--hn-muted);
  font-size: 13px;
  font-weight: 800;
}

strong {
  min-height: 38px;
  display: flex;
  align-items: baseline;
  gap: 7px;
  margin-top: 8px;
  color: var(--hn-accent);
  line-height: 1.05;
}

strong b {
  font-size: 32px;
}

strong.compact {
  align-items: center;
}

strong.compact b {
  font-size: 15px;
  line-height: 1.4;
}

strong em {
  color: var(--hn-text);
  font-size: 14px;
  font-style: normal;
  font-weight: 900;
}

p {
  min-height: 42px;
  margin: 10px 0 0;
  color: var(--hn-muted);
  line-height: 1.5;
}

.stat-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: auto 0 0;
  padding: 12px 0 0;
  list-style: none;
}

.stat-tags li {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border-radius: 6px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}
</style>
