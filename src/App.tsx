import { useMemo, useState } from 'react'
import {
  Activity, BarChart3, CloudRain, CloudSun, Droplets, Gauge, Globe2, LayoutDashboard,
  MapPin, Menu, Moon, Search, Settings2, Sun, Thermometer, Wind, X
} from 'lucide-react'
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

type NavItem={label:string; icon:typeof LayoutDashboard}
const nav:NavItem[]=[
  {label:'Overview',icon:LayoutDashboard},{label:'Weather',icon:CloudSun},
  {label:'Climate',icon:BarChart3},{label:'Visualizer',icon:Globe2},
  {label:'Simulator',icon:Activity},{label:'Data Lab',icon:Gauge}
]
const temp=[{time:'06',v:24},{time:'09',v:26},{time:'12',v:30},{time:'15',v:33},{time:'18',v:31},{time:'21',v:27}]
const forecast=[['Fri','28°','20°','Clear'],['Sat','30°','21°','Sunny'],['Sun','29°','22°','Cloudy'],['Mon','27°','21°','Rain'],['Tue','26°','20°','Rain'],['Wed','29°','19°','Clear'],['Thu','31°','20°','Sunny']]

function App(){
  const [active,setActive]=useState('Overview')
  const [mobileOpen,setMobileOpen]=useState(false)
  const [dark,setDark]=useState(true)
  const current=useMemo(()=>({temp:28,feels:30,humidity:63,pressure:1007,wind:12,rain:18}),[])
  return <div className={dark?'app dark':'app'}>
    <aside className={mobileOpen?'sidebar open':'sidebar'}>
      <div className="brand"><div className="brand-mark"><CloudSun size={22}/></div><div><strong>Clima-Tact</strong><span>Weather & Climate Observatory</span></div><button className="icon-btn mobile-close" onClick={()=>setMobileOpen(false)}><X size={18}/></button></div>
      <nav>{nav.map(({label,icon:Icon})=><button key={label} className={active===label?'nav-item active':'nav-item'} onClick={()=>{setActive(label);setMobileOpen(false)}}><Icon size={18}/><span>{label}</span></button>)}</nav>
      <div className="sidebar-bottom"><div className="status"><span className="pulse"></span><span>Data systems operational</span></div><button className="settings"><Settings2 size={17}/> Settings</button></div>
    </aside>
    {mobileOpen&&<button className="backdrop" onClick={()=>setMobileOpen(false)} aria-label="Close menu"/>}
    <main className="main">
      <header className="topbar">
        <button className="icon-btn mobile-menu" onClick={()=>setMobileOpen(true)}><Menu size={20}/></button>
        <div className="location"><MapPin size={17}/><span>Lucknow, India</span><span className="dot"></span><small>Today, 25 Sep</small></div>
        <div className="top-actions"><div className="search"><Search size={17}/><input placeholder="Search location..." /><kbd>⌘ K</kbd></div><button className="icon-btn" onClick={()=>setDark(!dark)} aria-label="Toggle theme">{dark?<Sun size={18}/>:<Moon size={18}/>}</button><div className="avatar">AK</div></div>
      </header>

      <section className="content">
        <div className="page-head"><div><p className="eyebrow">Atmospheric overview</p><h1>Good afternoon.</h1><p className="muted">A live snapshot of weather conditions and the climate signals behind them.</p></div><button className="location-btn"><MapPin size={16}/> Change location</button></div>

        <div className="hero-grid">
          <section className="card current-card">
            <div className="card-top"><div><span className="label">CURRENT WEATHER</span><h2>{current.temp}°<span>C</span></h2><p>Partly cloudy · Feels like {current.feels}°</p></div><div className="weather-orb"><Sun size={48}/><span className="orb-cloud"><CloudSun size={42}/></span></div></div>
            <div className="metrics">
              <Metric icon={<Droplets/>} label="Humidity" value={current.humidity+'%'} note="Comfortable"/>
              <Metric icon={<Gauge/>} label="Pressure" value={current.pressure+' hPa'} note="Near normal"/>
              <Metric icon={<Wind/>} label="Wind" value={current.wind+' km/h'} note="NE · Light"/>
              <Metric icon={<CloudRain/>} label="Rain chance" value={current.rain+'%'} note="Low probability"/>
            </div>
          </section>

          <section className="card insight-card"><div className="card-title"><div><span className="label">TODAY'S SIGNAL</span><h3>Atmosphere is stable</h3></div><span className="signal-tag">LOW VARIABILITY</span></div><p className="insight-copy">Pressure and humidity are holding steady. Expect mostly settled conditions through the evening, with a small chance of isolated showers.</p><div className="signal-bars"><div><span>Stability</span><b>78%</b><i><em style={{width:'78%'}}/></i></div><div><span>Moisture</span><b>61%</b><i><em style={{width:'61%'}}/></i></div><div><span>Convective energy</span><b>32%</b><i><em style={{width:'32%'}}/></i></div></div></section>
        </div>

        <div className="section-grid">
          <section className="card chart-card"><div className="card-title"><div><span className="label">TEMPERATURE</span><h3>Today's curve</h3></div><div className="mini-stat"><Thermometer size={15}/><strong>Peak 33°C</strong></div></div><div className="chart-wrap"><ResponsiveContainer width="100%" height="100%"><AreaChart data={temp}><defs><linearGradient id="fillTemp" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="currentColor" stopOpacity=".25"/><stop offset="100%" stopColor="currentColor" stopOpacity="0"/></linearGradient></defs><XAxis dataKey="time" tickLine={false} axisLine={false} tick={{fontSize:11}}/><YAxis hide domain={[20,36]}/><Tooltip contentStyle={{background:'var(--panel)',border:'1px solid var(--border)',borderRadius:12,fontSize:12}}/><Area type="monotone" dataKey="v" stroke="currentColor" fill="url(#fillTemp)" strokeWidth={2.5}/></AreaChart></ResponsiveContainer></div></section>

          <section className="card forecast-card"><div className="card-title"><div><span className="label">7 DAY FORECAST</span><h3>What's ahead</h3></div></div><div className="forecast-list">{forecast.map(([d,hi,lo,cond])=><div className="forecast-row" key={d}><span className="day">{d}</span><span className="forecast-icon">{cond==='Rain'?<CloudRain size={17}/>:cond==='Cloudy'?<CloudSun size={17}/>:<Sun size={17}/>}</span><span className="cond">{cond}</span><strong>{hi}</strong><span className="lo">{lo}</span></div>)}</div></section>
        </div>

        <div className="section-grid lower">
          <section className="card climate-card"><div className="card-title"><div><span className="label">CLIMATE CONTEXT</span><h3>Temperature anomaly</h3></div><span className="period">1991–2020 baseline</span></div><div className="climate-value">+0.8°<small>above baseline</small></div><div className="anomaly-line"><span>-1°</span><div><i/><b/></div><span>+1°</span></div><p className="muted small">Clima-Tact separates short-term weather noise from long-term climate signals.</p></section>
          <section className="card quick-card"><div className="card-title"><div><span className="label">EXPLORE</span><h3>Go deeper</h3></div></div><div className="quick-grid"><Quick icon={<Globe2/>} title="Weather visualizer" text="See atmospheric layers move across Earth" action={()=>setActive('Visualizer')}/><Quick icon={<Activity/>} title="Climate simulator" text="Experiment with simplified climate processes" action={()=>setActive('Simulator')}/><Quick icon={<BarChart3/>} title="Data lab" text="Analyze your own weather datasets" action={()=>setActive('Data Lab')}/></div></section>
        </div>
      </section>
    </main>
  </div>
}

function Metric({icon,label,value,note}:{icon:React.ReactNode;label:string;value:string;note:string}){return <div className="metric"><span className="metric-icon">{icon}</span><div><span className="label">{label}</span><strong>{value}</strong><small>{note}</small></div></div>}
function Quick({icon,title,text,action}:{icon:React.ReactNode;title:string;text:string;action:()=>void}){return <button className="quick" onClick={action}><span className="quick-icon">{icon}</span><span><strong>{title}</strong><small>{text}</small></span><span className="arrow">↗</span></button>}

export default App
