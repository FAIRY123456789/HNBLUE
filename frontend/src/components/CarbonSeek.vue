<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="碳溯"
      eyebrow="CarbonSeek"
      subtitle="面向蓝碳数据研判、模型解释与案例推演，展示碳储估算从数据输入到结果核验的完整过程"
      :tags="['模型流程', '碳储估算', '案例推演']"
    />

    <section class="process-grid">
      <article v-for="step in steps" :key="step.title" class="reveal-card">
        <span>{{ step.index }}</span>
        <h2>{{ step.title }}</h2>
        <p>{{ step.desc }}</p>
      </article>
    </section>

    <section class="panel map-panel">
      <SectionHeader title="海南蓝碳空间信息与证据图谱" eyebrow="Spatial Evidence Map" subtitle="用于研判区域差异、核验数据来源并关联模型解释，支持海南主岛与三沙市分区查阅">
        <router-link class="soft-link" to="/visual">打开地图页</router-link>
      </SectionHeader>
      <HainanBlueCarbonMap />
    </section>

    <section class="viz-layout">
      <article class="panel chart-panel">
        <SectionHeader title="模型解释" eyebrow="Model Flow" subtitle="说明 CatBoost/BAAD 等模型资产的输入、输出、关系图和适用范围，并结合真实输入理解结果">
          <button class="soft-link ai-open" type="button" @click="openGlobalAssistant">打开 AI 碳助手</button>
        </SectionHeader>
        <div class="model-pipeline" aria-label="碳储估算模型流程">
          <template v-for="(item, index) in modelPipeline" :key="item.title">
            <article class="pipeline-node">
              <span>{{ item.index }}</span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </article>
            <b v-if="index < modelPipeline.length - 1" class="pipeline-link" aria-hidden="true"></b>
          </template>
        </div>
      </article>
      <article class="panel explain-panel">
        <SectionHeader title="解释重点" eyebrow="Model Notes" />
        <ul>
          <li v-for="item in explanations" :key="item">{{ item }}</li>
        </ul>
      </article>
    </section>

    <section class="module-grid">
      <router-link v-for="module in modules" :key="module.title" :to="module.to" class="module-card">
        <span>{{ module.tag }}</span>
        <h2>{{ module.title }}</h2>
        <p>{{ module.desc }}</p>
      </router-link>
    </section>
  </PageShell>
</template>

<script setup>
import { onMounted } from "vue";
import PageShell from "@/components/common/PageShell.vue";
import UnifiedNav from "@/components/common/UnifiedNav.vue";
import PageHero from "@/components/common/PageHero.vue";
import SectionHeader from "@/components/common/SectionHeader.vue";
import HainanBlueCarbonMap from "@/components/HainanBlueCarbonMap.vue";

const steps = [
  { index: "01", title: "理解来源", desc: "查看面积记录、文献证据、遥感指标和区域指标的来源口径" },
  { index: "02", title: "建立模型", desc: "解释结构变量、生物量和碳储之间的估算关系" },
  { index: "03", title: "对照证据", desc: "把估算结果放回文献区间和区域记录中理解" },
  { index: "04", title: "案例推演", desc: "通过参数扰动和情景假设观察结果变化" },
];

const explanations = [
  "区分观测事实、参考指标、模型估算和情景推演。",
  "保留来源、年份、方法和质量信息，避免孤立解释数值。",
  "碳储价值转换用于教学和参考，不直接等同正式交易价格。",
  "接口不可用时显示轻量空状态，页面结构保持完整。",
];

const modelPipeline = [
  { index: "01", title: "输入变量", desc: "结构参数与环境变量" },
  { index: "02", title: "数据校验", desc: "单位、来源与质量检查" },
  { index: "03", title: "CatBoost", desc: "非线性关系建模" },
  { index: "04", title: "碳储估算", desc: "生物量与碳储输出" },
  { index: "05", title: "证据核验", desc: "文献区间与区域记录" },
];

const modules = [
  { tag: "模型", title: "结构参数", desc: "输入结构特征，理解生物量估算过程", to: "/structure-predictor" },
  { tag: "推演", title: "虚拟样地", desc: "构造参数扰动，观察估算结果变化", to: "/virtual-plot-designer" },
  { tag: "地图", title: "蓝碳地图", desc: "探索海南区域记录、证据状态和继承详情", to: "/visual" },
  { tag: "估值", title: "碳储价值", desc: "拆分碳储、碳价和价值参考边界", to: "/carbon-value-converter" },
  { tag: "证据", title: "文献比对", desc: "把模型结果放回证据区间中理解", to: "/literature-compare" },
  { tag: "AI", title: "碳助手", desc: "面向学习解释的问答入口", to: "/ai-assistant" },
];

function openGlobalAssistant() {
  window.dispatchEvent(new CustomEvent("hnblue-open-ai"));
}

onMounted(() => {
  document.documentElement.style.overflow = "auto";
  document.body.style.overflow = "auto";
});
</script>

<style scoped>
.process-grid,
.viz-layout,
.module-grid,
.map-panel {
  width: var(--hn-page);
  margin-left: auto;
  margin-right: auto;
}

.process-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.process-grid article,
.panel,
.module-card {
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.process-grid article,
.panel,
.module-card {
  padding: 22px;
}

.map-panel {
  margin-top: 18px;
}

.process-grid span,
.module-card span,
.soft-tag,
.soft-link {
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 900;
}

.soft-link {
  text-decoration: none;
  white-space: nowrap;
}

.ai-open {
  border: 0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
}

h2 {
  margin: 12px 0 8px;
  font-size: 23px;
}

p,
li {
  color: var(--hn-muted);
  line-height: 1.66;
}

.viz-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 16px;
  margin-top: 18px;
}

.chart-panel,
.explain-panel {
  padding: 18px 20px;
}

.chart-panel > p { margin: 0; }

.model-pipeline {
  min-height: 210px;
  display: grid;
  grid-template-columns: minmax(112px, 1fr) 34px minmax(112px, 1fr) 34px minmax(112px, 1fr) 34px minmax(112px, 1fr) 34px minmax(112px, 1fr);
  align-items: center;
  padding: 14px 4px 6px;
}

.pipeline-node {
  min-height: 112px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  padding: 11px 12px;
  border: 1px solid var(--hn-border-strong);
  border-top: 4px solid var(--hn-accent);
  border-radius: 6px;
  background: var(--hn-card);
  box-shadow: var(--hn-shadow-soft);
}

.pipeline-node span {
  color: var(--hn-accent);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.pipeline-node strong {
  color: var(--hn-text);
  font-size: 17px;
}

.pipeline-node small {
  color: var(--hn-muted);
  font-size: 12px;
  line-height: 1.4;
}

.pipeline-link {
  width: 100%;
  height: 2px;
  background: var(--hn-border-strong);
}

.explain-panel ul {
  margin: 0;
  padding-left: 20px;
}

.explain-panel li + li {
  margin-top: 6px;
}

.explain-panel li { line-height: 1.5; }

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 12px;
  padding-bottom: 0;
}

.module-card,
.reveal-card {
  color: var(--hn-text);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.module-card {
  min-height: 54px;
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
}

.module-card > span {
  width: 36px;
  flex: 0 0 36px;
}

.module-card h2 {
  margin: 0;
  font-size: 17px;
  white-space: nowrap;
}

.module-card p {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--hn-muted);
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-card:hover,
.reveal-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--hn-shadow);
}

@media (max-width: 900px) {
  .process-grid,
  .viz-layout,
  .module-grid {
    grid-template-columns: 1fr;
  }

  .model-pipeline {
    grid-template-columns: 1fr;
    justify-items: stretch;
  }

  .pipeline-node { min-height: 118px; }

  .pipeline-link {
    width: 2px;
    height: 24px;
    justify-self: center;
  }

  .module-card {
    grid-template-columns: auto auto minmax(0, 1fr);
  }
}
/* 模型流程与解释重点同步压缩，减少上下无效留白。 */
.chart-panel,
.explain-panel {
  padding: 14px 18px;
}

.chart-panel :deep(.section-header) {
  margin-bottom: 6px;
}

.chart-panel :deep(.section-header > div) {
  min-width: 0;
  flex: 1;
}

.chart-panel :deep(.subtitle) {
  max-width: none;
  margin-top: 5px;
  font-size: 14px;
  line-height: 1.4;
  white-space: nowrap;
}

.model-pipeline {
  min-height: 164px;
  padding: 8px 2px 2px;
}

.pipeline-node {
  min-height: 90px;
  gap: 3px;
  padding: 8px 10px;
}

.pipeline-node strong {
  font-size: 15px;
}

.pipeline-node small {
  line-height: 1.35;
}

.explain-panel ul {
  gap: 4px;
  margin-top: 8px;
}

.explain-panel li {
  line-height: 1.4;
}

@media (max-width: 760px) {
  .model-pipeline {
    min-height: 0;
  }

  .pipeline-node {
    min-height: 82px;
  }
}

@media (max-width: 1180px) {
  .chart-panel :deep(.subtitle) {
    white-space: normal;
  }
}
</style>
