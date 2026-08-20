package com.saipraveen.login_registration.service;

import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.entity.CollegeConfig;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.repository.CollegeConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String defaultKeyId;

    @Value("${razorpay.key.secret}")
    private String defaultKeySecret;

    @Autowired
    private PdfFileRepository pdfFileRepository;

    @Autowired
    private CampusBlockRepository campusBlockRepository;

    @Autowired
    private CollegeConfigRepository collegeConfigRepository;

    public Map<String, Object> createOrder(
            Double amount,
            String appOrderId
    ) throws Exception {

        String currentKeyId = defaultKeyId;
        String currentKeySecret = defaultKeySecret;

        try {
            PdfFile pdfFile = pdfFileRepository.findByOrderId(appOrderId);
            String collegeName = null;
            if (pdfFile != null && pdfFile.getBlockLocation() != null) {
                String loc = pdfFile.getBlockLocation().trim();
                CampusBlock block = campusBlockRepository.findByNameIgnoreCase(loc);
                collegeName = (block != null && block.getCollege() != null && !block.getCollege().trim().isEmpty()) 
                    ? block.getCollege().trim() 
                    : loc;

                CollegeConfig config = collegeConfigRepository.findByCollegeNameIgnoreCase(collegeName);
                if (config != null && config.getRazorpayKeyId() != null && !config.getRazorpayKeyId().trim().isEmpty()) {
                    currentKeyId = config.getRazorpayKeyId().trim();
                    currentKeySecret = config.getRazorpayKeySecret() != null ? config.getRazorpayKeySecret().trim() : "";
                    logger.info("Using custom Razorpay keys for matched college: {} (key_id: {})", collegeName, currentKeyId);
                }
            }

            // Fallback: If no direct match or still on test keys, check if any configured college has live keys in DB
            if (currentKeyId == null || currentKeyId.startsWith("rzp_test") || currentKeyId.equals(defaultKeyId)) {
                java.util.List<CollegeConfig> allConfigs = collegeConfigRepository.findAll();
                for (CollegeConfig cc : allConfigs) {
                    if (cc.getRazorpayKeyId() != null && !cc.getRazorpayKeyId().trim().isEmpty()) {
                        currentKeyId = cc.getRazorpayKeyId().trim();
                        currentKeySecret = cc.getRazorpayKeySecret() != null ? cc.getRazorpayKeySecret().trim() : "";
                        logger.info("Fallback: using active database Razorpay credentials from college: {} (key_id: {})", cc.getCollegeName(), currentKeyId);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not determine dynamic keys for appOrderId: {}, error: {}", appOrderId, e.getMessage());
        }

        logger.info(">> Initializing RazorpayClient with Key ID: {} for appOrderId: {}", currentKeyId, appOrderId);

        RazorpayClient client =
                new RazorpayClient(
                        currentKeyId,
                        currentKeySecret
                );

        JSONObject options =
                new JSONObject();

        options.put(
                "amount",
                Math.round(amount * 100)
        );

        options.put(
                "currency",
                "INR"
        );

        options.put(
                "receipt",
                "receipt_" +
                        System.currentTimeMillis()
        );

        options.put("payment_capture", 1);

        JSONObject notes = new JSONObject();
        notes.put("app_order_id", appOrderId);
        options.put("notes", notes);
        try {

            Order order =
                    client.orders.create(
                            options
                    );

            logger.info("Razorpay order created successfully");
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> orderMap = mapper.readValue(order.toString(), Map.class);
            orderMap.put("key_id", currentKeyId);
            
            logger.info("Order ID: {} | Amount: {} | Status: {}", 
                orderMap.get("id"), 
                orderMap.get("amount"), 
                orderMap.get("status"));
            
            return orderMap;

        } catch (Exception e) {

            logger.error("Failed to create Razorpay order for amount: {} INR. Error: {}", amount, e.getMessage(), e);
            throw new RuntimeException("Payment order creation failed: " + e.getMessage(), e);
        }
    }
}