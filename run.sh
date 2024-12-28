#!/bin/bash

# 编译项目
mvn clean package

# 启动服务器，同时添加JMX支持和指定主类
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -cp target/micro-tomcat-1.0-SNAPSHOT.jar \
     com.microtomcat.HttpServer &

# 保存服务器进程ID
SERVER_PID=$!

echo "Server started with PID: $SERVER_PID"
echo "You can now connect to JMX using:"
echo "1. jconsole localhost:9999"
echo "2. or use JDK Mission Control: jmc"

# 等待用户输入以停止服务器
echo "Press Enter to stop the server..."
read

# 停止服务器
kill $SERVER_PID
# 等待进程结束
wait $SERVER_PID 2>/dev/null
echo "Server stopped"

# 额外检查确保端口已释放
if lsof -i :9999 >/dev/null 2>&1; then
    echo "Warning: Port 9999 is still in use"
    kill -9 $(lsof -t -i:9999) 2>/dev/null
fi

if lsof -i :8080 >/dev/null 2>&1; then
    echo "Warning: Port 8080 is still in use"
    kill -9 $(lsof -t -i:8080) 2>/dev/null
fi