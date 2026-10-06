<template>
  <main ref="shellRef" :class="['page-shell', tone]">
    <slot />
    <PlatformFooter v-if="showFooter" />
  </main>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import PlatformFooter from './PlatformFooter.vue'

const props = defineProps({
  tone: { type: String, default: "light" },
  showFooter: { type: Boolean, default: true },
  scrollReveal: { type: Boolean, default: true },
});

const shellRef = ref(null)
let revealObserver = null
let contentObserver = null
let registerFrame = 0

const revealSelector = [
  '.page-hero',
  ':scope > section',
  ':scope > .gov-page > section',
  '.process-grid > article',
  '.stat-row > article',
  '.entry-grid > a',
  '.module-grid > a',
  '.card-grid > article',
  '.dataset-grid > article',
  '.source-grid > article',
  '.uav-layout > article',
  '.position-grid > article',
  '.fact-grid > article',
  '.quick-grid > a',
  '.decision-grid > article',
  '.asset-grid > article',
  '.region-list > button',
  '.workorder-list > article',
  '.account-page > *',
  '.admin-page > *',
].join(',')

function registerRevealTargets() {
  registerFrame = 0
  const shell = shellRef.value
  if (!shell || !revealObserver) return

  shell.querySelectorAll(revealSelector).forEach((element, index) => {
    if (element.dataset.hnRevealBound === '1' || element.closest('[role="dialog"]')) return
    element.dataset.hnRevealBound = '1'
    element.style.setProperty('--hn-reveal-delay', `${Math.min(index % 4, 3) * 60}ms`)
    element.classList.add('hn-scroll-reveal')
    revealObserver.observe(element)
  })
}

function scheduleRegister() {
  if (!registerFrame) registerFrame = requestAnimationFrame(registerRevealTargets)
}

onMounted(async () => {
  if (!props.scrollReveal || window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return

  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return
      entry.target.classList.add('is-visible')
      revealObserver?.unobserve(entry.target)
    })
  }, {
    threshold: 0.1,
    rootMargin: '0px 0px -7% 0px',
  })

  await nextTick()
  registerRevealTargets()
  contentObserver = new MutationObserver(scheduleRegister)
  contentObserver.observe(shellRef.value, { childList: true, subtree: true })
})

onBeforeUnmount(() => {
  if (registerFrame) cancelAnimationFrame(registerFrame)
  revealObserver?.disconnect()
  contentObserver?.disconnect()
})
</script>

<style scoped>
.page-shell {
  min-height: 100vh;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 82% 12%, rgba(84, 201, 179, 0.14), transparent 28%),
    linear-gradient(180deg, var(--hn-bg-soft), var(--hn-bg));
  color: var(--hn-text);
  font-family: Arial, "Microsoft YaHei", sans-serif;
}

.page-shell.deep {
  background:
    radial-gradient(circle at 80% 10%, rgba(84, 201, 179, 0.16), transparent 28%),
    linear-gradient(180deg, var(--hn-bg-soft), var(--hn-bg));
}

.page-shell :deep(.hn-scroll-reveal) {
  opacity: 0;
  filter: blur(3px);
  transform: translateY(24px);
  transition:
    opacity 0.72s cubic-bezier(0.22, 1, 0.36, 1) var(--hn-reveal-delay, 0ms),
    transform 0.72s cubic-bezier(0.22, 1, 0.36, 1) var(--hn-reveal-delay, 0ms),
    filter 0.58s ease var(--hn-reveal-delay, 0ms);
  will-change: opacity, transform, filter;
}

.page-shell :deep(.hn-scroll-reveal.is-visible) {
  opacity: 1;
  filter: blur(0);
  transform: translateY(0);
}

:global(:root[data-hn-theme="dark"] .page-shell),
:global(:root.theme-dark .page-shell),
:global(:root[data-hn-theme="dark"] .page-shell.deep),
:global(:root.theme-dark .page-shell.deep) {
  background:
    radial-gradient(circle at 82% 10%, rgba(47, 74, 69, 0.2), transparent 30%),
    linear-gradient(180deg, var(--hn-bg-soft), var(--hn-bg) 72%);
}

@media (prefers-reduced-motion: reduce) {
  .page-shell :deep(.hn-scroll-reveal) {
    opacity: 1;
    filter: none;
    transform: none;
    transition: none;
  }
}
</style>
