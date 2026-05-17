# Interview Prep: Backend (Spring Boot & Java)

If the interviewer asks you to explain the logic of your backend code, how a specific feature works, or how you handle real-time data like GPS, use this guide.

## 1. Anatomy of a Microservice

Every one of your 9 microservices follows the classic "Controller-Service-Repository" pattern. If asked to explain a feature, explain this flow:

1. **The Controller (The Waiter):** Found in `src/main/java/.../resource` or `controller`. 
   - *Example:* `DeliveryResource.java`. It receives the HTTP request from Angular (e.g., `PUT /agents/{id}/location`). It doesn't do business logic; it just takes the order and hands it to the kitchen.
2. **The Service (The Kitchen):** Found in `src/main/java/.../service/impl`. 
   - *Example:* `DeliveryServiceImpl.java`. This is where the heavy lifting happens. It contains the logic, checks if the data is valid, and decides what to do.
3. **The Repository (The Pantry):** Found in `src/main/java/.../repository`.
   - *Example:* `DeliveryAgentRepository.java`. An interface that extends `JpaRepository`. It translates Java commands into MySQL queries (like `save()` or `findById()`) to store the data permanently.

## 2. How the GPS Logic Works (`delivery-service`)

**Question:** *"How does the system track a delivery agent's location?"*
**Your Answer:**
1. **The Data Structure:** In my `DeliveryAgent` entity, I added two `Double` fields: `currentLatitude` and `currentLongitude`.
2. **The Frontend Side:** The Delivery Agent's mobile/web app uses the HTML5 Geolocation API (`navigator.geolocation`) to constantly read their physical GPS coordinates.
3. **The API Call:** The frontend sends a `PUT` request to `/agents/{id}/location` with a JSON body (`LocationUpdateDTO`) containing the lat and lng.
4. **The Backend Logic:** In `DeliveryServiceImpl.java`, the `updateLocation` method fetches the agent from the MySQL database using their ID, updates the latitude and longitude, and saves it back. 
5. **Real-time updates:** The customer's frontend polls the API to fetch these coordinates and can plot them on a map to track their food.

## 3. How the Messaging System Works (RabbitMQ)

**Question:** *"Why did you use RabbitMQ instead of normal REST calls for notifications?"*
**Your Answer:**
"In a microservices architecture, synchronous REST calls can cause cascading failures. If the `notification-service` is down, a REST call from `order-service` would crash and the user wouldn't be able to place an order! 

To fix this, I implemented an **Event-Driven Architecture** using RabbitMQ.
- In `OrderServiceImpl.java`, I injected `RabbitTemplate`. When an order is placed, instead of doing an HTTP POST, I call `rabbitTemplate.convertAndSend()` to drop a JSON message into the `notification_exchange`.
- The `order-service` instantly returns a 'Success' response to the user.
- Meanwhile, in `notification-service`, I created a `NotificationListener.java` class with the `@RabbitListener` annotation. It constantly listens to the queue in the background. When it sees the message, it grabs it and sends the notification. If the notification service is down, RabbitMQ safely holds the message in memory until the service comes back online!"

## 4. How the Caching Works (Redis)

**Question:** *"Your app loads very fast. How did you optimize the database queries?"*
**Your Answer:**
"I noticed that the list of restaurants rarely changes, but every single customer requests that list when they open the app. Hitting the MySQL database for this is highly inefficient.
- I implemented **Redis**, an in-memory datastore.
- I added the `@Cacheable(value = "restaurants")` annotation on my `getAllRestaurants()` method in `RestaurantServiceImpl.java`.
- Now, the first time a user asks for restaurants, Spring Boot hits MySQL and invisibly saves the JSON result in Redis. For the next 1,000 users, Spring Boot intercepts the method call, skips MySQL entirely, and returns the data from Redis RAM in less than 5 milliseconds.
- To keep data fresh, I used `@CacheEvict` when an admin updates a restaurant, which automatically flushes the old cache."

## 5. Security & Authentication (JWT)

**Question:** *"How do you make sure a Customer doesn't access the Admin dashboard or APIs?"*
**Your Answer:**
1. **The Login:** When a user logs in, `auth-service` validates their password and creates a **JWT (JSON Web Token)**. Inside the payload of this token, I embed their specific `role` (e.g., `CUSTOMER` or `ADMIN`).
2. **The Verification:** Every time the user makes a request to a protected microservice, they send this token in the `Authorization` header.
3. **The Guard:** In my `SecurityConfig.java`, I use Spring Security to intercept the request. I extract the JWT, verify its cryptographic signature, and read the role. If the endpoint requires `ADMIN` and the token says `CUSTOMER`, Spring Boot throws a `403 Forbidden` error. I also implemented a caching mechanism in Redis to blacklist JWTs when a user logs out so stolen tokens can't be reused!
