<template>
  <view v-if="order" class="page">
    <view class="section">
      <view class="head">
        <text class="title">{{ order.title }}</text>
        <text class="tag">{{ statusLabel(order.status) }}</text>
      </view>
      <view class="line">工单号：{{ order.code }}</view>
      <view class="line">图斑面积：{{ order.spotAreaM2 ? order.spotAreaM2.toFixed(1) : '-' }} ㎡</view>
      <view class="line">AI 置信度：{{ (order.spotConfidence * 100).toFixed(0) }}%</view>
      <view class="line">图斑位置：{{ centerText }}</view>
      <view class="line">说明：{{ order.description }}</view>
    </view>

    <view class="section">
      <view class="section-title">现场照片（{{ photos.length }}）</view>
      <view class="photos">
        <image v-for="(p, i) in photos" :key="i" :src="p.preview" class="photo" mode="aspectFill" @click="previewPhoto(i)" />
        <view class="photo add" @click="takePhoto">＋</view>
      </view>
    </view>

    <view class="section">
      <view class="section-title">核查意见</view>
      <textarea v-model="comment" class="textarea" placeholder="现场核查情况说明…" />
    </view>

    <view class="actions">
      <button v-for="a in availableActions" :key="a.action" class="btn" :class="a.cls" @click="submit(a.action)">
        {{ a.label }}
      </button>
    </view>

    <view class="section">
      <view class="section-title">流转记录</view>
      <view v-for="l in order.logs" :key="l.id" class="log">
        <text class="log-action">{{ actionLabel(l.action) }}</text>
        <text class="log-meta">{{ l.operatorName }} · {{ l.createdAt }}</text>
        <view v-if="l.comment" class="log-comment">{{ l.comment }}</view>
      </view>
      <view v-if="!order.logs || order.logs.length === 0" class="muted">暂无记录</view>
    </view>
  </view>
</template>

<script>
import { request, uploadPhoto, BASE_URL } from '../../utils/request'

const STATUS_LABEL = {
  PENDING: '待核查', INSPECTING: '核查中', CONFIRMED: '已认定',
  EXCLUDED: '已排除', RECTIFYING: '整改中', ARCHIVED: '已归档'
}
const ACTION_LABEL = { ASSIGN: '派单', CONFIRM: '认定违建', EXCLUDE: '排除', RECTIFY: '发起整改', ARCHIVE: '归档' }

export default {
  data() {
    return { id: null, order: null, comment: '', photos: [] }
  },
  computed: {
    centerText() {
      const coords = this.order?.spotGeom?.coordinates?.[0]
      if (!coords || !coords.length) return '-'
      const lng = coords.reduce((s, c) => s + c[0], 0) / coords.length
      const lat = coords.reduce((s, c) => s + c[1], 0) / coords.length
      return `${lng.toFixed(5)}, ${lat.toFixed(5)}`
    },
    availableActions() {
      const s = this.order?.status
      if (s === 'INSPECTING') return [
        { action: 'CONFIRM', label: '认定为违建', cls: 'danger' },
        { action: 'EXCLUDE', label: '排除（误报）', cls: '' }
      ]
      if (s === 'CONFIRMED') return [
        { action: 'RECTIFY', label: '发起整改', cls: 'primary' },
        { action: 'ARCHIVE', label: '直接归档', cls: '' }
      ]
      if (s === 'RECTIFYING') return [{ action: 'ARCHIVE', label: '整改完成归档', cls: 'primary' }]
      return []
    }
  },
  onLoad(query) {
    this.id = query.id
    this.load()
  },
  methods: {
    statusLabel(s) { return STATUS_LABEL[s] || s },
    actionLabel(a) { return ACTION_LABEL[a] || a },
    async load() {
      this.order = await request({ url: '/api/orders/' + this.id })
    },
    takePhoto() {
      uni.chooseImage({
        count: 3,
        sourceType: ['camera', 'album'],
        success: async (res) => {
          uni.showLoading({ title: '上传中' })
          try {
            for (const path of res.tempFilePaths) {
              const objectName = await uploadPhoto(path)
              this.photos.push({ objectName, preview: path })
            }
          } finally {
            uni.hideLoading()
          }
        }
      })
    },
    previewPhoto(i) {
      uni.previewImage({ urls: this.photos.map((p) => p.preview), current: i })
    },
    async submit(action) {
      if (this.photos.length === 0 && ['CONFIRM', 'EXCLUDE'].includes(action)) {
        uni.showToast({ title: '请至少上传一张现场照片', icon: 'none' })
        return
      }
      await request({
        url: `/api/orders/${this.id}/action`,
        method: 'POST',
        data: {
          action,
          comment: this.comment,
          photos: JSON.stringify(this.photos.map((p) => p.objectName))
        }
      })
      uni.showToast({ title: '提交成功', icon: 'success' })
      this.comment = ''
      this.photos = []
      this.load()
    }
  }
}
</script>

<style>
.page { padding-bottom: 40rpx; }
.section { background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 28rpx; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.title { font-weight: 600; font-size: 32rpx; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; background: #e6f4ff; color: #1677ff; }
.line { margin-top: 10rpx; color: #555; }
.section-title { font-weight: 600; margin-bottom: 16rpx; }
.photos { display: flex; flex-wrap: wrap; gap: 16rpx; }
.photo { width: 150rpx; height: 150rpx; border-radius: 12rpx; background: #f0f0f0; }
.add { display: flex; align-items: center; justify-content: center; font-size: 60rpx; color: #bbb; border: 2rpx dashed #ddd; }
.textarea { width: 100%; height: 160rpx; background: #f8f8f8; border-radius: 12rpx; padding: 20rpx; box-sizing: border-box; }
.actions { display: flex; gap: 20rpx; padding: 0 20rpx; }
.btn { flex: 1; background: #f0f0f0; color: #333; border-radius: 12rpx; font-size: 28rpx; }
.btn.primary { background: #1677ff; color: #fff; }
.btn.danger { background: #ff4d4f; color: #fff; }
.log { padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.log-action { font-weight: 600; margin-right: 16rpx; }
.log-meta { color: #999; font-size: 24rpx; }
.log-comment { margin-top: 8rpx; color: #555; }
.muted { color: #999; }
</style>
