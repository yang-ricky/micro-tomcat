#!/bin/bash

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 检查必要的目录是否存在
check_directories() {
    if [ ! -d "webroot" ]; then
        error "webroot directory not found!"
        exit 1
    fi
    
    if [ ! -d "target/classes" ]; then
        warn "target/classes not found, running mvn compile first..."
        mvn compile
    fi
}

# 编译 Servlet 文件
compile_servlets() {
    log "Starting to compile Servlet files..."
    
    # 计数器
    total=0
    success=0
    failed=0
    
    find webroot -name "*.java" | while read file; do
        total=$((total + 1))
        if [[ $file == */WEB-INF/classes/* ]]; then
            # 如果是在 WEB-INF/classes 目录下，保持在原目录编译
            dir=$(dirname "$file")
            log "Compiling $file to $dir"
            if javac -cp target/classes -d "$dir" "$file" 2>/dev/null; then
                success=$((success + 1))
                log "Successfully compiled $file"
            else
                failed=$((failed + 1))
                error "Failed to compile $file"
            fi
        else
            # 其他情况，编译到 webroot 目录
            log "Compiling $file to webroot"
            if javac -cp target/classes -d webroot "$file" 2>/dev/null; then
                success=$((success + 1))
                log "Successfully compiled $file"
            else
                failed=$((failed + 1))
                error "Failed to compile $file"
            fi
        fi
    done
    
    log "Compilation completed:"
    log "Total files: $total"
    log "Successfully compiled: $success"
    if [ $failed -gt 0 ]; then
        error "Failed to compile: $failed"
    fi
}

# 清理之前编译的文件
clean_class_files() {
    log "Cleaning previous class files..."
    find webroot -name "*.class" -type f -delete
    log "Clean completed"
}

# 主函数
main() {
    log "Starting servlet compilation process..."
    
    # 检查目录
    check_directories
    
    # 清理旧的 class 文件
    clean_class_files
    
    # 编译 servlet
    compile_servlets
    
    log "All done!"
}

# 运行主函数
main