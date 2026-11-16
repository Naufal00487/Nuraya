from pathlib import Path
import json,re,urllib.request,concurrent.futures,datetime
root=Path(__file__).resolve().parents[3];assets=root/'app/src/main/assets';out=Path(__file__).resolve().parent
selected=re.findall(r'Selection\("([^"]+)", "([^"]+)", "([^"]+)"\)',(root/'app/src/main/java/com/nuraya/app/media/CuratedReciters.kt').read_text())
verse={r['id']:r for r in json.loads((assets/'verse_reciters.json').read_text())['reciters']}
references={(v['surah'],v['ayah']):v['number'] for v in json.loads((assets/'quran.json').read_text())['verses']}
assert len(references)==6236 and references[114,6]==6236 and references[112,4]==6225
jobs=[]
for chapter,id,name in selected:
 if id in ['ar.alijaber','ar.abdullahalmatrood']:continue
 r=verse[id]
 for s,a in [(1,1),(2,6),(36,1),(67,1),(112,4),(114,6)]:
  url=(r['ayahBaseUrl'].rstrip('/')+f'/{s:03d}{a:03d}.mp3') if r.get('ayahBaseUrl') else f"https://cdn.islamic.network/quran/audio/{r['bitrateKbps']}/{id}/{references[s,a]}.mp3"
  jobs.append(dict(id=id,name=name,kind='ayah',reference=f'{s}:{a}',url=url))
 if r.get('timingFolder'):
  for s in [1,36,114]:
   t=json.loads((assets/'audio_timing'/r['timingFolder']/f'{s}.json').read_text())
   jobs.append(dict(id=id,name=name,kind='timed',reference=str(s),url=t['audioUrl']))
def probe(row):
 try:
  req=urllib.request.Request(row['url'],headers={'User-Agent':'Nuraya/1.7.1 audio availability audit','Range':'bytes=0-8191'})
  with urllib.request.urlopen(req,timeout=25) as r:
   sample=r.read(8192);mime=r.headers.get('Content-Type','');audio=sample[:3]==b'ID3' or any(sample[i]==255 and sample[i+1]&224==224 for i in range(min(len(sample)-1,512)))
   return row|dict(status=r.status,mime=mime,audio=audio,bytesRead=len(sample),ok=r.status in [200,206] and audio)
 except Exception as e:return row|dict(ok=False,error=str(e))
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:results=list(ex.map(probe,jobs))
report={'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'HTTP range probes, not listening or device playback. Six canonical ayah URLs per retained reciter and three paired timing sources where available.','probes':results}
(out/'audio-probes.json').write_text(json.dumps(report,indent=2)+'\n')
print('probes',len(results),'ok',sum(r['ok'] for r in results),flush=True)
for id in dict.fromkeys(r['id'] for r in results):
 failures=[r for r in results if r['id']==id and not r['ok']]
 if failures:print(id,[(r['kind'],r['reference'],r.get('status',r.get('error'))) for r in failures],flush=True)
