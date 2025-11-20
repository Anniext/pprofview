# 运行时采样使用指南

## 概述

运行时采样模式允许你在程序运行时自动采集性能数据。插件提供了完整的示例代码，展示如何通过环境变量配置 pprof 性能分析。

## 工作原理

1. 在你的 Go 程序中添加 pprof 初始化代码
2. 程序读取环境变量（如 `PPROF_OUTPUT_DIR`、`PPROF_ENABLE_CPU` 等）
3. 根据环境变量配置启用相应的性能分析
4. 程序运行时自动采集性能数据
5. 程序结束后生成 pprof 文件

## 快速开始

### 方法 1：使用示例代码（推荐）

1. **复制示例代码**
   
   将 `src/main/resources/examples/runtime_sampling_example.go` 复制到你的项目中。

2. **设置环境变量**
   
   在运行程序前设置以下环境变量：
   ```bash
   export PPROF_OUTPUT_DIR=./pprof_output
   export PPROF_ENABLE_CPU=true
   export PPROF_ENABLE_HEAP=true
   export PPROF_CPU_DURATION=30
   export PPROF_MEM_RATE=524288
   ```

3. **运行程序**
   
   ```bash
   go run runtime_sampling_example.go
   ```

4. **查看结果**
   
   程序结束后，在 `pprof_output` 目录中查看生成的 pprof 文件。

### 方法 2：集成到现有项目

如果你已经有一个 Go 项目，可以将 pprof 初始化代码集成到你的 `main` 函数中。

#### 步骤 1：添加 pprof 初始化代码

在你的 `main.go` 文件中添加以下代码：

```go
package main

import (
    "log"
    "os"
    "path/filepath"
    "runtime"
    "runtime/pprof"
    "strconv"
    "time"
)

func main() {
    // 初始化 pprof
    initPprof()
    
    // 你的程序逻辑
    // ...
}

func initPprof() {
    outputDir := os.Getenv("PPROF_OUTPUT_DIR")
    if outputDir == "" {
        return // 未启用 pprof
    }
    
    log.Printf("[pprof] 输出目录: %s", outputDir)
    
    // 设置采样率
    if memRateStr := os.Getenv("PPROF_MEM_RATE"); memRateStr != "" {
        if memRate, err := strconv.Atoi(memRateStr); err == nil && memRate > 0 {
            runtime.MemProfileRate = memRate
        }
    }
    
    if blockRateStr := os.Getenv("PPROF_BLOCK_RATE"); blockRateStr != "" {
        if blockRate, err := strconv.Atoi(blockRateStr); err == nil && blockRate > 0 {
            runtime.SetBlockProfileRate(blockRate)
        }
    }
    
    if mutexFractionStr := os.Getenv("PPROF_MUTEX_FRACTION"); mutexFractionStr != "" {
        if mutexFraction, err := strconv.Atoi(mutexFractionStr); err == nil && mutexFraction > 0 {
            runtime.SetMutexProfileFraction(mutexFraction)
        }
    }
    
    // CPU 分析
    if os.Getenv("PPROF_ENABLE_CPU") == "true" {
        cpuFile := filepath.Join(outputDir, "cpu.pprof")
        f, err := os.Create(cpuFile)
        if err == nil {
            if err := pprof.StartCPUProfile(f); err == nil {
                log.Printf("[pprof] CPU profiling 已启动")
                
                duration := 30
                if d := os.Getenv("PPROF_CPU_DURATION"); d != "" {
                    if dur, err := strconv.Atoi(d); err == nil {
                        duration = dur
                    }
                }
                
                go func() {
                    time.Sleep(time.Duration(duration) * time.Second)
                    pprof.StopCPUProfile()
                    f.Close()
                    log.Printf("[pprof] CPU profiling 已完成")
                }()
            }
        }
    }
    
    // 注册退出时的清理
    defer writePprofProfiles(outputDir)
}

func writePprofProfiles(outputDir string) {
    if os.Getenv("PPROF_ENABLE_HEAP") == "true" {
        heapFile := filepath.Join(outputDir, "heap.pprof")
        f, err := os.Create(heapFile)
        if err == nil {
            runtime.GC()
            pprof.WriteHeapProfile(f)
            f.Close()
        }
    }
    
    // 其他类型的 profile...
    // 参考 runtime_sampling_example.go 获取完整代码
}
```

#### 步骤 2：配置运行配置

按照方法 1 的步骤 2 配置运行配置。

#### 步骤 3：运行和查看结果

按照方法 1 的步骤 3-4 运行程序并查看结果。

## 配置选项说明

### 采集模式

- **运行时采样**：程序运行时自动采样（本指南的主题）
- **编译时插桩**：在编译时插入分析代码（如 `-race`）
- **手动采集**：完全由代码控制
- **HTTP 服务**：启动 HTTP 服务器提供实时数据

### 性能分析类型

| 类型 | 说明 | 环境变量 | 输出文件 |
|------|------|----------|----------|
| CPU 分析 | 分析 CPU 使用情况 | `PPROF_ENABLE_CPU` | `cpu.pprof` |
| 堆内存分析 | 分析内存分配情况 | `PPROF_ENABLE_HEAP` | `heap.pprof` |
| 协程分析 | 查看所有协程状态 | `PPROF_ENABLE_GOROUTINE` | `goroutine.pprof` |
| 阻塞分析 | 分析阻塞操作 | `PPROF_ENABLE_BLOCK` | `block.pprof` |
| 互斥锁分析 | 分析锁竞争 | `PPROF_ENABLE_MUTEX` | `mutex.pprof` |
| 内存分配分析 | 分析所有内存分配 | `PPROF_ENABLE_ALLOCS` | `allocs.pprof` |

### 采样参数

| 参数 | 说明 | 环境变量 | 默认值 |
|------|------|----------|--------|
| 输出目录 | pprof 文件保存位置 | `PPROF_OUTPUT_DIR` | 系统临时目录 |
| CPU 持续时间 | CPU 采样持续时间（秒） | `PPROF_CPU_DURATION` | 30 |
| 内存采样率 | 每分配多少字节采样一次 | `PPROF_MEM_RATE` | 524288 (512KB) |
| 阻塞采样率 | 阻塞事件采样率 | `PPROF_BLOCK_RATE` | 1 |
| 互斥锁采样率 | 互斥锁事件采样率 | `PPROF_MUTEX_FRACTION` | 1 |

## 环境变量参考

插件会自动设置以下环境变量：

```bash
# 输出目录
PPROF_OUTPUT_DIR=/path/to/output

# 启用的分析类型
PPROF_ENABLE_CPU=true
PPROF_ENABLE_HEAP=true
PPROF_ENABLE_GOROUTINE=true
PPROF_ENABLE_BLOCK=true
PPROF_ENABLE_MUTEX=true
PPROF_ENABLE_ALLOCS=true

# 采样参数
PPROF_CPU_DURATION=30
PPROF_MEM_RATE=524288
PPROF_BLOCK_RATE=1
PPROF_MUTEX_FRACTION=1
```

你的程序可以读取这些环境变量并调用相应的 pprof API。

## 常见问题

### Q: 为什么程序结束后没有生成 pprof 文件？

A: 请检查：
1. 是否在代码中添加了 pprof 初始化代码
2. 是否正确读取了环境变量
3. 输出目录是否有写入权限
4. 程序是否正常退出（而不是被强制终止）

### Q: CPU profiling 没有数据？

A: 请确保：
1. 程序运行时间足够长（至少几秒钟）
2. CPU 持续时间设置合理
3. 程序有实际的 CPU 负载

### Q: 内存 profiling 数据不准确？

A: 可以尝试：
1. 调整内存采样率（更小的值 = 更精确，但开销更大）
2. 在写入 profile 前调用 `runtime.GC()`
3. 让程序运行更长时间以积累更多样本

### Q: 如何在不修改代码的情况下使用运行时采样？

A: 目前运行时采样需要在代码中添加 pprof 初始化逻辑。如果不想修改代码，可以考虑：
1. 使用 HTTP 服务模式（需要导入 `net/http/pprof`）
2. 使用编译时插桩模式（适用于特定场景如竞态检测）
3. 使用手动采集模式（在需要分析的代码段手动调用 pprof API）

## 最佳实践

1. **开发阶段**：使用运行时采样快速定位性能问题
2. **生产环境**：使用 HTTP 服务模式，按需采集数据
3. **CI/CD**：使用编译时插桩进行竞态检测和覆盖率分析
4. **性能测试**：结合压测工具，使用运行时采样分析瓶颈

## 示例代码

完整的示例代码请参考：
- `src/main/resources/examples/runtime_sampling_example.go` - 运行时采样示例
- `src/main/resources/examples/pprof_example.go` - 所有模式的综合示例

## 相关文档

- [pprof 官方文档](https://github.com/google/pprof)
- [Go 性能分析指南](https://go.dev/blog/pprof)
- [runtime/pprof 包文档](https://pkg.go.dev/runtime/pprof)
