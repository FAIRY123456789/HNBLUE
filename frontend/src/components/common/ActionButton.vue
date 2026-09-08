<template>
  <button :class="['action-button', variant, size, { block, subtle }]" :type="type" :disabled="disabled">
    <span><slot>{{ label }}</slot></span>
  </button>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  icon: { type: String, default: '' },
  type: { type: String, default: 'button' },
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
  subtle: { type: Boolean, default: false },
});
</script>

<style scoped>
.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-accent);
  cursor: pointer;
  font-weight: 900;
  line-height: 1;
  white-space: nowrap;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, border-color 0.18s ease, opacity 0.18s ease;
}

.action-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(13, 107, 87, 0.12);
}

.action-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.action-button.primary {
  border-color: var(--hn-accent);
  background: var(--hn-accent);
  color: #fff;
}

.action-button.secondary {
  background: var(--hn-soft);
  color: var(--hn-accent);
}

.action-button.ghost {
  background: transparent;
  color: var(--hn-accent);
}

.action-button.danger {
  border-color: rgba(161, 58, 58, 0.24);
  background: color-mix(in srgb, var(--hn-danger) 12%, var(--hn-card));
  color: var(--hn-danger);
}

.action-button.sm {
  min-height: 32px;
  padding: 0 10px;
  font-size: 12px;
}

.action-button.block {
  width: 100%;
}
</style>
