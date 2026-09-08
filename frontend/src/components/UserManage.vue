<template>
  <PageShell>
    <UnifiedNav />
    <PageHero title="用户管理" eyebrow="Administration" subtitle="面向管理员的用户列表、搜索、详情、编辑与操作记录查看" :tags="['每页 10 条', '权限校验', '操作日志']" />
    <section class="admin-page">
      <ActionToast v-model="toast.visible" :type="toast.type" :message="toast.message" />
      <SearchPanel v-model="filters" :fields="searchFields" @search="search" @reset="resetSearch" />
      <div class="action-row">
        <ActionButton label="新增用户" @click="openEditor()" />
        <ActionButton label="批量删除" variant="danger" :disabled="!selectedIds.length" @click="removeSelected" />
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th><th>姓名</th><th>邮箱</th><th>生日</th><th>最近登录</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-if="isLoading"><td colspan="6">正在加载用户...</td></tr>
            <tr v-else-if="!users.length"><td colspan="6">暂无可用用户</td></tr>
            <tr v-for="user in users" :key="user.id">
              <td><input v-model="selectedIds" type="checkbox" :value="user.id" /></td>
              <td>{{ user.name }}</td><td>{{ user.email || '-' }}</td><td>{{ user.birthdate || '-' }}</td><td>{{ user.lastLoginAt || '暂无记录' }}</td>
              <td class="ops"><ActionButton size="sm" label="详情" variant="secondary" @click="openDetail(user)" /><ActionButton size="sm" label="编辑" variant="secondary" @click="openEditor(user)" /><ActionButton size="sm" label="删除" variant="danger" @click="removeOne(user)" /></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar :offset="page * 10" :page-size="10" :total="total" @change="setPage" />
    </section>

    <Teleport to="body"><div v-if="editing" class="modal-layer" @click.self="closeEditor"><form class="editor" @submit.prevent="saveUser"><button type="button" class="close" @click="closeEditor">关闭</button><h2>{{ editor.id ? '编辑用户' : '新增用户' }}</h2><label><span>姓名</span><input v-model="editor.name" required /></label><label><span>邮箱</span><input v-model="editor.email" type="email" required /></label><label><span>生日</span><input v-model="editor.birthdate" type="date" required /></label><label><span>密码</span><input v-model="editor.password" type="password" :required="!editor.id" placeholder="编辑时留空则不修改" /></label><ActionButton label="保存" type="submit" /></form></div></Teleport>

    <Teleport to="body"><div v-if="detailOpen" class="modal-layer" @click.self="detailOpen=false"><aside class="drawer"><button type="button" class="close" @click="detailOpen=false">关闭</button><p class="eyebrow">用户详情</p><h2>{{ detail.user?.name || '用户详情' }}</h2><dl><div><dt>邮箱</dt><dd>{{ detail.user?.email || '-' }}</dd></div><div><dt>生日</dt><dd>{{ detail.user?.birthdate || '-' }}</dd></div><div><dt>账户角色</dt><dd>普通用户</dd></div><div><dt>最近登录</dt><dd>{{ detail.user?.lastLoginAt || '暂无记录' }}</dd></div></dl><h3>最近操作记录</h3><ul v-if="detail.logs?.length"><li v-for="log in detail.logs" :key="log.id">{{ log.occurredAt }} · {{ log.actionDescription }}</li></ul><p v-else class="muted">暂无可用操作记录</p></aside></div></Teleport>
  </PageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import SearchPanel from '@/components/common/SearchPanel.vue';
import PaginationBar from '@/components/common/PaginationBar.vue';
import ActionButton from '@/components/common/ActionButton.vue';
import ActionToast from '@/components/common/ActionToast.vue';
import { addUser, deleteUser, deleteUsers, getUserDetail, listUsers, updateUser } from '@/services/adminService';

const router = useRouter();
const users = ref([]); const total = ref(0); const page = ref(0); const selectedIds = ref([]); const isLoading = ref(false);
const filters = ref({ keyword: '' });
const searchFields = [{ key: 'keyword', label: '姓名/账号/邮箱', placeholder: '输入姓名、账号或邮箱关键词' }];
const toast = reactive({ visible: false, type: 'info', message: '' });
const editing = ref(false); const editor = reactive({ id: null, name: '', email: '', birthdate: '', password: '' });
const detailOpen = ref(false); const detail = reactive({ user: null, logs: [] });
const PASSWORD_RULE_MESSAGE = '???? 8 ????????????????';
function isStrongPassword(value) { return typeof value === 'string' && value.length >= 8 && /[A-Za-z]/.test(value) && /\d/.test(value) && !/\s/.test(value); }
const allChecked = computed(() => users.value.length && selectedIds.value.length === users.value.length);
function show(message, type = 'info') { toast.message = message; toast.type = type; toast.visible = true; }
function normalizePage(data = {}) {
  const rows = data.content || data.users || data.records || data.data?.content || data.data?.users || [];
  return { rows: Array.isArray(rows) ? rows : [], total: data.totalElements ?? data.total ?? data.count ?? data.data?.totalElements ?? data.data?.total ?? (Array.isArray(rows) ? rows.length : 0) };
}
async function loadUsers() {
  isLoading.value = true;
  try {
    const data = await listUsers({ page: page.value, size: 10, keyword: filters.value.keyword });
    const normalized = normalizePage(data);
    users.value = normalized.rows;
    total.value = Number(normalized.total || 0);
    selectedIds.value = [];
  } catch (error) {
    users.value = [];
    total.value = 0;
    handleError(error);
  } finally { isLoading.value = false; }
}
function handleError(error) { if (error.status === 401) router.push('/login'); else if (error.status === 403) show(error.message || '管理员权限不足', 'error'); else show(error.message || '操作失败', 'error'); }
function search() { page.value = 0; loadUsers(); }
function resetSearch() { filters.value = { keyword: '' }; page.value = 0; loadUsers(); }
function setPage(offset) { page.value = Math.max(0, Math.floor(Number(offset || 0) / 10)); loadUsers(); }
function toggleAll(event) { selectedIds.value = event.target.checked ? users.value.map((u) => u.id) : []; }
function openEditor(user = null) { Object.assign(editor, user ? { id: user.id, name: user.name, email: user.email, birthdate: user.birthdate, password: '' } : { id: null, name: '', email: '', birthdate: '', password: '' }); editing.value = true; }
function closeEditor() { editing.value = false; }
async function saveUser() {
  try {
    const passwordChanged = Boolean(editor.password);
    if ((!editor.id || passwordChanged) && !isStrongPassword(editor.password)) {
      show(PASSWORD_RULE_MESSAGE, 'error');
      return;
    }
    const payload = { ...editor };
    if (!payload.password) delete payload.password;
    if (editor.id) await updateUser(payload); else await addUser(payload);
    editor.password = '';
    closeEditor();
    show(passwordChanged ? '用户信息和密码已保存，密码输入已清空' : '用户信息已保存', 'success');
    loadUsers();
  } catch (error) { handleError(error); }
}
async function removeOne(user) { if (!confirm(`确认删除用户 ${user.name} 吗？`)) return; try { await deleteUser(user.id); show('用户已删除', 'success'); loadUsers(); } catch (error) { handleError(error); } }
async function removeSelected() { if (!confirm(`确认删除选中的 ${selectedIds.value.length} 个用户吗？`)) return; try { await deleteUsers(selectedIds.value); show('批量删除完成', 'success'); loadUsers(); } catch (error) { handleError(error); } }
async function openDetail(user) { detailOpen.value = true; detail.user = user; detail.logs = []; try { const data = await getUserDetail(user.id); detail.user = data.user || user; detail.logs = data.logs || []; } catch (error) { detailOpen.value = false; handleError(error); } }
onMounted(loadUsers);
</script>

<style scoped>
.admin-page { width: var(--hn-page); margin: 0 auto 48px; display: grid; gap: 14px; }
.action-row { display: flex; gap: 10px; flex-wrap: wrap; }
.table-wrap { overflow-x: auto; border: 1px solid var(--hn-border); border-radius: var(--hn-radius); background: var(--hn-card); }
table { width: 100%; min-width: 920px; border-collapse: collapse; }
th, td { padding: 12px; border-bottom: 1px solid var(--hn-border); color: var(--hn-text); text-align: left; }
th { background: var(--hn-soft); font-size: 13px; }
.ops { display: flex; gap: 8px; }
.modal-layer { position: fixed; inset: 0; z-index: 1000; display: flex; justify-content: flex-end; background: rgba(4, 28, 24, 0.32); }
.editor, .drawer { width: min(560px, 94vw); height: 100vh; overflow: auto; display: grid; align-content: start; gap: 12px; padding: 24px; background: var(--hn-card); box-shadow: -18px 0 46px rgba(4, 28, 24, 0.22); }
.editor label { display: grid; gap: 7px; }
.field-hint { margin: -2px 0 0; color: var(--hn-muted); font-size: 12px; line-height: 1.45; }
.editor input { height: 42px; border: 1px solid var(--hn-border-strong); border-radius: 7px; background: var(--hn-control-bg); color: var(--hn-text); padding: 0 12px; }
.close { justify-self: end; height: 34px; border: 1px solid var(--hn-border); border-radius: 7px; background: var(--hn-card); color: var(--hn-accent); cursor: pointer; }
dl { display: grid; gap: 10px; } dl div { border-bottom: 1px solid var(--hn-border); padding-bottom: 8px; } dt { color: var(--hn-muted); font-size: 12px; } dd { margin: 4px 0 0; color: var(--hn-text); }
.muted { color: var(--hn-muted); }
</style>
