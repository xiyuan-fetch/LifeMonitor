# 生命监测系统 (Dead Man Switch)

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**生命监测系统** 是一款基于 Android 平台的“死亡开关”应用。它旨在通过周期性的用户手动“签到”来确认用户的安全状态。如果用户在设定的时间（默认 24 小时）内未进行签到，系统将自动触发预设的紧急逻辑（如向指定联系人发送求助短信）。

## ✨ 核心功能

* **日历式签到**：直观的 15 日打卡网格，实时掌握自己的状态记录。
* **自动化监测**：采用 `AlarmManager` 精准定时，确保 24 小时循环监测。
* **紧急求助**：超时未签到时，系统将通过后台自动发送短信给预设的紧急联系人。
* **个性化定制**：支持自定义磨砂玻璃背景壁纸及模糊度调节，适配 Material You 配色（Android 12+）。
* **自启保护**：支持开机自动恢复监测任务，防止因手机重启导致监测中断。

## 📸 界面预览

* **主页**：中心化的圆环签到按钮及下方的打卡统计网格。
* **设置**：配置紧急联系人号码、短信内容及界面 UI 效果。

## 🛠️ 权限说明

为了保证核心功能的正常运行，本应用需要以下权限：

1.  **发送短信 (`SEND_SMS`)**：核心权限，用于在监测超时后发出求助信息。
2.  **精准闹钟 (`SCHEDULE_EXACT_ALARM`)**：用于实现严格的 24 小时倒计时。
3.  **开机自启 (`RECEIVE_BOOT_COMPLETED`)**：确保设备重启后监测服务能自动重新挂载。
4.  **读取存储 (`READ_MEDIA_IMAGES`)**：用于用户自定义主页背景图片。

## 🚀 快速开始

### 开发环境
* Android Studio Jellyfish 或更高版本。
* Compile SDK: 34+
* Min SDK: 26 (Android 8.0)

### 编译运行
1.  克隆本仓库：
    ```bash
    git clone [https://github.com/Neofetch/DeadManSwitch.git](https://github.com/Neofetch/DeadManSwitch.git)
    ```
2.  在 Android Studio 中打开项目。
3.  连接手机并确保已开启“开发者选项”及“USB 调试”。
4.  点击 **Run** 按钮进行安装。

## ⚠️ 免责声明

* 本程序属于开源项目，仅供学习交流和极端情况下的辅助参考。
* **请勿依赖本程序作为唯一的生命安全保障。** 由于安卓系统的后台限制、电池优化策略或短信网关故障，短信发送可能存在失败风险。
* 开发者（Neofetch）不对因使用本程序导致的任何直接或间接后果负责。

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源。

---
**联系作者**: 2179887520@qq.com
