<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Hide, Lock, User, View } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AnimatedCharacters from '@/components/login/kube/AnimatedCharacters.vue'
import { useAuthStore } from '@/stores/auth'

const formRef = ref<FormInstance>()
const submitting = ref(false)
const showPassword = ref(false)
const activeField = ref<'username' | 'password' | null>(null)
const loginError = ref(false)
const form = reactive({ username: 'admin', password: 'adimin' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const passwordType = computed(() => showPassword.value ? 'text' : 'password')

function focus(field: 'username' | 'password') {
  activeField.value = field
  loginError.value = false
}

function blur() { activeField.value = null }

async function submit() {
  try { await formRef.value?.validate() } catch { loginError.value = true; return }
  submitting.value = true
  loginError.value = false
  try {
    await auth.login(form.username, form.password)
    await router.replace(String(route.query.redirect || '/workbench'))
  } catch (error: any) {
    loginError.value = true
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    submitting.value = false
  }
}

</script>

<template>
  <main class="login-page interactive-login">
    <section class="mascot-panel">
      <header class="login-brand"><strong>LIMS</strong><span class="brand-divider" /><b>实验室信息管理系统</b></header>
      <div class="panel-copy"><span>规范 · 受控 · 可追溯</span><h1>让每一份实验数据<br>都有清晰的来路</h1></div>
      <div class="kube-characters" :class="{ 'is-error': loginError, 'is-submitting': submitting }">
        <AnimatedCharacters
          :is-typing="activeField === 'username'"
          :has-secret="Boolean(form.password)"
          :secret-visible="showPassword"
        />
      </div>
      <footer><span>内网安全运行</span><i /> <span>CMA / CNAS 管理能力</span><i /> <span>关键操作全程留痕</span></footer>
    </section>
    <section class="login-panel">
      <el-form ref="formRef" :model="form" :rules="rules" class="login-card" @keyup.enter="submit">
        <div class="form-eyebrow">WELCOME BACK</div>
        <div class="card-title"><p>使用单位分配的账号继续工作</p></div>
        <el-form-item prop="username">
          <label class="field-label">用户名</label>
          <el-input v-model="form.username" size="large" placeholder="请输入系统账号" autocomplete="username" :prefix-icon="User" @focus="focus('username')" @blur="blur" />
        </el-form-item>
        <el-form-item prop="password">
          <label class="field-label">密码</label>
          <el-input v-model="form.password" :type="passwordType" size="large" placeholder="请输入登录密码" autocomplete="current-password" :prefix-icon="Lock" @focus="focus('password')" @blur="blur">
            <template #suffix><button class="password-toggle" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @mousedown.prevent @click="showPassword = !showPassword"><el-icon><Hide v-if="showPassword" /><View v-else /></el-icon></button></template>
          </el-input>
        </el-form-item>
        <div class="form-options"><el-checkbox>记住用户名</el-checkbox><span>首次登录请修改初始密码</span></div>
        <el-button type="primary" size="large" :loading="submitting" class="login-button" @click="submit">{{ submitting ? '正在核验…' : '进入系统' }}</el-button>
        <div class="security-note"><span class="security-dot" />系统将记录登录及关键业务操作，用于安全审计</div>
      </el-form>
      <p class="copyright">呼和浩特实验室信息管理系统 · V0.1</p>
    </section>
  </main>
</template>

<style scoped>
.interactive-login{grid-template-columns:minmax(600px,1.12fr) minmax(480px,.88fr);background:#f4f7f5}.mascot-panel{position:relative;min-height:100vh;overflow:hidden;display:flex;flex-direction:column;align-items:center;padding:38px 58px 28px;color:#fff;background:radial-gradient(circle at 72% 18%,rgba(124,202,169,.28),transparent 28%),linear-gradient(145deg,#0d392f 0%,#155343 55%,#1d6b55 100%)}.mascot-panel::before{content:"";position:absolute;inset:0;opacity:.09;background-image:linear-gradient(rgba(255,255,255,.55) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.55) 1px,transparent 1px);background-size:30px 30px;mask-image:linear-gradient(to bottom,black,transparent 75%)}.login-brand{position:relative;z-index:2;align-self:flex-start;display:flex;align-items:center;gap:17px;white-space:nowrap}.login-brand strong{font-size:28px;font-weight:900;letter-spacing:4px}.brand-divider{width:1px;height:36px;background:rgba(255,255,255,.42)}.login-brand b{font-size:33px;font-weight:750;letter-spacing:2px;text-shadow:0 4px 14px rgba(20,40,100,.22)}.panel-copy{position:relative;z-index:2;align-self:flex-start;margin:48px 0 -20px}.panel-copy span{color:#e5c57f;font-size:11px;letter-spacing:4.5px}.panel-copy h1{margin:12px 0 0;font-size:27px;font-weight:600;line-height:1.46;letter-spacing:.7px}.mascot-panel footer{position:relative;z-index:2;display:flex;align-items:center;gap:13px;margin-top:auto;color:rgba(255,255,255,.55);font-size:11px;letter-spacing:1px}.mascot-panel footer i{width:3px;height:3px;border-radius:50%;background:#e6c278}.login-panel{position:relative;display:grid;place-items:center;padding:42px;background:#f5f7f6}.login-card{width:440px;padding:48px 48px 42px;background:#fff;border:1px solid #dfe7e3;box-shadow:0 28px 80px rgba(25,62,52,.12)}.form-eyebrow{margin-bottom:15px;color:#1c6a54;font-size:11px;font-weight:700;letter-spacing:3px}.card-title{margin-bottom:34px}.card-title p{margin:0;color:#7d8c87;font-size:14px}.field-label{display:block;width:100%;margin-bottom:8px;color:#344b44;font-size:13px;font-weight:600}.login-card :deep(.el-form-item){display:block;margin-bottom:23px}.login-card :deep(.el-input__wrapper){height:48px;border-radius:3px;box-shadow:0 0 0 1px #d7e0dc inset}.login-card :deep(.el-input__wrapper.is-focus){box-shadow:0 0 0 1px #1c6a54 inset,0 0 0 3px rgba(28,106,84,.1)}.password-toggle{display:grid;place-items:center;padding:4px;border:0;background:transparent;color:#8b9994;cursor:pointer}.form-options{display:flex;justify-content:space-between;align-items:center;margin:-3px 0 23px}.form-options>span{color:#9aa6a2;font-size:11px}.login-button{width:100%;height:49px;border-radius:3px;background:#1c6a54;border-color:#1c6a54;font-weight:600;letter-spacing:2px}.login-button:hover{background:#165b48;border-color:#165b48}.security-note{display:flex;align-items:center;justify-content:center;gap:8px;margin-top:27px;color:#9aa5a2;font-size:11px}.security-dot{width:7px;height:7px;border-radius:50%;background:#6ab18f;box-shadow:0 0 0 4px rgba(106,177,143,.13)}.copyright{position:absolute;bottom:18px;color:#a2ada9;font-size:11px}@media(max-width:1100px){.interactive-login{grid-template-columns:1fr}.mascot-panel{display:none}.login-panel{min-height:100vh}}@media(max-width:560px){.login-panel{padding:18px}.login-card{width:100%;padding:36px 27px}.copyright{display:none}}
.kube-characters{position:relative;z-index:2;margin:auto 0 28px;filter:drop-shadow(0 24px 28px rgba(2,24,19,.22));transform-origin:center bottom}.kube-characters.is-error{animation:kube-shake .4s ease}.kube-characters.is-submitting{animation:kube-bob .75s ease-in-out infinite alternate}@keyframes kube-shake{25%{transform:translateX(-8px)}75%{transform:translateX(8px)}}@keyframes kube-bob{to{transform:translateY(-8px)}}@media(max-width:1250px){.kube-characters{transform:scale(.86)}}
.interactive-login{background:linear-gradient(135deg,#f5f9ff,#f1f0ff)}.mascot-panel{background:radial-gradient(circle at 72% 18%,rgba(81,214,255,.34),transparent 30%),radial-gradient(circle at 20% 78%,rgba(147,103,255,.3),transparent 33%),linear-gradient(145deg,#078fea 0%,#3368e8 52%,#6344bd 100%)}.login-panel{background:linear-gradient(145deg,#f8fbff,#f2f0ff)}.login-card{border-color:#dce2f6;box-shadow:0 28px 80px rgba(58,65,150,.17)}.form-eyebrow{color:#396afc}.login-card :deep(.el-input__wrapper.is-focus){box-shadow:0 0 0 1px #536dfe inset,0 0 0 3px rgba(83,109,254,.12)}.login-button{background:linear-gradient(110deg,#168bea,#5a4ed1);border:0}.login-button:hover{background:linear-gradient(110deg,#087fdc,#4b3fbd);border:0}.security-dot{background:linear-gradient(135deg,#168bea,#6650d8);box-shadow:0 0 0 4px rgba(83,109,254,.14)}
</style>
