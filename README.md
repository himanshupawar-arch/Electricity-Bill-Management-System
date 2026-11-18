# ⚡ Electricity Bill Management System (EBMS)

A desktop application built with **Java Swing** and **MySQL/MariaDB (JDBC)** to manage consumers, meter readings, slab-based electricity billing, payments, and overdue (defaulter) tracking.

Academic project, B.Tech Computer Science & Engineering, CGC University.

---

## 📌 Features

| # | Module | What it does |
|---|--------|--------------|
| 1 | **Consumer Registration** | Register Domestic or Commercial consumers (name, address, phone) and view all consumers in a table |
| 2 | **Meter Reading** | Enter the current reading; the previous reading is fetched automatically and validated |
| 3 | **Bill Calculation** | Generates a bill from a pending reading using a MySQL **stored procedure** (slab rates) |
| 4 | **Payment Recording** | Lists unpaid bills and marks the selected bill as **PAID** with the payment date |
| 5 | **Defaulter List** | Shows unpaid bills past their due date, with days overdue |

**Extras**
- Background thread keeps checking for overdue bills and shows a live alert on the main window
- Zero-setup database: the app creates the database, tables, stored procedure and sample data on first launch
- Auto-detects MySQL or MariaDB driver, port (3306/3307) and password, and downloads the JDBC driver once into `lib/`

---

## 💰 Tariff (Slab Rates)

Bill = **Fixed charge + slab charges**

| Consumer Type | Fixed Charge | 0 – 100 units | 101 – 300 units | Above 300 units |
|---------------|:-----------:|:-------------:|:---------------:|:---------------:|
| **Domestic**   | ₹50  | ₹3 / unit | ₹5 / unit | ₹7 / unit |
| **Commercial** | ₹150 | ₹6 / unit | ₹8 / unit | ₹10 / unit |

**Example:** Domestic consumer, 180 units
`50 + (100 × 3) + (80 × 5) = ₹750`

Due date is **15 days** after the bill date. An unpaid bill past its due date appears in the Defaulter List.

---

## 🛠️ Tech Stack

- **Language:** Java (JDK 11 or higher recommended)
- **GUI:** Java Swing (`JTabbedPane`, `JTable`, `JComboBox`, `JOptionPane`)
- **Database:** MySQL / MariaDB (XAMPP supported)
- **Connectivity:** JDBC (`PreparedStatement`, `CallableStatement`)
- **IDE:** VS Code (also works in IntelliJ IDEA and Eclipse)

---

## 📚 Syllabus Concepts Covered

| Unit | Concept | Where used |
|------|---------|-----------|
| 1 | Classes, objects, encapsulation | `Consumer`, `Meter`, `Bill` |
| 2 | Inheritance, abstract class, polymorphism, interface | `Consumer` (abstract) → `DomesticConsumer`, `CommercialConsumer`; `Payable` interface |
| 3 | User-defined exception, multithreading | `InvalidReadingException`, `DueDateAlertThread` |
| 4 | JDBC, `PreparedStatement`, `CallableStatement`, stored procedure | `Database.java`, `calculate_bill` procedure |
| 5 | Swing GUI and event handling | `MainFrame.java` |

---

## 📁 Project Structure

```
EBMS/
├── MainFrame.java              # Swing GUI (5 tabs) + entry point (main)
├── Database.java               # All JDBC code, auto-setup, driver/port detection
├── Consumer.java               # Abstract base class (implements Payable)
├── DomesticConsumer.java       # Domestic tariff
├── CommercialConsumer.java     # Commercial tariff
├── Meter.java                  # Meter reading + validation
├── Bill.java                   # Bill entity
├── Payable.java                # Interface for payment
├── InvalidReadingException.java# Custom checked exception
├── DueDateAlertThread.java     # Background overdue-bill checker
├── schema.sql                  # Reference / manual database setup
└── .vscode/                    # VS Code run + classpath settings

---

## 🗄️ Database Design

Database: **'ebms'**

```
consumers (consumer_id PK, name, address, phone, type)
    │ 1
    │
    ▼ N
readings  (reading_id PK, consumer_id FK, reading_date, previous_reading, current_reading)
    │ 1
    │
    ▼ 1
bills     (bill_id PK, consumer_id FK, reading_id FK UNIQUE, units, amount,
           bill_date, due_date, status [UNPAID/PAID], paid_date)
```

- One bill per reading (`reading_id` is `UNIQUE`)
- Stored procedure: `calculate_bill(IN reading_id, OUT bill_id, OUT units, OUT amount)`

---

## 🚀 How to Run

### Prerequisites
- JDK 11+ installed
- **XAMPP** (or any MySQL/MariaDB server) installed
- Internet connection on the **first run only** (to download the JDBC driver)

### Steps
1. Open the **XAMPP Control Panel** and click **Start** next to **MySQL**.
2. Open the `EBMS` folder in VS Code (with the *Extension Pack for Java* installed).
3. Open `MainFrame.java` and click **Run** (or press `F5`).

On first launch the app will automatically:
- download the JDBC driver into `lib/`
- detect MySQL/MariaDB on port `3306` or `3307` (password `root` or empty)
- create the `ebms` database, tables, `calculate_bill` procedure and sample data

### Run from terminal (optional)
```bash
cd EBMS
javac *.java
java MainFrame
```

### Custom database settings (optional)
Set these environment variables if your setup differs:

| Variable | Default |
|----------|---------|
| `EBMS_DB_HOST` | `localhost` |
| `EBMS_DB_PORT` | `3306` (then `3307`) |
| `EBMS_DB_USER` | `root` |
| `EBMS_DB_PASS` | `root` (empty is also tried) |

### Manual database setup (optional)
Import `schema.sql` using phpMyAdmin or the MySQL CLI:
```bash
mysql -u root -p < schema.sql
```
> ⚠️ `schema.sql` drops and recreates the tables. Use it only for a fresh setup.

---

## 🧪 Sample Data

A fresh install includes:
- **Ravi Kumar** (Domestic) with an already-overdue bill of ₹750, so the Defaulter List is not empty
- **Sharma Traders** (Commercial) with no bill yet, ready for a reading

---

## 🔄 Typical Workflow

1. **Register** a consumer (Tab 1)
2. **Enter a meter reading** (Tab 2); an invalid reading (negative, or lower than the previous one) is rejected with `InvalidReadingException`
3. **Calculate the bill** (Tab 3); the stored procedure computes the slab amount
4. **Record payment** (Tab 4)
5. Check the **Defaulter List** (Tab 5) for overdue bills

---

## 🩺 Troubleshooting

| Problem | Fix |
|---------|-----|
| *Cannot connect to the database* | Make sure MySQL is **started** in XAMPP |
| *Driver not found and download failed* | Connect to the internet once, or place a MySQL/MariaDB connector `.jar` in the `lib/` folder |
| Port conflict | Set `EBMS_DB_PORT` to your MySQL port |
| Access denied | Set `EBMS_DB_USER` and `EBMS_DB_PASS` |

---

