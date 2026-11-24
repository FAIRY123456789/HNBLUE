<template>
  <PageShell>
    <UnifiedNav />
    <PageHero title="蓝碳 Agent 工作台" eyebrow="Bounded agent task" subtitle="所有任务经 Java 权限、额度和可靠队列接纳；结果显示真实状态、来源与不足信息" :tags="[roleLabel, '任务可取消', '来源可追溯']" />
    <main class="workspace">
      <form class="panel task-form" @submit.prevent="submit">
        <h2>创建任务</h2>
        <label><span>能力</span><select v-model="form.capability"><option v-for="item in capabilities" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
        <div class="two"><label><span>Token 预算</span><input v-model.number="form.tokenBudget" type="number" min="128" max="200000" /></label><label><span>超时（秒）</span><input v-model.number="form.timeoutSeconds" type="number" min="5" max="600" /></label></div>
        <label><span>业务问题</span><textarea v-model.trim="form.message" maxlength="8000" required placeholder="例如：联合分析文昌红树林监测记录与适用方法学，并列出来源、单位和数据缺口。" /></label>
        <p v-if="feedback" :class="['feedback', feedbackType]" role="status">{{ feedback }}</p>
        <ActionButton label="提交到可靠任务队列" type="submit" :disabled="busy" />
      </form>

      <section class="panel status-panel">
        <div class="status-head"><div><p class="eyebrow">Task status</p><h2>{{ task?.status || '尚未提交' }}</h2></div><ActionButton v-if="canCancel" label="取消任务" variant="danger" size="sm" @click="cancel" /></div>
        <dl v-if="task"><div><dt>taskId</dt><dd>{{ task.taskId }}</dd></div><div><dt>traceId</dt><dd>{{ task.traceId }}</dd></div><div><dt>尝试次数</dt><dd>{{ task.attempts }}</dd></div><div><dt>Token</dt><dd>{{ task.actualTokens ?? '-' }} / {{ task.requestedTokens }}</dd></div><div v-if="task.errorCode"><dt>错误</dt><dd>{{ task.errorCode }} · {{ task.errorMessage }}</dd></div></dl>
        <article v-if="result" class="result"><h3>回答</h3><pre>{{ result.output || result }}</pre><h3>来源</h3><ul v-if="result.sources?.length"><li v-for="source in result.sources" :key="source.sourceId"><strong>{{ source.title || source.sourceId }}</strong><span>{{ source.datasetIdentity }} · {{ source.synthetic ? '合成' : '非合成' }}</span></li></ul><p v-else>当前结果没有可展示来源。</p></article>
      </section>
    </main>
  </PageShell>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue';
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import ActionButton from '@/components/common/ActionButton.vue';
import { useAuth } from '@/composables/useAuth';
import { cancelAgentTask, getAgentTask, submitAgentTask } from '@/services/agentTaskService';

const auth = useAuth();
const form = reactive({ capability: 'BASIC_QA', tokenBudget: 1200, timeoutSeconds: 90, message: '' });
const task = ref(null); const busy = ref(false); const feedback = ref(''); const feedbackType = ref('info'); let timer = null;
const roleLabel = computed(() => auth.role.value === 'Professional' ? '专业用户' : auth.role.value === 'Admin' ? '管理员' : '公众用户');
const capabilities = computed(() => auth.role.value === 'Professional' || auth.role.value === 'Admin'
  ? [{ value: 'BASIC_QA', label: '基础知识问答' }, { value: 'PROFESSIONAL_ANALYSIS', label: '专业分析' }, { value: 'MULTI_AGENT', label: '两 Worker 协作' }]
  : [{ value: 'BASIC_QA', label: '公众科普问答' }]);
const canCancel = computed(() => ['CREATED', 'QUEUED', 'RUNNING'].includes(task.value?.status));
const result = computed(() => { if (!task.value?.resultJson) return null; try { return JSON.parse(task.value.resultJson); } catch { return task.value.resultJson; } });

function schedulePoll() { clearTimeout(timer); if (canCancel.value) timer = setTimeout(refresh, 900); }
async function refresh() { if (!task.value?.taskId) return; try { task.value = await getAgentTask(task.value.taskId); schedulePoll(); } catch (error) { feedback.value = error.message; feedbackType.value = 'error'; } }
async function submit() {
  busy.value = true; feedback.value = '正在提交任务…'; feedbackType.value = 'info';
  try {
    const key = `web-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
    task.value = await submitAgentTask({ ...form }, key);
    feedback.value = '任务已接纳。'; feedbackType.value = 'success'; schedulePoll();
  } catch (error) { feedback.value = error.message || '任务提交失败。'; feedbackType.value = 'error'; }
  finally { busy.value = false; }
}
async function cancel() { try { task.value = await cancelAgentTask(task.value.taskId); feedback.value = '取消请求已处理。'; schedulePoll(); } catch (error) { feedback.value = error.message; feedbackType.value = 'error'; } }
onBeforeUnmount(() => clearTimeout(timer));
</script>

<style scoped>
.workspace { width: var(--hn-page); margin: 0 auto 60px; display: grid; grid-template-columns: minmax(320px,.85fr) minmax(360px,1.15fr); gap: 18px; }.panel { padding: 22px; border: 1px solid var(--hn-border); border-radius: var(--hn-radius); background: var(--hn-card); box-shadow: var(--hn-shadow); }.task-form { display: grid; gap: 14px; align-content: start; }.task-form h2,.status-panel h2 { margin: 0; color: var(--hn-text); }label { display:grid; gap:6px; color:var(--hn-muted); font-weight:800; }input,select,textarea { width:100%; border:1px solid var(--hn-border-strong); border-radius:7px; background:var(--hn-control-bg); color:var(--hn-text); padding:10px 12px; font:inherit; }textarea{min-height:180px;resize:vertical}.two{display:grid;grid-template-columns:1fr 1fr;gap:10px}.feedback{padding:10px;border-radius:7px;background:var(--hn-soft)}.feedback.error{color:var(--hn-danger)}.feedback.success{color:var(--hn-accent)}.status-head{display:flex;justify-content:space-between;gap:12px}.eyebrow{margin:0 0 4px;color:var(--hn-accent);font-size:12px;font-weight:900;text-transform:uppercase}dl{display:grid;gap:8px}dl div{padding-bottom:8px;border-bottom:1px solid var(--hn-border)}dt{color:var(--hn-muted);font-size:12px}dd{margin:3px 0 0;color:var(--hn-text);word-break:break-all}.result pre{white-space:pre-wrap;word-break:break-word;color:var(--hn-text);font:inherit;line-height:1.65}.result li{display:grid;gap:3px;margin:8px 0;color:var(--hn-text)}.result li span{color:var(--hn-muted);font-size:12px}@media(max-width:900px){.workspace{grid-template-columns:1fr}.two{grid-template-columns:1fr}}
</style>
