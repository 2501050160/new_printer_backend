package com.saipraveen.login_registration.service;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Sides;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.printing.PDFPageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.UserRepository;
import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.repository.CampusBlockRepository;

@Service
public class PdfFileService {
@Autowired
private QueueService queueService;

@Autowired
private UserService userService;

@Autowired
private PdfFileRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private PrinterConfigService printerConfigService;

    @Autowired
    private CampusBlockRepository blockRepository;

    @Autowired
    private GoogleDriveService googleDriveService;

    @Autowired
    private SseService sseService;

    @jakarta.annotation.PostConstruct
    public void initSystemSettings() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                initSetting("referral_enabled", "true");
                initSetting("referral_referrer_amount", "10.0");
                initSetting("referral_referee_amount", "5.0");
                initSetting("referral_popup_enabled", "true");
                initSetting("referral_popup_message", "Welcome! Share your referral code with friends. They get Rs. 5 and you get Rs. 10 on their first checkout!");
                initSetting("ad_enabled", "true");
                initSetting("ad_text", "📢 REFERRAL SPECIAL: Refer your friends using your unique Referral Code shown below and earn ₹10 instantly when they checkout! They get ₹5 off on their first order!");
                initSetting("offpeak_discount_percent", "15.0");
                initSetting("offpeak_start_hour", "21.0");
                initSetting("offpeak_end_hour", "7.0");
                initSetting("offpeak_morning_start", "7.0");
                initSetting("offpeak_morning_end", "9.0");
                systemSettingService.setSetting("admin_sms_phone", "9494189664");
            } catch (Exception e) {
                System.err.println("Warning: Default system settings initialization deferred: " + e.getMessage());
            }
        });
    }

    private void initSetting(String key, String defaultValue) {
        if (systemSettingService.getSetting(key, null) == null) {
            systemSettingService.setSetting(key, defaultValue);
        }
    }

public PdfFile savePdf(
        MultipartFile file,
        Long userId,
        String customerName,
        String blockLocation)
        throws IOException {

    PdfFile pdf = new PdfFile();

    // User Information
    pdf.setUserId(userId);
    pdf.setCustomerName(
            resolveCustomerName(
                    userId,
                    customerName
            )
    );
    pdf.setBlockLocation(
            normalizeBlockLocation(
                    blockLocation
            )
    );

    // Default values
    pdf.setCopies(1);
    pdf.setSelectedPages("ALL");

    // Set temporary order ID placeholder
    pdf.setOrderId("TEMP_PENDING");

    // Upload Time
    pdf.setUploadTime(LocalDateTime.now());

    // File Details
    byte[] fileBytes = file.getBytes();
    String contentType = file.getContentType();
    String filename = file.getOriginalFilename();
    boolean isImage = false;
    if (filename != null) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
            isImage = true;
        }
    }

    if (isImage) {
        fileBytes = convertImageToPdf(fileBytes, contentType);
        pdf.setFileType("application/pdf");
        if (filename != null && !filename.toLowerCase().endsWith(".pdf")) {
            pdf.setFileName(filename + ".pdf");
        } else {
            pdf.setFileName(filename);
        }
    } else {
        pdf.setFileType(contentType);
        pdf.setFileName(filename);
    }
    pdf.setFileSize((long) fileBytes.length);
    pdf.setUploadTime(LocalDateTime.now());
    pdf.setFileExpiryTime(LocalDateTime.now().plusDays(1)); // Auto-expiry after 1 day

    // Save to Google Drive instead of Database byte array
    if (googleDriveService != null && googleDriveService.isConfigured()) {
        try {
            com.google.api.services.drive.model.File driveFile = googleDriveService.uploadFile(
                    pdf.getFileName(), 
                    pdf.getFileType() != null ? pdf.getFileType() : "application/pdf", 
                    fileBytes
            );
            pdf.setGoogleDriveFileId(driveFile.getId());
            pdf.setGoogleDriveWebViewLink(driveFile.getWebViewLink());
            pdf.setPdfData(null); // Keep database lean (no byte array in DB)
            System.out.println("[GoogleDrive] Saved " + pdf.getFileName() + " to Google Drive (ID: " + driveFile.getId() + ")");
        } catch (Exception e) {
            System.err.println("[GoogleDrive] Upload failed, falling back to database bytecode: " + e.getMessage());
            pdf.setPdfData(fileBytes);
        }
    } else {
        pdf.setPdfData(fileBytes);
    }

    // Calculate PDF Page Count
    try (PDDocument document =
                 Loader.loadPDF(fileBytes)) {

        int pageCount =
                document.getNumberOfPages();

        pdf.setTotalPages(pageCount);

        System.out.println(
                "Total Pages = " + pageCount);
    }

    pdf.setStatus("DRAFT");
    pdf.setPaymentStatus("UNPAID");

    PdfFile savedPdf = repository.save(pdf);
    savedPdf.setOrderId("DRAFT_" + savedPdf.getId());
    PdfFile finalPdf = repository.save(savedPdf);
    if (sseService != null) {
        sseService.broadcastOrderEvent(finalPdf.getOrderId(), "ORDER_CREATED");
        sseService.broadcastQueueEvent("Order created: " + finalPdf.getOrderId());
    }
    return finalPdf;
}

private byte[] convertImageToPdf(byte[] imageBytes, String contentType) throws IOException {
    try (PDDocument document = new PDDocument();
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        
        float pageWidth = PDRectangle.A4.getWidth();
        float pageHeight = PDRectangle.A4.getHeight();
        if (image.getWidth() > image.getHeight()) {
            // Use landscape A4 if image is wider than it is tall
            pageWidth = PDRectangle.A4.getHeight();
            pageHeight = PDRectangle.A4.getWidth();
        }
        
        PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
        document.addPage(page);

        PDImageXObject pdImage = (contentType != null && contentType.contains("png"))
                ? LosslessFactory.createFromImage(document, image)
                : JPEGFactory.createFromImage(document, image);

        float imgWidth = image.getWidth();
        float imgHeight = image.getHeight();
        float scaleX = pageWidth / imgWidth;
        float scaleY = pageHeight / imgHeight;
        float scale = Math.min(scaleX, scaleY);

        float w = imgWidth * scale;
        float h = imgHeight * scale;
        float x = (pageWidth - w) / 2;
        float y = (pageHeight - h) / 2;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(pdImage, x, y, w, h);
        }
        document.save(baos);
        return baos.toByteArray();
    }
}

public PdfFile updateOrder(
        String orderId,
        Integer copies,
        String selectedPages,
        String printType,
        String blockLocation,
        String nupLayout) {
    return updateOrder(orderId, copies, selectedPages, printType, blockLocation, nupLayout, false);
}

public PdfFile updateOrder(
        String orderId,
        Integer copies,
        String selectedPages,
        String printType,
        String blockLocation,
        String nupLayout,
        Boolean doubleSided) {

    PdfFile pdf =
            repository.findByOrderId(orderId);

    if (pdf == null) {
        throw new RuntimeException(
                "Order not found");
    }

    // Generate real sequential Order ID ONLY when proceeding to checkout/order!
    if (pdf.getOrderId() != null && pdf.getOrderId().startsWith("DRAFT_")) {
        Long lastId = repository.getLastId();
        long nextId = (lastId != null ? lastId : 0L) + 1;
        String realOrderId = "ORD2026" + String.format("%04d", nextId);
        pdf.setOrderId(realOrderId);
        pdf.setStatus("ORDER_CREATED");
    }

    pdf.setCopies(copies);
    pdf.setSelectedPages(selectedPages);
    pdf.setPrintType(printType);
    pdf.setBlockLocation(
            normalizeBlockLocation(
                    blockLocation
            )
    );
    pdf.setNupLayout(nupLayout);
    pdf.setDoubleSided(doubleSided);

    try {
        com.saipraveen.login_registration.entity.PrinterConfig assigned = printerConfigService.getPrinterByBlockAndType(pdf.getBlockLocation(), pdf.getPrintType());
        if (assigned != null && assigned.getPrinterName() != null) {
            pdf.setAssignedPrinterName(assigned.getPrinterName());
        }
    } catch (Exception e) {
        System.err.println("Failed to resolve assigned printer name: " + e.getMessage());
    }

    int pages = 1;

    if ("ALL".equals(selectedPages) || selectedPages == null || selectedPages.trim().isEmpty()) {
        pages = pdf.getTotalPages() != null ? pdf.getTotalPages() : 1;
    } else {
        try {
            String[] range = selectedPages.split("-");
            if (range.length == 2) {
                pages = Integer.parseInt(range[1].trim())
                        - Integer.parseInt(range[0].trim())
                        + 1;
            } else if (range.length == 1) {
                pages = 1;
            }
        } catch (Exception e) {
            pages = 1;
        }
    }

    int actualSheets = pages;
    if ("2-up".equals(nupLayout)) {
        actualSheets = (int) Math.ceil(pages / 2.0);
    } else if ("4-up".equals(nupLayout)) {
        actualSheets = (int) Math.ceil(pages / 4.0);
    } else if ("6-up".equals(nupLayout)) {
        actualSheets = (int) Math.ceil(pages / 6.0);
    } else if ("8-up".equals(nupLayout)) {
        actualSheets = (int) Math.ceil(pages / 8.0);
    } else if ("9-up".equals(nupLayout)) {
        actualSheets = (int) Math.ceil(pages / 9.0);
    }

    int paperSheets = actualSheets;
    if (Boolean.TRUE.equals(doubleSided)) {
        paperSheets = (int) Math.ceil(actualSheets / 2.0);
    }

    Double rate = null;
    if (Boolean.TRUE.equals(doubleSided)) {
        rate = pricingService.getPrice("DUPLEX", pdf.getBlockLocation());
        if (rate == null || rate == 0.0) {
            rate = pricingService.getPrice("BW_DUPLEX", pdf.getBlockLocation());
        }
    }
    if (rate == null || rate == 0.0) {
        rate = pricingService.getPrice(printType, pdf.getBlockLocation());
    }
    if (rate == null || rate == 0.0) {
        rate = "COLOR".equalsIgnoreCase(printType) ? 5.0 : 2.0;
    }

    double basePrice = paperSheets * copies * rate;

    // 1. Off-peak Dynamic Discount
    String college = "KLU";
    if (pdf.getBlockLocation() != null) {
        CampusBlock block = blockRepository.findByName(pdf.getBlockLocation());
        if (block != null && block.getCollege() != null) {
            college = block.getCollege();
        }
    }
    
    boolean offpeakEnabled = systemSettingService.getSettingBool("offpeak_enabled_" + college, systemSettingService.getSettingBool("offpeak_enabled", true));
    double offpeakDiscountPercent = systemSettingService.getSettingDouble("offpeak_discount_percent_" + college, systemSettingService.getSettingDouble("offpeak_discount_percent", 15.0));
    int offpeakStartHour = (int) systemSettingService.getSettingDouble("offpeak_start_hour_" + college, systemSettingService.getSettingDouble("offpeak_start_hour", 21.0));
    int offpeakEndHour = (int) systemSettingService.getSettingDouble("offpeak_end_hour_" + college, systemSettingService.getSettingDouble("offpeak_end_hour", 7.0));
    int offpeakMorningStart = (int) systemSettingService.getSettingDouble("offpeak_morning_start_" + college, systemSettingService.getSettingDouble("offpeak_morning_start", 7.0));
    int offpeakMorningEnd = (int) systemSettingService.getSettingDouble("offpeak_morning_end_" + college, systemSettingService.getSettingDouble("offpeak_morning_end", 9.0));

    int hour = java.time.LocalDateTime.now().getHour();
    boolean isMorningOffPeak = (hour >= offpeakMorningStart && hour < offpeakMorningEnd);
    boolean isEveningOffPeak;
    if (offpeakStartHour > offpeakEndHour) {
        isEveningOffPeak = (hour >= offpeakStartHour || hour < offpeakEndHour);
    } else {
        isEveningOffPeak = (hour >= offpeakStartHour && hour < offpeakEndHour);
    }
    boolean isOffPeak = offpeakEnabled && (isMorningOffPeak || isEveningOffPeak);
    double dynamicDiscountPercent = isOffPeak ? offpeakDiscountPercent : 0.0;

    // 2. Thesis/Bulk print discount (College-wise)
    double thesisDiscountPercent = systemSettingService.getSettingDouble("thesis_discount_percent_" + college, systemSettingService.getSettingDouble("thesis_discount_percent", 15.0));
    double thesisDiscountPages = systemSettingService.getSettingDouble("thesis_discount_pages_" + college, systemSettingService.getSettingDouble("thesis_discount_pages", 50.0));
    double thesisDiscount = 0.0;
    int totalPagesToPrint = pages * (copies != null ? copies : 1);
    if (totalPagesToPrint >= (int) thesisDiscountPages) {
        thesisDiscount = thesisDiscountPercent;
    }

    double totalDiscountPercent = dynamicDiscountPercent + thesisDiscount;
    double discountAmount = basePrice * (totalDiscountPercent / 100.0);
    double finalPrice = Math.max(0.0, basePrice - discountAmount);

    pdf.setOriginalPrice(basePrice);
    pdf.setDiscountAmount(discountAmount);
    pdf.setPrice(finalPrice);

    return repository.save(pdf);
}

public PdfFile updateStatus(
        Long id,
        String status) {

    PdfFile pdf =
            repository.findById(id)
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "Order Not Found"
                            )
                    );

    pdf.setStatus(status);

    if ("COMPLETED".equals(status)) {
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPaymentStatus("PAID");
    }

    if ("PRINTING".equals(status) && pdf.getPrintingStartedAt() == null) {
        pdf.setPrintingStartedAt(LocalDateTime.now());
        pdf.setPaymentStatus("PAID");
    }

    if ("QUEUE".equals(status)) {
        pdf.setPaymentStatus("PAID");
    }

    PdfFile saved = repository.save(pdf);
    if (sseService != null) {
        sseService.broadcastOrderEvent(saved.getOrderId(), status);
        sseService.broadcastQueueEvent("Order " + saved.getOrderId() + " status: " + status);
    }
    return saved;
}

public PdfFile updateStatusAndOtp(Long id, String status, String otpCode) {
    PdfFile pdf = repository.findById(id).orElseThrow(() -> new RuntimeException("Order Not Found"));
    pdf.setStatus(status);
    if (otpCode != null && !otpCode.trim().isEmpty()) {
        pdf.setOtpCode(otpCode.trim());
    }
    PdfFile saved = repository.save(pdf);
    if (sseService != null) {
        sseService.broadcastOrderEvent(saved.getOrderId(), status);
        sseService.broadcastQueueEvent("Order " + saved.getOrderId() + " status: " + status);
    }
    return saved;
}


public PdfFile updatePaymentStatus(
        Long id,
        String paymentStatus
) {

    PdfFile pdf =
            repository.findById(id)
            .orElseThrow(
                    () -> new RuntimeException(
                            "Order Not Found"
                    )
            );

    pdf.setPaymentStatus(
            paymentStatus
    );

    return repository.save(
            pdf
    );
}
    // Update payment status using order ID (used by Razorpay webhook)
    public PdfFile updatePaymentStatusByOrderId(String orderId, String paymentStatus) {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            throw new RuntimeException("Order Not Found");
        }
        pdf.setPaymentStatus(paymentStatus);
        return repository.save(pdf);
    }


public List<?> getAllOrders() {

    return repository.findAllProjectedByOrderByIdAsc();
}

public Map<String,Object> getDashboardStats(String period) {

    Map<String,Object> stats =
            new HashMap<>();

    LocalDateTime start = resolvePeriodStart(period);

    Double grossRevenue;
    Double totalDiscounts;
    Double netRevenue;

    if (start == null) {
        grossRevenue = repository.getGrossRevenueAll();
        totalDiscounts = repository.getTotalDiscountsAll();
        netRevenue = repository.getNetRevenueAll();
    } else {
        grossRevenue = repository.getGrossRevenueSince(start);
        totalDiscounts = repository.getTotalDiscountsSince(start);
        netRevenue = repository.getNetRevenueSince(start);
    }

    stats.put("period", period);
    stats.put("grossRevenue", grossRevenue == null ? 0.0 : grossRevenue);
    stats.put("totalDiscounts", totalDiscounts == null ? 0.0 : totalDiscounts);
    stats.put("netRevenue", netRevenue == null ? 0.0 : netRevenue);
    stats.put("totalRevenue", netRevenue == null ? 0.0 : netRevenue);

    stats.put(
            "todayRevenue",
            repository.getTodayRevenue()
    );

    stats.put(
            "completedOrders",
            repository.getCompletedOrders()
    );

    stats.put(
            "printingOrders",
            repository.getPrintingOrders()
    );

    stats.put(
            "totalOrders",
            repository.getTotalOrders()
    );

    stats.put(
            "totalPages",
            repository.getTotalPagesPrinted()
    );

    stats.put(
            "pendingOrders",
            repository.getPendingOrders()
    );

    return stats;
}

private LocalDateTime resolvePeriodStart(String period) {

    if (period == null || period.isBlank() || "all".equalsIgnoreCase(period)) {
        return null;
    }

    LocalDateTime now = LocalDateTime.now();

    return switch (period.toLowerCase()) {
        case "today" -> now.toLocalDate().atStartOfDay();
        case "week" -> now.minusDays(7);
        case "month" -> now.minusDays(30);
        default -> null;
    };
}


public PdfFile getPdfById(
        Long id) {

    return repository.findById(id)
            .orElseThrow(
                    () ->
                            new RuntimeException(
                                    "PDF Not Found"
                            )
            );
}

public byte[] getRawPdfBytes(PdfFile pdf) {
    if (pdf == null) {
        return null;
    }
    // 1. If stored in DB bytecode (legacy / fallback)
    if (pdf.getPdfData() != null) {
        return pdf.getPdfData();
    }
    // 2. If stored in Google Drive
    if (pdf.getGoogleDriveFileId() != null && googleDriveService != null && googleDriveService.isConfigured()) {
        try {
            return googleDriveService.downloadFile(pdf.getGoogleDriveFileId());
        } catch (Exception e) {
            System.err.println("[GoogleDrive] Failed to download file " + pdf.getGoogleDriveFileId() + ": " + e.getMessage());
        }
    }
    return null;
}

public byte[] getPrintablePdfData(PdfFile pdf) {
    byte[] rawData = getRawPdfBytes(pdf);
    if (rawData == null) {
        return null;
    }
    if ((pdf.getSelectedPages() == null || "ALL".equalsIgnoreCase(pdf.getSelectedPages().trim())) 
        && "1-up".equals(pdf.getNupLayout())) {
        return rawData;
    }
    try (PDDocument document = Loader.loadPDF(rawData)) {
        try (PDDocument filteredDoc = createPrintableDocument(document, pdf.getSelectedPages())) {
            
            PDDocument finalDoc = filteredDoc;
            if ("2-up".equals(pdf.getNupLayout()) || "4-up".equals(pdf.getNupLayout()) || 
                "6-up".equals(pdf.getNupLayout()) || "8-up".equals(pdf.getNupLayout()) || 
                "9-up".equals(pdf.getNupLayout())) {
                finalDoc = applyNupLayout(filteredDoc, pdf.getNupLayout());
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            finalDoc.save(out);
            
            if (finalDoc != filteredDoc) {
                finalDoc.close();
            }
            
            return out.toByteArray();
        }
    } catch (Exception e) {
        e.printStackTrace();
        return rawData; // Fallback to raw document on error
    }
}

private PDDocument applyNupLayout(PDDocument source, String layout) throws Exception {
    PDDocument out = new PDDocument();
    LayerUtility layerUtility = new LayerUtility(out);
    int totalPages = source.getNumberOfPages();
    int pagesPerSheet = "2-up".equals(layout) ? 2 : 
                        "4-up".equals(layout) ? 4 : 
                        "6-up".equals(layout) ? 6 : 
                        "8-up".equals(layout) ? 8 : 
                        "9-up".equals(layout) ? 9 : 1;
    
    for (int i = 0; i < totalPages; i += pagesPerSheet) {
        boolean isLandscape = "2-up".equals(layout) || "6-up".equals(layout) || "8-up".equals(layout);
        PDRectangle newPageMediaBox = isLandscape ?
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) : 
            PDRectangle.A4; 
        
        PDPage newPage = new PDPage(newPageMediaBox);
        out.addPage(newPage);

        try (PDPageContentStream contentStream = new PDPageContentStream(out, newPage)) {
            for (int j = 0; j < pagesPerSheet; j++) {
                if (i + j >= totalPages) break;
                
                PDPage originalPage = source.getPage(i + j);
                PDFormXObject form = layerUtility.importPageAsForm(source, originalPage);
                
                PDRectangle mediaBox = originalPage.getMediaBox();
                float scaleX, scaleY, scale, tx = 0, ty = 0;

                if ("2-up".equals(layout)) {
                    // Fit into half of landscape A4 -> Portrait A5 (421 x 595)
                    float targetW = newPageMediaBox.getWidth() / 2f; 
                    float targetH = newPageMediaBox.getHeight();     
                    scaleX = targetW / mediaBox.getWidth();
                    scaleY = targetH / mediaBox.getHeight();
                    scale = Math.min(scaleX, scaleY);
                    
                    float actW = mediaBox.getWidth() * scale;
                    float actH = mediaBox.getHeight() * scale;
                    
                    tx = (targetW - actW) / 2f;
                    ty = (targetH - actH) / 2f;
                    
                    if (j == 1) {
                        tx += targetW; 
                    }
                    
                    contentStream.saveGraphicsState();
                    contentStream.transform(Matrix.getTranslateInstance(tx, ty));
                    contentStream.transform(Matrix.getScaleInstance(scale, scale));
                    contentStream.drawForm(form);
                    contentStream.restoreGraphicsState();
                } else if ("4-up".equals(layout)) {
                    // Fit into quarter of portrait A4 -> Portrait A6 (297.5 x 421)
                    float targetW = newPageMediaBox.getWidth() / 2f;
                    float targetH = newPageMediaBox.getHeight() / 2f;
                    scaleX = targetW / mediaBox.getWidth();
                    scaleY = targetH / mediaBox.getHeight();
                    scale = Math.min(scaleX, scaleY);
                    
                    float actW = mediaBox.getWidth() * scale;
                    float actH = mediaBox.getHeight() * scale;
                    
                    tx = (targetW - actW) / 2f;
                    ty = (targetH - actH) / 2f;
                    
                    if (j % 2 == 1) tx += targetW; 
                    if (j < 2) ty += targetH;      // Draw top row (j=0,1) at the top (ty + targetH)
                    
                    contentStream.saveGraphicsState();
                    contentStream.transform(Matrix.getTranslateInstance(tx, ty));
                    contentStream.transform(Matrix.getScaleInstance(scale, scale));
                    contentStream.drawForm(form);
                    contentStream.restoreGraphicsState();
                } else if ("6-up".equals(layout)) {
                    // Fit into 1/6 of landscape A4 (3 cols x 2 rows)
                    float targetW = newPageMediaBox.getWidth() / 3f;
                    float targetH = newPageMediaBox.getHeight() / 2f;
                    scaleX = targetW / mediaBox.getWidth();
                    scaleY = targetH / mediaBox.getHeight();
                    scale = Math.min(scaleX, scaleY);
                    
                    float actW = mediaBox.getWidth() * scale;
                    float actH = mediaBox.getHeight() * scale;
                    tx = (targetW - actW) / 2f + (j % 3) * targetW;
                    ty = (targetH - actH) / 2f + (1 - (j / 3)) * targetH; 
                    
                    contentStream.saveGraphicsState();
                    contentStream.transform(Matrix.getTranslateInstance(tx, ty));
                    contentStream.transform(Matrix.getScaleInstance(scale, scale));
                    contentStream.drawForm(form);
                    contentStream.restoreGraphicsState();
                } else if ("8-up".equals(layout)) {
                    // Fit into 1/8 of landscape A4 (4 cols x 2 rows)
                    float targetW = newPageMediaBox.getWidth() / 4f;
                    float targetH = newPageMediaBox.getHeight() / 2f;
                    scaleX = targetW / mediaBox.getWidth();
                    scaleY = targetH / mediaBox.getHeight();
                    scale = Math.min(scaleX, scaleY);
                    
                    float actW = mediaBox.getWidth() * scale;
                    float actH = mediaBox.getHeight() * scale;
                    tx = (targetW - actW) / 2f + (j % 4) * targetW;
                    ty = (targetH - actH) / 2f + (1 - (j / 4)) * targetH; 
                    
                    contentStream.saveGraphicsState();
                    contentStream.transform(Matrix.getTranslateInstance(tx, ty));
                    contentStream.transform(Matrix.getScaleInstance(scale, scale));
                    contentStream.drawForm(form);
                    contentStream.restoreGraphicsState();
                } else if ("9-up".equals(layout)) {
                    // Fit into 1/9 of portrait A4 (3 cols x 3 rows)
                    float targetW = newPageMediaBox.getWidth() / 3f;
                    float targetH = newPageMediaBox.getHeight() / 3f;
                    scaleX = targetW / mediaBox.getWidth();
                    scaleY = targetH / mediaBox.getHeight();
                    scale = Math.min(scaleX, scaleY);
                    
                    float actW = mediaBox.getWidth() * scale;
                    float actH = mediaBox.getHeight() * scale;
                    tx = (targetW - actW) / 2f + (j % 3) * targetW;
                    ty = (targetH - actH) / 2f + (2 - (j / 3)) * targetH; 
                    
                    contentStream.saveGraphicsState();
                    contentStream.transform(Matrix.getTranslateInstance(tx, ty));
                    contentStream.transform(Matrix.getScaleInstance(scale, scale));
                    contentStream.drawForm(form);
                    contentStream.restoreGraphicsState();
                }
            }
        }
    }
    return out;
}


public List<?> getUserOrders(
        Long userId
) {

    return repository.findProjectedByUserId(
            userId
    );
}

public PdfFile markAsPaid(
        String orderId,
        String paymentId
) {
    if (orderId == null || orderId.trim().isEmpty()) {
        throw new RuntimeException("Order ID cannot be empty");
    }

    String cleanOrderId = orderId.trim();
    PdfFile pdf = repository.findByOrderId(cleanOrderId);

    if (pdf == null) {
        // Try finding by ID if cleanOrderId is numeric or formatted
        try {
            String digits = cleanOrderId.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                long id = Long.parseLong(digits);
                pdf = repository.findById(id).orElse(null);
            }
        } catch (Exception ignored) {}
    }

    if (pdf == null) {
        throw new RuntimeException("Order Not Found for ID: " + cleanOrderId);
    }

    // Set payment fields immediately
    pdf.setRazorpayPaymentId(paymentId != null ? paymentId : "PAID");
    pdf.setPaymentStatus("PAID");
    if (pdf.getPaidAt() == null) {
        pdf.setPaidAt(LocalDateTime.now());
    }

    // Generate random 4-digit OTP if not already present
    if (pdf.getOtpCode() == null || pdf.getOtpCode().trim().isEmpty()) {
        int randomOtp = 1000 + new java.util.Random().nextInt(9000);
        pdf.setOtpCode(String.valueOf(randomOtp));
    }

    try {
        processReferralRewards(pdf);
    } catch (Exception e) {
        System.err.println("Referral rewards error for " + cleanOrderId + ": " + e.getMessage());
    }

    try {
        queueService.beginCancelWindow(pdf);
    } catch (Exception e) {
        System.err.println("Cancel window initialization error for " + cleanOrderId + ": " + e.getMessage());
        pdf.setStatus("PENDING_SCAN");
    }

    PdfFile saved = repository.save(pdf);

    try {
        if (sseService != null) {
            sseService.broadcastOrderEvent(saved.getOrderId(), "PAID");
            sseService.broadcastQueueEvent("Order paid: " + saved.getOrderId());
        }
    } catch (Exception e) {
        System.err.println("SSE broadcast error for " + cleanOrderId + ": " + e.getMessage());
    }

    return saved;
}

public java.util.Map<String, Object> payWithWallet(String orderId) {

    PdfFile pdf =
            repository.findByOrderId(orderId);

    if (pdf == null) {
        throw new RuntimeException("Order Not Found");
    }

    if (!"UNPAID".equals(pdf.getPaymentStatus())) {
        throw new RuntimeException("Order already paid");
    }

    Double price =
            pdf.getPrice() == null ? 0.0 : pdf.getPrice();

    userService.debitWallet(pdf.getUserId(), price);

    pdf.setRazorpayPaymentId("WALLET");

    processReferralRewards(pdf);

    queueService.beginCancelWindow(pdf);

    PdfFile savedOrder = repository.save(pdf);
    if (sseService != null) {
        sseService.broadcastOrderEvent(savedOrder.getOrderId(), "PAID");
        sseService.broadcastQueueEvent("Order paid with wallet: " + savedOrder.getOrderId());
    }

    double newBalance = 0.0;
    try {
        User user = userRepository.findById(savedOrder.getUserId()).orElse(null);
        if (user != null && user.getWalletBalance() != null) {
            newBalance = user.getWalletBalance();
        }
    } catch (Exception e) {}

    java.util.Map<String, Object> res = new java.util.HashMap<>();
    res.put("order", savedOrder);
    res.put("newWalletBalance", newBalance);
    return res;
}

public PdfFile getOrderByOrderId(String orderId) {
    if (orderId == null) return null;
    List<PdfFile> list = repository.findAll();
    for (int i = list.size() - 1; i >= 0; i--) {
        PdfFile p = list.get(i);
        if (orderId.equalsIgnoreCase(p.getOrderId())) {
            return p;
        }
    }
    return null;
}

public PdfFile updateFinalPrice(
        String orderId,
        Double price,
        Double originalPrice,
        Double discountAmount
) {

    PdfFile pdf = repository.findByOrderId(orderId);

    if (pdf == null) {
        throw new RuntimeException("Order Not Found");
    }

    if (!"UNPAID".equals(pdf.getPaymentStatus())) {
        throw new RuntimeException("Order already paid");
    }

    pdf.setPrice(price);

    if (originalPrice != null) {
        pdf.setOriginalPrice(originalPrice);
    } else if (pdf.getOriginalPrice() == null) {
        pdf.setOriginalPrice(price);
    }

    pdf.setDiscountAmount(
            discountAmount == null ? 0.0 : discountAmount
    );

    return repository.save(pdf);
}

@Transactional
public PdfFile updateScheduledInfo(
        String orderId,
        String scheduledTime
) {
    PdfFile pdf = repository.findByOrderId(orderId);
    if (pdf == null) {
        throw new RuntimeException("Order Not Found");
    }

    if (scheduledTime != null && !scheduledTime.trim().isEmpty()) {
        try {
            pdf.setScheduledTime(LocalDateTime.parse(scheduledTime));
        } catch (Exception e) {
            System.err.println("Failed to parse scheduled time: " + scheduledTime + " - " + e.getMessage());
        }
    } else {
        pdf.setScheduledTime(null);
    }

    return repository.save(pdf);
}

public Map<String, Object> getCancelWindowInfo(String orderId) {

    PdfFile pdf =
            repository.findByOrderId(orderId);

    Map<String, Object> info = new HashMap<>();

    if (pdf == null) {
        info.put("found", false);
        return info;
    }

    info.put("found", true);
    info.put("orderId", pdf.getOrderId());
    info.put("status", pdf.getStatus());
    info.put("otpCode", pdf.getOtpCode());
    info.put("fileName", pdf.getFileName());
    info.put("blockLocation", pdf.getBlockLocation());
    info.put("cancelWindowEndsAt", pdf.getCancelWindowEndsAt());
    info.put("cancelWindowSeconds", queueService.getCancelWindowSeconds());

    if (pdf.getCancelWindowEndsAt() != null) {
        long secondsLeft =
                java.time.Duration.between(
                        LocalDateTime.now(),
                        pdf.getCancelWindowEndsAt()
                ).getSeconds();

        info.put("secondsLeft", Math.max(0, secondsLeft));
    }

    return info;
}

private String resolveCustomerName(
        Long userId,
        String fallbackName
) {

    if (userId != null) {

        return userRepository.findById(userId)
                .map(user -> user.getName())
                .orElseGet(() -> cleanCustomerName(fallbackName));
    }

    return cleanCustomerName(fallbackName);
}

private String cleanCustomerName(String name) {

    if (name == null || name.trim().isEmpty()) {
        return "Customer";
    }

    return name.trim();
}

private String normalizeBlockLocation(String blockLocation) {

    if (blockLocation == null
            || blockLocation.trim().isEmpty()) {

        return "C Block";
    }

    return blockLocation.trim();
}

private void autoPrint(PdfFile pdf) {

    if (pdf.getPdfData() == null) {

        System.out.println(
                "PDF data expired for order "
                        + pdf.getOrderId()
        );

        return;
    }

    try (PDDocument document =
                 Loader.loadPDF(pdf.getPdfData())) {

        PDDocument documentToPrint =
                createPrintableDocument(document, pdf.getSelectedPages());

        try (documentToPrint) {

            PrinterJob printerJob =
                    PrinterJob.getPrinterJob();

            printerJob.setJobName(
                    pdf.getOrderId()
                            + " - "
                            + pdf.getFileName()
            );

            Integer copies =
                    pdf.getCopies();

            printerJob.setCopies(
                    copies == null || copies < 1
                            ? 1
                            : copies
            );

            printerJob.setPageable(
                    new PDFPageable(documentToPrint)
            );

            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            if (Boolean.TRUE.equals(pdf.getDoubleSided())) {
                attributes.add(Sides.DUPLEX);
            } else {
                attributes.add(Sides.ONE_SIDED);
            }

            printerJob.print(attributes);

            System.out.println(
                    "PRINT STARTED FOR ORDER "
                            + pdf.getOrderId()
                            + " (Duplex=" + pdf.getDoubleSided() + ")"
            );
        }

    } catch (IOException | PrinterException e) {

        e.printStackTrace();
    }
}

private PDDocument createPrintableDocument(
        PDDocument sourceDocument,
        String selectedPages
) throws IOException {

    if (selectedPages == null
            || selectedPages.trim().isEmpty()
            || "ALL".equalsIgnoreCase(selectedPages.trim())) {

        PDDocument copy =
                new PDDocument();

        for (int pageIndex = 0;
             pageIndex < sourceDocument.getNumberOfPages();
             pageIndex++) {

            copy.importPage(
                    sourceDocument.getPage(pageIndex)
            );
        }

        return copy;
    }

    PDDocument selectedDocument =
            new PDDocument();

    String[] parts =
            selectedPages.split(",");

    for (String part : parts) {

        String pagePart =
                part.trim();

        if (pagePart.isEmpty()) {
            continue;
        }

        if (pagePart.contains("-")) {

            String[] range =
                    pagePart.split("-");

            int startPage =
                    Integer.parseInt(range[0].trim());

            int endPage =
                    Integer.parseInt(range[1].trim());

            addPageRange(
                    sourceDocument,
                    selectedDocument,
                    startPage,
                    endPage
            );

        } else {

            int pageNumber =
                    Integer.parseInt(pagePart);

            addPageRange(
                    sourceDocument,
                    selectedDocument,
                    pageNumber,
                    pageNumber
            );
        }
    }

    if (selectedDocument.getNumberOfPages() == 0) {

        throw new IOException(
                "No printable pages selected for order "
                        + selectedPages
        );
    }

    return selectedDocument;
}

private void addPageRange(
        PDDocument sourceDocument,
        PDDocument selectedDocument,
        int startPage,
        int endPage
) throws IOException {

    int safeStart =
            Math.max(1, startPage);

    int safeEnd =
            Math.min(
                    sourceDocument.getNumberOfPages(),
                    endPage
            );

    if (safeStart > safeEnd) {
        return;
    }

    for (int pageNumber = safeStart;
         pageNumber <= safeEnd;
         pageNumber++) {

        selectedDocument.importPage(
                sourceDocument.getPage(pageNumber - 1)
        );
    }
}

    @Transactional
    public Map<String, Object> applyReferral(String orderId, String referralCode, Long currentUserId) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Verify Referral Program is enabled globally
        if (!systemSettingService.getSettingBool("referral_enabled", true)) {
            response.put("success", false);
            response.put("message", "Referral program is currently deactivated");
            return response;
        }

        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            response.put("success", false);
            response.put("message", "Order not found");
            return response;
        }

        if (pdf.getAppliedReferralCode() != null) {
            response.put("success", false);
            response.put("message", "Referral code already applied");
            return response;
        }

        User referrer = userRepository.findByReferralCode(referralCode.trim());
        if (referrer == null) {
            response.put("success", false);
            response.put("message", "Invalid referral code");
            return response;
        }

        if (referrer.getId().equals(currentUserId)) {
            response.put("success", false);
            response.put("message", "You cannot refer yourself");
            return response;
        }

        // 2. Verify user is on their first order
        long paidOrders = repository.countByUserIdAndPaymentStatus(currentUserId, "PAID");
        if (paidOrders > 0) {
            response.put("success", false);
            response.put("message", "Referral codes can only be applied to your first order.");
            return response;
        }

        pdf.setAppliedReferralCode(referralCode.trim());
        repository.save(pdf);

        response.put("success", true);
        response.put("message", "Referral code applied successfully! Rewards will be credited upon payment.");
        return response;
    }

    private void processReferralRewards(PdfFile pdf) {
        if (!systemSettingService.getSettingBool("referral_enabled", true)) {
            return;
        }
        String refCode = pdf.getAppliedReferralCode();
        if (refCode != null && !refCode.trim().isEmpty()) {
            try {
                User referrer = userRepository.findByReferralCode(refCode.trim());
                if (referrer != null && !referrer.getId().equals(pdf.getUserId())) {
                    double referrerAmt = systemSettingService.getSettingDouble("referral_referrer_amount", 10.0);
                    double refereeAmt = systemSettingService.getSettingDouble("referral_referee_amount", 5.0);

                    // Credit referrer
                    userService.creditWallet(referrer.getId(), referrerAmt);
                    // Credit referee (current order user)
                    userService.creditWallet(pdf.getUserId(), refereeAmt);
                    System.out.println("Applied referral code: " + refCode + ". Referrer " + referrer.getId() + " credited " + referrerAmt + ", referee " + pdf.getUserId() + " credited " + refereeAmt + ".");
                }
            } catch (Exception e) {
                System.err.println("Failed to apply referral code rewards: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void resetAllStats() {
        repository.deleteAll();
    }

    @Transactional
    public void resetStatsByScope(String scope, String targetName) {
        if ("COLLEGE".equalsIgnoreCase(scope) && targetName != null && !targetName.trim().isEmpty()) {
            repository.deleteByCollege(targetName.trim());
        } else if ("BLOCK".equalsIgnoreCase(scope) && targetName != null && !targetName.trim().isEmpty() && !"ALL".equalsIgnoreCase(targetName.trim())) {
            repository.deleteByBlockLocation(targetName.trim());
        } else {
            repository.deleteAll();
        }
    }

    @Transactional
    public void resetStatsByBlock(String blockLocation) {
        if (blockLocation == null || blockLocation.trim().isEmpty() || "ALL".equalsIgnoreCase(blockLocation.trim())) {
            repository.deleteAll();
        } else {
            repository.deleteByBlockLocation(blockLocation.trim());
        }
    }

    @Transactional
    public void deleteOrdersByOrderIds(List<String> orderIds) {
        if (orderIds != null && !orderIds.isEmpty()) {
            repository.deleteByOrderIdIn(orderIds);
        }
    }

    @Transactional
    public void deleteOrdersByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            repository.deleteByIdIn(ids);
        }
    }

    public PdfFile saveMultiplePdfs(
            MultipartFile[] files,
            Long userId,
            String customerName,
            String blockLocation
    ) throws IOException {
        java.util.List<byte[]> pdfBytesList = new java.util.ArrayList<>();
        String combinedName = "";
        
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            
            boolean isImage = false;
            if (filename != null) {
                String lower = filename.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
                    isImage = true;
                }
            }
            
            if (isImage) {
                fileBytes = convertImageToPdf(fileBytes, contentType);
            }
            
            pdfBytesList.add(fileBytes);
            
            String baseName = filename != null ? filename : "Document";
            if (isImage && !baseName.toLowerCase().endsWith(".pdf")) {
                baseName = baseName + ".pdf";
            }
            
            if (i == 0) {
                combinedName = baseName;
            } else if (i == 1) {
                combinedName = combinedName + " + " + baseName;
            }
        }
        
        if (files.length > 2) {
            combinedName = combinedName + " (and " + (files.length - 2) + " more)";
        }
        
        byte[] mergedPdfBytes = mergePdfs(pdfBytesList);
        
        PdfFile pdf = new PdfFile();
        pdf.setUserId(userId);
        pdf.setCustomerName(resolveCustomerName(userId, customerName));
        pdf.setBlockLocation(normalizeBlockLocation(blockLocation));
        pdf.setCopies(1);
        pdf.setSelectedPages("ALL");
        
        Long lastId = repository.getLastId();
        long nextId = (lastId != null ? lastId : 0L) + 1;
        String orderId = "ORD2026" + String.format("%04d", nextId);
        pdf.setOrderId(orderId);
        pdf.setUploadTime(LocalDateTime.now());
        pdf.setFileExpiryTime(LocalDateTime.now().plusDays(1)); // Auto-expiry after 1 day
        pdf.setFileName(combinedName);
        pdf.setFileType("application/pdf");
        pdf.setFileSize((long) mergedPdfBytes.length);

        // Save to Google Drive instead of Database byte array
        if (googleDriveService != null && googleDriveService.isConfigured()) {
            try {
                com.google.api.services.drive.model.File driveFile = googleDriveService.uploadFile(
                        pdf.getFileName(), 
                        "application/pdf", 
                        mergedPdfBytes
                );
                pdf.setGoogleDriveFileId(driveFile.getId());
                pdf.setGoogleDriveWebViewLink(driveFile.getWebViewLink());
                pdf.setPdfData(null); // Keep database lean (no byte array in DB)
                System.out.println("[GoogleDrive] Saved combined PDF to Google Drive (ID: " + driveFile.getId() + ")");
            } catch (Exception e) {
                System.err.println("[GoogleDrive] Upload failed, falling back to DB bytecode: " + e.getMessage());
                pdf.setPdfData(mergedPdfBytes);
            }
        } else {
            pdf.setPdfData(mergedPdfBytes);
        }
        
        try (PDDocument document = Loader.loadPDF(mergedPdfBytes)) {
            pdf.setTotalPages(document.getNumberOfPages());
        }
        
        pdf.setStatus("ORDER_CREATED");
        pdf.setPaymentStatus("UNPAID");
        
        PdfFile finalPdf = repository.save(pdf);
        if (sseService != null) {
            sseService.broadcastOrderEvent(finalPdf.getOrderId(), "ORDER_CREATED");
            sseService.broadcastQueueEvent("Multiple files order created: " + finalPdf.getOrderId());
        }
        return finalPdf;
    }

    private byte[] mergePdfs(java.util.List<byte[]> pdfBytesList) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            merger.setDestinationStream(baos);
            for (byte[] bytes : pdfBytesList) {
                merger.addSource(new org.apache.pdfbox.io.RandomAccessReadBuffer(bytes));
            }
            merger.mergeDocuments(null);
            return baos.toByteArray();
        }
    }

    public List<PdfFile> getPendingScanOrders(Long userId, String blockLocation) {
        return repository.findByUserIdAndBlockLocationAndStatus(userId, blockLocation, "PENDING_SCAN");
    }

    @Transactional
    public PdfFile releasePrintJob(String orderId, String otp) {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            throw new RuntimeException("Order Not Found");
        }
        if (!"PENDING_SCAN".equals(pdf.getStatus()) && !"CANCEL_WINDOW".equals(pdf.getStatus()) && !"PAID".equals(pdf.getStatus()) && !"ORDER_CREATED".equals(pdf.getStatus()) && !"PROCESSING".equals(pdf.getStatus())) {
            throw new RuntimeException("Order is not in valid release state: " + pdf.getStatus());
        }
        if (pdf.getOtpCode() == null || !pdf.getOtpCode().equals(otp)) {
            throw new RuntimeException("Invalid OTP Code");
        }
        pdf.setStatus("QUEUE");
        pdf.setQueuedAt(LocalDateTime.now());
        PdfFile saved = repository.save(pdf);
        if (sseService != null) {
            sseService.broadcastOrderEvent(saved.getOrderId(), "QUEUE");
            sseService.broadcastQueueEvent("Order released to queue: " + saved.getOrderId());
        }
        return saved;
    }

    public com.saipraveen.login_registration.repository.PdfFileProjection getOrderDetails(String orderId) {
        return repository.findProjectionByOrderId(orderId);
    }

    public Map<String, Object> getReferralStats(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        Map<String, Object> stats = new HashMap<>();
        if (user != null) {
            long totalReferrals = repository.countByAppliedReferralCodeAndPaymentStatus(user.getReferralCode(), "PAID");
            double referrerAmt = systemSettingService.getSettingDouble("referral_referrer_amount", 10.0);
            stats.put("referralCode", user.getReferralCode());
            stats.put("totalReferrals", totalReferrals);
            stats.put("cashbackEarned", totalReferrals * referrerAmt);
            stats.put("walletBalance", user.getWalletBalance());
        }
        return stats;
    }

    public List<Map<String, Object>> getReferralLeaderboardList() {
        List<Object[]> queryResult = repository.getReferralLeaderboard();
        List<Map<String, Object>> leaderboard = new java.util.ArrayList<>();
        for (Object[] row : queryResult) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", row[0]);
            entry.put("count", row[1]);
            leaderboard.add(entry);
        }
        return leaderboard;
    }

    public List<Map<String, Object>> getPrinterLiveStatusList() {
        List<com.saipraveen.login_registration.entity.PrinterConfig> printers = printerConfigService.getAllPrinters();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.saipraveen.login_registration.entity.PrinterConfig printer : printers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", printer.getId());
            map.put("blockLocation", printer.getBlockLocation());
            map.put("printerName", printer.getPrinterName());
            map.put("printerIp", printer.getPrinterIp());
            map.put("active", printer.getActive());
            map.put("maintenance", printer.getMaintenance());
            map.put("paperCount", printer.getPaperCount());
            map.put("online", queueService.isAgentOnline(printer.getBlockLocation()));
            
            long activeJobs = repository.countByBlockLocationAndStatusIn(
                printer.getBlockLocation(), 
                java.util.Arrays.asList("QUEUE", "PRINTING")
            );
            map.put("queueLoad", activeJobs);
            
            result.add(map);
        }
        return result;
    }
}
