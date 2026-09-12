import React, { useEffect, useState } from 'react'
import { MapContainer, TileLayer, GeoJSON, useMap } from 'react-leaflet'
import L from 'leaflet'

// 修复 leaflet 默认图标在打包下丢失的问题
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

function FitBounds({ data }) {
  const map = useMap()
  useEffect(() => {
    if (!data || !data.features || data.features.length === 0) return
    try {
      const layer = L.geoJSON(data)
      const bounds = layer.getBounds()
      if (bounds.isValid()) map.fitBounds(bounds, { padding: [30, 30] })
    } catch (e) { /* ignore */ }
  }, [data, map])
  return null
}

const STATUS_COLORS = { PENDING: '#faad14', CONFIRMED: '#f5222d', REJECTED: '#8c8c8c' }

/**
 * 通用 GeoJSON 地图
 * props: spots (FeatureCollection), imagery (FeatureCollection), onSpotClick(feature), height
 */
export default function GeoJsonMap({ spots, imagery, onSpotClick, height = 600 }) {
  const [key, setKey] = useState(0)
  useEffect(() => { setKey((k) => k + 1) }, [spots, imagery])

  return (
    <MapContainer center={[30.59, 114.30]} zoom={12} style={{ height, width: '100%', borderRadius: 8 }}>
      <TileLayer
        attribution='&copy; OpenStreetMap'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {imagery && (
        <GeoJSON
          key={`img-${key}`}
          data={imagery}
          style={{ color: '#1677ff', weight: 1.5, dashArray: '4', fillOpacity: 0.05 }}
          onEachFeature={(f, layer) => {
            layer.bindTooltip(`${f.properties.name} (${f.properties.captureDate || '-'})`)
          }}
        />
      )}
      {spots && (
        <GeoJSON
          key={`spot-${key}`}
          data={spots}
          style={(f) => ({
            color: STATUS_COLORS[f.properties.status] || '#faad14',
            weight: 2,
            fillOpacity: 0.35,
          })}
          onEachFeature={(f, layer) => {
            const p = f.properties
            layer.bindTooltip(
              `图斑#${p.id} 面积${Number(p.areaM2).toFixed(1)}㎡ 置信度${(p.confidence * 100).toFixed(0)}%`
            )
            layer.on('click', () => onSpotClick && onSpotClick(f))
          }}
        />
      )}
      <FitBounds data={spots && spots.features?.length ? spots : imagery} />
    </MapContainer>
  )
}
