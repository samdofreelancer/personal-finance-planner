<template>
  <span class="month-input-control">
    <span class="month-input-display" aria-hidden="true">{{ formattedMonth }}</span>
    <input
      type="month"
      :value="modelValue"
      :disabled="disabled"
      :aria-label="ariaLabel"
      @input="$emit('update:modelValue', $event.target.value)"
    >
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  ariaLabel: { type: String, default: '' },
})

defineEmits(['update:modelValue'])

const formattedMonth = computed(() => {
  const match = /^(\d{4})-(\d{2})$/.exec(props.modelValue)
  return match ? `${match[2]}/${match[1]}` : '--/----'
})
</script>
