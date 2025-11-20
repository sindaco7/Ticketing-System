# CPS510 A9 – E-Ticket Reservation System (Oracle + Java)

The project presents:

- An e-ticket system with a schema in 3NF/BCNF (Users, Organizers, Venues, Events, Showtimes, Seats, Orders, Payments, SeatMaps, and Tickets).

- A Java **console UI** (`ETicketUI`) that provides a menu to:

 1. Drop Tables  

 2. Create Tables  

 3. Populate Tables (insert test data)  

 4. Query Tables (Events sub-menu: list/add/update/delete/search)  

 0. Exit

- A Java **Swing GUI** (`ETicketGUI`) that:

 - Prompts the user for credentials to log into the TMU Oracle server, by specifying host/port/SID/user/password.

 - Offers Drop/Create/Populate the tables.

 - Allows for **Query Tables (Events)** sub-menu items:

  - List Events

  - Add Event

  - Update Event Title

  - Delete Event

  - Search Events by Title

 - Has a dedicated search box for events by title at the bottom of the Event panels.


There is no dependency for a local already existing database-a connected application can **drop**, **re-create**, and **populate** all of the tables in the project with dummy data, so it is ready for queries, inserts, updates, and deletes. 


## Files

- `ETicketUI.java`: Console menu application.

- `ETicketGUI.java`: Swing GUI with Oracle login dialog and Events sub-menu.

- `ojdbc8.jar`: Oracle JDBC driver (must be on the classpath).

---

## Prerequisites 

The following are required prior to running or compiling the project:

- Java JDK installed version 8 or greater (available in PATH for both `javac` and `java`

- Working Oracle account on TMU server:

  - Host: `oracle.scs.ryerson.ca`

  - Port: `1521`

  - SID: `orcl`

- You will need to have `ojdbc8.jar` file located in the same directory as the `.java` files (or at least, somewhere you can reference on the classpath).

Assume that all files are located in a folder called, for example: `A9_java/`.

---

## For Compiling

Using a terminal or PowerShell in the project folder, use the following commands:

**Windows:**

```bash

javac -cp ".;ojdbc8.jar" ETicketUI.java ETicketGUI.java
