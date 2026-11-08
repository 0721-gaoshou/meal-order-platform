<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Rank } from '@element-plus/icons-vue'
import {
  createCategory,
  deleteCategory,
  listCategories,
  sortCategories,
  updateCategory,
} from '@/api/goods'
import type { Category } from '@/api/goods'

const categories = ref<Category[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = ref<{ name: string; icon: string; status: number }>({ name: '', icon: '', status: 1 })
const rules: FormRules = {
  name: [
    { required: true, message: '请输入分类名', trigger: 'blur' },
    { max: 64, message: '分类名最长 64 字', trigger: 'blur' },
  ],
}

// ---------------- 拖拽排序（Drag-to-Reorder） ----------------
const dragIndex = ref(-1)
/** 拖拽开始时的快照，ESC 取消或提交失败时回滚 */
let snapshot: Category[] = []

function onDragStart(index: number) {
  dragIndex.value = index
  snapshot = [...categories.value]
}

function onDragOver(index: number, event: DragEvent) {
  event.preventDefault()
  event.dataTransfer && (event.dataTransfer.dropEffect = 'move')
  const from = dragIndex.value
  if (from < 0 || from === index) return
  // 每次悬停都重算落点并重排，其余行由 TransitionGroup 的 FLIP 追赶动画让位
  const list = categories.value
  const [moved] = list.splice(from, 1)
  list.splice(index, 0, moved)
  dragIndex.value = index
}

async function onDragEnd() {
  const from = dragIndex.value
  dragIndex.value = -1
  if (from < 0) return
  const changed = categories.value.some((item, i) => item.id !== snapshot[i]?.id)
  if (!changed) return
  await submitSort()
}

/** 键盘可访问：聚焦行 + Alt+↑/↓ 调序，调序即提交 */
async function onKeydown(index: number, event: KeyboardEvent) {
  if (!event.altKey) return
  if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return
  event.preventDefault()
  const target = event.key === 'ArrowUp' ? index - 1 : index + 1
  if (target < 0 || target >= categories.value.length) return
  const list = categories.value
  const [moved] = list.splice(index, 1)
  list.splice(target, 0, moved)
  await submitSort()
}

/** ESC 取消拖拽，回滚到快照 */
function onEscKey(event: KeyboardEvent) {
  if (event.key !== 'Escape' || dragIndex.value < 0) return
  categories.value = [...snapshot]
  dragIndex.value = -1
}

async function submitSort() {
  try {
    await sortCategories(categories.value.map((item) => item.id))
    ElMessage.success('排序已保存')
  } catch {
    // 拦截器已提示，回滚到拖拽前
    categories.value = [...snapshot]
  }
}

onMounted(() => {
  void load()
  window.addEventListener('keydown', onEscKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onEscKey)
})

// ---------------- CRUD ----------------

async function load() {
  loading.value = true
  try {
    const result = await listCategories()
    categories.value = result.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', icon: '', status: 1 }
  dialogVisible.value = true
}

function openEdit(row: Category) {
  editingId.value = row.id
  form.value = { name: row.name, icon: row.icon, status: row.status }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  const payload = {
    name: form.value.name.trim(),
    icon: form.value.icon.trim() || undefined,
    status: form.value.status,
  }
  if (editingId.value) {
    await updateCategory(editingId.value, payload)
    ElMessage.success('分类已更新')
  } else {
    await createCategory(payload)
    ElMessage.success('分类已创建')
  }
  dialogVisible.value = false
  await load()
}

async function handleDelete(row: Category) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '删除确认', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  await load()
}

async function toggleStatus(row: Category, status: number) {
  await updateCategory(row.id, { name: row.name, icon: row.icon || undefined, status })
  row.status = status
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>商品分类</span>
        <el-button type="primary" @click="openCreate">新增分类</el-button>
      </div>
    </template>

    <p class="tip">拖动行调整顺序（ESC 取消），松手后自动保存；也可聚焦行后用 Alt + ↑/↓ 调序。</p>

    <TransitionGroup tag="ul" name="cat" v-loading="loading" class="cat-list">
      <li
        v-for="(row, index) in categories"
        :key="row.id"
        class="cat-row"
        :class="{ dragging: dragIndex === index }"
        draggable="true"
        tabindex="0"
        @dragstart="onDragStart(index)"
        @dragover="onDragOver(index, $event)"
        @dragend="onDragEnd"
        @keydown="onKeydown(index, $event)"
      >
        <span class="handle"><el-icon><Rank /></el-icon></span>
        <span class="name">{{ row.name }}</span>
        <el-switch
          :model-value="row.status === 1"
          inline-prompt
          active-text="启用"
          inactive-text="停用"
          @change="(value: string | number | boolean) => toggleStatus(row, value ? 1 : 0)"
        />
        <div class="actions">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </div>
      </li>
      <li v-if="!loading && categories.length === 0" key="__empty" class="empty">暂无分类，点击右上角新增</li>
    </TransitionGroup>
  </el-card>

  <el-dialog
    v-model="dialogVisible"
    :title="editingId ? '编辑分类' : '新增分类'"
    width="440px"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="分类名" prop="name">
        <el-input v-model="form.name" maxlength="64" placeholder="如：热销推荐" />
      </el-form-item>
      <el-form-item label="图标 URL">
        <el-input v-model="form.icon" placeholder="选填，图片地址" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tip {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}

.cat-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.cat-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #fff;
  cursor: grab;
}

.cat-row:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 2px;
}

.cat-row.dragging {
  opacity: 0.45;
  cursor: grabbing;
}

/* FLIP 追赶动画：被让位的行平滑滑到新位置；reduced-motion 时直接落位 */
.cat-move {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

@media (prefers-reduced-motion: reduce) {
  .cat-move {
    transition: none;
  }
}

.handle {
  color: #909399;
  display: flex;
}

.name {
  flex: 1;
  font-weight: 500;
}

.actions {
  display: flex;
}

.empty {
  text-align: center;
  color: #909399;
  padding: 32px 0;
  cursor: default;
}
</style>
