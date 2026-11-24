<template>
  <PageShell>
    <UnifiedNav />
    <PageHero title="专业用户与 Agent 权益" eyebrow="Administration" subtitle="账号默认禁用、临时密码只显示一次；启用、重置和权益变更均由 Java 审计" :tags="['最多批量 300 个', '默认禁用', 'TokenVersion 撤销']" />
    <main class="admin-page">
      <section class="toolbar panel">
        <form @submit.prevent="createOne"><input v-model.trim="singleUsername" placeholder="专业用户名" required /><ActionButton label="创建禁用账号" type="submit" /></form>
        <form @submit.prevent="createBatch"><input v-model.trim="batch.prefix" placeholder="批次前缀" required /><input v-model.number="batch.count" type="number" min="1" max="300" /><ActionButton label="幂等批量创建" type="submit" variant="secondary" /></form>
        <form @submit.prevent="load"><input v-model.trim="keyword" placeholder="搜索专业账号" /><ActionButton label="查询" type="submit" variant="secondary" /></form>
        <p v-if="message" :class="['notice', messageType]" role="status">{{ message }}</p>
      </section>

      <section v-if="credentialRows.length" class="credential panel" aria-live="polite">
        <div><h2>一次性临时凭据</h2><p>仅在当前页面显示。请通过获授权介质交付，关闭后无法恢复明文。</p></div>
        <table><thead><tr><th>账号</th><th>临时密码</th></tr></thead><tbody><tr v-for="item in credentialRows" :key="item.accountId"><td>{{ item.username }}</td><td><code>{{ item.temporaryPassword }}</code></td></tr></tbody></table>
        <ActionButton label="清除页面明文" variant="danger" @click="credentialRows=[]" />
      </section>

      <section class="panel table-wrap">
        <table><thead><tr><th>账号</th><th>状态</th><th>首次改密</th><th>权益/额度</th><th>操作</th></tr></thead><tbody>
          <tr v-if="loading"><td colspan="5">正在加载…</td></tr><tr v-else-if="!accounts.length"><td colspan="5">暂无专业账号</td></tr>
          <tr v-for="account in accounts" :key="account.id">
            <td><strong>{{ account.username }}</strong><small>tokenVersion {{ account.tokenVersion }}</small></td>
            <td><span :class="['pill', account.enabled ? 'on' : 'off']">{{ account.enabled ? '已启用' : '已禁用' }}</span></td>
            <td>{{ account.passwordChangeRequired ? '必须修改' : '已完成' }}</td>
            <td><small>{{ enabledCapabilities(account.entitlement) }}</small><small>{{ account.entitlement.dailyRequestLimit }} 次/日 · {{ account.entitlement.dailyTokenLimit }} tokens</small></td>
            <td class="ops"><ActionButton size="sm" :label="account.enabled ? '禁用' : '启用'" :variant="account.enabled ? 'danger' : 'secondary'" @click="toggle(account)" /><ActionButton size="sm" label="重置密码" variant="secondary" @click="resetPassword(account)" /><ActionButton size="sm" label="编辑权益" @click="edit(account)" /></td>
          </tr>
        </tbody></table>
      </section>
    </main>

    <Teleport to="body"><div v-if="editing" class="modal-layer" @click.self="editing=null"><form class="editor" @submit.prevent="saveEntitlement"><button type="button" class="close" @click="editing=null">关闭</button><h2>{{ editing.username }} 的权益</h2>
      <label class="check"><input v-model="entitlement.basicQaEnabled" type="checkbox" />基础问答</label><label class="check"><input v-model="entitlement.professionalAnalysisEnabled" type="checkbox" />专业分析</label><label class="check"><input v-model="entitlement.multiAgentEnabled" type="checkbox" />两 Worker 协作</label>
      <label><span>日请求上限</span><input v-model.number="entitlement.dailyRequestLimit" type="number" min="1" max="100000" /></label><label><span>日 Token 上限</span><input v-model.number="entitlement.dailyTokenLimit" type="number" min="1" max="100000000" /></label><label><span>单次 Token 上限</span><input v-model.number="entitlement.maxTokensPerRequest" type="number" min="128" max="200000" /></label><label><span>并发任务上限</span><input v-model.number="entitlement.maxConcurrentTasks" type="number" min="1" max="32" /></label><label><span>队列优先级</span><input v-model.number="entitlement.queuePriority" type="number" min="0" max="100" /></label><ActionButton label="保存权益" type="submit" />
    </form></div></Teleport>
  </PageShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import PageShell from '@/components/common/PageShell.vue'; import UnifiedNav from '@/components/common/UnifiedNav.vue'; import PageHero from '@/components/common/PageHero.vue'; import ActionButton from '@/components/common/ActionButton.vue';
import { createProfessional, createProfessionalBatch, listProfessionals, resetProfessionalPassword, setProfessionalEnabled, updateProfessionalEntitlement } from '@/services/professionalService';

const accounts=ref([]),credentialRows=ref([]),loading=ref(false),keyword=ref(''),singleUsername=ref(''),message=ref(''),messageType=ref('info'),editing=ref(null); const batch=reactive({prefix:'researcher',count:3});
const entitlement=reactive({basicQaEnabled:true,professionalAnalysisEnabled:true,multiAgentEnabled:false,dailyRequestLimit:100,dailyTokenLimit:200000,maxTokensPerRequest:8000,maxConcurrentTasks:2,queuePriority:10});
function notify(text,type='info'){message.value=text;messageType.value=type} function pageRows(data){return data?.content||data?.data?.content||[]}
async function load(){loading.value=true;try{accounts.value=pageRows(await listProfessionals({keyword:keyword.value,size:100}))}catch(e){notify(e.message,'error')}finally{loading.value=false}}
async function createOne(){try{credentialRows.value=[await createProfessional(singleUsername.value)];singleUsername.value='';notify('已创建禁用账号；临时密码仅显示一次。','success');await load()}catch(e){notify(e.message,'error')}}
async function createBatch(){try{const result=await createProfessionalBatch(batch.prefix,Number(batch.count));credentialRows.value=result.created||[];notify(`新增 ${credentialRows.value.length} 个，跳过 ${result.skipped?.length||0} 个已存在账号。`,'success');await load()}catch(e){notify(e.message,'error')}}
async function toggle(account){try{await setProfessionalEnabled(account.id,!account.enabled);notify('账号状态已更新，旧令牌随 tokenVersion 失效。','success');await load()}catch(e){notify(e.message,'error')}}
async function resetPassword(account){if(!confirm(`重置 ${account.username} 的密码并撤销旧令牌吗？`))return;try{credentialRows.value=[await resetProfessionalPassword(account.id)];notify('密码已重置；临时密码仅显示一次。','success');await load()}catch(e){notify(e.message,'error')}}
function edit(account){editing.value=account;Object.assign(entitlement,account.entitlement)}
async function saveEntitlement(){try{await updateProfessionalEntitlement(editing.value.id,{...entitlement});editing.value=null;notify('Agent 权益已更新。','success');await load()}catch(e){notify(e.message,'error')}}
function enabledCapabilities(e={}){return [['基础问答',e.basicQaEnabled],['专业分析',e.professionalAnalysisEnabled],['两 Worker',e.multiAgentEnabled]].filter(([,on])=>on).map(([name])=>name).join('、')||'无'}
onMounted(load);
</script>

<style scoped>
.admin-page{width:var(--hn-page);margin:0 auto 60px;display:grid;gap:16px}.panel{padding:18px;border:1px solid var(--hn-border);border-radius:var(--hn-radius);background:var(--hn-card);box-shadow:var(--hn-shadow)}.toolbar{display:grid;gap:12px}.toolbar form{display:flex;gap:8px;flex-wrap:wrap}.toolbar input,.editor input{height:40px;padding:0 10px;border:1px solid var(--hn-border-strong);border-radius:7px;background:var(--hn-control-bg);color:var(--hn-text)}.notice{margin:0;padding:10px;border-radius:7px;background:var(--hn-soft)}.notice.error{color:var(--hn-danger)}.notice.success{color:var(--hn-accent)}.credential{display:grid;gap:10px;border-color:rgba(161,58,58,.35)}.credential h2,.credential p{margin:0}.table-wrap{overflow:auto;padding:0}table{width:100%;min-width:920px;border-collapse:collapse}th,td{padding:12px;border-bottom:1px solid var(--hn-border);text-align:left;color:var(--hn-text)}th{background:var(--hn-soft)}td small{display:block;color:var(--hn-muted);margin-top:4px}.pill{padding:4px 8px;border-radius:999px;font-size:12px;font-weight:900}.pill.on{background:rgba(31,138,122,.12);color:var(--hn-accent)}.pill.off{background:rgba(161,58,58,.1);color:var(--hn-danger)}.ops{display:flex;gap:6px;flex-wrap:wrap}.modal-layer{position:fixed;inset:0;z-index:1000;display:flex;justify-content:flex-end;background:rgba(4,28,24,.32)}.editor{width:min(520px,94vw);height:100vh;overflow:auto;display:grid;align-content:start;gap:12px;padding:24px;background:var(--hn-card)}.editor label{display:grid;gap:5px;color:var(--hn-muted);font-weight:800}.editor .check{grid-template-columns:auto 1fr;align-items:center}.close{justify-self:end;height:34px;border:1px solid var(--hn-border);border-radius:7px;background:var(--hn-card);color:var(--hn-accent)}
</style>
