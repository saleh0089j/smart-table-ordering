public class MenuItem {
    private final String id;
    private String name;
    private double price;
    private String imageUrl;
    private boolean visible;
    private boolean available;


    public MenuItem(String id, String name, double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.visible = true;
        this.available = true;
}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}