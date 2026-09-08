<template>
  <section class="search-panel">
    <label v-for="field in fields" :key="field.key">
      <span>{{ field.label }}</span>
      <select v-if="field.type === 'select'" :value="modelValue[field.key]" @change="update(field.key, $event.target.value)">
        <option v-for="option in field.options || []" :key="option.value ?? option" :value="option.value ?? option">
          {{ option.label ?? option }}
        </option>
      </select>
      <input
        v-else
        :type="field.type || 'text'"
        :placeholder="field.placeholder || ''"
        :value="modelValue[field.key]"
        @input="update(field.key, $event.target.value)"
        @keyup.enter="$emit('search')"
      />
    </label>
    <div class="actions">
      <ActionButton label="查询" @click="$emit('search')" />
      <ActionButton label="重置" variant="secondary" @click="$emit('reset')" />
    </div>
  </section>
</template>

<script setup>
import ActionButton from '@/components/common/ActionButton.vue';

const props = defineProps({
  fields: { type: Array, default: () => [] },
  modelValue: { type: Object, required: true },
});
const emit = defineEmits(['update:modelValue', 'search', 'reset']);

function update(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value });
}
</script>

<style scoped>
.search-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  align-items: end;
  padding: 14px;
  border: 1px solid var(--hn-border);
  border-radius: var(--hn-radius);
  background: var(--hn-surface);
  box-shadow: var(--hn-shadow-soft);
}

label {
  display: grid;
  gap: 6px;
}

span {
  color: var(--hn-muted);
  font-size: 12px;
  font-weight: 800;
}

input,
select {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-text);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

input:focus,
select:focus {
  border-color: rgba(31, 138, 122, 0.58);
  box-shadow: 0 0 0 3px rgba(31, 138, 122, 0.12);
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
