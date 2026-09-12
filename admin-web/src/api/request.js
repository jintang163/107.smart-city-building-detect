import axios from 'axios'
import { message } from 'antd'

const request = axios.create({ baseURL: '/api', timeout: 30000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers['X-Token'] = token
  return config
})

request.interceptors.response.use(
  (res) => {
    if (res.data && res.data.code !== 0) {
      message.error(res.data.message || '请求失败')
      return Promise.reject(new Error(res.data.message))
    }
    return res.data.data
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      if (!location.pathname.startsWith('/login')) location.href = '/login'
    }
    message.error(err.response?.data?.message || err.message)
    return Promise.reject(err)
  }
)

export default request
