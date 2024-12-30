#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 默认网关端口
GATEWAY_PORT=8090

# 存储进程ID
declare -a SERVER_PIDS

# 编译项目
echo -e "${GREEN}Compiling project...${NC}"
cd ..
mvn clean package
cd micro-tomcat-gateway

# 启动网关服务器
start_gateway() {
    echo -e "${GREEN}Starting Gateway Server on port ${GATEWAY_PORT}...${NC}"
    
    java -cp target/classes:../micro-tomcat-core/target/classes \
         com.microtomcat.gateway.GatewayServer --port=${GATEWAY_PORT} &
         
    local PID=$!
    SERVER_PIDS+=($PID)
    echo "Gateway started with PID: $PID"
}

# 停止网关服务
stop_gateway() {
    echo -e "${GREEN}Stopping Gateway...${NC}"
    for PID in "${SERVER_PIDS[@]}"; do
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            wait $PID 2>/dev/null
            echo "Stopped process with PID: $PID"
        fi
    done
}

# 清理端口
cleanup_ports() {
    echo "Checking for remaining processes..."
    if lsof -i :${GATEWAY_PORT} >/dev/null 2>&1; then
        echo "Cleaning up gateway port ${GATEWAY_PORT}"
        kill -9 $(lsof -t -i:${GATEWAY_PORT}) 2>/dev/null
    fi
}

# 启动网关
start_gateway

echo -e "\n${GREEN}Gateway is running!${NC}"
echo "Gateway is running on port: ${GATEWAY_PORT}"

# 等待用户输入以停止服务
echo -e "\nPress Enter to stop the gateway..."
read

# 停止网关
stop_gateway

# 清理端口
cleanup_ports

echo -e "${GREEN}Gateway stopped successfully${NC}" 