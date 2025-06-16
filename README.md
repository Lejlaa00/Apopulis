<p align="center">
  <img src="https://github.com/Lejlaa00/Apopulis/blob/4d9f5554228981e827c5f31cfcd1826667f0a0ba/Images/Apopulis_logo.png" alt="Apopulis Logo" width="300"/>
</p>

##

**Apopulis** je pametna platforma za zbiranje, analizo in prikaz slovenskih novic z uporabo NLP in geolokacije.  
Povezuje spletno in namizno aplikacijo, omogoča personalizacijo vsebin, interakcijo z novicami ter vizualizacijo podatkov na zemljevidu.  
Projekt je bil razvit kot multidisciplinarna rešitev v okviru študija Računalništva in informatike na FERI.

<br>

## 🧠 Cilj projekta

Razviti pametno platformo za zbiranje, analizo in vizualizacijo slovenskih novic z uporabo naprednih tehnologij. Aplikacija omogoča:

- Samodejno zajemanje novic iz več slovenskih virov  
- Kategorizacijo vsebine z uporabo NLP in ekstrakcijo ključnih besed  
- Prikaz novic na interaktivnem geografskem zemljevidu  
- Filtriranje, všečkanje, komentiranje in shranjevanje novic  
- Personalizirano izkušnjo glede na interese uporabnika  
- Upravljanje in urejanje novic prek namizne aplikacije 

<br>

## ⚙️ Funkcionalnosti

✅ Zajem in obdelava novic

- Samodejni scraping novic iz več slovenskih spletnih virov  
- Uporaba obdelave naravnega jezika (NLP) za kategorizacijo vsebin  
- Ekstrakcija ključnih besed za učinkovitejše iskanje in razvrščanje  

🗺️ Geolokacija in vizualizacija

- Prikaz lokacij povezanih z novicami na interaktivnem zemljevidu  
- Filtriranje po kategorijah in ključnih besedah
- Povezovanje vsebine s prostorskim kontekstom  

💬 Interakcija z vsebinami

- Všečkanje, komentiranje in shranjevanje izbranih novic  
- Personalizirana priporočila glede na uporabniške interese  
- Ogled osebne statistike na profilu uporabnika  

🖥️ Namizna aplikacija

- Ustvarjanje, urejanje in brisanje novic v grafičnem vmesniku  
- Uvoz novic
- Razvrščanje po kategorijah in ključnih besedah  

🔁 CI/CD in razvojna infrastruktura

- Avtomatsko testiranje in nameščanje prek GitHub Actions  
- Uporaba Docker okolja za razvoj, testiranje in produkcijo  
- Webhook podpora za posodobitve v realnem času na Azure VM  

<br>

## 🧩 Uporabljene tehnologije

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

## 🛠️ Namestitev 

### 1. Kloniraj repozitorij

```
git clone https://github.com/Lejlaa00/Apopulis.git
```

### 2. Zaženi backend (Node.js + Express)
```
cd backend
npm install
npm start
```

### 3. Zaženi frontend (React)
```
cd frontend
npm install
npm start
```

Frontend bo dostopen na: http://localhost:3000

Backend teče na: http://localhost:5001


### 4. Zaženi desktop aplikacijo (Kotlin + Compose)
- Odpri mapo desktop-app v IntelliJ IDEA (ali drugem Kotlin-podprtem IDE)
- Poišči Main.kt
- Zaženi aplikacijo

<br>

## 🔐 Prijava in registracija

Uporabniki lahko brez prijave berejo javne novice.

Če pa se registrirajo, dobijo dostop do dodatnih funkcij:

- Prilagojene novice
- Statistika uporabe
- Všečkanje, komentiranje in shranjevanje novic

<br>

## 👥 Ekipa
- Lejla Gutić
- Ivana Ilić
- Kenan Kravić
