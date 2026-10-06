<template>
  <PageShell :class="['home-page-shell', { 'home-intro': showIntro }]" :show-footer="false" :scroll-reveal="false">
    <div class="home-background" :style="{ '--hero-background': `url(${heroImage})` }" aria-hidden="true"></div>
    <section class="home-hero">
      <UnifiedNav variant="light" />
      <div class="hero-inner">
        <div class="hero-copy">
          <p class="eyebrow">Blue Carbon Platform</p>
          <h1>海南蓝碳评估分析与数字化应用平台</h1>
          <p class="lead">
            汇聚红树林面积、区域指标、文献碳储与来源证据，形成面向政府管理、公众认知、科研评估和第三方查验的统一平台入口
          </p>
          <div class="hero-actions" aria-label="首页快捷入口">
            <router-link class="home-action-button home-action-button--primary" to="/v2-public-data">查看数据资产</router-link>
            <router-link class="home-action-button home-action-button--secondary" to="/v2-government">进入政府端</router-link>
            <SystemGuideButton class="home-action-button home-action-button--guide" @open="guideOpen = true" />
          </div>
        </div>

      </div>
    </section>

    <section class="stat-row">
      <StatCard v-for="item in stats" :key="item.label" :label="item.label" :value="item.value" :unit="item.unit" :note="item.note" />
    </section>

    <section class="entry-grid">
      <router-link v-for="entry in entries" :key="entry.title" :to="entry.to" class="entry-card">
        <h2>{{ entry.title }}</h2>
        <p>{{ entry.desc }}</p>
        <b>{{ entry.action }}</b>
      </router-link>
    </section>

    <SystemGuideModal :open="guideOpen" @close="guideOpen = false" />
  </PageShell>
</template>

<script>
import PageShell from "@/components/common/PageShell.vue";
import UnifiedNav from "@/components/common/UnifiedNav.vue";
import StatCard from "@/components/common/StatCard.vue";
import SystemGuideButton from "@/components/home/SystemGuideButton.vue";
import SystemGuideModal from "@/components/guide/SystemGuideModal.vue";
import heroImage from "@/assets/hero-mangrove.webp";

export default {
  name: "HomePage",
  components: { PageShell, UnifiedNav, StatCard, SystemGuideButton, SystemGuideModal },
  data() {
    return {
      heroImage,
      guideOpen: false,
      showIntro: false,
      stats: [
        { value: "68", unit: "项", label: "可信数据来源", note: "公开遥感、权威资料与同行评审证据" },
        { value: "294", unit: "条", label: "区域观察与评价指标", note: "区域比较、变化识别与治理研判" },
        { value: "199", unit: "项", label: "标准化指标体系", note: "统一名称、单位、口径与来源" },
        { value: 561, unit: "项", label: "多源数据资产", note: "覆盖来源、区域指标与标准指标" },
      ],
      entries: [
        { title: "碳溯", desc: "覆盖查询、估算与模型解释", action: "进入碳溯", to: "/carbonseek" },
        { title: "数据资产", desc: "汇聚来源、证据、指标与面积", action: "浏览数据资产", to: "/v2-public-data" },
        { title: "治理工作台", desc: "支持筛选、追溯、管理与研判", action: "进入工作台", to: "/v2-government" },
        { title: "关于项目", desc: "说明定位、范围与查验边界", action: "查看说明", to: "/about" },
      ],
    };
  },
  mounted() {
    document.documentElement.style.overflow = "auto";
    document.body.style.overflow = "auto";
    try {
      if (sessionStorage.getItem("hnblue_home_intro_seen") !== "1") {
        this.showIntro = true;
        sessionStorage.setItem("hnblue_home_intro_seen", "1");
      }
    } catch (_) {
      this.showIntro = true;
    }
  },
};
</script>

<style scoped>
.home-page-shell {
  position: relative;
  isolation: isolate;
  overflow: hidden;
}

.home-background {
  position: absolute;
  inset: -18px;
  z-index: 0;
  background-image: var(--hero-background);
  background-position: center top;
  background-size: cover;
  filter: blur(7px) brightness(0.94) saturate(0.88);
  opacity: 0.9;
  transform: scale(1.018);
  pointer-events: none;
}

.home-background::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(224, 238, 233, 0.86) 0%, rgba(218, 234, 229, 0.6) 48%, rgba(208, 229, 222, 0.36) 100%);
}

.home-hero {
  position: relative;
  z-index: 1;
  min-height: 400px;
  background: transparent;
}

.hero-inner {
  position: relative;
  width: var(--hn-page);
  margin: 0 auto;
  min-height: 324px;
  padding: 28px 0 36px;
}

.hero-copy { max-width: 820px; }

.eyebrow {
  margin: 0 0 12px;
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  max-width: none;
  color: var(--hn-text);
  font-size: clamp(30px, 3.65vw, 52px);
  line-height: 1.14;
  letter-spacing: 0;
  white-space: nowrap;
}

.lead {
  max-width: 700px;
  margin: 20px 0 0;
  color: var(--hn-muted);
  font-size: 18px;
  line-height: 1.78;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-top: 30px;
}

.home-action-button {
  width: 160px;
  min-width: 160px;
  height: 46px;
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 900;
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
}

.home-action-button--primary {
  border: 1px solid var(--hn-accent);
  background: var(--hn-accent);
  color: #fff;
  box-shadow: 0 12px 24px rgba(13, 107, 87, 0.16);
}

.home-action-button--secondary {
  border: 1px solid var(--hn-border-strong);
  background: var(--hn-panel);
  color: var(--hn-accent);
  box-shadow: var(--hn-shadow-soft);
}

.home-action-button--guide {
  border-color: #8dbdce;
  background: linear-gradient(135deg, #d7edf6 0%, #edf7fb 100%);
  color: #16566b;
  box-shadow: 0 10px 22px rgba(34, 103, 128, 0.14);
}

.home-action-button--guide:hover {
  border-color: #659eb3;
  background: linear-gradient(135deg, #c8e5f0 0%, #e4f3f8 100%);
  box-shadow: 0 14px 28px rgba(34, 103, 128, 0.18);
}

.home-action-button--primary,
.home-action-button--secondary {
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.home-action-button--primary:hover,
.home-action-button--secondary:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(10, 104, 89, 0.16);
}

.home-action-button--primary:active,
.home-action-button--secondary:active {
  transform: translateY(0);
  box-shadow: 0 7px 16px rgba(10, 104, 89, 0.11);
}

.stat-row,
.entry-grid {
  position: relative;
  z-index: 1;
  width: var(--hn-page);
  margin: 22px auto;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  padding-bottom: 54px;
}

.entry-card {
  min-height: 230px;
  display: flex;
  flex-direction: column;
  padding: 22px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
  color: var(--hn-text);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.entry-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--hn-shadow);
}

.entry-card span {
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 900;
}

.entry-card h2 {
  margin: 16px 0 8px;
  font-size: 24px;
}

.entry-card p {
  flex: 1;
  margin: 0 0 18px;
  color: var(--hn-muted);
  line-height: 1.65;
}

.entry-card b {
  color: var(--hn-accent);
}

@media (max-width: 940px) {
  .home-hero { min-height: auto; }

  .hero-inner {
    min-height: auto;
    padding-bottom: 56px;
  }

  h1 {
    white-space: normal;
  }

  .stat-row,
  .entry-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .hero-actions {
    align-items: stretch;
  }

  .home-action-button {
    width: 100%;
    min-width: 0;
  }

  .stat-row,
  .entry-grid {
    grid-template-columns: 1fr;
  }
}

:global(:root[data-hn-theme="dark"] .home-action-button--primary),
:global(:root.theme-dark .home-action-button--primary) {
  color: var(--hn-on-accent);
}

:global(:root[data-hn-theme="dark"] .home-background),
:global(:root.theme-dark .home-background) {
  filter: blur(8px) brightness(0.48) saturate(0.68);
  opacity: 0.92;
}

:global(:root[data-hn-theme="dark"] .home-background::after),
:global(:root.theme-dark .home-background::after) {
  background: linear-gradient(90deg, rgba(2, 5, 6, 0.91) 0%, rgba(2, 5, 6, 0.7) 48%, rgba(3, 9, 10, 0.48) 100%);
}

:global(:root[data-hn-theme="dark"] .home-action-button--secondary),
:global(:root.theme-dark .home-action-button--secondary) {
  background: var(--hn-panel);
  border-color: var(--hn-border-strong);
  color: var(--hn-accent);
}

:global(:root[data-hn-theme="dark"] .home-action-button--guide),
:global(:root.theme-dark .home-action-button--guide) {
  border-color: rgba(78, 128, 149, 0.7);
  background: linear-gradient(135deg, rgba(22, 62, 76, 0.96), rgba(9, 25, 32, 0.98));
  color: #c5e3ee;
}

.home-intro .home-background {
  animation: home-scene-in 1.1s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.home-intro :deep(.unified-nav-wrap) {
  animation: home-nav-in 0.7s 0.05s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.home-intro .hero-copy {
  animation: home-content-in 0.78s 0.12s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.home-intro .stat-row {
  animation: home-content-in 0.72s 0.28s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.home-intro .entry-grid {
  animation: home-content-in 0.72s 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes home-scene-in {
  from { opacity: 0; filter: blur(14px) brightness(0.82) saturate(0.76); transform: scale(1.06); }
  to { opacity: 0.9; filter: blur(7px) brightness(0.94) saturate(0.88); transform: scale(1.018); }
}

@keyframes home-nav-in {
  from { opacity: 0; transform: translateY(-16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes home-content-in {
  from { opacity: 0; transform: translateY(22px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .home-intro .home-background,
  .home-intro :deep(.unified-nav-wrap),
  .home-intro .hero-copy,
  .home-intro .stat-row,
  .home-intro .entry-grid {
    animation: none !important;
  }
}

/* 首页首屏：强化中心构图，同时保留足够的背景呼吸空间。 */
.home-hero {
  min-height: 62vh;
}

.hero-inner {
  display: flex;
  box-sizing: border-box;
  min-height: calc(62vh - 76px);
  align-items: center;
  justify-content: center;
  padding: 8vh 0 6vh;
  text-align: center;
}

.hero-copy {
  width: min(1040px, 100%);
  max-width: none;
  margin: 0 auto;
  text-align: center;
}

.hero-copy h1 {
  max-width: none;
  margin: 24px auto 28px;
  font-size: clamp(44px, 5vw, 74px);
  line-height: 1.08;
  letter-spacing: -0.045em;
  text-shadow:
    0 2px 0 color-mix(in srgb, var(--hn-panel) 58%, transparent),
    0 12px 28px color-mix(in srgb, var(--hn-text) 18%, transparent);
}

.hero-copy .eyebrow {
  margin: 0;
  text-align: center;
}

.hero-copy .lead {
  max-width: 1040px;
  margin: 0 auto;
  font-size: 16px;
  line-height: 1.9;
  text-align: center;
  white-space: nowrap;
}

.hero-actions {
  justify-content: center;
  margin-top: 34px;
}

.stat-row {
  gap: 10px;
  margin-top: 14px;
  margin-bottom: 10px;
}

:deep(.stat-card) {
  height: 104px;
  min-height: 104px;
  display: grid;
  grid-template-rows: minmax(0, 3fr) minmax(0, 2fr);
  padding: 8px 14px 7px;
  background: color-mix(in srgb, var(--hn-panel) 72%, transparent);
  backdrop-filter: blur(10px);
}

:deep(.stat-card > strong) {
  min-height: 0;
  align-self: center;
  margin-top: 0;
}

:deep(.stat-card > strong:not(.compact) b) {
  font-size: 39px;
  line-height: 1;
}

:deep(.stat-card > strong.compact) {
  min-height: 0;
  margin-top: 4px;
  line-height: 1.32;
}

:deep(.stat-card > strong.compact b) {
  font-size: 14px;
}

:deep(.stat-card > p) {
  min-height: 0;
  align-self: start;
  margin: 0;
  font-size: 13px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-grid {
  gap: 10px;
  margin-top: 4px;
  padding-bottom: 20px;
}

.entry-card {
  min-height: 108px;
  padding: 12px 16px 10px;
  background: color-mix(in srgb, var(--hn-panel) 72%, transparent);
  backdrop-filter: blur(10px);
}

.entry-card h2 {
  margin: 0 0 7px;
  font-size: 22px;
  line-height: 1.18;
}

.entry-card p {
  margin: 0 0 8px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.48;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-card b {
  font-size: 13px;
}

@media (max-width: 760px) {
  .home-hero {
    min-height: auto;
  }

  .hero-inner {
    min-height: 520px;
    padding: 54px 0 48px;
  }

  .hero-copy h1 {
    font-size: clamp(38px, 12vw, 52px);
  }

  .hero-copy .lead {
    white-space: normal;
  }

  .hero-actions {
    justify-content: stretch;
  }

  .entry-card {
    min-height: 0;
  }
}
</style>
