<template>
  <button type="button" class="system-guide-button" aria-label="打开系统使用手册" @click="$emit('open')">
    <span class="guide-icon" aria-hidden="true">
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="M4.5 5.75A2.75 2.75 0 0 1 7.25 3H20v15.5H7.25A2.75 2.75 0 0 0 4.5 21.25V5.75Z" />
        <path d="M4.5 5.75A2.75 2.75 0 0 1 7.25 3H20v15.5H7.25A2.75 2.75 0 0 0 4.5 21.25V5.75Z" />
        <path d="M8 7h8M8 10.5h7M8 14h5" />
      </svg>
    </span>
    <span>系统使用手册</span>
  </button>
</template>

<script setup>
defineEmits(['open']);
</script>

<style scoped>
.system-guide-button {
  border: 1px solid rgba(31, 138, 122, 0.36);
  background: linear-gradient(135deg, rgba(210, 248, 239, 0.96), rgba(237, 255, 250, 0.94));
  color: #0a5d4e;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72), 0 10px 22px rgba(10, 104, 89, 0.12);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.system-guide-button:hover {
  transform: translateY(-2px);
  border-color: rgba(13, 107, 87, 0.58);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78), 0 14px 30px rgba(10, 104, 89, 0.16);
}

.system-guide-button:active {
  transform: translateY(0);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.62), 0 7px 16px rgba(10, 104, 89, 0.11);
}

.guide-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.guide-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

</style>
<style>
:root[data-hn-theme="dark"] .system-guide-button,
:root.theme-dark .system-guide-button {
  border-color: rgba(124, 226, 205, 0.42);
  background: linear-gradient(135deg, rgba(18, 96, 89, 0.74), rgba(15, 55, 62, 0.82));
  color: #dffbf5;
  box-shadow: inset 0 1px 0 rgba(212, 255, 246, 0.12), 0 12px 28px rgba(0, 0, 0, 0.28);
}

:root[data-hn-theme="dark"] .system-guide-button:hover,
:root.theme-dark .system-guide-button:hover {
  border-color: rgba(124, 226, 205, 0.72);
  box-shadow: inset 0 1px 0 rgba(212, 255, 246, 0.18), 0 16px 34px rgba(0, 0, 0, 0.34);
}
</style>
