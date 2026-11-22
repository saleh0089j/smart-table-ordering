# TableSync — Smart Restaurant Ordering & Kitchen Control

A real-time restaurant ordering, kitchen dispatch, and billing system built with a **Java backend** and **HTML/CSS/JS** web portals.

## Features
- **Guest Table Portal (`web/index.html`):** Easy check-in (optional phone), 1-tap menu selection with `+`/`-` piece counters, live order tracker, and bilingual English + Bangla voice/buzzer alerts.
- **Dual Payment Modes:**
  - **Pre-Pay:** Guests pay before cooking starts; cash orders lock until staff approve at the counter.
  - **Post-Pay:** Orders go straight to the kitchen; a dedicated Step 5 final bill appears after food is marked served.
- **Kitchen & Reception Control (`web/kitchen.html`):** Live order board, cash approval unlock, PA voice calls, Chef Menu Manager, and table occupancy reset.

## How to Run
1. Compile the Java files:
   ```bash
   javac *.java
   ```
2. Start the server:
   ```bash
   java Main
   ```
3. Open in your browser:
   - **Kitchen:** `http://localhost:8080/kitchen.html`
   - **Customer Table #1:** `http://localhost:8080/index.html?table=1`