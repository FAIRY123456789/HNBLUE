<template>
  <article class="result-panel">
    <div class="result-head">
      <span>{{ tag }}</span>
      <strong>{{ displayValue }} t/ha</strong>
    </div>
    <dl>
      <div><dt>模型状态</dt><dd>{{ status }}</dd></div>
      <div><dt>估算时间</dt><dd>{{ time || '-' }}</dd></div>
      <div><dt>核心参数</dt><dd>{{ summary }}</dd></div>
    </dl>
    <p>该结果用于结构参数分析和情景比较，正式业务使用仍需结合实测、遥感和文献证据核验</p>
  </article>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  value: { type: [Number, String], default: null },
  input: { type: Object, default: () => ({}) },
  status: { type: String, default: '模型估算完成' },
  time: { type: String, default: '' },
  tag: { type: String, default: '模型估算结果' },
});

const displayValue = computed(() => Number(props.value ?? 0).toFixed(3));
const summary = computed(() => {
  const input = props.input || {};
  return `树高 ${input.treeHeight ?? '-'} m，胸径 ${input.dbh ?? '-'} cm，冠幅 ${input.canopy ?? '-'} m`;
});
</script>

<style scoped>
.result-panel {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid rgba(31, 138, 122, 0.28);
  border-radius: 8px;
  background: color-mix(in srgb, var(--hn-accent) 8%, var(--hn-card));
}
.result-head { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; }
.result-head span { color: var(--hn-accent); font-size: 13px; font-weight: 900; }
.result-head strong { color: var(--hn-text); font-size: clamp(26px, 4vw, 40px); }
dl { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin: 0; }
dt { color: var(--hn-muted); font-size: 12px; }
dd { margin: 4px 0 0; color: var(--hn-text); font-weight: 800; line-height: 1.5; }
p { margin: 0; color: var(--hn-muted); line-height: 1.65; }
@media (max-width: 760px) { dl { grid-template-columns: 1fr; } .result-head { align-items: flex-start; flex-direction: column; } }
</style>
