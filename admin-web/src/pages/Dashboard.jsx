import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Table, Tag } from 'antd'
import { listSpots, listOrders, listCompare, listImagery } from '../api'

const ORDER_STATUS = {
  PENDING: ['待核查', 'gold'], INSPECTING: ['核查中', 'blue'], CONFIRMED: ['已认定', 'red'],
  EXCLUDED: ['已排除', 'default'], RECTIFYING: ['整改中', 'orange'], ARCHIVED: ['已归档', 'green'],
}

export default function Dashboard() {
  const [spots, setSpots] = useState([])
  const [orders, setOrders] = useState([])
  const [compares, setCompares] = useState([])
  const [imagery, setImagery] = useState([])

  useEffect(() => {
    Promise.all([listSpots(), listOrders(), listCompare(), listImagery()])
      .then(([s, o, c, i]) => { setSpots(s); setOrders(o); setCompares(c); setImagery(i) })
  }, [])

  const pendingSpots = spots.filter((s) => s.status === 'PENDING')
  const activeOrders = orders.filter((o) => !['ARCHIVED', 'EXCLUDED'].includes(o.status))

  return (
    <div>
      <Row gutter={16}>
        <Col span={6}><Card><Statistic title="已入库影像" value={imagery.length} suffix="景" /></Card></Col>
        <Col span={6}><Card><Statistic title="对比检测任务" value={compares.length} suffix="次" /></Card></Col>
        <Col span={6}><Card><Statistic title="待审核图斑" value={pendingSpots.length} suffix="处" valueStyle={{ color: '#faad14' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="处置中工单" value={activeOrders.length} suffix="件" valueStyle={{ color: '#f5222d' }} /></Card></Col>
      </Row>
      <Card title="最新工单" style={{ marginTop: 16 }}>
        <Table
          rowKey="id"
          size="small"
          pagination={false}
          dataSource={orders.slice(0, 8)}
          columns={[
            { title: '工单号', dataIndex: 'code' },
            { title: '标题', dataIndex: 'title' },
            { title: '面积(㎡)', dataIndex: 'spotAreaM2', render: (v) => v?.toFixed(1) },
            {
              title: '状态', dataIndex: 'status',
              render: (s) => { const [t, c] = ORDER_STATUS[s] || [s, 'default']; return <Tag color={c}>{t}</Tag> },
            },
            { title: '处置人', dataIndex: 'assigneeName', render: (v) => v || '-' },
            { title: '创建时间', dataIndex: 'createdAt' },
          ]}
        />
      </Card>
    </div>
  )
}
