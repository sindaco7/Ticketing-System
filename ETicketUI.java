import java.sql.*;
import java.util.Scanner;

/**
 * CPS510 A9 – Java Menu UI for E-Ticket DB
 *
 * Main menu:
 *  1) Drop Tables
 *  2) Create Tables
 *  3) Populate Tables (dummy data)
 *  4) Query Tables (sub-menu for Events)
 *  0) Exit
 *
 * Query sub-menu (Events):
 *  - List events
 *  - Add event
 *  - Update event title
 *  - Delete event
 *  - Search events by title
 *
 * The schema and dummy data are based on our A6/A8 3NF/BCNF design
 * for the E-Ticket Reservation System:
 *  Users, Organizers, Venues, Events, Showtimes, Seats, SeatMaps,
 *  Orders, Payments, Tickets.
 *
 * At the bottom of this file:
 *  - listEvents(...) implements a basic report (projection + ordering).
 *  - searchEventsByTitle(...) implements a search/filter report using
 *    a LIKE condition (selection with user-supplied keyword).
 */
public class ETicketUI {

    // --- DB connection info for TMU Oracle ---
    private static final String URL  =
        "jdbc:oracle:thin:@oracle.scs.ryerson.ca:1521:orcl";
    private static final String USER = "akanaan";
    private static final String PASS = "01029927";

    public static void main(String[] args) {
        // Load Oracle JDBC driver
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            System.out.println("Driver loaded.");
        } catch (ClassNotFoundException e) {
            System.out.println("Could not load Oracle JDBC driver: " + e.getMessage());
            return;
        }

        // Connect and show main menu
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Scanner in = new Scanner(System.in)) {

            System.out.println("Connected to Oracle as: " + USER);

            boolean running = true;
            while (running) {
                printMainMenu();
                System.out.print("Choose option: ");
                String choice = in.nextLine().trim();

                switch (choice) {
                    case "1":
                        dropTables(conn);
                        break;
                    case "2":
                        createTables(conn);
                        break;
                    case "3":
                        populateTables(conn);
                        break;
                    case "4":
                        queryMenu(conn, in);
                        break;
                    case "0":
                        running = false;
                        System.out.println("Exiting. Bye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.out.println("Database error:");
            e.printStackTrace();
        }
    }

    // ================= MAIN MENU =================

    private static void printMainMenu() {
        System.out.println("===== CPS510 A9 – E-Ticket System =====");
        System.out.println("1. Drop Tables");
        System.out.println("2. Create Tables");
        System.out.println("3. Populate Tables (insert dummy data)");
        System.out.println("4. Query Tables (Events sub-menu)");
        System.out.println("0. Exit");
    }

    // ================== 1) DROP TABLES ==================
    /**
     * Drop all project tables in dependency order (children first).
     * This matches the A9 "Drop Tables" menu requirement.
     */
    private static void dropTables(Connection conn) {
        System.out.println("=== Dropping tables (if they exist) ===");

        // Drop in dependency order (children first)
        String[] drops = {
            "DROP TABLE Tickets CASCADE CONSTRAINTS",
            "DROP TABLE SeatMaps CASCADE CONSTRAINTS",
            "DROP TABLE Payments CASCADE CONSTRAINTS",
            "DROP TABLE Orders CASCADE CONSTRAINTS",
            "DROP TABLE Seats CASCADE CONSTRAINTS",
            "DROP TABLE Showtimes CASCADE CONSTRAINTS",
            "DROP TABLE Events CASCADE CONSTRAINTS",
            "DROP TABLE Venues CASCADE CONSTRAINTS",
            "DROP TABLE Organizers CASCADE CONSTRAINTS",
            "DROP TABLE Users CASCADE CONSTRAINTS"
        };

        for (String sql : drops) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("OK: " + sql);
            } catch (SQLException e) {
                // If table doesn't exist, just show message and continue
                System.out.println("Skip: " + sql + " (" + e.getMessage() + ")");
            }
        }

        System.out.println("Done dropping tables.");
    }

    // ================== 2) CREATE TABLES ==================
    /**
     * Create the 3NF/BCNF schema for the e-ticket reservation system.
     * This corresponds to the logical design from A6/A8.
     */
    private static void createTables(Connection conn) {
        System.out.println("=== Creating tables ===");

        try (Statement stmt = conn.createStatement()) {

            // USERS
            stmt.executeUpdate(
                "CREATE TABLE Users (" +
                "    UserID       NUMBER(10)      PRIMARY KEY," +
                "    FirstName    VARCHAR2(100)   NOT NULL," +
                "    LastName     VARCHAR2(100)   NOT NULL," +
                "    Email        VARCHAR2(255)   NOT NULL UNIQUE," +
                "    Phone        VARCHAR2(30)," +
                "    CreatedAt    DATE            DEFAULT SYSDATE NOT NULL" +
                ")"
            );

            // ORGANIZERS
            stmt.executeUpdate(
                "CREATE TABLE Organizers (" +
                "    OrganizerID   NUMBER(10)     PRIMARY KEY," +
                "    Name          VARCHAR2(200)  NOT NULL," +
                "    ContactEmail  VARCHAR2(255)," +
                "    ContactPhone  VARCHAR2(30)" +
                ")"
            );

            // VENUES
            stmt.executeUpdate(
                "CREATE TABLE Venues (" +
                "    VenueID   NUMBER(10)     PRIMARY KEY," +
                "    Name      VARCHAR2(200)  NOT NULL," +
                "    Address   VARCHAR2(300)," +
                "    City      VARCHAR2(120)," +
                "    Capacity  NUMBER(10)" +
                ")"
            );

            // EVENTS
            stmt.executeUpdate(
                "CREATE TABLE Events (" +
                "    EventID     NUMBER(10)    PRIMARY KEY," +
                "    OrganizerID NUMBER(10)    NOT NULL," +
                "    Title       VARCHAR2(200) NOT NULL," +
                "    Category    VARCHAR2(100)," +
                "    Description VARCHAR2(1000)," +
                "    CONSTRAINT fk_events_organizer" +
                "        FOREIGN KEY (OrganizerID)" +
                "        REFERENCES Organizers (OrganizerID)" +
                ")"
            );

            // SHOWTIMES
            stmt.executeUpdate(
                "CREATE TABLE Showtimes (" +
                "    ShowtimeID    NUMBER(10)     PRIMARY KEY," +
                "    EventID       NUMBER(10)     NOT NULL," +
                "    VenueID       NUMBER(10)     NOT NULL," +
                "    StartDateTime DATE           NOT NULL," +
                "    BasePrice     NUMBER(10,2)   NOT NULL," +
                "    CONSTRAINT fk_showtimes_event" +
                "        FOREIGN KEY (EventID)" +
                "        REFERENCES Events (EventID)," +
                "    CONSTRAINT fk_showtimes_venue" +
                "        FOREIGN KEY (VenueID)" +
                "        REFERENCES Venues (VenueID)," +
                "    CONSTRAINT uq_showtimes_event_venue_start" +
                "        UNIQUE (EventID, VenueID, StartDateTime)" +
                ")"
            );

            // SEATS
            stmt.executeUpdate(
                "CREATE TABLE Seats (" +
                "    SeatID     NUMBER(10)    PRIMARY KEY," +
                "    VenueID    NUMBER(10)    NOT NULL," +
                "    Section    VARCHAR2(50)  NOT NULL," +
                "    RowLabel   VARCHAR2(20)  NOT NULL," +
                "    SeatNumber VARCHAR2(20)  NOT NULL," +
                "    CONSTRAINT fk_seats_venue" +
                "        FOREIGN KEY (VenueID)" +
                "        REFERENCES Venues (VenueID)," +
                "    CONSTRAINT uq_venue_section_row_seat" +
                "        UNIQUE (VenueID, Section, RowLabel, SeatNumber)" +
                ")"
            );

            // ORDERS
            stmt.executeUpdate(
                "CREATE TABLE Orders (" +
                "    OrderID        NUMBER(10)     PRIMARY KEY," +
                "    UserID         NUMBER(10)     NOT NULL," +
                "    OrderDateTime  DATE           DEFAULT SYSDATE NOT NULL," +
                "    OrderTotal     NUMBER(10,2)   NOT NULL," +
                "    Status         VARCHAR2(20)   NOT NULL," +
                "    CONSTRAINT fk_orders_user" +
                "        FOREIGN KEY (UserID)" +
                "        REFERENCES Users (UserID)" +
                ")"
            );

            // PAYMENTS
            stmt.executeUpdate(
                "CREATE TABLE Payments (" +
                "    PaymentID  NUMBER(10)     PRIMARY KEY," +
                "    OrderID    NUMBER(10)     NOT NULL," +
                "    Amount     NUMBER(10,2)   NOT NULL," +
                "    Method     VARCHAR2(40)   NOT NULL," +
                "    PaidAt     DATE," +
                "    AuthCode   VARCHAR2(64)," +
                "    CONSTRAINT fk_payments_order" +
                "        FOREIGN KEY (OrderID)" +
                "        REFERENCES Orders (OrderID)" +
                ")"
            );

            // SEATMAPS
            stmt.executeUpdate(
                "CREATE TABLE SeatMaps (" +
                "    SeatMapID  NUMBER(10)     PRIMARY KEY," +
                "    ShowtimeID NUMBER(10)     NOT NULL," +
                "    SeatID     NUMBER(10)     NOT NULL," +
                "    Status     VARCHAR2(16)   NOT NULL," +
                "    CONSTRAINT fk_seatmaps_showtime" +
                "        FOREIGN KEY (ShowtimeID)" +
                "        REFERENCES Showtimes (ShowtimeID)," +
                "    CONSTRAINT fk_seatmaps_seat" +
                "        FOREIGN KEY (SeatID)" +
                "        REFERENCES Seats (SeatID)," +
                "    CONSTRAINT uq_seatmaps_showtime_seat" +
                "        UNIQUE (ShowtimeID, SeatID)," +
                "    CONSTRAINT chk_seatmaps_status" +
                "        CHECK (Status IN ('AVAILABLE', 'HELD', 'SOLD'))" +
                ")"
            );

            // TICKETS
            stmt.executeUpdate(
                "CREATE TABLE Tickets (" +
                "    TicketID     NUMBER(10)     PRIMARY KEY," +
                "    OrderID      NUMBER(10)     NOT NULL," +
                "    ShowtimeID   NUMBER(10)     NOT NULL," +
                "    SeatID       NUMBER(10)     NOT NULL," +
                "    TicketPrice  NUMBER(10,2)   NOT NULL," +
                "    QRCode       VARCHAR2(128)  NOT NULL," +
                "    IsValidated  CHAR(1)        DEFAULT 'N' NOT NULL," +
                "    ValidatedAt  DATE," +
                "    CONSTRAINT fk_tickets_order" +
                "        FOREIGN KEY (OrderID)" +
                "        REFERENCES Orders (OrderID)," +
                "    CONSTRAINT fk_tickets_showtime" +
                "        FOREIGN KEY (ShowtimeID)" +
                "        REFERENCES Showtimes (ShowtimeID)," +
                "    CONSTRAINT fk_tickets_seat" +
                "        FOREIGN KEY (SeatID)" +
                "        REFERENCES Seats (SeatID)," +
                "    CONSTRAINT uq_tickets_qrcode" +
                "        UNIQUE (QRCode)," +
                "    CONSTRAINT uq_tickets_showtime_seat" +
                "        UNIQUE (ShowtimeID, SeatID)," +
                "    CONSTRAINT chk_tickets_isvalidated" +
                "        CHECK (IsValidated IN ('Y','N'))" +
                ")"
            );

            System.out.println("All tables created successfully.");

        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }

    // ================== 3) POPULATE TABLES ==================
    /**
     * Insert a small set of dummy records into all main tables.
     *
     * This data set is used to demonstrate:
     *  - Basic CRUD operations on EVENTS (see queryMenu)
     *  - Simple reports over EVENTS (listEvents, searchEventsByTitle)
     *  - Potential extension to advanced reports, e.g.:
     *      * tickets sold / revenue per event (from Tickets / Orders)
     *      * seat availability per showtime (from SeatMaps)
     *
     * All inserts respect the 3NF/BCNF schema from A6/A8.
     */
    private static void populateTables(Connection conn) {
        System.out.println("=== Inserting dummy data into tables ===");

        try {
            conn.setAutoCommit(false); // group inserts in one transaction

            // USERS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Users (UserID, FirstName, LastName, Email, Phone) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, 1);
                ps.setString(2, "Ahmad");
                ps.setString(3, "Kanaan");
                ps.setString(4, "ahmad@example.com");
                ps.setString(5, "4161111111");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setString(2, "John");
                ps.setString(3, "Doe");
                ps.setString(4, "john@example.com");
                ps.setString(5, "4162222222");
                ps.executeUpdate();

                ps.setInt(1, 3);
                ps.setString(2, "Sarah");
                ps.setString(3, "Ali");
                ps.setString(4, "sarah@example.com");
                ps.setString(5, "6473333333");
                ps.executeUpdate();
            }

            // ORGANIZERS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Organizers (OrganizerID, Name, ContactEmail, ContactPhone) " +
                    "VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, 1);
                ps.setString(2, "Live Nation");
                ps.setString(3, "contact@livenation.com");
                ps.setString(4, "4165550000");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setString(2, "Cineplex");
                ps.setString(3, "info@cineplex.com");
                ps.setString(4, "4165551234");
                ps.executeUpdate();
            }

            // VENUES
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Venues (VenueID, Name, Address, City, Capacity) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, 1);
                ps.setString(2, "Scotiabank Arena");
                ps.setString(3, "40 Bay St");
                ps.setString(4, "Toronto");
                ps.setInt(5, 20000);
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setString(2, "Cineplex YD Square");
                ps.setString(3, "10 Dundas St E");
                ps.setString(4, "Toronto");
                ps.setInt(5, 500);
                ps.executeUpdate();
            }

            // EVENTS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Events (EventID, OrganizerID, Title, Category, Description) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setString(3, "Drake Live Concert");
                ps.setString(4, "Concert");
                ps.setString(5, "Drake performing live in Toronto.");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setInt(2, 2);
                ps.setString(3, "Avengers: Endgame");
                ps.setString(4, "Movie");
                ps.setString(5, "Special screening of Avengers Endgame.");
                ps.executeUpdate();
            }

            // SHOWTIMES
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Showtimes (ShowtimeID, EventID, VenueID, StartDateTime, BasePrice) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setInt(3, 1);
                ps.setTimestamp(4, Timestamp.valueOf("2025-12-10 20:00:00"));
                ps.setDouble(5, 150.00);
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setInt(2, 2);
                ps.setInt(3, 2);
                ps.setTimestamp(4, Timestamp.valueOf("2025-12-12 18:00:00"));
                ps.setDouble(5, 20.00);
                ps.executeUpdate();
            }

            // SEATS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Seats (SeatID, VenueID, Section, RowLabel, SeatNumber) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                // Venue 1: 3 seats
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setString(3, "Floor");
                ps.setString(4, "A");
                ps.setString(5, "1");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setInt(2, 1);
                ps.setString(3, "Floor");
                ps.setString(4, "A");
                ps.setString(5, "2");
                ps.executeUpdate();

                ps.setInt(1, 3);
                ps.setInt(2, 1);
                ps.setString(3, "Floor");
                ps.setString(4, "A");
                ps.setString(5, "3");
                ps.executeUpdate();

                // Venue 2: 2 seats
                ps.setInt(1, 4);
                ps.setInt(2, 2);
                ps.setString(3, "Front");
                ps.setString(4, "B");
                ps.setString(5, "5");
                ps.executeUpdate();

                ps.setInt(1, 5);
                ps.setInt(2, 2);
                ps.setString(3, "Front");
                ps.setString(4, "B");
                ps.setString(5, "6");
                ps.executeUpdate();
            }

            // SEATMAPS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO SeatMaps (SeatMapID, ShowtimeID, SeatID, Status) " +
                    "VALUES (?, ?, ?, ?)")) {
                // Showtime 1
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setInt(3, 1);
                ps.setString(4, "AVAILABLE");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setInt(2, 1);
                ps.setInt(3, 2);
                ps.setString(4, "AVAILABLE");
                ps.executeUpdate();

                ps.setInt(1, 3);
                ps.setInt(2, 1);
                ps.setInt(3, 3);
                ps.setString(4, "HELD");
                ps.executeUpdate();

                // Showtime 2
                ps.setInt(1, 4);
                ps.setInt(2, 2);
                ps.setInt(3, 4);
                ps.setString(4, "AVAILABLE");
                ps.executeUpdate();

                ps.setInt(1, 5);
                ps.setInt(2, 2);
                ps.setInt(3, 5);
                ps.setString(4, "SOLD");
                ps.executeUpdate();
            }

            // ORDERS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Orders (OrderID, UserID, OrderDateTime, OrderTotal, Status) " +
                    "VALUES (?, ?, SYSDATE, ?, ?)")) {
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setDouble(3, 150.00);
                ps.setString(4, "PAID");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setInt(2, 2);
                ps.setDouble(3, 20.00);
                ps.setString(4, "PAID");
                ps.executeUpdate();
            }

            // PAYMENTS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Payments (PaymentID, OrderID, Amount, Method, PaidAt, AuthCode) " +
                    "VALUES (?, ?, ?, ?, SYSDATE, ?)")) {
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setDouble(3, 150.00);
                ps.setString(4, "CARD");
                ps.setString(5, "AUTH12345");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setInt(2, 2);
                ps.setDouble(3, 20.00);
                ps.setString(4, "CARD");
                ps.setString(5, "AUTH67890");
                ps.executeUpdate();
            }

            // TICKETS
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Tickets (TicketID, OrderID, ShowtimeID, SeatID, TicketPrice, QRCode, IsValidated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setInt(3, 1);
                ps.setInt(4, 1);
                ps.setDouble(5, 150.00);
                ps.setString(6, "QR-ABC-111");
                ps.setString(7, "N");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setInt(2, 2);
                ps.setInt(3, 2);
                ps.setInt(4, 5);
                ps.setDouble(5, 20.00);
                ps.setString(6, "QR-XYZ-222");
                ps.setString(7, "Y");
                ps.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("Dummy data inserted successfully.");

        } catch (SQLException e) {
            System.out.println("Error populating tables: " + e.getMessage());
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ex2) {
                System.out.println("Rollback error: " + ex2.getMessage());
            }
        }
    }

    // ================== 4) QUERY MENU (EVENTS) ==================
    /**
     * Sub-menu that focuses on CRUD and simple reports over EVENTS.
     * This covers:
     *  - Read (listEvents, searchEventsByTitle)
     *  - Create (addEvent)
     *  - Update (updateEventTitle)
     *  - Delete (deleteEvent)
     */
    private static void queryMenu(Connection conn, Scanner in) {
        boolean back = false;
        while (!back) {
            System.out.println("=== Query Menu (Events) ===");
            System.out.println("1. List Events");
            System.out.println("2. Add Event");
            System.out.println("3. Update Event Title");
            System.out.println("4. Delete Event");
            System.out.println("5. Search Events by Title");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose option: ");
            String choice = in.nextLine().trim();

            switch (choice) {
                case "1":
                    listEvents(conn);
                    break;
                case "2":
                    addEvent(conn, in);
                    break;
                case "3":
                    updateEventTitle(conn, in);
                    break;
                case "4":
                    deleteEvent(conn, in);
                    break;
                case "5":
                    searchEventsByTitle(conn, in);
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
            System.out.println();
        }
    }

    // ---- Query helpers on EVENTS ----

    /**
     * Simple report #1 (projection + ordering):
     * Lists all events (EventID, Title, Category) ordered by EventID.
     *
     * This corresponds to a basic SELECT / PROJECT query over EVENTS
     * and is used in the A9 demo to show that the application can
     * read from the database and present event information to the user.
     */
    private static void listEvents(Connection conn) {
        String sql = "SELECT EventID, Title, Category FROM Events ORDER BY EventID";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("EventID | Title | Category");
            System.out.println("--------------------------------------");

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                int id = rs.getInt("EventID");
                String title = rs.getString("Title");
                String cat = rs.getString("Category");
                System.out.println(id + " | " + title + " | " + cat);
            }

            if (!hasRows) {
                System.out.println("(No rows found in Events table)");
            }

        } catch (SQLException e) {
            System.out.println("Error listing events: " + e.getMessage());
        }
    }

    /**
     * Insert a new row into EVENTS (Create in CRUD).
     * Uses a parameterized INSERT to avoid SQL injection.
     */
    private static void addEvent(Connection conn, Scanner in) {
        try {
            System.out.println("=== Add New Event ===");
            System.out.print("New EventID (integer, must be unique): ");
            int eventId = Integer.parseInt(in.nextLine().trim());

            System.out.print("OrganizerID (must exist in ORGANIZERS): ");
            int organizerId = Integer.parseInt(in.nextLine().trim());

            System.out.print("Title: ");
            String title = in.nextLine().trim();

            System.out.print("Category (e.g., Concert, Movie): ");
            String category = in.nextLine().trim();

            System.out.print("Description (can be empty): ");
            String description = in.nextLine().trim();

            String sql = "INSERT INTO Events (EventID, OrganizerID, Title, Category, Description) " +
                         "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, eventId);
                ps.setInt(2, organizerId);
                ps.setString(3, title);
                ps.setString(4, category);
                ps.setString(5, description);

                int rows = ps.executeUpdate();
                System.out.println("Inserted " + rows + " row(s) into EVENTS.");
            }

        } catch (NumberFormatException ex) {
            System.out.println("Invalid number input. Event not added.");
        } catch (SQLException e) {
            System.out.println("Error inserting event: " + e.getMessage());
        }
    }

    /**
     * Update the Title attribute for an existing EVENTS row (Update in CRUD).
     */
    private static void updateEventTitle(Connection conn, Scanner in) {
        try {
            System.out.println("=== Update Event Title ===");
            System.out.print("EventID to update: ");
            int eventId = Integer.parseInt(in.nextLine().trim());

            System.out.print("New Title: ");
            String newTitle = in.nextLine().trim();

            String sql = "UPDATE Events SET Title = ? WHERE EventID = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newTitle);
                ps.setInt(2, eventId);

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    System.out.println("No event found with EventID = " + eventId);
                } else {
                    System.out.println("Updated " + rows + " row(s).");
                }
            }

        } catch (NumberFormatException ex) {
            System.out.println("Invalid number input. Nothing updated.");
        } catch (SQLException e) {
            System.out.println("Error updating event: " + e.getMessage());
        }
    }

    /**
     * Delete an existing EVENTS row by primary key (Delete in CRUD).
     * If there are foreign-key references (e.g., SHOWTIMES), Oracle
     * will raise an error, which we catch and display.
     */
    private static void deleteEvent(Connection conn, Scanner in) {
        try {
            System.out.println("=== Delete Event ===");
            System.out.print("EventID to delete: ");
            int eventId = Integer.parseInt(in.nextLine().trim());

            String sql = "DELETE FROM Events WHERE EventID = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, eventId);

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    System.out.println("No event found with EventID = " + eventId);
                } else {
                    System.out.println("Deleted " + rows + " row(s).");
                }
            }

        } catch (NumberFormatException ex) {
            System.out.println("Invalid number input. Nothing deleted.");
        } catch (SQLException e) {
            System.out.println("Error deleting event (maybe FK constraints): " + e.getMessage());
        }
    }

    /**
     * Simple report #2 (search / filtering):
     * Allows the user to search events by a keyword in the Title.
     *
     * This implements a selection with a LIKE condition:
     *   SELECT EventID, Title, Category
     *   FROM   Events
     *   WHERE  LOWER(Title) LIKE '%keyword%'
     *
     * It demonstrates a more “advanced” query than listEvents,
     * because it includes user input, case-insensitive matching,
     * and dynamic filtering of the result set.
     */
    private static void searchEventsByTitle(Connection conn, Scanner in) {
        System.out.println("=== Search Events by Title ===");
        System.out.print("Enter keyword: ");
        String keyword = in.nextLine().trim().toLowerCase();

        String sql = "SELECT EventID, Title, Category " +
                     "FROM Events " +
                     "WHERE LOWER(Title) LIKE ? " +
                     "ORDER BY EventID";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("EventID | Title | Category");
                System.out.println("--------------------------------------");

                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    int id = rs.getInt("EventID");
                    String title = rs.getString("Title");
                    String cat = rs.getString("Category");
                    System.out.println(id + " | " + title + " | " + cat);
                }

                if (!hasRows) {
                    System.out.println("(No events match that keyword)");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error searching events: " + e.getMessage());
        }
    }
}
