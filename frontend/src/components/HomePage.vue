<template>
  <PageShell>
    <section class="home-hero" @mousemove="moveHero" @mouseleave="resetHero">
      <UnifiedNav variant="light" />
      <div class="hero-inner">
        <div class="hero-copy">
          <p class="eyebrow">Blue Carbon Platform</p>
          <h1>海南蓝碳评估分析与数字化应用平台</h1>
          <p class="lead">
            汇聚红树林面积、区域指标、文献碳储与来源证据，形成面向政府管理、公众认知、科研评估和第三方查验的统一平台入口。
          </p>
          <div class="hero-actions" aria-label="首页快捷入口">
            <router-link class="home-action-button home-action-button--primary" to="/v2-public-data">查看数据资产</router-link>
            <router-link class="home-action-button home-action-button--secondary" to="/v2-government">进入政府端</router-link>
            <SystemGuideButton class="home-action-button home-action-button--guide" @open="guideOpen = true" />
          </div>
        </div>

        <figure class="photo-panel" :style="panelStyle" aria-label="海南红树林湿地实景主视觉">
          <img :src="heroImage" alt="海南红树林湿地与栈道" />
          <figcaption>
            <span>海南红树林湿地</span>
            <strong>蓝碳资源数字化治理</strong>
          </figcaption>
          <div class="photo-shade"></div>

          <div class="floating-card card-a"><strong>294</strong><span>区域指标</span></div>
          <div class="floating-card card-b"><strong>70</strong><span>文献证据</span></div>
          <div class="floating-card card-c"><strong>96</strong><span>面积记录</span></div>
        </figure>
      </div>
    </section>

    <section class="stat-row">
      <StatCard v-for="item in stats" :key="item.label" :label="item.label" :value="item.value" :unit="item.unit" :note="item.note" :tags="item.tags" />
    </section>

    <section class="entry-grid">
      <router-link v-for="entry in entries" :key="entry.title" :to="entry.to" class="entry-card">
        <span>{{ entry.name }}</span>
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
      tiltX: 0,
      tiltY: 0,
      stats: [
        { value: "68", unit: "项", label: "可信数据来源", note: "覆盖公开遥感、权威机构资料、同行评审文献与模型基础数据", tags: ["来源核验", "公开查验"] },
        { value: "294", unit: "条", label: "区域观察与评价指标", note: "用于区域比较、变化识别、生态状态分析和治理研判", tags: ["区域比较", "状态分析"] },
        { value: "199", unit: "项", label: "标准化指标体系", note: "统一指标名称、单位、统计口径和来源说明", tags: ["统一口径", "降低误读"] },
        { value: "数据治理 · 模型估算 · 存储服务 · 知识问答", unit: "", label: "多源数据协同底座", note: "支撑数据查询、来源核验、模型分析与辅助解释", tags: ["协同查询", "辅助解释"] },
      ],
      entries: [
        { name: "CarbonSeek", title: "碳溯", desc: "红树林覆盖与指标查询、碳储估算和模型解释", action: "进入碳溯", to: "/carbonseek" },
        { name: "DataAssets", title: "数据资产", desc: "来源追溯、文献证据、区域指标和面积记录", action: "浏览数据资产", to: "/v2-public-data" },
        { name: "Governance", title: "治理工作台", desc: "面向管理人员的筛选、追溯、管理和研判", action: "进入工作台", to: "/v2-government" },
        { name: "About", title: "关于项目", desc: "平台定位、数据基础、适用范围和查验边界", action: "查看说明", to: "/about" },
      ],
    };
  },
  computed: {
    panelStyle() {
      return { transform: `perspective(900px) rotateX(${this.tiltY}deg) rotateY(${this.tiltX}deg)` };
    },
  },
  mounted() {
    document.documentElement.style.overflow = "auto";
    document.body.style.overflow = "auto";
  },
  methods: {
    moveHero(event) {
      const rect = event.currentTarget.getBoundingClientRect();
      const x = (event.clientX - rect.left) / rect.width - 0.5;
      const y = (event.clientY - rect.top) / rect.height - 0.5;
      this.tiltX = x * 3.2;
      this.tiltY = y * -2.6;
    },
    resetHero() {
      this.tiltX = 0;
      this.tiltY = 0;
    },
  },
};
</script>

<style scoped>
.home-hero {
  min-height: 640px;
  background:
    radial-gradient(circle at 76% 16%, rgba(87, 210, 190, 0.18), transparent 28%),
    linear-gradient(145deg, var(--hn-bg-soft) 0%, var(--hn-bg) 58%, var(--hn-bg-soft) 100%);
}

.hero-inner {
  width: var(--hn-page);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 0.98fr) minmax(390px, 1.02fr);
  gap: 44px;
  align-items: center;
  padding: 36px 0 58px;
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  max-width: 820px;
  color: var(--hn-text);
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.14;
  letter-spacing: 0;
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

.photo-panel {
  position: relative;
  height: clamp(340px, 46vw, 430px);
  margin: 0;
  overflow: hidden;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
  box-shadow: 0 30px 78px rgba(12, 73, 63, 0.16), 0 0 90px rgba(31, 138, 122, 0.12);
  transition: transform 0.18s ease;
  isolation: isolate;
}

.photo-panel::before {
  content: "";
  position: absolute;
  inset: -34px;
  z-index: -1;
  background: radial-gradient(circle at 50% 48%, rgba(31, 138, 122, 0.22), transparent 66%);
  filter: blur(18px);
}

.photo-panel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(1.03) contrast(1.02);
}

.photo-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(5, 42, 36, 0.32), transparent 52%), linear-gradient(180deg, transparent 42%, rgba(5, 42, 36, 0.56));
}

figcaption {
  position: absolute;
  left: 24px;
  bottom: 24px;
  z-index: 2;
  display: grid;
  gap: 6px;
  color: #fff;
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.35);
}

figcaption span {
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

figcaption strong {
  font-size: 26px;
  line-height: 1.2;
}

.floating-card {
  position: absolute;
  z-index: 3;
  display: grid;
  gap: 2px;
  min-width: 118px;
  padding: 13px 15px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px);
  box-shadow: var(--hn-shadow-soft);
  animation: float 5s ease-in-out infinite;
}

.floating-card strong {
  color: #0d6b57;
  font-size: 27px;
}

.floating-card span {
  color: #31584f;
  font-size: 13px;
  font-weight: 900;
}

.card-a { right: 24px; top: 30px; }
.card-b { right: 34px; top: 132px; animation-delay: 0.8s; }
.card-c { left: 28px; top: 92px; animation-delay: 1.5s; }

.stat-row,
.entry-grid {
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

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@media (max-width: 940px) {
  .hero-inner {
    grid-template-columns: 1fr;
  }

  .stat-row,
  .entry-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .photo-panel {
    height: 360px;
  }

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

:global(:root[data-hn-theme="dark"]) .home-action-button--primary,
:global(:root.theme-dark) .home-action-button--primary {
  color: #08201c;
}

:global(:root[data-hn-theme="dark"]) .home-action-button--secondary,
:global(:root.theme-dark) .home-action-button--secondary {
  background: var(--hn-panel);
  border-color: var(--hn-border-strong);
  color: var(--hn-accent);
}

:global(:root[data-hn-theme="dark"]) .photo-panel,
:global(:root.theme-dark) .photo-panel {
  box-shadow: 0 34px 86px rgba(0, 0, 0, 0.34), 0 0 82px rgba(124, 226, 205, 0.1);
}

:global(:root[data-hn-theme="dark"]) .photo-shade,
:global(:root.theme-dark) .photo-shade {
  background: linear-gradient(90deg, rgba(2, 18, 20, 0.62), transparent 54%), linear-gradient(180deg, rgba(2, 18, 20, 0.08), rgba(2, 18, 20, 0.76));
}

:global(:root[data-hn-theme="dark"]) .floating-card,
:global(:root.theme-dark) .floating-card {
  border-color: rgba(176, 225, 214, 0.22);
  background: rgba(14, 45, 49, 0.78);
}

:global(:root[data-hn-theme="dark"]) .floating-card strong,
:global(:root[data-hn-theme="dark"]) .floating-card span,
:global(:root.theme-dark) .floating-card strong,
:global(:root.theme-dark) .floating-card span {
  color: var(--hn-text);
}
</style>
