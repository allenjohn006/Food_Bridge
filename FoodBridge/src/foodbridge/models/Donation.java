package foodbridge.models;

public class Donation {
    private int    donationId;
    private int    donorId;
    private String donorName;
    private String foodItem;
    private String category;
    private String quantity;
    private String expiryAt;
    private String status;

    public Donation(int donationId, int donorId, String donorName,
                    String foodItem, String category,
                    String quantity, String expiryAt, String status) {
        this.donationId = donationId;
        this.donorId    = donorId;
        this.donorName  = donorName;
        this.foodItem   = foodItem;
        this.category   = category;
        this.quantity   = quantity;
        this.expiryAt   = expiryAt;
        this.status     = status;
    }

    public int    getDonationId() { return donationId; }
    public int    getDonorId()    { return donorId;    }
    public String getDonorName()  { return donorName;  }
    public String getFoodItem()   { return foodItem;   }
    public String getCategory()   { return category;   }
    public String getQuantity()   { return quantity;   }
    public String getExpiryAt()   { return expiryAt;   }
    public String getStatus()     { return status;     }
}
