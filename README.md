# 🎓 VIT Class Reminder (VIT Pune)

> **An automated, offline-first timetable companion, class reminder, and task tracker for students of Vishwakarma Institute of Technology (VIT Pune).**

[![Platform](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Widgets](https://img.shields.io/badge/Widgets-Glance-00C853)](https://developer.android.com/jetpack/compose/glance)
[![Built with AI](https://img.shields.io/badge/Engineered_with-AI_Assistance-FF6F00)](https://github.com/dark-ous/VitReminder)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 💡 Why I Made This App

Let’s be honest: finding your classroom or checking whether you have a lecture, lab, or tutorial at VIT Pune should not take 5 minutes of zooming into a messy PDF.

Opening the official website, navigating clunky menus, downloading the latest revised PDF, scrolling through rows of timetable grids just to check **which room your batch (B1/B2/B3) has to run to** is an everyday hassle. 

I built **VIT Class Reminder** to solve this problem once and for all:
- Select your **Campus $\rightarrow$ Branch $\rightarrow$ Division $\rightarrow$ Batch** once.
- Have your exact timetable, room numbers, break countdowns, lecture reminders, and home screen widgets always ready — **100% offline, instant, and battery-efficient**.

---

## 🤖 Built Mostly with AI

This project was built and architected **predominantly with AI engineering assistance**. Rather than spending months wrestling with boilerplate code, AI pair-programming was leveraged to implement complex Android subsystems in record time:
- Custom PDF timetable parser extracting multi-page session grids.
- Jetpack Glance app widget system with reactive DataStore state.
- Precision `AlarmManager` scheduling with Android 12+ exact alarm compliance.
- Interactive class-bound note tagging and checklist systems.

---

## 🛡️ "Google Play Protect / Scan App" Prompt — Why Does It Happen?

When you install this APK, you might see a Google Play Protect prompt saying:
> *"Scan app with Play Protect?"* or *"Unrecognized developer / Blocked by Play Protect"*.

### Why this happens:
1. **Brand New Independent App**: This app was recently compiled and is distributed as an independent open-source APK rather than through the Google Play Store.
2. **Cloud Reputation**: Google Play Protect automatically flags any newly created developer signing certificate that hasn't yet been installed on tens of thousands of devices.
3. **100% Safe & Clean**: 
   - The app is **100% open source** — all code is right here in this repository.
   - It contains **zero ads, zero trackers, and zero telemetry**.
   - It is cryptographically signed with RSA 4096-bit v2 and v3 signatures (`CN=VIT Reminder, OU=VIT Pune, O=Vishwakarma Institute of Technology`).

### How to Install:
1. When the prompt appears, tap **"More details"** (small dropdown arrow).
2. Tap **"Install anyway"**.
3. *(If prompted to "Send app for scanning", you can either scan it or tap "Don't send" — both will work cleanly).*

---

## ✨ Features

### 1. 🏛️ Complete Campus Directory (Bibwewadi & Kondhwa)
- Direct support for all **45+ branches and divisions**:
  - **Bibwewadi Campus**: CS (A–L), AI (A–F), AIML (A–F), IT (A–F).
  - **Kondhwa Campus**: AIDS (A–F), CSSE (A–C), CSDS (A–C), CSCBI (A–C), ET (A–F), IC (A–C), CV (A–C), ME (A–B).
- 5-step picker: **Campus $\rightarrow$ Branch $\rightarrow$ Section $\rightarrow$ Batch $\rightarrow$ Semester**.
- Fallback support to upload custom PDFs from device storage or fetch via direct link.

### 2. 📅 Today's Live Schedule & Weekly Grid
- **Hero Card**: Highlights current ongoing class with a live duration progress bar and remaining countdown.
- **Room Numbers**: Prominently displays classroom numbers (e.g. `Room 1203`, `Rm 4108`) so you always know where to go.
- **Break Alerts**: Visual indicators and notifications for short breaks and lunch periods.
- **Auto Next-Day Preview**: Once today's lectures end (or on weekends), the schedule automatically switches to preview tomorrow's first lecture!
- **Interactive Weekly Grid**: Visual timetable matrix for all 6 days (Mon–Sat) filtered to your batch.

### 3. 📝 Class-Bound Notes & Interactive Checklist
- **Context-Aware Notes**: Jot down notes during a lecture or lab, and the app automatically tags it with your ongoing class, room, and time slot.
- **Interactive Checklist**: Check off completed tasks with satisfying strikethrough styling (`TextDecoration.LineThrough`).
- **Filters**: Filter notes by `All`, `Active Class`, `Pending`, or `Completed`.

### 4. 📱 Two Home Screen Glance Widgets
- **VIT Class Schedule Widget**: Displays your current or upcoming class, room number, countdown, and batch on your home screen. Resizable up to 4x3 cells.
- **VIT Tasks & Notes Widget**: Displays your pending class reminders and lets you check off completed items directly from your home screen without opening the app!

### 5. 🔔 Automated Reminders & Background Sync
- Uses Android `AlarmManager` exact alarms to notify you 10 minutes before each class starts.
- Persistent notification bar showing the ongoing class and countdown.
- Auto-reschedules alarms on phone reboot via `BootReceiver`.

---

## 📲 Download & Installation

1. Download the latest **[`VIT_Reminder.apk`](https://github.com/dark-ous/VitReminder/releases)** from the [Releases](https://github.com/dark-ous/VitReminder/releases) tab.
2. Tap on the downloaded APK on your Android phone to install.
3. If prompted by Play Protect, tap **"More details" $\rightarrow$ "Install anyway"**.
4. Open the app, select your Campus, Branch, Division, and Batch.
5. Add the home screen widgets for instant access!

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9.x
- **Framework**: Jetpack Compose (Material 3)
- **Widgets**: Jetpack Glance 1.1.0
- **Data Persistence**: Jetpack Preferences DataStore & Gson
- **Background Processing**: AndroidX WorkManager & AlarmManager
- **PDF Extraction**: PDFBox Android
- **Signing**: RSA 4096-bit (APK Signature Scheme v2 & v3)

---

## 🤝 Contributing

Contributions, feedback, and suggestions from fellow VITians and open-source developers are welcome!

### Potential Ideas & Roadmap:
- [ ] **Attendance Tracker**: Track 75% attendance criteria per course with a 1-tap attend/bunk counter.
- [ ] **Campus Map & Room Navigator**: Quick reference map for buildings, labs, and classroom numbers in Bibwewadi & Kondhwa.
- [ ] **Exam Schedule Mode**: Toggle between regular timetable and In-Sem / End-Sem exam timetables.
- [ ] **Material You Dynamic Theming**: Color palettes based on your wallpaper (Android 12+).
- [ ] **Dark / Light Mode Toggle**: Customizable OLED Black and Light themes.

To contribute:
1. Fork the Project (`https://github.com/dark-ous/VitReminder/fork`)
2. Create your Feature Branch (`git checkout -b feature/CoolFeature`)
3. Commit your Changes (`git commit -m 'Add some CoolFeature'`)
4. Push to the Branch (`git push origin feature/CoolFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

*Made by a VIT Pune student, for VIT Pune students. 🎓*
