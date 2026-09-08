<template>
  <Transition name="toast-fade">
    <div
      v-if="modelValue && message"
      :class="['action-toast', type]"
      role="status"
      aria-live="polite"
    >
      <div class="toast-mark" aria-hidden="true">{{ mark }}</div>
      <div class="toast-body">
        <strong>{{ title }}</strong>
        <p>{{ message }}</p>
      </div>
      <button type="button" class="toast-close" aria-label="关闭提示" @click="close">×</button>
      <span v-if="duration > 0" class="toast-progress" :style="progressStyle"></span>
    </div>
  </Transition>
</template>

<script setup>
import { computed, onBeforeUnmount, watch } from 'vue';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  type: { type: String, default: 'info' },
  message: { type: String, default: '' },
  duration: { type: Number, default: 5000 },
});
const emit = defineEmits(['update:modelValue', 'close']);
let timer = null;

const titleMap = { success: '操作成功', error: '操作失败', warning: '请注意', info: '系统提示' };
const markMap = { success: 'OK', error: '!', warning: 'i', info: 'i' };
const title = computed(() => titleMap[props.type] || titleMap.info);
const mark = computed(() => markMap[props.type] || markMap.info);
const progressStyle = computed(() => ({ animationDuration: `${Math.max(props.duration, 1)}ms` }));

function clearTimer() {
  if (timer) {
    window.clearTimeout(timer);
    timer = null;
  }
}
function close() {
  clearTimer();
  emit('update:modelValue', false);
  emit('close');
}
watch(() => [props.modelValue, props.message, props.duration], () => {
  clearTimer();
  if (props.modelValue && props.message && props.duration > 0) {
    timer = window.setTimeout(close, props.duration);
  }
}, { immediate: true });
onBeforeUnmount(clearTimer);
</script>

<style scoped>
.action-toast {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  overflow: hidden;
  padding: 14px 14px 16px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 8px;
  background: color-mix(in srgb, var(--hn-card) 90%, var(--hn-accent) 10%);
  color: var(--hn-text);
  box-shadow: var(--hn-shadow-soft);
}
.toast-mark {
  min-width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: var(--hn-accent);
  color: #fff;
  font-size: 11px;
  font-weight: 900;
}
.toast-body { display: grid; gap: 3px; }
.toast-body strong { color: var(--hn-text); font-size: 14px; }
.toast-body p { margin: 0; color: var(--hn-muted); line-height: 1.5; }
.toast-close {
  width: 30px;
  height: 30px;
  border: 1px solid var(--hn-border);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-muted);
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}
.toast-progress {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 3px;
  width: 100%;
  background: var(--hn-accent);
  transform-origin: left center;
  animation-name: toast-progress;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}
.action-toast.error .toast-mark,
.action-toast.error .toast-progress { background: var(--hn-danger); }
.action-toast.warning .toast-mark,
.action-toast.warning .toast-progress { background: #b48318; }
.action-toast.success .toast-mark,
.action-toast.success .toast-progress { background: var(--hn-accent); }
.toast-fade-enter-active,
.toast-fade-leave-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.toast-fade-enter-from,
.toast-fade-leave-to { opacity: 0; transform: translateY(-6px); }
@keyframes toast-progress { from { transform: scaleX(1); } to { transform: scaleX(0); } }
</style>
