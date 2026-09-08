<template>
  <span class="source-badge" :title="label">{{ displayLabel }}</span>
</template>

<script setup>
import { computed } from 'vue';
import { formatPublicSource } from '@/config/publicPresentationPolicy';

const props = defineProps({
  value: { type: [String, Number], default: '' },
  type: { type: String, default: '' },
  max: { type: Number, default: 18 },
});

const label = computed(() => formatPublicSource(props.value, props.type));
const displayLabel = computed(() => (label.value.length > props.max ? `${label.value.slice(0, props.max)}...` : label.value));
</script>

<style scoped>
.source-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  max-width: 100%;
  padding: 0 8px;
  border-radius: 6px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>
