# QuickBite Frontend Technologies Guide

Your frontend is a modern, responsive Single Page Application (SPA) built entirely in **Angular 17+**. Here is an explanation of the core concepts and technologies you implemented.

## 1. Angular & Standalone Components
Historically, Angular required complex `NgModule` files to declare every component. You built this application using the modern **Standalone Components** architecture.
- **What it means:** Every component (like `CustomerDashboardComponent`) manages its own imports. It imports exactly what it needs (`CommonModule`, `RouterLink`, etc.) directly in the `@Component` decorator, making the code much cleaner and easier to read.

## 2. TypeScript (The Language)
You used TypeScript, which is a superset of JavaScript that adds "types" (like `String`, `Number`, `Order`, `Restaurant`).
- **Why it's awesome:** If you try to access `order.restaurantName` but the backend only sends `order.restaurantId`, TypeScript will throw a red error in your editor before you even run the app. It prevents thousands of runtime bugs.

## 3. RxJS & Observables (Handling Asynchronous Data)
When your frontend asks the backend for data (like fetching restaurants), it takes a few milliseconds for the data to travel across the network.
- **Observables:** You used RxJS Observables (`HttpClient.get()`) to handle this waiting period. 
- **Subscriptions:** Your components `subscribe()` to these observables. As soon as the data arrives from the backend, the code inside the subscription executes and updates your variables.

## 4. Change Detection (The UI Refresher)
Angular has a hidden mechanism called "Change Detection". Whenever a variable changes (e.g., `this.walletBalance = 500`), Angular detects it and instantly updates the HTML on the screen.
- **The Catch:** Sometimes, if data arrives from a source Angular isn't watching closely, the screen might stay blank. You learned how to fix this by manually injecting `ChangeDetectorRef` and calling `this.cdr.detectChanges()` to forcefully tell Angular: *"Hey, the data arrived, update the screen right now!"*

## 5. Routing and Guards (Navigation & Security)
- **Routing:** You set up an `app.routes.ts` file to map URLs (like `/customer/dashboard`) to specific components. Since it's a Single Page App, navigating doesn't cause the browser page to reload; Angular just swaps out the components instantly.
- **Auth Guards:** You used Route Guards (`CanActivate`) to protect sensitive URLs. If someone tries to manually type `/owner/dashboard` in the URL bar, the Guard checks if they have a valid JWT token and the `OWNER` role. If not, it redirects them back to the login page.

## 6. Services & HTTP Interceptors
- **Services:** Instead of putting API calls inside your components, you created dedicated Services (like `OrderService`, `AuthService`). Components simply ask the service for data, keeping the UI code clean.
- **Interceptors:** You implemented an HTTP Interceptor that intercepts *every single outgoing network request* and automatically attaches the JWT token to the headers. This way, you don't have to manually attach the token on every API call you write!
