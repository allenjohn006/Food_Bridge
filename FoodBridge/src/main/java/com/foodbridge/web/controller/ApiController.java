package com.foodbridge.web.controller;

import com.foodbridge.web.dto.AddDonationRequest;
import com.foodbridge.web.dto.AuthRequest;
import com.foodbridge.web.dto.ClaimRequest;
import com.foodbridge.web.dto.NgoNeedRequest;
import com.foodbridge.web.dto.RegisterRequest;
import com.foodbridge.web.service.FoodBridgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final FoodBridgeService service;

    public ApiController(FoodBridgeService service) {
        this.service = service;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        Map<String, Object> user = service.login(req.email(), req.password(), req.role());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid credentials"));
        }
        return ResponseEntity.ok(Map.of("success", true, "user", user));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        boolean ok = service.register(req);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Registration failed. Email may already exist."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Registration successful"));
    }

    @GetMapping("/donations/available")
    public ResponseEntity<?> availableDonations() {
        return ResponseEntity.ok(service.getAvailableDonations());
    }

    @GetMapping("/donations/donor/{donorId}")
    public ResponseEntity<?> donorDonations(@PathVariable int donorId) {
        return ResponseEntity.ok(service.getDonorDonations(donorId));
    }

    @PostMapping("/donations")
    public ResponseEntity<?> addDonation(@RequestBody AddDonationRequest req) {
        boolean ok = service.addDonation(req);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Could not add donation. Check fields and request link."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Donation posted successfully"));
    }

    @PostMapping("/donations/{donationId}/claim")
    public ResponseEntity<?> claimDonation(@PathVariable int donationId, @RequestBody ClaimRequest req) {
        boolean ok = service.claimDonation(donationId, req.ngoId());
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Donation not available to claim"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Donation claimed successfully"));
    }

    @GetMapping("/ngo/{ngoId}/claims")
    public ResponseEntity<?> ngoClaims(@PathVariable int ngoId) {
        return ResponseEntity.ok(service.getNgoClaims(ngoId));
    }

    @PostMapping("/ngo/requests")
    public ResponseEntity<?> createRequest(@RequestBody NgoNeedRequest req) {
        boolean ok = service.createNgoRequest(req);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Could not create NGO request"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "NGO request posted"));
    }

    @GetMapping("/requests/open")
    public ResponseEntity<?> openRequests() {
        return ResponseEntity.ok(service.getOpenRequests());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(service.getPlatformStats());
    }

    @PostMapping("/admin/expire")
    public ResponseEntity<?> expireNow() {
        service.autoExpireDonations();
        return ResponseEntity.ok(Map.of("success", true, "message", "Expiry sweep completed"));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> allUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @GetMapping("/admin/donations")
    public ResponseEntity<?> allDonations() {
        return ResponseEntity.ok(service.getAllDonations());
    }

    @GetMapping("/admin/requests")
    public ResponseEntity<?> allRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }
}
