<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="文献比对"
      eyebrow="Evidence Check"
      subtitle="用于把模型估算结果放回 BAAD 等公开文献区间中理解；模型服务未连接时保持可读空状态"
      :tags="['BAAD', '模型解释', '服务状态']"
    />

    <section class="compare-panel">
      <SectionHeader title="区间验证" eyebrow="Literature Range" subtitle="该功能依赖外部模型服务，默认不自动请求，避免首屏报错">
        <ActionButton label="查看状态" variant="secondary" @click="loadRange" />
      </SectionHeader>

      <div class="compare-layout">
        <form class="input-card" @submit.prevent="compareValue">
          <label>
            <span>估算碳储量</span>
            <input v-model.number="userValue" type="number" min="0" step="0.1" placeholder="单位：tC/ha" />
          </label>
          <ActionButton label="执行比对" :disabled="!userValue" />
          <div v-if="rangeLoaded" class="result-area">
            <strong>参考区间 {{ range.min }} - {{ range.max }} tC/ha</strong>
            <div class="bar-track"><i :style="{ left: offsetLeft + '%', background: resultColor }"></i></div>
            <p>{{ explanationText }}</p>
          </div>
          <EmptyState v-else title="模型服务未启动" message="模型服务未连接时，本页仅展示结构和说明，不自动弹出失败提示" />
        </form>

        <article class="desc-card">
          <h3>BAAD 数据用途</h3>
          <p>BAAD 可作为生物量和碳储估算的公开参考区间，用于学习解释和模型结果合理性检查，不直接作为政府审批结论。</p>
          <dl>
            <div><dt>依赖服务</dt><dd>外部模型接口</dd></div>
            <div><dt>服务状态</dt><dd>当前未连接</dd></div>
            <div><dt>交付状态</dt><dd>保持可读空状态</dd></div>
          </dl>
        </article>
      </div>
    </section>
  </PageShell>
</template>

<script setup>
import { computed, ref } from 'vue';
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import SectionHeader from '@/components/common/SectionHeader.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import ActionButton from '@/components/common/ActionButton.vue';

const userValue = ref(null);
const range = ref({ min: 0, max: 0 });
const rangeLoaded = ref(false);
const resultColor = ref('#0d6b57');
const explanationText = ref('等待输入估算值。');

const offsetLeft = computed(() => {
  if (!rangeLoaded.value || !userValue.value) return 0;
  const spread = Math.max(1, range.value.max - range.value.min);
  return Math.max(0, Math.min(((Number(userValue.value) - range.value.min) / spread) * 100, 100));
});

function loadRange() {
  rangeLoaded.value = false;
  explanationText.value = '模型服务当前未连接，可继续查看公开文献证据和数据资产记录。';
}

function compareValue() {
  if (!rangeLoaded.value) {
    explanationText.value = '当前数据范围未覆盖该区间，请检查模型服务或使用已有文献记录。';
    return;
  }
  const value = Number(userValue.value);
  if (value < range.value.min) {
    resultColor.value = '#a13a3a';
    explanationText.value = '估算值低于参考区间，建议检查输入参数、样地类型和模型适用边界。';
  } else if (value > range.value.max) {
    resultColor.value = '#d99b35';
    explanationText.value = '估算值高于参考区间，需结合样地结构、物种组成和数据来源进一步解释。';
  } else {
    resultColor.value = '#0d6b57';
    explanationText.value = '估算值位于参考区间内，可作为模型解释和学习讨论的辅助依据。';
  }
}
</script>

<style scoped>
.compare-panel {
  width: var(--hn-page);
  margin: 0 auto 48px;
  padding: 24px;
  border: 1px solid var(--hn-border);
  border-radius: var(--hn-radius);
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.compare-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
}

.input-card,
.desc-card {
  padding: 18px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel-solid);
}

.input-card {
  display: grid;
  gap: 14px;
}

label {
  display: grid;
  gap: 8px;
}

label span,
dt {
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

.result-area {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 8px;
  background: var(--hn-soft);
}

.bar-track {
  position: relative;
  height: 14px;
  border-radius: 999px;
  background: rgba(13, 107, 87, 0.16);
}

.bar-track i {
  position: absolute;
  top: -5px;
  width: 8px;
  height: 24px;
  border-radius: 999px;
}

.desc-card h3 {
  margin: 0 0 10px;
}

.desc-card p {
  color: var(--hn-muted);
  line-height: 1.65;
}

dl {
  display: grid;
  gap: 10px;
}

dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid var(--hn-border);
  padding-top: 10px;
}

dd {
  margin: 0;
  color: var(--hn-text);
  font-weight: 900;
  text-align: right;
}

@media (max-width: 850px) {
  .compare-layout { grid-template-columns: 1fr; }
}
</style>
