# Follow-ups Index

这组文件是仓库里**唯一权威**的遗留问题跟进入口，用来替代散落在根目录的临时 TODO 文档。

## 如何使用

- `active.md`
  - 当前 1 到 2 个迭代内要继续收口的问题、真实阻塞项、以及已经上线但还要继续盯的事项。
- `release-watch.md`
  - 与生产环境、提审、审核期、发布窗口强相关的有界跟进项。
- `tech-debt.md`
  - 中长期技术债与架构改善项，不要求立刻做，但需要持续保持与代码现状一致。
- `v2-roadmap.md`
  - 1.0.3 审核通过后的 V2.0 迭代计划，按"直接接入 / 补齐开发 / 全新开发"三批排列优先级。
- `android-plan.md`
  - Android 版本开发计划，功能对齐 iOS 1.0.3，分 4 个阶段：工程基础、平台能力、IAP、V2 随版首发。

## 维护规则

- 新发现的 deferred item，先判断属于哪一类，再只写入一个权威文件。
- 已完成项不要长期堆积在 active 区，完成后：
  - 若仍需短期观察，移到 `release-watch.md`
  - 若只是历史背景，不再保留
- 根目录的 [TASKS.md](/Users/zhaoqiang/Documents/Project/what-to-eat/TASKS.md) 与 [TECH_DEBT.md](/Users/zhaoqiang/Documents/Project/what-to-eat/TECH_DEBT.md) 仅保留为兼容入口，不再作为独立事实来源。

## 当前状态快照（2026-06-14）

- App Store：`2.0.5 (29)` 已于 2026-06-14 审核通过并上线，为当前线上版本。
- 历史上线版本：2.0.1 (4)、2.0.2 (10)、2.0.3 (27)、2.0.4 (28)、2.0.5 (29)，详见 [release-watch.md](/Users/zhaoqiang/Documents/Project/what-to-eat/memory/follow-ups/release-watch.md)。
- 2.0.5 内容：菜谱朗读多音字优化、库存参与推荐改显式勾选、饭点 + 体验会员到期本地通知（Phase 1）、多关键词推荐丢食材修复。
- 平台能力（均已上线）：Apple ID 登录、7 天体验会员、购物清单图片分享、Pro 食材库存识别、本地通知 Phase 1。
- 线上 ECS 与真机 `remote` 可用，核心推荐链路正常；线上静态页（`/`、`/v2/`、`/support`、`/privacy`）保持 200。
- 设备范围 iPhone-only；Android 仍停留在早期版本（versionName `2.0.1`），未纳入当前发布节奏。
- 下一步：下一版方向为推送促活 Phase 2 · APNs 远程推送（详见 [active.md](/Users/zhaoqiang/Documents/Project/what-to-eat/memory/follow-ups/active.md)「下个版本」）；短信登录、视频、iPad、Android 与用户侧搜索仍不纳入本轮 iOS 节奏。
