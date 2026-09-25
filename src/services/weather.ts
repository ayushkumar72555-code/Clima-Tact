export type WeatherData = {
  latitude:number; longitude:number; timezone:string; current:{
    temperature_2m:number; relative_humidity_2m:number; apparent_temperature:number;
    precipitation:number; rain:number; weather_code:number; surface_pressure:number;
    wind_speed_10m:number; wind_direction_10m:number;
  }; hourly:{time:string[];temperature_2m:number[];precipitation_probability:number[];weather_code:number[]};
  daily:{time:string[];weather_code:number[];temperature_2m_max:number[];temperature_2m_min:number[];precipitation_probability_max:number[];sunrise:string[];sunset:string[]};
}
export type Place={name:string;country:string;admin1?:string;latitude:number;longitude:number;timezone:string}
const forecastParams='current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&timezone=auto&forecast_days=7'
export async function searchPlaces(query:string):Promise<Place[]>{
  if(!query.trim()) return []
  const res=await fetch('https://geocoding-api.open-meteo.com/v1/search?name='+encodeURIComponent(query)+'&count=6&language=en&format=json')
  if(!res.ok) throw new Error('Location search failed')
  const data=await res.json()
  return (data.results??[]).map((p:any)=>({name:p.name,country:p.country,admin1:p.admin1,latitude:p.latitude,longitude:p.longitude,timezone:p.timezone}))
}
export async function getWeather(lat:number,lon:number):Promise<WeatherData>{
  const res=await fetch('https://api.open-meteo.com/v1/forecast?latitude='+lat+'&longitude='+lon+'&'+forecastParams)
  if(!res.ok) throw new Error('Weather request failed')
  return res.json()
}
export function weatherLabel(code:number){
  if(code===0)return 'Clear sky'; if([1,2,3].includes(code))return code===1?'Mainly clear':code===2?'Partly cloudy':'Overcast'
  if([45,48].includes(code))return 'Fog'; if([51,53,55,56,57].includes(code))return 'Drizzle'
  if([61,63,65,66,67,80,81,82].includes(code))return 'Rain'; if([71,73,75,77,85,86].includes(code))return 'Snow'
  if([95,96,99].includes(code))return 'Thunderstorm'; return 'Unknown'
}
export function windDirection(deg:number){const dirs=['N','NE','E','SE','S','SW','W','NW'];return dirs[Math.round(deg/45)%8]}
