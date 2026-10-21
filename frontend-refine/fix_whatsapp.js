const fs = require('fs');
const p = 'src/features/sales/scheduling/components/AppointmentDetailPanel.tsx';
let s = fs.readFileSync(p,'utf8');
const idx = s.indexOf('export function whatsappUrl');
if(idx >= 0){
  const start = s.lastIndexOf('/**', idx);
  let i = s.indexOf('{', idx);
  let depth = 0;
  for(let j=i; j<s.length; j++){
    if(s[j] === '{') depth++;
    else if(s[j] === '}'){ depth--; if(depth === 0){ const end = j+1; s = s.slice(0, start>=0?start:idx) + \xport { buildWhatsAppUrl as whatsappUrl } from \"@/lib/whatsapp\";\n\ + s.slice(end); fs.writeFileSync(p, s); console.log('ok'); process.exit(0); }}
  }
}
console.log('no');
