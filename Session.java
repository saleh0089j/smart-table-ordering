
import java.time.Instant;

public class Session {
    private final String sessionId;
    private final int tableNumber;
    private final String customerName;
    private final String customerPhone;
    private final Instant createdAt;
    private boolean active;
    // Inside Session.java

    public Session(String sessionId, int tableNumber, String customerName, String customerPhone) {
        this.sessionId = sessionId;
        this.tableNumber = tableNumber;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.createdAt = Instant.now();
        this.active = true;
}

   

    public String getSessionId() {
        return sessionId;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void endSession() {
        this.active = false;
    }
}
