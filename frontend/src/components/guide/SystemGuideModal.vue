<template>
  <Teleport to="body">
    <div v-if="open" class="guide-overlay" @click="close">
      <section
        ref="modalRef"
        class="guide-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="system-guide-title"
        aria-label="系统使用手册"
        tabindex="-1"
        @click.stop
      >
        <header class="guide-header">
          <div>
            <p>系统使用手册</p>
            <h2 id="system-guide-title">海南蓝碳系统生态碳汇数据库及蓝碳评估分析与数字化应用平台</h2>
          </div>
          <button ref="closeRef" type="button" class="close-button" aria-label="关闭系统使用手册" @click="close">×</button>
        </header>

        <div class="guide-search-row">
          <label class="guide-search">
            <span>搜索手册内容</span>
            <input v-model.trim="keyword" type="search" placeholder="输入页面、操作、数据或权限关键词" />
          </label>
          <p v-if="permissionMessage" class="permission-message" role="status">{{ permissionMessage }}</p>
        </div>

        <div class="guide-layout">
          <SystemGuideSidebar :sections="filteredSections" :active-id="activeId" @select="scrollTo" />
          <main ref="contentRef" class="guide-content" @scroll="handleContentScroll">
            <p v-if="!filteredSections.length" class="empty-result">没有找到匹配内容，请更换关键词。</p>
            <SystemGuideSection
              v-for="section in filteredSections"
              :key="section.id"
              :ref="(el) => setSectionRef(section.id, el)"
              :section="section"
              @navigate="goToSectionRoute"
            />
          </main>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { guideSections } from '@/content/systemGuide';
import { useAuth } from '@/composables/useAuth';
import SystemGuideSidebar from '@/components/guide/SystemGuideSidebar.vue';
import SystemGuideSection from '@/components/guide/SystemGuideSection.vue';

const props = defineProps({
  open: { type: Boolean, default: false },
});
const emit = defineEmits(['close']);

const router = useRouter();
const auth = useAuth();
const keyword = ref('');
const activeId = ref(guideSections[0]?.id || '');
const permissionMessage = ref('');
const modalRef = ref(null);
const closeRef = ref(null);
const contentRef = ref(null);
const sectionRefs = new Map();
let previousBodyOverflow = '';
let previousHtmlOverflow = '';

const filteredSections = computed(() => {
  const query = keyword.value.toLowerCase();
  if (!query) return guideSections;
  return guideSections.filter((section) => stringifySection(section).toLowerCase().includes(query));
});

function stringifySection(section) {
  return JSON.stringify(section, (key, value) => (typeof value === 'function' ? undefined : value));
}

function close() {
  emit('close');
}

function lockScroll() {
  previousBodyOverflow = document.body.style.overflow;
  previousHtmlOverflow = document.documentElement.style.overflow;
  document.body.style.overflow = 'hidden';
  document.documentElement.style.overflow = 'hidden';
}

function unlockScroll() {
  document.body.style.overflow = previousBodyOverflow;
  document.documentElement.style.overflow = previousHtmlOverflow;
}

function handleKeydown(event) {
  if (event.key === 'Escape') close();
}

function setSectionRef(id, el) {
  if (!el) {
    sectionRefs.delete(id);
    return;
  }
  sectionRefs.set(id, el.$el || el);
}

function scrollTo(id) {
  permissionMessage.value = '';
  const target = sectionRefs.get(id);
  if (!target) return;
  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  activeId.value = id;
}

function handleContentScroll() {
  const content = contentRef.value;
  if (!content) return;
  const contentTop = content.getBoundingClientRect().top;
  let current = activeId.value;
  for (const section of filteredSections.value) {
    const el = sectionRefs.get(section.id);
    if (!el) continue;
    if (el.getBoundingClientRect().top - contentTop <= 84) current = section.id;
  }
  activeId.value = current;
}

function goToSectionRoute(section) {
  permissionMessage.value = '';
  if (!section.route) return;
  const resolved = router.resolve(section.route);
  const record = resolved.matched[resolved.matched.length - 1];
  const meta = record?.meta || {};
  if (meta.requiresAuth && !auth.isAuthenticated.value) {
    permissionMessage.value = '该页面需要登录后访问，请先登录，再从导航或手册入口进入。';
    return;
  }
  if (meta.roles?.length && !auth.hasRole(meta.roles)) {
    permissionMessage.value = '当前账号没有该页面权限，请使用具备相应角色的账号登录。';
    return;
  }
  emit('close');
  nextTick(() => router.push(section.route));
}

watch(() => props.open, async (isOpen) => {
  if (!isOpen) {
    unlockScroll();
    window.removeEventListener('keydown', handleKeydown);
    permissionMessage.value = '';
    return;
  }
  lockScroll();
  window.addEventListener('keydown', handleKeydown);
  await nextTick();
  activeId.value = filteredSections.value[0]?.id || '';
  closeRef.value?.focus();
});

watch(filteredSections, async (sections) => {
  await nextTick();
  activeId.value = sections[0]?.id || '';
  contentRef.value?.scrollTo({ top: 0 });
});

onBeforeUnmount(() => {
  unlockScroll();
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<style scoped>
.guide-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 3vh 3vw;
  background: rgba(4, 26, 24, 0.52);
  backdrop-filter: blur(8px);
}

.guide-modal {
  width: min(92vw, 1480px);
  height: min(90vh, 900px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 14px;
  padding: 20px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 8px;
  background: var(--hn-bg);
  box-shadow: 0 34px 100px rgba(3, 32, 29, 0.36);
  color: var(--hn-text);
  overflow: hidden;
}

.guide-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--hn-border);
}

.guide-header p {
  margin: 0 0 6px;
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}

.guide-header h2 {
  margin: 0;
  max-width: 1040px;
  color: var(--hn-text);
  font-size: clamp(22px, 2vw, 34px);
  line-height: 1.26;
}

.close-button {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border: 1px solid var(--hn-border-strong);
  border-radius: 8px;
  background: var(--hn-panel-solid);
  color: var(--hn-text);
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
}

.guide-search-row {
  display: grid;
  grid-template-columns: minmax(280px, 520px) minmax(0, 1fr);
  align-items: end;
  gap: 12px;
}

.guide-search {
  display: grid;
  gap: 7px;
}

.guide-search span {
  color: var(--hn-muted);
  font-size: 13px;
  font-weight: 900;
}

.guide-search input {
  height: 42px;
  width: 100%;
  border: 1px solid var(--hn-border-strong);
  border-radius: 8px;
  background: var(--hn-control-bg);
  color: var(--hn-text);
  padding: 0 13px;
}

.permission-message,
.empty-result {
  margin: 0;
  padding: 11px 13px;
  border: 1px solid rgba(217, 155, 53, 0.32);
  border-radius: 8px;
  background: rgba(217, 155, 53, 0.12);
  color: var(--hn-text);
  font-weight: 800;
}

.guide-layout {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(230px, 280px) minmax(0, 1fr);
  gap: 18px;
}

.guide-content {
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 14px;
  overflow: auto;
  padding: 0 6px 18px 2px;
  scroll-behavior: smooth;
}

@media (max-width: 860px) {
  .guide-overlay {
    padding: 0;
  }

  .guide-modal {
    width: 100vw;
    height: 100vh;
    border-radius: 0;
    padding: 14px;
  }

  .guide-header {
    align-items: center;
  }

  .guide-search-row,
  .guide-layout {
    grid-template-columns: 1fr;
  }

  .guide-layout {
    grid-template-rows: auto minmax(0, 1fr);
    gap: 10px;
  }
}

:global(:root[data-hn-theme="dark"]) .guide-overlay,
:global(:root.theme-dark) .guide-overlay {
  background: rgba(0, 12, 14, 0.7);
}
</style>
