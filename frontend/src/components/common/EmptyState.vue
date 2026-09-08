<template>
  <section class="empty-state">
    <strong>{{ title }}</strong>
    <p>{{ message }}</p>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: "暂无数据" },
  message: { type: String, default: "当前内容暂无可用记录。" },
});
</script>

<style scoped>
.empty-state {
  min-height: 150px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 24px;
  border: 1px dashed rgba(13, 107, 87, 0.28);
  border-radius: var(--hn-radius);
  background: linear-gradient(135deg, rgba(230, 243, 239, 0.72), rgba(255, 255, 255, 0.82));
  text-align: center;
}

strong {
  color: var(--hn-text);
  font-size: 18px;
}

p {
  max-width: 520px;
  margin: 0;
  color: var(--hn-muted);
  line-height: 1.6;
}
</style>
