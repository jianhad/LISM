import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, login as loginApi, type CurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(null)
  const loading = ref(false)
  const token = ref(localStorage.getItem('lims.accessToken'))
  const authenticated = computed(() => Boolean(token.value))

  async function login(username: string, password: string) {
    const result = await loginApi(username, password)
    localStorage.setItem('lims.accessToken', result.accessToken)
    token.value = result.accessToken
    await loadCurrentUser()
  }

  async function loadCurrentUser() {
    if (!token.value) return
    loading.value = true
    try {
      user.value = await getCurrentUser()
    } finally {
      loading.value = false
    }
  }

  function hasPermission(permission: string) {
    return user.value?.permissions.includes(permission) ?? false
  }

  function logout() {
    localStorage.removeItem('lims.accessToken')
    token.value = null
    user.value = null
  }

  return { user, loading, authenticated, login, loadCurrentUser, hasPermission, logout }
})
