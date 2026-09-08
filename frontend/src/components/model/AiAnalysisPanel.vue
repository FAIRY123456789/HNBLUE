<template>
  <section class="ai-panel">
    <div class="ai-head">
      <div>
        <p class="eyebrow">AI Analysis</p>
        <h3>{{ title }}</h3>
      </div>
      <ActionButton size="sm" variant="secondary" :label="isLoading ? '生成中' : buttonLabel" :disabled="disabled || isLoading" @click="$emit('generate')" />
    </div>
    <div v-if="isLoading && !content" class="ai-empty" aria-live="polite">{{ status || '正在生成智能解释...' }}</div>
    <div v-else-if="isLoading && content" class="ai-progress" aria-live="polite">{{ status || '正在生成智能解释...' }}</div>
    <MdPreview v-if="content" editor-id="hnblue-ai-preview" :model-value="content" preview-theme="github" />
    <div v-if="error" class="ai-error">{{ error }}</div>
    <div v-if="!content && !isLoading && !error" class="ai-empty">模型估算完成后将自动生成解释</div>
  </section>
</template>

<script setup>
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';
import ActionButton from '@/components/common/ActionButton.vue';

defineProps({
  title: { type: String, default: '智能解释' },
  content: { type: String, default: '' },
  error: { type: String, default: '' },
  isLoading: { type: Boolean, default: false },
  status: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  buttonLabel: { type: String, default: '重新生成解释' },
});
defineEmits(['generate']);
</script>

<style scoped>
.ai-panel {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
}
.ai-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.eyebrow { margin: 0 0 4px; color: var(--hn-accent); font-size: 12px; font-weight: 900; }
h3 { margin: 0; color: var(--hn-text); }
.ai-empty, .ai-error, .ai-progress { color: var(--hn-muted); line-height: 1.6; }
.ai-progress { font-size: 13px; }
.ai-error { color: var(--hn-danger); }
:deep(.md-editor-preview-wrapper) { padding: 0; background: transparent; }
:deep(.md-editor-preview) { color: var(--hn-text); background: transparent; font-size: 14px; }
</style>
