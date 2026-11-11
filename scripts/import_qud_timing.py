#!/usr/bin/env python3
"""Import pinned QUD v4.0.0 timings with their exact paired chapter recordings.

Timestamps are never interpolated or borrowed between reciters. Source word indices
must match the bundled study text, after removing orthographic marks only. Any
ambiguous word mapping, missing verse or invalid/overlapping interval falls back to
Nuraya's existing individual-verse audio with verse-level highlighting.
Usage: python3 scripts/import_qud_timing.py /path/to/downloaded/release-assets
"""
import argparse, gzip, hashlib, json, unicodedata, zipfile
from pathlib import Path
from urllib.parse import urlparse
ROOT=Path(__file__).resolve().parents[1]
MAPPING={
 'ar.abdulsamad':'abdulbasit_abdulsamad_tarteel',
 'ar.abdullahbasfar':'abdullah_basfar_qdc',
 'ar.abdurrahmaansudais':'abdulrahman_al_sudais_tarteel',
 'ar.shaatree':'abu_bakr_al_shatri_tarteel',
 'ar.ahmedajamy':'ahmed_al_ajmi_qdc',
 'ar.hanirifai':'hani_al_rifai_qdc_128k',
 'ar.hudhaify':'ali_al_huthaifi_mp3quran',
 'ar.husary':'mahmoud_khalil_al_husary_qdc_128k',
 'ar.husarymujawwad':'mahmoud_khalil_al_husary_mujawwad_tarteel',
 'ar.mahermuaiqly':'maher_al_muaiqly_tarteel',
 'ar.saoodshuraym':'saud_al_shuraim_mp3quran',
}
ALLOWED={'download.quranicaudio.com','audio-cdn.tarteel.ai','cdn.mp3quran.net'}
def sha(b):return hashlib.sha256(b).hexdigest()
def normalize(t):
 # Only compare the consonantal orthography; never alter the displayed Quran.
 return ''.join(c for c in unicodedata.normalize('NFKD',t.replace('ٱ','ا').replace('ى','ي')) if unicodedata.category(c).startswith('L') and c!='ـ')
def main(directory):
 script_bytes=(directory/'script.json').read_bytes()
 assert sha(script_bytes)=='19d5694b057dc68c3811e28f3ad1d58c0f07021a0c67a85cd25619ece7a9bf86'
 script=json.loads(script_bytes)
 studies={s:json.loads((ROOT/f'app/src/main/assets/study/{s}.json').read_text())['verses'] for s in range(1,115)}
 source_words={}
 for key,word in script.items():
  if "\u06dd" not in word["text"]:source_words.setdefault((int(word["surah"]),int(word["ayah"])),[]).append(word)
 mapping_ok={}
 for s,verses in studies.items():
  for v in verses:
   a=v['ayah'];words=v['words']
   source=source_words.get((s,a),[])
   source.sort(key=lambda w:int(w['word']))
   mapping_ok[s,a]=len(words)==len(source) and all(w['position']==int(q['word']) and normalize(w['arabic'])==normalize(q['text']) for w,q in zip(words,source))
 records=[]
 for rid,slug in MAPPING.items():
  archive=directory/(slug+'.zip');z=zipfile.ZipFile(archive)
  catalog=json.loads(z.read('catalog.json'));timings=json.loads(gzip.decompress(z.read('word_timestamps.json.gz')))
  assert catalog['slug']==slug and timings['_meta']['slug']==slug
  assert timings['_meta']['script_sha256']==sha(script_bytes) and timings['_meta']['units']=='ms'
  assert catalog['riwayah']=='hafs_an_asim'
  assert not catalog['audio'].get('chapter_offsets_ms'), 'Offsets require a separate validated adapter'
  rows={}
  for ref,start,end,canonical,silence,words in timings['rows']:
   if canonical:
    assert ref not in rows
    rows[ref]=(start,end,words)
  dest=ROOT/'app/src/main/assets/audio_timing'/slug;dest.mkdir(parents=True,exist_ok=True)
  entry={'reciterId':rid,'slug':slug,'archiveSha256':sha(archive.read_bytes()),'sourceUrl':f'https://github.com/QUD-Technologies/quranic-universal-audio/releases/download/v4.0.0/{slug}.zip','chapters':[]}
  for s,verses in studies.items():
   url=catalog['audio']['chapter_urls'].get(str(s));parsed=urlparse(url or '')
   assert parsed.scheme=='https' and parsed.hostname in ALLOWED and parsed.path.endswith('.mp3')
   result=[]
   for v in verses:
    a=v['ayah'];row=rows.get(f'{s}:{a}');start,end,words=row or (0,1,[])
    positions={w['position'] for w in v['words']}
    usable=bool(row and words and mapping_ok[s,a] and 0<=start<end and end-start>=300)
    usable=usable and {w[0] for w in words}==positions and all(p in positions and start<=b<e<=end for p,b,e in words)
    usable=usable and all(x[2]<=y[1] for x,y in zip(words,words[1:]))
    # Keep invalid source rows in the provenance archive, not in runtime segments.
    result.append({'ayah':a,'clipFrom':start if start>=0 and end>start else 0,'clipTo':end if start>=0 and end>start else 1,'segments':words if usable else [],'usable':bool(usable)})
   for left,right in zip(result,result[1:]):
    if left['usable'] and right['usable'] and left['clipTo']>right['clipFrom']:
     left['usable']=right['usable']=False;left['segments']=[];right['segments']=[]
   payload={'surah':s,'reciterId':rid,'sourceSlug':slug,'source':'QUD v4.0.0 · '+parsed.hostname,'audioUrl':url,'verses':result}
   b=(json.dumps(payload,separators=(',',':'))+'\n').encode();(dest/f'{s}.json').write_bytes(b)
   entry['chapters'].append({'surah':s,'sha256':sha(b),'audioUrl':url,'safeVerses':sum(v['usable'] for v in result),'verseCount':len(result)})
  entry['safeVerses']=sum(c['safeVerses'] for c in entry['chapters']);records.append(entry);print(rid,entry['safeVerses'],flush=True)
 metadata={'release':'v4.0.0','license':'CC BY 4.0 (timing data); recording rights remain with upstream sources and reciters','attribution':'QUD Technologies · Quranic Universal Audio; DigitalKhatt v2 script via QUL/Tarteel','scriptSha256':sha(script_bytes),'normalization':'NFKD; strip orthographic marks/tatweel; map alif wasla to alif and unpointed ya to ya for comparison only','unsupportedWordSync':['ar.aymanswoaid','ar.ibrahimakhbar','ar.muhammadayyoub','ar.muhammadjibreel','ar.parhizgar'],'reciters':records}
 (ROOT/'docs/sources/qud-word-timing.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
 p=ROOT/'app/src/main/assets/verse_reciters.json';catalog=json.loads(p.read_text())
 for r in catalog['reciters']:
  if r['id'] in MAPPING:r.update(supportsWordSync=True,timingFolder=MAPPING[r['id']])
 p.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('directory',type=Path);main(parser.parse_args().directory)
