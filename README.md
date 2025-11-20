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
<!-- Plugin description end -->

## 安装

- 使用 IDE 内置插件系统:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>搜索 "pprofview"</kbd> >
  <kbd>Install</kbd>
  
- 手动安装:

  从 [最新版本](https://github.com/Anniext/pprofview/releases/latest) 下载插件并手动安装:
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## 使用

1. 在项目中右键点击 pprof 文件 (`.pb.gz`, `.pprof` 等格式)
2. 选择 "Open with pprofview" 打开可视化界面
3. 查看火焰图、调用图等性能分析数据

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
