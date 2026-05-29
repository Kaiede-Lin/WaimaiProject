/**
 * Amap JS API Key — MUST be configured for "Web端(JS API)" platform in the
 * Amap console (https://console.amap.com/dev/key/app).
 * A "Web服务" key will fail with: FlyDataAuthTask error: USERKEY_PLAT_NOMATCH
 */
const AMAP_KEY = '09f8c04a3c0163c7f9deb6c9563ab2a3'
const AMAP_VERSION = '2.0'
const AMAP_PLUGINS = 'AMap.Geolocation,AMap.Geocoder,AMap.PlaceSearch,AMap.AutoComplete'

function getA() {
  return (window as any).AMap || window.AMap
}

let loadPromise: Promise<void> | null = null

export function loadAmap(): Promise<void> {
  if (loadPromise) return loadPromise
  if (getA()) {
    loadPromise = Promise.resolve()
    return loadPromise
  }

  loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=${AMAP_VERSION}&key=${AMAP_KEY}&plugin=${AMAP_PLUGINS}`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Amap CDN failed to load'))
    document.head.appendChild(script)
  })
  return loadPromise
}

export function regeo(
  lng: number,
  lat: number
): Promise<{ address: string; components: any }> {
  return new Promise((resolve, reject) => {
    const A = getA()
    const geocoder = new A.Geocoder()
    geocoder.getAddress([lng, lat], (status: string, result: any) => {
      if (status === 'complete' && result.regeocode) {
        resolve({
          address: result.regeocode.formattedAddress,
          components: result.regeocode.addressComponent
        })
      } else {
        reject(new Error('Reverse geocode failed'))
      }
    })
  })
}

export function geo(
  address: string
): Promise<{ lng: number; lat: number }> {
  return new Promise((resolve, reject) => {
    const A = getA()
    const geocoder = new A.Geocoder()
    geocoder.getLocation(address, (status: string, result: any) => {
      if (status === 'complete' && result.geocodes?.length) {
        const loc = result.geocodes[0].location
        resolve({ lng: loc.getLng(), lat: loc.getLat() })
      } else {
        reject(new Error('Geocode failed'))
      }
    })
  })
}

export function searchNearby(
  keyword: string,
  lng: number,
  lat: number,
  radius: number = 10000
): Promise<any[]> {
  return new Promise((resolve, reject) => {
    const A = getA()
    const ps = new A.PlaceSearch({ pageSize: 25 })
    ps.searchNearBy(keyword, [lng, lat], radius, (status: string, result: any) => {
      if (status === 'complete') {
        resolve(result.pois || [])
      } else {
        reject(new Error('Place search failed'))
      }
    })
  })
}

export function getBrowserLocation(): Promise<{ lng: number; lat: number }> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({ lng: 116.397428, lat: 39.90923 })
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lng: pos.coords.longitude, lat: pos.coords.latitude }),
      () => resolve({ lng: 116.397428, lat: 39.90923 }),
      { timeout: 10000, maximumAge: 0 }
    )
  })
}
