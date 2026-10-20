<script setup lang="ts">
import { computed, onMounted, onUnmounted, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { getAcceptingOrders, setAcceptingOrders } from '@/api/file'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore(),
  accepting = shallowRef<boolean>(),
  loading = shallowRef(false),
  error = shallowRef(false)
const isAdminRole = computed(() => auth.user?.role === 'ADMIN')
let epoch = 0
async function load() {
  const run = ++epoch
  loading.value = true
  error.value = false
  try {
    const res = await getAcceptingOrders()
    if (run === epoch) accepting.value = res.acceptingOrders
  } catch {
    if (run === epoch) error.value = true
  } finally {
    if (run === epoch) loading.value = false
  }
}
async function toggle(value: string | number | boolean) {
  if (!isAdminRole.value || loading.value || accepting.value === undefined) return
  const old = accepting.value
  loading.value = true
  try {
    const r = await setAcceptingOrders(Boolean(value))
    accepting.value = r.acceptingOrders
    ElMessage.success(r.acceptingOrders ? '已开放接单' : '已暂停接单')
  } catch {
    accepting.value = old
  } finally {
    loading.value = false
  }
}
onMounted(load)
onUnmounted(() => epoch++)
</script>
<template>
  <section class="platform-status">
    <div class="platform-heading">
      <span class="availability-dot" :class="{ open: accepting === true }" aria-hidden="true" />
      <div>
        <h3>平台接单状态</h3>
        <p>
          {{
            error
              ? '状态加载失败'
              : loading
                ? '正在更新接单状态'
                : accepting === true
                  ? '开放接单，准备迎接下一次出发'
                  : '暂停接单，已有预约仍可继续处理'
          }}
        </p>
      </div>
    </div>
    <el-button v-if="error" text @click="load">重试</el-button
    ><el-switch
      v-else
      :model-value="accepting === true"
      :loading="loading"
      :disabled="!isAdminRole || loading || accepting === undefined"
      aria-label="平台接单开关"
      @change="toggle"
    />
  </section>
</template>
<style scoped>
.platform-status {
  background: #fff;
  border: 1px solid var(--city-line);
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 20px 24px;
  margin-top: 23px;
}
.platform-heading {
  display: flex;
  gap: 13px;
  align-items: center;
}
.availability-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #aaa;
  flex-shrink: 0;
}
.availability-dot.open {
  background: #53905d;
  box-shadow: 0 0 0 5px #edf4e9;
}
.platform-heading h3 {
  margin-bottom: 4px;
  font-size: 12px;
}
.platform-heading p {
  margin: 0;
  font-size: 10px;
  color: #8b978d;
}
@media (max-width: 600px) {
  .platform-status {
    padding: 19px;
  }
  .platform-heading {
    gap: 12px;
  }
  .platform-heading p {
    line-height: 1.6;
  }
}
</style>
