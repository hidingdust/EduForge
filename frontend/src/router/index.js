import { createRouter, createWebHashHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Register from '../views/Register.vue'
import Login from '../views/Login.vue'
import Forget from '../views/Forget.vue'
import Profile from '../views/Profile.vue'
import Upload from '../views/Upload.vue'
import Download from '../views/Download.vue'
import EditProfile from '../views/EditProfile.vue'
import Help from '../views/Help.vue'
import ClassroomRender from '../views/classroomRender.vue'
import WorkDetail from '../views/WorkDetail.vue'
import Gold from '../views/Gold.vue'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: Home
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/forget',
    name: 'Forget',
    component: Forget
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile
  },
  {
    path: '/upload',
    name: 'Upload',
    component: Upload
  },
  {
    path: '/download',
    name: 'Download',
    component: Download
  },
  {
    path: '/editProfile',
    name: 'EditProfile',
    component: EditProfile
  },
  {
    path: '/help',
    name: 'Help',
    component: Help
  },
  {
    path: '/classroom-render',
    name: 'classroom-render',
    component: ClassroomRender
  },
  {
    path: '/workDetail',
    name: 'WorkDetail',
    component: WorkDetail
  },
  {
    path: '/gold',
    name: 'Gold',
    component: Gold
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router