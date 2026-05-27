# 多源异构数据融合与统计检验分析平台 V1.0 设计规格

## 目标

在 `D:\Java\project\platform` 中实现一个可本地运行的 Java Web 单体应用，用于“多源异构数据融合与统计检验分析平台 V1.0”的本地演示和软著源码材料支撑。

项目严格以 `propoal.md` 的 V1.0 范围为验收基础，同时视觉流程参考用户提供的截图：登录/注册后进入五个主导航页面，通过弹窗完成新增数据源、批量导入、规则配置、报告导出、通知公告、账号维护和退出确认等操作。

## 技术选型

- Java 17
- Maven
- Spring Boot 3.x 稳定版本
- Thymeleaf 服务端渲染
- 少量 AJAX 增强弹窗、上传预览和操作反馈
- SQLite 本地数据库
- Spring JDBC 或轻量 DAO
- Apache POI 解析 `.xlsx` 和 `.xls`
- Commons CSV 解析 CSV
- Jackson 处理 JSON
- Chart.js 实现统计页简单折线图和环形图

不使用 Docker，不使用 Flyway/Liquibase，不使用 Vue/React 独立前端，不使用 Spring Security/BCrypt。

## 项目命名与位置

- 项目目录：`D:\Java\project\platform`
- `groupId`: `com.dataprocess`
- `artifactId`: `data-fusion-platform`
- Java 包名：`com.dataprocess.platform`
- 应用名：`多源异构数据融合与统计检验分析平台V1.0`

运行和数据目录：

- 数据库：`D:\Java\project\platform\data\data_fusion.db`
- 上传文件：`D:\Java\project\platform\data\uploads`
- 导出文件：`D:\Java\project\platform\data\exports`
- 样例文件：`D:\Java\project\platform\data\samples`

## 页面结构

访问 `http://localhost:8080` 时，未登录用户跳转到 `/login`。登录成功后进入 `/aggregation`。

主导航保留五个页面：

- `/aggregation`：数据汇聚
- `/fusion`：融合处理
- `/statistics`：统计分析
- `/quality`：核验核查
- `/ledger`：台账查阅

不再单独实现传统 12 页面结构。任务书中的其他页面能力通过弹窗、顶部入口或页面内区域承载：

- 登录页和注册页独立存在
- 忘记密码页显示“联系管理员重置密码”
- 顶部铃铛弹窗展示通知公告
- 顶部齿轮入口进入账号维护
- 退出通过确认弹窗完成
- 新增数据源、批量导入、规则配置、导出报表、生成质检报告使用弹窗

## 界面风格

整体参考用户提供截图：

- 橙色顶部导航
- 浅灰页面背景
- 白色圆角内容区
- 橙色主按钮
- 轻量阴影
- 卡片和弹窗为主
- 中文界面、中文提示、中文日志、中文样例数据

登录和注册页不依赖图片资产，使用浅色/渐变背景加橙蓝数据感装饰。图标使用本地轻量 SVG/CSS，不依赖外网图标服务。Chart.js 尽量本地化，不依赖外网 CDN。

## 数据库设计

启动时自动创建 `data` 目录和 SQLite 数据库，自动建表、补字段、初始化数据。

沿用任务书基础表：

- `sys_user`
- `data_source`
- `import_batch`
- `import_record`
- `fusion_record`
- `check_result`
- `stat_report`
- `sys_log`

新增和扩展：

- `cleaned_record`：保存每次清洗后的行数据
- `clean_rule_config`：保存最近一次清洗规则配置
- `import_batch.source_id`：可空，关联 Excel/CSV 数据源

初始化数据：

- 管理员账号 `admin / 123456`
- 演示数据源
- 内置通知公告
- 最近一次清洗规则默认配置
- CSV、XLSX、XLS 样例文件

密码明文保存，符合本地演示版要求。

## 数据生命周期

导入数据：

- 上传原文件保存到 `data/uploads`
- 上传后先解析并预览，不立即入库
- 用户确认后写入 `import_batch` 和 `import_record`
- 每行数据以 JSON 存入 `import_record.data_json`
- 不为每个上传文件动态建表

清洗数据：

- 每次清洗新增一条 `fusion_record`
- 每次清洗后的行数据写入 `cleaned_record`
- 同一批次可多次清洗
- 默认统计和质检使用最新清洗结果
- 若不存在清洗结果，则使用原始导入数据

质检数据：

- 每次一键全检新增 `check_result`
- 默认展示最新质检结果
- 历史质检报告保留

导出数据：

- 每次导出新增文件和 `stat_report` 记录
- 不自动覆盖或删除旧文件
- 页面提供最近导出记录和下载链接

日志数据：

- 写入 `sys_log`
- 数据汇聚页展示最近操作日志
- 记录登录、注册、退出、新增数据源、刷新状态、上传预览、确认导入、执行清洗、执行质检、导出报告、重置密码

## 账号模块

默认管理员：

- 用户名：`admin`
- 密码：`123456`

功能：

- 登录校验 SQLite 用户表
- 注册校验用户名唯一、密码一致、同意条款
- 记住密码使用 Cookie 保存用户名和密码，下次进入登录页自动填充，不自动登录
- 忘记密码页提示联系管理员重置密码
- 顶部齿轮进入账号维护
- 管理员可查看用户并重置密码
- 普通用户只查看当前账号信息或收到无权限提示
- 退出前弹出确认提示

## 数据汇聚

页面职责：

- 展示数据源状态
- 展示数据概览指标
- 展示最近接入日志
- 新增演示数据源
- 批量导入文件

数据源：

- 支持类型：MySQL、API、Excel、CSV、Oracle
- MySQL/API/Oracle 只保存连接信息，不做真实连接
- Excel/CSV 可在导入时选择，也允许不选
- 刷新状态只更新演示状态，不进行真实连通检查

文件导入：

- 支持 `.xlsx`、`.xls`、CSV
- 文件最大 100MB
- Excel 默认读取第一个工作表
- 第一行作为表头，第二行开始为数据
- 空表头自动命名为 `字段1`、`字段2`
- CSV 兼容 UTF-8 和 GBK
- 上传后预览前 20 行，确认后入库全部数据
- 导入时选择业务分类，默认“系统”，可选用户、订单、产品、运营、客服、系统

## 融合处理

页面职责：

- 选择导入批次
- 启动清洗
- 配置最近一次清洗规则
- 展示处理进度
- 展示处理结果预览

规则配置为真实生效的固定规则开关，保存最近一次配置：

- 空值处理
- 首尾空格清理
- 整行去重
- 手机号标准化
- 邮箱标准化
- 日期标准化

不实现用户自定义正则、SQL、表达式或脚本规则。不实现字段映射交互。

清洗规则：

- 空值统一为空字符串，不删除整行
- 重复数据按整行 JSON 内容完全一致判断，保留第一条
- 手机号字段按字段名自动识别：`phone`、`mobile`、`手机号`、`电话`
- 邮箱字段按字段名自动识别：`email`、`邮箱`、`mail`
- 日期字段按字段名自动识别：`date`、`time`、`日期`、`时间`
- 日期统一输出 `yyyy-MM-dd`
- 标准化失败时保留原值，计入异常

## 统计分析

页面职责：

- 展示指标卡
- 展示 Chart.js 折线图和环形图
- 导出统计和数据报表

默认数据口径：

- 默认选最新导入批次
- 优先使用最新清洗结果
- 无清洗结果时使用原始导入数据

指标：

- 总数据量
- 字段数
- 增量数据量
- 空值率
- 重复数
- 质量检验通过率

图表：

- 折线图展示最近 7 个导入批次的记录数变化
- 环形图展示数据源类型占比
- 环形图展示业务分类占比

导出：

- 按批次导出原始数据
- 按批次导出清洗后数据
- 导出统计结果
- 格式支持 Excel 和 CSV

## 核验核查

页面职责：

- 一键全检
- 展示质检概览
- 展示异常列表
- 生成质检报告

检查范围：

- 完整性检查：空值数量和空值率
- 一致性检查：明显格式不一致的数据
- 重复性检查：重复记录数量
- 基础准确性检查：手机号、邮箱、日期字段格式

质检结果展示：

- 检查项
- 检查说明
- 是否通过
- 问题数量
- 通过率
- 生成时间

质检报告：

- 只支持 Excel 和 CSV
- 不支持 PDF
- 写入 `stat_report`

## 台账查阅

页面职责：

- 按业务分类展示台账卡片
- 支持搜索
- 支持分类详情弹窗
- 支持下载导出文件

业务分类：

- 用户
- 订单
- 产品
- 运营
- 客服
- 系统

每张卡展示：

- 导入批次数
- 记录数
- 最近导入时间

追踪详情展示该分类下的导入批次列表，可按关键字搜索。

## 后端结构

包结构：

```text
com.dataprocess.platform
├── DataFusionPlatformApplication.java
├── config
├── controller
├── service
├── repository
├── model
├── dto
├── util
└── init
```

保留旧源码文档中有意义的类名和业务方法名，不保留无意义占位方法。

核心类：

- `DataFusionPlatformApplication`：Spring Boot 启动类
- `DuoyuanRongheChuli`：核心业务门面类

`DuoyuanRongheChuli` 保留方法名：

- `chuliExcelDaoru`
- `zhixingQingxi`
- `zhixingTongji`
- `zhixingJianyan`
- `chaxunTaizhang`
- `daochuBaobao`

具体实现拆分到：

- `AuthService`
- `UserService`
- `DataSourceService`
- `FileImportService`
- `DataPreviewService`
- `DataCleanService`
- `FusionService`
- `StatisticsService`
- `QualityCheckService`
- `ReportExportService`
- `LedgerService`
- `NoticeService`
- `SystemLogService`

Controller：

- `AuthController`
- `AggregationController`
- `FusionController`
- `StatisticsController`
- `QualityCheckController`
- `LedgerController`
- `AccountController`
- `NoticeController`
- `ReportController`

Repository 使用 Spring JDBC 或轻量 DAO 封装 SQLite 访问。

## 验收

完成后执行本地验收：

- 生成 CSV、XLSX、XLS 三种样例文件
- `mvn package` 构建成功
- Spring Boot 启动成功
- 访问 `http://localhost:8080` 进入登录页
- 使用 `admin / 123456` 登录成功
- 新用户可注册
- 记住密码可自动填充
- 新增数据源可保存
- CSV/XLSX/XLS 可上传、预览、确认入库
- 数据库产生导入批次和导入行数据
- 清洗规则配置可保存并影响清洗结果
- 清洗产生 `fusion_record` 和 `cleaned_record`
- 统计页展示指标和图表
- 质检页可一键全检并保存结果
- 可生成 Excel/CSV 质检报告
- 可导出原始数据、清洗后数据和统计结果
- 台账页可按 6 类业务分类查看批次
- 顶部通知弹窗可展示内置公告
- 管理员可重置用户密码
- 退出确认弹窗可用
- `data/data_fusion.db`、`data/uploads`、`data/exports`、`data/samples` 正常生成

## 不包含范围

V1.0 不实现：

- 真实 MySQL 数据源连接
- 真实 API 调用
- Oracle 真实连接
- Vue 或 React 独立前端
- Docker
- PDF 导出
- 字段映射交互
- 用户自定义正则、SQL、表达式或脚本规则
- 短信、邮箱、第三方登录
- 通知公告后台管理
- 定时任务
- 生产级权限体系
