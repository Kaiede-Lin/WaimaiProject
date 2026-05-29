# 前端开发注意事项

## 项目概览

| 模块 | 路径 | 技术栈 |
|------|------|--------|
| 用户小程序 | `C:\Users\Administrator\WeChatProjects\miniprogram-1` | uni-app (Vue 3 + Vite) |
| 商家小程序 | `C:\Users\Administrator\WeChatProjects\miniprogram-2\src` | 原生微信小程序 |
| 骑手小程序 | `C:\Users\Administrator\WeChatProjects\miniprogram-3\src` | 原生微信小程序 |
| Web 管理后台 | `web-admin/` | Vue 3 + Vite |

---

## 1. 用户小程序 (miniprogram-1) — uni-app 编译与调试

用户小程序使用 uni-app 框架，**微信开发者工具无法直接打开项目根目录**。

### 启动步骤

**终端编译（保持运行）：**

```powershell
cd "C:\Users\Administrator\WeChatProjects\miniprogram-1"
npm run dev:mp-weixin
```

**微信开发者工具：**

导入项目时选择编译产物目录，**不是项目根目录**：

```
C:\Users\Administrator\WeChatProjects\miniprogram-1\dist\dev\mp-weixin
```

- `src/` — 源码目录（`.vue` 文件，微信开发者工具不认识）
- `dist/dev/mp-weixin/` — 编译产物（`.wxml/.wxss/.js/.json`，微信开发者工具才能识别）
- 每次改完 `src/` 下的代码，uni-app 会自动重新编译，微信开发者工具自动刷新

### 其他命令

```powershell
npm run build:mp-weixin   # 生产构建
```

---

## 2. 商家小程序 (miniprogram-2) — 原生开发

直接打开子目录即可：

```
C:\Users\Administrator\WeChatProjects\miniprogram-2\src
```

> 注意 `app.json` 在 `src/` 子目录下，不要打开 `miniprogram-2` 根目录。

---

## 3. 骑手小程序 (miniprogram-3) — 原生开发

```
C:\Users\Administrator\WeChatProjects\miniprogram-3\src
```

> 注意 `app.json` 在 `src/` 子目录下，不要打开 `miniprogram-3` 根目录。

---

## 4. Web 管理后台

```powershell
cd web-admin
npm run dev
```

访问 `http://localhost:5173`，默认凭据：管理员 `admin/admin123`

---

## 5. 小程序 AppID 配置

所有小程序的 `project.config.json` 中 `appid` 字段当前为 `"TODO:YOUR_APPID"`。
使用微信开发者工具"测试号"模式可以临时绕过，正式调试需要替换为真实 AppID。

---

## 6. 后端接口地址

所有前端模块请求后端 `localhost:8080`，微信开发者工具中需在"详情 → 本地设置"勾选"不校验合法域名"。
