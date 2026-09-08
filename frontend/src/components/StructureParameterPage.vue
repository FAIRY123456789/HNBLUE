<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="结构参数"
      eyebrow="Structure Parameter"
      subtitle="输入红树林结构参数，调用模型代理完成单位面积生物量估算，并生成可追溯解释"
      :tags="['真实输入', '模型估算', '历史导出', '智能解释']"
    />
    <StructurePredictor />
  </PageShell>
</template>

<script setup>
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import StructurePredictor from '@/components/model/StructurePredictor.vue';
</script>
