// src/api/user.js
import request from '../utils/request'

// 手机号密码登录
export function loginApi(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 获取当前登录用户信息
export function getUserProfile() {
  return request({
    url: '/user/profile',
    method: 'GET'
  })
}

// 更新个人资料
export function updateUserInfo(data) {
  return request({
    url: '/user/update',
    method: 'post',
    data
  })
}

// 修改密码
export function updatePassword(data) {
  return request({
    url: '/user/password',
    method: 'post',
    data
  })
}

// 退出登录接口（可选，如果后端需要调用注销）
export function logoutApi() {
  return request({
    url: '/user/logout',
    method: 'POST'
  })
}