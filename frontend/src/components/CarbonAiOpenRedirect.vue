<template>
  <main class="ai-open-redirect" aria-live="polite">
    <p>正在打开 AI 碳助手...</p>
  </main>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

onMounted(() => {
  window.dispatchEvent(new CustomEvent("hnblue-open-ai"));
  router.replace("/carbonseek");
});
</script>

<style scoped>
.ai-open-redirect {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: var(--hn-bg);
  color: var(--hn-text);
  font-family: Arial, "Microsoft YaHei", sans-serif;
}

.ai-open-redirect p {
  margin: 0;
  color: var(--hn-muted);
  font-weight: 800;
}
</style>
