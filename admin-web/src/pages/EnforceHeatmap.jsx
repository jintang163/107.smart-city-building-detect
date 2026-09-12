import React, { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, Button, Empty } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { MapContainer, TileLayer, GeoJSON } from 'react-leaflet'
import { analysisHeatmap } from '../api'

/** 热力等级色带（1 低 ~ 5 高） */
const LEVEL_COLORS = { 1: '#52c41a', 2: '#a0d911', 3: '#faad14', 4: '#fa541c', 5: '#f5222d' }

export default function EnforceHeatmap() {
  const [data, setData] = useState(null)
  const [mapKey, setMapKey] = useState(0)

  const load = () => analysisHeatmap().then((d) => { setData(d); setMapKey((k) => k + 1) })
  useEffect(() => { load() }, [])

  const summary = data?.summary || {}
  const priorities = data?.priorities || []
  const geojson = data?.geojson

  return (
    <Card
      title="执法资源热力图（未处置工单网格密度，超期加权）"
      extra={<Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>}
    >
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}><Card size="small"><Statistic title="未处置工单" value={summary.openOrders || 0} suffix="件" /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="其中超期" value={summary.overdueOrders || 0} suffix="件" valueStyle={{ color: '#f5222d' }} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="热点网格" value={summary.hotspotGrids || 0} suffix="个" valueStyle={{ color: '#fa541c' }} /></Card></Col>
        <Col span={6}>
          <Card size="small" title="热力等级">
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 8 }}>
              {[1, 2, 3, 4, 5].map((l) => (
                <span key={l} style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                  <i style={{ width: 14, height: 14, background: LEVEL_COLORS[l], display: 'inline-block', borderRadius: 2 }} />
                  {l}
                </span>
              ))}
            </div>
          </Card>
        </Col>
      </Row>
      <Row gutter={16}>
        <Col span={16}>
          {geojson && geojson.features.length > 0 ? (
            <MapContainer key={mapKey} center={[30.575, 114.33]} zoom={13} style={{ height: 520, width: '100%', borderRadius: 8 }}>
              <TileLayer
                attribution='&copy; OpenStreetMap'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <GeoJSON
                data={geojson}
                style={(f) => ({
                  color: LEVEL_COLORS[f.properties.level],
                  weight: 1.5,
                  fillColor: LEVEL_COLORS[f.properties.level],
                  fillOpacity: 0.35,
                })}
                onEachFeature={(f, layer) => {
                  const p = f.properties
                  layer.bindTooltip(`网格 ${p.gridCode}：未处置 ${p.count} 件，超期 ${p.overdueCount} 件，权重 ${p.weight}`)
                }}
              />
            </MapContainer>
          ) : (
            <Empty
              style={{ padding: '120px 0', border: '1px dashed #d9d9d9', borderRadius: 8 }}
              description="暂无未处置工单。完成「图斑审核 → 生成工单」后，此处将按网格聚合显示执法热力。"
            />
          )}
        </Col>
        <Col span={8}>
          <Card size="small" title="巡查 / 无人机航线优先级 Top5" style={{ height: '100%' }}>
            <Table
              rowKey="gridCode" size="small" dataSource={priorities} pagination={false}
              locale={{ emptyText: '暂无数据' }}
              columns={[
                { title: '排名', width: 56, render: (_, __, i) => <Tag color={i < 3 ? 'red' : 'default'}>{i + 1}</Tag> },
                { title: '网格', dataIndex: 'gridCode', width: 110 },
                { title: '未处置', dataIndex: 'count', width: 70 },
                { title: '超期', dataIndex: 'overdueCount', width: 60, render: (v) => <span style={{ color: v > 0 ? '#f5222d' : undefined }}>{v}</span> },
                { title: '中心坐标', render: (_, r) => `${r.centerLng}, ${r.centerLat}` },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </Card>
  )
}
