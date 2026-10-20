import { get, post } from './http'
import type { PageResult } from './types'
import type { OrderDetail,OrderSummary } from './booking'
export interface PaymentRecord { id:number;requestNo:string;channel:string;amountCents:number;status:string;paidAt:string|null }
export interface RefundRecord { id:number;refundNo:string;orderId:number;amountCents:number;status:string;failReason:string|null;compensation:boolean;successAt:string|null }
export interface PaymentRecords { payments:PaymentRecord[];refunds:RefundRecord[] }
export const payOrder=(id:number,scenario:string)=>post<PaymentRecords>(`/orders/${id}/payment`,{scenario})
export const cancelOrder=(id:number)=>post<OrderDetail>(`/orders/${id}/cancel`)
export const refundOrder=(id:number,reason:string)=>post<RefundRecord>(`/orders/${id}/refund`,{reason})
export const paymentRecords=(id:number,admin=false)=>get<PaymentRecords>(`/${admin?'admin/':''}orders/${id}/payments`)
export const adminOrders=(page:number,status?:string,keyword?:string)=>get<PageResult<OrderSummary>>('/admin/orders',{params:{page,pageSize:10,status:status||undefined,keyword:keyword||undefined}})
export const adminOrder=(id:number)=>get<OrderDetail>(`/admin/orders/${id}`)
export const orderAction=(id:number,action:'confirm'|'reject'|'complete',reason?:string)=>post<OrderDetail>(`/admin/orders/${id}/${action}`,reason?{reason}:undefined)
export const adminRefunds=(page:number)=>get<PageResult<RefundRecord>>('/admin/refunds',{params:{page,pageSize:10}})
export const retryRefund=(id:number)=>post<RefundRecord>(`/admin/refunds/${id}/retry`)
