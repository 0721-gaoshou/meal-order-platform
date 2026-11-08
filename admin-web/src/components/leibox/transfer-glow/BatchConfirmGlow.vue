<script setup lang="ts">
import { computed } from 'vue'
import TransferGlow from './TransferGlow.vue'
import type { TransferGlowOptions } from './TransferGlowEngine.js'

/**
 * 批量操作确认弹层：LeiBox transfer-glow 的业务包装
 * 确认(点击"确认执行"→emit confirm) → 光雾处理动效(与后端请求并行) → 成功态(emit complete)
 * 源组件为支付场景演示，文案已在 TransferGlowEngine.js 内适配为中文批量操作语境（保留署名）
 */
const props = defineProps<{
  open: boolean
  /** 参与批量操作的商品数 */
  count: number
  /** 操作名，如「上架」/「下架」 */
  action: string
  /** 目标状态值，如 ON_SALE / OFF_SALE */
  target: string
}>()

const emit = defineEmits<{
  confirm: []
  complete: []
  close: []
}>()

const options = computed<TransferGlowOptions>(() => ({
  open: props.open,
  amount: String(props.count),
  account: '商品列表',
  recipient: props.action,
  receivedAmount: props.target,
  duration: 4200,
  onConfirm: () => emit('confirm'),
  onComplete: () => emit('complete'),
  onOpenChange: (value) => {
    if (!value) emit('close')
  },
}))
</script>

<template>
  <div v-if="props.open" class="batch-glow-overlay">
    <TransferGlow :options="options" />
  </div>
</template>

<style scoped>
.batch-glow-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  overflow: auto;
  background: rgba(17, 18, 20, 0.92);
}
</style>
