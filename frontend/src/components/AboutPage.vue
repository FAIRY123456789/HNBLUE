<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="关于项目"
      eyebrow="About Platform"
      subtitle="面向蓝碳资源管理、数据核验和公众认知，集中展示平台的数据基础、分析能力与应用边界"
      :tags="['资源管理', '数据核验', '公众认知', '第三方查验']"
    />

    <section class="position-grid">
      <article v-for="item in positions" :key="item.title">
        <span>{{ item.name }}</span>
        <h2>{{ item.title }}</h2>
        <p>{{ item.desc }}</p>
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
        { name: "资源管理", title: "海南红树林与蓝碳资源", desc: "服务红树林保护、蓝碳资源认知和区域生态状态研判，帮助管理人员快速理解数据覆盖范围" },
        { name: "数据核验", title: "多来源数据汇聚", desc: "组织公开遥感、权威机构资料、文献证据和模型基础数据，保留来源、年份、方法和质量信息" },
        { name: "辅助研判", title: "区域指标与模型解释", desc: "把区域指标、文献证据和碳储估算放在同一语境中解释，降低跨来源数据误读" },
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
.boundary-grid {
  width: var(--hn-page);
  margin: 0 auto;
}

.position-grid,
.boundary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.position-grid article,
.architecture-panel,
.boundary-grid article {
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}

.position-grid article,
.boundary-grid article {
  min-height: 214px;
  padding: 22px;
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

.architecture-panel {
  margin-top: 18px;
  padding: 24px;
}

.flow {
  display: grid;
  grid-template-columns: 1fr 36px 1fr 36px 1fr 36px 1fr;
  gap: 8px;
  align-items: center;
}

.flow span {
  min-height: 82px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-weight: 900;
  text-align: center;
}

.flow b {
  height: 2px;
  background: rgba(13, 107, 87, 0.35);
}

.boundary-grid {
  margin-top: 18px;
  padding-bottom: 48px;
}

@media (max-width: 860px) {
  .position-grid,
  .boundary-grid,
  .flow {
    grid-template-columns: 1fr;
  }

  .flow b {
    width: 2px;
    height: 18px;
    justify-self: center;
  }
}
</style>
