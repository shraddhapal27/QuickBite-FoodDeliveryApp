# Interview Prep: How the Frontend Connects to the Backend

If an interviewer asks: *"How does your Angular frontend communicate with your 9 different Java microservices?"* — this is exactly how you answer it.

## 1. The Big Picture (The API Gateway)

**The Problem:** Your 9 microservices run on 9 different ports (8081, 8082, 8083, etc.). It would be a nightmare for the Angular frontend to remember all these different ports. Also, browsers block requests to different ports because of CORS (Cross-Origin Resource Sharing) security.

**The Solution:** You built an **API Gateway** running on port `8080`.
- Your Angular frontend *only ever talks to port 8080*. It has no idea that 9 microservices even exist.
- If Angular wants restaurants, it sends an HTTP GET request to `http://localhost:8080/restaurants`.
- The API Gateway looks at the URL `/restaurants`, checks its `application.yml` routing table, and silently forwards the request to `http://localhost:8082/restaurants` (the restaurant-service).
- The API Gateway also handles all **CORS configurations** globally, allowing `localhost:4200` (Angular) to talk to the backend without the browser blocking it.

## 2. How Angular Makes the Call

In your Angular code, you use the `HttpClient` module to make REST API calls. 

**Example (Fetching Restaurants):**
1. In `restaurant.service.ts`, you have a method:
   `return this.http.get<Restaurant[]>('http://localhost:8080/restaurants');`
2. Angular makes the network request. 
3. The `jwt.interceptor.ts` intercepts the request before it leaves the browser, gets the JWT token from Local Storage, and attaches it to the headers: `Authorization: Bearer <token>`.
4. The request hits the Gateway (8080), passes through to `restaurant-service` (8082), which verifies the token, fetches the data from Redis/MySQL, and sends it back to Angular.

## 3. The Core API Endpoints

If asked about the API structure, you can list these main endpoints you built. Notice how clean and RESTful the URLs are.

### 🔐 Auth Service (`/auth`)
- `POST /auth/register` : Creates a new user account.
- `POST /auth/login` : Verifies credentials and returns a JWT token.

### 🏪 Restaurant Service (`/restaurants`)
- `GET /restaurants` : Gets all approved restaurants (Cached in Redis).
- `GET /restaurants/{id}` : Gets a specific restaurant.
- `GET /restaurants/owner/{ownerId}` : Gets restaurants owned by a specific user.

### 🍔 Menu Service (`/menu`)
- `GET /menu/{restaurantId}` : Fetches all food items for a specific restaurant.
- `POST /menu` : (Admin/Owner) Adds a new food item to a restaurant.

### 🛒 Cart Service (`/cart`)
- `GET /cart/{customerId}` : Fetches the current user's cart.
- `POST /cart/{customerId}/add` : Adds an item to the cart.
- `DELETE /cart/{customerId}/clear` : Empties the cart.

### 📦 Order Service (`/orders`)
- `POST /orders` : Places a new order (Converts Cart into an Order).
- `GET /orders/customer/{customerId}` : Gets order history for a user.
- `PUT /orders/{orderId}/status` : Updates order state (e.g., PLACED -> PREPARING).

### 🛵 Delivery Service (`/agents`)
- `GET /agents` : Gets a list of delivery agents.
- `PUT /agents/{agentId}/location` : Updates the GPS `latitude` and `longitude` of a driver.

### 💳 Payment Service (`/wallet` & `/payments`)
- `GET /wallet/{customerId}/balance` : Fetches the user's current digital wallet balance.
- `POST /wallet/{customerId}/add` : Adds funds to the digital wallet.

### 🔔 Notification Service (`/notifications`)
- `GET /notifications/recipient/{userId}/unread` : Gets all unread alerts for a user.
- `POST /notifications/send` : Sends a manual notification (Though mostly, this is done automatically via RabbitMQ).
