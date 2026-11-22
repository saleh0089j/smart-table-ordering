import java.util.List;

public class Main {
    public static void main(String[] args) {
        RestaurantEngine engine = new RestaurantEngine();
        engine.addTable(1);
        engine.addTable(2);

        System.out.println("=== SCENARIO 1: PRE PAY TABLE ===");
        Session s1 = engine.startSession(1, "Guest 1", "+8801700000001");
        System.out.println("Table 1 status: " + engine.getTableStatus(1) + " | Session: " + s1.getSessionId());

        Order o1 = engine.placeOrder(
                s1.getSessionId(),
                List.of("beef Burger", "Cold Coffee"),
                650.0,
                PaymentMode.PRE_PAY,
                true);

        engine.updateFoodStatus(o1.getOrderId(), FoodStatus.PREPARING);
        engine.updateFoodStatus(o1.getOrderId(), FoodStatus.READY);
        engine.updateFoodStatus(o1.getOrderId(), FoodStatus.SERVED);
        engine.checkExitSensor(s1.getSessionId(), o1.getOrderId());
        System.out.println("Table 1 status after exit: " + engine.getTableStatus(1));

        System.out.println("\n=== SCENARIO 2: POST_PAY WALKOUT TEST ===");
        Session s2 = engine.startSession(2, "Guest 2", "+8801800000002");
        Order o2 = engine.placeOrder(
                s2.getSessionId(),
                List.of("Pasta Alfredo", "Lemonade"),
                520.0,
                PaymentMode.POST_PAY,
                false);
        engine.updateFoodStatus(o2.getOrderId(), FoodStatus.READY);
        engine.updateFoodStatus(o2.getOrderId(), FoodStatus.SERVED);

        engine.checkExitSensor(s2.getSessionId(), o2.getOrderId());

        System.out.println("-- Customer pays in portal --");
        engine.payOrder(o2.getOrderId());
        engine.checkExitSensor(s2.getSessionId(), o2.getOrderId());
        System.out.println("Table 2 status after payment & exit: " + engine.getTableStatus(2));

    }
}
