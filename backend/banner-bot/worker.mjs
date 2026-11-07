const ROUTES=new Set(['quran','murottal','practice','recitation','prayer','library','learn']);
const MAX_IMAGE=1_500_000;
export function safeDestination(value){
 if(ROUTES.has(value))return true;
 try{const u=new URL(value);return u.protocol==='https:'&&!u.username&&!u.password&&u.hostname.includes('.')&&u.hostname!=='localhost';}catch{return false;}
}
export function caption(value){
 const [title='',subtitle='',destination='quran']=value.split('|').map(x=>x.trim());
 if(!title||title.length>100||subtitle.length>180||!safeDestination(destination))throw new Error('Gunakan: Judul (maks 100) | Keterangan (maks 180) | quran / practice / URL https.');
 return {title,subtitle,destination};
}
const json=(value,status=200)=>new Response(JSON.stringify(value),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
async function telegram(env,method,data){
 const r=await fetch(`https://api.telegram.org/bot${env.TELEGRAM_TOKEN}/${method}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
 const body=await r.json();if(!r.ok||!body.ok)throw new Error('Telegram belum dapat dihubungi. Coba kembali.');return body.result;
}
async function all(env){const list=await env.BANNERS.list({prefix:'banner/',limit:100});return (await Promise.all(list.keys.map(k=>env.BANNERS.get(k.name,'json')))).filter(Boolean).sort((a,b)=>a.order-b.order);}
async function message(env,text){return telegram(env,'sendMessage',{chat_id:env.ADMIN_CHAT_ID,text});}
export async function handleUpdate(update,env){
 const m=update?.message;
 if(!m||m.chat?.type!=='private'||String(m.chat?.id)!==String(env.ADMIN_CHAT_ID))return;
 // Unique Telegram update IDs make duplicate delivery of photo uploads idempotent.
 if(!Number.isSafeInteger(update.update_id)||update.update_id<0)return;
 const id=`b${update.update_id}`;
 const text=(m.text||'').trim();
 if(m.photo?.length){
  if(await env.BANNERS.get(`banner/${id}`))return;
  const rows=await all(env);if(rows.length>=12){await message(env,'Maksimal 12 banner. Hapus banner lama terlebih dahulu.');return;}
  const info=caption(m.caption||'');
  const candidate=[...m.photo].reverse().find(p=>p.file_size>0&&p.file_size<=MAX_IMAGE&&p.width<=4096&&p.height<=4096);
  if(!candidate)throw new Error('Foto maksimal 1,5 MB dan 4096 piksel.');
  const file=await telegram(env,'getFile',{file_id:candidate.file_id});
  if(!file.file_path||file.file_path.includes('..')||file.file_size>MAX_IMAGE)throw new Error('Berkas foto tidak valid.');
  const response=await fetch(`https://api.telegram.org/file/bot${env.TELEGRAM_TOKEN}/${file.file_path}`);
  if(!response.ok||Number(response.headers.get('Content-Length'))>MAX_IMAGE)throw new Error('Foto belum dapat diunduh.');
  const bytes=await response.arrayBuffer();if(bytes.byteLength>MAX_IMAGE)throw new Error('Foto terlalu besar.');
  const prefix=new Uint8Array(bytes);if(prefix[0]!==255||prefix[1]!==216)throw new Error('Kirim sebagai foto JPEG melalui Telegram.');
  await env.BANNERS.put(`image/${id}`,bytes);
  await env.BANNERS.put(`banner/${id}`,JSON.stringify({...info,id,active:true,order:Date.now()}));
  await message(env,`Banner ${id} tersimpan. Pembaruan aplikasi dapat memerlukan hingga 15 menit.`);return;
 }
 if(/^\/(start|help)(?:@\w+)?$/.test(text)){await message(env,'Kirim FOTO dengan caption:\nJudul | Keterangan | quran\nTujuan: quran, murottal, practice, recitation, prayer, library, learn, atau URL https.\n/list\n/hapus ID\n/nonaktif ID\n/aktif ID\n/urutan ID ANGKA\n/ubah ID | Judul | Keterangan | Tujuan');return;}
 if(text==='/list'){const rows=await all(env);await message(env,rows.length?rows.map(b=>`${b.id} · ${b.active?'Aktif':'Nonaktif'} · ${b.order}\n${b.title}`).join('\n\n'):'Belum ada banner. Kirim foto untuk menambahkan.');return;}
 const match=text.match(/^\/(hapus|nonaktif|aktif|urutan|ubah)\s+(b\d+)(.*)$/s);
 if(!match){await message(env,'Perintah belum dikenal. Ketik /help.');return;}
 const [,command,key,tail]=match;const current=await env.BANNERS.get(`banner/${key}`,'json');
 if(!current){await message(env,'ID banner tidak ditemukan. Gunakan /list.');return;}
 if(command==='hapus'){await env.BANNERS.delete(`banner/${key}`);await env.BANNERS.delete(`image/${key}`);}
 else{
  if(command==='aktif'||command==='nonaktif')current.active=command==='aktif';
  if(command==='urutan'){const value=Number(tail.trim());if(!Number.isSafeInteger(value)||value<0||value>9999)throw new Error('Urutan harus 0–9999.');current.order=value;}
  if(command==='ubah')Object.assign(current,caption(tail.replace(/^\s*\|/,'')));
  await env.BANNERS.put(`banner/${key}`,JSON.stringify(current));
 }
 await message(env,`Banner ${key} diperbarui. Pembaruan dapat memerlukan hingga 15 menit.`);
}
export default {async fetch(request,env){
 const url=new URL(request.url);
 if(request.method==='GET'&&url.pathname==='/banners'){
  const banners=(await all(env)).filter(b=>b.active).slice(0,12).map(({id,title,subtitle,destination})=>({id,title,subtitle,destination,imageUrl:`${url.origin}/images/${id}`}));
  return json({schemaVersion:1,banners});
 }
 if(request.method==='GET'&&/^\/images\/b\d+$/.test(url.pathname)){
  const id=url.pathname.split('/').pop();const banner=await env.BANNERS.get(`banner/${id}`,'json');
  if(!banner?.active)return new Response('Not found',{status:404});
  const bytes=await env.BANNERS.get(`image/${id}`,'arrayBuffer');
  return bytes?new Response(bytes,{headers:{'Content-Type':'image/jpeg','Cache-Control':'public, max-age=900','X-Content-Type-Options':'nosniff'}}):new Response('Not found',{status:404});
 }
 if(url.pathname==='/webhook'&&request.method==='POST'){
  if(!env.WEBHOOK_SECRET||request.headers.get('X-Telegram-Bot-Api-Secret-Token')!==env.WEBHOOK_SECRET)return json({ok:false},403);
  if(Number(request.headers.get('Content-Length'))>32768)return json({ok:false},413);
  const raw=await request.text();if(new TextEncoder().encode(raw).length>32768)return json({ok:false},413);
  let update;try{update=JSON.parse(raw);}catch{return json({ok:false},400);}
  if(!update||typeof update!=="object"||Array.isArray(update))return json({ok:false},400);
  try{await handleUpdate(update,env);}catch(error){
   // Never return or log Telegram endpoints containing tokens.
   if(String(update.message?.chat?.id)===String(env.ADMIN_CHAT_ID)&&update.message?.chat?.type==='private'){
    await message(env,error.message.startsWith('Gunakan:')?error.message:'Perubahan belum tersimpan. Periksa format/foto lalu coba kembali.').catch(()=>{});
   }
  }
  return json({ok:true});
 }
 return json({error:'Not found'},404);
}};
