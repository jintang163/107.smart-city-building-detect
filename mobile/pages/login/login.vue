<template>
  <view class="login-page">
    <view class="logo">智慧城管</view>
    <view class="subtitle">违章建筑检测 · 现场核查</view>
    <view class="form">
      <input class="input" v-model="username" placeholder="用户名" />
      <input class="input" v-model="password" password placeholder="密码" />
      <button class="btn" :disabled="loading" @click="doLogin">{{ loading ? '登录中…' : '登 录' }}</button>
    </view>
  </view>
</template>

<script>
import { request } from '../../utils/request'

export default {
  data() {
    return { username: 'zhangsan', password: '123456', loading: false }
  },
  methods: {
    async doLogin() {
      if (!this.username || !this.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const data = await request({ url: '/api/auth/login', method: 'POST', data: { username: this.username, password: this.password } })
        uni.setStorageSync('token', data.token)
        uni.setStorageSync('user', data.user)
        uni.reLaunch({ url: '/pages/order/list' })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style>
.login-page { padding: 120rpx 60rpx; }
.logo { font-size: 56rpx; font-weight: 600; text-align: center; color: #1677ff; }
.subtitle { text-align: center; color: #999; margin: 16rpx 0 60rpx; }
.input { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 24rpx; }
.btn { background: #1677ff; color: #fff; border-radius: 12rpx; }
</style>
