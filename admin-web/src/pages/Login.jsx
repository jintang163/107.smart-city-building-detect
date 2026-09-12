import React, { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { login } from '../api'

export default function Login() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const data = await login(values)
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      message.success('登录成功')
      navigate('/dashboard')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f0f2f5' }}>
      <Card title="智慧城管 · 违章建筑检测系统" style={{ width: 380 }}>
        <Form layout="vertical" onFinish={onFinish} initialValues={{ username: 'admin', password: 'admin123' }}>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input placeholder="admin" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password placeholder="admin123" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>登 录</Button>
        </Form>
      </Card>
    </div>
  )
}
