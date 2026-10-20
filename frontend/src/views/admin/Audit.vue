<template><el-card v-loading="loading"><template #header>操作审计 <el-button @click="load">刷新</el-button></template><el-alert v-if="error" title="审计加载失败，请重试" type="error" :closable="false"/><el-table :data="rows"><el-table-column prop="operatorId" label="操作人 ID"/><el-table-column prop="targetType" label="目标类型" min-width="180"/><el-table-column prop="targetId" label="目标 ID"/><el-table-column prop="action" label="动作"/><el-table-column prop="summary" label="摘要"/><el-table-column label="结果"><template #default="s">{{s.row.success?'成功':'失败'}}</template></el-table-column><el-table-column label="时间" min-width="180"><template #default="s">{{formatDateTime(s.row.createdAt)}}</template></el-table-column></el-table><el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev,pager,next,total" @current-change="load"/></el-card></template>
<script setup lang="ts">
import {ref,onMounted} from 'vue'
import {get} from '@/api/http'
import {formatDateTime} from '@/utils/format'
import type{PageResult} from '@/api/types'
interface Row{operatorId:number;targetType:string;targetId:number|null;action:string;summary:string;success:boolean;createdAt:string}
const rows=ref<Row[]>([]),page=ref(1),total=ref(0),loading=ref(false),error=ref(false)
async function load(){loading.value=true;error.value=false;try{const r=await get<PageResult<Row>>('/admin/audit',{params:{page:page.value,pageSize:20}});rows.value=r.items;total.value=r.total}catch{error.value=true}finally{loading.value=false}}
onMounted(load)
</script>
