<template>
  <el-card v-loading="loading">
    <template #header><div class="bar"><span>购物车</span><el-button :disabled="!rows.length || busy" @click="clear">清空购物车</el-button></div></template>
    <el-alert v-if="error" title="购物车加载失败" type="error" :closable="false"><el-button text @click="load">重试</el-button></el-alert>
    <el-empty v-else-if="!loading && !rows.length" description="购物车是空的，去挑选活动或套餐吧" />
    <div v-for="row in rows" :key="row.item.id" class="row">
      <el-checkbox :model-value="row.item.selected" :disabled="busy" :aria-label="'选中 '+(row.product?.name || '失效商品')" @change="(v: string | number | boolean)=>change(row,row.item.quantity,Boolean(v))" />
      <div class="info"><strong>{{row.product?.name || '商品 #'+row.item.itemId}}</strong><el-tag size="small">{{row.item.itemType==='PACKAGE'?'套餐':'活动'}}</el-tag><p v-if="row.product">¥{{formatPrice(row.product.unitPriceCents)}} / 份</p><el-text v-if="!row.valid" type="danger">{{row.invalidReason}}</el-text></div>
      <el-input-number :model-value="row.item.quantity" :min="1" :max="5" :disabled="busy || !row.valid" @change="(v: number | undefined)=>change(row,Number(v),row.item.selected)" />
      <el-button type="danger" plain :disabled="busy" @click="remove(row.item.id)">移除</el-button>
    </div>
    <div v-if="rows.length" class="checkout">
      <el-alert v-if="selected.some(r=>!r.valid)" title="已选商品存在失效项，请取消勾选或移除。" type="warning" :closable="false" />
      <p>已选 {{selected.length}} 项，当前合计 <strong>¥{{formatPrice(total)}}</strong></p>
      <el-button type="primary" :disabled="!selected.length || selected.length>10 || selected.some(r=>!r.valid) || busy" @click="confirm">确认订单</el-button>
      <p v-if="selected.length>10">每单最多选择 10 项商品。</p>
    </div>
    <el-dialog v-model="visible" title="确认预约订单" width="min(600px, 94vw)">
      <el-empty v-if="!contacts.length" description="请先添加预约联系人"><el-button @click="$router.push('/contacts')">管理联系人</el-button></el-empty>
      <el-form v-else label-position="top">
        <el-form-item label="预约联系人"><el-select v-model="contactId" style="width:100%" @change="refreshPreview"><el-option v-for="c in contacts" :key="c.id" :label="c.name+' · '+c.phone" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="remark" maxlength="255" @input="preview=undefined" /></el-form-item>
      </el-form>
      <div v-loading="previewing"><p v-for="line in preview?.items" :key="line.request.itemType+line.request.itemId">{{line.product.name}} × {{line.request.quantity}} <strong>¥{{formatPrice(line.lineTotalCents)}}</strong></p></div>
      <el-alert title="价格与名额以提交时确认为准。每份套餐占各子场次一人名额；联系人信息将保存在订单中。" type="info" :closable="false" />
      <el-alert v-if="submissionError" :title="submissionError" type="warning" :closable="false" />
      <p v-if="preview">订单金额 <strong>¥{{formatPrice(preview.totalCents)}}</strong></p>
      <template #footer><el-button :disabled="submitting" @click="visible=false">返回购物车</el-button><el-button v-if="!preview" :loading="previewing" :disabled="!contactId" @click="refreshPreview">重新确认价格</el-button><el-button type="primary" :loading="submitting" :disabled="!preview || previewing" @click="submit">提交订单</el-button></template>
    </el-dialog>
  </el-card>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCart, listContacts, updateCartItem, deleteCartItem, clearCart, previewOrder, createOrder, type CartRow, type Contact, type Preview, type CheckoutRequest } from '@/api/booking'
import { formatPrice } from '@/utils/format'
import { ApiError } from '@/api/http'
import { CheckoutAttempt } from '@/utils/checkout'
const router=useRouter(), rows=ref<CartRow[]>([]),contacts=ref<Contact[]>([]),contactId=ref<number>(),remark=ref(''),preview=ref<Preview>()
const loading=ref(false),error=ref(false),busy=ref(false),visible=ref(false),previewing=ref(false),submitting=ref(false),attempt=new CheckoutAttempt()
const submissionError=ref('')
const selected=computed(()=>rows.value.filter(r=>r.item.selected))
const total=computed(()=>selected.value.reduce((sum,r)=>sum+(r.product?.unitPriceCents||0)*r.item.quantity,0))
function payload():CheckoutRequest{return{contactId:contactId.value!,remark:remark.value,items:selected.value.map(r=>({itemType:r.item.itemType,itemId:r.item.itemId,quantity:r.item.quantity}))}}
async function load(){loading.value=true;error.value=false;try{rows.value=await getCart()}catch{error.value=true}finally{loading.value=false}}
async function change(row:CartRow,quantity:number,isSelected:boolean){if(busy.value)return;busy.value=true;try{await updateCartItem(row.item.id,quantity,isSelected);await load()}finally{busy.value=false}}
async function remove(id:number){busy.value=true;try{await deleteCartItem(id);await load()}finally{busy.value=false}}
async function clear(){try{await ElMessageBox.confirm('确认清空购物车？','清空购物车')}catch{return}busy.value=true;try{await clearCart();await load()}finally{busy.value=false}}
async function confirm(){busy.value=true;try{contacts.value=await listContacts();contactId.value=contacts.value.find(c=>c.defaultContact)?.id||contacts.value[0]?.id;preview.value=undefined;visible.value=true;if(contactId.value)await refreshPreview()}finally{busy.value=false}}
async function refreshPreview(){if(!contactId.value)return;previewing.value=true;preview.value=undefined;try{preview.value=await previewOrder(payload())}finally{previewing.value=false}}
async function submit(){
  if(submitting.value||!preview.value)return
  const request=payload(), key=attempt.forRequest(request)
  submitting.value=true;submissionError.value=''
  try{const res=await createOrder(request,key);attempt.complete();visible.value=false;ElMessage.success('预约订单已创建');await router.push('/orders/'+res.order.id)}
  catch(error){
    if(error instanceof ApiError && (error.code===-1 || error.code===50300)) {
      // Preserve the preview/request/key when the response may have been lost after commit.
      submissionError.value='未能确认下单结果。保持商品和联系人不变可直接重试，也可先到我的订单查看。'
    } else {submissionError.value='商品或名额已变化，请重新确认价格。';preview.value=undefined}
  }finally{submitting.value=false}
}
onMounted(load)
</script>
<style scoped>.bar,.row{display:flex;align-items:center;gap:16px;justify-content:space-between}.row{padding:20px 0;border-bottom:1px solid var(--el-border-color-light);flex-wrap:wrap}.info{flex:1;min-width:160px}.info .el-tag{margin-left:8px}.checkout{text-align:right;padding-top:20px}.checkout strong{font-size:24px}</style>
