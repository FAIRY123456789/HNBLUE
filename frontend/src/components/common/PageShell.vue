<template>
  <main :class="['page-shell', tone]">
    <slot />
  </main>
</template>

<script setup>
defineProps({
  tone: { type: String, default: "light" },
});
</script>

<style scoped>
.page-shell {
  min-height: 100vh;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 82% 12%, rgba(84, 201, 179, 0.14), transparent 28%),
    linear-gradient(180deg, var(--hn-bg-soft), var(--hn-bg));
  color: var(--hn-text);
  font-family: Arial, "Microsoft YaHei", sans-serif;
}

.page-shell.deep {
  background:
    radial-gradient(circle at 80% 10%, rgba(84, 201, 179, 0.16), transparent 28%),
    linear-gradient(180deg, var(--hn-bg-soft), var(--hn-bg));
}
</style>
