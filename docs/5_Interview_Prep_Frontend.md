# Interview Prep: Frontend (Angular)

If the interviewer asks you to change a color, add a button, or explain how a page is rendered, this guide will tell you exactly where to go and what to say.

## 1. Where is Everything? (Folder Structure)

The entire frontend lives inside `quickbite-ui/src/app`. You divided the app into logical "feature modules" (folders):

- **`auth/`**: Contains `login.component` and `register.component`. This is where the initial UI is built.
- **`dashboard/`**: Contains sub-folders for each user role (`admin-dashboard`, `agent-dashboard`, `customer-dashboard`, `owner-dashboard`). 
  - *Interview Tip:* If they ask you to change the Customer Homepage, go to `dashboard/customer-dashboard/customer-dashboard.component.html`.
- **`core/`**: Contains things shared across the app.
  - `core/services/`: This is where all the API calls happen (`order.service.ts`, `auth.service.ts`).
  - `core/components/`: Things like the `sidebar.component` that show up on multiple pages.
  - `core/interceptors/`: Contains the `jwt.interceptor.ts` which magically attaches the secure token to every request.
- **`cart/`**, **`order/`**, **`restaurant/`**, **`menu/`**: Dedicated folders containing the UI components for those specific features.

## 2. Anatomy of an Angular Component

Every UI element is a "Component" made up of 3 main files that sit right next to each other. For example, in `customer-dashboard`:

1. **`customer-dashboard.component.ts` (The Logic)**
   - This is the "Brain". It runs TypeScript code. It talks to the `Services` to fetch data from the backend. 
   - It stores data in variables like `this.restaurants = [...]`.
2. **`customer-dashboard.component.html` (The Skeleton)**
   - This is the markup. It uses Angular syntax like `*ngFor="let res of restaurants"` to loop through the data sent by the `.ts` file and display it dynamically.
   - *Interview Tip:* If asked to add a new text box or button, you edit this file.
3. **`customer-dashboard.component.css` (The Makeup)**
   - This contains the styling. Because Angular uses "View Encapsulation", any CSS you write here *only* affects the `customer-dashboard`. It will never accidentally break the styling of the login page.
   - Note: Global styles (like the overall background color) are kept in `src/styles.css`.

## 3. Key Concepts to Explain in an Interview

### A. How do you pass data from TypeScript to HTML?
"I use Data Binding. For example, in the `.ts` file, I have `firstName = 'Anantika';`. In the `.html` file, I write `{{ firstName }}`. Angular automatically replaces the brackets with the name. This is called **Interpolation**."

### B. What happens if the interviewer says: "Change the layout of the restaurant cards?"
"I would open `restaurant-list.component.html`. The cards are likely using CSS Flexbox or CSS Grid. I would find the `<div>` holding the `*ngFor` loop, and then modify the CSS class in `restaurant-list.component.css` to adjust the layout."

### C. Why are the skeleton loaders showing, and how did you fix it?
"We had an issue where the data was fetched asynchronously, but Angular's Change Detection cycle didn't trigger to update the UI. I solved this by injecting `ChangeDetectorRef` and calling `this.cdr.detectChanges()` inside the RxJS `.subscribe()` callback. This forces Angular to re-render the view instantly when the API returns data."

### D. How is the Navigation Sidebar built?
"Instead of copy-pasting the sidebar code into every dashboard, I created a reusable Standalone Component called `SidebarComponent`. Then, in my dashboard HTML files, I simply insert `<app-sidebar></app-sidebar>`. It keeps the code extremely DRY (Don't Repeat Yourself)."
