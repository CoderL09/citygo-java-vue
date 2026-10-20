import {get,post} from './http'
import type {SessionSnapshot,OrderDetail} from './booking'
import type {SessionView} from './types'
export interface ScheduleHistory{id:number;fromSessionId:number;toSessionId:number;quantity:number;before:SessionSnapshot;after:SessionSnapshot;createdAt:string}
export interface Fulfillment{version:number;canReschedule:boolean;reason:string|null;current:SessionSnapshot|null;candidates:SessionView[];history:ScheduleHistory[];checkin:{orderId:number;source:string;operatorId:number;checkedAt:string}|null}
export interface CheckinCode{code:string;expiresAt:string;version:number;ttlSeconds:number}
export interface CheckinPreview{orderId:number;orderNo:string;status:string;canComplete:boolean;reason:string|null;detail:OrderDetail;version:number;fulfillment:Fulfillment}
export const getFulfillment=(id:number,admin=false)=>get<Fulfillment>('/'+(admin?'admin/':'')+'orders/'+id+'/fulfillment')
export const moveOrder=(id:number,targetSessionId:number,expectedVersion:number,key:string)=>post<Fulfillment>('/orders/'+id+'/reschedule',{targetSessionId,expectedVersion},{headers:{'Idempotency-Key':key}})
export const issueCode=(id:number)=>post<CheckinCode>('/orders/'+id+'/checkin-code')
export const previewCode=(code:string)=>post<CheckinPreview>('/admin/checkins/preview',{code})
export const redeemCode=(code:string)=>post<OrderDetail>('/admin/checkins/redeem',{code})
