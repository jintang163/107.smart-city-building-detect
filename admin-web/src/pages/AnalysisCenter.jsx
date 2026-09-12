import React, { useEffect, useState } from 'react'
import {
  Tabs, Card, Form, InputNumber, Select, Button, Tag, Table, Descriptions,
  Statistic, Row, Col, Modal, message, Space, Popconfirm, Alert,
} from 'antd'
import { ThunderboltOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons'
import {
  recommendRectify, predictOrders, listAlerts, scanAlerts, closeAlert,
  memberProfiles, listCases, createCase, syncCases, listOrders,
} from '../api'

export const VIOLATION_TYPE = {
  ROOFTOP_ADDITION: ['屋顶加盖', 'volcano'],
  ILLEGAL_EXPANSION: ['违法扩建', 'orange'],
  OCCUPY_LAND: ['违法占地', 'red'],
  TEMP_STRUCTURE: ['临时搭建', 'blue'],
}
export const RECTIFY_METHOD = {
  SELF_DEMOLITION: ['自拆', 'green'],
  ASSISTED_DEMOLITION: ['助拆', 'orange'],
  FORCED_DEMOLITION: ['强拆', 'red'],
}

const typeOptions = Object.entries(VIOLATION_TYPE).map(([v, [t]]) => ({ value: v, label: t }))
const methodOptions = Object.entries(RECTIFY_METHOD).map(([v, [t]]) => ({ value: v, label: t }))

/** 由 GeoJSON Polygon 粗略求质心（用于从工单带入坐标） */
function centroidOf(geom) {
  if (!geom || geom.type !== 'Polygon') return null
  const ring = geom.coordinates?.[0] || []
  if (ring.length === 0) return null
  let sx = 0, sy = 0
  ring.forEach(([x, y]) => { sx += x; sy += y })
  return [sx / ring.length, sy / ring.length]
}

/** Tab1 整改方案推荐 */
function RecommendPanel() {
  const [form, setForm] = useState({ violationType: 'ROOFTOP_ADDITION', areaM2: 60 })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [pendingOrders, setPendingOrders] = useState([])

  useEffect(() => { listOrders({ status: 'PENDING' }).then(setPendingOrders).catch(() => {}) }, [])

  const fillFromOrder = (orderId) => {
    const o = pendingOrders.find((x) => x.id === orderId)
    if (!o) return
    const c = centroidOf(o.spotGeom)
    setForm((f) => ({
      ...f,
      areaM2: Math.round((o.spotAreaM2 || 0) * 10) / 10,
      lng: c ? +c[0].toFixed(6) : f.lng,
      lat: c ? +c[1].toFixed(6) : f.lat,
    }))
    message.info(`已带入工单 ${o.code} 的面积与位置`)
  }

  const run = async () => {
    if (!form.areaM2) return message.warning('请填写面积')
    setLoading(true)
    try {
      setResult(await recommendRectify(form))
    } finally {
      setLoading(false)
    }
  }

  const method = result && RECTIFY_METHOD[result.rectifyMethod]

  return (
    <Row gutter={16}>
      <Col span={10}>
        <Card title="违建信息" size="small">
          <Form layout="vertical">
            <Form.Item label="从待核查工单带入（可选）">
              <Select
                allowClear placeholder="选择工单自动填充面积与位置"
                onChange={fillFromOrder}
                options={pendingOrders.map((o) => ({ value: o.id, label: `${o.code}（${o.spotAreaM2?.toFixed(1)}㎡）` }))}
              />
            </Form.Item>
            <Form.Item label="违建类型" required>
              <Select value={form.violationType} onChange={(v) => setForm({ ...form, violationType: v })} options={typeOptions} />
            </Form.Item>
            <Form.Item label="面积（㎡）" required>
              <InputNumber style={{ width: '100%' }} min={1} value={form.areaM2} onChange={(v) => setForm({ ...form, areaM2: v })} />
            </Form.Item>
            <Form.Item label="经度 / 纬度（可选，用于热点与重复违建判定）">
              <Space>
                <InputNumber placeholder="经度" step={0.001} value={form.lng} onChange={(v) => setForm({ ...form, lng: v })} />
                <InputNumber placeholder="纬度" step={0.001} value={form.lat} onChange={(v) => setForm({ ...form, lat: v })} />
              </Space>
            </Form.Item>
            <Button type="primary" icon={<ThunderboltOutlined />} loading={loading} onClick={run} block>
              生成整改方案
            </Button>
          </Form>
        </Card>
      </Col>
      <Col span={14}>
        <Card title="推荐结果" size="small">
          {!result ? <Alert type="info" message="填写左侧信息后点击「生成整改方案」，由 Drools 规则引擎 + 历史案例统计模型给出建议" /> : (
            <>
              <Row gutter={16} style={{ marginBottom: 16 }}>
                <Col span={6}><Statistic title="推荐整改方式" value={method[0]} formatter={(v) => <Tag color={method[1]} style={{ fontSize: 18, padding: '2px 12px' }}>{v}</Tag>} /></Col>
                <Col span={6}><Statistic title="预估工时（小时）" value={result.estimatedHours} /></Col>
                <Col span={6}><Statistic title="预测工期（天）" value={result.predictedDays} /></Col>
                <Col span={6}><Statistic title="风险等级" value={result.riskLevel} /></Col>
              </Row>
              <Descriptions column={2} size="small" bordered>
                <Descriptions.Item label="规则工时">{result.ruleHours} h</Descriptions.Item>
                <Descriptions.Item label="历史同类均值">{result.historyAvgHours == null ? '样本不足' : `${result.historyAvgHours} h`}</Descriptions.Item>
                <Descriptions.Item label="相似案例">{result.similarCases} 条</Descriptions.Item>
                <Descriptions.Item label="工期依据">{result.predictBasis}</Descriptions.Item>
                <Descriptions.Item label="区域特征" span={2}>
                  {result.hotspot && <Tag color="red">执法热点区域</Tag>}
                  {result.repeatArea && <Tag color="volcano">重复违建高发区</Tag>}
                  {!result.hotspot && !result.repeatArea && <Tag>普通区域</Tag>}
                  {result.gridCode && <span style={{ color: '#999' }}>网格 {result.gridCode}</span>}
                </Descriptions.Item>
                <Descriptions.Item label="命中规则说明" span={2}>{result.reason}</Descriptions.Item>
              </Descriptions>
            </>
          )}
        </Card>
      </Col>
    </Row>
  )
}

/** Tab2 工期预测与超期预警 */
function PredictPanel() {
  const [predictions, setPredictions] = useState([])
  const [alerts, setAlerts] = useState([])

  const load = () => {
    predictOrders().then(setPredictions)
    listAlerts().then(setAlerts)
  }
  useEffect(() => { load() }, [])

  const doScan = async () => {
    await scanAlerts()
    message.success('扫描完成')
    load()
  }
  const doClose = async (id) => {
    await closeAlert(id)
    message.success('已关闭')
    load()
  }

  const stateOf = (r) => r.overdue ? <Tag color="red">已超期</Tag> : r.dueSoon ? <Tag color="orange">临期</Tag> : <Tag color="green">正常</Tag>

  return (
    <>
      <Card
        title={`未处理预警（${alerts.length}）`} size="small" style={{ marginBottom: 16 }}
        extra={<Button icon={<SyncOutlined />} onClick={doScan}>立即扫描</Button>}
      >
        <Table
          rowKey="id" size="small" dataSource={alerts} pagination={false} locale={{ emptyText: '暂无预警' }}
          columns={[
            { title: '工单号', dataIndex: 'orderCode', width: 180 },
            {
              title: '类型', dataIndex: 'alertType', width: 100,
              render: (t) => t === 'OVERDUE' ? <Tag color="red">超期</Tag> : <Tag color="orange">临期</Tag>,
            },
            { title: '预警内容', dataIndex: 'message' },
            { title: '预测完成时间', dataIndex: 'predictedFinishAt', width: 180 },
            { title: '预警时间', dataIndex: 'createdAt', width: 180 },
            {
              title: '操作', width: 90,
              render: (_, r) => (
                <Popconfirm title="确认关闭该预警？" onConfirm={() => doClose(r.id)}>
                  <Button type="link" size="small">关闭</Button>
                </Popconfirm>
              ),
            },
          ]}
        />
      </Card>
      <Card title="在办工单工期预测" size="small" extra={<Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>}>
        <Table
          rowKey="orderId" size="small" dataSource={predictions} locale={{ emptyText: '暂无在办工单' }}
          columns={[
            { title: '工单号', dataIndex: 'orderCode', width: 180 },
            { title: '违建类型', dataIndex: 'violationType', width: 110, render: (t) => VIOLATION_TYPE[t]?.[0] || t },
            { title: '面积(㎡)', dataIndex: 'areaM2', width: 90, render: (v) => v?.toFixed(1) },
            { title: '已用(天)', dataIndex: 'elapsedDays', width: 90 },
            { title: '预测工期(天)', dataIndex: 'predictedDays', width: 110 },
            {
              title: '剩余(天)', dataIndex: 'remainingDays', width: 90,
              render: (v) => <span style={{ color: v < 0 ? '#f5222d' : undefined, fontWeight: v < 0 ? 600 : 400 }}>{v}</span>,
            },
            { title: '状态', key: 'state', width: 90, render: (_, r) => stateOf(r) },
            { title: '预测完成时间', dataIndex: 'predictedFinishAt', width: 180 },
            { title: '预测依据', dataIndex: 'basis', ellipsis: true },
          ]}
        />
      </Card>
    </>
  )
}

/** Tab3 队员能力画像 */
function MemberPanel() {
  const [members, setMembers] = useState([])
  useEffect(() => { memberProfiles().then(setMembers) }, [])

  return (
    <Card title="队员能力画像（基于历史案例统计）" size="small">
      <Table
        rowKey="userId" size="small" dataSource={members} pagination={false}
        columns={[
          { title: '队员', dataIndex: 'realName', width: 100 },
          { title: '累计处理', dataIndex: 'handledCount', width: 90, render: (v) => `${v} 起` },
          { title: '平均工期(天)', dataIndex: 'avgDurationDays', width: 110 },
          {
            title: '效率系数', dataIndex: 'efficiency', width: 100,
            render: (v) => <span style={{ color: v >= 1 ? '#52c41a' : '#f5222d' }}>{v}</span>,
          },
          {
            title: '准时率', dataIndex: 'onTimeRate', width: 90,
            render: (v) => `${(v * 100).toFixed(0)}%`,
          },
          {
            title: '当前负载', dataIndex: 'currentLoad', width: 90,
            render: (v) => v >= 5 ? <Tag color="red">{v} 单</Tag> : <Tag>{v} 单</Tag>,
          },
          {
            title: '各类型处理量', dataIndex: 'byType',
            render: (byType) => Object.entries(byType || {}).map(([t, n]) => (
              <Tag key={t} color={n > 0 ? VIOLATION_TYPE[t]?.[1] : 'default'} style={{ marginBottom: 4 }}>
                {VIOLATION_TYPE[t]?.[0] || t} {n}
              </Tag>
            )),
          },
        ]}
      />
    </Card>
  )
}

/** Tab4 历史案例库 */
function CasePanel() {
  const [cases, setCases] = useState([])
  const [type, setType] = useState()
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ violationType: 'ROOFTOP_ADDITION', rectifyMethod: 'SELF_DEMOLITION' })

  const load = () => listCases(type).then(setCases)
  useEffect(() => { load() }, [type])

  const doSync = async () => {
    const r = await syncCases()
    message.success(`同步完成：新增 ${r.created} 条，跳过已存在 ${r.skipped} 条`)
    load()
  }
  const doCreate = async () => {
    if (!form.areaM2) return message.warning('请填写面积')
    await createCase(form)
    message.success('已录入')
    setCreating(false)
    load()
  }

  return (
    <Card
      title={`历史案例库（${cases.length}）`} size="small"
      extra={
        <Space>
          <Select allowClear placeholder="类型过滤" style={{ width: 130 }} value={type} onChange={setType} options={typeOptions} />
          <Button onClick={() => setCreating(true)}>手工录入</Button>
          <Popconfirm title="从全部已归档工单补偿同步训练标签？" onConfirm={doSync}>
            <Button icon={<SyncOutlined />}>从工单同步</Button>
          </Popconfirm>
        </Space>
      }
    >
      <Table
        rowKey="id" size="small" dataSource={cases}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 60 },
          { title: '违建类型', dataIndex: 'violationType', width: 110, render: (t) => VIOLATION_TYPE[t]?.[0] || t },
          { title: '面积(㎡)', dataIndex: 'areaM2', width: 90, render: (v) => v?.toFixed(1) },
          {
            title: '整改方式', dataIndex: 'rectifyMethod', width: 100,
            render: (m) => { const [t, c] = RECTIFY_METHOD[m] || [m, 'default']; return <Tag color={c}>{t}</Tag> },
          },
          { title: '处理天数', dataIndex: 'durationDays', width: 90 },
          { title: '工时(h)', dataIndex: 'workHours', width: 90 },
          { title: '处置人', dataIndex: 'operatorName', width: 90, render: (v) => v || '-' },
          { title: '网格', dataIndex: 'gridCode', width: 120 },
          { title: '来源工单', dataIndex: 'sourceOrderId', width: 90, render: (v) => v || '手工录入' },
          { title: '办结时间', dataIndex: 'finishedAt', width: 170 },
        ]}
      />
      <Modal
        title="手工录入历史案例" open={creating} onCancel={() => setCreating(false)} onOk={doCreate} width={480}
      >
        <Form layout="vertical">
          <Form.Item label="违建类型" required>
            <Select value={form.violationType} onChange={(v) => setForm({ ...form, violationType: v })} options={typeOptions} />
          </Form.Item>
          <Form.Item label="整改方式" required>
            <Select value={form.rectifyMethod} onChange={(v) => setForm({ ...form, rectifyMethod: v })} options={methodOptions} />
          </Form.Item>
          <Form.Item label="面积（㎡）" required>
            <InputNumber style={{ width: '100%' }} min={1} value={form.areaM2} onChange={(v) => setForm({ ...form, areaM2: v })} />
          </Form.Item>
          <Form.Item label="经度 / 纬度">
            <Space>
              <InputNumber placeholder="经度" step={0.001} value={form.lng} onChange={(v) => setForm({ ...form, lng: v })} />
              <InputNumber placeholder="纬度" step={0.001} value={form.lat} onChange={(v) => setForm({ ...form, lat: v })} />
            </Space>
          </Form.Item>
          <Form.Item label="处理天数 / 工时（小时）">
            <Space>
              <InputNumber placeholder="天数" min={0} value={form.durationDays} onChange={(v) => setForm({ ...form, durationDays: v })} />
              <InputNumber placeholder="工时" min={0} value={form.workHours} onChange={(v) => setForm({ ...form, workHours: v })} />
            </Space>
          </Form.Item>
          <Form.Item label="处置人">
            <Select
              allowClear placeholder="选择队员"
              value={form.operatorName} onChange={(v) => setForm({ ...form, operatorName: v })}
              options={['张三', '李四', '王五', '赵六'].map((n) => ({ value: n, label: n }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}

export default function AnalysisCenter() {
  return (
    <Card
      title="智能分析（规则引擎 + 统计模型）"
      extra={<Tag color="blue">数据源独立于工单表，仅读取工单结果作为训练标签</Tag>}
    >
      <Tabs
        items={[
          { key: 'recommend', label: '整改方案推荐', children: <RecommendPanel /> },
          { key: 'predict', label: '工期预测与预警', children: <PredictPanel /> },
          { key: 'member', label: '队员能力匹配', children: <MemberPanel /> },
          { key: 'case', label: '历史案例库', children: <CasePanel /> },
        ]}
      />
    </Card>
  )
}
