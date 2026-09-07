<template>
  <div class="page-wrap">
    <Header />

    <main class="main-content">
      <!-- 页面标题 -->
      <div class="page-title">
        <h1>💰 我的钱包</h1>
        <p>金币充值 &amp; 积分兑换</p>
      </div>

      <!-- 顶部资产展示 -->
      <div class="asset-top glass-card">
        <div class="asset-item">
          <div class="asset-text">当前金币</div>
          <div class="asset-num gold-num">{{ safeNum(gold) }}</div>
          <div class="asset-unit">🪙 金币</div>
        </div>
        <div class="asset-divider"></div>
        <div class="asset-item">
          <div class="asset-text">当前积分</div>
          <div class="asset-num score-num">{{ safeNum(score) }}</div>
          <div class="asset-unit">⭐ 积分</div>
        </div>
      </div>

      <!-- 顶部切换按钮栏 -->
      <div class="tab-bar glass-card">
        <div
          class="tab-btn"
          :class="{ active: tab === 'recharge' }"
          @click="tab = 'recharge'"
        >
          💳 金币充值
        </div>
        <div
          class="tab-btn"
          :class="{ active: tab === 'exchange' }"
          @click="tab = 'exchange'"
        >
          🔄 积分兑换
        </div>
      </div>

      <!-- 金币充值面板 -->
      <div v-show="tab === 'recharge'" class="content-panel glass-card">
        <div class="panel-header">
          <h2 class="panel-title">金币充值</h2>
          <span class="rule-desc">规则：1 元 = 10 金币</span>
        </div>

        <div class="section-label">快速选择</div>
        <div class="quick-grid">
          <div
            v-for="item in rechargeList"
            :key="item.money"
            class="quick-box"
            :class="{ active: selectRecharge?.money === item.money }"
            @click="selectRecharge = item"
          >
            <div class="money">¥{{ item.money }}</div>
            <div class="gain">+{{ item.gold }} 金币</div>
          </div>
        </div>

        <!-- 自定义金额 -->
        <div class="center-section">
          <div class="section-label center-label">自定义金额</div>
          <div class="custom-row center-row">
            <div class="custom-input-wrap center-input">
              <span class="currency-symbol">¥</span>
              <el-input
                v-model="inputMoney"
                type="number"
                placeholder="输入充值金额"
                min="1"
                step="1"
                class="custom-input"
                clearable
              />
              <span class="input-suffix">元</span>
            </div>
          </div>
          <div class="custom-preview center-preview" v-if="inputMoney && safeNum(inputMoney) > 0">
            可获得 <strong>{{ safeNum(inputMoney) * 10 }}</strong> 金币
            <span v-if="safeNum(inputMoney) >= 50" class="bonus-text">
              + 赠 {{ Math.floor(safeNum(inputMoney) / 50) * 10 }} 金币
            </span>
          </div>
        </div>

        <!-- 支付方式 -->
        <div class="center-section">
          <div class="section-label center-label">支付方式</div>
          <div class="pay-row center-pay">
            <div
              v-for="method in paymentMethods"
              :key="method.value"
              class="payment-method"
              :class="{ active: payMode === method.value }"
              @click="payMode = method.value"
            >
              <el-icon :size="20"><component :is="method.icon" /></el-icon>
              <span>{{ method.label }}</span>
            </div>
          </div>
        </div>

        <el-button class="submit-btn" type="primary" :loading="submitting" @click="doRecharge">
          {{ submitting ? '处理中...' : '立即充值' }}
        </el-button>
      </div>

      <!-- 积分兑换面板 -->
      <div v-show="tab === 'exchange'" class="content-panel glass-card">
        <div class="panel-header">
          <h2 class="panel-title">积分兑换</h2>
          <span class="rule-desc">规则：1 金币 = 1 积分</span>
        </div>

        <div class="section-label">快速选择</div>
        <div class="quick-grid">
          <div
            v-for="item in exchangeList"
            :key="item.costGold"
            class="quick-box"
            :class="{ active: selectExchange?.costGold === item.costGold }"
            @click="selectExchange = item"
          >
            <div class="money">{{ item.costGold }} 🪙</div>
            <div class="gain">→ {{ item.getScore }} ⭐</div>
          </div>
        </div>

        <!-- 自定义消耗 -->
        <div class="center-section">
          <div class="section-label center-label">自定义消耗</div>
          <div class="custom-row center-row">
            <div class="custom-input-wrap center-input">
              <el-input
                v-model="inputGold"
                type="number"
                placeholder="输入兑换金币数量"
                min="1"
                step="1"
                class="custom-input"
                clearable
              />
              <span class="input-suffix">金币</span>
            </div>
          </div>
          <div class="custom-preview center-preview" v-if="inputGold && safeNum(inputGold) > 0">
            可获得 <strong>{{ safeNum(inputGold) }}</strong> 积分
          </div>
        </div>

        <div class="balance-tip" v-if="safeNum(gold) > 0">
          💡 当前可用金币：<strong>{{ safeNum(gold) }}</strong> 枚
        </div>
        <div class="balance-tip warn" v-else>
          ⚠️ 当前没有金币，请先去充值
        </div>

        <el-button class="submit-btn" type="primary" :loading="exchanging" @click="doExchange">
          {{ exchanging ? '处理中...' : '确认兑换' }}
        </el-button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, CreditCard } from '@element-plus/icons-vue'
import Header from './../components/Header.vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ✅ 安全数字转换
const safeNum = (val) => {
  if (val === undefined || val === null) return 0
  const num = Number(val)
  return isNaN(num) ? 0 : num
}

// 状态
const tab = ref('recharge')
const gold = ref(0)
const score = ref(0)
const submitting = ref(false)
const exchanging = ref(false)

// 支付方式
const paymentMethods = [
  { value: 'wechat', label: '微信支付', icon: Wallet },
  { value: 'alipay', label: '支付宝', icon: CreditCard },
]

// 充值档位
const rechargeList = reactive([
  { money: 10, gold: 100 },
  { money: 30, gold: 300 },
  { money: 50, gold: 500 },
  { money: 100, gold: 1000 },
  { money: 200, gold: 2000 },
  { money: 500, gold: 5000 },
])
const selectRecharge = ref(null)
const inputMoney = ref('')
const payMode = ref('wechat')

// 兑换档位
const exchangeList = reactive([
  { costGold: 20, getScore: 20 },
  { costGold: 50, getScore: 50 },
  { costGold: 100, getScore: 100 },
  { costGold: 200, getScore: 200 },
  { costGold: 500, getScore: 500 },
])
const selectExchange = ref(null)
const inputGold = ref('')

// ✅【已修正】加载用户资产数据，适配 request 解包后的结构
const loadUserData = async () => {
  try {
    // 积分
    const res = await request.get('/user/profile')
    const data = res.data
    if (data) {
      score.value = safeNum(data.score)
      userStore.setScore(safeNum(data.score))
    }

    // 金币
    const goldRes = await request.get('/gold/info')
    console.log('金币响应:', goldRes)

    let rawGold
    // 核心修正：goldRes 直接是后端响应体 {code, data:{gold:xxx}, msg}
    // 优先级1：标准结构 goldRes.data.gold
    if (goldRes?.data?.gold !== undefined) {
      rawGold = goldRes.data.gold
    }
    // 优先级2：兼容 goldRes.data 直接是数字
    else if (goldRes?.data !== undefined) {
      rawGold = goldRes.data
    }
    // 优先级3：兜底
    else {
      rawGold = goldRes
    }

    let goldNum = Number(rawGold)
    if (isNaN(goldNum) || !isFinite(goldNum)) goldNum = 0
    goldNum = Math.max(Math.floor(goldNum), 0)

    gold.value = goldNum
    userStore.setGold(goldNum)
  } catch (err) {
    gold.value = 0
    score.value = 0
    console.error('加载资产失败：', err)
  }
}

// 充值逻辑
const doRecharge = async () => {
  let money = 0
  if (selectRecharge.value) {
    money = safeNum(selectRecharge.value.money)
  } else {
    money = safeNum(inputMoney.value)
  }

  if (!money || money <= 0) {
    return ElMessage.warning('请选择档位或填写合法充值金额')
  }

  let addGold = money * 10
  let bonusGold = 0
  if (money >= 50) {
    bonusGold = Math.floor(money / 50) * 10
    addGold += bonusGold
  }

  try {
    await ElMessageBox({
      title: '充值确认',
      message: `
        <div class="recharge-confirm-content">
          <div class="confirm-amount-wrap">
            <span class="confirm-label">充值金额</span>
            <span class="confirm-amount">¥${money}</span>
          </div>
          <div class="confirm-points-wrap">
            <span class="confirm-label">可获得金币</span>
            <span class="confirm-points">${addGold} 金币</span>
          </div>
          ${bonusGold > 0 ? `<div class="confirm-rule">🎁 赠送 ${bonusGold} 金币</div>` : ''}
          <div class="confirm-payment">
            <span class="confirm-label">支付方式</span>
            <span class="confirm-payment-name">${paymentMethods.find(m => m.value === payMode.value)?.label || payMode.value}</span>
          </div>
        </div>
      `,
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确认支付',
      cancelButtonText: '再想想',
      distinguishCancelAndClose: true,
      customClass: 'recharge-confirm-dialog',
      showCancelButton: true,
    })

    submitting.value = true
    const resp = await request.post('/gold/recharge', {
      amount: money,
      paymentMethod: payMode.value,
    })
    // 1. 后端若直接返回 newGold/gainGold，立即更新（无需二次请求）
    const respData = resp?.data?.data || resp?.data || {}
    if (respData?.newGold !== undefined && respData?.newGold !== null) {
      const newGoldNum = safeNum(respData.newGold)
      gold.value = newGoldNum
      userStore.setGold(newGoldNum)
    }
    // 2. 重新拉取一次积分信息（后端 UserPO 里的 score 和 /sign/info 都要刷新）
    try {
      const profileRes = await request.get('/user/profile')
      const pd = profileRes?.data
      if (pd) {
        const newScore = safeNum(pd.score)
        score.value = newScore
        userStore.setScore(newScore)
      }
    } catch (e) {
      console.warn('刷新积分失败', e)
    }
    // 3. 兜底：再拉一次金币接口（确保和数据库一致）
    await loadUserData()

    ElMessage.success(`充值成功！获得 ${addGold} 金币${bonusGold > 0 ? `（含赠送 ${bonusGold} 金币）` : ''}`)
    selectRecharge.value = null
    inputMoney.value = ''
  } catch (err) {
    if (err !== 'cancel') {
      console.error('充值失败:', err)
      ElMessage.error(err.response?.data?.msg || '充值失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

// 兑换逻辑
const doExchange = async () => {
  let costGold = 0
  if (selectExchange.value) {
    costGold = safeNum(selectExchange.value.costGold)
  } else {
    costGold = safeNum(inputGold.value)
  }

  if (!costGold || costGold <= 0) {
    return ElMessage.warning('请选择档位或填写合法兑换金币')
  }

  if (safeNum(gold.value) < costGold) {
    return ElMessage.error(`金币余额不足！当前 ${safeNum(gold.value)} 金币，需要 ${costGold} 金币`)
  }

  try {
    await ElMessageBox({
      title: '兑换确认',
      message: `
        <div class="recharge-confirm-content">
          <div class="confirm-amount-wrap" style="background:linear-gradient(135deg,#f5f3ff,#ede9fe);border-color:rgba(124,58,237,0.12);">
            <span class="confirm-label">消耗金币</span>
            <span class="confirm-amount" style="color:#7c3aed;">${costGold} 🪙</span>
          </div>
          <div class="confirm-points-wrap" style="background:linear-gradient(135deg,#f0f7ff,#e8f0fe);border-color:rgba(37,99,235,0.12);">
            <span class="confirm-label">获得积分</span>
            <span class="confirm-points" style="color:#2563eb;">${costGold} ⭐</span>
          </div>
        </div>
      `,
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确认兑换',
      cancelButtonText: '再想想',
      distinguishCancelAndClose: true,
      customClass: 'recharge-confirm-dialog',
      showCancelButton: true,
    })

    exchanging.value = true
    await request.post('/gold/exchange', { gold: costGold })
    await loadUserData()

    ElMessage.success(`兑换成功！消耗 ${costGold} 金币，获得 ${costGold} 积分`)
    selectExchange.value = null
    inputGold.value = ''
  } catch (err) {
    if (err !== 'cancel') {
      console.error('兑换失败:', err)
      ElMessage.error(err.response?.data?.msg || '兑换失败，请稍后重试')
    }
  } finally {
    exchanging.value = false
  }
}

onMounted(() => {
  loadUserData()
})
</script>

<style scoped>
.page-wrap {
  max-width: 1720px;
  margin: 0 auto;
  padding: 24px;
}

.main-content {
  margin-top: 32px;
}

.page-title {
  text-align: center;
  margin-bottom: 32px;
}

.page-title h1 {
  font-size: 48px;
  margin: 0 0 8px;
  background: linear-gradient(90deg, #f59e0b, #ef4444);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-title p {
  font-size: 20px;
  color: #666;
}

.glass-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 6px 30px rgba(160, 180, 220, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.85);
  border-radius: 20px;
}

.asset-top {
  display: flex;
  padding: 28px 20px;
  margin-bottom: 20px;
  gap: 0;
}

.asset-item {
  flex: 1;
  text-align: center;
}

.asset-divider {
  width: 1px;
  background: rgba(200, 210, 230, 0.4);
}

.asset-text {
  font-size: 15px;
  color: #888;
}

.asset-num {
  font-size: 44px;
  font-weight: 700;
  line-height: 1.2;
}

.gold-num {
  color: #f59e0b;
  text-shadow: 0 0 12px rgba(245, 158, 11, 0.25);
}

.score-num {
  color: #2563eb;
  text-shadow: 0 0 12px rgba(37, 99, 235, 0.25);
}

.asset-unit {
  font-size: 14px;
  color: #999;
  margin-top: 2px;
}

.tab-bar {
  display: flex;
  padding: 6px;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.5);
}

.tab-btn {
  flex: 1;
  text-align: center;
  padding: 14px 0;
  font-size: 17px;
  font-weight: 500;
  color: #888;
  cursor: pointer;
  border-radius: 14px;
  transition: all 0.3s ease;
}

.tab-btn.active {
  background: linear-gradient(90deg, rgba(219, 234, 254, 0.8), rgba(233, 213, 255, 0.8));
  color: #2563eb;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.1);
}

.tab-btn:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
  color: #555;
}

.content-panel {
  padding: 30px 32px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.panel-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.rule-desc {
  font-size: 14px;
  color: #f59e0b;
  font-weight: 500;
}

.section-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
}

.center-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.center-label { text-align: center; }
.center-row { justify-content: center; width: 100%; }
.center-input { max-width: 360px; width: 100%; }
.center-pay { justify-content: center; }
.center-preview { text-align: center; }

.quick-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.quick-box {
  border: 2px solid #e8edf4;
  border-radius: 14px;
  padding: 16px 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  background: white;
}

.quick-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(160, 175, 200, 0.12);
  border-color: #93bbfc;
}

.quick-box.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.15);
}

.quick-box .money {
  font-size: 22px;
  font-weight: 700;
  color: #111;
}

.quick-box .gain {
  font-size: 13px;
  color: #666;
  margin-top: 2px;
}

.custom-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.custom-input-wrap {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 14px;
  border: 2px solid #e8edf4;
  transition: border-color 0.2s;
}

.custom-input-wrap:focus-within {
  border-color: #2563eb;
}

.currency-symbol {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  padding: 0 14px 0 18px;
}

.custom-input { flex: 1; }

.custom-input :deep(.el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
}

.custom-input :deep(.el-input__inner) {
  font-size: 18px;
  padding: 12px 0;
  height: auto;
}

.input-suffix {
  padding: 0 18px 0 4px;
  color: #999;
  font-size: 15px;
}

.custom-preview {
  font-size: 15px;
  color: #555;
}

.custom-preview strong {
  color: #2563eb;
  font-size: 18px;
}

.bonus-text {
  color: #ef4444;
  font-weight: 600;
  margin-left: 8px;
}

.pay-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.payment-method {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 24px;
  border-radius: 14px;
  border: 2px solid #e8edf4;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.payment-method:hover { border-color: #93bbfc; }
.payment-method.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.payment-method span {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.balance-tip {
  padding: 14px 20px;
  background: #f0f7ff;
  border-radius: 12px;
  margin-bottom: 20px;
  font-size: 15px;
  color: #555;
  text-align: center;
}

.balance-tip strong {
  color: #f59e0b;
  font-size: 18px;
}

.balance-tip.warn {
  background: #fef3f2;
  color: #dc2626;
}

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 999px;
  background: linear-gradient(90deg, #2563eb, #7c3aed);
  border: none;
}

.submit-btn:hover:not(:disabled) {
  background: linear-gradient(90deg, #1d4ed8, #6d28d9);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.3);
}

@media (max-width: 1200px) {
  .quick-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .page-wrap { padding: 12px; }
  .page-title h1 { font-size: 32px; }
  .page-title p { font-size: 16px; }
  .asset-num { font-size: 32px; }
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
  .content-panel { padding: 20px 16px; }
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .panel-title { font-size: 18px; }
  .center-input { max-width: 100%; }
  .pay-row { gap: 10px; }
  .payment-method { padding: 8px 16px; }
  .payment-method span { font-size: 13px; }
}

@media (max-width: 480px) {
  .asset-top {
    flex-direction: column;
    padding: 20px 16px;
  }
  .asset-divider {
    width: 100%;
    height: 1px;
    background: rgba(200, 210, 230, 0.4);
  }
  .asset-num { font-size: 28px; }
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .quick-box { padding: 12px 6px; }
  .quick-box .money { font-size: 18px; }
  .tab-btn {
    font-size: 14px;
    padding: 12px 0;
  }
  .submit-btn {
    height: 46px;
    font-size: 16px;
  }
}
</style>

<style>
.custom-input :deep(.el-input__inner)::-webkit-outer-spin-button,
.custom-input :deep(.el-input__inner)::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.custom-input :deep(.el-input__inner) {
  -moz-appearance: textfield;
}

.recharge-confirm-dialog {
  border-radius: 24px !important;
  background: rgba(255, 255, 255, 0.92) !important;
  backdrop-filter: blur(20px) !important;
  box-shadow: 0 20px 60px rgba(160, 175, 200, 0.25) !important;
  border: 1px solid rgba(255, 255, 255, 0.6) !important;
  padding: 8px !important;
  max-width: 420px !important;
}
.recharge-confirm-dialog .el-message-box__header {
  padding: 18px 24px 8px 24px !important;
}
.recharge-confirm-dialog .el-message-box__title {
  font-size: 22px !important;
  font-weight: 700 !important;
  background: linear-gradient(90deg, #f59e0b, #ef4444);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.recharge-confirm-dialog .el-message-box__content {
  padding: 8px 24px 20px 24px !important;
}
</style>