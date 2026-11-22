import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RestaurantEngine {
    private PaymentMode defaultPaymentMode = PaymentMode.POST_PAY;
    private int tableCount = 4;

    private final Map<Integer, TableStatus> tableStatuses = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<Integer, Session> activeSessionsByTable = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, Session> sessionsById = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<Integer, Integer> tableResetCounts = Collections.synchronizedMap(new LinkedHashMap<>());

    private final Map<String, MenuItem> menuItems = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, Order> orders = Collections.synchronizedMap(new LinkedHashMap<>());
    private int menuSeq = 1;
    private int orderSeq = 1;

    public RestaurantEngine() {
        configureTables(4);
    }

    public void initDefaultMenu() {
        // Intentionally empty: chefs build their own independent recipe list.
    }

    public synchronized void configureTables(int count) {
        if (count < 1 || count > 200) {
            throw new IllegalArgumentException("Table count must be between 1 and 200.");
        }
        this.tableCount = count;
        tableStatuses.clear();
        activeSessionsByTable.clear();
        for (int i = 1; i <= count; i++) {
            tableStatuses.put(i, TableStatus.EMPTY);
            tableResetCounts.putIfAbsent(i, 0);
        }
    }

    public int getTableCount() {
        return tableCount;
    }

    public TableStatus getTableStatus(int tableNumber) {
        return tableStatuses.getOrDefault(tableNumber, TableStatus.EMPTY);
    }

    public Session getActiveSessionForTable(int tableNumber) {
        return activeSessionsByTable.get(tableNumber);
    }

    public Session getSessionById(String sessionId) {
        return sessionsById.get(sessionId);
    }

    public int getTableResetCount(int tableNumber) {
        return tableResetCounts.getOrDefault(tableNumber, 0);
    }

    public synchronized void freeTable(int tableNumber) {
        if (tableNumber < 1 || tableNumber > tableCount) {
            throw new IllegalArgumentException("Invalid table number: " + tableNumber);
        }
        activeSessionsByTable.remove(tableNumber);
        tableStatuses.put(tableNumber, TableStatus.EMPTY);
        tableResetCounts.put(tableNumber, getTableResetCount(tableNumber) + 1);
    }

    public synchronized Session startSession(int tableNumber, String customerName, String customerPhone) {
        if (tableNumber < 1 || tableNumber > tableCount) {
            throw new IllegalArgumentException("Invalid table number: " + tableNumber);
        }
        String sessionId = "SES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Session session = new Session(sessionId, tableNumber, customerName, customerPhone);
        sessionsById.put(sessionId, session);
        activeSessionsByTable.put(tableNumber, session);
        tableStatuses.put(tableNumber, TableStatus.OCCUPIED);
        return session;
    }

    public PaymentMode getDefaultPaymentMode() {
        return defaultPaymentMode;
    }

    public void setDefaultPaymentMode(PaymentMode mode) {
        if (mode != null) {
            this.defaultPaymentMode = mode;
        }
    }

    public List<MenuItem> getMenuItems(boolean includeHidden) {
        List<MenuItem> result = new ArrayList<>();
        synchronized (menuItems) {
            for (MenuItem item : menuItems.values()) {
                if (includeHidden || item.isVisible()) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    public MenuItem getMenuItemByName(String name) {
        if (name == null) return null;
        synchronized (menuItems) {
            for (MenuItem item : menuItems.values()) {
                if (item.getName().equalsIgnoreCase(name.trim())) {
                    return item;
                }
            }
        }
        return null;
    }

    public synchronized MenuItem addMenuItem(String name, double price, String imageUrl) {
        String id = "M-" + (menuSeq++);
        MenuItem item = new MenuItem(id, name, price, imageUrl);
        menuItems.put(id, item);
        return item;
    }

    public synchronized MenuItem updateMenuItem(String id, String name, double price, String imageUrl) {
        MenuItem item = menuItems.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Menu item not found: " + id);
        }
        if (name != null && !name.isBlank()) {
            item.setName(name.trim());
        }
        if (price >= 0) {
            item.setPrice(price);
        }
        if (imageUrl != null) {
            item.setImageUrl(imageUrl.trim());
        }
        return item;
    }

    public synchronized MenuItem updateMenuFlags(String id, Boolean visible, Boolean available) {
        MenuItem item = menuItems.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Menu item not found: " + id);
        }
        if (visible != null) {
            item.setVisible(visible);
        }
        if (available != null) {
            item.setAvailable(available);
        }
        return item;
    }

    public synchronized void deleteMenuItem(String id) {
        if (menuItems.remove(id) == null) {
            throw new IllegalArgumentException("Menu item not found: " + id);
        }
    }

    public synchronized Order placeOrder(String sessionId, List<String> items, double total, PaymentMode mode, boolean paid) {
        Session session = sessionsById.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Active session not found: " + sessionId);
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }
        String orderId = "ORD-" + (orderSeq++);
        PaymentStatus payStatus = paid ? PaymentStatus.PAID : PaymentStatus.UNPAID;
        Order order = new Order(orderId, sessionId, new ArrayList<>(items), mode, FoodStatus.PENDING, payStatus);
        order.setTotal(total);
        orders.put(orderId, order);
        return order;
    }

    public Order getOrderById(String orderId) {
        return orders.get(orderId);
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public synchronized Order updateFoodStatus(String orderId, FoodStatus nextStatus) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        if (order.getPaymentMode() == PaymentMode.PRE_PAY
                && order.getPaymentStatus() == PaymentStatus.UNPAID
                && nextStatus == FoodStatus.PREPARING) {
            throw new IllegalStateException("Order is locked until payment is approved.");
        }
        order.setFoodStatus(nextStatus);
        return order;
    }

    public synchronized Order payOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        return order;
    }
    // Inside RestaurantEngine.java
public synchronized void addTable(int tableNumber) {
    if (tableNumber > this.tableCount) {
        this.tableCount = tableNumber;
    }
    tableStatuses.putIfAbsent(tableNumber, TableStatus.EMPTY);
    tableResetCounts.putIfAbsent(tableNumber, 0);
}

public void checkExitSensor(String sessionId, String orderId) {
    Order order = orders.get(orderId);
    Session session = sessionsById.get(sessionId);
    if (order != null && session != null) {
        if (order.getPaymentMode() == PaymentMode.POST_PAY && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Exit blocked: Post-pay bill is still UNPAID.");
        }
        freeTable(session.getTableNumber());
    }
}
}