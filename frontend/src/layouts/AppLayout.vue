<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataBoard, User, UserFilled, Lock, SwitchButton, Money, Goods, FolderOpened, DocumentChecked, OfficeBuilding, Key } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="236px" class="app-sidebar">
      <div class="app-brand"><span>L</span><div><strong>LIMS</strong><small>实验室信息管理</small></div></div>
      <el-menu router :default-active="activeMenu" class="app-menu">
        <el-menu-item index="/workbench"><el-icon><DataBoard /></el-icon><span>工作台</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('finance:charge:view')" index="/finance"><el-icon><Money /></el-icon><span>财务管理</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('inventory:stock:view')" index="/inventory"><el-icon><Goods /></el-icon><span>物料管理</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('document:file:view')" index="/documents"><el-icon><FolderOpened /></el-icon><span>文件管理</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('report:report:view')" index="/reports"><el-icon><DocumentChecked /></el-icon><span>报告管理</span></el-menu-item>
        <el-sub-menu index="system" v-if="auth.hasPermission('system:user:view') || auth.hasPermission('system:role:view') || auth.hasPermission('system:department:view') || auth.hasPermission('system:permission:view')">
          <template #title><el-icon><Lock /></el-icon><span>系统管理</span></template>
          <el-menu-item v-if="auth.hasPermission('system:user:view')" index="/system/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
          <el-menu-item v-if="auth.hasPermission('system:role:view')" index="/system/roles"><el-icon><UserFilled /></el-icon>角色权限</el-menu-item>
          <el-menu-item v-if="auth.hasPermission('system:department:view')" index="/system/departments"><el-icon><OfficeBuilding /></el-icon>部门管理</el-menu-item>
          <el-menu-item v-if="auth.hasPermission('system:permission:view')" index="/system/permissions"><el-icon><Key /></el-icon>权限配置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container class="app-main-shell">
      <div class="top-right-user">
        <el-dropdown>
          <span class="user-menu"><el-icon><User /></el-icon>{{ auth.user?.username }}</span>
          <template #dropdown><el-dropdown-menu><el-dropdown-item :icon="SwitchButton" @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </div>
      <el-main class="app-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>
