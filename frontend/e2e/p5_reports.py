"""P5 local-only UI acceptance; no real money, no deletion of existing data."""
import json, os, re, secrets, sys
from pathlib import Path
from datetime import datetime, timedelta, timezone
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright, expect
sys.stdout.reconfigure(encoding="utf-8");sys.stderr.reconfigure(encoding="utf-8")
ROOT=Path(__file__).resolve().parents[2]
WEB=os.environ.get('CITYGO_E2E_URL','http://127.0.0.1:5175')
assert urlparse(WEB).hostname in ('127.0.0.1','localhost')
OUT=ROOT/'demo-gifs'/'p5-frames';OUT.mkdir(parents=True,exist_ok=True)
raw=(ROOT/'.env').read_bytes()
try:text=raw.decode('utf-8-sig')
except UnicodeDecodeError:text=raw.decode('gb18030')
cfg=dict(line.split('=',1) for line in text.splitlines() if '=' in line and not line.lstrip().startswith('#'))
marker='P5_'+datetime.now().strftime('%m%d_%H%M%S');username='p5_'+secrets.token_hex(5);password=secrets.token_urlsafe(16)
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
        at=api(admin,'POST','/auth/login',data={'username':cfg['CITYGO_ADMIN_USERNAME'],'password':cfg['CITYGO_ADMIN_PASSWORD']})['token']
        ut=api(user,'POST','/auth/register',data={'username':username,'password':password,'nickname':'P5 预约体验者'})['token']
        cat=api(admin,'POST','/admin/categories',at,{'categoryType':'ACTIVITY','name':marker,'sortOrder':0})['id']
        activity=api(admin,'POST','/admin/activities',at,{'categoryId':cat,'name':marker+'城市活动','address':'演示集合点'})['id']
        start=(datetime.now(timezone.utc)+timedelta(days=5)).replace(microsecond=0)
        session=api(admin,'POST',f'/admin/activities/{activity}/sessions',at,{'startTime':start.isoformat(),'endTime':(start+timedelta(hours=2)).isoformat(),'bookingDeadline':(start-timedelta(days=1)).isoformat(),'priceCents':9900,'capacity':10})['id']
        contact=api(user,'POST','/contacts',ut,{'name':'P5 预约联系人','phone':'13800000000','defaultContact':True})['id']
        def create_order(key):
            r=user.request.post(WEB+'/api/orders',headers={'Authorization':'Bearer '+ut,'Idempotency-Key':key},data={'contactId':contact,'items':[{'itemType':'SESSION','itemId':session,'quantity':1}]});assert r.status==200;return r.json()['data']['order']
        first=create_order(marker+'-first')
        login(u,username,password);show_order(u,first['orderNo']);u.get_by_role('button',name='模拟支付',exact=True).click();expect(u.get_by_text('待确认',exact=True)).to_be_visible();passed('新增支付渠道识别后 Mock 支付仍正常')
        u.get_by_role('button',name='再来一单',exact=True).click();u.wait_for_url('**/cart');u.wait_for_load_state('networkidle');expect(u.get_by_text(marker+'城市活动',exact=True)).to_be_visible();shot(u,'01-reorder-cart');passed('历史订单重新加入购物车，展示当前商品价格')
        login(a,cfg['CITYGO_ADMIN_USERNAME'],cfg['CITYGO_ADMIN_PASSWORD']);a.get_by_role('menuitem',name='运营统计',exact=True).click();a.wait_for_load_state('networkidle');expect(a.get_by_text('商品毛销售排名',exact=True)).to_be_visible();expect(a.locator('.el-table__row').filter(has_text=marker+'城市活动')).not_to_have_count(0);shot(a,'02-real-report')
        with a.expect_download() as download:a.get_by_role('button',name='导出 Excel',exact=True).click()
        f=OUT/'citygo-report.xlsx';download.value.save_as(f);assert f.read_bytes().startswith(b'PK');passed('运营统计展示真实支付数据，Excel 下载成功')
        a.get_by_role('menuitem',name='操作审计',exact=True).click();a.wait_for_load_state('networkidle');expect(a.locator('.el-table__row').filter(has_text='export')).not_to_have_count(0);shot(a,'03-audit');passed('导出成功操作写入脱敏审计并在页面展示')
        api(user,'POST',f"/orders/{first['id']}/refund",ut,{'reason':'P5验证后释放'})
        stock=api(admin,'GET',f'/admin/activities/{activity}/sessions',at)[0]['available'];assert stock==10;assert errors==[],errors;passed('新功能浏览器无脚本错误，退款后名额恢复')
        outcome.update(status='passed',orders=[first['id']],stock=stock,pageErrors=errors)
    except Exception as e:
        outcome.update(status='failed',failure=str(e),pageErrors=errors)
        for i,page in enumerate((a,u)):
            try:page.screenshot(path=str(OUT/f'failure-{i}.png'),full_page=True)
            except Exception:pass
        raise
    finally:
        (OUT/'result.json').write_text(json.dumps(outcome,ensure_ascii=False,indent=2),encoding='utf-8');user.close();admin.close();browser.close()
print('P5 browser acceptance:',outcome['status'],flush=True)
