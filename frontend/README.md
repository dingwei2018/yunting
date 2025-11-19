# 北京云听音频精修系统 - 前端

## 项目简介

北京云听音频精修系统前端，基于 Vue 3 + Element Plus 开发。

## 技术栈

- Vue 3
- Vue Router 4
- Element Plus
- Axios
- Vite

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发运行

```bash
npm run dev
```

访问 http://localhost:3000

### 构建

```bash
npm run build
```

## 项目结构

```
frontend/
├── src/
│   ├── api/          # API 封装
│   ├── router/       # 路由配置
│   ├── views/        # 页面组件
│   ├── App.vue       # 根组件
│   └── main.js       # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 页面说明

- `/` - 测试页面，用于测试各个弹框
- `/text-input` - 文本输入页面
- `/sentences?task_id={id}` - 拆句结果页面
- `/edit?task_id={id}&sentence_id={id}` - 精修页面

## 开发说明

详细开发说明请查看 [开发说明文档](./开发说明文档.md)

## API 配置

在 `.env` 文件中配置 API 地址：

```
VITE_API_BASE_URL=/api/v1
```

开发环境使用 Vite 代理，生产环境需要配置 CORS。

