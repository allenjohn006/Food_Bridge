package foodbridge;

import foodbridge.dao.DBConnection;
import foodbridge.dao.UserDAO;
import foodbridge.models.User;
import foodbridge.ui.DonorUI;
import foodbridge.ui.NGOUI;

import java.util.Scanner;

/**
 * Main.java — Entry point for the FoodBridge application.
 *
 * Run from terminal (VS Code integrated terminal):
 *   javac -cp "lib/*" -d out $(find src -name "*.java")
 *   java  -cp "out:lib/*" foodbridge.Main
 *
 * On Windows:
 *   javac -cp "lib\*" -d out (Get-ChildItem src -Recurse -Filter *.java).FullName
 *   java  -cp "out;lib\*" foodbridge.Main
 */
public class Main {

    static final Scanner sc      = new Scanner(System.in);
    static final UserDAO userDAO = new UserDAO();

    public static void main(String[] args) {

        printBanner();

        while (true) {
            System.out.println("  ╔════════════════════════════════╗");
            System.out.println("  ║       MAIN MENU                ║");
            System.out.println("  ╠════════════════════════════════╣");
            System.out.println("  ║  1.  Login                     ║");
            System.out.println("  ║  2.  Register                  ║");
            System.out.println("  ║  3.  Exit                      ║");
            System.out.println("  ╚════════════════════════════════╝");
            System.out.print  ("  Choice: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> {
                    DBConnection.closeConnection();
                    System.out.println("\n  🌱  FoodBridge — Together we end food waste. Goodbye!\n");
                    System.exit(0);
                }
                default  -> System.out.println("  ⚠️  Please enter 1, 2, or 3.\n");
            }
        }
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────
    private static void login() {
        System.out.println("\n  ╔══════════════════╗");
        System.out.println(  "  ║     🔐 LOGIN     ║");
        System.out.println(  "  ╚══════════════════╝");

        System.out.println("  Role:  1. Donor   2. NGO");
        System.out.print  ("  Select role: ");
        String roleChoice = sc.nextLine().trim();
        String role = switch (roleChoice) {
            case "1" -> "DONOR";
            case "2" -> "NGO";
            default  -> "";
        };
        if (role.isEmpty()) { System.out.println("  ⚠️  Invalid role."); return; }

        System.out.print("  Email    : ");
        String email = sc.nextLine().trim();
        System.out.print("  Password : ");
        String password = sc.nextLine().trim();

        User user = userDAO.login(email, password, role);

        if (user == null) {
            System.out.println("\n  ❌  Invalid credentials. Please try again.");
            return;
        }

        System.out.printf("%n  ✅  Welcome back, %s!%n", user.getName());

        if (user.getRole().equals("DONOR")) {
            new DonorUI(sc, user).show();
        } else {
            new NGOUI(sc, user).show();
        }
    }

    // ── REGISTER ───────────────────────────────────────────────────────────
    private static void register() {
        System.out.println("\n  ╔══════════════════════╗");
        System.out.println(  "  ║    📝  REGISTER      ║");
        System.out.println(  "  ╚══════════════════════╝");

        System.out.println("  Role:  1. Donor (Restaurant/Hotel)   2. NGO/Charity");
        System.out.print  ("  Select role: ");
        String roleChoice = sc.nextLine().trim();
        String role = switch (roleChoice) {
            case "1" -> "DONOR";
            case "2" -> "NGO";
            default  -> "";
        };
        if (role.isEmpty()) { System.out.println("  ⚠️  Invalid role."); return; }

        System.out.print("  Organisation/Name : ");
        String name = sc.nextLine().trim();
        System.out.print("  Phone             : ");
        String phone = sc.nextLine().trim();
        System.out.print("  Email             : ");
        String email = sc.nextLine().trim();
        System.out.print("  Password          : ");
        String password = sc.nextLine().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            System.out.println("  ⚠️  Name, email and password are required.");
            return;
        }

        boolean ok = userDAO.register(name, role, phone, email, password);
        System.out.println(ok
            ? "\n  ✅  Registration successful! You can now login."
            : "\n  ❌  Registration failed. Email may already be in use.");
    }

    // ── BANNER ─────────────────────────────────────────────────────────────
    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════════╗");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║    🌱  F O O D B R I D G E                              ║");
        System.out.println("  ║        Connecting Surplus Food with Those Who Need It   ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║    Donors (Hotels/Restaurants) → NGOs → Communities     ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
