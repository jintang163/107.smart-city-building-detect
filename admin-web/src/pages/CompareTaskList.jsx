import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Select, InputNumber, Tag, message, Card } from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { listCompare, createCompare, listImagery } from '../api'

const STATUS = {
  PENDING: ['排队中', 'gold'], RUNNING: ['检测中', 'blue'],
  SUCCESS: ['已完成', 'green'], FAILED: ['失败', 'red'],
}

export default function CompareTaskList() {
  const [data, setData] = useState([])
  const [imagery, setImagery] = useState([])
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const load = () => {
    listCompare().then(setData)
    listImagery().then(setImagery)
  }
  useEffect(() => {
    load()
    const timer = setInterval(load, 5000) // 轮询任务状态
    return () => clearInterval(timer)
  }, [])

  const submit = async () => {
    const values = await form.validateFields()
    await createCompare(values)
    message.success('对比任务已创建，AI 检测中…')
    setOpen(false)
    load()
  }

  const options = imagery.map((i) => ({
    value: i.id,
    label: `#${i.id} ${i.name} (${i.captureDate || '未知日期'})`,
  }))

  return (
    <Card
      title="时序对比检测"
      extra={
        <>
          <Button icon={<ReloadOutlined />} onClick={load} style={{ marginRight: 8 }}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { form.resetFields(); setOpen(true) }}>发起对比</Button>
        </>
      }
    >
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 60 },
          { title: '历史底图', dataIndex: 'baseImageryName' },
          { title: '新影像', dataIndex: 'newImageryName' },
          {
            title: '状态', dataIndex: 'status',
            render: (s) => { const [t, c] = STATUS[s] || [s, 'default']; return <Tag color={c}>{t}</Tag> },
          },
          { title: '检出图斑', dataIndex: 'spotCount', render: (v) => `${v} 处` },
          { title: '最小面积(㎡)', dataIndex: 'minAreaM2' },
          { title: '错误信息', dataIndex: 'errorMsg', ellipsis: true },
          { title: '创建时间', dataIndex: 'createdAt' },
          {
            title: '操作',
            render: (_, r) => (
              <Button type="link" disabled={r.status !== 'SUCCESS'} onClick={() => navigate(`/spots?compareTaskId=${r.id}`)}>
                查看图斑
              </Button>
            ),
          },
        ]}
      />
      <Modal title="发起时序对比" open={open} onOk={submit} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical" initialValues={{ minAreaM2: 20 }}>
          <Form.Item name="baseImageryId" label="历史底图（旧）" rules={[{ required: true, message: '请选择历史底图' }]}>
            <Select options={options} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="newImageryId" label="新影像（本期）" rules={[{ required: true, message: '请选择新影像' }]}>
            <Select options={options} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="minAreaM2" label="最小图斑面积（㎡，过滤噪点）">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
