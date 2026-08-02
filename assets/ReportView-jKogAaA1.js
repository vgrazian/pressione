import{_ as T,u as k,o as A,a as i,c as l,b as n,F as D,r as O,t as h,f as y,g as x,h as d,i as f,z as N,A as b,y as L}from"./index-DCJI5Jzz.js";import{a as z,g as H}from"./dataService-BoIizkWE.js";import{c as _}from"./statistics-DznZkCdN.js";const B={class:"page"},F={class:"date-range-chips flex gap-sm mb-md",style:{"flex-wrap":"wrap"}},P=["onClick"],V={key:0,class:"empty-state"},M={class:"card mb-md"},U={class:"report-text"},j={class:"flex gap-sm"},q={key:2,class:"empty-state"},G={__name:"ReportView",setup(Z){const{user:m}=k(),p=d([]),$=d(null),v=d(!0),r=d("all"),C=[{value:"7d",label:"Ultimi 7 giorni"},{value:"30d",label:"Ultimi 30 giorni"},{value:"90d",label:"Ultimi 3 mesi"},{value:"all",label:"Tutto"}];A(async()=>{await w()});async function w(){v.value=!0;try{await z(m.value.username),p.value=await H(m.value.username),$.value=_(p.value)}catch(e){console.error("Load error:",e)}finally{v.value=!1}}const o=f(()=>{let e=[...p.value];if(r.value!=="all"){const a=parseInt(r.value),s=new Date(Date.now()-a*24*60*60*1e3);e=e.filter(t=>new Date(t.timestamp)>=s)}return e}),I=f(()=>_(o.value)),c=f(()=>{const e=I.value,a=o.value.length>0?new Date(o.value[o.value.length-1].timestamp).toLocaleDateString("it-IT"):"N/D",s=o.value.length>0?new Date(o.value[0].timestamp).toLocaleDateString("it-IT"):"N/D";let t=`REPORT PRESSIONE ARTERIOSA
`;t+=`Utente: ${m.value.username}
`,t+=`Periodo: ${a} - ${s}
`,t+=`Misurazioni: ${e.readingsCount}

`,t+=`--- MEDIE ---
`,t+=`Sistolica: ${e.avgSystolic} mmHg
`,t+=`Diastolica: ${e.avgDiastolic} mmHg
`,t+=`Freq. Cardiaca: ${e.avgHeartRate} BPM

`,t+=`--- INTERVALLI ---
`,t+=`Sistolica: ${e.minSystolic} - ${e.maxSystolic} mmHg
`,t+=`Diastolica: ${e.minDiastolic} - ${e.maxDiastolic} mmHg
`,t+=`Freq. Cardiaca: ${e.minHeartRate} - ${e.maxHeartRate} BPM

`,t+=`--- CATEGORIE ---
`;for(const[g,u]of Object.entries(e.categoryDistribution||{})){const E=(u/e.readingsCount*100).toFixed(1);t+=`${L(g)}: ${u} (${E}%)
`}t+=`
--- DISTRIBUZIONE ORARIA ---
`;for(const[g,u]of Object.entries(e.timeOfDayDistribution||{}))t+=`${g}: ${u}
`;return t});async function R(){try{await navigator.clipboard.writeText(c.value),alert("Report copiato negli appunti!")}catch{const e=document.createElement("textarea");e.value=c.value,document.body.appendChild(e),e.select(),document.execCommand("copy"),document.body.removeChild(e),alert("Report copiato negli appunti!")}}async function S(){if(navigator.share)try{await navigator.share({text:c.value,title:"Report Pressione"})}catch{}else await R()}return(e,a)=>(i(),l("div",B,[a[5]||(a[5]=n("div",{class:"page-header"},[n("h1",null,"Report")],-1)),n("div",F,[(i(),l(D,null,O(C,s=>n("button",{key:s.value,class:N(["chip",{"chip--active":r.value===s.value}]),onClick:t=>r.value=s.value},h(s.label),11,P)),64))]),v.value?(i(),l("div",V,[...a[0]||(a[0]=[n("p",null,"Caricamento...",-1)])])):o.value.length>0?(i(),l(D,{key:1},[n("div",M,[n("pre",U,h(c.value),1)]),n("div",j,[n("button",{class:"btn btn-primary",onClick:R},[y(b,{name:"copy",size:16}),a[1]||(a[1]=x(" Copia ",-1))]),n("button",{class:"btn btn-ghost",onClick:S},[y(b,{name:"share",size:16}),a[2]||(a[2]=x(" Condividi ",-1))])])],64)):(i(),l("div",q,[y(b,{name:"copy",size:48,color:"var(--color-text-tertiary)",class:"empty-state__icon"}),a[3]||(a[3]=n("h3",null,"Nessun dato disponibile",-1)),a[4]||(a[4]=n("p",null,"Aggiungi misurazioni per generare un report.",-1))]))]))}},W=T(G,[["__scopeId","data-v-23d8a659"]]);export{W as default};
