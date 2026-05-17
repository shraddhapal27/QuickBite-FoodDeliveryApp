# QuickBite Backend Technologies Guide

Your backend is powered by a robust **Java + Spring Boot** stack. Here is a breakdown of every technology you used, why you used it, and how it works.

## 1. Spring Boot (The Foundation)
Spring Boot is the framework that makes Java web development incredibly fast. It automatically configures things like web servers (Tomcat) and database connections so you don't have to write thousands of lines of boilerplate XML. 
- **In QuickBite:** Every single microservice (Auth, Order, Cart) is its own standalone Spring Boot application.

## 2. Spring Cloud (The Microservice Enabler)
Spring Cloud is a suite of tools specifically built for distributed systems.
- **Eureka Server:** Used to register all microservices so they can find each other dynamically.
- **Spring Cloud Gateway:** Used as the single entry point for the frontend, routing traffic to the correct microservice.

## 3. Spring Data JPA & MySQL (The Storage Layer)
- **MySQL:** The relational database used to store persistent data. Since you have 9 microservices, you actually have 9 *separate* databases (e.g., `quickbite_auth`, `quickbite_order`). This is a microservice best practice called "Database per Service".
- **Spring Data JPA:** Instead of writing raw SQL (`SELECT * FROM users`), JPA allows you to create Java classes (Entities) and Interfaces (Repositories) that automatically generate the SQL for you.

## 4. Spring Security & JWT (The Bouncer)
To protect your APIs from unauthorized access, you used Spring Security integrated with JSON Web Tokens (JWT).
- **How it works:** When a user logs in via `auth-service`, the server checks their password and hands them a cryptographically signed text string (the JWT).
- For every subsequent request (like viewing the cart), the Angular frontend attaches this token. The backend verifies the token's signature to know exactly who the user is and what role they have (Customer, Owner, Admin, or Agent).

## 5. Redis (The Cache)
Redis is an incredibly fast, in-memory database used for caching.
- **Why you need it:** Querying MySQL is slow because it reads from a hard drive.
- **How you used it:** When the frontend requests the list of all restaurants, the `restaurant-service` queries MySQL once, saves the result in Redis, and serves it to the user. The next time anyone asks for restaurants, it skips MySQL entirely and grabs it from Redis in less than 5 milliseconds.

## 6. RabbitMQ (The Asynchronous Messenger)
RabbitMQ is a message broker that allows services to talk to each other without waiting.
- **How you used it:** In your `order-service`, when an order is placed, it needs to send an email notification. Instead of waiting for the `notification-service` to process the email (which takes a few seconds), the `order-service` instantly drops a message into a RabbitMQ "Queue" and tells the user their order is confirmed. 
- The `notification-service` constantly listens to this queue, picks up the message, and sends the email in the background.
