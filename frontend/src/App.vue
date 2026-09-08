<template>
  <router-view />
  <CarbonAiFloatingAssistant />
</template>

<script>
import CarbonAiFloatingAssistant from "@/components/CarbonAiFloatingAssistant.vue";

export default {
  name: "App",
  components: { CarbonAiFloatingAssistant },
};
</script>

<style>
@import "@/components/common/hnblue-ui.css";
#app,
html,
body {
  width: 100%;
  min-height: 100%;
  margin: 0;
}

a {
  text-decoration: none;
}

.placeholder-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--hn-bg);
  color: var(--hn-text);
  font-family: Arial, "Microsoft YaHei", sans-serif;
}

.placeholder-panel {
  width: min(680px, calc(100% - 48px));
  padding: 32px;
  border: 1px solid var(--hn-border);
  background: var(--hn-card);
  border-radius: 8px;
}

.placeholder-panel h1 {
  margin: 0 0 12px;
  font-size: 28px;
}

.placeholder-panel p {
  margin: 0 0 20px;
  line-height: 1.7;
}

.placeholder-panel a {
  color: var(--hn-accent);
  font-weight: 700;
}
</style>
