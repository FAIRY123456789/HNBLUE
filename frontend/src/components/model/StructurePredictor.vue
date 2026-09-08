<template>
  <section class="model-tool">
    <div class="toolbar-row">
      <div class="status-pill" :class="health.ok ? 'ok' : 'warn'">
        <strong>{{ health.ok ? '模型服务已连接' : '模型服务当前不可用，参数仍可编辑；连接恢复后可提交估算' }}</strong>
        <span v-if="health.message">{{ health.message }}</span>
      </div>
      <ActionButton label="重新检查" variant="secondary" :disabled="health.loading" @click="loadHealth" />
    </div>

    <form class="form-stack" @submit.prevent="submitPredict">
      <section class="form-section open">
        <h3>核心结构参数</h3>
        <div class="field-grid">
          <label v-for="field in coreFields" :key="field.key">
            <span>{{ field.label }} <b>*</b></span>
            <input v-model.number="form[field.key]" type="number" :min="field.min" :max="field.max" :step="field.step" required />
            <small>{{ field.unit }} · {{ field.help }}</small>
          </label>
        </div>
      </section>

      <details open class="form-section">
        <summary>环境与空间参数</summary>
        <div class="field-grid">
          <label v-for="field in envFields" :key="field.key">
            <span>{{ field.label }}</span>
            <input v-model.number="form[field.key]" type="number" :min="field.min" :max="field.max" :step="field.step" />
            <small>{{ field.unit }} · {{ field.help }}</small>
          </label>
        </div>
      </details>

      <details class="form-section">
        <summary>样株属性</summary>
        <div class="field-grid">
          <label v-for="field in plantFields" :key="field.key">
            <span>{{ field.label }}</span>
            <input v-model.number="form[field.key]" type="number" :min="field.min" :max="field.max" :step="field.step" />
            <small>{{ field.unit }} · {{ field.help }}</small>
          </label>
        </div>
      </details>

      <details class="form-section">
        <summary>分类属性</summary>
        <div class="field-grid">
          <label v-for="field in categoryFields" :key="field.key">
            <span>{{ field.label }}</span>
            <select v-model="form[field.key]">
              <option v-for="option in field.options" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
            <small>{{ field.help }}</small>
          </label>
        </div>
      </details>

      <div v-if="formError" class="inline-error">{{ formError }}</div>
      <div class="action-row">
        <ActionButton label="估算生物量" type="submit" :disabled="isPredicting || !health.ok" />
        <ActionButton label="重置参数" variant="secondary" type="button" @click="resetForm" />
        <ActionButton label="导出历史记录" variant="secondary" type="button" :disabled="!history.length" @click="exportHistory" />
      </div>
    </form>

    <ModelResultPanel v-if="result" :value="result.prediction" :input="result.input" :time="result.time" />
    <AiAnalysisPanel
      v-if="result"
      title="结构参数智能解释"
      :content="ai.content"
      :error="ai.error"
      :is-loading="ai.loading"
      :status="ai.status"
      :disabled="!result"
      @generate="generateAiExplanation"
    />

    <section class="history-panel">
      <SectionHeader title="历史记录" eyebrow="Prediction History" subtitle="每次预测成功后记录输入参数、结果和时间，可导出 CSV" />
      <div class="history-table">
        <table>
          <thead><tr><th>时间</th><th>树高</th><th>胸径</th><th>冠幅</th><th>预测生物量</th></tr></thead>
          <tbody>
            <tr v-if="!history.length"><td colspan="5">暂无历史记录</td></tr>
            <tr v-for="item in history" :key="item.time">
              <td>{{ item.time }}</td><td>{{ item.treeHeight }} m</td><td>{{ item.dbh }} cm</td><td>{{ item.canopy }} m</td><td>{{ item.prediction }} t/ha</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import ActionButton from '@/components/common/ActionButton.vue';
import SectionHeader from '@/components/common/SectionHeader.vue';
import ModelResultPanel from '@/components/model/ModelResultPanel.vue';
import AiAnalysisPanel from '@/components/model/AiAnalysisPanel.vue';
import { checkModelHealth, predictCarbon } from '@/services/modelService';
import { parseAiStreamPayload, stripThinkBlocks } from '@/utils/aiStreamParser';

const HISTORY_KEY = 'hnblue_structure_prediction_history';
const defaults = {
  treeHeight: 8,
  dbh: 12,
  canopy: 4,
  latitude: 19.0,
  longitude: 109.5,
  mat: 25.5,
  map: 1800,
  age: 25,
  cd: 0.7,
  vegetation: 'mangrove',
  growingcondition: 'natural',
  pft: 'broadleaf',
};
const form = reactive({ ...defaults });
const health = reactive({ ok: false, loading: false, message: '' });
const result = ref(null);
const history = ref([]);
const isPredicting = ref(false);
const formError = ref('');
const ai = reactive({ loading: false, content: '', error: '', status: '' });
let aiEventSource = null;

const coreFields = [
  { key: 'treeHeight', label: '平均树高', unit: 'm', min: 0.1, max: 80, step: 0.1, help: '描述林分垂直结构' },
  { key: 'dbh', label: '胸径', unit: 'cm', min: 0.1, max: 200, step: 0.1, help: '用于生物量模型估算' },
  { key: 'canopy', label: '冠幅', unit: 'm', min: 0.1, max: 80, step: 0.1, help: '反映树冠水平扩展' },
];
const envFields = [
  { key: 'latitude', label: '纬度', unit: '°', min: -90, max: 90, step: 0.0001, help: '样地空间位置' },
  { key: 'longitude', label: '经度', unit: '°', min: -180, max: 180, step: 0.0001, help: '样地空间位置' },
  { key: 'mat', label: '年均温', unit: '℃', min: -20, max: 45, step: 0.1, help: '气候背景参数' },
  { key: 'map', label: '年降水量', unit: 'mm', min: 0, max: 8000, step: 1, help: '降水背景参数' },
];
const plantFields = [
  { key: 'age', label: '林龄', unit: '年', min: 1, max: 300, step: 1, help: '样株或林分年龄' },
  { key: 'cd', label: '木材密度', unit: 'g/cm³', min: 0.1, max: 1.5, step: 0.01, help: '模型参数 c.d' },
];
const categoryFields = [
  { key: 'vegetation', label: '植被类型', help: '模型分类输入', options: [{ value: 'mangrove', label: '红树林' }, { value: 'forest', label: '森林' }] },
  { key: 'growingcondition', label: '生长条件', help: '模型分类输入', options: [{ value: 'natural', label: '自然生长' }, { value: 'managed', label: '人工管理' }] },
  { key: 'pft', label: '植物功能类型', help: '模型分类输入', options: [{ value: 'broadleaf', label: '阔叶' }, { value: 'needleleaf', label: '针叶' }] },
];

function toPayload(input = form) {
  return {
    treeHeight: Number(input.treeHeight),
    dbh: Number(input.dbh),
    canopy: Number(input.canopy),
    latitude: Number(input.latitude),
    longitude: Number(input.longitude),
    mat: Number(input.mat),
    map: Number(input.map),
    age: Number(input.age),
    'c.d': Number(input.cd),
    vegetation: input.vegetation,
    growingcondition: input.growingcondition,
    pft: input.pft,
  };
}

function validateCore() {
  for (const key of ['treeHeight', 'dbh', 'canopy']) {
    if (!Number(form[key]) || Number(form[key]) <= 0) return '平均树高、胸径和冠幅为必填，且必须大于 0';
  }
  return '';
}

async function loadHealth() {
  health.loading = true;
  health.message = '';
  try {
    const data = await checkModelHealth();
    health.ok = Boolean(data?.ok);
    health.message = data?.message || '';
  } catch (error) {
    health.ok = false;
    health.message = error.message || '模型服务连接失败';
  } finally {
    health.loading = false;
  }
}

async function submitPredict() {
  formError.value = validateCore();
  if (formError.value) return;
  if (!health.ok) {
    formError.value = '模型服务当前不可用，参数已保留，请连接恢复后再提交估算';
    return;
  }
  isPredicting.value = true;
  formError.value = '';
  try {
    const input = { ...form };
    const data = await predictCarbon(toPayload(input));
    const prediction = Number(data?.prediction ?? data?.result?.prediction ?? data);
    const time = new Date().toLocaleString('zh-CN', { hour12: false });
    result.value = { prediction, input, time };
    const row = { time, ...input, prediction };
    history.value = [row, ...history.value].slice(0, 50);
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value));
    generateAiExplanation();
  } catch (error) {
    formError.value = error.message || '模型估算失败';
  } finally {
    isPredicting.value = false;
  }
}

function resetForm() {
  Object.assign(form, defaults);
  formError.value = '';
}

function exportHistory() {
  const headers = ['时间','树高','胸径','冠幅','纬度','经度','年均温','年降水量','林龄','木材密度','植被类型','生长条件','植物功能类型','预测生物量'];
  const rows = history.value.map((item) => [item.time,item.treeHeight,item.dbh,item.canopy,item.latitude,item.longitude,item.mat,item.map,item.age,item.cd,item.vegetation,item.growingcondition,item.pft,item.prediction]);
  downloadCsv('structure-prediction-history.csv', [headers, ...rows]);
}

function downloadCsv(filename, rows) {
  const csv = rows.map((row) => row.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\n');
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

async function generateAiExplanation() {
  if (!result.value) return;
  ai.loading = true;
  ai.content = '';
  ai.error = '';
  ai.status = '正在连接智能解释服务';
  const input = result.value.input;
  const prompt = `请根据以下结构参数和模型输出，生成简洁、专业的生态学解释。\n\n结构参数：\n平均树高：${input.treeHeight} m\n胸径：${input.dbh} cm\n冠幅：${input.canopy} m\n纬度：${input.latitude}\n经度：${input.longitude}\n年均温：${input.mat} ℃\n年降水量：${input.map} mm\n林龄：${input.age} 年\n木材密度：${input.cd} g/cm³\n植被类型：${input.vegetation}\n生长条件：${input.growingcondition}\n植物功能类型：${input.pft}\n\nCatBoost 模型输出：\n单位面积生物量估算值：${result.value.prediction} t/ha\n\n请说明：1. 生态学含义 2. 主要影响参数 3. 适用范围 4. 需要补充的输入数据 5. 不得将结果表述为正式核证结论`;
  try {
    await streamAi(prompt, (delta) => { ai.content = stripThinkBlocks(ai.content + delta); });
  } catch (error) {
    ai.error = error?.message || '智能解释生成失败，请稍后重新生成；模型估算结果已保留。';
  } finally {
    ai.loading = false;
    ai.status = '';
  }
}

async function streamAi(prompt, onDelta) {
  closeAiStream();
  return new Promise((resolve, reject) => {
    const sessionId = getAiSessionId();
    const url = `/api/chat/stream-carbon?message=${encodeURIComponent(prompt)}&sessionId=${encodeURIComponent(sessionId)}`;
    const source = new EventSource(url);
    aiEventSource = source;
    let settled = false;
    let receivedContent = false;

    const finish = (error) => {
      if (settled) return;
      settled = true;
      source.close();
      if (aiEventSource === source) aiEventSource = null;
      if (error) reject(error);
      else if (!receivedContent) reject(new Error('智能解释服务已结束，但没有返回可显示内容，请重新生成。'));
      else resolve();
    };

    source.onopen = () => { ai.status = '正在分析结构参数与模型结果'; };
    source.onmessage = (event) => {
      const payload = parseAiStreamPayload(event.data);
      if (!payload) return;
      if (payload.error) {
        finish(new Error(payload.error));
        return;
      }
      if (payload.status) ai.status = aiStatusLabel(payload.status);
      if (payload.delta) {
        receivedContent = true;
        onDelta(payload.delta);
        ai.status = '正在生成结构参数解释';
      }
      if (payload.done) finish();
    };
    source.onerror = () => finish(new Error('智能解释连接失败，请稍后重试；模型估算结果已保留。'));
  });
}

function getAiSessionId() {
  const key = 'hnblue_ai_session_id';
  let value = sessionStorage.getItem(key);
  if (!value) {
    value = crypto?.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    sessionStorage.setItem(key, value);
  }
  return value;
}

function aiStatusLabel(status) {
  return ({ connecting: '正在连接智能解释服务', thinking: '正在分析结构参数与模型结果', generating: '正在生成结构参数解释' })[status] || '正在生成结构参数解释';
}

function closeAiStream() {
  aiEventSource?.close();
  aiEventSource = null;
}

onBeforeUnmount(closeAiStream);
onMounted(() => {
  try { history.value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]'); } catch { history.value = []; }
  loadHealth();
});
</script>

<style scoped>
.model-tool { width: var(--hn-page); margin: 0 auto 48px; display: grid; gap: 18px; }
.toolbar-row, .action-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.status-pill { flex: 1; min-width: 260px; display: grid; gap: 4px; padding: 14px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-card); color: var(--hn-text); }
.status-pill span { color: var(--hn-muted); font-size: 13px; }
.status-pill.ok { border-color: rgba(31, 138, 122, 0.32); }
.status-pill.warn { border-color: rgba(217, 155, 53, 0.38); }
.form-stack, .history-panel { display: grid; gap: 14px; padding: 18px; border: 1px solid var(--hn-border); border-radius: var(--hn-radius); background: var(--hn-card); box-shadow: var(--hn-shadow-soft); }
.form-section { display: grid; gap: 12px; padding: 14px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-surface); }
.form-section h3, summary { color: var(--hn-text); font-weight: 900; cursor: pointer; }
.field-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
label { display: grid; gap: 7px; }
label span { color: var(--hn-muted); font-size: 12px; font-weight: 800; }
label b { color: var(--hn-danger); }
input, select { height: 42px; border: 1px solid var(--hn-border-strong); border-radius: 7px; background: var(--hn-control-bg); color: var(--hn-text); padding: 0 12px; }
small { color: var(--hn-muted); line-height: 1.45; }
.inline-error { color: var(--hn-danger); font-weight: 800; }
.history-table { overflow-x: auto; }
table { width: 100%; min-width: 760px; border-collapse: collapse; }
th, td { padding: 11px; border-bottom: 1px solid var(--hn-border); text-align: left; color: var(--hn-text); }
th { background: var(--hn-soft); font-size: 13px; }
@media (max-width: 860px) { .field-grid { grid-template-columns: 1fr; } }
</style>
