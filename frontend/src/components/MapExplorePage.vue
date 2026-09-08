<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="蓝碳地图"
      eyebrow="Blue Map"
      subtitle="作为碳溯与治理工作台共同复用的地图入口，支持区域概览、来源追溯和指标查询"
      :tags="['区域探索', '来源追溯', '主岛与三沙']"
    />
    <section class="map-page-panel">
      <SectionHeader title="探索图" eyebrow="Hainan" subtitle="支持省级与市县级切换，区域详情以基础资料、蓝碳线索、指标记录和来源依据为核心" />
      <HainanBlueCarbonMap />
    </section>
  </PageShell>
</template>

<script setup>
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import SectionHeader from '@/components/common/SectionHeader.vue';
import HainanBlueCarbonMap from '@/components/HainanBlueCarbonMap.vue';
</script>

<style scoped>
.map-page-panel {
  width: var(--hn-page);
  margin: 0 auto 48px;
  padding: 24px;
  border: 1px solid var(--hn-border);
  border-radius: var(--hn-radius);
  background: var(--hn-panel);
  box-shadow: var(--hn-shadow-soft);
}
</style>
