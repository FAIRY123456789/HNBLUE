<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="碳溯"
      eyebrow="CarbonSeek"
      subtitle="面向学习、模型解释和案例推演，展示碳储估算从数据输入到结果解释的过程"
      :tags="['模型流程', '碳储估算', '案例推演']"
    >
      <template #visual>
        <div class="trace-orbit">
          <span class="node n1">输入</span>
          <span class="node n2">模型</span>
          <span class="node n3">碳储</span>
          <span class="node n4">解释</span>
        </div>
      </template>
    </PageHero>

    <section class="process-grid">
      <article v-for="step in steps" :key="step.title" class="reveal-card">
        <span>{{ step.index }}</span>
        <h2>{{ step.title }}</h2>
        <p>{{ step.desc }}</p>
      </article>
    </section>

    <section class="panel map-panel">
      <SectionHeader title="探索图" eyebrow="Blue Map" subtitle="用于学习区域差异、证据来源和模型解释，支持海南主岛与三沙群岛独立查看">
        <router-link class="soft-link" to="/visual">打开地图页</router-link>
      </SectionHeader>
      <HainanBlueCarbonMap />
    </section>

    <section class="viz-layout">
      <article class="panel chart-panel">
        <SectionHeader title="模型解释" eyebrow="Model Flow" subtitle="说明 CatBoost/BAAD 等模型资产的输入、输出、关系图和适用范围，并结合真实输入理解结果">
          <button class="soft-link ai-open" type="button" @click="openGlobalAssistant">打开 AI 碳助手</button>
        </SectionHeader>
        <div ref="chartRef" class="chart"></div>
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
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import * as echarts from "echarts";
import PageShell from "@/components/common/PageShell.vue";
import UnifiedNav from "@/components/common/UnifiedNav.vue";
import PageHero from "@/components/common/PageHero.vue";
import SectionHeader from "@/components/common/SectionHeader.vue";
import HainanBlueCarbonMap from "@/components/HainanBlueCarbonMap.vue";

const chartRef = ref(null);
let chart = null;

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

const modules = [
  { tag: "模型", title: "结构参数", desc: "输入结构特征，理解生物量估算过程", to: "/structure-predictor" },
  { tag: "推演", title: "虚拟样地", desc: "构造参数扰动，观察估算结果变化", to: "/virtual-plot-designer" },
  { tag: "地图", title: "蓝碳地图", desc: "探索海南区域记录、证据状态和继承详情", to: "/visual" },
  { tag: "估值", title: "碳储价值", desc: "拆分碳储、碳价和价值参考边界", to: "/carbon-value-converter" },
  { tag: "证据", title: "文献比对", desc: "把模型结果放回证据区间中理解", to: "/literature-compare" },
  { tag: "数据", title: "数据资产", desc: "查看公开来源、区域指标和文献证据", to: "/v2-public-data" },
  { tag: "AI", title: "碳助手", desc: "面向学习解释的问答入口", to: "/ai-assistant" },
];

function renderChart() {
  if (!chartRef.value) return;
  if (!chart) chart = echarts.init(chartRef.value);
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'graph',
      layout: 'none',
      roam: false,
      symbolSize: 72,
      label: { show: true, color: '#123f37', fontWeight: 900 },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [4, 12],
      lineStyle: { color: '#0d6b57', width: 2, curveness: 0.08 },
      itemStyle: { color: '#dff1ec', borderColor: '#0d6b57', borderWidth: 2 },
      data: [
        { name: '结构参数', x: 80, y: 160 },
        { name: '公开数据', x: 230, y: 80 },
        { name: 'CatBoost', x: 390, y: 160 },
        { name: '碳储输出', x: 540, y: 80 },
        { name: '解释边界', x: 540, y: 240 },
      ],
      links: [
        { source: '结构参数', target: 'CatBoost' },
        { source: '公开数据', target: 'CatBoost' },
        { source: 'CatBoost', target: '碳储输出' },
        { source: 'CatBoost', target: '解释边界' },
      ],
    }],
  }, true);
}
function resizeChart() {
  chart?.resize();
}

function openGlobalAssistant() {
  window.dispatchEvent(new CustomEvent("hnblue-open-ai"));
}

onMounted(async () => {
  document.documentElement.style.overflow = "auto";
  document.body.style.overflow = "auto";
  await nextTick();
  renderChart();
  window.addEventListener("resize", resizeChart);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeChart);
  chart?.dispose();
});
</script>

<style scoped>
.trace-orbit {
  position: relative;
  width: 310px;
  height: 230px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff, #e4f3ef);
  box-shadow: var(--hn-shadow-soft);
}

.trace-orbit::before,
.trace-orbit::after {
  content: "";
  position: absolute;
  inset: 42px;
  border: 1px dashed rgba(13, 107, 87, 0.28);
  border-radius: 50%;
}

.node {
  position: absolute;
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--hn-accent);
  color: #fff;
  font-weight: 900;
  box-shadow: var(--hn-shadow-soft);
  animation: bob 4s ease-in-out infinite;
}

.n1 { left: 28px; top: 28px; }
.n2 { right: 34px; top: 42px; animation-delay: 0.5s; }
.n3 { left: 72px; bottom: 28px; animation-delay: 1s; }
.n4 { right: 58px; bottom: 34px; animation-delay: 1.5s; }

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

.chart {
  width: 100%;
  height: 360px;
}

.explain-panel ul {
  margin: 0;
  padding-left: 20px;
}

.explain-panel li + li {
  margin-top: 10px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 18px;
  padding-bottom: 48px;
}

.module-card,
.reveal-card {
  color: var(--hn-text);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.module-card:hover,
.reveal-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--hn-shadow);
}

@keyframes bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}

@media (max-width: 900px) {
  .process-grid,
  .viz-layout,
  .module-grid {
    grid-template-columns: 1fr;
  }

  .trace-orbit {
    width: 100%;
  }
}
</style>
