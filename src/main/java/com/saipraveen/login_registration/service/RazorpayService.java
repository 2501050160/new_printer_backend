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
            if (pdfFile != null) {
                CampusBlock block = campusBlockRepository.findByName(pdfFile.getBlockLocation());
                if (block != null && block.getCollege() != null) {
                    CollegeConfig config = collegeConfigRepository.findByCollegeName(block.getCollege());
                    if (config != null) {
                        currentKeyId = config.getRazorpayKeyId();
                        currentKeySecret = config.getRazorpayKeySecret();
                        logger.info("Using custom Razorpay keys for college: {}", block.getCollege());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not determine dynamic keys for appOrderId: {}, falling back to default keys.", appOrderId);
        }

        RazorpayClient client =
                new RazorpayClient(
                        currentKeyId,
                        currentKeySecret
                );

        JSONObject options =
                new JSONObject();

        options.put(
                "amount",
                amount * 100
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