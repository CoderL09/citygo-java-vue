<script setup lang="ts">
import { computed } from 'vue'
import type { TagProps } from 'element-plus'
import { statusText } from '@/utils/order'
const props = defineProps<{ status: string }>()
const tones: Record<string, TagProps['type']> = {
  PENDING_PAYMENT: 'warning',
  PENDING_CONFIRMATION: 'warning',
  CONFIRMED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
  CLOSED: 'info',
  REFUNDING: 'warning',
  REFUNDED: 'info',
}
const tone = computed(() => tones[props.status] || 'info')
</script>
<template>
  <el-tag :type="tone" effect="light" class="status-badge"
    ><span class="status-dot" aria-hidden="true" />{{ statusText(status) }}</el-tag
  >
</template>
<style scoped>
.status-badge {
  gap: 6px;
  border: 0;
  padding: 3px 10px;
  height: 27px;
}
.status-dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  margin-right: 6px;
  vertical-align: middle;
}
</style>
