<template>
  <span class="money-amount" :aria-label="`${formattedNumber} đồng`">
    <span class="money-number" aria-hidden="true">{{ formattedNumber }}</span>&nbsp;<span class="money-currency" aria-hidden="true">₫</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { formatNumber } from '../utils/currency.js'

const props = defineProps({ value: { type: [Number, String], default: 0 } })
const formattedNumber = computed(() => formatNumber(props.value))
</script>
