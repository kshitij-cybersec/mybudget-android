# My Budget 💰

A completely offline, privacy-first Android app for tracking your monthly budget — no accounts, no cloud, no data leaving your device.

> Built with a security-first mindset: your financial data stays on your phone, always.

\---

## 📱 Screenshots

<!-- Add your screenshots here -->

<!-- Tip: put 3-4 side-by-side using HTML img tags -->

<!--
<p align="center">
  <img src="screenshots/dashboard.png" width="23%" />
  <img src="screenshots/add-transaction.png" width="23%" />
  <img src="screenshots/categories.png" width="23%" />
  <img src="screenshots/statement-import.png" width="23%" />
</p>

*Screenshots coming soon — see* [*Features*](#-features) *for full capability breakdown.*

\---

## ✨ Features

### 🏦 Bank Statement Parsing

* Import your bank statement PDF/CSV and the app automatically extracts debits and credits
* No manual entry needed for bank transactions
* Parsing happens entirely on-device — your statement never touches a server

### ✍️ Manual Cash Flow Entry

* Log cash transactions manually (salary, rent, petty cash, etc.)
* Full control over every entry — edit or delete anytime

### 🗂️ Category Management

* Pre-built categories: Food, Transport, Utilities, Entertainment, Healthcare, Shopping, and more
* Create your own custom categories to match your lifestyle
* Assign any transaction to any category in seconds

### 💱 Multi-Currency Support

* Supports 4 currencies: **INR ₹ · USD $ · GBP £ · EUR €**
* Switch your display currency from Settings at any time

### 🔒 100% Offline \& Private

* Zero internet permissions — the app literally cannot send your data anywhere
* No account required, no sign-up, no tracking
* All data stored locally in an encrypted SQLite database on your device

\---

## 🛠️ Tech Stack

|Layer|Technology|
|-|-|
|Language|Kotlin|
|UI|Jetpack Compose + Material 3|
|Database|Room (SQLite)|
|Architecture|MVVM|
|PDF Parsing|PDFBox Android (`com.tom-roush:pdfbox-android` 2.0.27.0)|
|OCR Fallback|Google ML Kit Text Recognition 16.0.0|
|CSV Parsing|Kotlin standard library (no external dependency)|
|Min SDK|Android 8.0 (API 26)|

\---

## 🚀 Getting Started

### Prerequisites

* Android Studio Hedgehog or later
* Android SDK 26+
* Kotlin 1.9+
* Compose BOM compatible with your Studio version

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/kshitij-cybersec/my-budget.git

# 2. Open in Android Studio
File → Open → select the cloned folder

# 3. Sync Gradle
Android Studio will prompt you — click "Sync Now"

# 4. Run on device or emulator
Click the green Run button or press Shift + F10
```

### Build a signed APK

```
Build → Generate Signed Bundle / APK → APK → follow the prompts
```

\---

## 📁 Project Structure

```
my-budget/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kshitij/mybudget/
│   │   │   │   ├── data/          # Room database, DAOs, entities
│   │   │   │   ├── ui/            # Fragments, Activities, ViewModels
│   │   │   │   ├── parser/        # Bank statement parsing logic
│   │   │   │   ├── utils/         # Currency formatting, date helpers
│   │   │   │   └── model/         # Data classes
│   │   │   └── res/               # Layouts, drawables, strings
│   └── build.gradle
├── screenshots/                   # App screenshots for README
├── .gitignore
├── README.md
└── build.gradle
```

\---

## 🔐 Privacy \& Security Design

This app was built with data privacy as a core constraint, not an afterthought:

* **No INTERNET permission** declared in `AndroidManifest.xml` — verified by Android OS
* **Local-only storage** using Room (SQLite) with no sync functionality
* **No analytics, no crash reporting SDK, no ads** — zero third-party data collection
* Bank statement parsing uses PDFBox Android for text-based PDFs, with Google ML Kit Text Recognition as an OCR fallback for scanned/image-based statements — all on-device, no cloud API calls
* All user data is removed on app uninstall

This makes My Budget safe for storing real financial data without worrying about breaches or leaks.

\---

## 🗺️ Roadmap

### ✅ Implemented

* \[x] Fingerprint / PIN lock screen
* \[x] Monthly budget goal setting with alerts
* \[x] Charts and spending trends (monthly comparison)
* \[x] Dark mode
* \[x] Widget for home screen balance summary

### 🔧 Coming Soon

* \[ ] Export data as CSV or PDF report
* \[ ] Support for additional currencies beyond INR, USD, GBP, EUR

\---

## 🤝 Contributing

Contributions are welcome! Here's how:

```bash
# 1. Fork the repo
# 2. Create a feature branch
git checkout -b feature/your-feature-name

# 3. Make your changes and commit
git commit -m "Add: brief description of what you added"

# 4. Push and open a Pull Request
git push origin feature/your-feature-name
```

Please open an issue first for major changes so we can discuss the approach.

\---

## 🐛 Reporting Bugs

Found a bug? Open an issue with:

* Your Android version
* Steps to reproduce
* What you expected vs what happened
* Screenshot if relevant

\---

## 📄 License

```
MIT License

Copyright (c) 2026 Kshitij

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

\---

## 👤 Author

**Kshitij**

* GitHub: [@kshitij-cybersec](https://github.com/kshitij-cybersec)

\---

<p align="center">Built with ❤️ in India \&nbsp;|\&nbsp; 100% offline \&nbsp;|\&nbsp; Your data stays yours</p>

