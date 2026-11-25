# 🎮 Kotlin EsportSphere — Esports Discovery & Live Updates

<p align="center">
  <b>A native Android platform for discovering the esports ecosystem.</b>
  <br/>
  Explore players, teams, tournaments, live broadcasts, and esports news — all in one place.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Java-JVM-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Gradle-Build%20System-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
  <img src="https://img.shields.io/badge/Supabase-Backend-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
</p>

---

## 📖 About

**EsportSphere** is a native Android esports discovery and information platform designed to bring the competitive gaming ecosystem into one organized, accessible, and modern mobile experience.

The esports ecosystem is often fragmented across social media, streaming platforms, announcements, tournament pages, and community sources. EsportSphere aims to simplify this experience by providing a structured destination where users can discover esports personalities, explore teams, follow tournaments, stay informed about live broadcasts, and catch up on the latest news.

With a particular focus on **Free Fire esports and the Indian/South Asian competitive scene**, the platform is built around esports discovery, information, and staying connected with the competitive scene.

> **Discover the players. Follow the competition. Stay in the esports loop.**

## ✨ Features

- **🏠 Esports Discovery** — A centralized experience for exploring the competitive esports ecosystem.
- **👤 Player Directory** — Discover esports players and learn more about competitive personalities.
- **🛡️ Teams Directory** — Browse esports teams and explore the competitive landscape.
- **🏆 Tournament Discovery** — Stay informed about esports tournaments and competitive events.
- **🔴 Live Updates & Broadcasts** — Discover live esports broadcasts and relevant live activity.
- **📰 Esports News** — Keep up with announcements, updates, and news from the esports scene.
- **🔐 Google Authentication** — Convenient sign-in experience using Google authentication.
- **☁️ Cloud-Connected Backend** — Powered by Supabase and cloud-hosted PostgreSQL for application data.
- **🔔 Push Notifications** — Firebase Cloud Messaging (FCM) integration for push notifications and updates.
- **📱 Native Android Experience** — Built specifically for Android using Kotlin.

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Primary Android application development |
| **Java** | JVM ecosystem and Java interoperability |
| **Android SDK** | Native Android platform |
| **Gradle (Kotlin DSL)** | Build configuration and dependency management |
| **Supabase** | Cloud backend platform |
| **PostgreSQL** | Relational database |
| **Supabase Auth** | Authentication and identity management |
| **Google Sign-In** | Google authentication |
| **Firebase Cloud Messaging (FCM)** | Push notifications |
| **Git & GitHub** | Version control and project hosting |

## 🏗️ Project Structure

```text
Kotlin-EsportSphere/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/example/helloandroid/
│   │       │   │
│   │       │   ├── MainActivity.kt
│   │       │   ├── NewsSection.kt
│   │       │   ├── TeamsDirectory.kt
│   │       │   ├── TournamentDirectory.kt
│   │       │   ├── AppNotifications.kt
│   │       │   ├── EsportSphereMessagingService.kt
│   │       │   │
│   │       │   └── data/
│   │       │       ├── DeviceTokenRepository.kt
│   │       │       ├── EventsRepository.kt
│   │       │       ├── HomeCarouselRepository.kt
│   │       │       ├── NewsRepository.kt
│   │       │       ├── PlayersRepository.kt
│   │       │       └── TeamsRepository.kt
│   │       │
│   │       └── res/
│   │
│   ├── build.gradle.kts
│   └── google-services.json
│
├── supabase/
│   ├── functions/
│   │   └── delete-account/
│   │       └── index.ts
│   └── migrations/
│       └── 20260918_profiles_rls.sql
│
├── gradle/
│   └── wrapper/
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

## ⚙️ Getting Started

Follow these steps to run EsportSphere locally.

### Prerequisites

Make sure you have the following installed:

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Git
- A Supabase project
- A Firebase project configured for Android

### 1. Clone the Repository

```bash
git clone https://github.com/AkashGitX/Kotlin-EsportSphere-Esports-Discovery-Live-Updates.git
```

### 2. Open the Project

Open the cloned project folder in Android Studio and allow Gradle to sync.

### 3. Configure Backend Services

Configure the required credentials and integrations for your own development environment:

- Supabase project URL
- Supabase anon/publishable key
- Google authentication configuration
- Firebase Android configuration
- FCM configuration

Ensure your credentials match the configuration expected by the application.

### 4. Configure Firebase

Add your Firebase Android configuration file to the appropriate app directory if required.

Enable Firebase Cloud Messaging in your Firebase project and configure the necessary Android permissions and services.

### 5. Configure Supabase

Set up your Supabase project and configure the required database schema, authentication providers, and Row Level Security policies.

Apply the required database migrations from the `supabase/migrations` directory where applicable.

### 6. Build and Run

Run the application through Android Studio using an emulator or a connected Android device.

Alternatively, from the project root:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

## 🔐 Authentication & Cloud Integration

EsportSphere integrates cloud-backed services to support authentication, application data, and notification delivery.

| Service | Integration |
|---|---|
| Google | Google authentication |
| Supabase Auth | User authentication |
| Supabase Cloud | Cloud-hosted backend |
| PostgreSQL | Structured application data |
| Firebase Cloud Messaging | Push notification delivery |

## 🔔 Notifications

The project integrates **Firebase Cloud Messaging (FCM)** to support push notifications.

The notification infrastructure includes:

- Firebase messaging service integration
- Device token handling
- Notification management
- Push notification delivery infrastructure

## 🎯 Project Goals

EsportSphere is designed to:

- Organize fragmented esports information into a centralized discovery experience.
- Make competitive players and teams easier to discover.
- Improve visibility into tournaments and live esports activity.
- Provide an accessible esports news experience.
- Connect users to the competitive gaming ecosystem.
- Build a scalable foundation for esports-focused mobile features.

## 🗺️ Roadmap

- [ ] Expand esports player and team coverage.
- [ ] Improve tournament discovery and information.
- [ ] Enhance live broadcast discovery.
- [ ] Expand notification personalization.
- [ ] Improve content discovery and navigation.
- [ ] Extend support for additional esports titles.
- [ ] Continue improving UI/UX and performance.

## 👨‍💻 Author

**Akash Sutradhar**

B.Tech — Computer Science & Business Systems | Android & Software Development

- GitHub: [@AkashGitX](https://github.com/AkashGitX)
- LinkedIn: [Akash Sutradhar](https://www.linkedin.com/in/akash-sutradhar-b6a305287/)

## 📌 Repository

[**Kotlin EsportSphere — GitHub Repository**](https://github.com/AkashGitX/Kotlin-EsportSphere-Esports-Discovery-Live-Updates)

## ⭐ Support

If you find the project interesting, consider giving the repository a ⭐ on GitHub!

---

<p align="center">
  <b>Built with Kotlin for the esports community. 🎮</b>
</p>
