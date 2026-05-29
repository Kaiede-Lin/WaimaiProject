/**
 * Delivery fee: base ¥3 + ¥1.5/km, capped at ¥12
 */
export function calcDeliveryFee(distanceKm: number): number {
  if (!distanceKm || distanceKm <= 0) return 5
  return Math.round(Math.min(3 + distanceKm * 1.5, 12) * 100) / 100
}

/**
 * Delivery time: base 20min + 3min/km, capped at 60min
 */
export function calcDeliveryTime(distanceKm: number): number {
  if (!distanceKm || distanceKm <= 0) return 30
  return Math.round(Math.min(20 + distanceKm * 3, 60))
}

/**
 * Haversine distance in km between two lat/lng points
 */
export function haversineKm(
  lat1: number, lng1: number,
  lat2: number, lng2: number
): number {
  const R = 6371
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180)
    * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

const LOC_KEY = 'userLocation'

export function getStoredLocation(): { lng: number; lat: number } {
  try {
    const raw = localStorage.getItem(LOC_KEY)
    if (raw) return JSON.parse(raw)
  } catch {}
  return { lng: 116.397428, lat: 39.90923 }
}

export function saveLocation(lng: number, lat: number): void {
  localStorage.setItem(LOC_KEY, JSON.stringify({ lng, lat }))
}

export function setLocationFromAddress(addr: { longitude?: number | string; latitude?: number | string; address?: string } | null): void {
  if (!addr) return
  const lng = Number(addr.longitude)
  const lat = Number(addr.latitude)
  if (!isNaN(lng) && !isNaN(lat) && lng !== 0 && lat !== 0) {
    saveLocation(lng, lat)
  }
  if (addr.address) {
    localStorage.setItem('lastAddress', addr.address)
  }
}

export function getLastAddress(): string {
  return localStorage.getItem('lastAddress') || ''
}
