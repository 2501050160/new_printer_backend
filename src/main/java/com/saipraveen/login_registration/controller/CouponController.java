package com.saipraveen.login_registration.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.Coupon;
import com.saipraveen.login_registration.repository.CouponRepository;

@RestController
@RequestMapping("/api/coupon")
@CrossOrigin(origins = "*")
public class CouponController {

    @Autowired(required = false)
    private CouponRepository repository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handleException(Throwable t) {
        t.printStackTrace();
        String msg = t.getMessage() != null ? t.getMessage() : t.toString();
        return ResponseEntity.ok(Map.of("error", msg, "class", t.getClass().getName()));
    }

    private Map<String, Object> mapCoupon(Coupon c) {
        if (c == null) return Collections.emptyMap();
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("couponCode", c.getCouponCode());
        m.put("discountPercentage", c.getDiscountPercentage() != null ? c.getDiscountPercentage() : 0.0);
        m.put("discountAmount", c.getDiscountAmount() != null ? c.getDiscountAmount() : 0.0);
        m.put("minOrderAmount", c.getMinOrderAmount() != null ? c.getMinOrderAmount() : 0.0);
        m.put("expiryDate", c.getExpiryDate() != null ? c.getExpiryDate() : null);
        m.put("maxUses", c.getMaxUses() != null ? c.getMaxUses() : 100);
        m.put("usedCount", c.getUsedCount() != null ? c.getUsedCount() : 0);
        m.put("active", c.getActive() != null ? c.getActive() : true);
        return m;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCoupon(@RequestBody(required = false) Map<String, Object> body) {
        try {
            Coupon coupon = new Coupon();
            if (body != null) {
                if (body.get("id") != null) {
                    try {
                        coupon.setId(Long.parseLong(body.get("id").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("couponCode") != null) {
                    coupon.setCouponCode(body.get("couponCode").toString().trim().toUpperCase());
                }
                if (body.get("discountPercentage") != null) {
                    try {
                        coupon.setDiscountPercentage(Double.parseDouble(body.get("discountPercentage").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("discountAmount") != null) {
                    try {
                        coupon.setDiscountAmount(Double.parseDouble(body.get("discountAmount").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("minOrderAmount") != null) {
                    try {
                        coupon.setMinOrderAmount(Double.parseDouble(body.get("minOrderAmount").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("maxUses") != null) {
                    try {
                        coupon.setMaxUses(Integer.parseInt(body.get("maxUses").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("active") != null) {
                    coupon.setActive(Boolean.parseBoolean(body.get("active").toString()));
                }
                if (body.get("expiryDate") != null && !body.get("expiryDate").toString().trim().isEmpty()) {
                    String dateStr = body.get("expiryDate").toString().trim();
                    if (dateStr.contains("T")) {
                        dateStr = dateStr.split("T")[0];
                    }
                    coupon.setExpiryDate(dateStr);
                } else {
                    coupon.setExpiryDate(LocalDate.now().plusDays(30).toString());
                }
            }

            if (coupon.getCouponCode() == null || coupon.getCouponCode().trim().isEmpty()) {
                coupon.setCouponCode("PRINT" + (1000 + new java.util.Random().nextInt(9000)));
            }
            if (coupon.getUsedCount() == null) coupon.setUsedCount(0);
            if (coupon.getActive() == null) coupon.setActive(true);

            // Primary strategy: JPA save
            if (repository != null) {
                try {
                    List<Coupon> existingList = repository.findByCouponCodeIgnoreCase(coupon.getCouponCode());
                    if (existingList != null && !existingList.isEmpty()) {
                        Coupon existing = existingList.get(0);
                        if (coupon.getDiscountPercentage() != null) existing.setDiscountPercentage(coupon.getDiscountPercentage());
                        if (coupon.getDiscountAmount() != null) existing.setDiscountAmount(coupon.getDiscountAmount());
                        if (coupon.getMinOrderAmount() != null) existing.setMinOrderAmount(coupon.getMinOrderAmount());
                        if (coupon.getExpiryDate() != null) existing.setExpiryDate(coupon.getExpiryDate());
                        if (coupon.getMaxUses() != null) existing.setMaxUses(coupon.getMaxUses());
                        existing.setActive(coupon.getActive() != null ? coupon.getActive() : true);
                        Coupon saved = repository.save(existing);
                        return ResponseEntity.ok(mapCoupon(saved));
                    }
                    Coupon saved = repository.save(coupon);
                    return ResponseEntity.ok(mapCoupon(saved));
                } catch (Exception jpaEx) {
                    System.err.println("Notice: JPA save failed, falling back to JDBC: " + jpaEx.getMessage());
                }
            }

            // Fallback strategy: Direct JDBC insert/update
            if (jdbcTemplate != null) {
                jdbcTemplate.update(
                    "INSERT INTO coupons (coupon_code, discount_percentage, discount_amount, min_order_amount, expiry_date, max_uses, used_count, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (coupon_code) DO UPDATE SET " +
                    "discount_percentage = EXCLUDED.discount_percentage, " +
                    "discount_amount = EXCLUDED.discount_amount, " +
                    "min_order_amount = EXCLUDED.min_order_amount, " +
                    "expiry_date = EXCLUDED.expiry_date, " +
                    "max_uses = EXCLUDED.max_uses, " +
                    "active = EXCLUDED.active",
                    coupon.getCouponCode(),
                    coupon.getDiscountPercentage() != null ? coupon.getDiscountPercentage() : 0.0,
                    coupon.getDiscountAmount() != null ? coupon.getDiscountAmount() : 0.0,
                    coupon.getMinOrderAmount() != null ? coupon.getMinOrderAmount() : 0.0,
                    coupon.getExpiryDate(),
                    coupon.getMaxUses() != null ? coupon.getMaxUses() : 100,
                    coupon.getUsedCount() != null ? coupon.getUsedCount() : 0,
                    coupon.getActive() != null ? coupon.getActive() : true
                );
                return ResponseEntity.ok(mapCoupon(coupon));
            }

            return ResponseEntity.ok(mapCoupon(coupon));
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<?> createRefundCoupon(
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String code
    ) {
        try {
            Coupon coupon = new Coupon();
            String couponCode = (code != null && !code.trim().isEmpty()) 
                ? code.trim().toUpperCase() 
                : String.valueOf(100000 + new java.util.Random().nextInt(900000));
            coupon.setCouponCode(couponCode);
            coupon.setDiscountAmount(amount != null && amount > 0 ? amount : 2.0);
            coupon.setDiscountPercentage(0.0);
            coupon.setExpiryDate(LocalDate.now().plusDays(7).toString());
            coupon.setMaxUses(1);
            coupon.setUsedCount(0);
            coupon.setActive(true);
            if (repository != null) {
                try {
                    Coupon saved = repository.save(coupon);
                    return ResponseEntity.ok(mapCoupon(saved));
                } catch (Exception ignored) {}
            }
            return ResponseEntity.ok(mapCoupon(coupon));
        } catch (Throwable e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getCoupons() {
        try {
            if (repository != null) {
                try {
                    List<Coupon> raw = repository.findAll();
                    List<Map<String, Object>> result = new ArrayList<>();
                    if (raw != null) {
                        for (Coupon c : raw) {
                            result.add(mapCoupon(c));
                        }
                    }
                    return ResponseEntity.ok(result);
                } catch (Exception jpaEx) {
                    System.err.println("Notice: JPA findAll failed, using JDBC fallback: " + jpaEx.getMessage());
                }
            }

            if (jdbcTemplate != null) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, coupon_code as \"couponCode\", discount_percentage as \"discountPercentage\", " +
                    "discount_amount as \"discountAmount\", min_order_amount as \"minOrderAmount\", " +
                    "expiry_date as \"expiryDate\", max_uses as \"maxUses\", used_count as \"usedCount\", active " +
                    "FROM coupons ORDER BY id DESC"
                );
                return ResponseEntity.ok(rows != null ? rows : Collections.emptyList());
            }

            return ResponseEntity.ok(Collections.emptyList());
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestParam String couponCode) {
        try {
            if (couponCode == null || couponCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon code required"));
            }
            String cleanCode = couponCode.trim().toUpperCase();

            if (repository != null) {
                try {
                    List<Coupon> list = repository.findByCouponCodeIgnoreCase(cleanCode);
                    if (list != null && !list.isEmpty()) {
                        return ResponseEntity.ok(mapCoupon(list.get(0)));
                    }
                } catch (Exception ignored) {}
            }

            if (jdbcTemplate != null) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, coupon_code as \"couponCode\", discount_percentage as \"discountPercentage\", " +
                    "discount_amount as \"discountAmount\", min_order_amount as \"minOrderAmount\", " +
                    "expiry_date as \"expiryDate\", max_uses as \"maxUses\", used_count as \"usedCount\", active " +
                    "FROM coupons WHERE UPPER(coupon_code) = ?",
                    cleanCode
                );
                if (rows != null && !rows.isEmpty()) {
                    return ResponseEntity.ok(rows.get(0));
                }
            }

            return ResponseEntity.badRequest().body(Map.of("message", "Coupon Not Found"));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Invalid coupon"));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<?> useCoupon(@RequestParam String couponCode) {
        try {
            if (couponCode == null || couponCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon code required"));
            }
            String cleanCode = couponCode.trim().toUpperCase();

            if (repository != null) {
                try {
                    List<Coupon> list = repository.findByCouponCodeIgnoreCase(cleanCode);
                    if (list != null && !list.isEmpty()) {
                        Coupon c = list.get(0);
                        c.setUsedCount((c.getUsedCount() != null ? c.getUsedCount() : 0) + 1);
                        if (c.getMaxUses() != null && c.getUsedCount() >= c.getMaxUses()) {
                            c.setActive(false);
                        }
                        Coupon saved = repository.save(c);
                        return ResponseEntity.ok(mapCoupon(saved));
                    }
                } catch (Exception ignored) {}
            }

            if (jdbcTemplate != null) {
                jdbcTemplate.update(
                    "UPDATE coupons SET used_count = COALESCE(used_count, 0) + 1, " +
                    "active = CASE WHEN COALESCE(used_count, 0) + 1 >= COALESCE(max_uses, 100) THEN FALSE ELSE active END " +
                    "WHERE UPPER(coupon_code) = ?",
                    cleanCode
                );
                return ResponseEntity.ok(Map.of("message", "Coupon used successfully"));
            }

            return ResponseEntity.badRequest().body(Map.of("message", "Failed to use coupon"));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to use coupon"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteCoupon(@RequestParam Long id) {
        try {
            if (id != null) {
                if (repository != null) {
                    try {
                        repository.deleteById(id);
                    } catch (Exception ignored) {}
                }
                if (jdbcTemplate != null) {
                    jdbcTemplate.update("DELETE FROM coupons WHERE id = ?", id);
                }
            }
            return ResponseEntity.ok(Map.of("message", "Coupon Deleted"));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to delete coupon"));
        }
    }
}