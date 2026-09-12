import React, { useEffect, useMemo, useState } from 'react'
import { Table, Button, Tag, Card, Modal, Input, Select, Row, Col, message } from 'antd'
import { useSearchParams } from 'react-router-dom'
import { listSpots, reviewSpot, mapImagery } from '../api'
import GeoJsonMap from '../components/GeoJsonMap'

const STATUS = { PENDING: ['待审核', 'gold'], CONFIRMED: ['已确认', 'red'], REJECTED: ['已排除', 'default'] }

export default function SpotReview() {
  const [params] = useSearchParams()
  const compareTaskId = params.get('compareTaskId')
  const [data, setData] = useState([])
  const [imageryFc, setImageryFc] = useState(null)
  const [status, setStatus] = useState()
  const [reviewing, setReviewing] = useState(null)
  const [comment, setComment] = useState('')

  const load = () => {
    listSpots({ status, compareTaskId }).then(setData)
    mapImagery().then(setImageryFc)
  }
  useEffect(() => { load() }, [status, compareTaskId])

  const spotsFc = useMemo(() => ({
    type: 'FeatureCollection',
    features: data.filter((s) => s.geom).map((s) => ({
      type: 'Feature',
      geometry: s.geom,
      properties: { id: s.id, areaM2: s.areaM2, confidence: s.confidence, status: s.status },
    })),
  }), [data])

  const review = async (approved) => {
    await reviewSpot(reviewing.id, { approved, comment })
    message.success(approved ? '已确认，核查工单已自动生成' : '已排除')
    setReviewing(null)
    setComment('')
    load()
  }

  return (
    <Row gutter={16}>
      <Col span={10}>
        <Card
          title={`图斑审核${compareTaskId ? `（对比任务 #${compareTaskId}）` : ''}`}
          extra={
            <Select
              allowClear placeholder="状态过滤" style={{ width: 120 }}
              value={status} onChange={setStatus}
              options={Object.entries(STATUS).map(([v, [t]]) => ({ value: v, label: t }))}
            />
          }
        >
          <Table
            rowKey="id"
            size="small"
            dataSource={data}
            columns={[
              { title: 'ID', dataIndex: 'id', width: 60 },
              { title: '面积(㎡)', dataIndex: 'areaM2', render: (v) => v?.toFixed(1), sorter: (a, b) => a.areaM2 - b.areaM2 },
              {
                title: '置信度', dataIndex: 'confidence',
                render: (v) => `${(v * 100).toFixed(0)}%`, sorter: (a, b) => a.confidence - b.confidence,
              },
              {
                title: '状态', dataIndex: 'status',
                render: (s) => { const [t, c] = STATUS[s] || [s, 'default']; return <Tag color={c}>{t}</Tag> },
              },
              {
                title: '操作',
                render: (_, r) => r.status === 'PENDING'
                  ? <Button type="link" onClick={() => setReviewing(r)}>审核</Button>
                  : <span style={{ color: '#999' }}>{r.reviewBy}</span>,
              },
            ]}
          />
        </Card>
      </Col>
      <Col span={14}>
        <Card title="空间分布（黄=待审核 红=已确认 灰=已排除）" styles={{ body: { padding: 0 } }}>
          <GeoJsonMap spots={spotsFc} imagery={imageryFc} height={640}
            onSpotClick={(f) => {
              const spot = data.find((s) => s.id === f.properties.id)
              if (spot && spot.status === 'PENDING') setReviewing(spot)
            }} />
        </Card>
      </Col>
      <Modal
        title={`审核图斑 #${reviewing?.id}`}
        open={!!reviewing}
        onCancel={() => setReviewing(null)}
        footer={[
          <Button key="reject" danger onClick={() => review(false)}>排除（误报）</Button>,
          <Button key="ok" type="primary" onClick={() => review(true)}>确认违建并生成工单</Button>,
        ]}
      >
        {reviewing && (
          <div>
            <p>面积：<b>{reviewing.areaM2?.toFixed(1)} ㎡</b>　置信度：<b>{(reviewing.confidence * 100).toFixed(0)}%</b></p>
            <Input.TextArea
              rows={3} placeholder="审核意见（可选）" value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          </div>
        )}
      </Modal>
    </Row>
  )
}
