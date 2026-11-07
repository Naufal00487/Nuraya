// Run locally after deployment. Secrets are environment values, never CLI arguments or source constants.
const {TELEGRAM_TOKEN,WEBHOOK_SECRET}=process.env;
const base=process.argv[2];
if(!TELEGRAM_TOKEN||!WEBHOOK_SECRET||!base||!base.startsWith('https://')){
 console.error('Set TELEGRAM_TOKEN and WEBHOOK_SECRET in your local environment; run node setup-webhook.mjs https://your-worker.workers.dev');process.exit(1);
}
try{
 const url=new URL('/webhook',base);
 const result=await fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/setWebhook`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({url:url.href,secret_token:WEBHOOK_SECRET,allowed_updates:['message'],max_connections:1,drop_pending_updates:false})});
 const body=await result.json();
 if(!result.ok||!body.ok)throw new Error('Webhook setup failed');
 console.log('Webhook registered. Open your bot privately and send /help.');
}catch{console.error('Webhook setup failed. Verify deployment and runtime secrets; no token was printed.');process.exit(1);}
