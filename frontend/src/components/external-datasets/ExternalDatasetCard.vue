<template>
  <article class="external-card">
    <div class="card-topline">
      <span class="dataset-code">{{ dataset.name }}</span>
    </div>
    <h3>{{ dataset.fullName }}</h3>
    <p class="description">{{ dataset.description }}</p>
    <dl class="metrics">
      <div><dt>数据表</dt><dd>{{ formatCount(dataset.tableCount) }}</dd></div>
      <div><dt>真实记录</dt><dd>{{ formatCount(dataset.recordCount) }}</dd></div>
      <div><dt>关键字段</dt><dd>{{ formatCount(dataset.keyFieldCount) }}</dd></div>
    </dl>
    <dl class="details">
      <div><dt>数据来源</dt><dd>{{ dataset.source }}</dd></div>
      <div><dt>平台用途</dt><dd>{{ dataset.usage }}</dd></div>
    </dl>
    <div class="card-actions">
      <button type="button" class="open-button" @click="$emit('open', dataset)">
        查看数据
        <span aria-hidden="true">→</span>
      </button>
      <a class="source-link" :href="dataset.sourceUrl" target="_blank" rel="noopener noreferrer">权威来源 ↗</a>
    </div>
  </article>
</template>

<script setup>
defineProps({
  dataset: { type: Object, required: true },
});
defineEmits(["open"]);

function formatCount(value) {
  return new Intl.NumberFormat("zh-CN").format(Number(value || 0));
}
</script>

<style scoped>
.external-card {
  display: flex;
  min-width: 0;
  min-height: 330px;
  flex-direction: column;
  padding: 22px;
  border: 1px solid var(--hn-border);
  border-radius: 12px;
  background:
    radial-gradient(circle at 100% 0, rgba(52, 167, 153, 0.14), transparent 38%),
    var(--hn-card);
  box-shadow: var(--hn-shadow-soft);
  transition: transform 240ms ease, border-color 240ms ease, box-shadow 240ms ease;
}

.external-card:hover {
  z-index: 1;
  transform: translateY(-4px) scale(1.025);
  border-color: var(--hn-accent-2);
  box-shadow: 0 22px 44px rgba(10, 68, 58, 0.16);
}

.card-topline,
.metrics,
.details div,
.open-button {
  display: flex;
  align-items: center;
}

.card-topline { justify-content: space-between; gap: 12px; }
.dataset-code { color: var(--hn-accent); font-size: 13px; font-weight: 800; letter-spacing: 0.12em; }
h3 { margin: 18px 0 8px; color: var(--hn-text); font-size: 19px; line-height: 1.35; }
.description { min-height: 44px; margin: 0; color: var(--hn-muted); font-size: 14px; line-height: 1.65; }
.metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 18px 0; }
.metrics div { padding: 10px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-bg-soft); }
.metrics dt { color: var(--hn-muted); font-size: 11px; }
.metrics dd { margin: 4px 0 0; color: var(--hn-text); font-size: 18px; font-weight: 800; }
.details { display: grid; gap: 9px; margin: 0 0 18px; }
.details div { align-items: flex-start; gap: 8px; }
.details dt { flex: 0 0 62px; color: var(--hn-muted); font-size: 12px; }
.details dd { display: -webkit-box; overflow: hidden; margin: 0; color: var(--hn-text); font-size: 13px; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.card-actions { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; margin-top: auto; }
.open-button { justify-content: space-between; width: 100%; min-height: 42px; padding: 0 14px; border: 0; border-radius: 8px; background: var(--hn-accent); color: #fff; font-weight: 700; cursor: pointer; }
.source-link { display: inline-flex; min-height: 42px; align-items: center; justify-content: center; padding: 0 12px; border: 1px solid var(--hn-border-strong); border-radius: 8px; color: var(--hn-accent); font-size: 12px; font-weight: 800; text-decoration: none; white-space: nowrap; }
.source-link:hover { border-color: var(--hn-accent); background: var(--hn-soft); }
:global(:root[data-hn-theme="dark"] .open-button),
:global(:root.theme-dark .open-button) { color: var(--hn-on-accent); }

@media (max-width: 520px) {
  .external-card { min-height: 0; padding: 18px; }
  .metrics { gap: 6px; }
  .metrics div { padding: 8px; }
  .metrics dd { font-size: 16px; }
}
</style>
