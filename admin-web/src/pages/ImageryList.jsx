import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, DatePicker, Upload, InputNumber, Popconfirm, Tag, message, Card, Image } from 'antd'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { listImagery, uploadImagery, deleteImagery, previewImagery, listTasks } from '../api'

export default function ImageryList() {
  const [data, setData] = useState([])
  const [tasks, setTasks] = useState([])
  const [open, setOpen] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [previewUrl, setPreviewUrl] = useState(null)
  const [form] = Form.useForm()

  const load = () => {
    listImagery().then(setData)
    listTasks().then(setTasks)
  }
  useEffect(() => { load() }, [])

  const submit = async () => {
    const values = await form.validateFields()
    const file = values.file?.[0]?.originFileObj
    if (!file) { message.warning('请选择影像文件'); return }
    const fd = new FormData()
    fd.append('file', file)
    fd.append('name', values.name)
    if (values.taskId) fd.append('taskId', values.taskId)
    fd.append('type', values.type || 'ORTHO')
    if (values.captureDate) fd.append('captureDate', values.captureDate.format('YYYY-MM-DD'))
    fd.append('minx', values.minx); fd.append('miny', values.miny)
    fd.append('maxx', values.maxx); fd.append('maxy', values.maxy)
    setUploading(true)
    try {
      await uploadImagery(fd)
      message.success('影像入库成功')
      setOpen(false)
      load()
    } finally {
      setUploading(false)
    }
  }

  const preview = async (r) => {
    const { url } = await previewImagery(r.id)
    setPreviewUrl(url)
  }

  return (
    <Card
      title="影像管理"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => { form.resetFields(); setOpen(true) }}>影像入库</Button>}
    >
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 60 },
          { title: '影像名称', dataIndex: 'name' },
          {
            title: '类型', dataIndex: 'type',
            render: (t) => <Tag color={t === 'ORTHO' ? 'blue' : 'purple'}>{t === 'ORTHO' ? '正射' : '倾斜'}</Tag>,
          },
          { title: '拍摄日期', dataIndex: 'captureDate' },
          { title: '所属任务', dataIndex: 'taskName', render: (v) => v || '-' },
          { title: '空间范围', dataIndex: 'bbox', render: (b) => b?.map((v) => v.toFixed(4)).join(', ') },
          {
            title: '操作',
            render: (_, r) => (
              <>
                <Button type="link" onClick={() => preview(r)}>预览</Button>
                <Popconfirm title="确认删除？" onConfirm={() => deleteImagery(r.id).then(load)}>
                  <Button type="link" danger>删除</Button>
                </Popconfirm>
              </>
            ),
          },
        ]}
      />
      <Modal
        title="影像入库"
        open={open}
        onOk={submit}
        onCancel={() => setOpen(false)}
        confirmLoading={uploading}
        destroyOnClose
      >
        <Form form={form} layout="vertical" initialValues={{ type: 'ORTHO' }}>
          <Form.Item name="name" label="影像名称" rules={[{ required: true }]}>
            <Input placeholder="如：城东片区-2026Q3" />
          </Form.Item>
          <Form.Item name="file" label="影像文件" valuePropName="fileList" getValueFromEvent={(e) => e?.fileList} rules={[{ required: true, message: '请选择文件' }]}>
            <Upload beforeUpload={() => false} maxCount={1}>
              <Button icon={<UploadOutlined />}>选择文件（JPG/PNG/TIF）</Button>
            </Upload>
          </Form.Item>
          <Form.Item name="taskId" label="所属航拍任务">
            <Select options={tasks.map((t) => ({ value: t.id, label: t.name }))} allowClear />
          </Form.Item>
          <Form.Item name="type" label="影像类型">
            <Select options={[{ value: 'ORTHO', label: '正射影像' }, { value: 'OBLIQUE', label: '倾斜影像' }]} />
          </Form.Item>
          <Form.Item name="captureDate" label="拍摄日期">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="空间范围（EPSG:4326）" required style={{ marginBottom: 0 }}>
            <Form.Item name="minx" rules={[{ required: true, message: '必填' }]} style={{ display: 'inline-block', width: 'calc(50% - 8px)' }}>
              <InputNumber placeholder="minx 经度" step={0.0001} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="miny" rules={[{ required: true, message: '必填' }]} style={{ display: 'inline-block', width: 'calc(50% - 8px)', margin: '0 0 0 16px' }}>
              <InputNumber placeholder="miny 纬度" step={0.0001} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="maxx" rules={[{ required: true, message: '必填' }]} style={{ display: 'inline-block', width: 'calc(50% - 8px)' }}>
              <InputNumber placeholder="maxx 经度" step={0.0001} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="maxy" rules={[{ required: true, message: '必填' }]} style={{ display: 'inline-block', width: 'calc(50% - 8px)', margin: '0 0 0 16px' }}>
              <InputNumber placeholder="maxy 纬度" step={0.0001} style={{ width: '100%' }} />
            </Form.Item>
          </Form.Item>
        </Form>
      </Modal>
      <Modal open={!!previewUrl} footer={null} onCancel={() => setPreviewUrl(null)} width={800} title="影像预览">
        {previewUrl && <Image src={previewUrl} width="100%" />}
      </Modal>
    </Card>
  )
}
