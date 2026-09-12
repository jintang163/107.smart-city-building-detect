import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, InputNumber, Popconfirm, message, Card } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { listRoutes, createRoute, updateRoute, deleteRoute } from '../api'

export default function RouteList() {
  const [data, setData] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form] = Form.useForm()

  const load = () => listRoutes().then(setData)
  useEffect(() => { load() }, [])

  const submit = async () => {
    const values = await form.validateFields()
    // bbox 输入为 "minx,miny,maxx,maxy"
    if (values.bbox) {
      values.region = values.bbox.split(',').map(Number)
      delete values.bbox
    }
    if (editing) await updateRoute(editing.id, values)
    else await createRoute(values)
    message.success('保存成功')
    setOpen(false)
    load()
  }

  return (
    <Card
      title="航线管理"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); form.resetFields(); setOpen(true) }}>新建航线</Button>}
    >
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 60 },
          { title: '航线名称', dataIndex: 'name' },
          { title: '说明', dataIndex: 'description' },
          { title: '航高(m)', dataIndex: 'altitude' },
          { title: '重叠度(%)', dataIndex: 'overlap' },
          { title: '创建时间', dataIndex: 'createdAt' },
          {
            title: '操作',
            render: (_, r) => (
              <>
                <Button type="link" onClick={() => {
                  setEditing(r)
                  const ring = r.region?.coordinates?.[0]
                  form.setFieldsValue({
                    ...r,
                    bbox: ring ? [ring[0][0], ring[0][1], ring[2][0], ring[2][1]].join(',') : '',
                  })
                  setOpen(true)
                }}>编辑</Button>
                <Popconfirm title="确认删除？" onConfirm={() => deleteRoute(r.id).then(load)}>
                  <Button type="link" danger>删除</Button>
                </Popconfirm>
              </>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑航线' : '新建航线'}
        open={open}
        onOk={submit}
        onCancel={() => setOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="航线名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="bbox" label="覆盖范围 bbox（minx,miny,maxx,maxy，EPSG:4326）">
            <Input placeholder="114.30,30.55,114.36,30.60" />
          </Form.Item>
          <Form.Item name="altitude" label="航高（米）">
            <InputNumber min={50} max={1000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="overlap" label="重叠度（%）">
            <InputNumber min={0} max={95} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
