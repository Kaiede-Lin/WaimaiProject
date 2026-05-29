declare namespace AMap {
  interface LngLat {
    getLng(): number
    getLat(): number
  }

  class LngLat {
    constructor(lng: number, lat: number)
  }

  interface Pixel {
    getX(): number
    getY(): number
  }

  interface MapOptions {
    zoom?: number
    center?: [number, number]
    resizeEnable?: boolean
    viewMode?: '2D' | '3D'
  }

  class Map {
    constructor(container: string | HTMLElement, opts?: MapOptions)
    setCenter(center: [number, number]): void
    getCenter(): { lng: number; lat: number }
    add(overlay: any): void
    remove(overlay: any): void
    clearMap(): void
    setFitView(overlays: any[], immediately?: boolean, rect?: number[]): void
    on(event: string, fn: (...args: any[]) => void): void
    off(event: string, fn: (...args: any[]) => void): void
    destroy(): void
    setZoom(zoom: number): void
    getZoom(): number
  }

  interface MarkerOptions {
    position?: [number, number]
    content?: string | HTMLElement
    offset?: Pixel
    icon?: string | Icon
    zIndex?: number
    title?: string
  }

  class Marker {
    constructor(opts?: MarkerOptions)
    setPosition(position: [number, number]): void
    setContent(content: string | HTMLElement): void
    getPosition(): { lng: number; lat: number }
    on(event: string, fn: (...args: any[]) => void): void
    remove(): void
    setTitle(title: string): void
    setLabel(label: any): void
  }

  class Pixel {
    constructor(x: number, y: number)
  }

  class Icon {
    constructor(opts: { size?: Pixel; image?: string; imageSize?: Pixel })
  }

  interface PolylineOptions {
    path?: [number, number][]
    strokeColor?: string
    strokeWeight?: number
    strokeStyle?: 'solid' | 'dashed'
    strokeOpacity?: number
    lineJoin?: 'round' | 'miter' | 'bevel'
  }

  class Polyline {
    constructor(opts?: PolylineOptions)
    setPath(path: [number, number][]): void
  }

  interface GeolocationOptions {
    enableHighAccuracy?: boolean
    timeout?: number
    maximumAge?: number
  }

  interface GeolocationResult {
    position: LngLat
    accuracy: number
    formattedAddress: string
    addressComponent: any
  }

  class Geolocation {
    constructor(opts?: GeolocationOptions)
    getCurrentPosition(
      callback: (status: string, result: GeolocationResult) => void
    ): void
  }

  interface RegeoResult {
    regeocode: {
      formattedAddress: string
      addressComponent: any
    }
  }

  interface GeoResult {
    geocodes: Array<{
      location: LngLat
      formattedAddress: string
    }>
  }

  class Geocoder {
    constructor()
    getAddress(
      location: [number, number],
      callback: (status: string, result: RegeoResult) => void
    ): void
    getLocation(
      address: string,
      callback: (status: string, result: GeoResult) => void
    ): void
  }

  interface POI {
    id: string
    name: string
    location: LngLat
    address: string
    distance: number
    tel: string
    type: string
  }

  interface PlaceSearchOptions {
    pageSize?: number
    pageIndex?: number
    type?: string
  }

  class PlaceSearch {
    constructor(opts?: PlaceSearchOptions)
    searchNearBy(
      keyword: string,
      center: [number, number],
      radius: number,
      callback: (status: string, result: { pois: POI[] }) => void
    ): void
  }

  interface AutoCompleteOptions {
    city?: string
    citylimit?: boolean
  }

  interface AutoCompleteTip {
    name: string
    location: LngLat
    address: string
  }

  class AutoComplete {
    constructor(opts?: AutoCompleteOptions)
    search(
      keyword: string,
      callback: (status: string, result: { tips: AutoCompleteTip[] }) => void
    ): void
  }

  interface DrivingOptions {
    map?: Map
    panel?: any
    autoFitView?: boolean
    showTraffic?: boolean
    policy?: number
  }

  interface DrivingResult {
    routes: Array<{
      steps: any[]
      distance: number
      time: number
    }>
  }

  class Driving {
    constructor(opts?: DrivingOptions)
    search(
      origin: LngLat | [number, number],
      destination: LngLat | [number, number],
      callback: (status: string, result: DrivingResult) => void
    ): void
    clear(): void
  }

  const plugin: (plugins: string[], callback: () => void) => void
}

declare var AMap: typeof AMap

interface Window {
  AMap: typeof AMap
}
