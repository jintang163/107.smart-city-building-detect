<script>
export default {
  onLaunch() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/login' })
    }
  }
}
</script>

<style>
page {
  background: #f5f5f5;
  font-size: 28rpx;
  color: #333;
}
</style>
