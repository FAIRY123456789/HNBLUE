<template>
  <Teleport to="body">
    <div v-if="open" class="drawer-layer" @click.self="$emit('close')">
      <aside class="detail-drawer">
        <button class="close" type="button" @click="$emit('close')">关闭</button>
        <p class="eyebrow">记录详情</p>
        <h2>{{ title }}</h2>
        <dl v-if="publicItems.length">
          <div v-for="item in publicItems" :key="item.key">
            <dt>{{ item.label }}</dt>
            <dd>{{ item.value }}</dd>
          </div>
        </dl>
        <p v-else class="empty-detail">当前记录暂无可展示字段。</p>
      </aside>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from "vue";
import { formatRecordForPublic } from "@/config/publicPresentationPolicy";

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: "记录详情" },
  record: { type: Object, default: () => ({}) },
});
defineEmits(["close"]);

const publicItems = computed(() => formatRecordForPublic(props.record));
</script>

<style scoped>
.drawer-layer {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
  background: rgba(4, 28, 24, 0.3);
}

.detail-drawer {
  width: min(560px, 94vw);
  height: 100vh;
  overflow: auto;
  padding: 26px;
  background: var(--hn-card);
  box-shadow: -18px 0 46px rgba(4, 28, 24, 0.22);
}

.close {
  float: right;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--hn-border);
  border-radius: 7px;
  background: var(--hn-card);
  color: var(--hn-accent);
  cursor: pointer;
  font-weight: 900;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}

h2 {
  margin: 0 0 20px;
  color: var(--hn-text);
}

dl {
  display: grid;
  gap: 12px;
}

div {
  padding-bottom: 10px;
  border-bottom: 1px solid var(--hn-border);
}

dt {
  color: var(--hn-muted);
  font-size: 12px;
}

dd {
  margin: 5px 0 0;
  color: var(--hn-text);
  line-height: 1.55;
  word-break: break-word;
}

.empty-detail {
  color: var(--hn-muted);
}
</style>
