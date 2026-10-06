<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="关于项目"
      eyebrow="About Platform"
      subtitle="面向蓝碳资源管理、数据核验和公众认知，集中展示平台的数据基础、分析能力与应用边界"
      :tags="['资源管理', '数据核验', '公众认知', '第三方查验']"
    />

    <section class="about-command">
      <article class="mission-card">
        <span>PROJECT MISSION</span>
        <h2>让蓝碳数据可查、可比、可追溯</h2>
        <p>HNBLUE 把区域资源记录、公开来源、文献证据与模型解释集中到统一入口，重点解决“数据从哪里来、口径是否一致、结果如何复核”三个实际问题。</p>
        <div class="mission-actions">
          <router-link to="/v2-public-data">核验数据资产</router-link>
          <router-link to="/carbonseek">进入碳溯分析</router-link>
        </div>
      </article>
      <div class="fact-grid" aria-label="平台能力概览">
        <article v-for="fact in facts" :key="fact.label">
          <strong>{{ fact.value }}</strong>
          <span>{{ fact.label }}</span>
          <small>{{ fact.note }}</small>
        </article>
      </div>
    </section>

    <section class="position-grid">
      <article v-for="item in positions" :key="item.title">
        <span>{{ item.name }}</span>
        <h2>{{ item.title }}</h2>
        <p>{{ item.desc }}</p>
        <dl><div><dt>适用对象</dt><dd>{{ item.audience }}</dd></div><div><dt>主要用途</dt><dd>{{ item.use }}</dd></div></dl>
        <router-link :to="item.to">{{ item.action }}</router-link>
      </article>
    </section>

    <section class="architecture-panel">
      <SectionHeader title="平台组织方式" eyebrow="Platform Scope" subtitle="围绕红树林与蓝碳资源管理需求，把多来源数据、区域指标、文献证据、模型解释和治理研判组织到同一套可查验流程中" />
      <div class="flow">
        <span>公开来源</span>
        <b></b>
        <span>数据资产</span>
        <b></b>
        <span>碳储估算</span>
        <b></b>
        <span>治理研判</span>
      </div>
    </section>

    <section class="quick-panel">
      <SectionHeader title="常用工作入口" eyebrow="Operational Entry" />
      <div class="quick-grid">
        <router-link v-for="entry in quickEntries" :key="entry.title" :to="entry.to">
          <span>{{ entry.index }}</span>
          <div><h2>{{ entry.title }}</h2><p>{{ entry.desc }}</p></div>
          <b>进入</b>
        </router-link>
      </div>
    </section>

    <section class="boundary-grid">
      <article v-for="item in boundaries" :key="item.title">
        <h2>{{ item.title }}</h2>
        <p>{{ item.desc }}</p>
      </article>
    </section>
  </PageShell>
</template>

<script>
import PageShell from "@/components/common/PageShell.vue";
import UnifiedNav from "@/components/common/UnifiedNav.vue";
import PageHero from "@/components/common/PageHero.vue";
import SectionHeader from "@/components/common/SectionHeader.vue";

export default {
  name: "AboutPage",
  components: { PageShell, UnifiedNav, PageHero, SectionHeader },
  data() {
    return {
      positions: [
{ name: "资源管理", title: "蓝碳资源演示", desc: "使用合成面积、生态状态和空间记录演示数据覆盖与区域差异", audience: "政府管理与项目人员", use: "区域筛选、状态研判", action: "查看区域资料", to: "/v2-public-data" },
{ name: "数据核验", title: "多来源数据汇聚", desc: "组织合成遥感指标、虚构来源元数据和示例证据，演示来源、年份、方法和质量信息", audience: "科研与第三方查验人员", use: "来源追溯、口径复核", action: "核验数据来源", to: "/v2-public-data" },
{ name: "辅助研判", title: "区域指标与模型解释", desc: "把合成指标、示例证据和演示估算放在同一语境中解释，降低跨来源数据误读", audience: "业务分析与评审人员", use: "模型解释、证据对照", action: "进入碳溯模块", to: "/carbonseek" },
      ],
      facts: [
        { value: "68", label: "可信来源", note: "公开数据与权威材料" },
        { value: "294", label: "区域指标", note: "支持市县级查询" },
        { value: "70", label: "文献证据", note: "保留引用与来源链接" },
        { value: "41", label: "数据库表", note: "覆盖八个业务域" },
      ],
      quickEntries: [
{ index: "01", title: "查询与核验数据", desc: "查看合成面积、区域指标、示例证据与虚构来源信息", to: "/v2-public-data" },
{ index: "02", title: "开展碳储分析", desc: "进入结构参数、演示推演与证据核验流程", to: "/carbonseek" },
{ index: "03", title: "执行治理流程", desc: "使用测试账号演示筛选、追溯、变更申请与工单审批", to: "/v2-government" },
{ index: "04", title: "查看空间资料", desc: "浏览不附带真实业务指标的基础地图与合成空间记录", to: "/visual" },
      ],
      boundaries: [
        { title: "政府治理与辅助研判", desc: "平台提供筛选、追溯、比对和工单流转能力，辅助管理部门形成材料和研判依据" },
        { title: "公众认知与第三方查验", desc: "页面优先展示中文名称、用途说明、数值单位和来源依据，便于首次接触平台的访问者理解" },
        { title: "适用范围与使用边界", desc: "模型估算和情景推演用于解释与参考，不直接替代正式核证、执法判断或政府统计结果" },
      ],
    };
  },
  mounted() {
    document.documentElement.style.overflow = "auto";
    document.body.style.overflow = "auto";
  },
};
</script>

<style scoped>
.position-grid,
.architecture-panel,
.boundary-grid,
.about-command,
.quick-panel {
  width: var(--hn-page);
  margin: 0 auto;
}

.position-grid,
.boundary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.about-command {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  gap: 14px;
  margin-bottom: 18px;
}

.mission-card,
.quick-panel {
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.mission-card {
  padding: 28px;
  border-color: var(--hn-border);
  background: linear-gradient(135deg, var(--hn-soft) 0%, var(--hn-panel) 72%);
  box-shadow: var(--hn-shadow-soft);
}

.mission-card > span {
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}

.mission-card h2 { color: var(--hn-text); font-size: clamp(25px, 3vw, 38px); }
.mission-card p { color: var(--hn-muted); }

.mission-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.mission-actions a,
.position-grid a {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  padding: 0 13px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-accent);
  font-weight: 900;
}

.mission-actions a {
  border-color: var(--hn-border-strong);
  background: var(--hn-control-bg);
  color: var(--hn-accent);
}

.fact-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0;
  overflow: hidden;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.fact-grid article {
  display: grid;
  grid-template-columns: 58px 1fr;
  grid-template-rows: auto auto;
  align-content: center;
  gap: 2px 10px;
  min-height: 0;
  padding: 14px 16px;
}

.fact-grid article:nth-child(odd) { border-right: 1px solid var(--hn-border); }
.fact-grid article:nth-child(-n + 2) { border-bottom: 1px solid var(--hn-border); }

.fact-grid strong { grid-row: 1 / 3; align-self: center; color: var(--hn-accent); font-size: 28px; }
.fact-grid span { color: var(--hn-text); font-weight: 900; }
.fact-grid small { color: var(--hn-muted); }

.position-grid article,
.architecture-panel,
.boundary-grid article {
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.position-grid article {
  min-height: 250px;
  padding: 22px;
}

.boundary-grid article {
  min-height: 144px;
  padding: 18px 20px;
}

.position-grid span {
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 900;
}

h2 {
  margin: 14px 0 10px;
  font-size: 24px;
}

p {
  margin: 0;
  color: var(--hn-muted);
  line-height: 1.72;
}

.position-grid dl {
  display: grid;
  gap: 6px;
  margin: 18px 0;
}

.position-grid dl div {
  display: grid;
  grid-template-columns: 68px 1fr;
  gap: 8px;
  padding-top: 7px;
  border-top: 1px solid var(--hn-border);
  font-size: 13px;
}

.position-grid dt { color: var(--hn-muted); }
.position-grid dd { margin: 0; color: var(--hn-text); font-weight: 800; }

.architecture-panel {
  margin-top: 18px;
  padding: 24px;
}

.flow {
  width: min(900px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 24px minmax(140px, 1fr) 24px minmax(140px, 1fr) 24px minmax(140px, 1fr);
  gap: 6px;
  align-items: center;
}

.flow span {
  min-height: 64px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-weight: 900;
  text-align: center;
  padding: 0 12px;
}

.flow b {
  height: 2px;
  background: rgba(13, 107, 87, 0.35);
}

.boundary-grid {
  margin-top: 18px;
  padding-bottom: 36px;
}

.quick-panel {
  margin-top: 18px;
  padding: 18px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.quick-grid a {
  min-height: 78px;
  display: grid;
  grid-template-columns: 26px 1fr;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
  color: var(--hn-text);
}

.quick-grid a > span { color: var(--hn-accent); font-size: 12px; font-weight: 900; }
.quick-grid h2 { margin: 0 0 3px; font-size: 15px; }
.quick-grid p { font-size: 12px; line-height: 1.45; }
.quick-grid b { display: none; color: var(--hn-accent); }

:global(:root[data-hn-theme="dark"] .mission-card),
:global(:root.theme-dark .mission-card) {
  border-color: var(--hn-border);
  background: linear-gradient(135deg, var(--hn-soft) 0%, var(--hn-panel) 76%);
  box-shadow: var(--hn-shadow-soft);
}

@media (max-width: 860px) {
  .position-grid,
  .boundary-grid,
  .flow,
  .about-command,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .flow b {
    width: 2px;
    height: 18px;
    justify-self: center;
  }
}
/* 三组使用边界已迁入全站公共脚栏，避免关于页重复展示。 */
.boundary-grid {
  display: none;
}
</style>
