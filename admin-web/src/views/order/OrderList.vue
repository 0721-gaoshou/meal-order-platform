<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageOrders, transitionOrderStatus } from '@/api/order'
import type { OrderVO } from '@/api/order'

// ---------------- 状态与流转 ----------------

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

const STATUS_META: Record<string, { label: string; tag: TagType }> = {
  PENDING_PAY: { label: '待支付', tag: 'warning' },
  PAID: { label: '已支付', tag: 'primary' },
  MAKING: { label: '制作中', tag: 'primary' },
  READY: { label: '待取餐', tag: 'success' },
  COMPLETED: { label: '已完成', tag: 'success' },
  CLOSED: { label: '已取消', tag: 'info' },
  REFUNDING: { label: '退款中', tag: 'danger' },
  REFUNDED: { label: '已退款', tag: 'info' },
}

/** 与后端 NEXT_STATUS 一致：只允许逐步推进 */
const NEXT_ACTION: Record<string, { to: string; label: string }> = {
  PAID: { to: 'MAKING', label: '开始制作' },
  MAKING: { to: 'READY', label: '出餐完成' },
  READY: { to: 'COMPLETED', label: '确认取餐' },
}

function statusLabel(status: string): string {
  return STATUS_META[status]?.label ?? status
}

function statusTag(status: string): TagType {
  return STATUS_META[status]?.tag ?? 'info'
}

// ---------------- 列表与查询 ----------------

const list = ref<OrderVO[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ status: '' })
const pageNum = ref(1)
const pageSize = ref(10)

async function fetchPage() {
  loading.value = true
  try {
    const result = await pageOrders({
      status: query.status || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    list.value = result.data.records
    total.value = Number(result.data.total)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.status = ''
  pageNum.value = 1
  void fetchPage()
}

// ---------------- 展示辅助 ----------------

function yuan(fen: number): string {
  return `¥${(fen / 100).toFixed(2)}`
}

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function itemsSummary(row: OrderVO): string {
  if (row.items.length === 0) return '-'
  const first = row.items[0]
  const head = `${first.spuName} ×${first.quantity}`
  return row.items.length > 1 ? `${head} 等${row.items.length}种` : head
}

function refundText(refundStatus: string): string {
  return refundStatus === 'REFUNDED' ? '已退款' : '-'
}

// ---------------- 状态流转 ----------------

async function handleTransition(row: OrderVO) {
  const action = NEXT_ACTION[row.status]
  if (!action) return
  await ElMessageBox.confirm(
    `订单 ${row.orderNo} 推进到「${statusLabel(action.to)}」？`,
    '状态流转确认',
    { type: 'warning', confirmButtonText: '推进', cancelButtonText: '取消' },
  )
  await transitionOrderStatus(row.id, action.to)
  ElMessage.success(`已推进到「${statusLabel(action.to)}」`)
  await fetchPage()
}

onMounted(() => {
  void fetchPage()
})
</script>

<template>
  <div class="order-page">
    <el-card shadow="never" class="toolbar">
      <div class="filters">
        <el-select v-model="query.status" placeholder="状态" clearable class="w-160">
          <el-option
            v-for="(meta, status) in STATUS_META"
            :key="status"
            :label="meta.label"
            :value="status"
          />
        </el-select>
        <el-button type="primary" @click="() => { pageNum = 1; void fetchPage() }">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>订单列表</span>
          <span class="tip">仅已支付订单可推进：已支付 → 制作中 → 待取餐 → 已完成</span>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="item-panel">
              <el-table :data="row.items" size="small" border>
                <el-table-column prop="spuName" label="商品" min-width="160" />
                <el-table-column prop="skuSpec" label="规格" width="120" />
                <el-table-column prop="quantity" label="数量" width="80" />
                <el-table-column label="单价" width="110">
                  <template #default="scope">{{ yuan(scope.row.unitPrice) }}</template>
                </el-table-column>
                <el-table-column label="券分摊" width="110">
                  <template #default="scope">{{ yuan(scope.row.couponShareAmount) }}</template>
                </el-table-column>
                <el-table-column label="退款" width="100">
                  <template #default="scope">{{ refundText(scope.row.refundStatus) }}</template>
                </el-table-column>
              </el-table>
              <div class="order-meta">
                <span>订单号：{{ row.orderNo }}</span>
                <span>原价 {{ yuan(row.totalAmount) }}</span>
                <span>优惠 {{ yuan(row.discountAmount) }}</span>
                <span v-if="row.expireTime">支付截止 {{ formatTime(row.expireTime) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="订单号" width="190">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ itemsSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="实付" width="100">
          <template #default="{ row }">
            <div class="amount">{{ yuan(row.payAmount) }}</div>
            <div v-if="row.discountAmount > 0" class="discount">已优惠 {{ yuan(row.discountAmount) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" width="160">
          <template #default="{ row }">{{ formatTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="NEXT_ACTION[row.status]"
              link
              type="primary"
              @click="handleTransition(row)"
            >
              {{ NEXT_ACTION[row.status].label }}
            </el-button>
            <span v-else class="no-action">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="() => fetchPage()"
          @size-change="() => { pageNum = 1; void fetchPage() }"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.order-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar :deep(.el-card__body) {
  padding: 14px 16px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.w-160 { width: 160px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tip {
  color: #909399;
  font-size: 12px;
}

.order-no {
  font-family: Consolas, monospace;
  font-size: 13px;
}

.amount {
  font-weight: 600;
}

.discount {
  color: #909399;
  font-size: 12px;
}

.no-action {
  color: #c0c4cc;
}

.item-panel {
  padding: 8px 24px 12px 56px;
}

.order-meta {
  display: flex;
  gap: 24px;
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>