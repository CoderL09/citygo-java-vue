import { defineStore } from 'pinia'
import { computed,ref } from 'vue'
import { get,post,put } from '@/api/http'
export interface Notice { id:number;eventType:string;title:string;content:string;createdAt:string;read:boolean }
export const useNotifications=defineStore('notifications',()=>{
  const items=ref<Notice[]>([]),unread=ref(0),online=ref(false),error=ref(false)
  let socket:WebSocket|undefined,retry:ReturnType<typeof setTimeout>|undefined,poll:ReturnType<typeof setInterval>|undefined,epoch=0,cursor=0,attempt=0,pulling=false,again=false
  const silent={headers:{'X-Citygo-Silent':'1'}}
  function merge(rows:Notice[]){const byId=new Map(items.value.map(n=>[n.id,n]));for(const n of rows)byId.set(n.id,n);items.value=[...byId.values()].sort((a,b)=>b.id-a.id)}
  async function pull(){
    if(pulling){again=true;return}const run=epoch;pulling=true
    try{let pages=0;do{again=false;const rows=await get<Notice[]>('/notifications',{params:{after:cursor,limit:50},...silent});if(run!==epoch)return;merge(rows);if(rows.length)cursor=rows[rows.length-1].id;again=rows.length===50;pages++}while(again&&pages<10)
      const count=await get<{count:number}>('/notifications/unread',silent);if(run===epoch){unread.value=count.count;error.value=false}
    }catch{if(run===epoch){error.value=true;again=false}}finally{if(run===epoch){pulling=false;if(again)setTimeout(()=>void pull(),100)}}
  }
  async function connect(run:number){
    if(run!==epoch)return
    try{const {ticket}=await post<{ticket:string}>('/ws-ticket',undefined,silent);if(run!==epoch)return
      const url=new URL('/ws',location.href);url.protocol=location.protocol==='https:'?'wss:':'ws:';url.searchParams.set('ticket',ticket)
      const connection=new WebSocket(url);socket=connection
      connection.onopen=()=>{if(run!==epoch){connection.close();return}online.value=true;attempt=0;void pull()}
      connection.onmessage=event=>{try{const n=JSON.parse(event.data) as Notice;if(typeof n.id==='number'){merge([n]);void pull()}}catch{}}
      connection.onclose=()=>{if(run!==epoch)return;online.value=false;schedule(run)}
      connection.onerror=()=>connection.close()
    }catch{if(run===epoch){online.value=false;schedule(run)}}
  }
  function schedule(run:number){if(retry)clearTimeout(retry);retry=setTimeout(()=>void connect(run),Math.min(30000,1000*2**Math.min(attempt++,5)))}
  function stop(){epoch++;if(retry)clearTimeout(retry);if(poll)clearInterval(poll);socket?.close();socket=undefined;online.value=false;items.value=[];unread.value=0;cursor=0;pulling=false;again=false}
  function start(){stop();const run=epoch;void pull();void connect(run);poll=setInterval(()=>void pull(),30000)}
  async function markRead(id:number){await put('/notifications/'+id+'/read');items.value=items.value.map(n=>n.id===id?{...n,read:true}:n);await pull()}
  async function readAll(){await put('/notifications/read-all');items.value=items.value.map(n=>({...n,read:true}));unread.value=0}
  const hasUnread=computed(()=>unread.value>0)
  return {items,unread,online,error,hasUnread,start,stop,pull,markRead,readAll}
})
