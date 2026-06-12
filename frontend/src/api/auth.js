import api from './request'

export function register(username, password) {
  return api.post('/api/auth/register', { username, password })
}

export function login(username, password) {
  return api.post('/api/auth/login', { username, password })
}
