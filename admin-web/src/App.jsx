import React from 'react'
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Dropdown, Avatar, Tag } from 'antd'
import {
  DashboardOutlined, RocketOutlined, ScheduleOutlined, PictureOutlined,
  SwapOutlined, AuditOutlined, FileDoneOutlined, GlobalOutlined, UserOutlined,
  BulbOutlined, FireOutlined
} from '@ant-design/icons'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import RouteList from './pages/RouteList'
import FlightTaskList from './pages/FlightTaskList'
import ImageryList from './pages/ImageryList'
import CompareTaskList from './pages/CompareTaskList'
import SpotReview from './pages/SpotReview'
import WorkOrderList from './pages/WorkOrderList'
import MapQuery from './pages/MapQuery'
import AnalysisCenter from './pages/AnalysisCenter'
import EnforceHeatmap from './pages/EnforceHeatmap'

const { Sider, Header, Content } = Layout

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/routes', icon: <RocketOutlined />, label: '航线管理' },
  { key: '/tasks', icon: <ScheduleOutlined />, label: '航拍任务' },
  { key: '/imagery', icon: <PictureOutlined />, label: '影像管理' },
  { key: '/compare', icon: <SwapOutlined />, label: '对比检测' },
  { key: '/spots', icon: <AuditOutlined />, label: '图斑审核' },
  { key: '/orders', icon: <FileDoneOutlined />, label: '工单处置' },
  { key: '/analysis', icon: <BulbOutlined />, label: '智能分析' },
  { key: '/heatmap', icon: <FireOutlined />, label: '执法热力图' },
  { key: '/map', icon: <GlobalOutlined />, label: '地图查询' },
]

function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = JSON.parse(localStorage.getItem('user') || '{}')

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    navigate('/login')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark">
        <div style={{ color: '#fff', padding: 16, fontWeight: 600, fontSize: 15 }}>
          智慧城管 · 违建检测
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
          <Dropdown menu={{ items: [{ key: 'logout', label: '退出登录', onClick: logout }] }}>
            <span style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} style={{ marginRight: 8 }} />
              {user.realName || user.username}
              <Tag color={user.role === 'ADMIN' ? 'gold' : 'blue'} style={{ marginLeft: 8 }}>
                {user.role === 'ADMIN' ? '管理员' : '队员'}
              </Tag>
            </span>
          </Dropdown>
        </Header>
        <Content style={{ margin: 16 }}>
          <Routes>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/routes" element={<RouteList />} />
            <Route path="/tasks" element={<FlightTaskList />} />
            <Route path="/imagery" element={<ImageryList />} />
            <Route path="/compare" element={<CompareTaskList />} />
            <Route path="/spots" element={<SpotReview />} />
            <Route path="/orders" element={<WorkOrderList />} />
            <Route path="/analysis" element={<AnalysisCenter />} />
            <Route path="/heatmap" element={<EnforceHeatmap />} />
            <Route path="/map" element={<MapQuery />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/*" element={
          localStorage.getItem('token') ? <MainLayout /> : <Navigate to="/login" replace />
        } />
      </Routes>
    </BrowserRouter>
  )
}
