<template>
  <PageShell>
    <UnifiedNav />
    <PageHero
      title="虚拟样地"
      eyebrow="Virtual Plot Designer"
      subtitle="创建、编辑和比较虚拟样地，使用批量模型估算支撑情景分析"
      :tags="['样地管理', '批量估算', '情景比较', '结果导出']"
    />
    <VirtualPlotDesigner />
  </PageShell>
</template>

<script setup>
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import VirtualPlotDesigner from '@/components/model/VirtualPlotDesigner.vue';
</script>
