<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>活动管理</span>
        <el-button type="primary" @click="openCreate">新建活动</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-select v-model="categoryId" placeholder="分类" clearable class="select" @change="load(1)">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable class="select" @change="load(1)">
        <el-option label="在售" value="ON_SALE" />
        <el-option label="已下架" value="OFF_SALE" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索名称/地址"
        clearable
        class="search"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button @click="load(1)">搜索</el-button>
    </div>

    <el-table v-loading="loading" :data="items" empty-text="暂无活动">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column prop="address" label="地址" min-width="150" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'">
            {{ row.status === 'ON_SALE' ? '在售' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openSessions(row)">场次/图片</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" @click="toggleStatus(row)">
            {{ row.status === 'ON_SALE' ? '下架' : '上架' }}
          </el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑活动' : '新建活动'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" class="full">
            <el-option v-for="c in enabledCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="介绍">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="sessionDrawer" :title="current?.name || '场次管理'" size="720px">
      <el-tabs>
        <el-tab-pane label="场次">
          <div class="sec-head">
            <el-button type="primary" size="small" @click="openSessionCreate">新增场次</el-button>
          </div>
          <el-table v-loading="sessionLoading" :data="sessions" empty-text="暂无场次">
            <el-table-column label="开始时间" min-width="140">
              <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" min-width="140">
              <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="预约截止" min-width="140">
              <template #default="{ row }">{{ formatDateTime(row.bookingDeadline) }}</template>
            </el-table-column>
            <el-table-column label="价格(元)" width="90">
              <template #default="{ row }">{{ formatPrice(row.priceCents) }}</template>
            </el-table-column>
            <el-table-column label="名额" width="90">
              <template #default="{ row }">{{ row.available }}/{{ row.capacity }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ON_SALE' ? '在售' : '已下架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openSessionEdit(row)">编辑</el-button>
                <el-button size="small" @click="toggleSession(row)">
                  {{ row.status === 'ON_SALE' ? '下架' : '上架' }}
                </el-button>
                <el-button size="small" type="danger" plain @click="removeSession(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="图片">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="doUpload"
            accept=".jpg,.jpeg,.png,.webp"
          >
            <el-button type="primary" size="small">上传图片</el-button>
          </el-upload>
          <div class="images">
            <div v-for="img in images" :key="img.id" class="img-item">
              <img :src="img.url" class="thumb" alt="图片" />
              <el-button size="small" type="danger" plain @click="removeImage(img.id)">移除</el-button>
            </div>
            <el-empty v-if="images.length === 0" description="暂无图片" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-dialog v-model="sessionDialog" :title="sessionEditing ? '编辑场次' : '新增场次'" width="480px">
      <el-form :model="sessionForm" label-width="100px">
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="sessionForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="sessionForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full"
          />
        </el-form-item>
        <el-form-item label="预约截止">
          <el-date-picker
            v-model="sessionForm.bookingDeadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full"
          />
        </el-form-item>
        <el-form-item label="价格（元）">
          <el-input-number v-model="sessionForm.priceYuan" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="总名额">
          <el-input-number v-model="sessionForm.capacity" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sessionDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSession">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import {
  addActivityImage,
  createActivity,
  createSession,
  deleteActivity,
  deleteActivityImage,
  deleteSession,
  getAdminActivity,
  listAdminSessions,
  pageAdminActivities,
  setActivityStatus,
  setSessionStatus,
  updateActivity,
  updateSession,
} from '@/api/activity'
import { pageAdminCategories } from '@/api/category'
import { uploadFile } from '@/api/file'
import type { ActivityImageView, ActivityView, CategoryView, SessionView } from '@/api/types'
import { formatDateTime, formatPrice, toUtcDateTime, toShanghaiInput } from '@/utils/format'

const categories = ref<CategoryView[]>([])
const enabledCategories = computed(() => categories.value.filter((c) => c.status))

const categoryId = ref<number | undefined>(undefined)
const status = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const items = ref<ActivityView[]>([])
const loading = ref(false)
const saving = ref(false)

const dialogVisible = ref(false)
const editing = ref(false)
const form = reactive({ id: 0, categoryId: undefined as number | undefined, name: '', address: '', description: '' })

const sessionDrawer = ref(false)
const current = ref<ActivityView | null>(null)
const sessions = ref<SessionView[]>([])
const images = ref<ActivityImageView[]>([])
const sessionLoading = ref(false)

const sessionDialog = ref(false)
const sessionEditing = ref(false)
const sessionForm = reactive({
  id: 0,
  startTime: '',
  endTime: '',
  bookingDeadline: '',
  priceYuan: 0,
  capacity: 10,
})

async function loadCategories() {
  const res = await pageAdminCategories({ type: 'ACTIVITY', page: 1, pageSize: 100 })
  categories.value = res.items
}

async function load(toPage?: number) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const res = await pageAdminActivities({
      categoryId: categoryId.value,
      status: status.value || undefined,
      keyword: keyword.value || undefined,
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
  Object.assign(form, { id: 0, categoryId: undefined, name: '', address: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row: ActivityView) {
  editing.value = true
  Object.assign(form, {
    id: row.id,
    categoryId: row.categoryId,
    name: row.name,
    address: row.address,
    description: row.description ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.categoryId || !form.name || !form.address) {
    ElMessage.warning('请填写分类、名称和地址')
    return
  }
  saving.value = true
  try {
    const payload = {
      categoryId: form.categoryId,
      name: form.name,
      address: form.address,
      description: form.description || undefined,
    }
    if (editing.value) {
      await updateActivity(form.id, payload)
    } else {
      await createActivity(payload)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: ActivityView) {
  const next = row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  await setActivityStatus(row.id, next)
  ElMessage.success(next === 'ON_SALE' ? '已上架' : '已下架')
  load()
}

async function remove(row: ActivityView) {
  try {
    await ElMessageBox.confirm(
      `确认删除活动「${row.name}」？存在有效订单的场次时无法删除`,
      '提示',
      { type: 'warning' },
    )
  } catch {
    return
  }
  await deleteActivity(row.id)
  ElMessage.success('已删除')
  load()
}

async function openSessions(row: ActivityView) {
  current.value = row
  sessionDrawer.value = true
  await Promise.all([loadSessions(), loadImages()])
}

async function loadSessions() {
  if (!current.value) return
  sessionLoading.value = true
  try {
    sessions.value = await listAdminSessions(current.value.id)
  } finally {
    sessionLoading.value = false
  }
}

async function loadImages() {
  if (!current.value) return
  const detail = await getAdminActivity(current.value.id)
  images.value = detail.images
}

function openSessionCreate() {
  sessionEditing.value = false
  Object.assign(sessionForm, { id: 0, startTime: '', endTime: '', bookingDeadline: '', priceYuan: 0, capacity: 10 })
  sessionDialog.value = true
}

function openSessionEdit(row: SessionView) {
  sessionEditing.value = true
  Object.assign(sessionForm, {
    id: row.id,
    startTime: toShanghaiInput(row.startTime),
    endTime: toShanghaiInput(row.endTime),
    bookingDeadline: toShanghaiInput(row.bookingDeadline),
    priceYuan: row.priceCents / 100,
    capacity: row.capacity,
  })
  sessionDialog.value = true
}

async function submitSession() {
  if (!sessionForm.startTime || !sessionForm.endTime || !sessionForm.bookingDeadline) {
    ElMessage.warning('请填写场次时间')
    return
  }
  saving.value = true
  try {
    const payload = {
      startTime: toUtcDateTime(sessionForm.startTime),
      endTime: toUtcDateTime(sessionForm.endTime),
      bookingDeadline: toUtcDateTime(sessionForm.bookingDeadline),
      priceCents: Math.round(sessionForm.priceYuan * 100),
      capacity: sessionForm.capacity,
    }
    if (sessionEditing.value) {
      await updateSession(sessionForm.id, payload)
    } else if (current.value) {
      await createSession(current.value.id, payload)
    }
    ElMessage.success('已保存')
    sessionDialog.value = false
    loadSessions()
  } finally {
    saving.value = false
  }
}

async function toggleSession(row: SessionView) {
  const next = row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  await setSessionStatus(row.id, next)
  ElMessage.success(next === 'ON_SALE' ? '已上架' : '已下架')
  loadSessions()
}

async function removeSession(row: SessionView) {
  try {
    await ElMessageBox.confirm('确认删除该场次？存在有效订单时无法删除', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteSession(row.id)
  ElMessage.success('已删除')
  loadSessions()
}

function beforeUpload(file: File) {
  const ok = /\.(jpg|jpeg|png|webp)$/i.test(file.name) && file.size <= 5 * 1024 * 1024
  if (!ok) ElMessage.warning('仅支持 5MB 以内的 jpg/jpeg/png/webp 图片')
  return ok
}

async function doUpload(options: UploadRequestOptions) {
  const res = await uploadFile(options.file as File)
  if (current.value) {
    await addActivityImage(current.value.id, res.id, images.value.length)
  }
  ElMessage.success('上传成功')
  loadImages()
}

async function removeImage(imageId: number) {
  await deleteActivityImage(imageId)
  ElMessage.success('已移除')
  loadImages()
}

onMounted(() => {
  loadCategories()
  load(1)
})
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
  width: 170px;
}
.search {
  max-width: 220px;
}
.full {
  width: 100%;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.sec-head {
  margin-bottom: 12px;
}
.images {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}
.img-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.thumb {
  width: 140px;
  height: 100px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
}
</style>
