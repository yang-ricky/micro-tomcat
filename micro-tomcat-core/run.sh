#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 默认配置
DEFAULT_PORTS=(8080 8081 8082)
JMX_PORT_START=9999

# 编译项目
echo -e "${GREEN}Compiling project...${NC}"
mvn clean package

# 存储进程ID
declare -a SERVER_PIDS

# 启动集群节点
start_node() {
    local PORT=$1
    local JMX_PORT=$2
    
    echo -e "${GREEN}Starting node on port ${PORT} (JMX port: ${JMX_PORT})${NC}"
    
    java -Dcom.sun.management.jmxremote \
         -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
         -Dcom.sun.management.jmxremote.authenticate=false \
         -Dcom.sun.management.jmxremote.ssl=false \
         -cp target/classes:target/dependency/* \
         com.microtomcat.HttpServer --port=${PORT} &
    
    local PID=$!
    SERVER_PIDS+=($PID)
    echo "Node started with PID: $PID"
    
    sleep 5
}

# 停止所有节点
stop_nodes() {
    echo -e "${GREEN}Stopping all nodes...${NC}"
    for PID in "${SERVER_PIDS[@]}"; do
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            wait $PID 2>/dev/null
            echo "Stopped node with PID: $PID"
        fi
    done
}

# 清理端口
cleanup_ports() {
    echo "Checking for remaining processes..."
    for PORT in "${DEFAULT_PORTS[@]}"; do
        if lsof -i :$PORT >/dev/null 2>&1; then
            echo "Cleaning up port $PORT"
            kill -9 $(lsof -t -i:$PORT) 2>/dev/null
        fi
    done
    
    # 清理JMX端口
    for ((i=0; i<${#DEFAULT_PORTS[@]}; i++)); do
        local JMX_PORT=$((JMX_PORT_START + i))
        if lsof -i :$JMX_PORT >/dev/null 2>&1; then
            echo "Cleaning up JMX port $JMX_PORT"
            kill -9 $(lsof -t -i:$JMX_PORT) 2>/dev/null
        fi
    done
}

# 启动集群
echo -e "${GREEN}Starting MicroTomcat Cluster...${NC}"
for ((i=0; i<${#DEFAULT_PORTS[@]}; i++)); do
    start_node ${DEFAULT_PORTS[$i]} $((JMX_PORT_START + i))
done

echo -e "\n${GREEN}Cluster is running!${NC}"
echo "You can connect to JMX using:"
for ((i=0; i<${#DEFAULT_PORTS[@]}; i++)); do
    echo "Node $((i+1)): jconsole localhost:$((JMX_PORT_START + i))"
done

# 等待用户输入以停止服务器
echo -e "\nPress Enter to stop the cluster..."
read

# 停止所有节点
stop_nodes

# 清理所有端口
cleanup_ports

echo -e "${GREEN}Cluster stopped successfully${NC}"