import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || null)
  const username = ref(localStorage.getItem('username') || '')

  function setAuth(tokenVal, userIdVal, usernameVal) {
    token.value = tokenVal
    userId.value = userIdVal
    username.value = usernameVal
    localStorage.setItem('token', tokenVal)
    localStorage.setItem('userId', userIdVal)
    localStorage.setItem('username', usernameVal)
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
  }

  return { token, userId, username, setAuth, logout }
})
