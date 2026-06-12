<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #409eff; color: white; display: flex; align-items: center; justify-content: space-between">
      <h2 style="margin: 0">文件管理中心</h2>
      <div>
        <span style="margin-right: 16px">{{ authStore.username }}</span>
        <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background: white">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/files">
            <el-icon><FolderOpened /></el-icon>
            <span>文件列表</span>
          </el-menu-item>
          <el-menu-item index="/upload">
            <el-icon><Upload /></el-icon>
            <span>上传文件</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main style="padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
