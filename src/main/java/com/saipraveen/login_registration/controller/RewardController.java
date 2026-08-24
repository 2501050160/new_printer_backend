package com.saipraveen.login_registration.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.Reward;
import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.entity.UserRewardClaim;
import com.saipraveen.login_registration.repository.RewardRepository;
import com.saipraveen.login_registration.repository.UserRewardClaimRepository;
import com.saipraveen.login_registration.service.UserService;

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(origins = "http://localhost:5173")
public class RewardController {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private UserRewardClaimRepository claimRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private com.saipraveen.login_registration.repository.UserRepository userRepository;

    @Autowired
    private com.saipraveen.login_registration.service.VoucherService voucherService;

    @GetMapping("/all")
    public ResponseEntity<List<Reward>> getAllRewards() {
        return ResponseEntity.ok(rewardRepository.findAll());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createReward(@RequestBody Reward reward) {
        if (reward.getClaimCode() == null || reward.getClaimCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Claim code cannot be empty");
        }
        if (reward.getRewardAmount() == null || reward.getRewardAmount() <= 0) {
            return ResponseEntity.badRequest().body("Reward amount must be greater than 0");
        }
        
        reward.setClaimCode(reward.getClaimCode().trim().toUpperCase());
        Reward existing = rewardRepository.findByClaimCode(reward.getClaimCode());
        if (existing != null) {
            return ResponseEntity.badRequest().body("Reward with this claim code already exists");
        }
        
        if (reward.getClaimedCount() == null) {
            reward.setClaimedCount(0);
        }
        if (reward.getActive() == null) {
            reward.setActive(true);
        }
        
        Reward saved = rewardRepository.save(reward);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/claim")
    public ResponseEntity<?> claimReward(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String claimCode,
            @RequestParam(required = false) String code
    ) {
        Map<String, Object> response = new HashMap<>();
        String rawCode = (claimCode != null && !claimCode.trim().isEmpty()) ? claimCode : code;
        if (rawCode == null || rawCode.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Claim code cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        if (userId == null) {
            response.put("success", false);
            response.put("message", "User ID is required");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.badRequest().body(response);
        }

        Map<String, Object> result = voucherService.redeemVoucherOrCoupon(user, rawCode);
        if (Boolean.TRUE.equals(result.get("success"))) {
            result.put("amount", result.get("creditedAmount"));
            result.put("walletBalance", result.get("newBalance"));
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/update-status")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> updateStatus(@RequestParam Long id, @RequestParam Boolean active) {
        return rewardRepository.findById(id).map(rew -> {
            rew.setActive(active);
            rewardRepository.save(rew);
            return ResponseEntity.ok("Reward status updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @org.springframework.transaction.annotation.Transactional
    @RequestMapping(value = {"/delete", "/delete/{id}", "/{id}"}, method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<?> deleteReward(
            @RequestParam(required = false) Long id,
            @PathVariable(required = false) Long idPath,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Long targetId = id;
        if (targetId == null) {
            targetId = idPath;
        }
        if (targetId == null && body != null && body.containsKey("id")) {
            try {
                targetId = Long.valueOf(body.get("id").toString());
            } catch (Exception ignored) {}
        }

        if (targetId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Reward ID is required for deletion");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            claimRepository.deleteByRewardId(targetId);
            if (rewardRepository.existsById(targetId)) {
                rewardRepository.deleteById(targetId);
            }
            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("message", "Reward deleted successfully");
            return ResponseEntity.ok(ok);
        } catch (Exception ex) {
            System.err.println("Error deleting reward " + targetId + ": " + ex.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Could not delete reward: " + ex.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000)
    @org.springframework.transaction.annotation.Transactional
    public void autoDeleteInvalidRewards() {
        List<Reward> allRewards = rewardRepository.findAll();
        for (Reward reward : allRewards) {
            boolean fullyClaimed = reward.getClaimedCount() != null && reward.getMaxClaims() != null && reward.getClaimedCount() >= reward.getMaxClaims();
            boolean inactive = Boolean.FALSE.equals(reward.getActive());
            if (fullyClaimed || inactive) {
                claimRepository.deleteByRewardId(reward.getId());
                rewardRepository.delete(reward);
                System.out.println("Auto-deleted invalid reward: " + reward.getClaimCode() + " (Reason: " + (fullyClaimed ? "Fully Claimed" : "Inactive") + ")");
            }
        }
    }
}

