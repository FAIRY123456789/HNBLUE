<template>
  <Teleport to="body">
    <Transition name="guide-fade">
      <div v-if="open" class="guide-overlay" @click.self="close">
        <section class="guide-modal" role="dialog" aria-modal="true" aria-labelledby="guide-title">
          <header class="guide-head">
            <div>
              <p>System Guide</p>
              <h2 id="guide-title">系统使用手册</h2>
            </div>
            <button type="button" aria-label="关闭系统使用手册" @click="close">×</button>
          </header>
          <div class="guide-layout">
            <nav aria-label="手册章节">
              <button v-for="section in guideSections" :key="section.id" type="button" @click="scrollTo(section.id)">{{ section.title }}</button>
            </nav>
            <article ref="contentRef" class="guide-content">
              <section v-for="section in guideSections" :key="section.id" :id="section.id">
                <h3>{{ section.title }}</h3>
                <p v-for="line in section.body" :key="line">{{ line }}</p>
                <div v-if="section.theme" class="theme-previews">
                  <div class="theme-card light"><strong>浅色主题</strong><span>白底、绿色强调、适合白天阅读和投屏。</span></div>
                  <div class="theme-card dark"><strong>深色主题</strong><span>深色底、青绿色强调、适合低亮度环境。</span></div>
                </div>
              </section>
            </article>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue';
import { guideSections } from '@/content/systemGuide';

const props = defineProps({ open: { type: Boolean, default: false } });
const emit = defineEmits(['close']);
const contentRef = ref(null);
let previousOverflow = '';
function close() { emit('close'); }
function onKey(event) { if (event.key === 'Escape') close(); }
function scrollTo(id) { contentRef.value?.querySelector(`#${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }
watch(() => props.open, (value) => {
  if (value) {
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKey);
  } else {
    document.body.style.overflow = previousOverflow;
    window.removeEventListener('keydown', onKey);
  }
});
onBeforeUnmount(() => {
  document.body.style.overflow = previousOverflow;
  window.removeEventListener('keydown', onKey);
});
</script>

<style scoped>
.guide-overlay { position: fixed; inset: 0; z-index: 1200; display: grid; place-items: center; padding: 4vh 4vw; background: rgba(3, 24, 22, 0.48); }
.guide-modal { width: min(90vw, 1180px); height: min(86vh, 820px); display: grid; grid-template-rows: auto minmax(0, 1fr); border: 1px solid var(--hn-border-strong); border-radius: 8px; background: var(--hn-card); color: var(--hn-text); box-shadow: 0 30px 90px rgba(0, 0, 0, 0.26); }
.guide-head { display: flex; justify-content: space-between; gap: 16px; align-items: center; padding: 18px 20px; border-bottom: 1px solid var(--hn-border); }
.guide-head p { margin: 0 0 4px; color: var(--hn-accent); font-size: 12px; font-weight: 900; text-transform: uppercase; }
.guide-head h2 { margin: 0; font-size: 24px; }
.guide-head button { width: 36px; height: 36px; border: 1px solid var(--hn-border); border-radius: 7px; background: var(--hn-control-bg); color: var(--hn-text); cursor: pointer; font-size: 24px; }
.guide-layout { min-height: 0; display: grid; grid-template-columns: 220px minmax(0, 1fr); }
.guide-layout nav { overflow: auto; padding: 14px; border-right: 1px solid var(--hn-border); background: var(--hn-soft); }
.guide-layout nav button { width: 100%; min-height: 38px; margin-bottom: 6px; border: 1px solid transparent; border-radius: 7px; background: transparent; color: var(--hn-muted); cursor: pointer; text-align: left; font-weight: 800; }
.guide-layout nav button:hover { border-color: var(--hn-border); background: var(--hn-card); color: var(--hn-accent); }
.guide-content { overflow: auto; padding: 20px 24px 34px; scroll-behavior: smooth; }
.guide-content section { padding: 4px 0 22px; border-bottom: 1px solid var(--hn-border); }
.guide-content h3 { margin: 0 0 10px; color: var(--hn-text); font-size: 22px; }
.guide-content p { margin: 8px 0; color: var(--hn-muted); line-height: 1.75; }
.theme-previews { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 14px; }
.theme-card { min-height: 110px; display: grid; align-content: end; gap: 6px; padding: 16px; border-radius: 8px; border: 1px solid var(--hn-border); }
.theme-card.light { background: #f6fbf8; color: #123b35; }
.theme-card.dark { background: #10272b; color: #d9f4ef; }
.theme-card span { color: inherit; opacity: 0.78; line-height: 1.5; }
.guide-fade-enter-active, .guide-fade-leave-active { transition: opacity 0.18s ease; }
.guide-fade-enter-from, .guide-fade-leave-to { opacity: 0; }
@media (max-width: 760px) { .guide-modal { width: 94vw; height: 88vh; } .guide-layout { grid-template-columns: 1fr; } .guide-layout nav { display: flex; gap: 8px; overflow-x: auto; border-right: 0; border-bottom: 1px solid var(--hn-border); } .guide-layout nav button { min-width: 136px; } .theme-previews { grid-template-columns: 1fr; } }
</style>
