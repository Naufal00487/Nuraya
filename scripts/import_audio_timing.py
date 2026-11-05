#!/usr/bin/env python3
"""Import Quran.com's qari-matched chapter audio and word segments; never estimate timing.

Each imported timestamp keeps its paired audio_url. Unsupported/malformed segment
markers (such as a lone word index) are omitted, leaving those moments unhighlighted.
Run without --fetch for a reproducible offline structural/integrity verification.
"""
from concurrent.futures import ThreadPoolExecutor
import argparse
import hashlib
import json
from pathlib import Path
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'app/src/main/assets/audio_timing/alafasy'
SOURCE = ROOT / 'docs/sources/quran-com-alafasy-timing.json'
URL = 'https://api.quran.com/api/v4/chapter_recitations/7/{}?segments=true'

def counts():
    data=json.loads((ROOT/'app/src/main/assets/quran.json').read_text())
    return {s['id']:s['ayahCount'] for s in data['surahs']}

def safe_bounds(records, surah):
    """Keep source metadata intact; reject inconsistent adjacent timings, never guess speech."""
    study=json.loads((ROOT/f'app/src/main/assets/study/{surah}.json').read_text())
    word_counts={v['ayah']:max(w['position'] for w in v['words']) for v in study['verses']}
    for v in records:
        words=v['segments']
        v['clipFrom']=min([v['from']]+[x[1] for x in words])
        v['clipTo']=max([v['to']]+[x[2] for x in words])
        v['usable']=bool(words) and v['to']-v['from']>=300 and v['from']-v['clipFrom']<=1000 and v['clipTo']-v['to']<=1000
        v['wordCount']=word_counts[v['ayah']]
        if any(w[0]>v['wordCount'] for w in words):v['usable']=False
        if any(a[1]>b[1] for a,b in zip(words,words[1:])):v['usable']=False
        # Overlapping different word positions have no unambiguous active word.
        # Keep the original segments for audit but use individual-ayah audio instead.
        if any(a[2]>b[1] and a[0]!=b[0] for a,b in zip(words,words[1:])):v['usable']=False
    for v,n in zip(records,records[1:]):
        last=max([v['clipFrom']]+[x[2] for x in v['segments']])
        if last>n['clipFrom']:
            v['usable']=False;n['usable']=False
        elif n['clipFrom']<=v['clipTo']:
            v['clipTo']=n['clipFrom']
    return records

def fetch_chapter(surah):
    url=URL.format(surah)
    for retry in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Nuraya-audio-import/1.1'}),timeout=45) as response:
                data=response.read(3_000_000)
            payload=json.loads(data)['audio_file'];break
        except Exception:
            if retry==2:raise
            time.sleep(1+retry)
    assert payload['chapter_id']==surah
    assert payload['audio_url'].startswith('https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/')
    records=[]; dropped=0
    for t in payload['timestamps']:
        s,a=map(int,t['verse_key'].split(':')); assert s==surah
        start,end=t['timestamp_from'],t['timestamp_to']
        assert isinstance(start,int) and isinstance(end,int) and 0<=start<end
        segments=[]
        for segment in t.get('segments',[]):
            if len(segment)<3:
                dropped+=1;continue
            word,begin,finish=segment[:3]
            if not all(isinstance(x,int) for x in (word,begin,finish)) or word<1 or not 0<=begin<finish or begin>end or finish<start:
                dropped+=1;continue
            segments.append([word,begin,finish])
        records.append({'ayah':a,'from':start,'to':end,'segments':segments})
    result={'surah':surah,'recitationId':7,'sourceUrl':url,'audioUrl':payload['audio_url'],'verses':safe_bounds(records,surah)}
    DEST.mkdir(parents=True,exist_ok=True)
    path=DEST/f'{surah}.json';path.write_text(json.dumps(result,separators=(',',':'))+'\n')
    return {'surah':surah,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'sourceResponseSha256':hashlib.sha256(data).hexdigest(),'omittedMalformedSegments':dropped,'verseCount':len(records),'wordSegments':sum(len(v['segments']) for v in records),'safeWordSyncVerses':sum(v['usable'] for v in records)}

def verify():
    meta=json.loads(SOURCE.read_text()); total=0; segments=0; safe=0
    for s,n in counts().items():
        path=DEST/f'{s}.json'; data=path.read_bytes();entry=meta['chapters'][s-1]
        assert entry['surah']==s and hashlib.sha256(data).hexdigest()==entry['sha256']
        chapter=json.loads(data)
        assert chapter['surah']==s and chapter['recitationId']==7 and chapter['sourceUrl']==URL.format(s)
        assert chapter['audioUrl']==f'https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/{s}.mp3'
        assert [v['ayah'] for v in chapter['verses']]==list(range(1,n+1))
        assert chapter['verses']==safe_bounds(json.loads(data)['verses'],s), 'Safety flags or clip bounds differ from the source-derived policy'
        for v in chapter['verses']:
            assert 0<=v['from']<v['to']
            if v['usable']:
                assert v['segments'] and 0<=v['clipFrom']<v['clipTo']
                assert all(1<=word<=v['wordCount'] and v['clipFrom']<=start<end<=v['clipTo'] for word,start,end in v['segments'])
                assert not any(a[2]>b[1] and a[0]!=b[0] for a,b in zip(v['segments'],v['segments'][1:]))
                safe+=1
            for word,start,end in v['segments']: assert word>=1 and 0<=start<end and start<=v['to'] and end>=v['from']
        total+=n;segments+=sum(len(v['segments']) for v in chapter['verses'])
    assert total==6236
    print(f'Verified exact paired audio/timing: 114 chapters, {total} verses, {segments} source word segments; {safe} safe word-sync verses (Alafasy).')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--fetch',action='store_true');parser.add_argument('--verify',action='store_true',help='Verify bundled metadata without fetching (the default).');args=parser.parse_args()
    if args.fetch:
        with ThreadPoolExecutor(max_workers=4) as pool: chapters=list(pool.map(fetch_chapter,range(1,115)))
        SOURCE.write_text(json.dumps({'provider':'Quran.com API v4 / QuranicAudio','reciter':'Mishari Rashid al-Afasy','recitationId':7,'retrieved':'2026-10-24','sourceUrl':URL.format('{surah}'),'rights':'Audio remains with respective reciter/rightsholders; only source metadata and timing are bundled. Audio streams directly from its paired source.','chapters':chapters},indent=2)+'\n')
    verify()
