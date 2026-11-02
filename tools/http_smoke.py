"""Runs the packaged application with an isolated in-memory DB; exercises real HTTP."""
import concurrent.futures,http.cookiejar,json,pathlib,subprocess,time,urllib.error,urllib.request,sys
root=pathlib.Path(__file__).resolve().parents[1];jar=pathlib.Path(sys.argv[1]).resolve();port=18080;base=f'http://127.0.0.1:{port}'
class Client:
 def __init__(self):self.opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));self.csrf=None
 def call(self,path,body=None,method=None):
  method=method or ('POST' if body is not None else 'GET');headers={'Accept':'*/*'}
  if method!='GET':
   if not self.csrf:self.csrf=self.call('/api/csrf')[1]
   headers[self.csrf['headerName']]=self.csrf['token']
  data=json.dumps(body).encode() if body is not None else None
  if data is not None:headers['Content-Type']='application/json'
  req=urllib.request.Request(base+path,data=data,headers=headers,method=method)
  try:r=self.opener.open(req,timeout=15)
  except urllib.error.HTTPError as e:r=e
  raw=r.read();ct=r.headers.get('Content-Type','');return r.status,json.loads(raw) if 'application/json' in ct and raw else raw.decode(errors='replace')
 def login(self,email,password):
  status,data=self.call('/api/login',{'email':email,'password':password});assert status==200,(status,data);self.csrf=None
log_path=pathlib.Path(__import__('tempfile').gettempdir())/'runway-http-server.log';log=open(log_path,'w');process=subprocess.Popen([str(pathlib.Path(__import__('shutil').which('java')).resolve()),'-jar',str(jar),'--server.address=127.0.0.1',f'--server.port={port}','--spring.datasource.url=jdbc:h2:mem:runway-http;MODE=MySQL;DB_CLOSE_DELAY=-1'],cwd=root,stdout=log,stderr=log)
results=[]
def ok(label):results.append(label);print('PASS',label,flush=True)
try:
 c=Client()
 for attempt in range(120):
  if process.poll() is not None:raise RuntimeError('Server stopped: '+log_path.read_text()[-3000:])
  try:
   status,data=c.call('/api/products')
   if status==200 and isinstance(data,list) and len(data)==11:break
   time.sleep(.25)
  except (OSError,urllib.error.URLError) as error:
   if attempt in (20,80):print('Startup connection:',repr(error),flush=True)
   time.sleep(.25)
 else:raise RuntimeError('Server startup timed out')
 status,products=c.call('/api/products');assert status==200 and len(products)==11;ok('packaged JAR starts and serves 11 products')
 for page in ['/','/shop','/product/'+products[0]['slug'],'/experience/runway.html','/experience/nike-orbit.html','/experience/scene-rail.html']:
  reply=c.call(page);assert reply[0]==200,(page,reply[0],str(reply[1])[:300])
 ok('all six representative frontend routes return HTTP 200')
 image=products[0]['image'];assert c.call(image)[0]==200;ok('local product media is served')
 c.login('demo@runway.local','Demo@12345');assert c.call('/api/me')[1]['user']['role']=='CUSTOMER';ok('real CSRF + session login')
 assert c.call('/api/admin/products')[0]==403;ok('customer denied admin API')
 admin=Client();admin.login('admin@runway.local','Admin@12345');variant=products[0]['variants'][0];vid=variant['id'];size=variant['size'];price=variant['price']
 assert admin.call('/api/admin/variants/'+str(vid),{'size':size,'price':100000,'stock':1},'PUT')[0]==200
 buyers=[Client(),Client()]
 for n,b in enumerate(buyers):
  email=f'http-race-{n}@example.test';password='HttpFixture@12345'
  assert b.call('/api/register',{'name':'HTTP fixture','email':email,'password':password})[0]==200
  b.login(email,password);assert b.call('/cart/add.js',{'items':[{'id':vid,'quantity':1}]})[0]==200
 payload=lambda n:{'recipient':'Khách hàng kiểm thử','phone':'0912345678','address':'Địa chỉ mẫu phục vụ kiểm thử Hà Nội','note':'Dữ liệu kiểm thử','requestId':f'concurrent-checkout-{n}-0001'}
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
  replies=list(pool.map(lambda i:buyers[i].call('/api/checkout',payload(i)),range(2)))
 assert sorted(x[0] for x in replies)==[200,400],replies
 ok('two simultaneous buyers for the final item: exactly one order succeeds')
 winner=next(i for i,r in enumerate(replies) if r[0]==200);order=replies[winner][1]['order'];assert order['subtotal']==100000 and order['shipping']==30000 and order['total']==130000;ok('server-side price and shipping calculation')
 new_vid=products[1]['variants'][0]['id'];assert buyers[winner].call('/cart/add.js',{'id':new_vid,'quantity':2})[0]==200
 again=buyers[winner].call('/api/checkout',payload(winner));assert again[0]==200 and again[1]['order']['id']==order['id'];ok('HTTP retry returns existing order without duplicate stock deduction');assert buyers[winner].call('/api/cart')[1]['item_count']==2;ok('retry preserves a new persistent cart')
 assert buyers[winner].call('/api/orders/'+str(order['id'])+'/cancel',{})[0]==200
 fresh=admin.call('/api/admin/products')[1];stock=next(v['stock'] for p in fresh for v in p['variants'] if v['id']==vid);assert stock==1;ok('cancel restores stock exactly once')
 assert buyers[winner].call('/api/orders/'+str(order['id'])+'/cancel',{})[0]==400;ok('repeated cancellation is rejected')
 assert admin.call('/api/admin/variants/'+str(vid),{'size':size,'price':price,'stock':20},'PUT')[0]==200
 ok('admin updates price and stock via real authenticated HTTP')
 report={'passed':len(results),'checks':results,'database':'isolated H2 in-memory','jar':jar.name,'browserVisualQA':'separate Playwright report in docs/evidence/playwright-results.json','runAt':__import__('datetime').datetime.now(__import__('datetime').timezone.utc).isoformat()}
 (root/'docs/http-smoke-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print('ALL',len(results),'HTTP CHECKS PASSED',flush=True)
finally:
 process.terminate()
 try:process.wait(timeout=10)
 except subprocess.TimeoutExpired:process.kill();process.wait()
 log.close()
