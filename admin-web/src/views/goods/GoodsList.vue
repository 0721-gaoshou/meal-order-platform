<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  batchChangeStatus,
  createSpu,
  deleteSpu,
  listCategories,
  pageSpus,
  updateSpu,
} from '@/api/goods'
import type { Category, SpuVO } from '@/api/goods'
import { uploadImage } from '@/api/upload'
import BatchConfirmGlow from '@/components/leibox/transfer-glow/BatchConfirmGlow.vue'

// ---------------- 列表与查询 ----------------

const list = ref<SpuVO[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', categoryId: '', status: '' })
const pageNum = ref(1)
const pageSize = ref(10)

async function fetchPage() {
  loading.value = true
  try {
    const result = await pageSpus({
      keyword: query.keyword.trim() || undefined,
      categoryId: query.categoryId || undefined,
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
  query.keyword = ''
  query.categoryId = ''
  query.status = ''
  pageNum.value = 1
  void fetchPage()
}

function priceText(row: SpuVO): string {
  if (row.skus.length === 0) return '-'
  const prices = row.skus.map((sku) => sku.price)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const fmt = (fen: number) => `¥${(fen / 100).toFixed(2)}`
  return min === max ? fmt(min) : `${fmt(min)} ~ ${fmt(max)}`
}

function stockText(row: SpuVO): number {
  return row.skus.reduce((sum, sku) => sum + sku.stock, 0)
}

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

// ---------------- 批量勾选（主交互：Staggered Bulk Selection） ----------------

const selectedIds = ref<string[]>([])
/** 每次"全选/取消全选"自增，作为 key 重新挂载勾选动画，错峰重放 */
const staggerTick = ref(0)

const allSelected = computed(
  () => list.value.length > 0 && list.value.every((row) => selectedIds.value.includes(row.id)),
)
const partialSelected = computed(
  () => selectedIds.value.length > 0 && !allSelected.value,
)

function toggleRow(row: SpuVO) {
  const index = selectedIds.value.indexOf(row.id)
  if (index >= 0) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(row.id)
  }
}

function toggleAll(checked: boolean | string | number) {
  // 状态立即更新，视觉反馈（错峰弹入）不阻塞后续操作
  selectedIds.value = checked ? list.value.map((row) => row.id) : []
  staggerTick.value++
}

function clearSelection() {
  selectedIds.value = []
}

// ---------------- 批量上下架 + LeiBox 确认弹层 ----------------

const glow = reactive({ open: false, count: 0, action: '', target: '' })
let pendingOnSale = false
/** 确认执行时发出的后端请求（与光雾动效并行），完成回调里收结果 */
let pendingApi: Promise<boolean> | null = null

function openBatch(onSale: boolean) {
  if (selectedIds.value.length === 0) return
  pendingOnSale = onSale
  glow.count = selectedIds.value.length
  glow.action = onSale ? '上架' : '下架'
  glow.target = onSale ? 'ON_SALE' : 'OFF_SALE'
  glow.open = true
}

async function onGlowConfirm() {
  const apiPromise = batchChangeStatus([...selectedIds.value], pendingOnSale)
  pendingApi = apiPromise.then(
    () => true,
    () => false,
  )
  const ok = await pendingApi
  // 请求失败：立刻收起弹层；成功则等动效走完在 onComplete 里统一收尾
  if (!ok) {
    glow.open = false
    pendingApi = null
    ElMessage.error('批量操作失败')
  }
}

async function onGlowComplete() {
  glow.open = false
  const ok = (await pendingApi) ?? false
  pendingApi = null
  if (ok) {
    ElMessage.success(`批量${glow.action}成功`)
    clearSelection()
    await fetchPage()
  }
}

function onGlowClose() {
  glow.open = false
  pendingApi = null
}

// ---------------- 单行操作 ----------------

async function toggleSingle(row: SpuVO) {
  const onSale = row.status !== 'ON_SALE'
  await batchChangeStatus([row.id], onSale)
  ElMessage.success(onSale ? '已上架' : '已下架')
  await fetchPage()
}

async function handleDelete(row: SpuVO) {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」？删除后不可恢复显示。`, '删除确认', {
    type: 'warning',
  })
  await deleteSpu(row.id)
  ElMessage.success('已删除')
  selectedIds.value = selectedIds.value.filter((id) => id !== row.id)
  await fetchPage()
}

// ---------------- 新建/编辑抽屉 ----------------

interface SkuFormRow {
  spec: string
  priceYuan: number
  stock: number
}

const drawerVisible = ref(false)
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  categoryId: '',
  mainImage: '',
  tags: '',
  description: '',
  skus: [{ spec: '', priceYuan: 1, stock: 0 }] as SkuFormRow[],
})
const rules: FormRules = {
  name: [
    { required: true, message: '请输入商品名', trigger: 'blur' },
    { max: 128, message: '商品名最长 128 字', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    categoryId: '',
    mainImage: '',
    tags: '',
    description: '',
    skus: [{ spec: '', priceYuan: 1, stock: 0 }],
  })
  imagePop.value = 0
  drawerVisible.value = true
}

function openEdit(row: SpuVO) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    categoryId: row.categoryId,
    mainImage: row.mainImage,
    tags: row.tags,
    description: row.description,
    skus: row.skus.map((sku) => ({
      spec: sku.spec,
      priceYuan: sku.price / 100,
      stock: sku.stock,
    })),
  })
  imagePop.value = 0
  drawerVisible.value = true
}

function addSkuRow() {
  form.skus.push({ spec: '', priceYuan: 1, stock: 0 })
}

function removeSkuRow(index: number) {
  form.skus.splice(index, 1)
}

async function submitForm() {
  await formRef.value?.validate()
  if (form.skus.length === 0) {
    ElMessage.warning('至少需要一个 SKU 规格')
    return
  }
  for (const [index, sku] of form.skus.entries()) {
    if (!sku.spec.trim()) {
      ElMessage.warning(`第 ${index + 1} 个规格名不能为空`)
      return
    }
    if (sku.priceYuan <= 0) {
      ElMessage.warning(`第 ${index + 1} 个规格价格必须大于 0`)
      return
    }
    if (sku.stock < 0) {
      ElMessage.warning(`第 ${index + 1} 个规格库存不能为负`)
      return
    }
  }
  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      categoryId: form.categoryId,
      mainImage: form.mainImage || undefined,
      tags: form.tags.trim() || undefined,
      description: form.description.trim() || undefined,
      skus: form.skus.map((sku) => ({
        spec: sku.spec.trim(),
        price: Math.round(sku.priceYuan * 100),
        stock: Math.floor(sku.stock),
      })),
    }
    if (editingId.value) {
      await updateSpu(editingId.value, payload)
      ElMessage.success('商品已更新')
    } else {
      await createSpu(payload)
      ElMessage.success('商品已创建（默认下架，可批量上架）')
    }
    drawerVisible.value = false
    await fetchPage()
  } finally {
    submitting.value = false
  }
}

// ---------------- 主图上传（上传中 loading → 成功缩略图弹入） ----------------

const uploading = ref(false)
/** 上传成功时自增，重放缩略图弹入动画 */
const imagePop = ref(0)

function beforeImageUpload(file: File): boolean {
  const okType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!okType) {
    ElMessage.error('仅支持 jpg/png/webp 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

async function doUpload(options: { file: File }) {
  uploading.value = true
  try {
    const result = await uploadImage(options.file)
    form.mainImage = result.data.url
    imagePop.value++
    ElMessage.success('图片上传成功')
  } finally {
    uploading.value = false
  }
}

// ---------------- 初始化 ----------------

onMounted(async () => {
  await Promise.all([fetchPage(), loadCategories()])
})

async function loadCategories() {
  const result = await listCategories()
  categories.value = result.data
}
</script>

<template>
  <div class="goods-page">
    <!-- 搜索与批量操作 -->
    <el-card shadow="never" class="toolbar">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="商品名"
          clearable
          class="w-200"
          @keyup.enter="() => { pageNum = 1; void fetchPage() }"
        />
        <el-select v-model="query.categoryId" placeholder="分类" clearable class="w-160">
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable class="w-140">
          <el-option label="在售" value="ON_SALE" />
          <el-option label="下架" value="OFF_SALE" />
        </el-select>
        <el-button type="primary" @click="() => { pageNum = 1; void fetchPage() }">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <transition name="bar">
      <div v-if="selectedIds.length > 0" class="batch-bar">
        <span class="count">已选 {{ selectedIds.length }} 件</span>
        <el-button size="small" type="success" @click="openBatch(true)">批量上架</el-button>
        <el-button size="small" type="warning" @click="openBatch(false)">批量下架</el-button>
        <el-button size="small" link @click="clearSelection">取消选择</el-button>
      </div>
    </transition>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>商品列表</span>
          <el-button type="primary" @click="openCreate">新建商品</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id" class="goods-table">
        <el-table-column width="52" align="center">
          <template #header>
            <el-checkbox
              :model-value="allSelected"
              :indeterminate="partialSelected"
              aria-label="全选"
              @change="toggleAll"
            />
          </template>
          <template #default="{ row, $index }">
            <!-- Staggered Bulk Selection：全选时勾选从第一行起错峰弹入（40ms/行） -->
            <span
              :key="staggerTick"
              class="check-cell check-pop"
              :style="{ animationDelay: `${$index * 40}ms` }"
            >
              <el-checkbox
                :model-value="selectedIds.includes(row.id)"
                :aria-label="`选择 ${row.name}`"
                @change="() => toggleRow(row)"
              />
            </span>
          </template>
        </el-table-column>

        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="sku-panel">
              <el-table :data="row.skus" size="small" border>
                <el-table-column prop="spec" label="规格" min-width="140" />
                <el-table-column label="价格">
                  <template #default="scope">¥{{ (scope.row.price / 100).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="stock" label="库存" width="100" />
                <el-table-column label="SKU ID" min-width="180">
                  <template #default="scope">
                    <span class="sku-id">{{ scope.row.id }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="主图" width="84">
          <template #default="{ row }">
            <el-image
              v-if="row.mainImage"
              :src="row.mainImage"
              :preview-src-list="[row.mainImage]"
              preview-teleported
              fit="cover"
              class="thumb"
            />
            <span v-else class="no-image">无图</span>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="商品名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column label="价格" width="140">
          <template #default="{ row }">{{ priceText(row) }}</template>
        </el-table-column>
        <el-table-column label="库存" width="90">
          <template #default="{ row }">{{ stockText(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ON_SALE' ? '在售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'ON_SALE' ? 'warning' : 'success'" @click="toggleSingle(row)">
              {{ row.status === 'ON_SALE' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新建/编辑抽屉 -->
    <el-drawer v-model="drawerVisible" :title="editingId ? '编辑商品' : '新建商品'" size="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品名" prop="name">
          <el-input v-model="form.name" maxlength="128" placeholder="如：经典芝士牛肉堡" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" class="w-100p">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主图">
          <div class="upload-wrap">
            <el-upload
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp"
              :before-upload="beforeImageUpload"
              :http-request="doUpload"
            >
              <img
                v-if="form.mainImage"
                :key="imagePop"
                :src="form.mainImage"
                class="main-image image-pop"
                alt="商品主图"
              />
              <div v-else v-loading="uploading" class="upload-placeholder">
                <el-icon><Plus /></el-icon>
                <span>{{ uploading ? '上传中…' : '上传主图' }}</span>
              </div>
            </el-upload>
            <span class="upload-tip">jpg/png/webp，≤5MB，上传后自动填充</span>
          </div>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：招牌,微辣（AI 推荐预留字段）" />
        </el-form-item>
        <el-form-item label="详情">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>

        <el-form-item label="SKU 规格">
          <div class="sku-rows">
            <div v-for="(sku, index) in form.skus" :key="index" class="sku-row">
              <el-input v-model="sku.spec" placeholder="规格名，如：大份" class="w-160" />
              <el-input-number v-model="sku.priceYuan" :min="0.01" :precision="2" :step="0.5" controls-position="right" class="w-140" />
              <el-input-number v-model="sku.stock" :min="0" :step="1" :precision="0" controls-position="right" class="w-120" />
              <el-button link type="danger" :disabled="form.skus.length <= 1" @click="removeSkuRow(index)">删除</el-button>
            </div>
            <el-button link type="primary" @click="addSkuRow">+ 添加规格</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <!-- LeiBox transfer-glow：批量上下架确认弹层 -->
    <BatchConfirmGlow
      :open="glow.open"
      :count="glow.count"
      :action="glow.action"
      :target="glow.target"
      @confirm="onGlowConfirm"
      @complete="onGlowComplete"
      @close="onGlowClose"
    />
  </div>
</template>

<style scoped>
.goods-page {
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

.w-200 { width: 200px; }
.w-160 { width: 160px; }
.w-140 { width: 140px; }
.w-120 { width: 120px; }
.w-100p { width: 100%; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 批量操作条：弹入 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
}

.batch-bar .count {
  font-weight: 600;
  color: #409eff;
  margin-right: 6px;
}

.bar-enter-active {
  transition: all 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.bar-leave-active {
  transition: all 0.18s ease;
}

.bar-enter-from,
.bar-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 勾选错峰弹入（弹性过冲）；reduced-motion 直接显示 */
.check-pop {
  display: inline-flex;
  animation: check-pop 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes check-pop {
  0% { transform: scale(0.7); opacity: 0.2; }
  60% { transform: scale(1.12); }
  100% { transform: scale(1); opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .check-pop {
    animation: none;
  }
}

.thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  display: block;
}

.no-image {
  color: #c0c4cc;
  font-size: 12px;
}

.sku-panel {
  padding: 8px 24px 12px 56px;
}

.sku-id {
  color: #909399;
  font-size: 12px;
  font-family: Consolas, monospace;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 抽屉表单 */
.upload-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.main-image {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
  cursor: pointer;
}

.image-pop {
  animation: image-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes image-pop {
  0% { transform: scale(0.6); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .image-pop {
    animation: none;
  }
}

.upload-placeholder {
  width: 96px;
  height: 96px;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
}

.sku-rows {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sku-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
