<template>
  <el-card v-loading="loading">
    <template #header><div class="bar"><span>套餐管理</span><el-button type="primary" @click="open()">新建套餐</el-button></div></template>
    <div class="toolbar"><el-input v-model="keyword" placeholder="搜索套餐名称" clearable @keyup.enter="load(1)" @clear="load(1)" /><el-button @click="load(1)">搜索</el-button></div>
    <el-alert v-if="error" title="套餐或可选场次加载失败" type="error" :closable="false"><el-button text @click="initialize">重试</el-button></el-alert>
    <el-table :data="items" empty-text="暂无套餐">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="套餐名称" min-width="160" />
      <el-table-column label="版本" width="80"><template #default="{row}">v{{row.version}}</template></el-table-column>
      <el-table-column label="套餐价（元）" width="130"><template #default="{row}">{{formatPrice(row.packagePriceCents)}}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.status==='ON_SALE'?'success':'info'">{{row.status==='ON_SALE'?'在售':'已下架'}}</el-tag></template></el-table-column>
      <el-table-column label="操作" min-width="260"><template #default="{row}"><el-button size="small" @click="show(row.id)">查看组成</el-button><el-button size="small" @click="open(row.id)">创建新版本</el-button><el-button size="small" :disabled="saving" @click="toggle(row)">{{row.status==='ON_SALE'?'下架':'上架'}}</el-button></template></el-table-column>
    </el-table>
    <el-pagination :current-page="page" :page-size="10" :total="total" layout="prev,pager,next,total" @current-change="load" />
    <el-dialog v-model="visible" :title="source ? '创建套餐新版本' : '新建套餐'" width="min(650px, 94vw)">
      <el-alert title="保存为下架状态，核对后可上架。价格与组成保留在当前版本中，调整时另建新版本。" type="info" :closable="false" />
      <el-form label-position="top" style="margin-top:16px">
        <el-form-item label="套餐分类"><el-select v-model="form.categoryId" style="width:100%"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="套餐名称"><el-input v-model="form.name" maxlength="128" /></el-form-item>
        <el-form-item label="套餐介绍"><el-input v-model="form.description" type="textarea" maxlength="10000" /></el-form-item>
        <el-form-item label="套餐价格（元）"><el-input-number v-model="form.priceYuan" :min="0" :max="21474836.47" :precision="2" /></el-form-item>
        <el-form-item label="固定组成场次（最多 10 个）"><el-select v-model="form.sessionIds" multiple filterable style="width:100%" placeholder="选择固定场次"><el-option v-for="s in options" :key="s.id" :value="s.id" :label="s.label" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存套餐</el-button></template>
    </el-dialog>
    <el-dialog v-model="detailVisible" title="套餐组成" width="min(600px, 94vw)">
      <template v-if="detail"><h3>{{detail.product.name}} · v{{detail.product.version}}</h3><p>每份 ¥{{formatPrice(detail.product.packagePriceCents)}}</p><p v-for="s in detail.sessions" :key="s.id">场次 #{{s.id}} · {{formatDateTime(s.startTime)}} · 剩余 {{s.available}} 人</p></template>
    </el-dialog>
  </el-card>
</template>
<script setup lang="ts">
import { onMounted,reactive,ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAdminCategories } from '@/api/category'
import { pageAdminActivities,listAdminSessions } from '@/api/activity'
import { listPackages,getPackage,savePackage,setPackageStatus,type Package,type PackageDetail } from '@/api/booking'
import type { CategoryView } from '@/api/types'
import { formatDateTime,formatPrice } from '@/utils/format'
const loading=ref(false),saving=ref(false),error=ref(false),items=ref<Package[]>([]),categories=ref<CategoryView[]>([]),options=ref<{id:number;label:string}[]>([])
const keyword=ref(''),page=ref(1),total=ref(0),visible=ref(false),source=ref<number>(),detailVisible=ref(false),detail=ref<PackageDetail>()
const form=reactive({categoryId:undefined as number|undefined,name:'',description:'',priceYuan:0,sessionIds:[] as number[]})
async function load(p=page.value){page.value=p;loading.value=true;error.value=false;try{const res=await listPackages(true,p,keyword.value);items.value=res.items;total.value=res.total}catch{error.value=true}finally{loading.value=false}}
async function initialize(){await load();try{
  const cats=await pageAdminCategories({type:'PACKAGE',status:true,page:1,pageSize:100});categories.value=cats.items
  const all=[] as {id:number;label:string}[]
  let p=1,totalActivities=0
  do{const activities=await pageAdminActivities({page:p,pageSize:100});totalActivities=activities.total
    const groups=await Promise.all(activities.items.map(async a=>(await listAdminSessions(a.id)).map(s=>({id:s.id,label:a.name+' · '+formatDateTime(s.startTime)+' · #'+s.id}))))
    all.push(...groups.flat());p++
  }while((p-1)*100<totalActivities)
  options.value=all
}catch{error.value=true}}
async function open(id?:number){source.value=id;if(id){const d=await getPackage(id,true);Object.assign(form,{categoryId:d.product.categoryId,name:d.product.name,description:d.product.description||'',priceYuan:d.product.packagePriceCents/100,sessionIds:d.sessions.map(s=>s.id)})}else Object.assign(form,{categoryId:undefined,name:'',description:'',priceYuan:0,sessionIds:[]});visible.value=true}
async function save(){if(!form.categoryId||!form.name.trim()||!form.sessionIds.length||form.sessionIds.length>10){ElMessage.warning('请填写分类、名称并选择 1 至 10 个场次');return}if(saving.value)return;saving.value=true;try{await savePackage({categoryId:form.categoryId,name:form.name,description:form.description,packagePriceCents:Math.round(form.priceYuan*100),sessionIds:form.sessionIds},source.value);visible.value=false;ElMessage.success('套餐已保存，请核对后上架');await load()}finally{saving.value=false}}
async function toggle(p:Package){saving.value=true;try{await setPackageStatus(p.id,p.status==='ON_SALE'?'OFF_SALE':'ON_SALE');ElMessage.success('状态已更新');await load()}finally{saving.value=false}}
async function show(id:number){detail.value=await getPackage(id,true);detailVisible.value=true}
onMounted(initialize)
</script>
<style scoped>.bar{display:flex;align-items:center;justify-content:space-between;gap:12px}.toolbar{display:flex;gap:12px;max-width:480px;margin-bottom:16px}.el-pagination{margin-top:18px}</style>
