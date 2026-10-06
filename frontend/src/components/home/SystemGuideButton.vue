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
  border: 1px solid rgba(92, 154, 178, 0.5);
  background: linear-gradient(135deg, rgba(210, 235, 246, 0.98), rgba(237, 248, 252, 0.96));
  color: #16566b;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76), 0 10px 22px rgba(34, 103, 128, 0.14);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.system-guide-button:hover {
  transform: translateY(-2px);
  border-color: rgba(60, 127, 153, 0.72);
  background: linear-gradient(135deg, rgba(197, 228, 241, 0.98), rgba(226, 243, 249, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8), 0 14px 30px rgba(34, 103, 128, 0.18);
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
  border-color: rgba(83, 128, 118, 0.42);
  background: linear-gradient(135deg, rgba(26, 52, 48, 0.92), rgba(9, 19, 22, 0.96));
  color: #c8d2cf;
  box-shadow: inset 0 1px 0 rgba(136, 167, 160, 0.08), 0 12px 28px rgba(0, 0, 0, 0.46);
}

:root[data-hn-theme="dark"] .system-guide-button:hover,
:root.theme-dark .system-guide-button:hover {
  border-color: rgba(91, 145, 132, 0.62);
  box-shadow: inset 0 1px 0 rgba(136, 167, 160, 0.12), 0 16px 34px rgba(0, 0, 0, 0.54);
}
</style>
