<template>
  <button type="button" class="guide-button" @click="$emit('open')">系统使用手册</button>
</template>

<script setup>
defineEmits(['open']);
</script>

<style scoped>
.guide-button {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  padding: 0 18px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 8px;
  background: var(--hn-card);
  color: var(--hn-accent);
  cursor: pointer;
  font-weight: 900;
}
.guide-button:hover { box-shadow: var(--hn-shadow-soft); }
</style>
