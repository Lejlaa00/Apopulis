<p align="center">
  <img src="https://github.com/Lejlaa00/Apopulis/blob/4d9f5554228981e827c5f31cfcd1826667f0a0ba/Images/Apopulis_logo.png" alt="Apopulis Logo" width="300"/>
</p>

##

<br>

**Apopulis** is an intelligent platform for collecting, analyzing, and presenting Slovenian news using NLP and geolocation.
It connects a web and desktop application, enables content personalization, interaction with news articles, and data visualization on a map.
The project was developed as a multidisciplinary solution within the Computer Science and Information Technology studies at FERI.

<br>

## 🧠 Project Goal

To develop an intelligent platform for collecting, analyzing, and visualizing Slovenian news using advanced technologies. The application enables:

- Automatic collection of news from multiple Slovenian sources
- Content categorization using NLP and keyword extraction
- Display of news on an interactive geographic map
- Filtering, liking, commenting, and saving news
- A personalized user experience based on user interests
- News management and editing through a desktop application

<br>

## ⚙️ Features

✅ News collection and processing

- Automatic scraping of news from multiple Slovenian online sources
- Use of Natural Language Processing (NLP) for content categorization
- Keyword extraction for more efficient searching and sorting

🗺️ Geolocation and visualization

- Display of locations related to news on an interactive map
- Filtering by categories and keywords
- Linking content with spatial context

💬 Content interaction

- Liking, commenting on, and saving selected news
- Personalized recommendations based on user interests
- Viewing personal statistics on the user profile

🖥️ Desktop application

- Creating, editing, and deleting news via a graphical interface
- News import
- Sorting by categories and keywords

🔁 CI/CD and development infrastructure

- Automated testing and deployment via GitHub Actions
- Use of Docker environments for development, testing, and production
- Webhook support for real-time updates on an Azure VM

<br>

## 🧩 Technologies Used

| Sloj         | Tehnologija                          |
|--------------|--------------------------------------|
| Frontend     | React, JavaScript                    |
| Backend      | Node.js, Express                     |
| Baza podatkov| MongoDB, MongoDB Atlas               |
| NLP analiza  | Natural, stopword, keyword-extractor |
| Namizna app  | Kotlin, Jetpack Compose Desktop      |
| CI/CD        | GitHub Actions, Docker, Webhook      |
| Gostovanje   | Azure Virtual Machine                |
| Avtentikacija| JSON Web Token (JWT)                 |


![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=node-dot-js&logoColor=white)
![Express](https://img.shields.io/badge/Express.js-000000?style=for-the-badge&logo=express&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=Kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)
![Azure](https://img.shields.io/badge/Azure-0078D4?style=for-the-badge&logo=Microsoft%20Azure&logoColor=white)

<br>

## 🛠️ Installation 

### 1. Clone the repository

```
git clone https://github.com/Lejlaa00/Apopulis.git
```

### 2. Run the backend (Node.js + Express)
```
cd backend
npm install
npm start
```

### 3. Run the frontend (React)
```
cd frontend
npm install
npm start
```

The frontend will be available at: http://localhost:3000

The backend runs at: http://localhost:5001


### 4. Run the desktop application (Kotlin + Compose)
- Open the desktop-app folder in IntelliJ IDEA (or another Kotlin-supported IDE)
- Locate Main.kt
- Run the application

<br>

## 🔐 Login and Registration

Users can read public news without logging in.

By registering, they gain access to additional features:

- Personalized news
- Usage statistics
- Liking, commenting, and saving news

<br>

## 👥 Ekipa
- Lejla Gutić
- Ivana Ilić
- Kenan Kravić
