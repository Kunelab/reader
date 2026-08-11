# Kune Reader - RSVP EPUB 速读器

[English](README.md) · [Français](README.fr.md) · [Español](README.es.md) · **简体中文**

一款快速、专注的 Android EPUB 阅读器，采用 **RSVP**（Rapid Serial Visual Presentation，快速序列视觉呈现）方式，并将 ORP（Optimal Recognition Point，最佳识别点）字符加粗居中显示。

## 功能

- **RSVP 阅读**：按你设定的速度逐词闪现
- **ORP 高亮**：最佳识别点字符以红色加粗显示，并在屏幕上居中对齐
- **上一个词**：上一个词显示在顶部，便于回看
- **上下文语句**：下方显示最近 N 个词（可配置），提供段落上下文
- **速度可调**：每分钟 50–1500 词，可用滑块或 +/- 按钮调整
- **标点停顿**：可分别配置以下停顿时长（毫秒）：
  - 逗号、分号、冒号
  - 句号、感叹号、问号
  - 段落间断
- **深色主题**：护眼的深色界面
- **EPUB 支持**：通过 Android 文件选择器打开标准 `.epub` 文件
- **多语言界面**：英语、法语、西班牙语和简体中文

## 构建

1. 用 Android Studio（Hedgehog 或更新版本）打开项目
2. 同步 Gradle
3. 在设备或模拟器上运行（API 26 及以上）

## 架构

```
app/
├── epub/           # EPUB 解析（Jsoup + XmlPullParser）
├── model/          # 数据类（BookData、RsvpWord 等）
├── rsvp/           # RSVP 播放引擎（基于协程）
├── settings/       # DataStore 偏好设置
├── ui/
│   ├── components/ # RsvpWordDisplay
│   ├── screens/    # HomeScreen、ReaderScreen、SettingsScreen
│   └── theme/      # 深色主题配色
└── viewmodel/      # ReaderViewModel
```

## 操作

- **点击屏幕**：播放 / 暂停
- **+/- 按钮**：每次调整 25 词/分
- **⏪ / ⏩**：后退或前进 10 个词
- **设置**：全部节奏与显示选项

## 语言

界面已翻译为英语、法语、西班牙语和简体中文。应用默认跟随系统语言；在 Android 13 及以上版本，也可在 *设置 → 应用 → Kune Reader → 语言* 中为其单独设置语言。

## 许可证

本项目采用 Apache License 2.0 授权。完整文本见 [LICENSE](LICENSE)。

[LICENSE.zh-CN](LICENSE.zh-CN) 提供非官方中文译本，仅供参考；**以英文版本为准**。

```
Copyright 2026 Maxime Pinard

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
