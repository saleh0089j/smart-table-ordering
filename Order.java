import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order {
    private final String orderId;
    private final String sessionId;
    private final List<String> items;
    private final PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private FoodStatus foodStatus;
    private double total;
    private double totalAmount;

    public Order(String orderId, String sessionId, List<String> items,
                 PaymentMode paymentMode, FoodStatus foodStatus, PaymentStatus paymentStatus) {
        this.orderId = (orderId != null && !orderId.isBlank())
                ? orderId
                : UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        this.paymentMode = paymentMode;
        this.foodStatus = (foodStatus != null) ? foodStatus : FoodStatus.PENDING;
        this.paymentStatus = (paymentStatus != null) ? paymentStatus : PaymentStatus.UNPAID;
        this.total = 0.0;
        this.totalAmount = 0.0;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<String> getItems() {
        return items;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public FoodStatus getFoodStatus() {
        return foodStatus;
    }

    public void setFoodStatus(FoodStatus foodStatus) {
        this.foodStatus = foodStatus;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
        this.totalAmount = total;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
        this.total = totalAmount;
    }
}