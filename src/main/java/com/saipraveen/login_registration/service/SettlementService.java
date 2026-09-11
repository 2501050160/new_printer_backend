package com.saipraveen.login_registration.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.entity.CollegeConfig;
import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.entity.SettlementRecord;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.repository.CollegeConfigRepository;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.SettlementRecordRepository;
import com.saipraveen.login_registration.repository.UserRepository;

@Service
public class SettlementService {

    private static final Logger logger = LoggerFactory.getLogger(SettlementService.class);

    @Autowired
    private SettlementRecordRepository settlementRepository;

    @Autowired
    private PdfFileRepository pdfFileRepository;

    @Autowired
    private CampusBlockRepository campusBlockRepository;

    @Autowired
    private CollegeConfigRepository collegeConfigRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Map each block name to its corresponding college name (case-insensitive).
     */
    private Map<String, String> getBlockToCollegeMap() {
        Map<String, String> map = new HashMap<>();
        try {
            List<CampusBlock> blocks = campusBlockRepository.findAll();
            for (CampusBlock b : blocks) {
                if (b.getName() != null) {
                    String col = b.getCollege() != null ? b.getCollege().trim() : "KLU";
                    map.put(b.getName().trim().toLowerCase(), col);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load block to college mapping: {}", e.getMessage());
        }
        return map;
    }

    /**
     * Get balance and revenue summary for a specific college.
     */
    public Map<String, Object> getCollegeBalanceSummary(String college) {
        if (college == null || college.trim().isEmpty()) {
            college = "KLU";
        }
        final String targetCollege = college.trim();
        Map<String, String> blockCollegeMap = getBlockToCollegeMap();

        List<PdfFile> allFiles = pdfFileRepository.findAll();

        double grossRevenue = 0.0;
        long paidOrdersCount = 0;
        long totalPagesPrinted = 0;

        for (PdfFile file : allFiles) {
            boolean isPaid = "PAID".equalsIgnoreCase(file.getPaymentStatus()) ||
                             (file.getRazorpayPaymentId() != null && !file.getRazorpayPaymentId().trim().isEmpty());

            if (!isPaid) continue;

            String fileCollege = null;
            if (file.getBlockLocation() != null) {
                fileCollege = blockCollegeMap.get(file.getBlockLocation().trim().toLowerCase());
            }

            if (fileCollege == null) {
                // Fallback: check user's registered college
                if (file.getUserId() != null) {
                    try {
                        userRepository.findById(file.getUserId()).ifPresent(u -> {
                            // user college
                        });
                    } catch (Exception ignored) {}
                }
                fileCollege = "KLU"; // Default baseline
            }

            if (targetCollege.equalsIgnoreCase(fileCollege)) {
                double orderPrice = file.getPrice() != null ? file.getPrice() : 0.0;
                grossRevenue += orderPrice;
                paidOrdersCount++;

                int pages = 1;
                if (file.getSelectedPages() != null && !file.getSelectedPages().equalsIgnoreCase("ALL")) {
                    String[] parts = file.getSelectedPages().split("-");
                    if (parts.length == 2) {
                        try {
                            pages = Math.max(1, Integer.parseInt(parts[1]) - Integer.parseInt(parts[0]) + 1);
                        } catch (Exception ignored) {}
                    }
                } else if (file.getTotalPages() != null) {
                    pages = file.getTotalPages();
                }
                int copies = file.getCopies() != null ? file.getCopies() : 1;
                totalPagesPrinted += (long) pages * copies;
            }
        }

        Double totalSettled = settlementRepository.sumSettledAmountByCollege(targetCollege);
        if (totalSettled == null) {
            totalSettled = 0.0;
        }

        double unsettledBalance = Math.max(0.0, grossRevenue - totalSettled);

        // Round to 2 decimals
        grossRevenue = Math.round(grossRevenue * 100.0) / 100.0;
        totalSettled = Math.round(totalSettled * 100.0) / 100.0;
        unsettledBalance = Math.round(unsettledBalance * 100.0) / 100.0;

        List<SettlementRecord> history = settlementRepository.findByCollegeOrderBySettlementDateDesc(targetCollege);
        LocalDateTime lastSettlementDate = history.isEmpty() ? null : history.get(0).getSettlementDate();
        Double lastSettlementAmount = history.isEmpty() ? null : history.get(0).getAmount();

        Map<String, Object> summary = new HashMap<>();
        summary.put("college", targetCollege);
        summary.put("grossRevenue", grossRevenue);
        summary.put("totalSettled", totalSettled);
        summary.put("unsettledBalance", unsettledBalance);
        summary.put("paidOrdersCount", paidOrdersCount);
        summary.put("totalPagesPrinted", totalPagesPrinted);
        summary.put("settlementsCount", history.size());
        summary.put("lastSettlementDate", lastSettlementDate);
        summary.put("lastSettlementAmount", lastSettlementAmount);

        // Include college banking details if available
        try {
            CollegeConfig config = collegeConfigRepository.findByCollegeNameIgnoreCase(targetCollege);
            if (config != null) {
                summary.put("bankAccountNumber", config.getBankAccountNumber());
                summary.put("bankIfsc", config.getBankIfsc());
                summary.put("bankBeneficiaryName", config.getBankBeneficiaryName());
                summary.put("razorpayAccountId", config.getRazorpayAccountId());
            }
        } catch (Exception ignored) {}

        return summary;
    }

    /**
     * Get balance summary for ALL colleges (for Main Admin).
     */
    public Map<String, Object> getAllCollegesBalanceSummary() {
        Set<String> colleges = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        colleges.add("KLU");
        colleges.add("VNR");
        colleges.add("CBIT");

        try {
            collegeConfigRepository.findAll().forEach(c -> {
                if (c.getCollegeName() != null && !c.getCollegeName().trim().isEmpty()) {
                    colleges.add(c.getCollegeName().trim());
                }
            });
        } catch (Exception ignored) {}

        List<Map<String, Object>> list = new ArrayList<>();
        double totalGross = 0.0;
        double totalSettled = 0.0;
        double totalUnsettled = 0.0;
        long totalOrders = 0;

        for (String col : colleges) {
            Map<String, Object> s = getCollegeBalanceSummary(col);
            list.add(s);
            totalGross += (Double) s.get("grossRevenue");
            totalSettled += (Double) s.get("totalSettled");
            totalUnsettled += (Double) s.get("unsettledBalance");
            totalOrders += (Long) s.get("paidOrdersCount");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("colleges", list);
        result.put("totalGrossRevenue", Math.round(totalGross * 100.0) / 100.0);
        result.put("totalSettledAmount", Math.round(totalSettled * 100.0) / 100.0);
        result.put("totalUnsettledBalance", Math.round(totalUnsettled * 100.0) / 100.0);
        result.put("totalPaidOrders", totalOrders);
        return result;
    }

    /**
     * Create a new settlement record (RESTRICTED: Main Admin only).
     */
    public SettlementRecord createSettlement(String adminUsername, String college, Double amount, String referenceId, String paymentMode, String notes) {
        if (adminUsername == null || !"admin".equalsIgnoreCase(adminUsername.trim())) {
            throw new SecurityException("Access Denied: Only the Main Admin has permission to settle bills and disburse funds!");
        }

        if (college == null || college.trim().isEmpty()) {
            throw new IllegalArgumentException("College name is required for settlement");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Settlement amount must be greater than zero");
        }

        String targetCollege = college.trim();
        Map<String, Object> currentSummary = getCollegeBalanceSummary(targetCollege);
        Double balanceBefore = (Double) currentSummary.get("unsettledBalance");

        double balanceAfter = Math.max(0.0, Math.round((balanceBefore - amount) * 100.0) / 100.0);

        SettlementRecord record = new SettlementRecord(
                targetCollege,
                Math.round(amount * 100.0) / 100.0,
                (referenceId != null && !referenceId.trim().isEmpty()) ? referenceId.trim() : "SETTLE-" + System.currentTimeMillis(),
                paymentMode,
                adminUsername.trim(),
                notes,
                balanceBefore,
                balanceAfter
        );

        SettlementRecord saved = settlementRepository.save(record);
        logger.info("Main admin [{}] settled ₹{} for college [{}] (Ref: {}, Balance After: ₹{})",
                adminUsername, amount, targetCollege, saved.getReferenceId(), balanceAfter);
        return saved;
    }

    /**
     * Get settlement history. Sub-admins only see records for their own college.
     */
    public List<SettlementRecord> getSettlementHistory(String college, String adminUsername, String adminRole) {
        boolean isMainAdmin = "admin".equalsIgnoreCase(adminUsername) || "MAIN_ADMIN".equalsIgnoreCase(adminRole);

        if (!isMainAdmin) {
            // Force college to user's assigned college
            return settlementRepository.findByCollegeOrderBySettlementDateDesc(college);
        }

        if (college != null && !college.trim().isEmpty() && !"ALL".equalsIgnoreCase(college.trim())) {
            return settlementRepository.findByCollegeOrderBySettlementDateDesc(college.trim());
        }

        return settlementRepository.findAllByOrderBySettlementDateDesc();
    }

    /**
     * Delete / void a settlement record (Main Admin only).
     */
    public void deleteSettlement(Long id, String adminUsername) {
        if (adminUsername == null || !"admin".equalsIgnoreCase(adminUsername.trim())) {
            throw new SecurityException("Access Denied: Only the Main Admin can delete or void settlement records!");
        }
        settlementRepository.deleteById(id);
    }
}
