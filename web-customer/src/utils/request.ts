import axios from 'axios'
import { showToast } from 'vant'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
}, error => Promise.reject(error))

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) return res
    showToast(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  error => {
    showToast(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
