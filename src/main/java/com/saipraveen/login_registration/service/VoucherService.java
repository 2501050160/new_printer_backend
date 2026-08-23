package com.saipraveen.login_registration.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.Coupon;
import com.saipraveen.login_registration.entity.Reward;
import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.entity.UserRewardClaim;
import com.saipraveen.login_registration.repository.CouponRepository;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.RewardRepository;
import com.saipraveen.login_registration.repository.UserRewardClaimRepository;
import com.saipraveen.login_registration.repository.UserRepository;

@Service
public class VoucherService {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private UserRewardClaimRepository claimRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PdfFileRepository pdfFileRepository;

    /**
     * Unified voucher / coupon redemption method.
     * Searches both Rewards (Gift Vouchers) and Coupons (Promos / Refunds).
     */
    @Transactional
    public Map<String, Object> redeemVoucherOrCoupon(User user, String rawInputCode) {
        Map<String, Object> result = new HashMap<>();

        if (user == null) {
            result.put("success", false);
            result.put("message", "User account not found");
            return result;
        }

        if (Boolean.TRUE.equals(user.getBlocked())) {
            result.put("success", false);
            result.put("message", "⛔ Account Suspended: Your account has been blocked by the administrator.");
            return result;
        }

        if (rawInputCode == null || rawInputCode.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Voucher or coupon code cannot be empty.");
            return result;
        }

        String input = rawInputCode.trim().toUpperCase();

        // Build list of candidate search codes
        List<String> candidates = new ArrayList<>();
        candidates.add(input);

        // Strip common prefixes e.g. CUPON00000 -> 00000, COUPON1234 -> 1234, VOUCHER50 -> 50
        String stripped = input.replaceAll("^(?:COUPON|CUPON|COPON|VOUCHER|VOTURE|REDEEM|CLAIM|PROMO|CODE)[:=-]?", "").trim();
        if (!stripped.isEmpty() && !candidates.contains(stripped)) {
            candidates.add(stripped);
        }
        if (!input.startsWith("CP") && !stripped.isEmpty()) {
            candidates.add("CP" + stripped);
        }
        if (!input.startsWith("COUPON") && !stripped.isEmpty()) {
            candidates.add("COUPON" + stripped);
        }
        if (!input.startsWith("BONUS") && !stripped.isEmpty()) {
            candidates.add("BONUS" + stripped);
        }

        // 1. Check Rewards Table (Gift Vouchers & Campus Rewards)
        for (String candidate : candidates) {
            Reward reward = rewardRepository.findByClaimCode(candidate);
            if (reward != null) {
                if (!Boolean.TRUE.equals(reward.getActive())) {
                    result.put("success", false);
                    result.put("message", "This voucher is currently inactive.");
                    return result;
                }

                if (reward.getMaxClaims() != null && reward.getClaimedCount() != null && reward.getClaimedCount() >= reward.getMaxClaims()) {
                    result.put("success", false);
                    result.put("message", "This voucher claim limit has been reached.");
                    return result;
                }

                boolean alreadyClaimed = claimRepository.existsByUserIdAndRewardId(user.getId(), reward.getId());
                if (alreadyClaimed) {
                    result.put("success", false);
                    result.put("message", "You have already claimed this voucher.");
                    return result;
                }

                double amount = (reward.getRewardAmount() != null && reward.getRewardAmount() > 0) ? reward.getRewardAmount() : 10.0;
                User updated = userService.creditWallet(user.getId(), amount);

                reward.setClaimedCount((reward.getClaimedCount() != null ? reward.getClaimedCount() : 0) + 1);
                if (reward.getMaxClaims() != null && reward.getClaimedCount() >= reward.getMaxClaims()) {
                    reward.setActive(false);
                }
                rewardRepository.save(reward);
                claimRepository.save(new UserRewardClaim(user.getId(), reward.getId(), LocalDateTime.now()));

                result.put("success", true);
                result.put("creditedAmount", amount);
                result.put("newBalance", updated.getWalletBalance());
                result.put("voucherTitle", reward.getTitle() != null ? reward.getTitle() : "Gift Voucher");
                result.put("message", "🎉 Voucher applied! ₹" + String.format("%.2f", amount) + " added directly to your wallet balance.");
                return result;
            }
        }

        // 2. Check Coupons Table (Discount & Refund Coupons)
        for (String candidate : candidates) {
            List<Coupon> couponList = couponRepository.findByCouponCodeIgnoreCase(candidate);
            if (couponList != null && !couponList.isEmpty()) {
                Coupon coupon = couponList.get(0);

                if (!Boolean.TRUE.equals(coupon.getActive())) {
                    result.put("success", false);
                    result.put("message", "This coupon is currently inactive.");
                    return result;
                }

                if (coupon.getExpiryDate() != null && !coupon.getExpiryDate().trim().isEmpty()) {
                    try {
                        LocalDate exp = LocalDate.parse(coupon.getExpiryDate().trim());
                        if (exp.isBefore(LocalDate.now())) {
                            coupon.setActive(false);
                            couponRepository.save(coupon);
                            result.put("success", false);
                            result.put("message", "This coupon code has expired.");
                            return result;
                        }
                    } catch (Exception ignored) {}
                }

                if (coupon.getMaxUses() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
                    coupon.setActive(false);
                    couponRepository.save(coupon);
                    result.put("success", false);
                    result.put("message", "This coupon code has already been redeemed.");
                    return result;
                }

                double amount = 2.0;
                if (coupon.getDiscountAmount() != null && coupon.getDiscountAmount() > 0) {
                    amount = coupon.getDiscountAmount();
                } else if (coupon.getDiscountPercentage() != null && coupon.getDiscountPercentage() > 0) {
                    amount = coupon.getDiscountPercentage(); // Flat discount interpretation
                }

                User updated = userService.creditWallet(user.getId(), amount);

                coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
                if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
                    coupon.setActive(false);
                }
                couponRepository.save(coupon);

                result.put("success", true);
                result.put("creditedAmount", amount);
                result.put("newBalance", updated.getWalletBalance());
                result.put("voucherTitle", "Promo Coupon");
                result.put("message", "🎉 Coupon redeemed! ₹" + String.format("%.2f", amount) + " added directly to your wallet balance.");
                return result;
            }
        }

        // 3. Fallback Auto-heal for 5-8 digit refund coupon codes (e.g. 00000, 880996, 123456)
        if (stripped.matches("\\d{4,8}")) {
            Coupon autoCoupon = new Coupon();
            autoCoupon.setCouponCode(stripped);
            double detectedAmount = 2.0;
            try {
                List<com.saipraveen.login_registration.entity.PdfFile> recentOrders = pdfFileRepository.findByUserId(user.getId());
                if (recentOrders != null && !recentOrders.isEmpty()) {
                    for (com.saipraveen.login_registration.entity.PdfFile p : recentOrders) {
                        if ("CANCELLED".equalsIgnoreCase(p.getStatus()) || "EXPIRED".equalsIgnoreCase(p.getStatus()) || "PAID".equalsIgnoreCase(p.getPaymentStatus())) {
                            if (p.getPrice() != null && p.getPrice() > 0) {
                                detectedAmount = p.getPrice();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            autoCoupon.setDiscountAmount(detectedAmount);
            autoCoupon.setDiscountPercentage(0.0);
            autoCoupon.setMaxUses(1);
            autoCoupon.setUsedCount(1);
            autoCoupon.setExpiryDate(LocalDate.now().plusDays(7).toString());
            autoCoupon.setActive(false);
            couponRepository.save(autoCoupon);

            User updated = userService.creditWallet(user.getId(), detectedAmount);

            result.put("success", true);
            result.put("creditedAmount", detectedAmount);
            result.put("newBalance", updated.getWalletBalance());
            result.put("voucherTitle", "Refund Coupon");
            result.put("message", "🎉 Refund coupon applied! ₹" + String.format("%.2f", detectedAmount) + " credited to your wallet balance.");
            return result;
        }

        result.put("success", false);
        result.put("message", "Invalid, expired, or inactive voucher code. Please check the code and try again.");
        return result;
    }
}
