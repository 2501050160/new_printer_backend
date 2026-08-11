package com.saipraveen.login_registration.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.Reward;
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
    public ResponseEntity<?> claimReward(@RequestParam Long userId, @RequestParam String claimCode) {
        Map<String, Object> response = new HashMap<>();
        if (claimCode == null || claimCode.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Claim code cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        String normalizedCode = claimCode.trim().toUpperCase();
        
        // Also support claiming referrals directly via the same box if it is entered here!
        // We can check if it matches a user's referral code and handle it, or stick strictly to reward claim code.
        // Wait, standard reward code claim:
        Reward reward = rewardRepository.findByClaimCode(normalizedCode);
        if (reward == null) {
            response.put("success", false);
            response.put("message", "Invalid reward claim code");
            return ResponseEntity.badRequest().body(response);
        }

        if (!reward.getActive()) {
            response.put("success", false);
            response.put("message", "This reward is currently inactive");
            return ResponseEntity.badRequest().body(response);
        }

        if (reward.getClaimedCount() >= reward.getMaxClaims()) {
            response.put("success", false);
            response.put("message", "This reward claim limit has been reached");
            return ResponseEntity.badRequest().body(response);
        }

        boolean alreadyClaimed = claimRepository.existsByUserIdAndRewardId(userId, reward.getId());
        if (alreadyClaimed) {
            response.put("success", false);
            response.put("message", "You have already claimed this reward");
            return ResponseEntity.badRequest().body(response);
        }

        // Process claim (credit wallet balance)
        userService.creditWallet(userId, reward.getRewardAmount());
        
        // Record claim
        claimRepository.save(new UserRewardClaim(userId, reward.getId(), LocalDateTime.now()));
        
        // Increment claimed count
        reward.setClaimedCount(reward.getClaimedCount() + 1);
        rewardRepository.save(reward);

        response.put("success", true);
        response.put("message", "Reward claimed successfully! Rs. " + reward.getRewardAmount() + " added to your wallet.");
        response.put("amount", reward.getRewardAmount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestParam Long id, @RequestParam Boolean active) {
        return rewardRepository.findById(id).map(rew -> {
            rew.setActive(active);
            rewardRepository.save(rew);
            return ResponseEntity.ok("Reward status updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteReward(@RequestParam Long id) {
        if (rewardRepository.existsById(id)) {
            claimRepository.deleteByRewardId(id);
            rewardRepository.deleteById(id);
            return ResponseEntity.ok("Reward deleted successfully");
        }
        return ResponseEntity.notFound().build();
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
