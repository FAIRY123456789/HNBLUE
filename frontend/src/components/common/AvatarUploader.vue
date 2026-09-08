<template>
  <section class="avatar-uploader">
    <input
      ref="fileInput"
      class="file-input"
      type="file"
      accept="image/png,image/jpeg,image/webp"
      @change="handleInput"
    />
    <button
      type="button"
      class="drop-zone"
      @click="openPicker"
      @dragover.prevent
      @drop.prevent="handleDrop"
    >
      <span class="preview" :style="previewStyle">{{ previewText }}</span>
      <span class="drop-copy">
        <strong>选择或拖放头像</strong>
        <small>{{ fileName || 'PNG、JPG、WebP，最大 2MB' }}</small>
      </span>
    </button>
    <div class="uploader-actions">
      <button type="button" class="select-btn" @click="openPicker">选择文件</button>
      <button type="button" class="upload-btn" :disabled="!selectedFile || busy" @click="$emit('upload')">
        {{ busy ? '上传中...' : '上传头像' }}
      </button>
    </div>
    <div v-if="busy" class="upload-progress" aria-label="头像上传中"><span></span></div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';

const props = defineProps({
  avatar: { type: String, default: '' },
  busy: { type: Boolean, default: false },
});
const emit = defineEmits(['select', 'upload', 'error']);
const fileInput = ref(null);
const selectedFile = ref(null);
const objectUrl = ref('');

const fileName = computed(() => selectedFile.value?.name || '');
const previewSource = computed(() => {
  if (objectUrl.value) return objectUrl.value;
  if (!props.avatar) return '';
  return String(props.avatar).startsWith('data:') ? props.avatar : `data:image/png;base64,${props.avatar}`;
});
const previewStyle = computed(() => previewSource.value ? { backgroundImage: `url(${previewSource.value})`, color: 'transparent' } : {});
const previewText = computed(() => fileName.value ? fileName.value.slice(0, 1).toUpperCase() : 'HN');

function openPicker() { fileInput.value?.click(); }
function resetObjectUrl() {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value);
  objectUrl.value = '';
}
function acceptFile(file) {
  if (!file) return;
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
    emit('error', '头像仅支持 PNG、JPG、WebP 格式');
    return;
  }
  if (file.size > 2 * 1024 * 1024) {
    emit('error', '头像文件不能超过 2MB');
    return;
  }
  resetObjectUrl();
  selectedFile.value = file;
  objectUrl.value = URL.createObjectURL(file);
  emit('select', file);
}
function handleInput(event) { acceptFile(event.target.files?.[0]); }
function handleDrop(event) { acceptFile(event.dataTransfer.files?.[0]); }
watch(() => props.avatar, () => { if (!selectedFile.value) resetObjectUrl(); });
onBeforeUnmount(resetObjectUrl);
</script>

<style scoped>
.avatar-uploader { display: grid; gap: 12px; }
.file-input { position: absolute; width: 1px; height: 1px; opacity: 0; pointer-events: none; }
.drop-zone {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  width: 100%;
  padding: 14px;
  border: 1px dashed var(--hn-border-strong);
  border-radius: 8px;
  background: var(--hn-soft);
  color: var(--hn-text);
  cursor: pointer;
  text-align: left;
}
.preview {
  width: 96px;
  height: 96px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--hn-accent);
  background-position: center;
  background-size: cover;
  color: #fff;
  font-size: 24px;
  font-weight: 900;
}
.drop-copy { display: grid; gap: 6px; }
.drop-copy strong { color: var(--hn-text); }
.drop-copy small { color: var(--hn-muted); line-height: 1.5; }
.uploader-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.select-btn,
.upload-btn {
  min-height: 38px;
  padding: 0 13px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-accent);
  cursor: pointer;
  font-weight: 900;
}
.upload-btn { border-color: var(--hn-accent); background: var(--hn-accent); color: #fff; }
.upload-btn:disabled { cursor: not-allowed; opacity: 0.55; }
.upload-progress { height: 6px; overflow: hidden; border-radius: 999px; background: var(--hn-border); }
.upload-progress span { display: block; width: 45%; height: 100%; background: var(--hn-accent); animation: avatar-progress 1.1s ease-in-out infinite; }
@keyframes avatar-progress { 0% { transform: translateX(-100%); } 100% { transform: translateX(230%); } }
@media (max-width: 560px) { .drop-zone { grid-template-columns: 1fr; justify-items: start; } }
</style>
