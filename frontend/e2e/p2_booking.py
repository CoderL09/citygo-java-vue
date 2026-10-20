"""Local-only P2 browser acceptance. Creates uniquely named fixtures; never deletes existing data."""
import json
import os
import re
import secrets
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright, expect
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

ROOT = Path(os.environ.get("CITYGO_PROJECT_ROOT", str(Path(__file__).resolve().parents[2])))
WEB = os.environ.get("CITYGO_E2E_URL", "http://127.0.0.1:5174")
assert urlparse(WEB).hostname in {"127.0.0.1", "localhost"}, "Local browser tests only"
OUT = ROOT / "demo-gifs" / "p2-frames"
OUT.mkdir(parents=True, exist_ok=True)
cfg = {}
raw_env = (ROOT / ".env").read_bytes()
try:
    env_text = raw_env.decode("utf-8-sig")
except UnicodeDecodeError:
    env_text = raw_env.decode("gb18030")
for line in env_text.splitlines():
    if line and not line.lstrip().startswith("#") and "=" in line:
        k, v = line.split("=", 1)
        cfg[k.strip()] = v.strip()
marker = "P2_" + datetime.now().strftime("%m%d_%H%M%S")
username = "p2_" + secrets.token_hex(5)
password = secrets.token_urlsafe(16)
errors = []
frames = []
result = {"marker": marker, "checks": [], "status": "running"}

def check(name):
    result["checks"].append(name)
    print("PASS", name, flush=True)

def shot(page, name):
    page.wait_for_load_state("networkidle")
    # Capture the settled product state, without accumulated short-lived success toasts.
    page.wait_for_function("document.querySelectorAll('.el-message').length === 0")
    file = OUT / (name + ".png")
    page.screenshot(path=str(file), full_page=True)
    frames.append(str(file))

def api(ctx, method, path, token=None, data=None):
    headers = {"Authorization": "Bearer " + token} if token else {}
    r = ctx.request.fetch(WEB + "/api" + path, method=method, headers=headers, data=data)
    body = r.json()
    assert r.status == 200 and body.get("code") == 0, (path, r.status, body.get("message"))
    return body["data"]

def login(page, user, pwd):
    page.goto(WEB + "/login")
    page.wait_for_load_state("networkidle")
    assert "登录" in page.get_by_role("button").all_text_contents()
    page.get_by_label("用户名", exact=True).fill(user)
    page.get_by_label("密码", exact=True).fill(pwd)
    page.get_by_role("button", name="登录", exact=True).click()
    page.wait_for_url(lambda url: "/login" not in url)
    page.wait_for_load_state("networkidle")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True, channel="chrome")
    admin = browser.new_context(viewport={"width": 1440, "height": 1000})
    user = browser.new_context(viewport={"width": 1280, "height": 1000})
    admin_page, page = admin.new_page(), user.new_page()
    for tab in (admin_page, page):
        tab.on("pageerror", lambda e: errors.append(str(e)))
        tab.set_default_timeout(15000)
    try:
        auth = api(admin, "POST", "/auth/login", data={"username": cfg["CITYGO_ADMIN_USERNAME"], "password": cfg["CITYGO_ADMIN_PASSWORD"]})
        admin_token = auth["token"]
        activity_cat = api(admin, "POST", "/admin/categories", admin_token, {"categoryType": "ACTIVITY", "name": marker + "活动", "sortOrder": 0})["id"]
        package_cat_name = marker + "套餐"
        package_cat = api(admin, "POST", "/admin/categories", admin_token, {"categoryType": "PACKAGE", "name": package_cat_name, "sortOrder": 0})["id"]
        activity_names, activity_ids, session_ids = [], [], []
        start = (datetime.now(timezone.utc) + timedelta(days=10)).replace(hour=2, minute=0, second=0, microsecond=0)
        for i, price in enumerate((10000, 8000)):
            name = marker + ("博物馆" if i == 0 else "城市漫步")
            a = api(admin, "POST", "/admin/activities", admin_token, {"categoryId": activity_cat, "name": name, "address": "上海集合点" + str(i), "description": "P2 browser fixture"})
            when = start + timedelta(days=i)
            s = api(admin, "POST", f"/admin/activities/{a['id']}/sessions", admin_token, {"startTime": when.isoformat(), "endTime": (when + timedelta(hours=2)).isoformat(), "bookingDeadline": (when - timedelta(days=1)).isoformat(), "priceCents": price, "capacity": 20})
            activity_names.append(name); activity_ids.append(a["id"]); session_ids.append(s["id"])

        login(admin_page, cfg["CITYGO_ADMIN_USERNAME"], cfg["CITYGO_ADMIN_PASSWORD"])
        admin_page.get_by_role("menuitem", name="套餐管理", exact=True).click()
        admin_page.wait_for_load_state("networkidle")
        admin_page.get_by_role("button", name="新建套餐", exact=True).click()
        dialog = admin_page.get_by_role("dialog", name="新建套餐", exact=True)
        dialog.locator(".el-select").first.locator(".el-select__wrapper").click()
        admin_page.get_by_role("option", name=package_cat_name, exact=True).click()
        package_name = marker + "博物馆与城市漫步套餐"
        dialog.get_by_label("套餐名称", exact=True).fill(package_name)
        dialog.get_by_label("套餐介绍", exact=True).fill("固定两场次，每份各占一个名额")
        dialog.get_by_label("套餐价格（元）", exact=True).fill("150")
        dialog.locator(".el-select").last.locator(".el-select__wrapper").click()
        for name in activity_names:
            admin_page.get_by_role("option").filter(has_text=name).click()
        dialog.get_by_role("heading").click()
        with admin_page.expect_response(lambda r: r.url.endswith("/api/admin/packages") and r.request.method == "POST") as response:
            dialog.get_by_role("button", name="保存套餐", exact=True).click()
        saved = response.value.json()
        assert saved["code"] == 0, saved.get("message")
        package_id = saved["data"]["product"]["id"]
        expect(dialog).not_to_be_visible()
        row = admin_page.locator(".el-table__row").filter(has_text=package_name)
        expect(row).to_be_visible()
        row.get_by_role("button", name="上架", exact=True).click()
        expect(row).to_contain_text("在售")
        shot(admin_page, "01-package-published")
        check("运营页面创建并上架固定场次套餐")

        page.goto(WEB + "/register"); page.wait_for_load_state("networkidle")
        page.get_by_label("用户名（3-32 位字母数字下划线）", exact=True).fill(username)
        page.get_by_label("密码（至少 6 位）", exact=True).fill(password)
        page.get_by_label("昵称（可选）", exact=True).fill("P2 浏览器体验者")
        with page.expect_response(lambda r: r.url.endswith("/api/auth/register")) as response:
            page.get_by_role("button", name="注册", exact=True).click()
        registered = response.value.json()
        assert registered["code"] == 0
        user_token = registered["data"]["token"]
        page.wait_for_url(WEB + "/")
        page.get_by_role("link", name="联系人", exact=True).click()
        page.get_by_role("button", name="新增联系人", exact=True).click()
        d = page.get_by_role("dialog", name="新增联系人", exact=True)
        d.get_by_label("姓名", exact=True).fill("演示联系人")
        d.get_by_label("联系电话", exact=True).fill("13800000000")
        d.get_by_role("button", name="保存联系人", exact=True).click()
        expect(d).not_to_be_visible()
        expect(page.get_by_text("默认", exact=True)).to_be_visible()
        shot(page, "02-contact-default")
        check("用户注册并建立默认联系人")

        page.get_by_role("link", name="套餐", exact=True).click()
        page.locator(".package-card").filter(has_text=package_name).get_by_role("button", name="查看套餐").click()
        expect(page.get_by_role("heading", name=package_name, exact=True)).to_be_visible()
        page.get_by_role("spinbutton").fill("2")
        page.get_by_role("button", name="加入购物车", exact=True).click()
        expect(page.get_by_text("已加入购物车", exact=True)).to_be_visible()
        page.get_by_role("link", name="活动", exact=True).click()
        page.locator(".activity-card").filter(has_text=activity_names[0]).get_by_role("link", name="查看详情："+activity_names[0], exact=True).click()
        page.get_by_role("button", name="加入购物车", exact=True).click()
        page.get_by_role("link", name="购物车", exact=True).click()
        expect(page.locator(".row")).to_have_count(2)
        expect(page.locator(".checkout")).to_contain_text("400.00")
        shot(page, "03-mixed-cart")
        page.get_by_role("button", name="确认订单", exact=True).click()
        confirmation = page.get_by_role("dialog", name="确认预约订单", exact=True)
        expect(confirmation).to_contain_text("400.00")
        expect(confirmation.get_by_role("button", name="提交订单", exact=True)).to_be_enabled()
        shot(page, "04-server-preview")
        check("套餐两份与重复子场次活动混合购物车，服务端确认 400 元")

        attempts = []
        committed_id = []
        def lose_first_response(route):
            if route.request.method != "POST":
                route.continue_(); return
            attempts.append(route.request.headers.get("idempotency-key"))
            if len(attempts) == 1:
                response = route.fetch()
                body = response.json()
                assert body["code"] == 0, body.get("message")
                committed_id.append(body["data"]["order"]["id"])
                route.abort("failed")
            else:
                route.continue_()
        page.route(re.compile(r".*/api/orders$"), lose_first_response)
        confirmation.get_by_role("button", name="提交订单", exact=True).click()
        expect(confirmation).to_contain_text("未能确认下单结果")
        expect(confirmation.get_by_role("button", name="提交订单", exact=True)).to_be_enabled()
        shot(page, "05-response-lost-retry")
        confirmation.get_by_role("button", name="提交订单", exact=True).click()
        page.wait_for_url(re.compile(r".*/orders/\d+$"))
        assert len(attempts) == 2 and attempts[0] and attempts[0] == attempts[1]
        order_id = int(page.url.rsplit("/", 1)[1])
        assert order_id == committed_id[0]
        expect(page.get_by_text("待支付", exact=True)).to_be_visible()
        expect(page.locator(".total")).to_contain_text("400.00")
        shot(page, "06-order-snapshots")
        mine = api(user, "GET", "/orders", user_token)
        assert mine["total"] == 1
        assert api(user, "GET", "/cart", user_token) == []
        for i, expected in enumerate((17, 18)):
            sessions = api(admin, "GET", f"/admin/activities/{activity_ids[i]}/sessions", admin_token)
            assert next(s["available"] for s in sessions if s["id"] == session_ids[i]) == expected
        check("响应丢失后重试沿用原键与原订单，只生成一单，A/B 名额分别扣 3/2")

        contacts = api(user, "GET", "/contacts", user_token)
        api(user, "PUT", f"/contacts/{contacts[0]['id']}", user_token, {"name": "更新后的联系人", "phone": "13900000000", "defaultContact": True})
        api(admin, "PUT", f"/admin/activities/{activity_ids[0]}", admin_token, {"categoryId": activity_cat, "name": marker + "已修改活动", "address": "新地址", "description": "目录已变更"})
        order = api(user, "GET", f"/orders/{order_id}", user_token)
        assert order["order"]["contactName"] == "演示联系人"
        session_item = next(i for i in order["items"] if i["itemType"] == "SESSION")
        assert session_item["sessionSnapshot"]["activityName"] == activity_names[0]
        assert session_item["sessionSnapshot"]["address"] == "上海集合点0"
        check("目录与联系人修改后订单仍保存原始快照")

        page.reload(); page.wait_for_load_state("networkidle")
        expect(page).to_have_url(re.compile(r".*/login.*"))
        page.get_by_label("用户名", exact=True).fill(username)
        page.get_by_label("密码", exact=True).fill(password)
        page.get_by_role("button", name="登录", exact=True).click()
        page.wait_for_url(re.compile(r".*/orders/\d+$"))
        expect(page.get_by_text("演示联系人", exact=False)).to_be_visible()
        check("刷新需重新登录，重新读取订单仍为历史快照")
        page.set_viewport_size({"width": 390, "height": 844})
        page.wait_for_load_state("networkidle")
        dimensions = page.evaluate("({width:innerWidth, scrollWidth:document.documentElement.scrollWidth})")
        assert dimensions["width"] == 390 and dimensions["scrollWidth"] <= 391, dimensions
        shot(page, "07-mobile-order")
        check("390px 用户订单页面无页面级横向溢出")
        assert errors == [], errors
        result.update(status="passed", orderId=order_id, packageId=package_id, sessionIds=session_ids, frames=frames, pageErrors=errors)
    except Exception as exc:
        result.update(status="failed", failure=str(exc), pageErrors=errors)
        for i, tab in enumerate((admin_page, page)):
            try: tab.screenshot(path=str(OUT / f"failure-{i}.png"), full_page=True)
            except Exception: pass
        raise
    finally:
        (OUT / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        user.close(); admin.close(); browser.close()
print("P2 browser acceptance:", result["status"], flush=True)
