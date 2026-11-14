#!/usr/bin/env python3
"""Train a compact local LSA retrieval model from Nuraya's attributed source corpus.

No external API, generative answer, or pretrained religious knowledge is used.
Requires numpy, scipy, scikit-learn only when rebuilding; Android uses a small
quantized projection and cosine similarity without an ML runtime dependency.
"""
from pathlib import Path
import json, hashlib, re, struct, unicodedata, io, argparse
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.decomposition import TruncatedSVD
from sklearn.preprocessing import normalize

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
STOP=set('apa apakah saya aku gw gua ingin mau tolong bisa carikan cari jelaskan penjelasan belajar pelajaran alquran quran ayat surah surat tafsir arti makna menurut bagaimana kenapa mengapa tentang lagi butuh punya kepada merupakan sehingga tersebut mereka kita kamu engkau dia nya ialah akan karena juga telah dapat memang jika dengan pada dalam yang dan di ke dari ini itu untuk dengan dalam pada adalah akan sebagai atau karena juga oleh agar telah dapat tidak the and of to in a an is for with that this as are be'.split())
ALIASES={'galau':'sedih khawatir', 'cemas':'khawatir takut', 'down':'sedih', 'ortu':'orang tua', 'rezeki':'rezeki rizki', 'rizki':'rezeki rizki', 'ngaji':'quran membaca', 'sholat':'salat', 'shalat':'salat', 'capek':'lelah', 'gak':'tidak', 'nggak':'tidak', 'putusasa':'putus asa', 'susah':'kesulitan', 'kerjaan':'pekerjaan', 'maafin':'memaafkan', 'mengampuni':'mengampuni ampunan', 'kesepian':'sendiri kesedihan', 'overthinking':'khawatir takut', 'hutang':'utang', 'solat':'salat', 'gosip':'menggunjing ghibah', 'bergosip':'menggunjing', 'ampun':'ampunan', 'menyerah':'putus asa', 'ketenangan':'tenang tenteram', 'tenang':'tenang tenteram', 'sabar':'sabar bersabar kesabaran', 'kesulitan':'kesulitan kesukaran', 'miskin':'miskin kemiskinan', 'orangtua':'orang tua', 'sayang':'kasih sayang'}
def normalized(text):
 text=''.join(c for c in unicodedata.normalize('NFD',text.lower()) if not unicodedata.combining(c))
 return re.findall(r'[^\W\d_]{2,}',text)
def tokens(text):return [t for t in normalized(text) if t not in STOP]
def query_tokens(text):return [t for word in tokens(text) for t in tokens(ALIASES.get(word,word))]
def quantize(rows):
 scale=np.max(np.abs(rows),axis=1)/127
 scale[scale==0]=1
 return np.rint(rows/scale[:,None]).clip(-127,127).astype('int8'),scale.astype('>f4')
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--verify',action='store_true');args=ap.parse_args()
 binary=ASSETS/'semantic_index.bin';meta_path=ASSETS/'semantic_index_sources.json'
 if args.verify:
  meta=json.loads(meta_path.read_text());assert hashlib.sha256(binary.read_bytes()).hexdigest()==meta['sha256']
  for path,sha in meta['sources'].items():assert hashlib.sha256((ASSETS/path).read_bytes()).hexdigest()==sha,path
  print('Verified local semantic model and all 115 corpus source hashes.');return
 quran=json.loads((ASSETS/'quran.json').read_text())['verses'];studies={};sources={}
 for path in [ASSETS/'quran.json']+[ASSETS/f'study/{i}.json' for i in range(1,115)]:
  sources[str(path.relative_to(ASSETS))]=hashlib.sha256(path.read_bytes()).hexdigest()
  if path.name!='quran.json':studies[int(path.stem)]=json.loads(path.read_text())['verses']
 corpus=[v['indonesian']+' '+v['indonesian']+' '+v['english']+' '+(studies[v['surah']][v['ayah']-1]['tafsir'] or '') for v in quran]
 vectorizer=TfidfVectorizer(tokenizer=tokens,token_pattern=None,lowercase=False,max_features=20000,min_df=2,sublinear_tf=True,dtype=np.float32)
 matrix=vectorizer.fit_transform(corpus)
 svd=TruncatedSVD(n_components=128,n_iter=8,random_state=161)
 documents=normalize(svd.fit_transform(matrix)).astype(np.float32)
 terms=vectorizer.get_feature_names_out();projection=svd.components_.T.copy()
 quant_terms,term_scale=quantize(projection);quant_docs,doc_scale=quantize(documents)
 out=io.BytesIO();out.write(b'NSEM');out.write(struct.pack('>HHI',128,len(quran),len(terms)))
 for term,idf,scale,vector in zip(terms,vectorizer.idf_,term_scale,quant_terms):
  encoded=term.encode();out.write(struct.pack('>H',len(encoded)));out.write(encoded);out.write(struct.pack('>ff',idf,float(scale)));out.write(vector.tobytes())
 for scale,vector in zip(doc_scale,quant_docs):out.write(struct.pack('>f',float(scale)));out.write(vector.tobytes())
 data=out.getvalue();binary.write_bytes(data)
 meta={'schemaVersion':1,'method':'TF-IDF + 128-dimensional latent semantic analysis (TruncatedSVD), signed int8 row quantization, cosine retrieval; no generative model','verseCount':len(quran),'terms':len(terms),'dimensions':128,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'sources':sources,'stopWords':sorted(STOP),'aliases':ALIASES,'explainedVariance':float(svd.explained_variance_ratio_.sum()),'randomSeed':161}
 meta_path.write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n')
 print('Model bytes:',len(data),'terms:',len(terms),'variance:',meta['explainedVariance'])
 for query in ['lagi susah dan butuh harapan','jangan menyerah pada ampunan Allah','berbuat baik kepada orang tua','hati tenang mengingat Allah','menjaga lisan dari gosip','kesulitan akan berlalu','cemas karena takut miskin','aku ingin belajar sabar']:
  q=normalize(svd.transform(vectorizer.transform([' '.join(query_tokens(query))])))[0];scores=documents@q
  result=np.argsort(-scores)[:6]
  print(query, [(f"{quran[i]['surah']}:{quran[i]['ayah']}",round(float(scores[i]),3),quran[i]['indonesian'][:90]) for i in result])
if __name__=='__main__':main()
