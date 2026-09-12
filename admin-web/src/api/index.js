import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const profile = () => request.get('/auth/profile')

// 用户
export const listUsers = () => request.get('/users')

// 航线
export const listRoutes = () => request.get('/routes')
export const createRoute = (data) => request.post('/routes', data)
export const updateRoute = (id, data) => request.put(`/routes/${id}`, data)
export const deleteRoute = (id) => request.delete(`/routes/${id}`)

// 航拍任务
export const listTasks = () => request.get('/tasks')
export const createTask = (data) => request.post('/tasks', data)
export const updateTask = (id, data) => request.put(`/tasks/${id}`, data)
export const deleteTask = (id) => request.delete(`/tasks/${id}`)

// 影像
export const listImagery = (taskId) => request.get('/imagery', { params: { taskId } })
export const uploadImagery = (formData) =>
  request.post('/imagery/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 300000 })
export const previewImagery = (id) => request.get(`/imagery/${id}/preview`)
export const deleteImagery = (id) => request.delete(`/imagery/${id}`)

// 对比检测
export const listCompare = () => request.get('/compare')
export const createCompare = (data) => request.post('/compare', data)

// 图斑
export const listSpots = (params) => request.get('/spots', { params })
export const reviewSpot = (id, data) => request.post(`/spots/${id}/review`, data)

// 工单
export const listOrders = (params) => request.get('/orders', { params })
export const orderDetail = (id) => request.get(`/orders/${id}`)
export const orderAction = (id, data) => request.post(`/orders/${id}/action`, data)

// 地图
export const mapSpots = (status) => request.get('/map/spots', { params: { status } })
export const mapImagery = () => request.get('/map/imagery')
