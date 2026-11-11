#!/usr/bin/env python3
"""Offline integrity/boundary audit of imported reciter-specific QUD timing assets."""
import hashlib,json
from pathlib import Path
from import_qud_timing import MAPPING
ROOT=Path(__file__).resolve().parents[1]
def main():
 m=json.loads((ROOT/'docs/sources/qud-word-timing.json').read_text())
 studies={s:json.loads((ROOT/f'app/src/main/assets/study/{s}.json').read_text())['verses'] for s in range(1,115)}
 counts=[]
 for reciter in m['reciters']:
  assert MAPPING[reciter['reciterId']]==reciter['slug']
  assert len(reciter['chapters'])==114
  safe=0;total=0
  for c in reciter['chapters']:
   s=c['surah'];p=ROOT/f"app/src/main/assets/audio_timing/{reciter['slug']}/{s}.json";raw=p.read_bytes();d=json.loads(raw)
   assert hashlib.sha256(raw).hexdigest()==c['sha256']
   assert d['reciterId']==reciter['reciterId'] and d['sourceSlug']==reciter['slug'] and d['audioUrl']==c['audioUrl']
   assert [v['ayah'] for v in d['verses']]==list(range(1,len(studies[s])+1))
   for v,study in zip(d['verses'],studies[s]):
    total+=1
    if not v['usable']:assert not v['segments'];continue
    words=v['segments'];safe+=1
    assert {w[0] for w in words}=={w['position'] for w in study['words']}
    assert all(v['clipFrom']<=a<b<=v['clipTo'] for _,a,b in words)
    assert all(x[2]<=y[1] for x,y in zip(words,words[1:]))
   assert c['safeVerses']==sum(v['usable'] for v in d['verses'])
  assert total==6236 and safe==reciter['safeVerses'] and safe>6200
  counts.append({'reciterId':reciter['reciterId'],'usableVerses':safe,'fallbackVerses':total-safe})
 out=ROOT/'docs/audit/1.5.0';out.mkdir(parents=True,exist_ok=True)
 (out/'word-timing.json').write_text(json.dumps({'scope':'Source hashes, identity mapping, canonical word indices and boundaries; not human listening to every word.','additionalReciters':counts,'unsupportedPerWord':m['unsupportedWordSync']},indent=2)+'\n')
 print(f'PASS: {len(counts)} additional reciters; {sum(x["usableVerses"] for x in counts)} usable verse clips; no interpolated timings.')
if __name__=='__main__':main()
