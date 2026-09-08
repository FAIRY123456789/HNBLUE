<template>
  <section class="plot-tool">
    <div class="toolbar-row">
      <div class="status-pill" :class="health.ok ? 'ok' : 'warn'">
        <strong>{{ health.ok ? '模型服务已连接' : '模型服务当前不可用，参数仍可编辑；连接恢复后可提交估算' }}</strong>
        <span v-if="health.message">{{ health.message }}</span>
      </div>
      <ActionButton label="重新检查" variant="secondary" :disabled="health.loading" @click="loadHealth" />
    </div>

    <section class="settings-panel">
      <SectionHeader title="全局环境参数" eyebrow="Shared Environment" subtitle="各样地默认使用该组环境参数，高级参数展开后可以单独覆盖" />
      <div class="field-grid compact">
        <label><span>纬度</span><input v-model.number="shared.latitude" type="number" step="0.0001" /></label>
        <label><span>经度</span><input v-model.number="shared.longitude" type="number" step="0.0001" /></label>
        <label><span>年均温 ℃</span><input v-model.number="shared.mat" type="number" step="0.1" /></label>
        <label><span>年降水量 mm</span><input v-model.number="shared.map" type="number" step="1" /></label>
      </div>
    </section>

    <section class="scenario-panel">
      <SectionHeader title="情景推演" eyebrow="Scenario" subtitle="预设只修改输入参数，用于模型情景比较，不代表真实政策效果" />
      <div class="scenario-row">
        <label><span>情景名称</span><select v-model="scenario.name"><option>基准情景</option><option>生长改善情景</option><option>结构扰动情景</option><option>自定义情景</option></select></label>
        <label><span>树高变化</span><input v-model.number="scenario.treeHeight" type="number" step="1" /></label>
        <label><span>胸径变化</span><input v-model.number="scenario.dbh" type="number" step="1" /></label>
        <label><span>冠幅变化</span><input v-model.number="scenario.canopy" type="number" step="1" /></label>
        <ActionButton label="应用情景" @click="applyScenario" />
      </div>
      <p class="scenario-note">当前变化：树高 {{ scenario.treeHeight }}%，胸径 {{ scenario.dbh }}%，冠幅 {{ scenario.canopy }}%</p>
    </section>

    <div class="action-row">
      <ActionButton label="添加样地" @click="addPlot" />
      <ActionButton label="批量估算" :disabled="isPredicting || !health.ok" @click="runBatchPrediction" />
      <ActionButton label="清空样地" variant="secondary" @click="clearPlots" />
      <ActionButton label="导出结果" variant="secondary" :disabled="!results.length" @click="exportResults" />
    </div>
    <div v-if="error" class="inline-error">{{ error }}</div>

    <section class="plot-grid">
      <article v-for="plot in plots" :key="plot.id" class="plot-card" :class="{ invalid: plotErrors[plot.id] }">
        <header>
          <input v-model="plot.name" aria-label="样地名称" />
          <span>{{ plot.scenario }}</span>
        </header>
        <div class="field-grid compact">
          <label><span>平均树高 m *</span><input v-model.number="plot.treeHeight" type="number" step="0.1" /></label>
          <label><span>胸径 cm *</span><input v-model.number="plot.dbh" type="number" step="0.1" /></label>
          <label><span>冠幅 m *</span><input v-model.number="plot.canopy" type="number" step="0.1" /></label>
          <label><span>样地面积 m²</span><input v-model.number="plot.area" type="number" step="1" /></label>
          <label><span>林龄 年</span><input v-model.number="plot.age" type="number" step="1" /></label>
          <label><span>木材密度 g/cm³</span><input v-model.number="plot.cd" type="number" step="0.01" /></label>
          <label><span>植株密度 株/公顷</span><input v-model.number="plot.density" type="number" step="1" /></label>
        </div>
        <details>
          <summary>高级参数</summary>
          <div class="field-grid compact">
            <label><span>纬度</span><input v-model.number="plot.latitude" type="number" step="0.0001" /></label>
            <label><span>经度</span><input v-model.number="plot.longitude" type="number" step="0.0001" /></label>
            <label><span>年均温</span><input v-model.number="plot.mat" type="number" step="0.1" /></label>
            <label><span>年降水量</span><input v-model.number="plot.map" type="number" step="1" /></label>
            <label><span>植被类型</span><select v-model="plot.vegetation"><option value="mangrove">红树林</option><option value="forest">森林</option></select></label>
            <label><span>生长条件</span><select v-model="plot.growingcondition"><option value="natural">自然生长</option><option value="managed">人工管理</option></select></label>
            <label><span>植物功能类型</span><select v-model="plot.pft"><option value="broadleaf">阔叶</option><option value="needleleaf">针叶</option></select></label>
          </div>
        </details>
        <p v-if="plotErrors[plot.id]" class="card-error">{{ plotErrors[plot.id] }}</p>
        <footer>
          <ActionButton size="sm" label="复制样地" variant="secondary" @click="copyPlot(plot)" />
          <ActionButton size="sm" label="删除" variant="danger" @click="removePlot(plot.id)" />
        </footer>
      </article>
    </section>

    <section v-if="results.length" class="results-panel">
      <SectionHeader title="方案对比结果" eyebrow="Batch Result" subtitle="图表和表格均来自批量模型预测结果，柱体顶部显示数值" />
      <div ref="chartRef" class="compare-chart"></div>
      <div class="history-table">
        <table>
          <thead><tr><th>样地</th><th>情景</th><th>树高</th><th>胸径</th><th>冠幅</th><th>面积</th><th>预测生物量</th><th>相对变化</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="item in results" :key="item.id">
              <td>{{ item.name }}</td><td>{{ item.scenario }}</td><td>{{ item.treeHeight }} m</td><td>{{ item.dbh }} cm</td><td>{{ item.canopy }} m</td><td>{{ item.area }} m²</td><td>{{ item.prediction }} t/ha</td><td>{{ item.change }}</td><td>{{ item.status }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <AiAnalysisPanel
      v-if="results.length"
      title="方案对比分析"
      :content="ai.content"
      :error="ai.error"
      :is-loading="ai.loading"
      button-label="重新分析"
      @generate="generateAiAnalysis"
    />
  </section>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue';
import * as echarts from 'echarts';
import ActionButton from '@/components/common/ActionButton.vue';
import SectionHeader from '@/components/common/SectionHeader.vue';
import AiAnalysisPanel from '@/components/model/AiAnalysisPanel.vue';
import { checkModelHealth, predictCarbonBatch } from '@/services/modelService';
import { parseAiStreamPayload, stripThinkBlocks } from '@/utils/aiStreamParser';

const shared = reactive({ latitude: 19.0, longitude: 109.5, mat: 25.5, map: 1800 });
const scenario = reactive({ name: '基准情景', treeHeight: 0, dbh: 0, canopy: 0 });
const health = reactive({ ok: false, loading: false, message: '' });
const plots = ref([createPlot(1, '样地 A', 8, 12, 4), createPlot(2, '样地 B', 9, 14, 4.5)]);
const results = ref([]);
const plotErrors = reactive({});
const isPredicting = ref(false);
const error = ref('');
const chartRef = ref(null);
const ai = reactive({ loading: false, content: '', error: '' });
let nextId = 3;

function createPlot(id, name, treeHeight = 8, dbh = 12, canopy = 4) {
  return { id, name, scenario: '基准情景', treeHeight, dbh, canopy, area: 400, age: 25, cd: 0.7, density: 1200, latitude: '', longitude: '', mat: '', map: '', vegetation: 'mangrove', growingcondition: 'natural', pft: 'broadleaf' };
}

async function loadHealth() {
  health.loading = true;
  try {
    const data = await checkModelHealth();
    health.ok = Boolean(data?.ok);
    health.message = data?.message || '';
  } catch (err) {
    health.ok = false;
    health.message = err.message || '模型服务连接失败';
  } finally {
    health.loading = false;
  }
}

function addPlot() { plots.value.push(createPlot(nextId++, `样地 ${nextId - 1}`)); }
function copyPlot(plot) { plots.value.push({ ...plot, id: nextId++, name: `${plot.name} 副本` }); }
function removePlot(id) {
  if (plots.value.length === 1 && !confirm('确认删除最后一个样地吗？删除后将自动创建一个空样地。')) return;
  plots.value = plots.value.filter((plot) => plot.id !== id);
  if (!plots.value.length) addPlot();
}
function clearPlots() { if (confirm('确认清空全部样地并恢复两个默认样地吗？')) plots.value = [createPlot(1, '样地 A', 8, 12, 4), createPlot(2, '样地 B', 9, 14, 4.5)]; }
function applyScenario() {
  plots.value = plots.value.map((plot) => ({ ...plot, scenario: scenario.name, treeHeight: round(plot.treeHeight * (1 + scenario.treeHeight / 100)), dbh: round(plot.dbh * (1 + scenario.dbh / 100)), canopy: round(plot.canopy * (1 + scenario.canopy / 100)) }));
}
function round(value) { return Math.round(Number(value) * 100) / 100; }

function validatePlots() {
  Object.keys(plotErrors).forEach((key) => delete plotErrors[key]);
  let ok = true;
  for (const plot of plots.value) {
    if (!plot.name || !Number(plot.treeHeight) || !Number(plot.dbh) || !Number(plot.canopy)) {
      plotErrors[plot.id] = '样地名称、平均树高、胸径和冠幅均为必填';
      ok = false;
    }
  }
  return ok;
}

function toPayload(plot) {
  return { treeHeight: Number(plot.treeHeight), dbh: Number(plot.dbh), canopy: Number(plot.canopy), latitude: Number(plot.latitude || shared.latitude), longitude: Number(plot.longitude || shared.longitude), mat: Number(plot.mat || shared.mat), map: Number(plot.map || shared.map), age: Number(plot.age), 'c.d': Number(plot.cd), vegetation: plot.vegetation, growingcondition: plot.growingcondition, pft: plot.pft };
}

async function runBatchPrediction() {
  error.value = '';
  if (!health.ok) { error.value = '模型服务当前不可用，样地参数已保留，请连接恢复后再估算'; return; }
  if (!validatePlots()) { error.value = '存在输入不完整的样地，请先补全标记项'; return; }
  isPredicting.value = true;
  try {
    const payload = plots.value.map(toPayload);
    const data = await predictCarbonBatch(payload);
    const predictions = data?.predictions || [];
    const base = Number(predictions[0] || 0);
    const time = new Date().toLocaleString('zh-CN', { hour12: false });
    results.value = plots.value.map((plot, index) => {
      const prediction = Number(predictions[index] ?? 0);
      const change = base ? `${(((prediction - base) / base) * 100).toFixed(1)}%` : '-';
      return { ...plot, prediction, change: index === 0 ? '基准' : change, status: '估算完成', time, environment: `${plot.latitude || shared.latitude}, ${plot.longitude || shared.longitude}; ${plot.mat || shared.mat} ℃; ${plot.map || shared.map} mm` };
    });
    await nextTick();
    renderChart();
    generateAiAnalysis();
  } catch (err) {
    error.value = err.message || '批量估算失败';
  } finally {
    isPredicting.value = false;
  }
}

function renderChart() {
  if (!chartRef.value) return;
  const chart = echarts.init(chartRef.value);
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 58, right: 24, top: 36, bottom: 48 },
    xAxis: { type: 'category', data: results.value.map((item) => item.name), axisLabel: { color: getTextColor() } },
    yAxis: { type: 'value', name: 't/ha', axisLabel: { color: getTextColor() }, nameTextStyle: { color: getTextColor() } },
    series: [{ type: 'bar', data: results.value.map((item) => item.prediction), itemStyle: { color: '#1f8a7a' }, label: { show: true, position: 'top', color: getTextColor(), formatter: (params) => Number(params.value).toFixed(2) } }],
  });
}
function getTextColor() { return getComputedStyle(document.documentElement).getPropertyValue('--hn-text').trim() || '#173f38'; }

function exportResults() {
  const headers = ['样地编号','样地名称','情景名称','样地面积','树高','胸径','冠幅','林龄','木材密度','植株密度','环境参数','分类参数','预测生物量','相对变化','预测时间'];
  const rows = results.value.map((item) => [item.id,item.name,item.scenario,item.area,item.treeHeight,item.dbh,item.canopy,item.age,item.cd,item.density,item.environment,`${item.vegetation}/${item.growingcondition}/${item.pft}`,item.prediction,item.change,item.time]);
  downloadCsv('virtual-plot-results.csv', [headers, ...rows]);
}
function downloadCsv(filename, rows) {
  const csv = rows.map((row) => row.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\n');
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url; link.download = filename; link.click(); URL.revokeObjectURL(url);
}

async function generateAiAnalysis() {
  if (!results.value.length) return;
  ai.loading = true; ai.content = ''; ai.error = '';
  const lines = results.value.map((item) => `${item.name}：情景 ${item.scenario}，树高 ${item.treeHeight} m，胸径 ${item.dbh} cm，冠幅 ${item.canopy} m，预测 ${item.prediction} t/ha，相对变化 ${item.change}`).join('\n');
  const prompt = `请根据以下虚拟样地模型结果进行方案对比分析。\n${lines}\n\n请说明：1. 各样地模型结果差异 2. 主要参数变化 3. 可能的结构性原因 4. 哪些结论仅属于情景模拟 5. 哪些数据需要实测验证。不得表述为正式核证结论。`;
  try { await streamAi(prompt, (delta) => { ai.content = stripThinkBlocks(ai.content + delta); }); }
  catch { ai.error = '模型估算已完成，智能解释服务当前不可用'; }
  finally { ai.loading = false; }
}
async function streamAi(prompt, onDelta) {
  const response = await fetch('/api/chat/stream-carbon', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ message: prompt }) });
  if (!response.ok || !response.body) throw new Error('AI 服务不可用');
  const reader = response.body.getReader(); const decoder = new TextDecoder('utf-8'); let buffer = '';
  while (true) { const { value, done } = await reader.read(); if (done) break; buffer += decoder.decode(value, { stream: true }); const parts = buffer.split('\n'); buffer = parts.pop() || ''; for (const line of parts) { const trimmed = line.trim(); if (!trimmed.startsWith('data:')) continue; const parsed = parseAiStreamPayload(trimmed.slice(5).trim()); if (parsed.error) throw new Error(parsed.error); if (parsed.delta) onDelta(parsed.delta); } }
}

onMounted(loadHealth);
</script>

<style scoped>
.plot-tool { width: var(--hn-page); margin: 0 auto 48px; display: grid; gap: 18px; }
.toolbar-row, .action-row, .scenario-row { display: flex; gap: 12px; align-items: end; flex-wrap: wrap; }
.status-pill, .settings-panel, .scenario-panel, .results-panel { display: grid; gap: 12px; padding: 16px; border: 1px solid var(--hn-border); border-radius: var(--hn-radius); background: var(--hn-card); box-shadow: var(--hn-shadow-soft); }
.status-pill { flex: 1; min-width: 260px; box-shadow: none; }
.status-pill span { color: var(--hn-muted); font-size: 13px; }
.status-pill.ok { border-color: rgba(31, 138, 122, 0.32); }
.status-pill.warn { border-color: rgba(217, 155, 53, 0.38); }
.field-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.field-grid.compact { grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); }
label { display: grid; gap: 7px; min-width: 150px; }
label span { color: var(--hn-muted); font-size: 12px; font-weight: 800; }
input, select { height: 40px; border: 1px solid var(--hn-border-strong); border-radius: 7px; background: var(--hn-control-bg); color: var(--hn-text); padding: 0 10px; }
.scenario-note { margin: 0; color: var(--hn-muted); }
.inline-error, .card-error { color: var(--hn-danger); font-weight: 800; }
.plot-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.plot-card { display: grid; gap: 12px; padding: 16px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-card); }
.plot-card.invalid { border-color: rgba(161, 58, 58, 0.42); }
.plot-card header, .plot-card footer { display: flex; gap: 10px; justify-content: space-between; align-items: center; }
.plot-card header input { font-size: 18px; font-weight: 900; }
.plot-card header span { color: var(--hn-accent); font-size: 12px; font-weight: 900; }
.compare-chart { width: 100%; height: 320px; }
.history-table { overflow-x: auto; }
table { width: 100%; min-width: 920px; border-collapse: collapse; }
th, td { padding: 11px; border-bottom: 1px solid var(--hn-border); text-align: left; color: var(--hn-text); white-space: nowrap; }
th { background: var(--hn-soft); font-size: 13px; }
@media (max-width: 960px) { .plot-grid { grid-template-columns: 1fr; } }
</style>
