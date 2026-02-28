import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;

public class ATMInterfaceGUI extends JFrame {
    static class Account {
        String accountNumber; // corresponds to accounts.account_number (primary key)
        String cardNo;        // corresponds to accounts.card_no (physcial card identifier)
        String pin;
        double balance;
        String ifscCode;
        String address;
        String name;
        java.util.List<String> transactionHistory = new ArrayList<>();

        public Account(String accountNumber, String cardNo, String pin, double balance, String name, String ifscCode, String address) {
            this.accountNumber = accountNumber;
            this.cardNo = cardNo;
            this.pin = pin;
            this.balance = balance;
            this.name = name;
            this.ifscCode = ifscCode;
            this.address = address;
        }

        // Accept either account number or physical card number for authentication
        public boolean authenticate(String inputCardOrAccount, String inputPin) {
            if (!pin.equals(inputPin)) return false;
            return accountNumber.equals(inputCardOrAccount) || cardNo.equals(inputCardOrAccount);
        }

        public void addTransaction(String transaction) {
            transactionHistory.add(transaction);
        }
    }

    private static Map<String, Account> accounts = new HashMap<>();
    private static final double MINIMUM_BALANCE = 100.0;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Login components
    private JTextField cardNumberField;
    private JPasswordField pinField;
    private JLabel loginMessageLabel;

    // Welcome screen
    private JLabel welcomeLabel;

    // Main menu components
    private JLabel balanceLabel;
    private JLabel dateTimeLabel;
    private Account currentUser;

    public ATMInterfaceGUI() {
        setTitle("Anywhere ATM");
        // Increase default UI font sizes more so the app scales better on fullscreen
        increaseGlobalFont(1.5f);
        // Start maximized so the larger fonts fill the screen
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Load accounts from DB (falls back to in-memory defaults on error)
        loadAccountsFromDB();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "login");
        mainPanel.add(createWelcomePanel(), "welcome");
        mainPanel.add(createMenuPanel(), "menu");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 144, 255)); // Dodger Blue
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel titleLabel = new JLabel("Anywhere ATM", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        // skip subtitle to keep a clean login screen
        gbc.gridwidth = 1;
        gbc.gridy++;

        panel.add(new JLabel("Card Number:"), gbc);
        cardNumberField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(cardNumberField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("PIN:"), gbc);
        pinField = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(pinField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        loginMessageLabel = new JLabel(" ", SwingConstants.CENTER);
        loginMessageLabel.setForeground(Color.RED);
        panel.add(loginMessageLabel, gbc);

        gbc.gridy++;
        JPanel buttonWrap = new JPanel();
        buttonWrap.setOpaque(false);
        JButton loginButton = new JButton("Login");
        buttonWrap.add(loginButton);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(buttonWrap, gbc);

        loginButton.addActionListener(e -> { e.hashCode(); authenticateUser(); });

        return panel;
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        welcomeLabel = new JLabel("Welcome!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(welcomeLabel, BorderLayout.CENTER);

        JButton continueBtn = new JButton("Continue");
        panel.add(continueBtn, BorderLayout.SOUTH);

        continueBtn.addActionListener(e -> { e.hashCode(); updateDateTimeLabel(); updateBalanceLabel(); cardLayout.show(mainPanel, "menu"); });

        return panel;
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel menuLabel = new JLabel("ATM Main Menu", SwingConstants.CENTER);
        menuLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        dateTimeLabel = new JLabel("", SwingConstants.CENTER);
        dateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel northPanel = new JPanel(new GridLayout(2, 1));
        northPanel.add(menuLabel);
        northPanel.add(dateTimeLabel);

        panel.add(northPanel, BorderLayout.NORTH);

        // Use 3 rows x 2 columns for a compact, readable layout
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 10, 10));

        JButton transferBtn = new JButton("Transfer Funds");
        JButton checkBalanceBtn = new JButton("Check Balance");
        JButton accountDetailsBtn = new JButton("Account Details");
        JButton changePinBtn = new JButton("Change PIN");
        JButton transactionHistoryBtn = new JButton("Transaction History");
        JButton logoutBtn = new JButton("Logout");

        buttonPanel.add(transferBtn);
        buttonPanel.add(checkBalanceBtn);
        buttonPanel.add(accountDetailsBtn);
        buttonPanel.add(changePinBtn);
        buttonPanel.add(transactionHistoryBtn);
        buttonPanel.add(logoutBtn);

        panel.add(buttonPanel, BorderLayout.CENTER);

        balanceLabel = new JLabel("Balance: $0.00", SwingConstants.CENTER);
        balanceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        // balance label intentionally not added to UI (removed from main menu per request)

        transferBtn.addActionListener(e -> { e.hashCode(); transferFunds(); });
        checkBalanceBtn.addActionListener(e -> { e.hashCode(); checkBalance(); });
        accountDetailsBtn.addActionListener(e -> { e.hashCode(); showAccountDetails(); });
        changePinBtn.addActionListener(e -> { e.hashCode(); changePin(); });
        transactionHistoryBtn.addActionListener(e -> { e.hashCode(); showTransactionHistory(); });
        logoutBtn.addActionListener(e -> { e.hashCode(); logout(); });

        return panel;
    }

    private void authenticateUser() {
        String input = cardNumberField.getText().trim();
        String pin = new String(pinField.getPassword()).trim();
        // prefer login by physical card number (card_no), fall back to account_number
        Account account = null;
        for (Account a : accounts.values()) {
            if (a.cardNo != null && a.cardNo.equals(input)) {
                account = a;
                break;
            }
        }
        // fallback: maybe the user pasted an account number
        if (account == null) account = accounts.get(input);

        if (account != null && account.authenticate(input, pin)) {
            currentUser = account;
            loginMessageLabel.setText(" ");
            cardNumberField.setText("");
            pinField.setText("");
            welcomeLabel.setText("Welcome, " + currentUser.name);
            cardLayout.show(mainPanel, "welcome");
        } else {
            loginMessageLabel.setText("Invalid card number or PIN.");
        }
    }

    private void updateBalanceLabel() {
        balanceLabel.setText(String.format("Balance: $%.2f", currentUser.balance));
        if (currentUser.balance < MINIMUM_BALANCE) {
            JOptionPane.showMessageDialog(this,
                    "⚠️ Warning: Your balance is below the minimum required $" + MINIMUM_BALANCE,
                    "Low Balance Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateDateTimeLabel() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        dateTimeLabel.setText("Current Date & Time: " + dtf.format(now));
    }

    private String generateReceiptNumber() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String getCurrentTimestamp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return dtf.format(LocalDateTime.now());
    }

    private void transferFunds() {
        String recipientAccount = JOptionPane.showInputDialog(this, "Enter the account number:");
        if (recipientAccount == null) return;
        Account recipient = accounts.get(recipientAccount.trim());
        if (recipient == null) {
            JOptionPane.showMessageDialog(this, "Recipient account not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Enter transfer amount:");
        if (input == null) return;
        try {
            double amount = Double.parseDouble(input);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (currentUser.balance >= amount) {
                currentUser.balance -= amount;
                recipient.balance += amount;
                String receipt = generateReceiptNumber();
                String timestamp = getCurrentTimestamp();
                currentUser.addTransaction("Receipt#" + receipt + " [" + timestamp + "]: Transferred $" + amount + " to " + recipient.accountNumber);
                recipient.addTransaction("Receipt#" + receipt + " [" + timestamp + "]: Received $" + amount + " from " + currentUser.accountNumber);
                // Persist balances and transaction record
                persistAccountBalance(currentUser);
                persistAccountBalance(recipient);
                insertTransaction(receipt, currentUser.accountNumber, recipient.accountNumber, amount, "Transfer");
                JOptionPane.showMessageDialog(this, "Transferred $" + amount + " to " + recipient.accountNumber + "\nReceipt: " + receipt);
                updateBalanceLabel();
            } else {
                JOptionPane.showMessageDialog(this, "Insufficient funds.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTransactionHistory() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "No user is currently logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Recent DB transactions:\n");
        // fetch transactions where user is from_card or to_card
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT receipt, occurred_at, from_card, to_card, amount, description FROM transactions WHERE from_card = ? OR to_card = ? ORDER BY occurred_at DESC LIMIT 50")) {
            ps.setString(1, currentUser.accountNumber);
            ps.setString(2, currentUser.accountNumber);
            ResultSet rs = ps.executeQuery();
            boolean any = false;
            while (rs.next()) {
                any = true;
                String receipt = rs.getString("receipt");
                Timestamp when = rs.getTimestamp("occurred_at");
                String from = rs.getString("from_card");
                String to = rs.getString("to_card");
                double amount = rs.getDouble("amount");
                String desc = rs.getString("description");
                sb.append(String.format("%s [%s]: %s %s->%s $%.2f\n", receipt, when, desc, from, to, amount));
            }
            if (!any) sb.append("  (no DB transactions)\n");
        } catch (SQLException ex) {
            sb.append("Failed to load DB transactions: ").append(ex.getMessage()).append("\n");
        }

        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(500, 300));
        JOptionPane.showMessageDialog(this, sp, "Transaction History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void checkBalance() {
        JOptionPane.showMessageDialog(this, String.format("Your current balance is $%.2f", currentUser.balance));
        updateBalanceLabel();
    }

    private void showAccountDetails() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "No user logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Require PIN entry before showing sensitive account details (masked)
        String inputPin = promptForMaskedPin("Enter your PIN to view account details:");
        if (inputPin == null) return; // user cancelled
        if (!currentUser.pin.equals(inputPin.trim())) {
            JOptionPane.showMessageDialog(this, "Invalid PIN.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(currentUser.name).append("\n\n");
        sb.append("Account Number: ").append(currentUser.accountNumber).append("\n");
        sb.append("Card No: ").append(currentUser.cardNo).append("\n");
        sb.append("IFSC Code: ").append(currentUser.ifscCode).append("\n");
        sb.append("Address: ").append(currentUser.address == null ? "(not provided)" : currentUser.address).append("\n");
        sb.append(String.format("\nBalance: $%.2f", currentUser.balance));
        JOptionPane.showMessageDialog(this, sb.toString(), "Account Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // showAccountDetails will require PIN confirmation now

    private void changePin() {
        // Prompt for the physical card number (card_no in DB)
        String input = JOptionPane.showInputDialog(this, "Enter your card no. to set/reset PIN:");
        if (input == null || input.trim().isEmpty()) return;
        String key = input.trim();

        // Try direct lookup by map key first (map keys are account numbers), then by cardNo
        Account account = accounts.get(key);
        if (account == null) {
            for (Account a : accounts.values()) {
                if (a.cardNo != null && a.cardNo.equals(key)) {
                    account = a;
                    break;
                }
            }
        }

        if (account == null) {
            JOptionPane.showMessageDialog(this, "Account not found for that card no.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newPin = promptForMaskedPin("Enter new PIN for account " + account.accountNumber + ":");
        if (newPin == null || newPin.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "PIN cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to change the PIN for account " + account.accountNumber + "?",
            "Confirm PIN Change",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (ok != JOptionPane.YES_OPTION) return;

        account.pin = newPin.trim();
        // update in DB
        updateAccountPin(account);
        JOptionPane.showMessageDialog(this, "PIN updated successfully for account " + account.accountNumber + ".");
    }

    private void loadAccountsFromDB() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT account_number, card_no, pin, name, ifsc_code, balance FROM accounts")) {
            ResultSet rs = ps.executeQuery();
            int loaded = 0;
            while (rs.next()) {
                String account = rs.getString("account_number");
                String cardNo = rs.getString("card_no");
                String pin = rs.getString("pin");
                String name = rs.getString("name");
                String ifsc = rs.getString("ifsc_code");
                String address = null;
                // address may be absent from some schemas; leave null unless the column exists
                if (hasColumn(rs, "address")) {
                    try {
                        address = rs.getString("address");
                    } catch (SQLException ignored) {
                        // ignore and leave address null
                    }
                }
                double balance = rs.getDouble("balance");
                accounts.put(account, new Account(account, cardNo, pin, balance, name, ifsc, address));
                loaded++;
            }
            System.out.println("Loaded " + loaded + " accounts from DB.");
        } catch (SQLException ex) {
            System.err.println("Failed to load accounts from DB. Using in-memory defaults. Error: " + ex.getMessage());
            // fallback accounts (same as before)
            // fallback: supply a cardNo different from the account number
            accounts.put("1234567890", new Account("1234567890", "CARD-0001", "1234", 1000.0, "John Doe", "IFSC1234567", "123 Main St, Anytown"));
            accounts.put("1111222233", new Account("1111222233", "CARD-0002", "4321", 500.0, "Jane Smith", "IFSC7654321", "456 Oak Ave, Somewhere"));
            System.out.println("Loaded fallback in-memory accounts: 2 entries.");
        }
    }

    // Helper to check whether the current ResultSet contains a named column.
    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                if (md.getColumnLabel(i).equalsIgnoreCase(columnName) || md.getColumnName(i).equalsIgnoreCase(columnName)) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    // Prompt the user for a PIN using a masked JPasswordField. Returns the entered PIN string or null if cancelled.
    private String promptForMaskedPin(String message) {
        JPasswordField pwd = new JPasswordField(10);
        pwd.setEchoChar('\u2022'); // bullet dot
        Object[] obj = {message, pwd};
        int result = JOptionPane.showConfirmDialog(this, obj, "PIN Entry", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            return new String(pwd.getPassword());
        }
        return null;
    }

    private void persistAccountBalance(Account account) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE account_number = ?")) {
            ps.setDouble(1, account.balance);
            ps.setString(2, account.accountNumber);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to persist balance for " + account.accountNumber + ": " + ex.getMessage());
        }
    }

    private void updateAccountPin(Account account) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET pin = ? WHERE account_number = ?")) {
            ps.setString(1, account.pin);
            ps.setString(2, account.accountNumber);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to update PIN for " + account.accountNumber + ": " + ex.getMessage());
        }
    }

    private void insertTransaction(String receipt, String fromCard, String toCard, double amount, String description) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO transactions (receipt, from_card, to_card, amount, description) VALUES (?, ?, ?, ?, ?)") ) {
            ps.setString(1, receipt);
            ps.setString(2, fromCard);
            ps.setString(3, toCard);
            ps.setDouble(4, amount);
            ps.setString(5, description);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to insert transaction: " + ex.getMessage());
        }
    }

    private void logout() {
        // Clear current user and reset UI fields, then show login screen
        currentUser = null;
        balanceLabel.setText("Balance: $0.00");
        dateTimeLabel.setText("");
        welcomeLabel.setText("Welcome!");
        loginMessageLabel.setText(" ");
        cardLayout.show(mainPanel, "login");
    }

    public static void main(String[] args) {
        System.out.println("ATM starting...");
        SwingUtilities.invokeLater(() -> {
            ATMInterfaceGUI atm = new ATMInterfaceGUI();
            atm.setVisible(true);
        });
    }

    // Helper: scale all UIManager fonts by the given factor
    private void increaseGlobalFont(float scale) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font) {
                Font f = (Font) value;
                Font larger = f.deriveFont(f.getSize2D() * scale);
                UIManager.put(key, larger);
            }
        }
    }
}

// simple stub for database connectivity; real implementation not included
class DatabaseConnection {
    public static Connection getConnection() throws SQLException {
        throw new SQLException("Database connection not available in this build.");
    }
}
