<template>
  <article class="stat-card">
    <strong class="stat-value-line" :class="{ compact: isCompactValue }">
      <b>{{ displayValue }}</b>
      <span class="stat-context">
        <em v-if="unit">{{ unit }}</em>
        <span v-if="label" class="stat-label">{{ label }}</span>
      </span>
    </strong>
    <p v-if="cleanNote">{{ cleanNote }}</p>
    <ul v-if="tags.length" class="stat-tags">
      <li v-for="tag in tags" :key="tag">{{ tag }}</li>
    </ul>
  </article>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({
  label: { type: String, default: "" },
  value: { type: [String, Number], default: "-" },
  unit: { type: String, default: "" },
  note: { type: String, default: "" },
  tags: { type: Array, default: () => [] },
});

const isCompactValue = computed(() => String(props.value || '').length > 12);
const cleanNote = computed(() => String(props.note || '').trim().replace(/[。.]$/, ''));
const displayValue = ref(props.value);
let animationFrame = 0;

function numericTarget(value) {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  const text = String(value ?? '').trim();
  return /^-?\d+(?:\.\d+)?$/.test(text) ? Number(text) : null;
}

function formatAnimatedValue(value, target) {
  const decimals = Number.isInteger(target) ? 0 : Math.min(2, String(target).split('.')[1]?.length || 0);
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

function animateTo(value) {
  cancelAnimationFrame(animationFrame);
  const target = numericTarget(value);
  const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

  if (target === null || reduceMotion || target === 0) {
    displayValue.value = value;
    return;
  }

  const startedAt = performance.now();
  const duration = 800;
  const tick = (now) => {
    const progress = Math.min((now - startedAt) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    displayValue.value = formatAnimatedValue(target * eased, target);
    if (progress < 1) animationFrame = requestAnimationFrame(tick);
  };
  displayValue.value = formatAnimatedValue(0, target);
  animationFrame = requestAnimationFrame(tick);
}

onMounted(() => {
  watch(() => props.value, animateTo, { immediate: true });
});

onBeforeUnmount(() => cancelAnimationFrame(animationFrame));
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
  animation: stat-card-rise 0.48s cubic-bezier(0.22, 1, 0.36, 1) both;
}

strong {
  min-height: 38px;
  display: flex;
  align-items: baseline;
  gap: 6px;
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

.stat-context {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  color: var(--hn-muted);
  font-size: 13px;
  line-height: 1.2;
  white-space: nowrap;
}

strong em {
  color: inherit;
  font-size: inherit;
  font-style: normal;
  font-weight: 800;
}

.stat-label {
  color: inherit;
  font-size: inherit;
  font-weight: 800;
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

:global(:root[data-hn-theme="dark"]) .stat-card,
:global(:root.theme-dark) .stat-card {
  border-color: var(--hn-border);
  box-shadow: var(--hn-shadow-soft), inset 0 1px 0 rgba(255, 255, 255, 0.07);
}

@keyframes stat-card-rise {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
