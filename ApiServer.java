import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiServer {
    private static final RestaurantEngine engine = new RestaurantEngine();
    private static final Map<String, Integer> paCounters = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void main(String[] args) throws IOException {
        engine.configureTables(4);
        engine.initDefaultMenu();

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path))
                path = "/index.html";
            Path webRoot = Path.of("web").toAbsolutePath().normalize();
            Path file = webRoot.resolve(path.substring(1)).normalize();
            if (!file.startsWith(webRoot) || !Files.exists(file) || Files.isDirectory(file)) {
                sendText(exchange, 404, "Not found");
                return;
            }
            String contentType = detectContentType(file.toString());
            byte[] bytes = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/api/session", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                int table = Integer.parseInt(q.getOrDefault("table", "1"));
                String name = q.getOrDefault("name", "Guest").trim();
                if (name.isEmpty())
                    name = "Guest";
                String phone = q.getOrDefault("phone", "").trim();
                Session s = engine.startSession(table, name, phone);
                sendJson(exchange, 200, "{\"sessionId\":\"" + escape(s.getSessionId()) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/tables/config", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                int count = Integer.parseInt(q.getOrDefault("count", "4"));
                engine.configureTables(count);
                sendJson(exchange, 200, "{\"totalTables\":" + count + "}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/config/payment", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, "{\"mode\":\"" + engine.getDefaultPaymentMode() + "\"}");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                PaymentMode mode = PaymentMode.valueOf(q.getOrDefault("mode", "POST_PAY").toUpperCase());
                engine.setDefaultPaymentMode(mode);
                sendJson(exchange, 200, "{\"mode\":\"" + mode + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/menu", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            boolean all = Boolean.parseBoolean(q.getOrDefault("all", "false"));
            List<MenuItem> items = engine.getMenuItems(all);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0)
                    sb.append(",");
                MenuItem m = items.get(i);
                sb.append("{\"id\":\"").append(escape(m.getId())).append("\",")
                        .append("\"name\":\"").append(escape(m.getName())).append("\",")
                        .append("\"price\":").append(m.getPrice()).append(",")
                        .append("\"imageUrl\":\"").append(escape(m.getImageUrl())).append("\",")
                        .append("\"visible\":").append(m.isVisible()).append(",")
                        .append("\"available\":").append(m.isAvailable()).append("}");
            }
            sb.append("]");
            sendJson(exchange, 200, sb.toString());
        });

        server.createContext("/api/menu/add", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String raw = !body.isBlank() ? body : exchange.getRequestURI().getRawQuery();
            Map<String, String> q = parseQuery(raw);
            try {
                String name = q.getOrDefault("name", "").trim();
                if (name.isEmpty())
                    throw new IllegalArgumentException("Dish name required.");
                double price = Double.parseDouble(q.getOrDefault("price", "0"));
                String imageUrl = q.getOrDefault("imageUrl", "").trim();
                MenuItem created = engine.addMenuItem(name, price, imageUrl);
                sendJson(exchange, 200, "{\"id\":\"" + escape(created.getId()) + "\",\"name\":\"" + escape(created.getName()) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/menu/edit", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String raw = !body.isBlank() ? body : exchange.getRequestURI().getRawQuery();
            Map<String, String> q = parseQuery(raw);
            try {
                String id = q.getOrDefault("id", "");
                String name = q.getOrDefault("name", "").trim();
                double price = Double.parseDouble(q.getOrDefault("price", "0"));
                String imageUrl = q.getOrDefault("imageUrl", "").trim();
                MenuItem updated = engine.updateMenuItem(id, name, price, imageUrl);
                sendJson(exchange, 200, "{\"id\":\"" + escape(updated.getId()) + "\",\"name\":\"" + escape(updated.getName()) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/menu/toggle", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String id = q.getOrDefault("id", "");
                Boolean visible = q.containsKey("visible") ? Boolean.parseBoolean(q.get("visible")) : null;
                Boolean available = q.containsKey("available") ? Boolean.parseBoolean(q.get("available")) : null;
                MenuItem updated = engine.updateMenuFlags(id, visible, available);
                sendJson(exchange, 200, "{\"id\":\"" + escape(updated.getId()) + "\",\"visible\":" + updated.isVisible() + ",\"available\":" + updated.isAvailable() + "}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/menu/delete", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String id = q.getOrDefault("id", "");
                engine.deleteMenuItem(id);
                sendJson(exchange, 200, "{\"deleted\":\"" + escape(id) + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/order", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String sessionId = q.getOrDefault("sessionId", "");
                String rawItems = q.getOrDefault("items", q.getOrDefault("item", "")).trim();
                if (rawItems.isEmpty())
                    throw new IllegalArgumentException("At least one item is required.");

                List<String> selectedItems = new ArrayList<>();
                double computedTotal = 0.0;
                for (String part : rawItems.split(",")) {
                    String dish = part.trim();
                    if (!dish.isEmpty()) {
                        selectedItems.add(dish);
                        MenuItem mi = engine.getMenuItemByName(dish);
                        if (mi != null)
                            computedTotal += mi.getPrice();
                    }
                }
                if (q.containsKey("total")) {
                    computedTotal = Double.parseDouble(q.get("total"));
                }

                String payMethod = q.getOrDefault("method", "ONLINE").toUpperCase();
                PaymentMode mode = engine.getDefaultPaymentMode();
                boolean paid;
                if ("CASH".equals(payMethod)) {
                    paid = false;
                } else {
                    paid = (mode == PaymentMode.PRE_PAY) && Boolean.parseBoolean(q.getOrDefault("paid", "true"));
                }

                Order order = engine.placeOrder(sessionId, selectedItems, computedTotal, mode, paid);
                paCounters.putIfAbsent(order.getOrderId(), 0);
                sendJson(exchange, 200, orderToJson(order));
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/orders", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            List<Order> allOrders = engine.getAllOrders();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < allOrders.size(); i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(orderToJson(allOrders.get(i)));
            }
            sb.append("]");
            sendJson(exchange, 200, sb.toString());
        });

        server.createContext("/api/order/status", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String id = q.getOrDefault("id", "");
                FoodStatus status = FoodStatus.valueOf(q.getOrDefault("status", "PREPARING"));
                Order updated = engine.updateFoodStatus(id, status);
                if (status == FoodStatus.READY) {
                    paCounters.put(id, paCounters.getOrDefault(id, 0) + 1);
                }
                sendJson(exchange, 200, orderToJson(updated));
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/order/pa", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            String id = q.getOrDefault("id", "");
            if (engine.getOrderById(id) == null) {
                sendJson(exchange, 404, "{\"error\":\"Order not found\"}");
                return;
            }
            int nextSeq = paCounters.getOrDefault(id, 0) + 1;
            paCounters.put(id, nextSeq);
            sendJson(exchange, 200, "{\"id\":\"" + escape(id) + "\",\"paSeq\":" + nextSeq + "}");
        });

        server.createContext("/api/order/pay", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String id = q.getOrDefault("id", "");
                Order paidOrder = engine.payOrder(id);
                sendJson(exchange, 200, orderToJson(paidOrder));
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.createContext("/api/tables", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (int t = 1; t <= engine.getTableCount(); t++) {
                TableStatus status = engine.getTableStatus(t);
                Session active = (status == TableStatus.EMPTY) ? null : engine.getActiveSessionForTable(t);
                String guestName = (active != null) ? active.getCustomerName() : "";
                String guestPhone = (active != null) ? active.getCustomerPhone() : "";
                int resetSeq = engine.getTableResetCount(t);
                if (!first)
                    sb.append(",");
                sb.append("{\"tableNumber\":").append(t)
                        .append(",\"status\":\"").append(status).append("\"")
                        .append(",\"guestName\":\"").append(escape(guestName)).append("\"")
                        .append(",\"guestPhone\":\"").append(escape(guestPhone)).append("\"")
                        .append(",\"tableResetSeq\":").append(resetSeq).append("}");
                first = false;
            }
            sb.append("]");
            sendJson(exchange, 200, sb.toString());
        });

        server.createContext("/api/table/free", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                int table = Integer.parseInt(q.getOrDefault("table", "1"));
                engine.freeTable(table);
                sendJson(exchange, 200, "{\"table\":" + table + ",\"status\":\"EMPTY\"}");
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Java Restaurant Server running at http://0.0.0.0:8080");
    }

    private static String detectContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html"))
            return "text/html; charset=utf-8";
        if (lower.endsWith(".css"))
            return "text/css; charset=utf-8";
        if (lower.endsWith(".js"))
            return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json"))
            return "application/json; charset=utf-8";
        if (lower.endsWith(".png"))
            return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".webp"))
            return "image/webp";
        return "text/plain; charset=utf-8";
    }

    private static String orderToJson(Order o) {
        Session s = engine.getSessionById(o.getSessionId());
        int tableNum = (s != null) ? s.getTableNumber() : 0;
        String guestName = (s != null) ? s.getCustomerName() : "Guest";
        String guestPhone = (s != null) ? s.getCustomerPhone() : "";
        int paSeq = paCounters.getOrDefault(o.getOrderId(), 0);
        int tableResetSeq = engine.getTableResetCount(tableNum);

        String itemsJson = "[" + String.join(",", o.getItems().stream()
                .map(i -> "\"" + escape(i) + "\"").toList()) + "]";

        List<String> imgs = o.getItems().stream().map(dishName -> {
            MenuItem mi = engine.getMenuItemByName(dishName);
            String img = (mi != null) ? mi.getImageUrl() : "";
            return "\"" + escape(img) + "\"";
        }).toList();
        String imagesJson = "[" + String.join(",", imgs) + "]";

        return "{" +
                "\"id\":\"" + escape(o.getOrderId()) + "\"," +
                "\"sessionId\":\"" + escape(o.getSessionId()) + "\"," +
                "\"tableNumber\":" + tableNum + "," +
                "\"guestName\":\"" + escape(guestName) + "\"," +
                "\"guestPhone\":\"" + escape(guestPhone) + "\"," +
                "\"mode\":\"" + o.getPaymentMode() + "\"," +
                "\"foodStatus\":\"" + o.getFoodStatus() + "\"," +
                "\"paymentStatus\":\"" + o.getPaymentStatus() + "\"," +
                "\"total\":" + o.getTotal() + "," +
                "\"paSeq\":" + paSeq + "," +
                "\"tableResetSeq\":" + tableResetSeq + "," +
                "\"itemImages\":" + imagesJson + "," +
                "\"items\":" + itemsJson +
                "}";
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isBlank())
            return map;
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sendJson(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}