<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# pprofview 更新日志

## [Unreleased]
### Added
- 初始化项目结构
- 配置基础开发环境
- Pprof 运行配置
  - 支持三种运行种类：文件、目录、软件包
  - 响应式智能填充：
    - 文件模式：自动查找 main.go 或包含 main 函数的文件
    - 目录模式：自动使用工作目录
    - 软件包模式：自动读取 go.mod 并扫描所有子包
  - 动态更新：切换运行种类或更改工作目录时自动更新选项
  - 支持多种采集模式：运行时采样、HTTP 服务、手动采集、编译时插桩
  - 支持多种性能分析类型：CPU、堆内存、协程、阻塞、互斥锁、内存分配
  - 可配置工作目录、程序参数、环境变量、Go 构建标志
  - 自动设置 pprof 相关环境变量
  - 可配置输出目录和采样参数
- pprof 示例代码
  - HTTP 服务模式示例
  - 运行时采样模式示例（runtime_sampling_example.go）
  - 手动采集模式示例
  - 编译时插桩模式说明
  - 详细的使用指南（USAGE.md、RUNTIME_SAMPLING_GUIDE.md）
