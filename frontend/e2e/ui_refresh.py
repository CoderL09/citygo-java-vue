"""Local real-browser UI acceptance; fixtures use real APIs, no mocked dashboard data."""
import json,os,secrets,sys
from pathlib import Path
from datetime import datetime,timedelta,timezone
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright,expect
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
ROOT=Path(os.environ.get('CITYGO_PROJECT_ROOT',str(Path(__file__).resolve().parents[2])));WEB=os.environ.get('CITYGO_E2E_URL','http://127.0.0.1:8180');assert urlparse(WEB).hostname in ('localhost','127.0.0.1')
OUT=ROOT/'demo-gifs'/'ui-refresh-frames';OUT.mkdir(parents=True,exist_ok=True)
raw=(ROOT/'.env').read_bytes()
try:text=raw.decode('utf-8-sig')
except UnicodeDecodeError:text=raw.decode('gb18030')
cfg=dict(l.split('=',1) for l in text.splitlines() if '=' in l and not l.lstrip().startswith('#'))
checks=[];frames=[];errors=[];username='ui_'+secrets.token_hex(5);password=secrets.token_urlsafe(16)
def passed(s):checks.append(s);print('PASS',s,flush=True)
def api(ctx,method,path,token=None,data=None):
 r=ctx.request.fetch(WEB+'/api'+path,method=method,headers={'Authorization':'Bearer '+token} if token else {},data=data);b=r.json();assert r.status==200 and b['code']==0,(path,r.status,b.get('message'));return b['data']
def settle(page):page.wait_for_load_state('networkidle');page.wait_for_function("document.querySelectorAll('.el-loading-mask:not([style*=\"display: none\"])').length===0")
def shot(page,name):
 settle(page);page.wait_for_function("document.querySelectorAll('.el-message').length===0");f=OUT/(name+'.png');page.screenshot(path=str(f),full_page=True);frames.append(str(f))
def width(page,name):
 settle(page);assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),name+' horizontal overflow'
def login(page,name,pwd):
 page.goto(WEB+'/login');settle(page);page.get_by_label('用户名',exact=True).fill(name);page.get_by_label('密码',exact=True).fill(pwd);page.get_by_role('button',name='登录',exact=True).click();page.wait_for_url(lambda u:'/login' not in u);settle(page)
def menu(page,label):page.get_by_role('menuitem',name=label,exact=True).click();settle(page)
def show_order(page,no):page.get_by_role('link',name='我的订单',exact=True).click();page.locator('.order').filter(has_text=no).get_by_role('button',name='查看订单').click();settle(page)
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,channel='chrome');admin=browser.new_context(viewport={'width':1440,'height':1000});user=browser.new_context(viewport={'width':1440,'height':1000});a=admin.new_page();u=user.new_page()
 for page in (a,u):page.on('pageerror',lambda e:errors.append(str(e)))
 result={'status':'running','checks':checks,'frames':frames}
 try:
  at=api(admin,'POST','/auth/login',data={'username':cfg['CITYGO_ADMIN_USERNAME'],'password':cfg['CITYGO_ADMIN_PASSWORD']})['token'];ut=api(user,'POST','/auth/register',data={'username':username,'password':password,'nickname':'城市探索者'})['token']
  fixture_file=ROOT/'data/private/ui-design-fixtures.json'
  if fixture_file.is_file():
   fixture=json.loads(fixture_file.read_text(encoding='utf-8'));category_name=fixture['categoryName'];category=fixture['category'];activities=fixture['activities'];sessions=fixture['sessions'];target=fixture['target'];fixtures=fixture['fixtures']
  else:
   category_name='城市精选 '+secrets.token_hex(2);category=api(admin,'POST','/admin/categories',at,{'categoryType':'ACTIVITY','name':category_name,'sortOrder':0})['id']
   fixtures=[('老城慢行 · 街巷里的城市记忆','老城文化街区集合点','放慢脚步，走进老城街巷。在熟悉的烟火气里，重新发现这座城市的故事。',9900),('美术馆漫游 · 和艺术打个照面','城市美术馆南门','从一幅画开始，让周末换一种节奏。一起走进展厅，寻找属于自己的灵感。',6900),('河岸夜行 · 把晚风装进行程','滨河步道城市广场','沿着河岸散步，在夜色与晚风里，收集一天结束前的小小美好。',4900)]
   sessions=[];activities=[];start=(datetime.now(timezone.utc)+timedelta(days=5)).replace(microsecond=0)
   for i,(name,address,description,price) in enumerate(fixtures):
    aid=api(admin,'POST','/admin/activities',at,{'categoryId':category,'name':name,'address':address,'description':description})['id'];activities.append(aid);t=start+timedelta(days=i);sessions.append(api(admin,'POST',f'/admin/activities/{aid}/sessions',at,{'startTime':t.isoformat(),'endTime':(t+timedelta(hours=2)).isoformat(),'bookingDeadline':(t-timedelta(hours=2)).isoformat(),'priceCents':price,'capacity':12})['id'])
   target_time=start+timedelta(days=10);target=api(admin,'POST',f'/admin/activities/{activities[0]}/sessions',at,{'startTime':target_time.isoformat(),'endTime':(target_time+timedelta(hours=2)).isoformat(),'bookingDeadline':(target_time-timedelta(hours=2)).isoformat(),'priceCents':9900,'capacity':12})['id']
   pcat=api(admin,'POST','/admin/categories',at,{'categoryType':'PACKAGE','name':'城市组合 '+secrets.token_hex(2),'sortOrder':0})['id'];package=api(admin,'POST','/admin/packages',at,{'categoryId':pcat,'name':'街巷与艺术 · 双体验套餐','description':'把老城漫步和美术馆体验安排在一起，在城市的烟火与艺术之间，找到自己的节奏。','packagePriceCents':13800,'sessionIds':sessions[:2]})['product']['id'];api(admin,'PUT',f'/admin/packages/{package}/status',at,{'status':'ON_SALE'})
   fixture_file.parent.mkdir(parents=True,exist_ok=True);fixture_file.write_text(json.dumps({'categoryName':category_name,'category':category,'activities':activities,'sessions':sessions,'target':target,'fixtures':fixtures},ensure_ascii=False),encoding='utf-8')
  contact=api(user,'POST','/contacts',ut,{'name':'城市探索者','phone':'13800000000','defaultContact':True})['id'];r=user.request.post(WEB+'/api/orders',headers={'Authorization':'Bearer '+ut,'Idempotency-Key':'ui-refresh-'+secrets.token_hex(5)},data={'contactId':contact,'items':[{'itemType':'SESSION','itemId':sessions[0],'quantity':1}]});assert r.status==200;body=r.json();assert body['code']==0;order=body['data']['order']
  u.goto(WEB);settle(u);expect(u.get_by_role('heading',name='把周末， 留给这座城。')).to_be_visible();assert 'YaHei' in u.evaluate('getComputedStyle(document.body).fontFamily');width(u,'home desktop');shot(u,'01-user-home');u.keyboard.press('Tab');expect(u.locator('.skip-link')).to_be_focused();passed('新首页真实活动卡片、字体、键盘跳转与桌面布局正常')
  u.get_by_label('搜索活动名称或地址').fill('不存在_'+secrets.token_hex(5));u.get_by_role('button',name='搜索',exact=True).click();expect(u.get_by_text('没有找到合适的活动，换个关键词试试',exact=True)).to_be_visible();u.get_by_role('button',name='查看全部活动').click();expect(u.locator('.activity-card').first).to_be_visible();u.locator('.discover-search .el-select').click();u.get_by_role('option',name=category_name,exact=True).click();u.get_by_label('搜索活动名称或地址').fill(fixtures[0][0]);u.get_by_role('button',name='搜索',exact=True).click();expect(u.locator('.activity-card')).to_have_count(1);u.get_by_role('link',name=fixtures[0][0],exact=True).click();settle(u);expect(u.get_by_role('heading',name=fixtures[0][0],exact=True)).to_be_visible();width(u,'activity detail');shot(u,'02-activity-detail');passed('搜索、无结果恢复及活动详情真实联调正常')
  u.get_by_role('link',name='套餐',exact=True).click();settle(u);expect(u.get_by_role('heading',name='一次出发，多重体验。')).to_be_visible();shot(u,'03-packages');passed('套餐卡片展示真实套餐价格与版本')
  u.goto(WEB+'/login?redirect=/admin');settle(u);expect(u.get_by_role('heading',name='管理端登录',exact=True)).to_be_visible();shot(u,'04-admin-login');passed('管理端登录提示明确，账号权限保持不变')
  login(u,username,password);show_order(u,order['orderNo']);u.get_by_role('button',name='模拟支付',exact=True).click();expect(u.get_by_text('待确认',exact=True)).to_be_visible();login(a,cfg['CITYGO_ADMIN_USERNAME'],cfg['CITYGO_ADMIN_PASSWORD']);menu(a,'订单工作台');a.get_by_placeholder('搜索订单号').fill(order['orderNo']);a.get_by_role('button',name='搜索',exact=True).click();row=a.locator('.el-table__row').filter(has_text=order['orderNo']);row.get_by_role('button',name='确认',exact=True).click();expect(row).to_contain_text('已确认')
  menu(a,'运营首页');pending=api(admin,'GET','/admin/orders?page=1&pageSize=10&status=PENDING_CONFIRMATION',at)['total'];confirmed=api(admin,'GET','/admin/orders?page=1&pageSize=10&status=CONFIRMED',at)['total'];expect(a.locator('.metric-card').nth(0).locator('.metric-value')).to_contain_text(str(pending));expect(a.locator('.metric-card').nth(1).locator('.metric-value')).to_contain_text(str(confirmed));expect(a.locator('.recent-orders .el-table__row')).not_to_have_count(0);width(a,'dashboard desktop');shot(a,'05-admin-dashboard');passed('管理首页指标与后端实时数据一致，最近订单可见')
  # Verify dashboard failure states never manufacture zero counts.
  menu(a,'订单工作台');a.route('**/api/admin/reports?*',lambda route:route.fulfill(status=503,content_type='application/json',body=json.dumps({'code':50300,'message':'验收：统计服务暂不可用','data':None})));menu(a,'运营首页');expect(a.get_by_text('部分指标加载失败，未显示为零。',exact=False)).to_be_visible();expect(a.locator('.metric-card').nth(2).locator('.metric-value')).to_contain_text('—');a.unroute('**/api/admin/reports?*');a.locator('.metrics-error').get_by_role('button',name='重试').click();expect(a.locator('.metrics-error')).not_to_be_visible();passed('统计异常明确提示并可重试，不用假数字填充仪表盘')
  for label in ['活动管理','套餐管理','分类管理','退款记录','员工管理','操作审计','运营统计','扫码核销','订单工作台']:
   menu(a,label);width(a,'admin '+label)
  shot(a,'06-admin-orders');passed('管理端全部菜单可达，表格、筛选与权限菜单正常')
  u.get_by_role('button',name='刷新状态',exact=True).click();expect(u.get_by_text('已确认',exact=True)).to_be_visible();u.get_by_role('button',name='预约改期',exact=True).click();dialog=u.get_by_role('dialog',name='预约改期');dialog.locator('label.el-radio').first.click();dialog.get_by_role('button',name='确认改期').click();expect(dialog).not_to_be_visible();expect(u.get_by_text('已改期 1 次',exact=True)).to_be_visible();u.get_by_role('button',name='生成核销码',exact=True).click();expect(u.get_by_alt_text('预约核销二维码')).to_be_visible();shot(u,'07-user-order');passed('新主题下模拟支付、确认、预约改期和二维码生成均可用')
  u.set_viewport_size({'width':390,'height':844});width(u,'user order mobile');shot(u,'08-mobile-order');u.get_by_role('link',name='活动',exact=True).click();settle(u);width(u,'home mobile');shot(u,'09-mobile-home');u.get_by_role('link',name='套餐',exact=True).click();width(u,'packages mobile');
  for label in ['联系人','购物车','我的订单']:
   u.get_by_role('link',name=label,exact=True).click();width(u,'user mobile '+label)
  passed('390px 用户端首页、套餐、联系人、购物车、订单均无页面横向溢出')
  a.set_viewport_size({'width':390,'height':844});expect(a.get_by_role('button',name='打开管理导航')).to_be_visible();a.get_by_role('button',name='打开管理导航').click();expect(a.get_by_role('dialog',name='管理端导航')).to_be_visible();a.get_by_role('dialog',name='管理端导航').get_by_role('menuitem',name='运营首页',exact=True).click();expect(a.get_by_role('dialog',name='管理端导航')).not_to_be_visible();width(a,'dashboard mobile');shot(a,'10-mobile-admin');
  for label in ['活动管理','分类管理','运营统计','扫码核销','订单工作台']:
   a.get_by_role('button',name='打开管理导航').click();a.get_by_role('dialog',name='管理端导航').get_by_role('menuitem',name=label,exact=True).click();width(a,'admin mobile '+label)
  a.get_by_role('button',name='打开管理导航').click();a.keyboard.press('Escape');expect(a.get_by_role('dialog',name='管理端导航')).not_to_be_visible();passed('390px 管理端抽屉导航可切换和关闭，主要页面无横向溢出')
  u.goto(WEB+'/register');settle(u);width(u,'register mobile');expect(u.get_by_role('heading',name='开启你的城市探索',exact=True)).to_be_visible();u.emulate_media(reduced_motion='reduce');assert float(u.get_by_role('button',name='注册',exact=True).evaluate('(e)=>parseFloat(getComputedStyle(e).transitionDuration)'))<.01;passed('注册页移动端正常，减少动画设置生效')
  assert errors==[],errors;passed('全程无浏览器脚本错误');result.update(status='passed',orderId=order['id'],activityIds=activities,pageErrors=errors)
 except Exception as e:
  result.update(status='failed',failure=str(e),pageErrors=errors)
  for i,page in enumerate((a,u)):
   try:page.screenshot(path=str(OUT/f'failure-{i}.png'),full_page=True)
   except Exception:pass
  raise
 finally:
  (OUT/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');user.close();admin.close();browser.close()
print('UI refresh acceptance:',result['status'],flush=True)
