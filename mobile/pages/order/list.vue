<template>
  <view>
    <view class="tabs">
      <view
        v-for="t in tabs" :key="t.value"
        class="tab" :class="{ active: status === t.value }"
        @click="switchTab(t.value)"
      >{{ t.label }}</view>
    </view>
    <view v-if="orders.length === 0" class="empty">暂无工单</view>
    <view v-for="o in orders" :key="o.id" class="card" @click="goDetail(o.id)">
      <view class="row">
        <text class="title">{{ o.title }}</text>
        <text class="tag" :class="'st-' + o.status">{{ statusLabel(o.status) }}</text>
      </view>
      <view class="line">工单号：{{ o.code }}</view>
      <view class="line">图斑面积：{{ o.spotAreaM2 ? o.spotAreaM2.toFixed(1) : '-' }} ㎡　置信度：{{ (o.spotConfidence * 100).toFixed(0) }}%</view>
      <view class="line muted">创建时间：{{ o.createdAt }}</view>
    </view>
  </view>
</template>

<script>
import { request } from '../../utils/request'

const STATUS_LABEL = {
  PENDING: '待核查', INSPECTING: '核查中', CONFIRMED: '已认定',
  EXCLUDED: '已排除', RECTIFYING: '整改中', ARCHIVED: '已归档'
}

export default {
  data() {
    return {
      orders: [],
      status: '',
      tabs: [
        { label: '全部', value: '' },
        { label: '待核查', value: 'PENDING' },
        { label: '核查中', value: 'INSPECTING' },
        { label: '整改中', value: 'RECTIFYING' },
        { label: '已归档', value: 'ARCHIVED' }
      ]
    }
  },
  onShow() { this.load() },
  onPullDownRefresh() { this.load().then(() => uni.stopPullDownRefresh()) },
  methods: {
    statusLabel(s) { return STATUS_LABEL[s] || s },
    async load() {
      const params = ['mine=true']
      if (this.status) params.push('status=' + this.status)
      this.orders = await request({ url: '/api/orders?' + params.join('&') })
    },
    switchTab(v) {
      this.status = v
      this.load()
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/order/detail?id=' + id })
    }
  }
}
</script>

<style>
.tabs { display: flex; background: #fff; padding: 16rpx 0; position: sticky; top: 0; z-index: 1; }
.tab { flex: 1; text-align: center; padding: 12rpx 0; color: #666; }
.tab.active { color: #1677ff; font-weight: 600; border-bottom: 4rpx solid #1677ff; }
.empty { text-align: center; color: #999; padding: 120rpx 0; }
.card { background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 28rpx; }
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.title { font-weight: 600; font-size: 30rpx; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; background: #f0f0f0; color: #666; }
.st-PENDING { background: #fff7e6; color: #d48806; }
.st-INSPECTING { background: #e6f4ff; color: #1677ff; }
.st-CONFIRMED { background: #fff1f0; color: #cf1322; }
.st-RECTIFYING { background: #fff7e6; color: #d46b08; }
.st-ARCHIVED { background: #f6ffed; color: #389e0d; }
.line { margin-top: 8rpx; }
.muted { color: #999; font-size: 24rpx; }
</style>
