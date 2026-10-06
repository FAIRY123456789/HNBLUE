<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="碳储价值"
      eyebrow="Carbon Value"
      subtitle="把生物量、碳分数和碳价拆开计算，用于教学推演和价值理解，不作为正式交易价格"
      :tags="['教学推演', '价值换算', '边界说明']"
    />

    <section class="converter-panel">
      <SectionHeader title="换算工具" eyebrow="Calculator" subtitle="输入参数后即时计算，并保留本页历史记录，可导出 CSV" />
      <div class="converter-layout">
        <form class="form-card" @submit.prevent="computeValue">
          <label><span>单位生物量 Mg/ha</span><input v-model.number="formData.biomass" type="number" min="0" step="0.1" /></label>
          <label><span>碳分数</span><input v-model.number="formData.carbonFraction" type="number" min="0" max="1" step="0.01" /></label>
          <label><span>参考碳价 元/tC</span><input v-model.number="formData.price" type="number" min="0" step="1" /></label>
          <div class="actions"><ActionButton label="计算价值" /><ActionButton type="button" label="导出记录" variant="secondary" :disabled="!history.length" @click="exportCsv" /></div>
        </form>

        <article class="result-card">
          <span>结果预览</span>
          <h3>{{ result.carbon }} tC/ha</h3>
          <p>参考价值：{{ result.value }} 元/ha</p>
          <small>计算公式：生物量 x 碳分数 x 碳价。该结果仅用于学习推演。</small>
        </article>
      </div>

      <div class="history-grid" v-if="history.length">
        <article v-for="item in history" :key="item.id">
          <strong>{{ item.value }} 元/ha</strong>
          <span>{{ item.carbon }} tC/ha · {{ item.price }} 元/tC</span>
        </article>
      </div>
      <EmptyState v-else title="暂无换算记录" message="输入参数并点击计算后，本页会保留本次浏览器会话中的历史记录" />
    </section>
  </PageShell>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import SectionHeader from '@/components/common/SectionHeader.vue';
import ActionButton from '@/components/common/ActionButton.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const formData = reactive({ biomass: 120, carbonFraction: 0.48, price: 100 });
const history = ref([]);
const result = computed(() => {
  const carbon = Number(formData.biomass || 0) * Number(formData.carbonFraction || 0);
  const value = carbon * Number(formData.price || 0);
  return { carbon: carbon.toFixed(2), value: value.toFixed(2) };
});

function computeValue() {
  history.value.unshift({ id: Date.now(), carbon: result.value.carbon, value: result.value.value, price: formData.price });
}

function exportCsv() {
  const rows = [['carbon_tC_ha', 'price_cny_tC', 'value_cny_ha'], ...history.value.map((item) => [item.carbon, item.price, item.value])];
  const csv = rows.map((row) => row.join(',')).join('\n');
  const link = document.createElement('a');
  link.href = `data:text/csv;charset=utf-8,%EF%BB%BF${encodeURIComponent(csv)}`;
  link.download = 'carbon_value_history.csv';
  document.body.appendChild(link);
  link.click();
  link.remove();
}
</script>

<style scoped>
.converter-panel {
  width: var(--hn-page);
  margin: 0 auto 48px;
  padding: 24px;
  border: 1px solid var(--hn-border);
  border-radius: var(--hn-radius);
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.converter-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.form-card,
.result-card,
.history-grid article {
  padding: 18px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel-solid);
}

.form-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

label {
  display: grid;
  gap: 8px;
}

label span,
.result-card span {
  color: var(--hn-muted);
  font-size: 12px;
  font-weight: 900;
}

input {
  height: 42px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-panel-solid);
  color: var(--hn-text);
  padding: 0 12px;
}

.actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.result-card h3 {
  margin: 8px 0;
  color: var(--hn-accent);
  font-size: 32px;
}

.result-card p,
.result-card small,
.history-grid span {
  color: var(--hn-muted);
  line-height: 1.6;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.history-grid strong,
.history-grid span {
  display: block;
}

.history-grid strong {
  color: var(--hn-accent);
  margin-bottom: 6px;
}

@media (max-width: 900px) {
  .converter-layout,
  .form-card { grid-template-columns: 1fr; }
}
</style>
