# pprofview 项目开发规范

## 项目概述

pprofview 是一个 JetBrains IDE 插件,用于可视化 Go 语言 pprof 性能分析数据。

### 核心功能

- 解析 pprof 格式文件 (protobuf 和文本格式)
- 火焰图可视化展示
- 调用图展示
- 性能数据统计分析
- 支持多种性能分析类型 (CPU、Heap、Goroutine 等)

## 技术栈

- **语言**: Kotlin
- **框架**: IntelliJ Platform SDK
- **构建工具**: Gradle (Kotlin DSL)
- **JVM 版本**: 21
- **目标平台**: IntelliJ IDEA Community Edition 2024.3+

## 代码规范

### 包结构

```
com.github.anniext.pprofview
├── parser/          # pprof 文件解析
├── model/           # 数据模型
├── ui/              # 用户界面组件
│   ├── flamegraph/  # 火焰图
│   ├── callgraph/   # 调用图
│   └── viewer/      # 文件查看器
├── actions/         # IDE 操作
├── services/        # 服务层
└── utils/           # 工具类
```

### 命名约定

- **类名**: 使用 PascalCase,如 `PprofParser`, `FlameGraphPanel`
- **函数名**: 使用 camelCase,如 `parseProfile`, `renderFlameGraph`
- **常量**: 使用 UPPER_SNAKE_CASE,如 `MAX_STACK_DEPTH`
- **包名**: 使用小写,如 `parser`, `flamegraph`

### 代码风格

- 使用 Kotlin 官方代码风格
- 每行最大长度 120 字符
- 使用 4 空格缩进
- 优先使用 Kotlin 特性 (data class, sealed class, extension functions 等)
- 避免使用 `!!` 操作符,优先使用安全调用 `?.` 和 Elvis 操作符 `?:`

### 注释规范

- 所有公共 API 必须有 KDoc 注释
- 注释使用中文
- 复杂逻辑需要添加行内注释说明
- 示例:

```kotlin
/**
 * 解析 pprof 格式的性能分析文件
 *
 * @param file 要解析的文件
 * @return 解析后的性能分析数据
 * @throws PprofParseException 当文件格式不正确时
 */
fun parseProfile(file: VirtualFile): Profile {
    // 实现代码
}
```

## 依赖管理

### 添加依赖

在 `gradle/libs.versions.toml` 中定义版本和依赖:

```toml
[versions]
protobuf = "3.25.0"

[libraries]
protobuf-java = { module = "com.google.protobuf:protobuf-java", version.ref = "protobuf" }
```

在 `build.gradle.kts` 中引用:

```kotlin
dependencies {
    implementation(libs.protobuf.java)
}
```

### 常用依赖

- Protocol Buffers: 用于解析 pprof protobuf 格式
- JFreeChart 或类似库: 用于图表绘制 (可选)

## UI 开发规范

### Swing 组件使用

- 优先使用 IntelliJ Platform 提供的 UI 组件 (JBPanel, JBLabel 等)
- 使用 Kotlin DSL 构建 UI
- 遵循 IntelliJ Platform UI Guidelines

### 示例

```kotlin
class FlameGraphPanel : JBPanel<FlameGraphPanel>() {
    init {
        layout = BorderLayout()
        // 添加组件
    }
}
```

## 测试规范

### 测试结构

```
src/test/kotlin/
└── com/github/anniext/pprofview/
    ├── parser/
    │   └── PprofParserTest.kt
    └── model/
        └── ProfileTest.kt
```

### 测试命名

- 测试类名: `{ClassName}Test`
- 测试方法名: 使用描述性名称,如 `testParseValidProfile()`

### 测试数据

- 测试数据放在 `src/test/testData/` 目录
- 使用真实的 pprof 文件作为测试数据

## 插件配置

### plugin.xml 规范

- 所有扩展点必须有注释说明用途
- 操作 (Actions) 必须配置合适的图标和快捷键
- 文件类型关联必须明确指定支持的文件扩展名

### 示例

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- pprof 文件类型 -->
    <fileType
        name="pprof"
        implementationClass="com.github.anniext.pprofview.PprofFileType"
        fieldName="INSTANCE"
        language="pprof"
        extensions="pprof;pb.gz"/>
</extensions>
```

## 性能优化

### 文件解析

- 大文件使用流式解析,避免一次性加载到内存
- 使用后台线程解析,避免阻塞 UI 线程
- 实现进度指示器

### UI 渲染

- 使用虚拟化技术渲染大量数据
- 实现懒加载和按需渲染
- 缓存渲染结果

## 错误处理

### 异常处理

- 定义自定义异常类型
- 使用 Result 类型处理可能失败的操作
- 向用户显示友好的错误消息

### 示例

```kotlin
sealed class ParseResult {
    data class Success(val profile: Profile) : ParseResult()
    data class Error(val message: String, val cause: Throwable?) : ParseResult()
}
```

## 日志规范

- 使用 IntelliJ Platform 的日志系统
- 日志级别:
  - ERROR: 错误和异常
  - WARN: 警告信息
  - INFO: 重要操作信息
  - DEBUG: 调试信息

### 示例

```kotlin
import com.intellij.openapi.diagnostic.thisLogger

class PprofParser {
    private val logger = thisLogger()
    
    fun parse(file: VirtualFile) {
        logger.info("开始解析文件: ${file.name}")
        try {
            // 解析逻辑
        } catch (e: Exception) {
            logger.error("解析文件失败", e)
        }
    }
}
```

## 版本控制

### 提交规范

使用 Conventional Commits 规范:

- `feat:` 新功能
- `fix:` Bug 修复
- `docs:` 文档更新
- `style:` 代码格式调整
- `refactor:` 重构
- `test:` 测试相关
- `chore:` 构建/工具相关

### 示例

```
feat: 添加火焰图渲染功能
fix: 修复大文件解析内存溢出问题
docs: 更新 README 使用说明
```

## 发布流程

1. 更新 `CHANGELOG.md`
2. 更新 `gradle.properties` 中的版本号
3. 运行测试: `./gradlew test`
4. 构建插件: `./gradlew buildPlugin`
5. 创建 Git tag
6. 发布到 GitHub Releases

## 资源

- [IntelliJ Platform SDK 文档](https://plugins.jetbrains.com/docs/intellij/)
- [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- [pprof 格式规范](https://github.com/google/pprof/tree/main/proto)
