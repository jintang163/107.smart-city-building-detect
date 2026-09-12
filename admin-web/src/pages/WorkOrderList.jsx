import React, { useEffect, useState } from 'react'
import { Table, Button, Tag, Card, Modal, Select, Input, Timeline, Descriptions, message } from 'antd'
import { listOrders, orderDetail, orderAction, listUsers } from '../api'

const STATUS = {
  PENDING: ['待核查', 'gold'], INSPECTING: ['核查中', 'blue'], CONFIRMED: ['已认定', 'red'],
  EXCLUDED: ['已排除', 'default'], RECTIFYING: ['整改中', 'orange'], ARCHIVED: ['已归档', 'green'],
}
const ACTION_LABEL = { ASSIGN: '派单', CONFIRM: '认定违建', EXCLUDE: '排除', RECTIFY: '发起整改', ARCHIVE: '归档' }

export default function WorkOrderList() {
  const [data, setData] = useState([])
  const [users, setUsers] = useState([])
  const [status, setStatus] = useState()
  const [detail, setDetail] = useState(null)
  const [assigning, setAssigning] = useState(null)
  const [assigneeId, setAssigneeId] = useState()
  const [actionModal, setActionModal] = useState(null) // {order, action}
  const [comment, setComment] = useState('')

  const load = () => {
    listOrders({ status }).then(setData)
    listUsers().then((us) => setUsers(us.filter((u) => u.role === 'OPERATOR')))
  }
  useEffect(() => { load() }, [status])

  const doAction = async (orderId, action, extra = {}) => {
    await orderAction(orderId, { action, ...extra })
    message.success('操作成功')
    setAssigning(null); setActionModal(null); setComment('')
    load()
    if (detail?.id === orderId) setDetail(await orderDetail(orderId))
  }

  const openDetail = async (r) => setDetail(await orderDetail(r.id))

  const nextActions = (o) => {
    switch (o.status) {
      case 'PENDING': return [<Button key="a" type="link" onClick={() => setAssigning(o)}>派单</Button>]
      case 'INSPECTING': return [
        <Button key="c" type="link" onClick={() => setActionModal({ order: o, action: 'CONFIRM' })}>认定</Button>,
        <Button key="e" type="link" onClick={() => setActionModal({ order: o, action: 'EXCLUDE' })}>排除</Button>,
      ]
      case 'CONFIRMED': return [
        <Button key="r" type="link" onClick={() => setActionModal({ order: o, action: 'RECTIFY' })}>发起整改</Button>,
        <Button key="ar" type="link" onClick={() => setActionModal({ order: o, action: 'ARCHIVE' })}>归档</Button>,
      ]
      case 'RECTIFYING': return [
        <Button key="ar" type="link" onClick={() => setActionModal({ order: o, action: 'ARCHIVE' })}>归档</Button>,
      ]
      default: return []
    }
  }

  return (
    <Card
      title="工单处置"
      extra={
        <Select
          allowClear placeholder="状态过滤" style={{ width: 140 }}
          value={status} onChange={setStatus}
          options={Object.entries(STATUS).map(([v, [t]]) => ({ value: v, label: t }))}
        />
      }
    >
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: '工单号', dataIndex: 'code', width: 180 },
          { title: '标题', dataIndex: 'title' },
          { title: '面积(㎡)', dataIndex: 'spotAreaM2', render: (v) => v?.toFixed(1) },
          {
            title: '状态', dataIndex: 'status',
            render: (s) => { const [t, c] = STATUS[s] || [s, 'default']; return <Tag color={c}>{t}</Tag> },
          },
          { title: '处置人', dataIndex: 'assigneeName', render: (v) => v || '-' },
          { title: '创建时间', dataIndex: 'createdAt' },
          {
            title: '操作',
            render: (_, r) => (
              <>
                <Button type="link" onClick={() => openDetail(r)}>详情</Button>
                {nextActions(r)}
              </>
            ),
          },
        ]}
      />

      {/* 派单 */}
      <Modal
        title={`派单 - ${assigning?.code}`}
        open={!!assigning}
        onCancel={() => setAssigning(null)}
        onOk={() => doAction(assigning.id, 'ASSIGN', { assigneeId })}
      >
        <Select
          style={{ width: '100%' }} placeholder="选择处置队员"
          value={assigneeId} onChange={setAssigneeId}
          options={users.map((u) => ({ value: u.id, label: `${u.realName}（${u.username}）` }))}
        />
      </Modal>

      {/* 其他动作 */}
      <Modal
        title={`${ACTION_LABEL[actionModal?.action]} - ${actionModal?.order.code}`}
        open={!!actionModal}
        onCancel={() => setActionModal(null)}
        onOk={() => doAction(actionModal.order.id, actionModal.action, { comment })}
      >
        <Input.TextArea rows={3} placeholder="处置意见" value={comment} onChange={(e) => setComment(e.target.value)} />
      </Modal>

      {/* 详情 */}
      <Modal title={`工单详情 - ${detail?.code}`} open={!!detail} footer={null} onCancel={() => setDetail(null)} width={640}>
        {detail && (
          <>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="标题">{detail.title}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS[detail.status]?.[1]}>{STATUS[detail.status]?.[0]}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="图斑面积">{detail.spotAreaM2?.toFixed(1)} ㎡</Descriptions.Item>
              <Descriptions.Item label="AI 置信度">{(detail.spotConfidence * 100).toFixed(0)}%</Descriptions.Item>
              <Descriptions.Item label="处置人">{detail.assigneeName || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{detail.createdAt}</Descriptions.Item>
              <Descriptions.Item label="说明" span={2}>{detail.description}</Descriptions.Item>
            </Descriptions>
            <h4 style={{ marginTop: 16 }}>流转记录</h4>
            <Timeline
              items={detail.logs.map((l) => ({
                children: `${l.createdAt}　${ACTION_LABEL[l.action] || l.action}　${l.operatorName}　${l.comment || ''}`,
              }))}
            />
          </>
        )}
      </Modal>
    </Card>
  )
}
