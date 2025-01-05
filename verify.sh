#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color
YELLOW='\033[1;33m'

# 测试配置
PORTS=(8080)
TIMEOUT=3  # 超时时间（秒）

# 测试用例定义 (使用普通数组)
TEST_PATHS=(
    "/"
    "/hello.txt"
    "/ping"
    "/servlet/HelloServlet"
    "/servlet/SessionTestServlet"
    "/servlet/WebRootServlet"
    "/app1/servlet/App1Servlet"
    "/app2/servlet/App2Servlet"
    "/app1/hello.txt"
    "/app2/hello.txt"
)
TEST_NAMES=(
    "Root Access"
    "Static File"
    "Health Check"
)

# 打印带颜色的消息
print_result() {
    local test_name=$1
    local status=$2
    local port=$3
    local message=$4
    
    if [ "$status" == "PASS" ]; then
        echo -e "${GREEN}✓${NC} Port $port - $test_name: ${GREEN}$message${NC}"
    else
        echo -e "${RED}✗${NC} Port $port - $test_name: ${RED}$message${NC}"
    fi
}

# 测试单个URL
test_url() {
    local port=$1
    local path=$2
    local test_name=$3
    local url="http://localhost:${port}${path}"
    
    echo -e "${YELLOW}Testing $url${NC}"
    
    # 使用curl测试URL，设置超时
    response=$(curl -s -w "%{http_code}" --max-time $TIMEOUT "$url")
    status_code=${response: -3}  # 获取最后3个字符（HTTP状态码）
    content=${response:0:${#response}-3}  # 获取响应内容
    
    if [ $? -eq 0 ] && [ "$status_code" == "200" ]; then
        if [ ! -z "$content" ]; then
            print_result "$test_name" "PASS" "$port" "OK (Status: $status_code)"
            return 0
        else
            print_result "$test_name" "FAIL" "$port" "Empty response"
            return 1
        fi
    else
        print_result "$test_name" "FAIL" "$port" "Failed (Status: $status_code)"
        return 1
    fi
}

# 主测试函数
run_tests() {
    echo -e "${YELLOW}Starting verification tests...${NC}"
    echo "------------------------"
    
    local total_tests=0
    local passed_tests=0
    
    for port in "${PORTS[@]}"; do
        echo -e "\n${YELLOW}Testing node on port $port${NC}"
        echo "------------------------"
        
        for i in "${!TEST_PATHS[@]}"; do
            path=${TEST_PATHS[$i]}
            name=${TEST_NAMES[$i]}
            total_tests=$((total_tests + 1))
            
            if test_url $port "$path" "$name"; then
                passed_tests=$((passed_tests + 1))
            fi
            echo "------------------------"
        done
    done
    
    echo -e "\n${YELLOW}Test Summary${NC}"
    echo "------------------------"
    echo -e "Total Tests: $total_tests"
    echo -e "Passed: ${GREEN}$passed_tests${NC}"
    echo -e "Failed: ${RED}$((total_tests - passed_tests))${NC}"
    
    if [ $passed_tests -eq $total_tests ]; then
        echo -e "\n${GREEN}All tests passed! ✨${NC}"
        return 0
    else
        echo -e "\n${RED}Some tests failed! 🔥${NC}"
        return 1
    fi
}

# 检查服务器是否在运行
check_servers() {
    echo -e "${YELLOW}Checking if servers are running...${NC}"
    local all_running=true
    
    for port in "${PORTS[@]}"; do
        if nc -z localhost $port 2>/dev/null; then
            echo -e "${GREEN}✓${NC} Server on port $port is running"
        else
            echo -e "${RED}✗${NC} Server on port $port is not running"
            all_running=false
        fi
    done
    
    if [ "$all_running" = false ]; then
        echo -e "\n${RED}Error: Some servers are not running. Please start them first.${NC}"
        exit 1
    fi
    
    echo "------------------------"
}

# 主函数
main() {
    # 检查必要的命令
    command -v curl >/dev/null 2>&1 || { echo "curl is required but not installed. Aborting." >&2; exit 1; }
    command -v nc >/dev/null 2>&1 || { echo "netcat is required but not installed. Aborting." >&2; exit 1; }
    
    # 检查服务器状态
    check_servers
    
    # 运行测试
    run_tests
}

# 执行主函数
main 