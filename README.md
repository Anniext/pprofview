# pprofview

![Build](https://github.com/Anniext/pprofview/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
一个用于在 JetBrains IDE 中可视化 pprof 性能分析数据的插件。

支持解析和展示 Go 语言 pprof 格式的性能分析文件,包括 CPU、内存、goroutine 等性能数据的可视化展示。

主要功能:
- 解析 pprof 格式文件 (protobuf 和文本格式)
- 火焰图可视化展示
- 调用图展示
- 性能数据统计分析
- 支持多种性能分析类型 (CPU、Heap、Goroutine 等)
- 提供完整的 pprof 使用示例代码
<!-- Plugin description end -->

## 安装

- 使用 IDE 内置插件系统:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>搜索 "pprofview"</kbd> >
  <kbd>Install</kbd>
  
- 手动安装:

  从 [最新版本](https://github.com/Anniext/pprofview/releases/latest) 下载插件并手动安装:
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## 使用

详细使用指南请参考：[USAGE.md](USAGE.md)

### 查看 pprof 文件

1. 在项目中右键点击 pprof 文件 (`.pb.gz`, `.pprof` 等格式)
2. 选择 "Open with pprofview" 打开可视化界面
3. 查看火焰图、调用图等性能分析数据

### 使用 pprof 性能分析

插件提供了示例代码，展示如何在 Go 程序中使用 pprof 进行性能分析。

#### 示例代码

参考 `src/main/resources/examples/pprof_example.go`，其中包含了多种 pprof 使用方式：

**1. HTTP 服务模式**
- 启动 pprof HTTP 服务器，提供实时性能数据访问
- 适用场景：长期运行的服务、实时监控
- 访问地址：`http://localhost:6060/debug/pprof/`

**2. 运行时采样模式**
- 程序运行时自动采样性能数据
- 适用场景：CPU、内存、协程等常规性能分析
- 参考示例：`src/main/resources/examples/runtime_sampling_example.go`

**3. 手动采集模式**
- 在代码中手动调用 pprof API 控制采集
- 适用场景：需要精确控制采集时机和范围

**4. 编译时插桩模式**
- 使用编译参数如 `-race`（竞态检测）或 `-cover`（代码覆盖率）

#### 性能分析类型

- **CPU 分析**：分析 CPU 使用情况，找出热点函数
- **堆内存分析**：分析内存分配情况，发现内存泄漏
- **协程分析**：查看所有协程的状态和调用栈
- **阻塞分析**：分析阻塞操作（channel、锁等）
- **互斥锁分析**：分析锁竞争情况
- **内存分配分析**：分析所有内存分配（包括已释放的）

#### 快速开始

1. 复制示例代码到你的项目
2. 根据需要选择合适的采集模式
3. 运行程序生成 pprof 文件
4. 使用插件打开和分析 pprof 文件

## 开发

本项目基于 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template) 构建。

### 构建

```bash
./gradlew buildPlugin
```

### 运行

```bash
./gradlew runIde
```

### 测试

```bash
./gradlew test
```
