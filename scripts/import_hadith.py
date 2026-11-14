#!/usr/bin/env python3
"""Import source-aligned Arabic/Indonesian hadiths into an offline SQLite FTS index."""
from pathlib import Path
import argparse, concurrent.futures, gzip, hashlib, json, re, sqlite3, unicodedata, urllib.request

ROOT=Path(__file__).resolve().parents[1]
COMMIT='df57907be35291c91ad6a6691180e22ca9920784'
BASE=f'https://raw.githubusercontent.com/fawazahmed0/hadith-api/{COMMIT}'
BOOKS={'bukhari':'Shahih al-Bukhari','muslim':'Shahih Muslim'}
def digest(data):return hashlib.sha256(data).hexdigest()
def normalized(text):
    text=''.join(x for x in unicodedata.normalize('NFD',text.lower()) if not unicodedata.category(x).startswith('M')).replace('ـ','').replace('ٱ','ا')
    return ' '.join(re.findall(r'[^\W_]+',text))
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--cache',type=Path,default=Path('/tmp/nuraya-hadith'));ap.add_argument('--verify',action='store_true');args=ap.parse_args()
    assets=ROOT/'app/src/main/assets/hadith';assets.mkdir(exist_ok=True)
    if args.verify:
        meta=json.loads((assets/'sources.json').read_text());packed=(assets/'hadith.database').read_bytes()
        assert digest(packed)==meta['gzipSha256'];raw=gzip.decompress(packed);assert digest(raw)==meta['databaseSha256']
        args.cache.mkdir(parents=True,exist_ok=True);dbpath=args.cache/'verify.sqlite';dbpath.write_bytes(raw)
        with sqlite3.connect(dbpath) as db:
            # FTS4 contentless tables deliberately cannot reconstruct document text.
            # SQLite 3.53 full integrity_check tries that reconstruction; check each
            # physical table instead, then independently exercise postings below.
            physical = ['hadith'] + [r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'hadith_search_%'")]
            for table in physical:
                assert db.execute("PRAGMA integrity_check('"+table+"')").fetchone()[0]=='ok'
            for identity, indonesian, arabic in db.execute('SELECT id,indonesian,arabic FROM hadith'):
                tokens=normalized(indonesian or arabic).split()[:8]
                if tokens:
                    match=' '.join('"'+t+'"' for t in tokens)
                    assert db.execute('SELECT docid FROM hadith_search WHERE hadith_search MATCH ? AND docid = ?', (match,identity)).fetchone()==(identity,)
            assert db.execute('SELECT count(*) FROM hadith').fetchone()[0]==meta['total']
            assert db.execute('SELECT count(*) FROM hadith WHERE indonesian <> ""').fetchone()[0]==meta['indonesianAvailable']
            assert db.execute('SELECT count(*) FROM hadith_search WHERE hadith_search MATCH ?',('niat',)).fetchone()[0]>0
        print('Verified hadith database, source counts, checksums and FTS search.');return
    args.cache.mkdir(parents=True,exist_ok=True)
    def fetch(edition):
        p=args.cache/(edition+'.json');url=BASE+'/editions/'+edition+'.min.json'
        if not p.exists():
            with urllib.request.urlopen(url,timeout=45) as response:p.write_bytes(response.read())
        return edition,p,url
    editions=[lang+'-'+book for book in BOOKS for lang in ['ara','ind']]
    sources={};data={}
    for edition,path,url in concurrent.futures.ThreadPoolExecutor(max_workers=4).map(fetch,editions):
        content=path.read_bytes();data[edition]=json.loads(content)
        sources[edition]={'url':url,'sha256':digest(content),'bytes':len(content)}
    dbpath=args.cache/'hadith.sqlite'
    if dbpath.exists():dbpath.unlink()
    db=sqlite3.connect(dbpath)
    db.executescript('''PRAGMA user_version=1;
        CREATE TABLE hadith (id INTEGER PRIMARY KEY, collection TEXT NOT NULL, number TEXT NOT NULL, sort_number REAL NOT NULL, section TEXT NOT NULL, arabic TEXT NOT NULL, indonesian TEXT NOT NULL, grades TEXT NOT NULL, UNIQUE(collection,number));
        CREATE INDEX hadith_reference ON hadith(collection,number);
        CREATE VIRTUAL TABLE hadith_search USING fts4(indonesian,arabic,content='',tokenize=unicode61);
    ''')
    total=available=arabic_available=0;collections=[]
    for book,title in BOOKS.items():
        original=data['ara-'+book];translated=data['ind-'+book]
        arabic={str(x['hadithnumber']):x for x in original['hadiths']}
        indonesian={str(x['hadithnumber']):x for x in translated['hadiths']}
        assert len(arabic)==len(original['hadiths']) and len(indonesian)==len(translated['hadiths'])
        assert arabic.keys()==indonesian.keys()
        count_translated=0
        for number,row in arabic.items():
            trans=indonesian[number];assert row['reference']==trans['reference'],(book,number)
            total+=1;ar=row['text'];idn=trans['text']
            available+=bool(idn.strip());arabic_available+=bool(ar.strip());count_translated+=bool(idn.strip())
            section=translated['metadata']['sections'].get(str(row['reference']['book']),'')
            db.execute('INSERT INTO hadith VALUES (?,?,?,?,?,?,?,?)',(total,book,number,float(number),section,ar,idn,json.dumps(row.get('grades',[]),ensure_ascii=False)))
            db.execute('INSERT INTO hadith_search(docid,indonesian,arabic) VALUES(?,?,?)',(total,normalized(idn),normalized(ar)))
        collections.append({'id':book,'title':title,'records':len(arabic),'indonesianAvailable':count_translated})
    db.commit();db.execute("INSERT INTO hadith_search(hadith_search) VALUES('optimize')");db.commit();db.execute('VACUUM');db.close()
    raw=dbpath.read_bytes();packed=gzip.compress(raw,compresslevel=9,mtime=0);(assets/'hadith.database').write_bytes(packed)
    metadata={'schemaVersion':1,'provider':'fawazahmed0/hadith-api','sourceCommit':COMMIT,'license':'Unlicense','licenseUrl':BASE+'/LICENSE',
        'total':total,'indonesianAvailable':available,'arabicAvailable':arabic_available,'collections':collections,'sources':sources,
        'notice':'Numbers follow this dataset edition. Text is preserved verbatim; blank source texts remain unavailable. Arabic and Indonesian are joined by exact hadithnumber and matching book/hadith reference, never by array position.',
        'databaseBytes':len(raw),'databaseSha256':digest(raw),'gzipBytes':len(packed),'gzipSha256':digest(packed)}
    (assets/'sources.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:metadata[k] for k in ['total','indonesianAvailable','arabicAvailable','databaseBytes','gzipBytes']},indent=2))
if __name__=='__main__':main()
