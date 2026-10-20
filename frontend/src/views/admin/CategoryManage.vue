<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>分类管理</span>
        <el-button type="primary" @click="openCreate">新建分类</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-select v-model="type" placeholder="类型" clearable class="select" @change="load(1)">
        <el-option label="活动 ACTIVITY" value="ACTIVITY" />
        <el-option label="套餐 PACKAGE" value="PACKAGE" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable class="select" @change="load(1)">
        <el-option label="启用" :value="true" />
        <el-option label="停用" :value="false" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="items" empty-text="暂无分类">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ row.categoryType === 'ACTIVITY' ? '活动' : '套餐' }}</template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status"
            @change="(v: boolean | string | number) => toggleStatus(row, v as boolean)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next, total"
      class="pager"
      @current-change="load()"
    />

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑分类' : '新建分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item v-if="!editing" label="类型">
          <el-select v-model="form.categoryType">
            <el-option label="活动 ACTIVITY" value="ACTIVITY" />
            <el-option label="套餐 PACKAGE" value="PACKAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCategory,
  deleteCategory,
  pageAdminCategories,
  setCategoryStatus,
  updateCategory,
} from '@/api/category'
import type { CategoryView } from '@/api/types'

const type = ref('')
const status = ref<boolean | undefined>(undefined)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const items = ref<CategoryView[]>([])
const loading = ref(false)
const saving = ref(false)

const dialogVisible = ref(false)
const editing = ref(false)
const form = reactive({ id: 0, categoryType: 'ACTIVITY', name: '', sortOrder: 0 })

async function load(toPage?: number) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const res = await pageAdminCategories({
      type: type.value || undefined,
      status: status.value,
      page: page.value,
      pageSize,
    })
    items.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, categoryType: 'ACTIVITY', name: '', sortOrder: 0 })
  dialogVisible.value = true
}

function openEdit(row: CategoryView) {
  editing.value = true
  Object.assign(form, { id: row.id, categoryType: row.categoryType, name: row.name, sortOrder: row.sortOrder })
  dialogVisible.value = true
}

async function submit() {
  saving.value = true
  try {
    if (editing.value) {
      await updateCategory(form.id, { name: form.name, sortOrder: form.sortOrder })
    } else {
      await createCategory({ categoryType: form.categoryType, name: form.name, sortOrder: form.sortOrder })
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: CategoryView, value: boolean) {
  await setCategoryStatus(row.id, value)
  ElMessage.success(value ? '已启用' : '已停用')
  load()
}

async function remove(row: CategoryView) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.name}」？分类下有活动/套餐时无法删除`, '提示', {
      type: 'warning',
    })
  } catch {
    return
  }
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
.select {
  width: 200px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
