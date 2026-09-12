import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, DatePicker, Popconfirm, Tag, message, Card } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { listTasks, createTask, updateTask, deleteTask, listRoutes } from '../api'

const STATUS = { PLANNED: ['已规划', 'gold'], EXECUTING: ['执行中', 'blue'], DONE: ['已完成', 'green'] }

export default function FlightTaskList() {
  const [data, setData] = useState([])
  const [routes, setRoutes] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form] = Form.useForm()

  const load = () => {
    listTasks().then(setData)
    listRoutes().then(setRoutes)
  }
  useEffect(() => { load() }, [])

  const submit = async () => {
    const values = await form.validateFields()
    if (values.plannedDate) values.plannedDate = values.plannedDate.format('YYYY-MM-DD')
    if (editing) await updateTask(editing.id, values)
    else await createTask(values)
    message.success('保存成功')
    setOpen(false)
    load()
  }

  return (
    <Card
      title="航拍任务"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); form.resetFields(); setOpen(true) }}>新建任务</Button>}
    >
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 60 },
          { title: '任务名称', dataIndex: 'name' },
          { title: '所属航线', dataIndex: 'routeName', render: (v) => v || '-' },
          {
            title: '状态', dataIndex: 'status',
            render: (s) => { const [t, c] = STATUS[s] || [s, 'default']; return <Tag color={c}>{t}</Tag> },
          },
          { title: '计划日期', dataIndex: 'plannedDate' },
          { title: '执行人', dataIndex: 'operator' },
          {
            title: '操作',
            render: (_, r) => (
              <>
                <Button type="link" onClick={() => {
                  setEditing(r)
                  form.setFieldsValue({ ...r, routeId: r.routeId, plannedDate: r.plannedDate ? dayjs(r.plannedDate) : null })
                  setOpen(true)
                }}>编辑</Button>
                <Popconfirm title="确认删除？" onConfirm={() => deleteTask(r.id).then(load)}>
                  <Button type="link" danger>删除</Button>
                </Popconfirm>
              </>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑任务' : '新建任务'}
        open={open}
        onOk={submit}
        onCancel={() => setOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="任务名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="routeId" label="所属航线">
            <Select options={routes.map((r) => ({ value: r.id, label: r.name }))} allowClear />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="PLANNED">
            <Select options={Object.entries(STATUS).map(([v, [t]]) => ({ value: v, label: t }))} />
          </Form.Item>
          <Form.Item name="plannedDate" label="计划日期">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="operator" label="执行人">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
