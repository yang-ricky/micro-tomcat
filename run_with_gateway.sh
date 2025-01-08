#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 默认配置
CORE_PORTS=(8080)
GATEWAY_PORT=8090
JMX_PORT_START=9999

# 存储进程ID
declare -a SERVER_PIDS

# 编译项目
echo -e "${GREEN}Compiling project...${NC}"
mvn clean package

# 启动核心服务节点
start_core_node() {
    local PORT=$1
    local JMX_PORT=$2
    
    echo -e "${GREEN}Starting core node on port ${PORT} (JMX port: ${JMX_PORT})${NC}"
    
    # 切换到 micro-tomcat-core 目录执行，这样可以正确找到 webroot
    cd micro-tomcat-core
    
    java -Dcom.sun.management.jmxremote \
         -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
         -Dcom.sun.management.jmxremote.authenticate=false \
         -Dcom.sun.management.jmxremote.ssl=false \
         -cp target/classes:target/dependency/* \
         com.microtomcat.HttpServer --port=${PORT} &
    
    local PID=$!
    SERVER_PIDS+=($PID)
    echo "Core node started with PID: $PID"
    
    # 切回根目录
    cd ..
    
    sleep 2
}

# 启动网关服务器
start_gateway() {
    echo -e "${GREEN}Starting Gateway Server on port ${GATEWAY_PORT}...${NC}"
    
    java -cp micro-tomcat-gateway/target/classes:micro-tomcat-core/target/classes \
         com.microtomcat.gateway.GatewayServer --port=${GATEWAY_PORT} &
         
    local PID=$!
    SERVER_PIDS+=($PID)
    echo "Gateway started with PID: $PID"
}

# 停止所有服务
stop_all() {
    echo -e "${GREEN}Stopping all services...${NC}"
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
    for PORT in "${CORE_PORTS[@]}"; do
        if lsof -i :$PORT >/dev/null 2>&1; then
            echo "Cleaning up port $PORT"
            kill -9 $(lsof -t -i:$PORT) 2>/dev/null
        fi
    done
    
    if lsof -i :${GATEWAY_PORT} >/dev/null 2>&1; then
        echo "Cleaning up gateway port ${GATEWAY_PORT}"
        kill -9 $(lsof -t -i:${GATEWAY_PORT}) 2>/dev/null
    fi
    
    # 清理JMX端口
    for ((i=0; i<${#CORE_PORTS[@]}; i++)); do
        local JMX_PORT=$((JMX_PORT_START + i))
        if lsof -i :$JMX_PORT >/dev/null 2>&1; then
            echo "Cleaning up JMX port $JMX_PORT"
            kill -9 $(lsof -t -i:$JMX_PORT) 2>/dev/null
        fi
    done
}

# 启动所有服务
echo -e "${GREEN}Starting MicroTomcat Cluster...${NC}"

# 先启动核心服务节点
for ((i=0; i<${#CORE_PORTS[@]}; i++)); do
    start_core_node ${CORE_PORTS[$i]} $((JMX_PORT_START + i))
done

# 等待核心服务启动完成
sleep 3

# 启动网关
start_gateway

echo -e "\n${GREEN}All services are running!${NC}"
echo "Core nodes are running on ports: ${CORE_PORTS[@]}"
echo "Gateway is running on port: ${GATEWAY_PORT}"
echo -e "\nYou can connect to JMX using:"
for ((i=0; i<${#CORE_PORTS[@]}; i++)); do
    echo "Core Node $((i+1)): jconsole localhost:$((JMX_PORT_START + i))"
done

# 等待用户输入以停止服务
echo -e "\nPress Enter to stop all services..."
read

# 停止所有服务
stop_all

# 清理所有端口
cleanup_ports

echo -e "${GREEN}All services stopped successfully${NC}"