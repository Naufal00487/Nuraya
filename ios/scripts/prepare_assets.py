#!/usr/bin/env python3
"""Package Android's exact source content for iOS; no Quran text/image edits."""
import hashlib, json, pathlib, re, shutil, gzip
ROOT=pathlib.Path(__file__).resolve().parents[2]
SOURCE=ROOT/'app/src/main/assets';DEST=ROOT/'ios/Nuraya/Resources/Content'
CORE=ROOT/'app/src/main/java/com/nuraya/app'
def export():
 DEST.mkdir(parents=True,exist_ok=True)
 shutil.copytree(SOURCE,DEST,dirs_exist_ok=True)
 shutil.copy2(ROOT/'app/src/main/res/font/amiri_quran.ttf',DEST/'amiri_quran.ttf')
 data= (CORE/'media/CuratedReciters.kt').read_text()
 qaris=[dict(zip(['chapterId','verseId','name'],x)) for x in re.findall(r'Selection\("([^"]+)", "([^"]+)", "([^"]+)"\)',data)]
 assert len(qaris)==28 and len({q['chapterId'] for q in qaris})==28
 (DEST/'curated_reciters.json').write_text(json.dumps(qaris,ensure_ascii=False))
 data=(CORE/'quranreading/QuranReadingModes.kt').read_text()
 modes=[dict(zip(['id','titleId','titleEn','descriptionId','descriptionEn'],x)) for x in re.findall(r'QuranReadingMode\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)"\)',data)]
 assert len(modes)==11
 (DEST/'reading_modes.json').write_text(json.dumps(modes,ensure_ascii=False))
 themes=[]
 for ident,title,_,refs in re.findall(r'QuranTheme\("([^"]+)", "([^"]+)", "([^"]+)", listOf\(([^\n]+)\)\)',data):
  themes.append({'id':ident,'title':title,'references':[[int(a),int(b)] for a,b in re.findall(r'(\d+) to (\d+)',refs)]})
 assert len(themes)==10
 (DEST/'themes.json').write_text(json.dumps(themes,ensure_ascii=False))
 waqaf=[dict(zip(['mark','name','explanation'],x)) for x in re.findall(r'WaqafSign\("([^"]+)", "([^"]+)", "([^"]+)"\)',data)]
 (DEST/'waqaf_guide.json').write_text(json.dumps(waqaf,ensure_ascii=False))
 icons=[]
 for name,w,h,path in re.findall(r'vector\("([^"]+)", ([\d.]+)f, ([\d.]+)f, "([^"]+)"\)',(CORE/'ui/icons/AppIcons.kt').read_text()):
  icons.append(dict(id=name,width=float(w),height=float(h),path=path))
 assert len(icons)>30
 (DEST/'ui_vectors.json').write_text(json.dumps(icons))
 raw=gzip.decompress((SOURCE/'hadith/hadith.database').read_bytes())
 metadata=json.loads((SOURCE/'hadith/sources.json').read_text())
 assert hashlib.sha256(raw).hexdigest()==metadata['databaseSha256']
 (DEST/'hadith/hadith.sqlite').write_bytes(raw)
 (DEST/'quran_model.json').write_text(json.dumps({'url':'https://raw.githubusercontent.com/Naufal00487/Nuraya/1d5a31aa2e60dbb9af70981e606a9c06b1b18c3e/nuraya-quran-whisper-base-q5_1.bin','bytes':59707625,'sha256':'1de4498dc16fdee2045e67a97d73cda4624017640dbe5ddf809f3130da7c234a','fileName':'nuraya-quran-whisper-base-q5_1.bin'}))
 manifest={str(p.relative_to(SOURCE)):hashlib.sha256(p.read_bytes()).hexdigest() for p in SOURCE.rglob('*') if p.is_file()}
 for rel,digest in manifest.items():assert hashlib.sha256((DEST/rel).read_bytes()).hexdigest()==digest
 (DEST/'android_assets_sha256.json').write_text(json.dumps(manifest,indent=2))
 print(f'Packaged {len(manifest)} byte-identical Android assets, {len(qaris)} qari, {len(modes)} modes, {len(icons)} vectors.')
if __name__=='__main__':export()
