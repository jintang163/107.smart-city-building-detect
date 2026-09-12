// 后端地址：真机调试时改为电脑局域网 IP，如 http://192.168.1.100:8080
export const BASE_URL = 'http://localhost:8080'

export function request(options) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'X-Token': uni.getStorageSync('token') || ''
      },
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          return reject(new Error('未登录'))
        }
        if (res.data && res.data.code === 0) {
          resolve(res.data.data)
        } else {
          uni.showToast({ title: res.data?.message || '请求失败', icon: 'none' })
          reject(new Error(res.data?.message || '请求失败'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 上传照片到证据库，返回对象名
export function uploadPhoto(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/api/files/upload',
      filePath,
      name: 'file',
      header: { 'X-Token': uni.getStorageSync('token') || '' },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 0) resolve(data.data.objectName)
          else reject(new Error(data.message))
        } catch (e) { reject(e) }
      },
      fail: reject
    })
  })
}
