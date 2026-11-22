public class DiningTable {
    private final int tableNumber;
    private String activeSessionId;
    private TableStatus tableStatus;

    public DiningTable(int tableNumber) {
        this.tableNumber = tableNumber;
        this.tableStatus = TableStatus.EMPTY;
        this.activeSessionId = null;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public TableStatus getTableStatus() {
        return tableStatus;
    }

    public void occupy(String sessionId) {
        this.tableStatus = TableStatus.OCCUPIED;
        this.activeSessionId = sessionId;
    }

    public String getActiveSessionId() {
        return activeSessionId;
    }

    public void markneedsCleaning() {
        this.tableStatus = TableStatus.NEEDS_CLEANING;
        this.activeSessionId = null;
    }

    public void markEmpty() {
        this.tableStatus = TableStatus.EMPTY;
        this.activeSessionId = null;
    }
}
