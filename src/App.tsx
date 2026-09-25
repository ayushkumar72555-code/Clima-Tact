import { useEffect, useState } from 'react'
import {
  Activity, BarChart3, CloudRain, CloudSun, Droplets, Gauge, Globe2, LayoutDashboard,
  MapPin, Menu, Moon, Search, Settings2, Sun, Thermometer, Wind, X, Loader2
} from 'lucide-react'
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { getWeather, searchPlaces, weatherLabel, windDirection, type WeatherData, type Place } from './services/weather'

type Page='Overview'|'Weather'|'Climate'|'Visualizer'|'Simulator'|'Data Lab'
type NavItem={label:Page;icon:typeof LayoutDashboard}
const nav:NavItem[]=[
  {label:'Overview',icon:LayoutDashboard},{label:'Weather',icon:CloudSun},
  {label:'Climate',icon:BarChart3},{label:'Visualizer',icon:Globe2},
  {label:'Simulator',icon:Activity},{label:'Data Lab',icon:Gauge}
]
const defaultPlace:Place={name:'Lucknow',country:'India',latitude:26.8467,longitude:80.9462,timezone:'Asia/Kolkata'}

function App(){
  const [active,setActive]=useState<Page>('Overview')
  const [mobileOpen,setMobileOpen]=useState(false)
  const [dark,setDark]=useState(true)
  const [place,setPlace]=useState<Place>(defaultPlace)
  const [weather,setWeather]=useState<WeatherData|null>(null)
  const [loading,setLoading]=useState(true)
  const [error,setError]=useState('')
  const [query,setQuery]=useState('')
  const [results,setResults]=useState<Place[]>([])
  const [searching,setSearching]=useState(false)

  const loadWeather=async(p:Place)=>{
    setLoading(true);setError('')
    try{setWeather(await getWeather(p.latitude,p.longitude))}
    catch{setError('Live weather data could not be loaded. Check your connection and try again.')}
    finally{setLoading(false)}
  }
  useEffect(()=>{loadWeather(place)},[place])
  useEffect(()=>{
    const t=setTimeout(async()=>{
      if(query.trim().length<2){setResults([]);return}
      setSearching(true)
      try{setResults(await searchPlaces(query))}catch{setResults([])}finally{setSearching(false)}
    },350)
    return()=>clearTimeout(t)
  },[query])

  const choosePlace=(p:Place)=>{setPlace(p);setQuery('');setResults([])}
  const dateLabel=new Intl.DateTimeFormat('en-IN',{weekday:'short',day:'numeric',month:'short'}).format(new Date())
  return <div className={dark?'app dark':'app'}>
    <aside className={mobileOpen?'sidebar open':'sidebar'}>
      <div className="brand"><div className="brand-mark"><CloudSun size={22}/></div><div><strong>Clima-Tact</strong><span>Weather & Climate Observatory</span></div><button className="icon-btn mobile-close" onClick={()=>setMobileOpen(false)}><X size={18}/></button></div>
      <nav>{nav.map(({label,icon:Icon})=><button key={label} className={active===label?'nav-item active':'nav-item'} onClick={()=>{setActive(label);setMobileOpen(false)}}><Icon size={18}/><span>{label}</span></button>)}</nav>
      <div className="sidebar-bottom"><div className="status"><span className="pulse"></span><span>{weather?'Live data connected':'Connecting to data'}</span></div><button className="settings"><Settings2 size={17}/> Settings</button></div>
    </aside>
    {mobileOpen&&<button className="backdrop" onClick={()=>setMobileOpen(false)} aria-label="Close menu"/>}
    <main className="main">
      <header className="topbar">
        <button className="icon-btn mobile-menu" onClick={()=>setMobileOpen(true)}><Menu size={20}/></button>
        <div className="location"><MapPin size={17}/><span>{place.name}, {place.country}</span><span className="dot"></span><small>{dateLabel}</small></div>
        <div className="top-actions">
          <div className="search"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search location..." /><kbd>⌘ K</kbd>
            {(results.length>0||searching)&&<div className="search-results">{searching?<div className="search-state"><Loader2 className="spin" size={15}/> Searching…</div>:results.map(p=><button key={p.latitude+':'+p.longitude} onClick={()=>choosePlace(p)}><MapPin size={14}/><span><strong>{p.name}</strong><small>{p.admin1?p.admin1+', ':''}{p.country}</small></span></button>)}</div>}
          </div>
          <button className="icon-btn" onClick={()=>setDark(!dark)} aria-label="Toggle theme">{dark?<Sun size={18}/>:<Moon size={18}/>}</button><div className="avatar">CT</div>
        </div>
      </header>

      {active==='Overview'&&<Overview weather={weather} loading={loading} error={error} setActive={setActive}/>}
      {active==='Weather'&&<WeatherPage weather={weather} loading={loading} error={error} place={place} reload={()=>loadWeather(place)}/>}
      {active!=='Overview'&&active!=='Weather'&&<ComingSoon page={active} setActive={setActive}/>}
    </main>
  </div>
}

function Overview({weather,loading,error,setActive}:{weather:WeatherData|null;loading:boolean;error:string;setActive:(p:Page)=>void}){
  const c=weather?.current
  const chart=weather?weather.hourly.time.slice(0,24).map((t,i)=>({time:new Date(t).toLocaleTimeString([], {hour:'2-digit'}),v:Math.round(weather.hourly.temperature_2m[i])})): []
  const daily=weather?.daily.time.map((d,i)=>({date:new Date(d+'T12:00:00'),hi:weather.daily.temperature_2m_max[i],lo:weather.daily.temperature_2m_min[i],code:weather.daily.weather_code[i]}))??[]
  return <section className="content">
    <div className="page-head"><div><p className="eyebrow">Atmospheric overview</p><h1>Weather, in context.</h1><p className="muted">Live observations now feed the Clima-Tact dashboard. Climate analysis comes next.</p></div></div>
    {error&&<div className="error-banner">{error}</div>}
    <div className="hero-grid">
      <section className="card current-card">
        <div className="card-top"><div><span className="label">CURRENT WEATHER</span>{loading?<div className="loading-value"><Loader2 className="spin" size={28}/></div>:<><h2>{Math.round(c?.temperature_2m??0)}°<span>C</span></h2><p>{weatherLabel(c?.weather_code??0)} · Feels like {Math.round(c?.apparent_temperature??0)}°</p></>}</div><div className="weather-orb"><Sun size={48}/><span className="orb-cloud"><CloudSun size={42}/></span></div></div>
        <div className="metrics">
          <Metric icon={<Droplets/>} label="Humidity" value={c?c.relative_humidity_2m+'%':'—'} note="Relative humidity"/>
          <Metric icon={<Gauge/>} label="Pressure" value={c?Math.round(c.surface_pressure)+' hPa':'—'} note="Surface pressure"/>
          <Metric icon={<Wind/>} label="Wind" value={c?Math.round(c.wind_speed_10m)+' km/h':'—'} note={c?windDirection(c.wind_direction_10m)+' · '+Math.round(c.wind_direction_10m)+'°':'—'}/>
          <Metric icon={<CloudRain/>} label="Rain chance" value={weather?Math.round(weather.daily.precipitation_probability_max[0])+'%':'—'} note="Today's forecast"/>
        </div>
      </section>
      <section className="card insight-card"><div className="card-title"><div><span className="label">DATA STATUS</span><h3>{loading?'Connecting…':'Live observations active'}</h3></div><span className="signal-tag">{weather?'LIVE':'WAITING'}</span></div><p className="insight-copy">{weather?'Current conditions are being supplied by Open-Meteo. The dashboard is now ready for forecast and climate layers.':'Connecting to the weather service…'}</p><div className="signal-bars"><div><span>Temperature</span><b>{c?Math.round(c.temperature_2m)+'°C':'—'}</b><i><em style={{width:c?Math.min(100,Math.max(10,(c.temperature_2m/45)*100))+'%':'0%'}}/></i></div><div><span>Humidity</span><b>{c?c.relative_humidity_2m+'%':'—'}</b><i><em style={{width:c?c.relative_humidity_2m+'%':'0%'}}/></i></div><div><span>Precipitation</span><b>{weather?Math.round(weather.daily.precipitation_probability_max[0])+'%':'—'}</b><i><em style={{width:weather?weather.daily.precipitation_probability_max[0]+'%':'0%'}}/></i></div></div></section>
    </div>
    <div className="section-grid">
      <section className="card chart-card"><div className="card-title"><div><span className="label">TEMPERATURE</span><h3>Next 24 hours</h3></div><div className="mini-stat"><Thermometer size={15}/><strong>{weather?Math.round(Math.max(...weather.hourly.temperature_2m.slice(0,24)))+'°C peak':'Loading'}</strong></div></div><div className="chart-wrap"><ResponsiveContainer width="100%" height="100%"><AreaChart data={chart}><defs><linearGradient id="fillTemp" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="currentColor" stopOpacity=".25"/><stop offset="100%" stopColor="currentColor" stopOpacity="0"/></linearGradient></defs><XAxis dataKey="time" tickLine={false} axisLine={false} tick={{fontSize:10}} interval={3}/><YAxis hide domain={['dataMin - 2','dataMax + 2']}/><Tooltip contentStyle={{background:'var(--panel)',border:'1px solid var(--border)',borderRadius:12,fontSize:12}}/><Area type="monotone" dataKey="v" stroke="currentColor" fill="url(#fillTemp)" strokeWidth={2.5}/></AreaChart></ResponsiveContainer></div></section>
      <section className="card forecast-card"><div className="card-title"><div><span className="label">7 DAY FORECAST</span><h3>What's ahead</h3></div></div><div className="forecast-list">{daily.map((d,i)=><div className="forecast-row" key={d.date.toISOString()}><span className="day">{i===0?'Today':d.date.toLocaleDateString('en-IN',{weekday:'short'})}</span><span className="forecast-icon">{d.code>=51?<CloudRain size={17}/>:d.code>=1?<CloudSun size={17}/>:<Sun size={17}/>}</span><span className="cond">{weatherLabel(d.code)}</span><strong>{Math.round(d.hi)}°</strong><span className="lo">{Math.round(d.lo)}°</span></div>)}</div></section>
    </div>
    <div className="section-grid lower">
      <section className="card climate-card"><div className="card-title"><div><span className="label">NEXT MODULE</span><h3>Climate Explorer</h3></div><span className="period">Phase 3</span></div><div className="climate-value">Historical<small>trends, anomalies & extremes</small></div><p className="muted small">Clima-Tact will separate short-term weather from long-term climate statistics using historical datasets.</p><button className="inline-link" onClick={()=>setActive('Climate')}>Open Climate →</button></section>
      <section className="card quick-card"><div className="card-title"><div><span className="label">EXPLORE</span><h3>Go deeper</h3></div></div><div className="quick-grid"><Quick icon={<Globe2/>} title="Weather visualizer" text="See atmospheric layers move across Earth" action={()=>setActive('Visualizer')}/><Quick icon={<Activity/>} title="Climate simulator" text="Experiment with simplified climate processes" action={()=>setActive('Simulator')}/><Quick icon={<BarChart3/>} title="Data lab" text="Analyze your own weather datasets" action={()=>setActive('Data Lab')}/></div></section>
    </div>
  </section>
}

function WeatherPage({weather,loading,error,place,reload}:{weather:WeatherData|null;loading:boolean;error:string;place:Place;reload:()=>void}){
  const c=weather?.current
  const hours=weather?.hourly.time.slice(0,24).map((t,i)=>({time:new Date(t).toLocaleTimeString([], {hour:'numeric'}),temp:Math.round(weather.hourly.temperature_2m[i]),rain:weather.hourly.precipitation_probability[i]}))??[]
  return <section className="content"><div className="page-head"><div><p className="eyebrow">Live meteorology</p><h1>{place.name} weather.</h1><p className="muted">Current observations and the next seven days, updated directly from the weather data service.</p></div><button className="location-btn" onClick={reload}><Activity size={16}/> Refresh data</button></div>
    {error&&<div className="error-banner">{error}</div>}
    <div className="weather-detail-grid">
      <section className="card weather-big"><div className="label">NOW</div>{loading?<div className="loading-value"><Loader2 className="spin" size={28}/> Loading observations</div>:<><div className="big-temp">{Math.round(c?.temperature_2m??0)}°<small>C</small></div><h3>{weatherLabel(c?.weather_code??0)}</h3><p className="muted">Feels like {Math.round(c?.apparent_temperature??0)}° · Wind {Math.round(c?.wind_speed_10m??0)} km/h {windDirection(c?.wind_direction_10m??0)}</p></>}</section>
      <MetricPanel icon={<Droplets/>} label="Humidity" value={c?c.relative_humidity_2m+'%':'—'} detail="Relative humidity"/>
      <MetricPanel icon={<Gauge/>} label="Pressure" value={c?Math.round(c.surface_pressure)+' hPa':'—'} detail="Surface pressure"/>
      <MetricPanel icon={<CloudRain/>} label="Precipitation" value={c?c.precipitation+' mm':'—'} detail="Current hour"/>
      <MetricPanel icon={<Wind/>} label="Wind" value={c?Math.round(c.wind_speed_10m)+' km/h':'—'} detail={c?windDirection(c.wind_direction_10m)+' direction':'—'}/>
    </div>
    <section className="card chart-card full"><div className="card-title"><div><span className="label">HOURLY OUTLOOK</span><h3>Temperature & rain probability</h3></div></div><div className="chart-wrap tall"><ResponsiveContainer width="100%" height="100%"><AreaChart data={hours}><defs><linearGradient id="weatherTemp" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="currentColor" stopOpacity=".22"/><stop offset="100%" stopColor="currentColor" stopOpacity="0"/></linearGradient></defs><XAxis dataKey="time" tickLine={false} axisLine={false} tick={{fontSize:10}} interval={2}/><YAxis yAxisId="temp" hide/><YAxis yAxisId="rain" orientation="right" hide/><Tooltip contentStyle={{background:'var(--panel)',border:'1px solid var(--border)',borderRadius:12,fontSize:12}}/><Area yAxisId="temp" type="monotone" dataKey="temp" stroke="currentColor" fill="url(#weatherTemp)" strokeWidth={2.5}/><Area yAxisId="rain" type="monotone" dataKey="rain" stroke="var(--accent2)" fill="none" strokeWidth={1.5}/></AreaChart></ResponsiveContainer></div></section>
  </section>
}

function ComingSoon({page,setActive}:{page:Page;setActive:(p:Page)=>void}){return <section className="empty-page"><div className="empty-icon">{page==='Visualizer'?<Globe2/>:page==='Simulator'?<Activity/>:page==='Climate'?<BarChart3/>:<Gauge/>}</div><p className="eyebrow">Clima-Tact · Next module</p><h1>{page}</h1><p>We're building this module on top of the live data layer. The foundation is ready.</p><button className="location-btn" onClick={()=>setActive('Overview')}>← Back to overview</button></section>}

function Metric({icon,label,value,note}:{icon:React.ReactNode;label:string;value:string;note:string}){return <div className="metric"><span className="metric-icon">{icon}</span><div><span className="label">{label}</span><strong>{value}</strong><small>{note}</small></div></div>}
function MetricPanel({icon,label,value,detail}:{icon:React.ReactNode;label:string;value:string;detail:string}){return <div className="card metric-panel"><span className="panel-icon">{icon}</span><span className="label">{label}</span><strong>{value}</strong><small>{detail}</small></div>}
function Quick({icon,title,text,action}:{icon:React.ReactNode;title:string;text:string;action:()=>void}){return <button className="quick" onClick={action}><span className="quick-icon">{icon}</span><span><strong>{title}</strong><small>{text}</small></span><span className="arrow">↗</span></button>}

export default App
