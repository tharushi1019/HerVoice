# 📱 HerVoice - Women's Emergency Safety App

HerVoice is a mobile application designed to support **UN Sustainable Development Goal 5 (Gender Equality)** by providing emergency safety features for women. The app enables users to **send real-time location alerts** to **trusted contacts**, manage their emergency list, and maintain secure profile settings — all powered by **Firebase**.

---

## 🎯 Goal

To empower women with **instant location-based emergency alerts**, integrated **Firebase services**, and a secure contact management system — accessible even without logging in.

---

## 🚀 Features

### 🔐 User Authentication
- Firebase Email & Password Authentication
- Sign Up / Sign In / Password Reset

### 📇 Contact Management
- Add/edit/delete up to **5 trusted contacts**
- Enable/disable **emergency SMS alerts**
- Direct call & SMS options

### 📍 Emergency Alert
- **Send real-time location** via SMS to selected contacts
- **Live map view** (Google Maps)
- Vibration + siren sound upon sending alerts

### 👤 Profile
- View/update name & phone number
- Upload/change profile image
- **Secure account deletion** with password re-authentication
- Firebase Firestore integration

### 🔧 Backend (Firebase)
- Firebase Authentication
- Firebase Firestore for contacts
- Firebase Crashlytics for monitoring
- Firebase App Distribution for deployment

---

## 🛠️ Tech Stack

| Layer               | Technology                          |
|--------------------|--------------------------------------|
| Frontend           | Android (Java, XML)                  |
| Backend-as-a-Service | Firebase (Auth, Firestore, Crashlytics) |
| CI/CD              | GitHub Actions                       |
| Monitoring         | Firebase Crashlytics                 |
| Deployment         | Firebase App Distribution            |

---

## 📦 CI/CD Workflow

- Built with **GitHub Actions**
- Injects `google-services.json` and `secrets.xml` using **GitHub Secrets**
- Includes **Gradle caching** for performance
- Runs **unit tests** with every build

🔒 **No secrets or credentials are pushed to the repo**

---

## ✅ Completed DevOps Practices

- 🔁 GitHub Flow: branching, PRs, reviews
- 🔒 Secrets managed with GitHub Actions
- 🧪 Unit testing integration
- 📈 Firebase observability (Crashlytics, Analytics)
- 📤 App Distribution for testing

---

## 📷 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/59624734-e0cf-4093-8a5e-bc85e58b8292" width="250"/>
  <img src="https://github.com/user-attachments/assets/c6be51d7-d7e6-4312-98ee-b7e9b7a87d20" width="250"/>
  <img src="https://github.com/user-attachments/assets/1a70b7a3-dedd-4fb8-b625-0b5326e62dd2" width="250"/>
</p>

---

## 📲 Installation (For Testers)

This app is distributed via **Firebase App Distribution**:

1. Download link will be sent via email (upon being added as a tester).
2. Click the link → Install HerVoice → Test on device.
3. APK distribution via GitHub Releases **may be added in the future**.

---

## 🧪 Testing

Basic unit tests are located in:
```
app/src/test/java/com/example/hervoice/ExampleUnitTest.java
```

You can run tests with:

```bash
./gradlew test
```

---

## 🔐 Security

- Secrets are not stored in code.
- Secure user re-authentication before deleting profile.
- Permissions requested only when required (Location, SMS, Call).

---

## 📘 Documentation

- 🔗 [Project Wiki](https://github.com/tharushi1019/HerVoice/wiki)  
  Includes architecture diagrams, DevOps pipelines, deployment steps, and decisions.

---

## 👩‍💻 Developer

**Tharushi**  
Undergraduate, BSc (Hons) in Information Technology  
[GitHub Profile](https://github.com/tharushi1019)

---

## 📄 License

This project is for educational purposes and is not intended for production use.  
Please contact the author for reuse or collaboration.

---
