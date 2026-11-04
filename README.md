# Podman Web App Manager (DOPE)

> An advanced full-stack Web Application for managing, customizing, and testing Podman images and containers through an intuitive graphical interface and dedicated REST APIs. Developed as a university project for the *Web Applications* course at the University of Padua.

---

## Tech Stack & Technologies
The project was developed adopting a robust architecture and industry standards for the Enterprise Java world:

* **Language:** Java
* **Backend & Routing:** Java Servlets, REST API, REST Dispatcher Pattern
* **Architectural Pattern:** DAO (Data Access Object) Pattern, 3-Tier Architecture
* **Database:** PostgreSQL / MySQL
* **Application Server:** Apache Tomcat
* **Build Automation:** Maven
* **Frontend:** HTML5, CSS3, JavaScript, Java Server Pages (JSP), Bootstrap
* **Containerization & Engine:** Podman (OCI-compliant), Docker Compose

---

## Features Overview
* **Repositories Management:** Creation and management of personal repositories (public or private) to organize OCI images.
* **Guided Image Building:** Graphical interface mimicking the `Containerfile` / `Dockerfile` format to compile and build new custom images.
* **Container Testing Environment:** Execution of temporary containers (volatile containers) based on pre-existing images with support for:
  * Starting, stopping (killing), and executing custom commands.
  * Real-time viewing of container logs and executed commands.
* **Commit Functionality:** Ability to commit a running container directly into a new image by specifying its tag and description.
* **Complete REST Coverage:** Every feature of the web app is also accessible via REST endpoints protected by authentication filters and granular access controls.

---

## System Architecture
The system adopts an extended 3-Tier model for Podman integration:
1. **Presentation Layer:** Managed via JSP, frontend servlets, Bootstrap for responsive design, and JS scripts for asynchronous calls (Ajax).
2. **Business Layer:** Centralized through a `RestDispatcherServlet` and routing components that communicate with the `PodmanClientContainer` / `PodmanClientImage` middleware to interface with Podman REST APIs.
3. **Data Access Layer:** Implemented using the DAO pattern and transactional management on a relational database (PostgreSQL/MySQL), secured by session-based authentication mechanisms and password hashing (Bcrypt).

---

## Quick Start Guide

To ensure proper application startup and avoid state conflicts, strictly follow this procedure on every restart:

1. **Stop and remove previous containers** (via Docker Compose or equivalent Podman command):
   ```bash
   docker compose down
   ```
2. **Clean local database data** by deleting the dedicated folder:
   ```bash
   rm -rf dope-webapp/data/db
   ```
3. **Rebuild the project** using Maven:
   ```bash
   mvn clean package
   ```
4. **Start the services** with Docker Compose or equivalent Podman:
   ```bash
   docker compose up
   ```

---

## Documentation
The project's compiled detailed technical documentation (Javadocs) can be found at the following local path:
`dope-webapp/target/reports`

---

## Development Team (DOPE Group)
Project developed by the following students from the University of Padua:

| Name and Surname | ID Number |
|------------------|-----------|
| Marco Martini    | 2087642   |
| Steven Laghetto  | 2139226   |
| Iegor Toporov    | 2131355   |
| Nicolas Brentel  | 2161073   |
| Francesco Pivotto| 2158296   |
| Alessio Demo     | 2142885   |