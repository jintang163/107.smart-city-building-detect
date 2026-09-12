import React, { useEffect, useState } from 'react'
import { Card, Select, Space, Tag } from 'antd'
import { mapSpots, mapImagery } from '../api'
import GeoJsonMap from '../components/GeoJsonMap'

export default function MapQuery() {
  const [spots, setSpots] = useState(null)
  const [imagery, setImagery] = useState(null)
  const [status, setStatus] = useState()

  useEffect(() => {
    mapSpots(status).then(setSpots)
    mapImagery().then(setImagery)
  }, [status])

  return (
    <Card
      title="地图查询"
      extra={
        <Space>
          <Tag color="gold">待审核</Tag><Tag color="red">已确认</Tag><Tag color="default">已排除</Tag>
          <Select
            allowClear placeholder="图斑状态" style={{ width: 140 }}
            value={status} onChange={setStatus}
            options={[
              { value: 'PENDING', label: '待审核' },
              { value: 'CONFIRMED', label: '已确认' },
              { value: 'REJECTED', label: '已排除' },
            ]}
          />
        </Space>
      }
      styles={{ body: { padding: 0 } }}
    >
      <GeoJsonMap spots={spots} imagery={imagery} height={720} />
    </Card>
  )
}
