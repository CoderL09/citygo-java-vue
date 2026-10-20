"""Local real-browser acceptance for rescheduling and QR image decoding."""
import json,os,secrets,sys
from pathlib import Path
from datetime import datetime,timedelta,timezone
from playwright.sync_api import sync_playwright,expect
sys.stdout.reconfigure(encoding='utf-8')
ROOT=Path(__file__).resolve().parents[2];WEB=os.environ.get('CITYGO_E2E_URL','http://127.0.0.1:8181')
OUT=ROOT/'demo-gifs'/'fulfillment-frames';OUT.mkdir(parents=True,exist_ok=True)
from urllib.parse import urlparse
assert urlparse(WEB).hostname in ('127.0.0.1','localhost')
CLOCK=Path(os.environ['CITYGO_TEST_CLOCK_FILE']);assert CLOCK.is_file()
raw=(ROOT/'.env').read_bytes()
try:text=raw.decode('utf-8-sig')
except UnicodeDecodeError:text=raw.decode('gb18030')
cfg=dict(l.split('=',1) for l in text.splitlines() if '=' in l and not l.lstrip().startswith('#'))
now=datetime.fromisoformat(CLOCK.read_text().strip().replace('Z','+00:00'))
checks=[];frames=[];errors=[];username='ful_'+secrets.token_hex(5);password=secrets.token_urlsafe(16)
def api(ctx,method,path,token=None,data=None):
 r=ctx.request.fetch(WEB+'/api'+path,method=method,headers={'Authorization':'Bearer '+token} if token else {},data=data);b=r.json();assert r.status==200 and b['code']==0,(path,r.status,b.get('message'));return b['data']
def login(page,name,pwd):
 page.goto(WEB+'/login');page.wait_for_load_state('networkidle');page.get_by_label('用户名',exact=True).fill(name);page.get_by_label('密码',exact=True).fill(pwd);page.get_by_role('button',name='登录',exact=True).click();page.wait_for_url(lambda u:'/login' not in u);page.wait_for_load_state('networkidle')
def passed(s):checks.append(s);print('PASS',s,flush=True)
def show_order(page,no):
 page.get_by_role('link',name='我的订单',exact=True).click();page.locator('.order').filter(has_text=no).get_by_role('button',name='查看订单').click();page.wait_for_load_state('networkidle')
def shot(page,name):
 page.wait_for_load_state('networkidle');page.wait_for_function("document.querySelectorAll('.el-message').length===0");f=OUT/(name+'.png');page.screenshot(path=str(f),full_page=True);frames.append(str(f))
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,channel='chrome');admin=browser.new_context(viewport={'width':1440,'height':1000});user=browser.new_context(viewport={'width':1280,'height':1000});a=admin.new_page();u=user.new_page()
 for page in (a,u):page.on('pageerror',lambda e:errors.append(str(e)))
 result={'status':'running','checks':checks,'frames':frames}
 try:
  at=api(admin,'POST','/auth/login',data={'username':cfg['CITYGO_ADMIN_USERNAME'],'password':cfg['CITYGO_ADMIN_PASSWORD']})['token'];ut=api(user,'POST','/auth/register',data={'username':username,'password':password,'nickname':'改期体验者'})['token']
  cat=api(admin,'POST','/admin/categories',at,{'categoryType':'ACTIVITY','name':'改期核销_'+secrets.token_hex(3),'sortOrder':0})['id'];activity=api(admin,'POST','/admin/activities',at,{'categoryId':cat,'name':'城市漫步·改期与核销体验','address':'城市广场集合点'})['id']
  start=(now+timedelta(days=2)).replace(microsecond=0);target_start=start+timedelta(days=1)
  def session(t):return api(admin,'POST',f'/admin/activities/{activity}/sessions',at,{'startTime':t.isoformat(),'endTime':(t+timedelta(hours=2)).isoformat(),'bookingDeadline':(t-timedelta(hours=2)).isoformat(),'priceCents':1000,'capacity':2})['id']
  source=session(start);target=session(target_start);contact=api(user,'POST','/contacts',ut,{'name':'核销体验联系人','phone':'13800000000','defaultContact':True})['id']
  r=user.request.post(WEB+'/api/orders',headers={'Authorization':'Bearer '+ut,'Idempotency-Key':'feature-'+secrets.token_hex(8)},data={'contactId':contact,'items':[{'itemType':'SESSION','itemId':source,'quantity':1}]});assert r.status==200;r=r.json();assert r['code']==0;order=r['data']['order'];oid=order['id']
  login(u,username,password);show_order(u,order['orderNo']);u.get_by_role('button',name='模拟支付',exact=True).click();expect(u.get_by_text('待确认',exact=True)).to_be_visible()
  login(a,cfg['CITYGO_ADMIN_USERNAME'],cfg['CITYGO_ADMIN_PASSWORD']);a.get_by_role('menuitem',name='订单工作台',exact=True).click();a.get_by_placeholder('搜索订单号').fill(order['orderNo']);a.get_by_role('button',name='搜索',exact=True).click();row=a.locator('.el-table__row').filter(has_text=order['orderNo']);row.get_by_role('button',name='确认',exact=True).click();expect(row).to_contain_text('已确认')
  u.get_by_role('button',name='刷新状态',exact=True).click();u.get_by_role('button',name='生成核销码',exact=True).click();qr=u.get_by_alt_text('预约核销二维码');expect(qr).to_be_visible();old=OUT/'old-qr.png';qr.screenshot(path=str(old));shot(u,'01-user-qr')
  a.get_by_role('menuitem',name='扫码核销',exact=True).click();a.wait_for_load_state('networkidle');a.get_by_label('二维码图片').set_input_files(str(old));expect(a.get_by_role('button',name='确认核销',exact=True)).to_be_disabled();expect(a.locator('.preview')).to_contain_text(order['orderNo']);shot(a,'02-early-checkin-rejected');passed('真实二维码图片识别成功，未开始场次禁止核销')
  lost={'done':False,'keys':[]}
  def lose_first(route):
   if route.request.method!='POST':route.continue_();return
   lost['keys'].append(route.request.headers.get('idempotency-key'))
   if not lost['done']:
    response=route.fetch();assert response.json()['data']['version']==1;lost['done']=True;route.abort()
   else:route.continue_()
  u.route('**/api/orders/'+str(oid)+'/reschedule',lose_first);u.get_by_role('button',name='预约改期',exact=True).click();dialog=u.get_by_role('dialog',name='预约改期');dialog.locator('label.el-radio').first.click();expect(dialog.get_by_role('radio').first).to_be_checked();dialog.get_by_role('button',name='确认改期').click();expect(dialog.get_by_role('button',name='确认改期')).to_be_enabled();assert lost['done'];dialog.get_by_role('button',name='确认改期').click();expect(dialog).not_to_be_visible();expect(u.get_by_text('已改期 1 次',exact=True)).to_be_visible();expect(qr).not_to_be_visible();assert len(lost['keys'])==2 and lost['keys'][0]==lost['keys'][1];state=api(user,'GET',f'/orders/{oid}/fulfillment',ut);assert state['version']==1 and len(state['history'])==1 and state['current']['sessionId']==target;shot(u,'03-rescheduled');passed('响应丢失后沿用幂等键重试，只改期一次，旧码从页面清除')
  a.get_by_label('二维码图片').set_input_files(str(old));expect(a.get_by_text('核销码已过期，请用户刷新',exact=True)).to_be_visible();passed('改期后旧二维码服务端失效')
  CLOCK.write_text((target_start+timedelta(minutes=1)).isoformat().replace('+00:00','Z'),encoding='utf-8')
  login(u,username,password);show_order(u,order['orderNo']);u.get_by_role('button',name='生成核销码').click();fresh=OUT/'fresh-qr.png';expect(qr).to_be_visible();qr.screenshot(path=str(fresh))
  login(a,cfg['CITYGO_ADMIN_USERNAME'],cfg['CITYGO_ADMIN_PASSWORD']);a.get_by_role('menuitem',name='扫码核销',exact=True).click();a.wait_for_load_state('networkidle');a.get_by_label('二维码图片').set_input_files(str(fresh));button=a.get_by_role('button',name='确认核销',exact=True);expect(button).to_be_enabled();shot(a,'04-checkin-preview');button.click();a.get_by_role('button',name='确定',exact=True).click();expect(a.locator('.el-result__title').get_by_text('核销成功',exact=True)).to_be_visible();shot(a,'05-checkin-completed');passed('按改期后的时间核对预约，工作人员确认后扫码核销完成')
  a.get_by_label('二维码图片').set_input_files(str(fresh));expect(a.get_by_text('此核销码已使用，请勿重复核销',exact=True)).to_be_visible();passed('重复扫描已使用二维码被拒绝')
  u.get_by_role('button',name='刷新状态',exact=True).click();expect(u.get_by_text('已完成',exact=True)).to_be_visible();expect(u.locator('.fulfillment')).to_contain_text('扫码核销');u.set_viewport_size({'width':390,'height':844});shot(u,'06-mobile-completed');assert u.evaluate('document.documentElement.scrollWidth<=innerWidth'), 'mobile overflow'
  at=api(admin,'POST','/auth/login',data={'username':cfg['CITYGO_ADMIN_USERNAME'],'password':cfg['CITYGO_ADMIN_PASSWORD']})['token'];stock={s['id']:s['available'] for s in api(admin,'GET',f'/admin/activities/{activity}/sessions',at)};assert stock[source]==2 and stock[target]==1;assert errors==[],errors;passed('原场次名额归还，完成核销不释放新场次名额，移动端无横向溢出且无脚本错误')
  result.update(status='passed',orderId=oid,sourceAvailable=stock[source],targetAvailable=stock[target],pageErrors=errors)
 except Exception as e:
  result.update(status='failed',failure=str(e),pageErrors=errors)
  for i,page in enumerate((a,u)):
   try:page.screenshot(path=str(OUT/f'failure-{i}.png'),full_page=True)
   except Exception:pass
  raise
 finally:
  (OUT/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');user.close();admin.close();browser.close()
print('Fulfillment acceptance:',result['status'],flush=True)
