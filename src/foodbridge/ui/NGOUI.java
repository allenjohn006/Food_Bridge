package foodbridge.ui;

import foodbridge.dao.DonationDAO;
import foodbridge.models.Donation;
import foodbridge.models.User;

import java.util.List;
import java.util.Scanner;

public class NGOUI {

    private final Scanner     sc;
    private final DonationDAO donationDAO;
    private final User        ngo;

    public NGOUI(Scanner sc, User ngo) {
        this.sc          = sc;
        this.ngo         = ngo;
        this.donationDAO = new DonationDAO();
    }

    public void show() {
        donationDAO.autoExpireDonations();   // housekeeping on login

        while (true) {
            System.out.println("\n");
            System.out.println("  ╔════════════════════════════════════════╗");
            System.out.printf ("  ║  🤝  NGO DASHBOARD — %-17s ║%n", ngo.getName());
            System.out.println("  ╠════════════════════════════════════════╣");
            System.out.println("  ║  1.  View Available Food               ║");
            System.out.println("  ║  2.  Claim a Donation                  ║");
            System.out.println("  ║  3.  My Claim History                  ║");
            System.out.println("  ║  4.  Platform Impact Stats             ║");
            System.out.println("  ║  5.  Logout                            ║");
            System.out.println("  ╚════════════════════════════════════════╝");
            System.out.print  ("  Choice: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewAvailable();
                case "2" -> claimDonation();
                case "3" -> donationDAO.printNGOClaims(ngo.getUserId());
                case "4" -> donationDAO.printImpactStats();
                case "5" -> { System.out.println("\n  👋  Logged out. Thank you for serving communities!"); return; }
                default  -> System.out.println("  ⚠️  Invalid option. Try 1-5.");
            }
        }
    }

    // ── VIEW AVAILABLE ─────────────────────────────────────────────────────
    private List<Donation> viewAvailable() {
        List<Donation> list = donationDAO.getAvailableDonations();

        System.out.println("\n  ┌──────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.println(  "  │                          AVAILABLE FOOD DONATIONS                                    │");
        System.out.println(  "  ├──────┬─────────────────────┬──────────┬───────────┬─────────────────────┬───────────┤");
        System.out.println(  "  │ ID   │ Food Item           │ Category │ Quantity  │ Expiry              │ Donor     │");
        System.out.println(  "  ├──────┼─────────────────────┼──────────┼───────────┼─────────────────────┼───────────┤");

        if (list.isEmpty()) {
            System.out.println("  │                 No food available right now. Check back soon!                       │");
        } else {
            for (Donation d : list) {
                System.out.printf("  │ %-4d │ %-19s │ %-8s │ %-9s │ %-19s │ %-9s │%n",
                    d.getDonationId(),
                    d.getFoodItem(),
                    d.getCategory(),
                    d.getQuantity(),
                    d.getExpiryAt(),
                    d.getDonorName().length() > 9 ? d.getDonorName().substring(0,9) : d.getDonorName());
            }
        }
        System.out.println("  └──────┴─────────────────────┴──────────┴───────────┴─────────────────────┴───────────┘");
        return list;
    }

    // ── CLAIM DONATION ─────────────────────────────────────────────────────
    private void claimDonation() {
        List<Donation> available = viewAvailable();
        if (available.isEmpty()) return;

        System.out.print("\n  Enter Donation ID to claim: ");
        String input = sc.nextLine().trim();
        int donationId;
        try {
            donationId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("  ⚠️  Please enter a valid number.");
            return;
        }

        // Validate it's in the available list
        boolean valid = available.stream().anyMatch(d -> d.getDonationId() == donationId);
        if (!valid) {
            System.out.println("  ⚠️  That donation ID is not in the available list.");
            return;
        }

        System.out.print("  Confirm claim? (yes/no): ");
        String confirm = sc.nextLine().trim().toLowerCase();
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("  Claim cancelled.");
            return;
        }

        boolean success = donationDAO.claimDonation(donationId, ngo.getUserId());
        System.out.println(success
            ? "\n  ✅  Donation claimed successfully! Impact logged."
            : "\n  ❌  Claim failed — may have just been taken by another NGO.");
    }
}
