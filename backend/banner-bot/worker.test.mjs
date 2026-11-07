import test from 'node:test';
import assert from 'node:assert/strict';
import worker,{caption,handleUpdate,safeDestination} from './worker.mjs';
class KV {
 constructor(){this.data=new Map();}
 async get(key,type){const value=this.data.get(key);return value===undefined?null:type==='json'?JSON.parse(value):value;}
 async put(key,value){this.data.set(key,value);}
 async delete(key){this.data.delete(key);}
 async list({prefix}){return {keys:[...this.data.keys()].filter(k=>k.startsWith(prefix)).map(name=>({name}))};}
}
const env=()=>({BANNERS:new KV(),ADMIN_CHAT_ID:'100',WEBHOOK_SECRET:'test-webhook-secret',TELEGRAM_TOKEN:'test-only-not-a-credential'});
test('only safe explicit destinations and bounded captions are accepted',()=>{
 assert.deepEqual(caption('Belajar | Mulai hari ini | practice'),{title:'Belajar',subtitle:'Mulai hari ini',destination:'practice'});
 for(const target of ['javascript:alert(1)','file:///etc/passwd','http://example.com','https://user:pass@example.com','https://localhost/a'])assert.equal(safeDestination(target),false);
 assert.throws(()=>caption('a'.repeat(101)));assert.throws(()=>caption('Title | Desc | javascript:alert(1)'));
});
test('webhook rejects missing/incorrect secret before any bot activity',async()=>{
 const e=env();const r=await worker.fetch(new Request('https://b.example/webhook',{method:'POST',body:'{}'}),e);
 assert.equal(r.status,403);assert.equal(e.BANNERS.data.size,0);
});
test('non-admin private messages and admin messages from groups cannot mutate banners',async()=>{
 const e=env();await handleUpdate({update_id:1,message:{chat:{id:999,type:'private'},photo:[{}],caption:'test'}},e);
 await handleUpdate({update_id:2,message:{chat:{id:100,type:'group'},photo:[{}],caption:'test'}},e);
 assert.equal(e.BANNERS.data.size,0);
});
test('public feed omits inactive banners, never exposes bot secrets and honors order',async()=>{
 const e=env();
 await e.BANNERS.put('banner/b1',JSON.stringify({id:'b1',title:'A',subtitle:'Read',destination:'quran',active:true,order:3}));
 await e.BANNERS.put('banner/b2',JSON.stringify({id:'b2',title:'B',subtitle:'Hidden',destination:'practice',active:false,order:1}));
 await e.BANNERS.put('banner/b3',JSON.stringify({id:'b3',title:'C',subtitle:'Learn',destination:'learn',active:true,order:2}));
 const r=await worker.fetch(new Request('https://b.example/banners'),e);const body=await r.text();const parsed=JSON.parse(body);
 assert.deepEqual(parsed.banners.map(b=>b.id),['b3','b1']);assert.equal(body.includes(e.TELEGRAM_TOKEN),false);
 assert.equal(parsed.banners[0].imageUrl,'https://b.example/images/b3');
 assert.equal((await worker.fetch(new Request('https://b.example/images/b2'),e)).status,404);
});
test('authenticated webhook rejects malformed and oversized payloads',async()=>{
 const e=env();const headers={'X-Telegram-Bot-Api-Secret-Token':e.WEBHOOK_SECRET};
 assert.equal((await worker.fetch(new Request('https://b.example/webhook',{method:'POST',headers,body:'{bad'}),e)).status,400);
 assert.equal((await worker.fetch(new Request('https://b.example/webhook',{method:'POST',headers,body:'x'.repeat(33000)}),e)).status,413);
});
test('duplicate photo delivery is idempotent without downloading again',async()=>{
 const e=env();await e.BANNERS.put('banner/b33',JSON.stringify({id:'b33',title:'Existing'}));
 await handleUpdate({update_id:33,message:{chat:{id:100,type:'private'},photo:[{file_id:'never-fetch'}],caption:'New'}},e);
 assert.equal((await e.BANNERS.get('banner/b33','json')).title,'Existing');
});
test('authorized photo can be published, edited, disabled, reordered and deleted',async()=>{
 const e=env();const originalFetch=globalThis.fetch;const calls=[];
 globalThis.fetch=async(url,options)=>{
  calls.push({url,options});
  if(url.includes('/file/bot'))return new Response(new Uint8Array([255,216,255,217]),{headers:{'Content-Length':'4'}});
  if(url.endsWith('/getFile'))return Response.json({ok:true,result:{file_path:'photos/file.jpg',file_size:4}});
  return Response.json({ok:true,result:{message_id:1}});
 };
 try{
  const send=async(message)=>handleUpdate({update_id:70,message:{chat:{id:100,type:'private'},...message}},e);
  await send({photo:[{file_id:'photo',file_size:4,width:100,height:50}],caption:'Mulai | Belajar bersama | practice'});
  let feed=await(await worker.fetch(new Request('https://b.example/banners'),e)).json();
  assert.equal(feed.banners[0].destination,'practice');
  assert.equal((await worker.fetch(new Request('https://b.example/images/b70'),e)).status,200);
  await send({text:'/ubah b70 | Quran | Hari ini | quran'});
  assert.equal((await e.BANNERS.get('banner/b70','json')).title,'Quran');
  await send({text:'/urutan b70 2'});
  assert.equal((await e.BANNERS.get('banner/b70','json')).order,2);
  await send({text:'/nonaktif b70'});
  assert.equal((await worker.fetch(new Request('https://b.example/images/b70'),e)).status,404);
  await send({text:'/aktif b70'});
  assert.equal((await e.BANNERS.get('banner/b70','json')).active,true);
  await send({text:'/hapus b70'});
  assert.equal(await e.BANNERS.get('banner/b70'),null);assert.equal(await e.BANNERS.get('image/b70'),null);
  assert.equal(calls.filter(c=>c.url.includes('/file/bot')).length,1);
 }finally{globalThis.fetch=originalFetch;}
});
