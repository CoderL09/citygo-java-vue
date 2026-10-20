<template>
  <div v-loading="loading">
    <el-page-header content="套餐详情" @back="$router.push('/packages')" />
    <el-result v-if="error" icon="error" title="套餐不可用或加载失败"><template #extra><el-button @click="load">重试</el-button></template></el-result>
    <el-card v-if="detail && !error" style="margin-top:20px">
      <h2>{{detail.product.name}}</h2><p>{{detail.product.description}}</p>
      <p>每份 <strong>¥{{formatPrice(detail.product.packagePriceCents)}}</strong> · 版本 {{detail.product.version}}</p>
      <el-alert title="每份套餐占用以下每个场次各 1 人名额，不支持替换场次。" type="info" :closable="false" />
      <div v-for="s in detail.sessions" :key="s.id" class="session">
        <strong>{{detail.composition.find(c=>c.sessionId===s.id)?.activityName}} · 场次 #{{s.id}}</strong><p>{{detail.composition.find(c=>c.sessionId===s.id)?.address}}</p><p>{{formatDateTime(s.startTime)}} 至 {{formatDateTime(s.endTime)}}</p><p>预约截止 {{formatDateTime(s.bookingDeadline)}} · 剩余 {{s.available}} 人</p>
      </div>
      <el-input-number v-model="quantity" :min="1" :max="5" />
      <el-button type="primary" :loading="saving" @click="add">加入购物车</el-button>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPackage, addCartItem, type PackageDetail } from '@/api/booking'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, formatPrice } from '@/utils/format'
const route=useRoute(),router=useRouter(),auth=useAuthStore(),detail=ref<PackageDetail>(),quantity=ref(1),loading=ref(false),error=ref(false),saving=ref(false)
async function load(){loading.value=true;error.value=false;try{detail.value=await getPackage(Number(route.params.id))}catch{error.value=true}finally{loading.value=false}}
async function add(){if(!auth.user){router.push({path:'/login',query:{redirect:route.fullPath}});return}if(auth.user.role!=='USER'){ElMessage.warning('请使用用户账号预约');return}if(saving.value)return;saving.value=true;try{await addCartItem({itemType:'PACKAGE',itemId:Number(route.params.id),quantity:quantity.value});ElMessage.success('已加入购物车')}finally{saving.value=false}}
onMounted(load)
</script>
<style scoped>.session{padding:16px 0;border-bottom:1px solid var(--el-border-color-light);margin-bottom:16px}p{line-height:1.7}</style>
