import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/MainLayout.vue'),
    redirect: '/files',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'files',
        name: 'FileList',
        component: () => import('../views/FileListView.vue')
      },
      {
        path: 'upload',
        name: 'Upload',
        component: () => import('../views/UploadView.vue')
      },
      {
        path: 'file/:id',
        name: 'FileDetail',
        component: () => import('../views/FileDetailView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
