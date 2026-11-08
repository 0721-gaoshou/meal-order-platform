<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { health } from '@/api/health'
import type { HealthInfo } from '@/api/health'

const status = ref<'checking' | 'ok' | 'fail'>('checking')
const info = ref<HealthInfo | null>(null)

onMounted(async () => {
  try {
    const result = await health()
    info.value = result.data
    status.value = 'ok'
  } catch {
    status.value = 'fail'
  }
})
</script>

<template>
  <div>
    <el-alert
      v-if="status === 'ok'"
      type="success"
      :closable="false"
      show-icon
      :title="`后端连接正常（code 00000）· 服务：${info?.service} · 后端时间：${info?.time}`"
      description="前端 → Vite 代理 → Spring Boot → MySQL 容器整条链路已贯通（M0 验收项）。"
    />
    <el-alert
      v-else-if="status === 'fail'"
      type="error"
      :closable="false"
      show-icon
      title="后端不可达"
      description="请先启动后端：java -jar backend/target/meal-order-backend.jar（或在 IDEA 中直接运行主类）。"
    />
    <el-skeleton v-else :rows="3" animated />
  </div>
</template>
