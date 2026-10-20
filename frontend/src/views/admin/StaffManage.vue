<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>员工管理</span>
        <el-button type="primary" @click="openCreate">新建员工</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名/昵称"
        clearable
        class="search"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button @click="load(1)">搜索</el-button>
    </div>

    <el-table v-loading="loading" :data="items" empty-text="暂无员工">
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'warning'">
            {{ row.role === 'ADMIN' ? '管理员' : '运营' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            @change="(v: boolean | string | number) => toggleEnabled(row, v as boolean)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" plain @click="openReset(row)">重置密码</el-button>
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

    <el-dialog v-model="createVisible" title="新建员工" width="420px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role">
            <el-option label="运营 OPERATOR" value="OPERATOR" />
            <el-option label="管理员 ADMIN" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑员工" width="420px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role">
            <el-option label="运营 OPERATOR" value="OPERATOR" />
            <el-option label="管理员 ADMIN" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="380px">
      <el-input v-model="resetPassword" type="password" show-password placeholder="新密码（至少 6 位）" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createStaff,
  pageStaff,
  resetStaffPassword,
  setStaffEnabled,
  updateStaff,
} from '@/api/staff'
import type { UserView } from '@/api/types'

const keyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const items = ref<UserView[]>([])
const loading = ref(false)
const saving = ref(false)

const createVisible = ref(false)
const createForm = reactive({ username: '', password: '', nickname: '', role: 'OPERATOR' as 'OPERATOR' | 'ADMIN' })

const editVisible = ref(false)
const editForm = reactive({ id: 0, nickname: '', role: 'OPERATOR' as 'OPERATOR' | 'ADMIN' })

const resetVisible = ref(false)
const resetId = ref(0)
const resetPassword = ref('')

async function load(toPage?: number) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const res = await pageStaff({ keyword: keyword.value || undefined, page: page.value, pageSize })
    items.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, { username: '', password: '', nickname: '', role: 'OPERATOR' })
  createVisible.value = true
}

async function submitCreate() {
  saving.value = true
  try {
    await createStaff({
      username: createForm.username,
      password: createForm.password,
      nickname: createForm.nickname || undefined,
      role: createForm.role,
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    load(1)
  } finally {
    saving.value = false
  }
}

function openEdit(row: UserView) {
  Object.assign(editForm, { id: row.id, nickname: row.nickname ?? '', role: row.role })
  editVisible.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    await updateStaff(editForm.id, { nickname: editForm.nickname || undefined, role: editForm.role })
    ElMessage.success('已保存')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row: UserView, value: boolean) {
  try {
    await ElMessageBox.confirm(
      value ? `确认启用员工「${row.username}」？` : `确认禁用员工「${row.username}」？禁用后其登录立即失效`,
      '提示',
      { type: 'warning' },
    )
  } catch {
    return
  }
  await setStaffEnabled(row.id, value)
  ElMessage.success('已更新')
  load()
}

function openReset(row: UserView) {
  resetId.value = row.id
  resetPassword.value = ''
  resetVisible.value = true
}

async function submitReset() {
  if (resetPassword.value.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  saving.value = true
  try {
    await resetStaffPassword(resetId.value, resetPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
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
.search {
  max-width: 240px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
