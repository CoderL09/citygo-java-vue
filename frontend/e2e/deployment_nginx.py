"""DEPLOY local-only UI acceptance; no real money, no deletion of existing data."""
import json, os, re, secrets, sys
from pathlib import Path
from datetime import datetime, timedelta, timezone
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright, expect
sys.stdout.reconfigure(encoding="utf-8");sys.stderr.reconfigure(encoding="utf-8")
ROOT=Path(__file__).resolve().parents[2]
WEB=os.environ.get('CITYGO_E2E_URL','http://127.0.0.1:8180')
assert urlparse(WEB).hostname in ('127.0.0.1','localhost')
OUT=ROOT/'demo-gifs'/'deployment-frames';OUT.mkdir(parents=True,exist_ok=True)
raw=(ROOT/'.env').read_bytes()
try:text=raw.decode('utf-8-sig')
except UnicodeDecodeError:text=raw.decode('gb18030')
cfg=dict(line.split('=',1) for line in text.splitlines() if '=' in line and not line.lstrip().startswith('#'))
marker='DEPLOY_'+datetime.now().strftime('%m%d_%H%M%S');username='deploy_'+secrets.token_hex(5);password=secrets.token_urlsafe(16)
checks=[];frames=[];errors=[]
def api(ctx,method,path,token=None,data=None):
    r=ctx.request.fetch(WEB+'/api'+path,method=method,headers={'Authorization':'Bearer '+token} if token else {},data=data)
    body=r.json();assert r.status==200 and body['code']==0,(path,r.status,body.get('message'));return body['data']
def shot(page,name):
    page.wait_for_load_state('networkidle');page.wait_for_function("document.querySelectorAll('.el-message').length===0")
    f=OUT/(name+'.png');page.screenshot(path=str(f),full_page=True);frames.append(str(f))
def passed(name):checks.append(name);print('PASS',name,flush=True)
def login(page,name,pwd):
    page.goto(WEB+'/login');page.wait_for_load_state('networkidle');assert page.get_by_role('button',name='登录',exact=True).count()==1
    page.get_by_label('用户名',exact=True).fill(name);page.get_by_label('密码',exact=True).fill(pwd);page.get_by_role('button',name='登录',exact=True).click();page.wait_for_url(lambda u:'/login' not in u);page.wait_for_load_state('networkidle')
def show_order(page,no):
    page.get_by_role('link',name='我的订单',exact=True).click();page.locator('.order').filter(has_text=no).get_by_role('button',name='查看订单').click();page.wait_for_load_state('networkidle')
def admin_row(page,no):
    page.get_by_role('menuitem',name='订单工作台',exact=True).click();page.get_by_placeholder('搜索订单号').fill(no);page.get_by_role('button',name='搜索',exact=True).click();return page.locator('.el-table__row').filter(has_text=no)
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True,channel='chrome');admin=browser.new_context(viewport={'width':1440,'height':1000});user=browser.new_context(viewport={'width':1280,'height':1000});a=admin.new_page();u=user.new_page()
    for page in (a,u):page.on('pageerror',lambda e:errors.append(str(e)))
    outcome={'status':'running','checks':checks,'frames':frames}
    try:
        html=admin.request.get(WEB+'/orders/123');assert html.status==200 and '@vite/client' not in html.text();assert '/assets/' in html.text()
        missing=admin.request.get(WEB+'/assets/missing-file.js');assert missing.status==404;passed('Nginx 提供构建产物及 SPA 深层路由，缺失资源返回404')
        at=api(admin,'POST','/auth/login',data={'username':cfg['CITYGO_ADMIN_USERNAME'],'password':cfg['CITYGO_ADMIN_PASSWORD']})['token']
        ut=api(user,'POST','/auth/register',data={'username':username,'password':password,'nickname':'DEPLOY 预约体验者'})['token']
        cat=api(admin,'POST','/admin/categories',at,{'categoryType':'ACTIVITY','name':marker,'sortOrder':0})['id']
        activity=api(admin,'POST','/admin/activities',at,{'categoryId':cat,'name':marker+'城市活动','address':'演示集合点'})['id']
        start=(datetime.now(timezone.utc)+timedelta(days=5)).replace(microsecond=0)
        session=api(admin,'POST',f'/admin/activities/{activity}/sessions',at,{'startTime':start.isoformat(),'endTime':(start+timedelta(hours=2)).isoformat(),'bookingDeadline':(start-timedelta(days=1)).isoformat(),'priceCents':9900,'capacity':10})['id']
        contact=api(user,'POST','/contacts',ut,{'name':'DEPLOY 预约联系人','phone':'13800000000','defaultContact':True})['id']
        def create_order(key):
            r=user.request.post(WEB+'/api/orders',headers={'Authorization':'Bearer '+ut,'Idempotency-Key':key},data={'contactId':contact,'items':[{'itemType':'SESSION','itemId':session,'quantity':1}]});assert r.status==200;return r.json()['data']['order']
        first=create_order(marker+'-first')
        login(u,username,password);u.locator('a[href="/notifications"]').click();u.wait_for_load_state('networkidle')
        expect(u.get_by_text('消息在线',exact=True)).to_be_visible(timeout=15000)
        expect(u.locator('.notice').filter(has_text=first['orderNo'])).not_to_have_count(0,timeout=15000);passed('单次票据建立实时连接，订单通知可见')
        login(a,cfg['CITYGO_ADMIN_USERNAME'],cfg['CITYGO_ADMIN_PASSWORD']);a.locator('a[href="/admin/notifications"]').click();a.wait_for_load_state('networkidle');expect(a.get_by_text('消息在线',exact=True)).to_be_visible(timeout=15000)
        api(user,'POST',f"/orders/{first['id']}/payment",ut,{'scenario':'SUCCESS'})
        expect(u.locator('.notice').filter(has_text='待确认')).not_to_have_count(0,timeout=15000);expect(a.locator('.notice').filter(has_text=first['orderNo'])).not_to_have_count(0,timeout=15000);shot(u,'01-live-notices');passed('支付状态实时推送给本人及运营')
        api(user,'POST',f"/orders/{first['id']}/urge",ut)
        expect(a.locator('.notice').filter(has_text='催确认')).not_to_have_count(0,timeout=15000)
        limited=user.request.post(WEB+f"/api/orders/{first['id']}/urge",headers={'Authorization':'Bearer '+ut});assert limited.status==429;passed('催单实时送达运营，重复催单限流')
        u.get_by_role('button',name='全部已读',exact=True).click();expect(u.get_by_role('button',name='全部已读',exact=True)).to_be_disabled();assert api(user,'GET','/notifications/unread',ut)['count']==0;shot(a,'02-operator-urge');passed('本人全部已读持久化，运营未读独立')
        user.set_offline(True);api(admin,'POST',f"/admin/orders/{first['id']}/confirm",at)
        user.set_offline(False);u.reload();login(u,username,password);u.locator('a[href="/notifications"]').click();u.wait_for_load_state('networkidle');expect(u.locator('.notice').filter(has_text='已确认')).not_to_have_count(0,timeout=15000);passed('离线期间状态通知持久保存，重新连接后补拉')
        old=api(user,'POST','/auth/login',data={'username':username,'password':password})['token'];api(user,'POST','/auth/logout',old)
        denied=user.request.get(WEB+'/api/auth/me',headers={'Authorization':'Bearer '+old});assert denied.status==401 and denied.json()['code']==40103;passed('退出撤销令牌，旧令牌无法再次访问')
        shot(u,'03-offline-catchup');api(user,'POST',f"/orders/{first['id']}/refund",ut,{'reason':'DEPLOY验证后释放名额'})
        png=a.screenshot();upload=admin.request.post(WEB+'/api/admin/files',headers={'Authorization':'Bearer '+at},multipart={'file':{'name':'nginx-acceptance.png','mimeType':'image/png','buffer':png}});assert upload.status==200;uploaded=upload.json()['data'];download=admin.request.get(WEB+uploaded['url']);assert download.status==200 and download.body()==png;assert download.headers.get('x-content-type-options')=='nosniff';assert download.headers.get('content-type','').startswith('image/png');deleted=admin.request.delete(WEB+'/api/admin/files/'+str(uploaded['id']),headers={'Authorization':'Bearer '+at});assert deleted.status==200;passed('通过真实 Nginx 上传及读取PNG，字节一致，响应含nosniff')
        stock=api(admin,'GET',f'/admin/activities/{activity}/sessions',at)[0]['available'];assert stock==10;assert errors==[],errors;passed('完整通知流程没有页面异常，退款后库存恢复')
        outcome.update(status='passed',orders=[first['id']],stock=stock,pageErrors=errors)
    except Exception as e:
        outcome.update(status='failed',failure=str(e),pageErrors=errors)
        for i,page in enumerate((a,u)):
            try:page.screenshot(path=str(OUT/f'failure-{i}.png'),full_page=True)
            except Exception:pass
        raise
    finally:
        (OUT/'result.json').write_text(json.dumps(outcome,ensure_ascii=False,indent=2),encoding='utf-8');user.close();admin.close();browser.close()
print('DEPLOY browser acceptance:',outcome['status'],flush=True)
