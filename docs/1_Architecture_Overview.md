# QuickBite Architecture Overview

Welcome to the QuickBite Food Delivery Platform! You have built a highly scalable, distributed system using the **Microservices Architecture**. Instead of building one massive application (a monolith), you divided the system into small, independent services that talk to each other.

## 1. The Core Infrastructure

### Service Registry (Eureka)
- **What it does:** Think of Eureka as the "phonebook" of your application.
- **How it works:** When a microservice (like `order-service`) starts up, it registers itself with Eureka. When one service needs to talk to another, it asks Eureka for the IP address and port. This means you never have to hardcode URLs!

### API Gateway (Spring Cloud Gateway)
- **What it does:** The front door to your backend.
- **How it works:** Your Angular frontend **only** talks to the API Gateway (port 8080). The Gateway looks at the URL (e.g., `/api/orders`) and routes the request to the correct microservice behind the scenes. It also handles CORS (allowing your frontend to talk to the backend).

## 2. The Microservices

You built 9 separate microservices, each with its own specific database and responsibility:

1. **Auth Service (`quickbite_auth`):** Handles user registration, login, password hashing, and generates JWT (JSON Web Tokens) for security.
2. **Restaurant Service (`quickbite_restaurant`):** Manages restaurant profiles, approval statuses, and basic info.
3. **Menu Service (`quickbite_menu`):** Manages categories and food items for each restaurant.
4. **Cart Service (`quickbite_cart`):** Temporarily holds items a customer wants to buy. 
5. **Order Service (`quickbite_order`):** The brain of the transaction. Calculates totals, handles checkout, and tracks order status (Placed -> Preparing -> Delivered).
6. **Payment Service (`quickbite_payment`):** Integrates with payment gateways (like Razorpay) and manages digital wallets.
7. **Delivery Service (`quickbite_delivery`):** Manages delivery agents, tracking, and assigning orders to available drivers.
8. **Review Service (`quickbite_review`):** Handles customer ratings and feedback for restaurants and items.
9. **Notification Service (`quickbite_notification`):** Sends alerts, emails, and in-app messages to users.

## 3. How Services Communicate

Because these are separate applications running on different ports, they need to talk to each other to get things done. You implemented two types of communication:

### A. Synchronous Communication (REST via RestTemplate)
- **Used when:** Service A *must wait* for an answer from Service B before continuing.
- **Example:** When placing an order, `order-service` makes a REST call to `cart-service` to fetch the items. It cannot proceed until it gets that data.

### B. Asynchronous Communication (RabbitMQ)
- **Used when:** Service A wants to announce something happened, but doesn't care who is listening, and doesn't want to wait.
- **Example:** When an order is placed, `order-service` drops a message into a RabbitMQ queue. The `notification-service` picks it up and sends an email. The `order-service` doesn't wait for the email to send; it responds to the customer instantly.

---
**Summary:** You have built an enterprise-grade backend that mimics how massive companies like UberEats or Zomato structure their code. If `review-service` crashes, people can still place orders. If `cart-service` gets heavy traffic, you can duplicate it without copying the whole app!


<!-- I have successfully copied all 8 guide files into your project folder! You can find them here: c:\Users\91870\OneDrive\Documents\Desktop\FoodApp\QuickBite-FoodDeliveryApp\docs\

Now, let me explain the diagrams for you, so you can describe them perfectly in your interview:

1. System Architecture (The Big Picture)
This diagram shows the "Bird's Eye View" of your project.

The Flow: It shows how the Angular UI sends a request to the API Gateway. The Gateway then distributes that request to one of the 8 specialized microservices.
Infrastructure: You can see Eureka in the middle—this is the "Phonebook" that helps services find each other.
Optimization: It shows how Redis sits next to the services to speed them up, and how RabbitMQ acts as the bridge to send messages to the Notification service without slowing down the Order service.
2. ER Diagram (The Database Heart)
This shows how data is organized across your different databases.

Relationships: It explains that a User can have many Orders (1-to-Many), and a Restaurant has many Menu Items.
Linking: Even though the databases are separate, they link using IDs. For example, the Order table has a customer_id which refers back to the User table in the Auth database.
Entities: It lists exactly what fields you are storing, like email, role, price, and order_status.
3. Order Flow Sequence Diagram (The Step-by-Step)
This is a "Timeline" of a single transaction.

The Chain Reaction: It shows the Order Service talking to the Cart Service to get the items, then calculating the total, saving it to MySQL, and then—critically—telling the Cart Service to delete the items since they are now bought.
Async Logic: At the bottom, you see the "Asynchronous Process." This is where the Order Service "fires and forgets" a message to RabbitMQ, which the Notification Service picks up later. This is why the app feels so fast to the user!
4. Class Diagram (The Code Structure)
This explains how you organized your Java code inside each microservice.

The Layers: It shows the 4 layers:
Resource (Controller): Handles the incoming web request.
Service (Interface): Defines what the service can do.
ServiceImpl: The actual "brain" where the Java logic is written.
Repository: The part that actually writes the data to the MySQL database.
Encapsulation: This proves you used Clean Architecture principles, making your code easy to maintain and test.
Interview Tip: When you show these diagrams, use your finger to trace the lines and explain the "Path of a Request." Interviewers love to see that you understand the data flow!

Is there any specific part of these diagrams you want me to explain in even more detail? -->

