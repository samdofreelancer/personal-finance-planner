<template>
  <input
    :id="id"
    :value="displayValue"
    :name="name"
    type="text"
    inputmode="numeric"
    autocomplete="off"
    spellcheck="false"
    :required="required"
    :placeholder="placeholder"
    :aria-label="ariaLabel"
    :aria-invalid="ariaInvalid"
    @input="onInput"
    @blur="onBlur"
  >
</template>

<script setup>
import { computed, nextTick } from 'vue'
import { formatNumber, parseCurrencyInput } from '../utils/currency.js'

const props = defineProps({
  id: { type: String, default: undefined },
  modelValue: { type: [Number, String], default: '' },
  name: { type: String, required: true },
  placeholder: { type: String, default: '' },
  ariaLabel: { type: String, default: undefined },
  ariaInvalid: { type: String, default: undefined },
  required: { type: Boolean, default: true },
  nullable: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const displayValue = computed(() =>
  props.modelValue === '' || props.modelValue == null
    ? ''
    : formatNumber(props.modelValue),
)

function onInput(event) {
  const input = event.target
  const caret = input.selectionStart ?? input.value.length
  const digitsBeforeCaret = input.value.slice(0, caret).replace(/\D/g, '').length
  const parsed = parseCurrencyInput(input.value)
  emit('update:modelValue', parsed === '' && props.nullable ? null : parsed)

  const formatted = parsed === '' ? '' : formatNumber(parsed)
  input.value = formatted
  const nextCaret = positionAfterDigits(formatted, digitsBeforeCaret)
  nextTick(() => input.setSelectionRange(nextCaret, nextCaret))
}

function onBlur(event) {
  event.target.value = displayValue.value
}

function positionAfterDigits(value, digitCount) {
  if (digitCount === 0) return 0
  let seen = 0
  for (let index = 0; index < value.length; index += 1) {
    if (/\d/.test(value[index])) seen += 1
    if (seen === digitCount) return index + 1
  }
  return value.length
}
</script>
