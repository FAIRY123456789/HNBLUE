<template>
  <button class="theme-toggle" type="button" :aria-pressed="modelValue === 'dark'" @click="toggle">
    <span class="track"><span class="thumb"></span></span>
    <span>{{ modelValue === 'dark' ? '深色' : '浅色' }}</span>
  </button>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: String, default: 'light' },
});
const emit = defineEmits(['update:modelValue']);

function toggle() {
  emit('update:modelValue', props.modelValue === 'dark' ? 'light' : 'dark');
}
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 999px;
  background: var(--hn-panel-solid);
  color: var(--hn-text);
  cursor: pointer;
  font-weight: 900;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.theme-toggle:hover {
  transform: translateY(-1px);
  border-color: rgba(31, 138, 122, 0.42);
}

.track {
  position: relative;
  width: 38px;
  height: 20px;
  border-radius: 999px;
  background: rgba(13, 107, 87, 0.18);
}

.thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--hn-accent);
  transition: transform 0.2s ease, background 0.2s ease;
}

.theme-toggle[aria-pressed='true'] {
  background: rgba(255, 255, 255, 0.08);
  color: #edf8f5;
  border-color: rgba(166, 220, 207, 0.28);
}

.theme-toggle[aria-pressed='true'] .track {
  background: rgba(166, 220, 207, 0.24);
}

.theme-toggle[aria-pressed='true'] .thumb {
  transform: translateX(18px);
  background: #a6dccf;
}
</style>
