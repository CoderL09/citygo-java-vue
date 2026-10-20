<template>
  <el-card v-loading="loading">
    <template #header><div class="bar"><span>预约联系人</span><el-button type="primary" @click="open()">新增联系人</el-button></div></template>
    <el-alert v-if="error" title="联系人加载失败" type="error" :closable="false"><el-button text @click="load">重试</el-button></el-alert>
    <el-empty v-else-if="!loading && !items.length" description="添加预约联系人，方便下单时选择" />
    <div v-for="item in items" :key="item.id" class="contact">
      <div><strong>{{ item.name }}</strong>　{{ item.phone }}　<el-tag v-if="item.defaultContact" size="small">默认</el-tag></div>
      <div><el-button @click="open(item)">编辑</el-button><el-button type="danger" plain @click="remove(item)">删除</el-button></div>
    </div>
    <el-dialog v-model="visible" :title="editing ? '编辑联系人' : '新增联系人'" width="min(460px, 92vw)">
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="姓名"><el-input v-model="form.name" maxlength="64" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
        <el-checkbox v-model="form.defaultContact">设为默认联系人</el-checkbox>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存联系人</el-button></template>
    </el-dialog>
  </el-card>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listContacts, saveContact, deleteContact, type Contact } from '@/api/booking'
const loading=ref(false), saving=ref(false), error=ref(false), visible=ref(false), editing=ref<number>()
const items=ref<Contact[]>([])
const form=reactive({name:'',phone:'',defaultContact:false})
async function load(){loading.value=true;error.value=false;try{items.value=await listContacts()}catch{error.value=true}finally{loading.value=false}}
function open(item?:Contact){editing.value=item?.id;Object.assign(form,item?{name:item.name,phone:item.phone,defaultContact:item.defaultContact}:{name:'',phone:'',defaultContact:false});visible.value=true}
async function save(){if(!form.name.trim() || !/^[0-9+() -]{5,20}$/.test(form.phone)){ElMessage.warning('请填写姓名和有效联系电话');return}if(saving.value)return;saving.value=true;try{await saveContact(editing.value,form);visible.value=false;ElMessage.success('联系人已保存');await load()}finally{saving.value=false}}
async function remove(item:Contact){try{await ElMessageBox.confirm('确认删除联系人「'+item.name+'」？历史订单中的联系人记录会保留。','删除联系人')}catch{return}await deleteContact(item.id);await load()}
onMounted(load)
</script>
<style scoped>.bar,.contact{display:flex;justify-content:space-between;gap:12px;align-items:center}.contact{padding:18px 0;border-bottom:1px solid var(--el-border-color-light);flex-wrap:wrap}</style>
