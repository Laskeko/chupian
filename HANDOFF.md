# 出片搭子 ChuPian — 项目交接文档

> 本文档是给「下一个接手会话」的完整上下文快照。先读它，再看 `docs/PRD.md` 和 `.workbuddy/memory/`。
> 生成时间：2026-11-11，对应代码提交 `d1d7e47`。

## 项目一句话

开源 Android AI 摄影搭子：**抬起手机，就知怎么拍**。GitHub：https://github.com/Laskeko/chupian（public，Apache-2.0，Laskeko 个人账号）。

## 产品定位与核心共识（已拍死，别推翻）

1. **私有素材库是核心**：用户手机相册收藏夹 = 机位索引 + 被拍者偏好画像。不爬数据、不建公共库、零合规负担。
2. **不做模型训练**：个性化 = 结构化 profile JSON + 上下文工程（拼 prompt）。“训练”二字出现在任何方案里都要打回。
3. **数据不出设备**：打标、索引、画像全端侧；云端 VLM 只收一帧画面且用户可全局关闭。Manifest 当前刻意没有 INTERNET 权限，接 VLM 时再申请。
4. **不锁定“拍对象”**：被拍者任意（伴侣/闺蜜/孩子/父母/自己），每人一份独立偏好画像，切换即用。
5. **Android only**：核心成员无 iOS 设备。Kotlin + Jetpack Compose + CameraX + ML Kit。核心逻辑与数据格式保持平台无关，给社区承接 iOS 留口子。
6. **推荐分层**：场景知识卡（docs/scene-cards.md，初始 20 张）给确定性拍法建议；VLM 只做场景识别 + 个性化组合，不编造摄影知识。
7. **VLM 选型**：DeepSeek 纯文本无视觉，不适用。默认 GLM-4V-Flash（免费档），可插拔豆包/Qwen-VL。
8. **分发**：APK / GitHub Releases，不上架。

## 已完成

| 提交 | 内容 |
|---|---|
| `6578310` | 立项：README + PRD v0.1 + 场景知识卡 20 张 |
| `4bcec6a` | 平台切 Android only |
| `d1d7e47` | **M2 骨架**：CameraX 取景 + ML Kit 骨架实时叠加 + 权限流程 + 状态芯片 +「怎么拍」占位按钮 |

## 当前代码结构

```
app/src/main/kotlin/com/chupian/app/
├── MainActivity.kt            # 入口，恒定暗色主题
├── ui/CameraScreen.kt         # 权限流程 + 取景绑定 + 状态芯片 + 占位按钮
├── pose/PoseImageAnalyzer.kt  # ImageAnalysis 流帧 → ML Kit STREAM_MODE → 归一化地标；SKELETON_CONNECTIONS 连线表在此
└── overlay/PoseOverlay.kt     # Canvas 骨架绘制；FILL_CENTER center-crop 坐标映射（取景叠加最易翻车点，已按此模式实现）
```

## 下一步（按优先级）

1. **真机验证 M2 骨架**（用户侧动作）：Android Studio 打开工程同步 → 真机跑 → 确认骨架稳定跟人走。⚠️ 已知疑点：少数机型横竖转换可能镜像，若骨架偏转发截图修正。
2. **F1 素材库**（开发侧，M2 主块）：Photo Picker 导入 → 离线批量打标（景别/构图/姿势/场景/光线）→ 端侧 CLIP embedding（TFLite/MobileCLIP）建向量索引 → EXIF 定位读取。
3. **F2 点位匹配**：取景帧 1~2s/次 embedding 检索 + GPS 200m 粗筛 + 参考图叠加 + 一键确认两段式。
4. **F3 场景推荐**：接通 VLM，「怎么拍」按钮落地。

## 环境备忘

- 开发机 macOS（用户名 laskeko），**当前无 Android SDK / JDK / Gradle**——骨架代码未经本地编译验证，依赖 Android Studio 首次同步。
- git 推送：gh 活动账号为 **Laskeko**，已 `gh auth setup-git`。若 push 报 403 `Permission denied to FofaInfo`，是凭证串到了另一个账号，重跑 `gh auth switch --user Laskeko && gh auth setup-git`。
- 项目记忆在 `.workbuddy/memory/`（已加 .gitignore，不进仓库）。

## 用户协作风格

直接果断、拍板即推进（"开工吧""删吧"级别指令），期望无人值守式默认推进 + 主动声明假设；反感"导师腔"说教；文档偏好表格化、紧凑、少官话。
