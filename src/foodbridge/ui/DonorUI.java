package foodbridge.ui;

import foodbridge.dao.DonationDAO;
import foodbridge.models.Donation;
import foodbridge.models.User;

import java.util.List;
import java.util.Scanner;

public class DonorUI {

    private final Scanner     sc;
    private final DonationDAO donationDAO;
    private final User        donor;

    public DonorUI(Scanner sc, User donor) {
        this.sc          = sc;
        this.donor       = donor;
        this.donationDAO = new DonationDAO();
    }

    public void show() {
        donationDAO.autoExpireDonations();   // housekeeping on login

        while (true) {
            System.out.println("\n");
            System.out.println("  ╔════════════════════════════════════════╗");
            System.out.printf ("  ║  🍴  DONOR DASHBOARD — %-15s ║%n", donor.getName());
            System.out.println("  ╠════════════════════════════════════════╣");
            System.out.println("  ║  1.  Add New Donation                  ║");
            System.out.println("  ║  2.  View My Donations                 ║");
            System.out.println("  ║  3.  Platform Impact Stats             ║");
            System.out.println("  ║  4.  Logout                            ║");
            System.out.println("  ╚════════════════════════════════════════╝");
            System.out.print  ("  Choice: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> addDonation();
                case "2" -> viewMyDonations();
                case "3" -> donationDAO.printImpactStats();
                case "4" -> { System.out.println("\n  👋  Logged out. Thank you for reducing waste!"); return; }
                default  -> System.out.println("  ⚠️  Invalid option. Try 1-4.");
            }
        }
    }

    // ── ADD DONATION ───────────────────────────────────────────────────────
    private void addDonation() {
        System.out.println("\n  ╔══════════════════════╗");
        System.out.println(  "  ║   ➕ Add Donation    ║");
        System.out.println(  "  ╚══════════════════════╝");

        System.out.print("  Food Item Name   : ");
        String item = sc.nextLine().trim();
        if (item.isEmpty()) { System.out.println("  ⚠️  Item name cannot be empty."); return; }

        System.out.print("  Quantity (e.g. 5 kg / 20 portions) : ");
        String qty = sc.nextLine().trim();
        if (qty.isEmpty()) { System.out.println("  ⚠️  Quantity cannot be empty."); return; }

        System.out.print("  Expiry Date & Time (YYYY-MM-DD HH:MM) : ");
        String expiry = sc.nextLine().trim() + ":00";   // append seconds

        boolean success = donationDAO.addDonation(donor.getUserId(), item, qty, expiry);
        System.out.println(success
            ? "\n  ✅  Donation added successfully!"
            : "\n  ❌  Failed to add donation. Check the date format.");
    }

    // ── VIEW MY DONATIONS ──────────────────────────────────────────────────
    private void viewMyDonations() {
        List<Donation> list = donationDAO.getDonorDonations(donor.getUserId());

        System.out.println("\n  ┌───────────────────────────────────────────────────────────────────────────┐");
        System.out.println(  "  │                         MY DONATIONS                                      │");
        System.out.println(  "  ├──────┬─────────────────────┬───────────┬─────────────────────┬───────────┤");
        System.out.println(  "  │ ID   │ Food Item           │ Qty       │ Expiry              │ Status    │");
        System.out.println(  "  ├──────┼─────────────────────┼───────────┼─────────────────────┼───────────┤");

        if (list.isEmpty()) {
            System.out.println("  │               You have not made any donations yet.                        │");
        } else {
            for (Donation d : list) {
                String statusIcon = switch (d.getStatus()) {
                    case "AVAILABLE" -> "🟢 AVAIL";
                    case "CLAIMED"   -> "🔵 CLAIMD";
                    case "EXPIRED"   -> "🔴 EXPIRD";
                    default          -> d.getStatus();
                };
                System.out.printf("  │ %-4d │ %-19s │ %-9s │ %-19s │ %-9s │%n",
                    d.getDonationId(), d.getFoodItem(), d.getQuantity(),
                    d.getExpiryAt(), statusIcon);
            }
        }
        System.out.println("  └──────┴─────────────────────┴───────────┴─────────────────────┴───────────┘");
    }
}
