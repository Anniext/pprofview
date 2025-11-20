# pprof 可视化使用示例

## 功能概述

pprofview 插件集成了 `go tool pprof` 命令，提供多种可视化方式来分析性能数据。

## 使用方法

### 1. 右键菜单可视化

在项目视图或编辑器中，右键点击 `.pprof` 文件，选择 **"使用 go tool pprof 可视化"**。

支持的文件格式：
- `.pprof` 文件
- `.pb.gz` 文件
- 包含 `cpu`、`heap`、`goroutine` 等关键词的文件

### 2. 可视化类型

#### Web 浏览器（推荐）
- **描述**：在浏览器中打开交互式可视化界面
- **特点**：
  - 火焰图、调用图、源码视图等多种视图
  - 可交互式探索数据
  - 支持搜索和过滤
- **使用**：选择后会自动启动本地 HTTP 服务并打开浏览器

#### 文本报告
- **描述**：显示文本格式的性能报告
- **特点**：
  - 列出函数的 flat 和 cumulative 时间
  - 适合快速查看热点函数
- **输出位置**：pprof Output 工具窗口

#### 调用图 (SVG)
- **描述**：生成调用关系图
- **特点**：
  - 显示函数调用关系
  - 节点大小表示资源消耗
  - 箭头粗细表示调用频率
- **输出**：在文件同目录生成 `*_graph.svg`

#### 火焰图 (SVG)
- **描述**：生成火焰图
- **特点**：
  - 横向展示调用栈
  - 宽度表示资源消耗
  - 适合发现性能瓶颈
- **输出**：在文件同目录生成 `*_flame.svg`

#### Top 10 函数
- **描述**：显示资源消耗最多的 10 个函数
- **特点**：快速定位热点
- **输出位置**：pprof Output 工具窗口

#### 完整函数列表
- **描述**：显示所有函数的详细信息
- **特点**：包含源码位置和详细统计
- **输出位置**：pprof Output 工具窗口

#### 简要信息
- **描述**：显示 profile 的概要信息
- **特点**：快速了解数据概况
- **输出位置**：pprof Output 工具窗口

## 自动可视化

在运行配置中启用 **"自动打开结果"** 选项后，程序运行完成会自动：
1. 查找生成的 pprof 文件
2. 优先选择 CPU profile
3. 在浏览器中打开交互式可视化

## 命令行等效

插件执行的命令等效于：

```bash
# Web 可视化
go tool pprof -http=:0 cpu.pprof

# 文本报告
go tool pprof -text cpu.pprof

# 生成 SVG 图表
go tool pprof -svg -output=graph.svg cpu.pprof
go tool pprof -flame -output=flame.svg cpu.pprof

# Top 函数
go tool pprof -top cpu.pprof

# 函数列表
go tool pprof -list=. cpu.pprof

# 简要信息
go tool pprof -peek=. cpu.pprof
```

## 前置要求

- 已安装 Go 工具链
- `go tool pprof` 可用
- 对于图形化输出，需要安装 Graphviz（可选）

## 提示

1. **Web 可视化最强大**：推荐首选，提供最丰富的交互功能
2. **SVG 文件可分享**：生成的 SVG 文件可以分享给团队成员
3. **文本报告适合 CI**：在 CI/CD 环境中可以使用文本报告
4. **工具窗口查看历史**：pprof Output 工具窗口保留所有输出历史

## 常见问题

### 浏览器没有自动打开
- 检查系统默认浏览器设置
- 查看 IDE 通知中的 URL，手动复制到浏览器

### 生成 SVG 失败
- 安装 Graphviz：`brew install graphviz` (macOS) 或 `apt install graphviz` (Linux)
- 或使用 Web 可视化代替

### 找不到 go 命令
- 确保 Go 已正确安装
- 检查 PATH 环境变量
- 在 IDE 设置中配置 Go SDK 路径
