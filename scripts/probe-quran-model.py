#!/usr/bin/env python3
"""Record four controlled ASR observations using the same JNI wrapper as Android.

This probes actual words returned, not religious recitation accuracy. Run
scripts/test-native-ai.sh first to build the host JNI library and Java harness.
"""
from pathlib import Path
import argparse, array, hashlib, json, os, subprocess, sys, urllib.request

ROOT=Path(__file__).resolve().parents[1]
SOURCE='https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/1.mp3'
SOURCE_SHA='1e82007d1dc37fa879c217db756c379f348e8414a4749b07eddac14b007394d2'


def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('model',type=Path)
    ap.add_argument('--tools',type=Path,default=Path(os.environ.get('NURAYA_TOOLS','/workspace/.tools')))
    args=ap.parse_args()
    folder=args.model.resolve().parent/'probes';folder.mkdir(parents=True,exist_ok=True)
    source=folder.parent/'paired-alafasy-1.mp3'
    if not source.exists():
        with urllib.request.urlopen(SOURCE,timeout=30) as r: content=r.read(2_000_001)
        if len(content)>2_000_000: raise SystemExit('Unexpected audio source size')
        source.write_bytes(content)
    if sha(source)!=SOURCE_SHA:raise SystemExit('Paired audio checksum mismatch')
    full=folder/'paired-full.pcm'
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(source),'-map','0:a:0','-ar','16000','-ac','1','-f','f32le',str(full)],check=True)
    samples=array.array('f');samples.frombytes(full.read_bytes())
    if sys.byteorder!='little':samples.byteswap()
    timing=json.loads((ROOT/'app/src/main/assets/audio_timing/alafasy/1.json').read_text())
    verse=next(v for v in timing['verses'] if v['ayah']==1)
    segment=next(s for s in verse['segments'] if s[0]==3)
    start=round(verse['clipFrom']*16);end=round(verse['clipTo']*16)
    base=samples[start:end]
    muted=array.array('f',base)
    lo=round(segment[1]*16)-start;hi=round(segment[2]*16)-start
    muted[lo:hi]=array.array('f',[0.0])*(hi-lo)
    cases={'verse-1-1-complete':base,'verse-1-1-rahman-interval-muted':muted,
           'verse-1-1-before-rahim-truncated':base[:hi],'digital-silence':array.array('f',[0.0])*(6*16000)}
    inputs={'sourceUrl':SOURCE,'sourceSha256':SOURCE_SHA,'timingSource':'app/src/main/assets/audio_timing/alafasy/1.json',
            'scope':'Controlled host probes; muted interval uses source word metadata, not expert phonetic segmentation. No expected verse prompt is passed to ASR.','cases':[]}
    paths=[]
    for name,values in cases.items():
        p=folder/(name+'.pcm')
        if sys.byteorder!='little':values.byteswap()
        p.write_bytes(values.tobytes());paths.append(p)
        inputs['cases'].append({'file':p.name,'durationSeconds':len(values)/16000,'sha256':sha(p)})
    (folder/'inputs.json').write_text(json.dumps(inputs,indent=2)+'\n')
    host=args.tools/'nuraya-native-host';java=args.tools/'jdk-17.0.14+7/bin/java'
    cmd=[str(java),'-cp',str(host/'java'),'com.nuraya.app.recitation.ai.QuranWhisperEngine',str(host/'libnuraya_quran_ai.so'),str(args.model.resolve()),str(paths[0]),'--probe',*[str(p) for p in paths[1:]]]
    result=subprocess.run(cmd,capture_output=True,text=True,check=True,timeout=240)
    (folder/'probe.log').write_text(result.stdout+result.stderr)
    observations=[]
    for line in result.stdout.splitlines():
        if line.startswith('PROBE\t'):
            _,name,seconds,text=line.split('\t',3)
            observations.append({'file':name,'hostInferenceSeconds':float(seconds),'transcript':text})
    if len(observations)!=4:raise SystemExit('Not all controlled probes completed')
    report={'modelSha256':sha(args.model),'scope':inputs['scope'],'inputs':inputs,'observations':observations,
            'accuracyClaim':False,'deviceTest':False}
    (folder/'results.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
    print(json.dumps(report,indent=2,ensure_ascii=False))

if __name__=='__main__':main()
