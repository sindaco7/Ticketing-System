import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * CPS510 A9 – Swing UI for E-Ticket DB
 *
 * This GUI demonstrates:
 *  - Logging into TMU Oracle DB with user-entered credentials
 *  - Dropping / creating / populating tables
 *  - Query Tables (Events sub-menu) with:
 *      * List Events
 *      * Add Event
 *      * Update Event Title
 *      * Delete Event
 *      * Search Events by Title
 *
 * It mirrors the console menu structure:
 *  1) Drop Tables
 *  2) Create Tables
 *  3) Populate Tables
 *  4) Query Tables (Events sub-menu)
 *  0) Exit
 */
public class ETicketGUI extends JFrame {

    // Connection is passed in from the login dialog / main()
    private Connection conn;
    private JTextArea outputArea;
    private JTextField searchField;
    private JPanel buttonPanel;

    /**
     * Main GUI constructor: we receive an already-open Oracle Connection
     * plus the username (for display in the window title).
     */
    public ETicketGUI(Connection conn, String currentUser) {
        this.conn = conn;

        // ===== Window setup =====
        setTitle("CPS510 E-Ticket System – Java UI (User: " + currentUser + ")");
        setSize(800, 500);
        setLocationRelativeTo(null); // center on screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // ===== Output area (acts like console) =====
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(outputArea);
        add(scroll, BorderLayout.CENTER);

        // ===== Top button panel (mirrors main menu) =====
        buttonPanel = new JPanel();
        // 5 main actions: Drop / Create / Populate / Query Tables / Exit
        buttonPanel.setLayout(new GridLayout(1, 5, 6, 6));

        JButton btnDrop     = new JButton("Drop Tables");
        JButton btnCreate   = new JButton("Create Tables");
        JButton btnPopulate = new JButton("Populate Dummy Data");
        JButton btnQuery    = new JButton("Query Tables (Events)");
        JButton btnExit     = new JButton("Exit");

        buttonPanel.add(btnDrop);
        buttonPanel.add(btnCreate);
        buttonPanel.add(btnPopulate);
        buttonPanel.add(btnQuery);
        buttonPanel.add(btnExit);

        add(buttonPanel, BorderLayout.NORTH);

        // ===== Bottom search panel (simple direct search) =====
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField();
        JButton btnSearchGo = new JButton("Search");

        searchPanel.add(new JLabel("Search title keyword:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(btnSearchGo, BorderLayout.EAST);

        add(searchPanel, BorderLayout.SOUTH);

        // Connection is already open at this point
        appendLine("Connected to Oracle as: " + currentUser);

        // ===== Wire button actions =====
        btnDrop.addActionListener(e -> dropTables());
        btnCreate.addActionListener(e -> createTables());
        btnPopulate.addActionListener(e -> populateTables());

        // New: Query Tables button opens the Query Menu (Events sub-menu)
        btnQuery.addActionListener(e -> showQueryMenu());

        // Bottom "Search" executes the filtered query using the search box
        btnSearchGo.addActionListener(e -> searchEvents());

        // Exit button closes DB connection and app
        btnExit.addActionListener(e -> {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ex) {
                // ignore
            }
            System.exit(0);
        });
    }

    // ============== Small helper methods ==============

    private void appendLine(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void setButtonsEnabled(boolean enabled) {
        for (Component c : buttonPanel.getComponents()) {
            c.setEnabled(enabled);
        }
    }

    // ============== Query Menu (Events sub-menu) ==============

    /**
     * This replicates the console "Query Menu (Events)" sub-menu
     * using a Swing dialog with buttons for each operation.
     */
    private void showQueryMenu() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        boolean done = false;
        while (!done) {
            String[] options = {
                    "List Events",
                    "Add Event",
                    "Update Event Title",
                    "Delete Event",
                    "Search Events by Title",
                    "Back"
            };

            int choice = JOptionPane.showOptionDialog(
                    this,
                    "=== Query Menu (Events) ===",
                    "Query Tables – Events",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 0) {
                listEvents();
            } else if (choice == 1) {
                addEvent();
            } else if (choice == 2) {
                updateEventTitle();
            } else if (choice == 3) {
                deleteEvent();
            } else if (choice == 4) {
                promptSearchEvents();
            } else {
                // Back or dialog closed
                done = true;
            }
        }
    }

    // ============== 1) Drop Tables ==============

    /**
     * Drop all project tables in dependency order (children first).
     * Same logic as the console version, but writes to the UI text area.
     */
    private void dropTables() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        appendLine("=== Dropping tables (if they exist) ===");

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
                appendLine("OK: " + sql);
            } catch (SQLException e) {
                appendLine("Skip: " + sql + " (" + e.getMessage() + ")");
            }
        }

        appendLine("Done dropping tables.");
    }

    // ============== 2) Create Tables ==============

    /**
     * Create the 3NF/BCNF schema for the e-ticket system.
     * This is the same schema used in the console app.
     */
    private void createTables() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        appendLine("=== Creating tables ===");

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

            appendLine("All tables created successfully.");

        } catch (SQLException e) {
            appendLine("Error creating tables: " + e.getMessage());
        }
    }

    // ============== 3) Populate Tables ==============

    /**
     * Insert the same dummy records as in the console program.
     * This gives us data to show in the simple UI reports.
     */
    private void populateTables() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        appendLine("=== Inserting dummy data into tables ===");

        try {
            conn.setAutoCommit(false);

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
            appendLine("Dummy data inserted successfully.");

        } catch (SQLException e) {
            appendLine("Error populating tables: " + e.getMessage());
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ex2) {
                appendLine("Rollback error: " + ex2.getMessage());
            }
        }
    }

    // ============== 4) Simple reports & CRUD on EVENTS ==============

    /**
     * Simple report #1: list all events (EventID, Title, Category).
     */
    private void listEvents() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        appendLine("=== Events ===");
        String sql = "SELECT EventID, Title, Category FROM Events ORDER BY EventID";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                int id = rs.getInt("EventID");
                String title = rs.getString("Title");
                String cat = rs.getString("Category");
                appendLine(id + " | " + title + " | " + cat);
            }

            if (!hasRows) {
                appendLine("(No rows found in Events table)");
            }

        } catch (SQLException e) {
            appendLine("Error listing events: " + e.getMessage());
        }
    }

    /**
     * Add a new event (interactive prompts via dialogs).
     */
    private void addEvent() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        try {
            String idStr = JOptionPane.showInputDialog(
                    this,
                    "New EventID (integer, must be unique):",
                    "Add Event",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (idStr == null) return; // cancelled
            int eventId = Integer.parseInt(idStr.trim());

            String orgStr = JOptionPane.showInputDialog(
                    this,
                    "OrganizerID (must exist in ORGANIZERS):",
                    "Add Event",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (orgStr == null) return;
            int organizerId = Integer.parseInt(orgStr.trim());

            String title = JOptionPane.showInputDialog(
                    this,
                    "Title:",
                    "Add Event",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (title == null) return;
            title = title.trim();

            String category = JOptionPane.showInputDialog(
                    this,
                    "Category (e.g., Concert, Movie):",
                    "Add Event",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (category == null) category = "";
            category = category.trim();

            String description = JOptionPane.showInputDialog(
                    this,
                    "Description (can be blank):",
                    "Add Event",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (description == null) description = "";
            description = description.trim();

            String sql = "INSERT INTO Events (EventID, OrganizerID, Title, Category, Description) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, eventId);
                ps.setInt(2, organizerId);
                ps.setString(3, title);
                ps.setString(4, category);
                ps.setString(5, description);

                int rows = ps.executeUpdate();
                appendLine("Inserted " + rows + " row(s) into EVENTS.");
            }

        } catch (NumberFormatException ex) {
            appendLine("Invalid number input. Event not added.");
        } catch (SQLException e) {
            appendLine("Error inserting event: " + e.getMessage());
        }
    }

    /**
     * Update event title by EventID (interactive).
     */
    private void updateEventTitle() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        try {
            String idStr = JOptionPane.showInputDialog(
                    this,
                    "EventID to update:",
                    "Update Event Title",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (idStr == null) return;
            int eventId = Integer.parseInt(idStr.trim());

            String newTitle = JOptionPane.showInputDialog(
                    this,
                    "New Title:",
                    "Update Event Title",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (newTitle == null) return;
            newTitle = newTitle.trim();

            String sql = "UPDATE Events SET Title = ? WHERE EventID = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newTitle);
                ps.setInt(2, eventId);

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    appendLine("No event found with EventID = " + eventId);
                } else {
                    appendLine("Updated " + rows + " row(s).");
                }
            }

        } catch (NumberFormatException ex) {
            appendLine("Invalid number input. Nothing updated.");
        } catch (SQLException e) {
            appendLine("Error updating event: " + e.getMessage());
        }
    }

    /**
     * Delete event by EventID (interactive, with confirmation).
     */
    private void deleteEvent() {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        try {
            String idStr = JOptionPane.showInputDialog(
                    this,
                    "EventID to delete:",
                    "Delete Event",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (idStr == null) return;
            int eventId = Integer.parseInt(idStr.trim());

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete EventID = " + eventId + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                appendLine("Delete cancelled.");
                return;
            }

            String sql = "DELETE FROM Events WHERE EventID = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, eventId);

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    appendLine("No event found with EventID = " + eventId);
                } else {
                    appendLine("Deleted " + rows + " row(s).");
                }
            }

        } catch (NumberFormatException ex) {
            appendLine("Invalid number input. Nothing deleted.");
        } catch (SQLException e) {
            appendLine("Error deleting event (maybe FK constraints): " + e.getMessage());
        }
    }

    /**
     * Helper used by Query Menu "Search Events by Title":
     * prompts for keyword and then calls the search logic.
     */
    private void promptSearchEvents() {
        String keyword = JOptionPane.showInputDialog(
                this,
                "Keyword to search in Title (case-insensitive):",
                "Search Events",
                JOptionPane.QUESTION_MESSAGE
        );
        if (keyword == null) return; // cancelled
        searchEventsByKeyword(keyword);
    }

    /**
     * Bottom search box handler – gets keyword from the text field.
     */
    private void searchEvents() {
        String keyword = searchField.getText();
        searchEventsByKeyword(keyword);
    }

    /**
     * Core search implementation (used both by bottom search box
     * and by the Query Menu "Search Events by Title").
     */
    private void searchEventsByKeyword(String keywordRaw) {
        if (conn == null) {
            appendLine("No DB connection.");
            return;
        }

        String keyword = (keywordRaw == null ? "" : keywordRaw).trim().toLowerCase();
        if (keyword.isEmpty()) {
            appendLine("Please type a keyword in the search box or dialog.");
            return;
        }

        appendLine("=== Search Events: \"" + keyword + "\" ===");

        String sql = "SELECT EventID, Title, Category " +
                "FROM Events " +
                "WHERE LOWER(Title) LIKE ? " +
                "ORDER BY EventID";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    int id = rs.getInt("EventID");
                    String title = rs.getString("Title");
                    String cat = rs.getString("Category");
                    appendLine(id + " | " + title + " | " + cat);
                }

                if (!hasRows) {
                    appendLine("(No events match that keyword)");
                }
            }

        } catch (SQLException e) {
            appendLine("Error searching events: " + e.getMessage());
        }
    }

    // ============== Login dialog ==============

    /**
     * Small modal dialog that asks the user for Oracle connection info
     * (host, port, SID, username, password).
     *
     * Defaults for TMU:
     *  - Host: oracle.scs.ryerson.ca
     *  - Port: 1521
     *  - SID:  orcl
     */
    private static class LoginDialog extends JDialog {
        private JTextField hostField;
        private JTextField portField;
        private JTextField sidField;
        private JTextField userField;
        private JPasswordField passField;
        private boolean succeeded = false;

        public LoginDialog(Frame parent) {
            super(parent, "Connect to Oracle", true);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            hostField = new JTextField("oracle.scs.ryerson.ca", 20);
            portField = new JTextField("1521", 6);
            sidField  = new JTextField("orcl", 10);
            userField = new JTextField("", 15);
            passField = new JPasswordField("", 15);

            int row = 0;

            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel("Host:"), gbc);
            gbc.gridx = 1;
            panel.add(hostField, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel("Port:"), gbc);
            gbc.gridx = 1;
            panel.add(portField, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel("SID:"), gbc);
            gbc.gridx = 1;
            panel.add(sidField, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel("Username:"), gbc);
            gbc.gridx = 1;
            panel.add(userField, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            panel.add(passField, gbc);
            row++;

            JButton btnConnect = new JButton("Connect");
            JButton btnCancel  = new JButton("Cancel");

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.add(btnConnect);
            buttons.add(btnCancel);

            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 2;
            panel.add(buttons, gbc);

            getContentPane().add(panel);
            pack();
            setLocationRelativeTo(parent);

            btnConnect.addActionListener(e -> {
                if (userField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Username is required.",
                            "Input Error",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                succeeded = true;
                dispose();
            });

            btnCancel.addActionListener(e -> {
                succeeded = false;
                dispose();
            });
        }

        public boolean isSucceeded() { return succeeded; }

        public String getHost() { return hostField.getText().trim(); }
        public String getPort() { return portField.getText().trim(); }
        public String getSid()  { return sidField.getText().trim(); }
        public String getUsername() { return userField.getText().trim(); }
        public String getPassword() { return new String(passField.getPassword()); }
    }

    // ============== main() to show login + launch the GUI ==============

    public static void main(String[] args) {
        // Optional: nicer look on Windows / macOS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> {
            // 1) Ask user for DB credentials
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);

            if (!login.isSucceeded()) {
                // User cancelled the login dialog
                System.exit(0);
            }

            Connection conn = null;
            try {
                // Load Oracle JDBC driver
                Class.forName("oracle.jdbc.driver.OracleDriver");

                String host = login.getHost();
                String port = login.getPort();
                String sid  = login.getSid();
                String user = login.getUsername();
                String pass = login.getPassword();

                String url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;

                conn = DriverManager.getConnection(url, user, pass);

                // 2) Launch the main GUI with this connection
                ETicketGUI gui = new ETicketGUI(conn, user);
                gui.setVisible(true);

            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not load Oracle JDBC driver:\n" + e.getMessage(),
                        "Driver Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Failed to connect to Oracle:\n" + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
                if (conn != null) {
                    try { conn.close(); } catch (SQLException ignored2) {}
                }
                System.exit(1);
            }
        });
    }
}
