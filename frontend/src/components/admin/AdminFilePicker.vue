<script setup>
import { ref } from 'vue'

defineProps({
  label: { type: String, required: true },
  hint: String,
  accept: String,
  multiple: Boolean,
  disabled: Boolean,
  busy: Boolean
})
const emit = defineEmits(['select'])
const input = ref(null)
function select(event) {
  const files = Array.from(event.target.files)
  event.target.value = ''
  if (files.length) emit('select', files)
}
</script>

<template>
  <div class="admin-file-picker">
    <input ref="input" type="file" hidden :accept="accept" :multiple="multiple" :disabled="disabled || busy" @change="select" />
    <a-button :disabled="disabled" :loading="busy" @click="input.click()">{{ label }}</a-button>
    <span v-if="hint" class="muted">{{ hint }}</span>
  </div>
</template>

<style scoped>
.admin-file-picker { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; min-width: 0; }
.admin-file-picker span { overflow-wrap: anywhere; }
</style>
