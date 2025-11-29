#!/bin/bash

# 云听后端服务 - 快速部署脚本
# 适用于 Ubuntu 24.04.3 系统，root 用户

set -e

echo "=========================================="
echo "云听后端服务 - Docker 部署脚本"
echo "=========================================="

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo "错误: 请使用 root 用户运行此脚本"
    exit 1
fi

# 检查 Docker 是否已安装
if ! command -v docker &> /dev/null; then
    echo "Docker 未安装，开始安装 Docker..."
    
    # 更新系统包
    apt update
    
    # 安装必要的依赖
    apt install -y ca-certificates curl gnupg lsb-release
    
    # 添加 Docker 官方 GPG 密钥
    mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    
    # 添加 Docker 仓库
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    # 安装 Docker Engine
    apt update
    apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    # 启动 Docker 服务
    systemctl start docker
    systemctl enable docker
    
    echo "Docker 安装完成！"
else
    echo "Docker 已安装: $(docker --version)"
fi

# 检查 Docker Compose 是否已安装
if ! command -v docker compose &> /dev/null; then
    echo "错误: Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
else
    echo "Docker Compose 已安装: $(docker compose version)"
fi

# 检查 jar 文件是否存在
if [ ! -f "target/backend-1.0.0.jar" ]; then
    echo "错误: 未找到 target/backend-1.0.0.jar 文件"
    echo "请确保 jar 文件已正确放置在 target/ 目录下"
    exit 1
fi

echo ""
echo "开始构建 Docker 镜像..."
docker compose build

echo ""
echo "启动服务..."
docker compose up -d

echo ""
echo "等待服务启动（30秒）..."
sleep 30

echo ""
echo "检查服务状态..."
docker compose ps

echo ""
echo "检查后端服务健康状态..."
if curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "✓ 后端服务运行正常！"
else
    echo "⚠ 后端服务可能还在启动中，请稍后访问: http://localhost:8080/api/health"
    echo "查看日志: docker compose logs backend -f"
fi

echo ""
echo "=========================================="
echo "部署完成！"
echo "=========================================="
echo ""
echo "常用命令："
echo "  查看日志: docker compose logs -f"
echo "  查看状态: docker compose ps"
echo "  停止服务: docker compose stop"
echo "  重启服务: docker compose restart"
echo "  健康检查: curl http://localhost:8080/api/health"
echo ""
echo "访问地址: http://$(hostname -I | awk '{print $1}'):8080"
echo ""

