<template>
  <section :class="['page-hero', tone]">
    <div class="hero-copy">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="cleanSubtitle" class="subtitle">{{ cleanSubtitle }}</p>
      <div v-if="tags?.length" class="tag-row">
        <span v-for="tag in tags" :key="tag">{{ tag }}</span>
      </div>
    </div>
    <slot name="visual" />
  </section>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: "" },
  eyebrow: { type: String, default: "" },
  tags: { type: Array, default: () => [] },
  tone: { type: String, default: "light" },
});

const cleanSubtitle = computed(() => trimCaptionEnd(props.subtitle));

function trimCaptionEnd(value) {
  return String(value || '').trim().replace(/[。.]$/, '');
}
</script>

<style scoped>
.page-hero {
  width: var(--hn-page);
  margin: 0 auto;
  padding: 54px 0 36px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 28px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 10px;
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.12;
  letter-spacing: 0;
}

.subtitle {
  max-width: 780px;
  margin: 16px 0 0;
  color: var(--hn-muted);
  font-size: 17px;
  line-height: 1.75;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.tag-row span {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 800;
}

.dark {
  color: #fff;
}

.dark .eyebrow {
  color: #75dcc6;
}

.dark .subtitle {
  color: rgba(255, 255, 255, 0.82);
}

.dark .tag-row span {
  background: rgba(255, 255, 255, 0.13);
  color: #fff;
}

@media (max-width: 820px) {
  .page-hero {
    grid-template-columns: 1fr;
    padding-top: 34px;
  }
}
</style>
