# SQL ANALYZER

## Project Overview

SQL ANALYZER is a Java Swing based DBMS visualization project that allows users to execute SQL queries and visualize internal database operations.

The project is designed as an educational SQL execution and visualization tool for understanding how DBMS systems process queries internally.

The application supports:

- SQL query execution
- SELECT, INSERT, UPDATE, DELETE operations
- Query result visualization
- Database metadata display
- PostgreSQL integration
- Dockerized database infrastructure

The backend database runs inside Docker using PostgreSQL and Docker Compose.

---

# Technologies Used

## Frontend / UI
- Java Swing

## Backend
- JDBC
- PostgreSQL

## DevOps / Infrastructure
- Docker
- Docker Compose

## Database Tools
- pgAdmin
- VS Code PostgreSQL Extension

---

# Project Architecture

Java Swing Application  
↓  
JDBC Connection  
↓  
Docker PostgreSQL Container  
↓  
Persistent Docker Volume

---

# Prerequisites

Before running the project, install:

- Java JDK 21 or above
- Docker Desktop
- Git
- VS Code (Recommended)

---

# How to Run the Project

## Step 1 - Clone Repository

```bash
git clone https://github.com/karthikgowdav/SQL-ANALYZER.git
```

## Step 2 - Open Project Folder

```bash
cd SQL-ANALYZER
```

## Step 3 - Start PostgreSQL Docker Container

```bash
docker compose up -d
```

This will automatically:

- Start PostgreSQL container
- Create sql_visualizer database
- Execute init.sql
- Create tables
- Insert sample data

## Step 4 - Compile Java Files

```bash
javac -cp "lib/postgresql-42.7.11.jar" src/*.java
```

## Step 5 - Run Application

```bash
java -cp "lib/postgresql-42.7.11.jar;src" SQLVisualizerUI
```

---

# Docker Configuration

The PostgreSQL database runs using Docker Compose.

## Port Mapping

Host Machine:
5434

Docker Container:
5432

## JDBC URL

```java
jdbc:postgresql://localhost:5434/sql_visualizer
```

---

# Features

- SQL Query Execution
- Query Result Display
- Database Metadata Information
- Dockerized PostgreSQL Setup
- Automatic Database Initialization
- Educational DBMS Visualization Foundation

---

# Future Scope

- Query execution animations
- Sequential scan visualization
- Index scan visualization
- Join visualization
- Sorting visualization
- Query execution plans
- DBMS internal operation simulation

---

# Team Setup Notes

Every teammate should:

1. Install Docker Desktop
2. Install Java JDK
3. Clone repository
4. Run docker compose up -d
5. Compile and run Java project

No local PostgreSQL installation is required.

---

# Author

Karthik Gowda