package com.saipraveen.login_registration.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.saipraveen.login_registration.entity.PdfFile;

public interface PdfFileRepository
        extends JpaRepository<PdfFile, Long> {

    PdfFile findByOrderId(String orderId);

    PdfFileProjection findProjectionByOrderId(String orderId);

    @Query(
        value = "SELECT COALESCE(MAX(id),0) FROM pdf_files",
        nativeQuery = true
    )
    Long getLastId();

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM PdfFile p WHERE p.blockLocation = :block")
    void deleteByBlockLocation(@Param("block") String block);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM PdfFile p WHERE p.blockLocation IN (SELECT b.name FROM CampusBlock b WHERE UPPER(b.college) = UPPER(:college))")
    void deleteByCollege(@Param("college") String college);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByOrderIdIn(List<String> orderIds);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByIdIn(List<Long> ids);


@Query(
 "SELECT SUM(price) FROM PdfFile"
)
Double getTotalRevenue();




@Query(
    "SELECT COUNT(p) FROM PdfFile p"
)
Long getTotalOrders();

@Query(
    "SELECT COALESCE(SUM(p.totalPages),0) FROM PdfFile p"
)
Long getTotalPagesPrinted();

@Query(
    "SELECT COUNT(p) FROM PdfFile p WHERE p.status='ORDER_CREATED'"
)
Long getPendingOrders();
@Query(
"SELECT COALESCE(SUM(p.price),0) FROM PdfFile p WHERE CAST(p.uploadTime AS date)=CURRENT_DATE"
)
Double getTodayRevenue();


@Query(
"SELECT COUNT(p) FROM PdfFile p WHERE p.status='PRINTING'"
)
Long getPrintingOrders();

@Query(
"SELECT COUNT(p) FROM PdfFile p WHERE p.status='COMPLETED'"
)
Long getCompletedOrders();


    List<PdfFile> findByUserId(
            Long userId
    );

    List<PdfFileProjection> findProjectedByUserId(Long userId);

    @Query("SELECT p FROM PdfFile p WHERE p.status <> 'DRAFT' ORDER BY p.id ASC")
    List<PdfFileProjection> findAllProjectedByOrderByIdAsc();

    @Modifying
    @Query("DELETE FROM PdfFile p WHERE p.paymentStatus = 'UNPAID' AND p.uploadTime <= :cutoff")
    int deleteUnpaidOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    List<PdfFile> findByUserIdAndBlockLocationAndStatus(
            Long userId,
            String blockLocation,
            String status
    );

@Query(
    "SELECT COUNT(p) FROM PdfFile p WHERE p.status='QUEUE'"
)
Long getQueuedOrders();

@Query(
    "SELECT p FROM PdfFile p WHERE (p.paymentStatus IN ('PAID', 'SUCCESS', 'COMPLETED') OR p.razorpayPaymentId IS NOT NULL) " +
    "AND p.status='QUEUE' " +
    "AND (LOWER(TRIM(p.blockLocation)) = LOWER(TRIM(:block)) " +
    "OR REPLACE(REPLACE(LOWER(TRIM(p.blockLocation)), ' ', ''), '-', '') = REPLACE(REPLACE(LOWER(TRIM(:block)), ' ', ''), '-', '') " +
    "OR LOWER(TRIM(p.blockLocation)) LIKE CONCAT('%', LOWER(TRIM(:block)), '%') " +
    "OR :block = 'ALL') " +
    "ORDER BY p.id ASC"
)
List<PdfFile> findQueueByBlock(
        @Param("block") String blockLocation
);

@Query(
    "SELECT p FROM PdfFile p WHERE p.status='CANCEL_WINDOW' AND p.cancelWindowEndsAt <= :now"
)
List<PdfFile> findExpiredCancelWindows(
        @Param("now") LocalDateTime now
);

@Query(
    "SELECT p FROM PdfFile p WHERE (p.paymentStatus IN ('PAID', 'SUCCESS', 'COMPLETED') OR p.razorpayPaymentId IS NOT NULL) " +
    "AND p.status IN ('CANCEL_WINDOW','QUEUE','PRINTING') " +
    "AND (LOWER(TRIM(p.blockLocation)) = LOWER(TRIM(:block)) " +
    "OR REPLACE(REPLACE(LOWER(TRIM(p.blockLocation)), ' ', ''), '-', '') = REPLACE(REPLACE(LOWER(TRIM(:block)), ' ', ''), '-', '') " +
    "OR LOWER(TRIM(p.blockLocation)) LIKE CONCAT('%', LOWER(TRIM(:block)), '%') " +
    "OR :block = 'ALL') " +
    "ORDER BY p.id ASC"
)
List<PdfFile> findActiveQueueByBlock(
        @Param("block") String blockLocation
);

@Query(
    "SELECT p FROM PdfFile p WHERE p.status = 'PENDING_SCAN' AND p.cancelWindowEndsAt <= :cutoff"
)
List<PdfFile> findExpiredPendingScanOrders(
        @Param("cutoff") LocalDateTime cutoff
);

@Query(
    "SELECT p FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status='QUEUE' ORDER BY p.id ASC"
)
List<PdfFile> findAllQueuedOrders();

@Query(
    "SELECT p FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status IN ('QUEUE','PRINTING') AND p.queuedAt IS NOT NULL AND p.queuedAt <= :cutoff"
)
List<PdfFile> findTimedOutOrders(
        @Param("cutoff") LocalDateTime cutoff
);

@Query(
    "SELECT COALESCE(SUM(COALESCE(p.originalPrice, p.price)),0) FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status NOT IN ('CANCELLED') AND p.paidAt >= :start"
)
Double getGrossRevenueSince(
        @Param("start") LocalDateTime start
);

@Query(
    "SELECT COALESCE(SUM(COALESCE(p.originalPrice, p.price)),0) FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status NOT IN ('CANCELLED')"
)
Double getGrossRevenueAll();

@Query(
    "SELECT COALESCE(SUM(COALESCE(p.discountAmount, 0)),0) FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status NOT IN ('CANCELLED') AND p.paidAt >= :start"
)
Double getTotalDiscountsSince(
        @Param("start") LocalDateTime start
);

@Query(
    "SELECT COALESCE(SUM(COALESCE(p.discountAmount, 0)),0) FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status NOT IN ('CANCELLED')"
)
Double getTotalDiscountsAll();

@Query(
    "SELECT COALESCE(SUM(p.price),0) FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status NOT IN ('CANCELLED') AND p.paidAt >= :start"
)
Double getNetRevenueSince(
        @Param("start") LocalDateTime start
);

@Query(
    "SELECT COALESCE(SUM(p.price),0) FROM PdfFile p WHERE p.paymentStatus='PAID' AND p.status NOT IN ('CANCELLED')"
)
Double getNetRevenueAll();

@Modifying
@Query(
    "UPDATE PdfFile p SET p.pdfData = null WHERE p.pdfData IS NOT NULL AND p.status IN ('COMPLETED','CANCELLED') AND p.finishedAt IS NOT NULL AND p.finishedAt <= :cutoff"
)
int clearPdfDataFinishedBefore(
        @Param("cutoff") LocalDateTime cutoff
);

@Modifying
@Query(
    "UPDATE PdfFile p SET p.pdfData = null WHERE p.pdfData IS NOT NULL AND p.status IN ('COMPLETED','CANCELLED')"
)
int clearPdfDataForFinishedOrders();

    @Modifying
    @Query("UPDATE PdfFile p SET p.pdfData = null WHERE p.pdfData IS NOT NULL AND p.paymentStatus='UNPAID' AND p.uploadTime < :cutoff")
    int clearPdfDataForUnpaidOlderThan(
            @Param("cutoff") LocalDateTime cutoff
    );

    long countByUserIdAndPaymentStatus(Long userId, String paymentStatus);

    @Modifying
    @Query("UPDATE PdfFile p SET p.status = :status, p.queuedAt = :queuedAt WHERE p.orderId = :orderId")
    int updateStatusAndQueuedAtByOrderId(
            @Param("orderId") String orderId,
            @Param("status") String status,
            @Param("queuedAt") LocalDateTime queuedAt
    );

    @Modifying
    @Query("UPDATE PdfFile p SET p.status = :status WHERE p.orderId = :orderId")
    int updateStatusByOrderId(
            @Param("orderId") String orderId,
            @Param("status") String status
    );


    @Query("SELECT p FROM PdfFile p WHERE p.status = 'SCHEDULED' AND p.scheduledTime <= :cutoff")
    List<PdfFile> findPendingScheduledOrders(
            @Param("cutoff") LocalDateTime cutoff
    );

    @Modifying
    @Query("UPDATE PdfFile p SET p.status = :status, p.cancelWindowEndsAt = :cancelWindowEndsAt WHERE p.orderId = :orderId")
    int updateStatusAndCancelWindowEndsAtByOrderId(
            @Param("orderId") String orderId,
            @Param("status") String status,
            @Param("cancelWindowEndsAt") LocalDateTime cancelWindowEndsAt
    );

    @Query("SELECT u.name, COUNT(DISTINCT p.userId) FROM PdfFile p, User u " +
           "WHERE p.appliedReferralCode = u.referralCode AND p.appliedReferralCode IS NOT NULL AND p.paymentStatus = 'PAID' " +
           "GROUP BY u.name " +
           "ORDER BY COUNT(DISTINCT p.userId) DESC")
    List<Object[]> getReferralLeaderboard();

    long countByAppliedReferralCodeAndPaymentStatus(String referralCode, String paymentStatus);

    long countByBlockLocationAndStatusIn(String blockLocation, List<String> statuses);

    @Query("SELECT COUNT(DISTINCT p.userId) FROM PdfFile p WHERE p.status = 'COMPLETED'")
    Long countDistinctUsersWithCompletedOrders();

    @Query("SELECT COUNT(p) FROM PdfFile p WHERE p.paymentStatus = 'PAID'")
    Long countTotalPaidOrders();

    List<PdfFile> findByGoogleDriveFileIdIsNotNullAndUploadTimeBefore(LocalDateTime cutoff);

    @Query("SELECT p FROM PdfFile p WHERE p.googleDriveFileId IS NOT NULL AND ((p.fileExpiryTime IS NOT NULL AND p.fileExpiryTime <= :now) OR (p.uploadTime <= :cutoff))")
    List<PdfFile> findExpiredGoogleDriveFiles(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT p FROM PdfFile p WHERE p.googleDriveFileId IS NOT NULL AND p.status IN ('COMPLETED','CANCELLED') AND p.finishedAt IS NOT NULL AND p.finishedAt <= :cutoff")
    List<PdfFile> findFinishedGoogleDriveFilesBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT p FROM PdfFile p WHERE p.googleDriveFileId IS NOT NULL AND p.status IN ('COMPLETED','CANCELLED') ORDER BY p.uploadTime ASC")
    List<PdfFile> findOldestFinishedGoogleDriveFiles();

    @Query("SELECT p FROM PdfFile p WHERE p.googleDriveFileId IS NOT NULL AND p.paymentStatus = 'UNPAID' ORDER BY p.uploadTime ASC")
    List<PdfFile> findOldestUnpaidGoogleDriveFiles();

    @Query("SELECT p FROM PdfFile p WHERE p.googleDriveFileId IS NOT NULL AND p.status NOT IN ('QUEUE','PRINTING') ORDER BY p.uploadTime ASC")
    List<PdfFile> findOldestPurgeableGoogleDriveFiles();
}
