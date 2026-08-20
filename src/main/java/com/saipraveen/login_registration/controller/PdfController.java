package com.saipraveen.login_registration.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.service.PdfFileService;
import com.saipraveen.login_registration.service.QueueService;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "http://localhost:5173")
public class PdfController {

@Autowired
private PdfFileService service;

@Autowired
private QueueService queueService;

@PostMapping("/updateOrder")
public ResponseEntity<?> updateOrder(
        @RequestParam String orderId,
        @RequestParam Integer copies,
        @RequestParam String selectedPages,
        @RequestParam String printType,
        @RequestParam(required = false) String blockLocation,
        @RequestParam(required = false, defaultValue = "1-up") String nupLayout,
        @RequestParam(required = false, defaultValue = "false") Boolean doubleSided
) {
    return ResponseEntity.ok(
            service.updateOrder(
                    orderId,
                    copies,
                    selectedPages,
                    printType,
                    blockLocation,
                    nupLayout,
                    doubleSided
            )
    );
}

@PostMapping("/updatePayment")
public ResponseEntity<?> updatePayment(

        @RequestParam Long id,

        @RequestParam String paymentStatus

) {

    return ResponseEntity.ok(

            service.updatePaymentStatus(
                    id,
                    paymentStatus
            )
    );
}


@PostMapping("/upload")
public ResponseEntity<?> uploadPdf(
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "files", required = false) MultipartFile[] files,
        @RequestParam("userId") Long userId,
        @RequestParam(value = "customerName", required = false) String customerName,
        @RequestParam(value = "blockLocation", required = false) String blockLocation
) throws IOException {
    if (files != null && files.length > 0) {
        return ResponseEntity.ok(
                service.saveMultiplePdfs(
                        files,
                        userId,
                        customerName,
                        blockLocation
                )
        );
    } else if (file != null) {
        return ResponseEntity.ok(
                service.savePdf(
                        file,
                        userId,
                        customerName,
                        blockLocation
                )
        );
    } else {
        return ResponseEntity.badRequest().body("No file or files uploaded");
    }
}

@GetMapping("/order/{orderId}")
public ResponseEntity<?> getOrderByOrderId(@PathVariable String orderId) {
    PdfFile pdf = service.getOrderByOrderId(orderId);
    if (pdf == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(pdf);
}

@GetMapping("/orders")
public ResponseEntity<?> getAllOrders() {

    return ResponseEntity.ok(
            service.getAllOrders()
    );
}
@GetMapping("/userOrders")
public ResponseEntity<?> getUserOrders(
        @RequestParam Long userId
) {
    try {
        return ResponseEntity.ok(
                service.getUserOrders(
                        userId
                )
        );
    } catch (Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return ResponseEntity.status(500).body("Error: " + e.getMessage() + "\nStacktrace:\n" + sw.toString());
    }
}

@GetMapping("/stats")
public ResponseEntity<?> getStats(
        @RequestParam(defaultValue = "all") String period
) {

    return ResponseEntity.ok(
            service.getDashboardStats(period)
    );
}
@PostMapping("/updateStatus")
public ResponseEntity<?> updateStatus(

        @RequestParam Long id,

        @RequestParam String status

) {

    return ResponseEntity.ok(

            service.updateStatus(
                    id,
                    status
            )
    );
}

@GetMapping("/download/{id}")
public ResponseEntity<byte[]> downloadPdf(
        @PathVariable Long id) {

    PdfFile pdf =
            service.getPdfById(id);

    byte[] printableData = service.getPrintablePdfData(pdf);

    if (printableData == null) {

        return ResponseEntity.status(
                HttpStatus.GONE
        ).body(null);
    }

    return ResponseEntity.ok()
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\""
                            + pdf.getFileName()
                            + "\""
            )
            .contentType(
                    MediaType.APPLICATION_PDF
            )
            .body(
                    printableData
            );
}


@PostMapping("/paymentSuccess")
public ResponseEntity<?> paymentSuccess(
        @RequestParam String orderId,
        @RequestParam(required = false) String paymentId
) {
    try {
        com.saipraveen.login_registration.entity.PdfFile paid = service.markAsPaid(
                orderId,
                paymentId != null ? paymentId : "DIRECT_PAY"
        );
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", true);
        resp.put("orderId", paid.getOrderId());
        resp.put("paymentStatus", paid.getPaymentStatus());
        resp.put("otpCode", paid.getOtpCode());
        resp.put("status", paid.getStatus());
        resp.put("price", paid.getPrice());
        return ResponseEntity.ok(resp);
    } catch (Exception e) {
        System.err.println("Failed to mark order as paid: " + orderId + " error: " + e.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Collections.singletonMap("error", e.getMessage()));
    }
}

@PostMapping("/payWithWallet")
public ResponseEntity<?> payWithWallet(

        @RequestParam String orderId

) {

    return ResponseEntity.ok(

            service.payWithWallet(orderId)
    );
}

@PostMapping("/updatePrice")
public ResponseEntity<?> updatePrice(

        @RequestParam String orderId,

        @RequestParam Double price,

        @RequestParam(required = false) Double originalPrice,

        @RequestParam(required = false) Double discountAmount

) {

    return ResponseEntity.ok(

            service.updateFinalPrice(
                    orderId,
                    price,
                    originalPrice,
                    discountAmount
            )
    );
}

@PostMapping("/updateScheduledInfo")
public ResponseEntity<?> updateScheduledInfo(
        @RequestParam String orderId,
        @RequestParam(required = false) String scheduledTime
) {
    return ResponseEntity.ok(
            service.updateScheduledInfo(orderId, scheduledTime)
    );
}

@PostMapping("/cancelOrder")
public ResponseEntity<?> cancelOrder(

        @RequestParam String orderId,

        @RequestParam Long userId

) {

    return ResponseEntity.ok(

            queueService.cancelOrder(
                    orderId,
                    userId
            )
    );
}

@GetMapping("/cancelWindow")
public ResponseEntity<?> cancelWindow(

        @RequestParam String orderId

) {

    return ResponseEntity.ok(

            service.getCancelWindowInfo(orderId)
    );
}

    @PostMapping("/applyReferral")
    public ResponseEntity<?> applyReferral(
            @RequestParam String orderId,
            @RequestParam String referralCode,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                service.applyReferral(orderId, referralCode, userId)
        );
    }

    @GetMapping("/pendingScan")
    public ResponseEntity<?> getPendingScan(
            @RequestParam Long userId,
            @RequestParam String blockLocation
    ) {
        return ResponseEntity.ok(
                service.getPendingScanOrders(userId, blockLocation)
        );
    }

    @PostMapping("/releasePrint")
    public ResponseEntity<?> releasePrint(
            @RequestParam String orderId,
            @RequestParam String otp
    ) {
        return ResponseEntity.ok(
                service.releasePrintJob(orderId, otp)
        );
    }

    @GetMapping("/details")
    public ResponseEntity<?> getOrderDetails(
            @RequestParam String orderId
    ) {
        com.saipraveen.login_registration.repository.PdfFileProjection pdf = service.getOrderDetails(orderId);
        if (pdf == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pdf);
    }

    @GetMapping("/referrals/stats")
    public ResponseEntity<?> getReferralStats(@RequestParam Long userId) {
        return ResponseEntity.ok(service.getReferralStats(userId));
    }

    @GetMapping("/referrals/leaderboard")
    public ResponseEntity<?> getReferralLeaderboard() {
        return ResponseEntity.ok(service.getReferralLeaderboardList());
    }
}
