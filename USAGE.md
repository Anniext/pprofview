# pprofview 使用指南

## 概述

pprofview 是一个 JetBrains IDE 插件，提供了完整的 Go pprof 性能分析示例代码和工具。

## 主要功能

### 1. pprof 示例代码

插件提供了多种 pprof 使用方式的示例代码：

#### HTTP 服务模式

参考 `src/main/resources/examples/pprof_example.go` 中的 HTTP 服务模式部分。

```go
import _ "net/http/pprof"

go func() {
    log.Println(http.ListenAndServe(":6060", nil))
}()
```

访问 `http://localhost:6060/debug/pprof/` 查看性能数据。

#### 运行时采样模式

参考 `src/main/resources/examples/runtime_sampling_example.go`。

这是一个完整的示例，展示如何：
- 读取环境变量配置
- 启动 CPU 分析
- 在程序退出时写入其他类型的 profile

**使用步骤：**

1. 复制示例代码到你的项目
2. 设置环境变量：
   ```bash
   export PPROF_OUTPUT_DIR=./pprof_output
   export PPROF_ENABLE_CPU=true
   export PPROF_ENABLE_HEAP=true
   export PPROF_CPU_DURATION=30
   ```
3. 运行程序
4. 查看生成的 pprof 文件

#### 手动采集模式

在代码中手动控制 pprof 采集：

```go
// CPU 分析
cpuFile, _ := os.Create("cpu.pprof")
pprof.StartCPUProfile(cpuFile)
defer func() {
    pprof.StopCPUProfile()
    cpuFile.Close()
}()

// 堆内存快照
heapFile, _ := os.Create("heap.pprof")
pprof.WriteHeapProfile(heapFile)
heapFile.Close()
```

### 2. 支持的性能分析类型

- **CPU 分析** (`cpu.pprof`)：分析 CPU 使用情况
- **堆内存分析** (`heap.pprof`)：分析内存分配
- **协程分析** (`goroutine.pprof`)：查看协程状态
- **阻塞分析** (`block.pprof`)：分析阻塞操作
- **互斥锁分析** (`mutex.pprof`)：分析锁竞争
- **内存分配分析** (`allocs.pprof`)：分析所有内存分配

### 3. 环境变量配置

运行时采样模式支持以下环境变量：

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `PPROF_OUTPUT_DIR` | 输出目录 | 当前目录 |
| `PPROF_ENABLE_CPU` | 启用 CPU 分析 | false |
| `PPROF_ENABLE_HEAP` | 启用堆内存分析 | false |
| `PPROF_ENABLE_GOROUTINE` | 启用协程分析 | false |
| `PPROF_ENABLE_BLOCK` | 启用阻塞分析 | false |
| `PPROF_ENABLE_MUTEX` | 启用互斥锁分析 | false |
| `PPROF_ENABLE_ALLOCS` | 启用内存分配分析 | false |
| `PPROF_CPU_DURATION` | CPU 采样持续时间（秒） | 30 |
| `PPROF_MEM_RATE` | 内存采样率（字节） | 524288 |
| `PPROF_BLOCK_RATE` | 阻塞采样率 | 1 |
| `PPROF_MUTEX_FRACTION` | 互斥锁采样率 | 1 |

## 快速开始

### 1. 使用 HTTP 服务模式（最简单）

```go
package main

import (
    "log"
    "net/http"
    _ "net/http/pprof"
)

func main() {
    go func() {
        log.Println(http.ListenAndServe(":6060", nil))
    }()
    
    // 你的程序逻辑
    // ...
}
```

运行程序后访问：
- `http://localhost:6060/debug/pprof/` - 查看所有可用的 profile
- `http://localhost:6060/debug/pprof/heap` - 下载堆内存 profile
- `http://localhost:6060/debug/pprof/profile?seconds=30` - 下载 30 秒的 CPU profile

### 2. 使用运行时采样模式（推荐）

1. 复制 `src/main/resources/examples/runtime_sampling_example.go` 到你的项目

2. 创建一个脚本来设置环境变量并运行：

```bash
#!/bin/bash
# run_with_pprof.sh

export PPROF_OUTPUT_DIR=./pprof_output
export PPROF_ENABLE_CPU=true
export PPROF_ENABLE_HEAP=true
export PPROF_ENABLE_GOROUTINE=true
export PPROF_CPU_DURATION=30
export PPROF_MEM_RATE=524288

go run main.go
```

3. 运行脚本：
```bash
chmod +x run_with_pprof.sh
./run_with_pprof.sh
```

4. 查看生成的 pprof 文件：
```bash
go tool pprof -http=:8080 pprof_output/cpu.pprof
```

### 3. 使用手动采集模式（精确控制）

```go
package main

import (
    "os"
    "runtime/pprof"
)

func main() {
    // 开始 CPU 分析
    cpuFile, _ := os.Create("cpu.pprof")
    pprof.StartCPUProfile(cpuFile)
    defer pprof.StopCPUProfile()
    defer cpuFile.Close()
    
    // 你的程序逻辑
    doWork()
    
    // 写入堆内存快照
    heapFile, _ := os.Create("heap.pprof")
    pprof.WriteHeapProfile(heapFile)
    heapFile.Close()
}

func doWork() {
    // 你的工作负载
}
```

## 分析 pprof 文件

### 使用 go tool pprof

```bash
# 交互式分析
go tool pprof cpu.pprof

# Web UI 分析
go tool pprof -http=:8080 cpu.pprof

# 生成火焰图
go tool pprof -http=:8080 cpu.pprof

# 查看 top 函数
go tool pprof -top cpu.pprof

# 生成调用图
go tool pprof -pdf cpu.pprof > cpu.pdf
```

### 常用命令

在 pprof 交互模式中：

- `top` - 显示占用最多的函数
- `list <function>` - 显示函数的源代码
- `web` - 在浏览器中显示调用图
- `pdf` - 生成 PDF 格式的调用图
- `help` - 显示帮助信息

## 最佳实践

### 1. 开发阶段

使用运行时采样快速定位性能问题：

```bash
export PPROF_OUTPUT_DIR=./pprof
export PPROF_ENABLE_CPU=true
export PPROF_ENABLE_HEAP=true
export PPROF_CPU_DURATION=10
go run main.go
```

### 2. 生产环境

使用 HTTP 服务模式，按需采集：

```go
import _ "net/http/pprof"

go func() {
    // 只在内网监听
    log.Println(http.ListenAndServe("localhost:6060", nil))
}()
```

### 3. 性能测试

结合压测工具使用：

```bash
# 启动程序（HTTP 模式）
go run main.go &

# 开始压测
ab -n 10000 -c 100 http://localhost:8080/

# 采集 CPU profile
curl http://localhost:6060/debug/pprof/profile?seconds=30 > cpu.pprof

# 分析
go tool pprof -http=:8081 cpu.pprof
```

### 4. 内存泄漏排查

```bash
# 采集两次堆内存快照
curl http://localhost:6060/debug/pprof/heap > heap1.pprof
# 等待一段时间
sleep 60
curl http://localhost:6060/debug/pprof/heap > heap2.pprof

# 对比分析
go tool pprof -base heap1.pprof heap2.pprof
```

## 常见问题

### Q: CPU profile 为空？

A: 确保：
1. 程序运行时间足够长
2. 有实际的 CPU 负载
3. CPU 采样持续时间设置合理

### Q: 内存数据不准确？

A: 尝试：
1. 调整内存采样率（更小 = 更精确）
2. 在采集前调用 `runtime.GC()`
3. 让程序运行更长时间

### Q: 如何在生产环境安全使用？

A: 建议：
1. 只在内网监听 pprof HTTP 服务
2. 使用防火墙限制访问
3. 考虑添加认证
4. 监控 pprof 对性能的影响

## 相关资源

- [Go 官方 pprof 文档](https://pkg.go.dev/runtime/pprof)
- [pprof 工具文档](https://github.com/google/pprof)
- [Go 性能分析博客](https://go.dev/blog/pprof)
- [详细的运行时采样指南](RUNTIME_SAMPLING_GUIDE.md)
