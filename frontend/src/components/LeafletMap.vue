<template>
  <div ref="mapContainer" :style="containerStyle"></div>
</template>

<script>
import { onMounted, onBeforeUnmount, watch, ref } from 'vue'
import L from 'leaflet'
import 'leaflet-draw'

import markerIconUrl from 'leaflet/dist/images/marker-icon.png'
import markerIcon2xUrl from 'leaflet/dist/images/marker-icon-2x.png'
import markerShadowUrl from 'leaflet/dist/images/marker-shadow.png'

L.Icon.Default.mergeOptions({
  iconUrl: markerIconUrl,
  iconRetinaUrl: markerIcon2xUrl,
  shadowUrl: markerShadowUrl
})

export default {
  name: 'LeafletMap',
  props: {
    center: { type: Array, default: () => [22.383, 114.0823] },
    zoom: { type: Number, default: 13 },
    markers: { type: Array, default: () => [] }, // [{id, lat, lon, label}]
    tracks: { type: Array, default: () => [] }, // array of {latitude,longitude} or [[lat,lon],...]
    fences: { type: Array, default: () => [] }, // GeoJSON features
    editable: { type: Boolean, default: false },
    height: { type: String, default: '400px' }
  },
  emits: ['fence-created', 'fence-edited', 'fence-deleted', 'marker-click', 'map-ready'],
  setup(props, { emit }) {
    const map = ref(null)
    const mapContainer = ref(null)
    const containerStyle = { width: '100%', height: props.height }
    let markersLayer = null
    let tracksLayer = null
    let fencesLayer = null
    let drawControl = null

    onMounted(() => {
      if (!mapContainer.value) return
      map.value = L.map(mapContainer.value).setView(props.center, props.zoom)
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap contributors'
      }).addTo(map.value)

      markersLayer = L.layerGroup().addTo(map.value)
      tracksLayer = L.layerGroup().addTo(map.value)
      fencesLayer = new L.FeatureGroup().addTo(map.value)

      if (props.editable) {
        const options = {
          position: 'topright',
          draw: {
            polyline: false,
            polygon: true,
            rectangle: true,
            circle: true,
            marker: false,
            circlemarker: false
          },
          edit: {
            featureGroup: fencesLayer,
            remove: true
          }
        }
        drawControl = new L.Control.Draw(options)
        map.value.addControl(drawControl)

        map.value.on(L.Draw.Event.CREATED, function (e) {
          const layer = e.layer
          fencesLayer.addLayer(layer)
          // If circle, emit center + radius to preserve radius info
          if (layer instanceof L.Circle) {
            const c = layer.getLatLng()
            const r = layer.getRadius()
            emit('fence-created', { type: 'circle', center: [c.lat, c.lng], radius: r, geojson: layer.toGeoJSON() })
          } else {
            emit('fence-created', { type: 'geojson', geojson: layer.toGeoJSON() })
          }
        })

        map.value.on(L.Draw.Event.EDITED, function (e) {
          const layers = e.layers
          layers.eachLayer(l => {
            if (l instanceof L.Circle) {
              const c = l.getLatLng()
              const r = l.getRadius()
              emit('fence-edited', { type: 'circle', center: [c.lat, c.lng], radius: r, geojson: l.toGeoJSON() })
            } else {
              emit('fence-edited', { type: 'geojson', geojson: l.toGeoJSON() })
            }
          })
        })

        map.value.on(L.Draw.Event.DELETED, function (e) {
          const layers = e.layers
          layers.eachLayer(l => {
            if (l instanceof L.Circle) {
              const c = l.getLatLng()
              const r = l.getRadius()
              emit('fence-deleted', { type: 'circle', center: [c.lat, c.lng], radius: r, geojson: l.toGeoJSON() })
            } else {
              emit('fence-deleted', { type: 'geojson', geojson: l.toGeoJSON() })
            }
          })
        })
      }

      // initial population
      updateMarkers(props.markers)
      updateTracks(props.tracks)
      updateFences(props.fences)

      if (props.markers && props.markers.length) fitToMarkers()

      emit('map-ready', map.value)
    })

    onBeforeUnmount(() => {
      if (map.value) map.value.remove()
      map.value = null
    })

    function fitToMarkers() {
      try {
        const latlngs = props.markers.map(m => [Number(m.lat), Number(m.lon)])
        if (latlngs.length === 0) return
        const bounds = L.latLngBounds(latlngs)
        map.value.fitBounds(bounds, { padding: [50, 50] })
      } catch (e) {
        // ignore
      }
    }

    function updateMarkers(list) {
      if (!markersLayer) return
      markersLayer.clearLayers()
      if (!Array.isArray(list)) return
      list.forEach(m => {
        if (m == null || m.lat == null || m.lon == null) return
        const mk = L.marker([Number(m.lat), Number(m.lon)])
        if (m.label) mk.bindPopup(m.label)
        mk.on('click', () => emit('marker-click', m))
        markersLayer.addLayer(mk)
      })
    }

    function updateTracks(tracks) {
      if (!tracksLayer) return
      tracksLayer.clearLayers()
      if (!tracks) return
      // tracks can be array of latlngs or array of objects
      if (Array.isArray(tracks) && tracks.length > 0 && Array.isArray(tracks[0]) && typeof tracks[0][0] === 'number') {
        const poly = L.polyline(tracks.map(p => [Number(p[0]), Number(p[1])]), { color: 'blue' })
        tracksLayer.addLayer(poly)
      } else if (Array.isArray(tracks)) {
        const pts = tracks.map(pt => {
          if (Array.isArray(pt)) return [Number(pt[0]), Number(pt[1])]
          return [Number(pt.latitude || pt.lat), Number(pt.longitude || pt.lon)]
        }).filter(p => Number.isFinite(p[0]) && Number.isFinite(p[1]))
        if (pts.length > 0) {
          const poly = L.polyline(pts, { color: 'blue' })
          tracksLayer.addLayer(poly)
        }
      }
    }

    function updateFences(flist) {
      if (!fencesLayer) return
      fencesLayer.clearLayers()
      if (!Array.isArray(flist)) return
      flist.forEach(f => {
        try {
          if (f && f.type && f.type === 'Feature') {
            L.geoJSON(f).eachLayer(l => fencesLayer.addLayer(l))
          } else if (f && f.type && f.type === 'Polygon') {
            L.geoJSON({ type: 'Feature', geometry: f }).eachLayer(l => fencesLayer.addLayer(l))
          } else if (f && f.geometry) {
            L.geoJSON(f).eachLayer(l => fencesLayer.addLayer(l))
          }
        } catch (e) {
          // ignore invalid
        }
      })
    }

    watch(() => props.markers, (nv) => updateMarkers(nv), { deep: true })
    watch(() => props.tracks, (nv) => updateTracks(nv), { deep: true })
    watch(() => props.fences, (nv) => updateFences(nv), { deep: true })

    function centerOn(lat, lon, zoom) {
      if (!map.value) return
      map.value.setView([lat, lon], zoom || map.value.getZoom())
    }

    return {
      mapContainer,
      containerStyle,
      centerOn
    }
  }
}
</script>

<style scoped>
:host {
  display: block;
}
</style>
