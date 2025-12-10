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
- `/sentences?task_id={id}` - 拆句结果页面（支持内联精修、音色/参数调整）
- `/edit?task_id={id}&sentence_id={id}` - 精修页面

## 开发说明

详细开发说明请查看 [开发说明文档](./开发说明文档.md)

## 环境配置

项目支持多环境配置，通过环境变量文件管理不同环境的接口地址：

### 环境文件说明

- `.env` - 默认配置（本地开发）
- `.env.local` - 本地开发配置（会被 git 忽略，优先级最高）
- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置

### 当前配置

**本地环境** (`.env.local`):
- 接口地址: `http://localhost:8080`
- 使用相对路径 `/api/v1`，通过 Vite 代理转发

**开发环境** (`.env.development`):
- 接口地址: `http://123.60.64.142:8080/api/v1`
- 使用完整 URL，直接请求

**生产环境** (`.env.production`):
- 接口地址: 暂未配置（后续添加）

### 使用方法

**本地开发**（默认使用 `.env.local`）:
```bash
npm run dev
# 或
npm run dev:local
```

**开发环境**:
```bash
npm run dev:development
```

**构建开发环境**:
```bash
npm run build:development
```

**构建生产环境**:
```bash
npm run build:production
```

### API / Mock 配置

在对应的环境文件中配置：

```
VITE_API_BASE_URL=/api/v1  # 或完整 URL
VITE_USE_MOCK=true
```

- `VITE_USE_MOCK=true` 时，前端会使用内置 mock 数据，接口不可用时也能预览完整交互
- 设置为 `false` 即可切换到真实后端 API

**注意**: `.env.local` 文件会被 git 忽略，适合存放个人本地配置。如果需要使用开发环境配置，可以临时重命名或删除 `.env.local` 文件。

