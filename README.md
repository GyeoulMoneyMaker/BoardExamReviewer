# 🎓 Board Exam Reviewer: Secure AI Study Hub

![Banner](https://img.shields.io/badge/Status-Production--Ready-success?style=for-the-badge)
![AI](https://img.shields.io/badge/Powered%20By-Google%20Gemini-blue?style=for-the-badge)
![Security](https://img.shields.io/badge/Security-Multi--User%20Isolated-red?style=for-the-badge)

**Board Exam Reviewer** is a state-of-the-art Android application designed to help students ace their board exams using artificial intelligence, focus-driven timers, and secure multi-user management.

---

## 🌟 Key Features

### 🤖 AI-Powered Study Engine
*   **Instant Summaries**: Upload PDF, PPT, or Word documents and generate comprehensive board-exam-ready reviewers in seconds.
*   **Smart Quiz Generator**: Generate dynamic 5-question multiple-choice quizzes directly from your uploaded files or your saved reviewer summaries.
*   **Multi-Source Selection**: Choose exactly which document or reviewer you want the AI to analyze.

### 🛡️ Professional Privacy & Security
*   **Multi-User Profiles**: Support for multiple students on a single device.
*   **Password Protection**: Each profile is gated by a password to ensure your study data stays yours.
*   **Data Isolation**: Reviewers, Quizzes, and Files are strictly filtered by User ID. Your progress is invisible to other users.

### ⏲️ Focus & Immersion (Pomodoro)
*   **Background Timer**: A robust foreground service that keeps your timer ticking even if you leave the app.
*   **Ambient Soundscapes**: Built-in audio engine featuring **Rain, Waves, and Forest** sounds to help you enter the "Flow State."
*   **User-Specific Persistance**: Your timer automatically pauses when you switch profiles and resumes exactly where you left off when you return.

---

## 🛠️ Tech Stack
*   **Language**: Kotlin
*   **Database**: Room Persistence (SQLite) for offline data.
*   **Networking**: Retrofit & OKHttp for secure AI communication.
*   **AI Engine**: Google Gemini 1.5/2.5 Flash.
*   **UI Architecture**: MVVM with ViewBinding and Fragment Navigation.

---

## 🚀 Getting Started

### 1. Prerequisites
*   Android Studio Ladybug or later.
*   JDK 17.
*   A Google Gemini API Key.

### 2. Installation
1.  Clone the repository:
    ```bash
    git clone https://github.com/GyeoulMoneyMaker/BoardExamReviewer.git
    ```
2.  Open in Android Studio.
3.  Add your API Key to `local.properties`:
    ```properties
    GEMINI_API_KEY=your_key_here
    ```
4.  Build and Run!

---

## 📸 Screenshots
*(Add your screenshots here to show off the UI!)*

---

## 🤝 Contributing
Contributions are welcome! If you have ideas for new focus sounds or AI features, feel free to open a Pull Request.

---

## 📜 License
This project is for educational purposes. Built with ❤️ by **Gyeoul**.
