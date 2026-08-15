// SPK-OS 研发 UI 无头验证台
// 用系统 chrome(puppeteer-core) 登录 yudao SPA，逐页加载，抓 console/page 错误 + 失败网络请求。
// 用法: node verify-spk.mjs [routePath...]  默认验证全部 spk 路由
import puppeteer from './yudao-ui/yudao-ui-admin-vue3/node_modules/puppeteer-core/lib/puppeteer/puppeteer-core.js'

const BASE = 'http://localhost:5173'
const CHROME = '/usr/bin/google-chrome'
const ALL_ROUTES = {
  '/spk/ipd-home': '研发管理(cockpit)',
  '/spk/projects': '项目管理',
  '/spk/ipd-monitor': '项目监控',
  '/spk/spk-agent/agent': '智能体',
  '/spk/ipd-cockpit': 'IPD监控台',
  '/spk/ipd-overview': 'IPD总览',
  '/spk/ipd-team': '团队与智能体',
  '/spk/ipd-approval': '审批决策',
}
const argRoutes = process.argv.slice(2)
const routes = argRoutes.length ? Object.fromEntries(argRoutes.map(r=>[r,ALL_ROUTES[r]||r])) : ALL_ROUTES

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  args: ['--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--disable-dev-shm-usage','--window-size=1600,1000'],
})
const page = await browser.newPage()
await page.setViewport({width:1600,height:1000})

const errors = []  // {route, kind, text}
const addErr = (route,kind,text)=>errors.push({route,kind,text:text.slice(0,300)})

// ---- 登录：直接调后端 /admin-api/system/auth/login 取 token，按 web-storage-cache
// 序列化格式注入 localStorage（{c,e,v} 三元组，v 为 JSON.stringify(value)），
// 随后 goto /index 触发路由守卫拉取 permission-info 装配动态路由，再逐页验证。
const FAR_FUTURE = 253402300799000  // new Date("Fri, 31 Dec 9999 23:59:59 UTC").getTime()
function wscSet(value){ return JSON.stringify({c: Date.now(), e: FAR_FUTURE, v: JSON.stringify(value)}) }
async function login(){
  await page.goto(BASE+'/', {waitUntil:'domcontentloaded', timeout:30000})
  // 1. 取 tenantId + 登录拿 token（在页面上下文 fetch 走同源策略，baseURL 直连后端）
  const auth = await page.evaluate(async ()=>{
    const API='http://192.168.56.101:48080/admin-api'
    const tid = await fetch(API+'/system/tenant/get-id-by-name?name=%E8%8A%8B%E9%81%93%E6%BA%90%E7%A0%81',{headers:{'tenant-id':'1'}}).then(r=>r.json()).then(j=>j.data)
    const t = await fetch(API+'/system/auth/login',{method:'POST',headers:{'Content-Type':'application/json','tenant-id':String(tid),'client-code':'sdk'},body:JSON.stringify({username:'admin',password:'admin123',tenantId:tid})}).then(r=>r.json()).then(j=>j.data)
    return {tid, t}
  })
  console.log('  login api:', auth.t?.userId, 'token=', auth.t?.accessToken?.slice(0,8)+'...')
  // 2. 注入 ACCESS_TOKEN / REFRESH_TOKEN / tenantId（web-storage-cache 格式）
  await page.evaluate((auth, wscFarFuture)=>{
    const enc = (v)=>JSON.stringify({c:Date.now(), e:wscFarFuture, v:JSON.stringify(v)})
    localStorage.setItem('ACCESS_TOKEN', enc(auth.t.accessToken))
    localStorage.setItem('REFRESH_TOKEN', enc(auth.t.refreshToken))
    localStorage.setItem('tenantId', enc(auth.tid))
  }, auth, FAR_FUTURE)
  // 3. 触发路由守卫：goto /index，守卫见 token 拉 permission-info 装配动态路由
  await page.goto(BASE+'/index', {waitUntil:'networkidle2', timeout:30000}).catch(e=>console.log('  goto /index warn:',e.message.slice(0,80)))
  await new Promise(r=>setTimeout(r,2500))
  console.log('  after auth url:', page.url())
  return page.url()
}

await login()

// ---- 逐页验证 ----
for (const [route,name] of Object.entries(routes)){
  console.log(`\n=== ${name} ${route} ===`)
  const consoleMsgs=[], pageErrs=[], reqFails=[]
  const onConsole = m => { if(m.type()==='error') consoleMsgs.push(m.text()) }
  const onPageErr = e => pageErrs.push(e.message)
  const onReqFail = r => {
    const s=r.response()?.status(); if(s && (s>=400)) reqFails.push(`${s} ${r.url()}`)
  }
  page.on('console', onConsole)
  page.on('pageerror', onPageErr)
  page.on('response', ()=>{})  // noop, failures via requestfailed below
  page.on('requestfailed', r=>reqFails.push('FAIL '+r.url().slice(0,120)))
  // 用 response 抓 4xx/5xx
  const onResp = r=>{const s=r.status(); if(s>=400) reqFails.push(`${s} ${r.url().slice(0,120)}`)}
  page.on('response', onResp)

  try{
    await page.goto(BASE+route, {waitUntil:'domcontentloaded', timeout:20000})
  }catch(e){ addErr(route,'goto-timeout',e.message); console.log('  goto timeout:', e.message.slice(0,100)) }
  await new Promise(r=>setTimeout(r,3000))  // 让异步请求完成
  // 取页面可见状态
  const state = await page.evaluate(()=>{
    const spin=document.querySelector('.el-loading-mask');
    const empty=document.querySelector('.el-empty');
    return {
      title: document.title,
      hasLoadingMask: !!spin && getComputedStyle(spin).display!=='none',
      hasEmpty: !!empty,
      bodyTextLen: document.body.innerText.length,
      h1: document.querySelector('h1,h2,.spk-home__title,[class*=title]')?.innerText?.slice(0,40),
    }
  }).catch(()=>({title:'eval-fail'}))
  console.log('  state:', JSON.stringify(state))
  consoleMsgs.forEach(t=>{console.log('  CONSOLE-ERR:',t); addErr(route,'console',t)})
  pageErrs.forEach(t=>{console.log('  PAGE-ERR:',t); addErr(route,'pageerror',t)})
  // 过滤非致命噪音：外网头像/图片失败、字体 404 等
  const noise = /yudao\.iocoder\.cn|\.png$|\.jpg$|\.jpeg$|\.svg$|\.woff2?|fonts\.googleapis|unpkg\.com/i
  reqFails.filter(t=>!noise.test(t)).forEach(t=>{console.log('  NET-FAIL:',t); addErr(route,'net',t)})
  page.off('console', onConsole); page.off('pageerror', onPageErr); page.off('response', onResp); page.off('requestfailed',r=>{})
}

await browser.close()
console.log('\n========== 汇总 ==========')
if(!errors.length){console.log('✅ 全部页面无 console/page 错误、无 4xx/5xx 失败请求')}
else{ console.log(`❌ 共 ${errors.length} 处:`); for(const e of errors) console.log(`  [${e.kind}] ${e.route}: ${e.text}`) }
