RESTAURANT TABLE AND ORDER MANAGEMENT SYSTEM
Java backend + HTML/CSS/JavaScript frontend

WHAT THIS DEMO DOES
- Displays a sample food menu and prices.
- Displays restaurant tables, seating capacity, and availability.
- Accepts customer name, table selection, and menu item selection.
- Checks table availability and validates the order.
- Calculates the bill automatically.
- Marks the selected table as Occupied.
- Displays placed orders and their totals.

REQUIREMENT
- JDK 11 or newer installed.
- No external Java libraries are required for this runnable demo.

PROJECT FILES
- RestaurantServer.java : Java HTTP server and backend logic
- index.html             : Frontend user interface

HOW TO RUN IN VS CODE (Windows)
1. Create a folder named oopslab.
2. Put RestaurantServer.java and index.html inside that folder (they are already inside oopslab).
3. Open that folder in VS Code.
4. Open Terminal > New Terminal.
5. Compile:
   javac RestaurantServer.java
6. Copy index.html into the classpath root (same folder is fine):
   (It is already in the same folder.)
7. Run:
   java RestaurantServer
8. Open your browser and visit:
   http://localhost:8080

IMPORTANT
- Keep the terminal running while using the website.
- Stop the server with Ctrl+C in the terminal.
- This beginner-friendly runnable version stores data in memory. Orders and table changes
  reset when the server is stopped. It does not yet use MySQL/JDBC persistence.
- The supplied PDF describes JDBC/database integration. JDBC/MySQL can be added as a
  next step, but this version avoids database setup so you can run and test the interface first.

SAMPLE TEST
1. Enter: Priya
2. Select Table 1
3. Select Veg Burger (₹120) and Fresh Lime Soda (₹50)
4. Click "Place Order & Generate Bill"
Expected: success message and total bill ₹170.00; Table 1 changes to Occupied.

STYLING AND OPENING THE PAGE
- index.html now has a colourful restaurant theme (tomato red, saffron gold, herb green, cream).
  Only CSS was added for the look; all the original HTML and JavaScript logic is unchanged.
- Always start the Java server first, then use http://localhost:8080 (recommended).
- If you open index.html by double-clicking it, or with VS Code Live Server, the page now still talks to the
  Java server on http://localhost:8080, so the menu and tables load - but the Java server must be running.
- RestaurantServer.java now also finds index.html on disk if it is not on the classpath, so it works with the
  VS Code "Run" button as well as with "java RestaurantServer".
