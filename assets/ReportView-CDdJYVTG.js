import{_ as T,u as k,o as A,a as i,c as l,b as o,F as h,r as O,f as d,t as x,g as D,h as m,i as y,s as L,A as b}from"./index-DWDr2aAE.js";import{a as N,g as H,e as z}from"./dataService-BnxV3Nwr.js";import{c as _}from"./statistics-DyMDp9Li.js";import{S as B}from"./SkeletonLoader-BA6C9vNv.js";const F={class:"page"},P={class:"date-range-chips flex gap-sm mb-md",style:{"flex-wrap":"wrap"}},V=["onClick"],M={key:0,class:"p-lg"},U={class:"card mb-md"},j={class:"report-text"},q={class:"flex gap-sm"},G={key:2,class:"empty-state"},Z={__name:"ReportView",setup(J){const{user:p}=k(),v=m([]),$=m(null),g=m(!0),r=m("all"),C=[{value:"7d",label:"Ultimi 7 giorni"},{value:"30d",label:"Ultimi 30 giorni"},{value:"90d",label:"Ultimi 3 mesi"},{value:"all",label:"Tutto"}];A(async()=>{await S()});async function S(){g.value=!0;try{await N(p.value.username),v.value=await H(p.value.username),$.value=_(v.value)}catch(e){console.error("Load error:",e)}finally{g.value=!1}}const s=y(()=>{let e=[...v.value];if(r.value!=="all"){const a=parseInt(r.value),n=new Date(Date.now()-a*24*60*60*1e3);e=e.filter(t=>new Date(t.timestamp)>=n)}return e}),w=y(()=>_(s.value)),c=y(()=>{const e=w.value,a=s.value.length>0?new Date(s.value[s.value.length-1].timestamp).toLocaleDateString("it-IT"):"N/D",n=s.value.length>0?new Date(s.value[0].timestamp).toLocaleDateString("it-IT"):"N/D";let t=`REPORT PRESSIONE ARTERIOSA
`;t+=`Utente: ${p.value.username}
`,t+=`Periodo: ${a} - ${n}
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
`;for(const[f,u]of Object.entries(e.categoryDistribution||{})){const E=(u/e.readingsCount*100).toFixed(1);t+=`${z(f)}: ${u} (${E}%)
`}t+=`
--- DISTRIBUZIONE ORARIA ---
`;for(const[f,u]of Object.entries(e.timeOfDayDistribution||{}))t+=`${f}: ${u}
`;return t});async function R(){try{await navigator.clipboard.writeText(c.value),alert("Report copiato negli appunti!")}catch{const e=document.createElement("textarea");e.value=c.value,document.body.appendChild(e),e.select(),document.execCommand("copy"),document.body.removeChild(e),alert("Report copiato negli appunti!")}}async function I(){if(navigator.share)try{await navigator.share({text:c.value,title:"Report Pressione"})}catch{}else await R()}return(e,a)=>(i(),l("div",F,[a[4]||(a[4]=o("div",{class:"page-header"},[o("h1",null,"Report")],-1)),o("div",P,[(i(),l(h,null,O(C,n=>o("button",{key:n.value,class:L(["chip",{"chip--active":r.value===n.value}]),onClick:t=>r.value=n.value},x(n.label),11,V)),64))]),g.value?(i(),l("div",M,[d(B,{type:"text",count:12})])):s.value.length>0?(i(),l(h,{key:1},[o("div",U,[o("pre",j,x(c.value),1)]),o("div",q,[o("button",{class:"btn btn-primary",onClick:R},[d(b,{name:"copy",size:16}),a[0]||(a[0]=D(" Copia ",-1))]),o("button",{class:"btn btn-ghost",onClick:I},[d(b,{name:"share",size:16}),a[1]||(a[1]=D(" Condividi ",-1))])])],64)):(i(),l("div",G,[d(b,{name:"copy",size:48,color:"var(--color-text-tertiary)",class:"empty-state__icon"}),a[2]||(a[2]=o("h3",null,"Nessun dato disponibile",-1)),a[3]||(a[3]=o("p",null,"Aggiungi misurazioni per generare un report.",-1))]))]))}},Y=T(Z,[["__scopeId","data-v-10e1f543"]]);export{Y as default};
