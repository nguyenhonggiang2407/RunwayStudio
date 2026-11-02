#!/usr/bin/env python3
"""Audit resolved Maven runtime dependencies using OSV. Network errors fail closed."""
import json,re,sys,urllib.request,datetime,pathlib,concurrent.futures
source=pathlib.Path('target/runtime-dependencies.txt').read_text();source=re.sub(r'\x1b\[[0-9;]*m','',source)
packages=[]
for line in source.splitlines():
    f=line.strip().split()[0].split(':') if line.strip() else []
    if len(f)>=5 and f[2]=='jar' and f[-1] in ('compile','runtime'):packages.append({'package':{'ecosystem':'Maven','name':':'.join(f[:2])},'version':f[-2]})
if not packages:raise SystemExit('No runtime dependencies; refusing an empty audit.')
def request(url,payload=None):
    req=urllib.request.Request(url,data=None if payload is None else json.dumps(payload).encode(),headers={'Content-Type':'application/json','User-Agent':'runway-studio-dependency-audit/2.0'})
    with urllib.request.urlopen(req,timeout=45) as r:return json.load(r)
results=request('https://api.osv.dev/v1/querybatch',{'queries':packages})['results'];ids=sorted({v['id'] for r in results for v in r.get('vulns',[])})
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:details=dict(zip(ids,pool.map(lambda i:request('https://api.osv.dev/v1/vulns/'+i),ids)))
triage=json.loads(pathlib.Path('docs/security-triage.json').read_text());valid=triage['expiresAt']>=datetime.date.today().isoformat();findings=[]
for p,r in zip(packages,results):
    for v in r.get('vulns',[]):
        d=details[v['id']];f={'package':p['package']['name'],'version':p['version'],'id':v['id'],'aliases':d.get('aliases',[]),'severity':d.get('database_specific',{}).get('severity') or d.get('ecosystem_specific',{}).get('severity') or 'UNKNOWN','summary':d.get('summary',''),'url':'https://osv.dev/vulnerability/'+v['id']}
        f['assessment']=next((a for a in triage['assessments'] if valid and all(a[k]==f[k] for k in ('id','package','version')) and a['status']=='not_affected'),None);findings.append(f)
report={'runAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source':'https://api.osv.dev/v1/querybatch','scope':'Resolved Maven runtime dependencies; excludes undisclosed issues, application logic and container images. Raw findings remain visible even when reviewed.','packagesScanned':len(packages),'findings':findings}
out=pathlib.Path('docs/evidence/dependency-audit.json');out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n');unreviewed=[f for f in findings if f['severity'].upper() in ('HIGH','CRITICAL','UNKNOWN') and not f['assessment']];print(json.dumps({'packagesScanned':len(packages),'rawFindings':len(findings),'unreviewedHighCriticalUnknown':len(unreviewed)}))
if '--fail-on-high' in sys.argv and unreviewed:raise SystemExit(1)
