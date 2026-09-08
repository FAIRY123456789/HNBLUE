<template>
  <div class="section-header">
    <div>
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="cleanSubtitle" class="subtitle">{{ cleanSubtitle }}</p>
    </div>
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  eyebrow: { type: String, default: "" },
  title: { type: String, required: true },
  subtitle: { type: String, default: "" },
});

const cleanSubtitle = computed(() => String(props.subtitle || '').trim().replace(/[。.]$/, ''));
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  color: var(--hn-text);
  font-size: 24px;
  line-height: 1.25;
}

.subtitle {
  max-width: 680px;
  margin: 8px 0 0;
  color: var(--hn-muted);
  line-height: 1.65;
}

@media (max-width: 720px) {
  .section-header {
    flex-direction: column;
  }
}
</style>
