package com.saipraveen.login_registration.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.repository.PdfFileRepository;

@Service
public class PdfCleanupService {

    @Autowired
    private PdfFileRepository repository;

    @Autowired
    private GoogleDriveService googleDriveService;

    @Value("${print.pdf.retention-minutes:30}")
    private int retentionMinutes;

    @Value("${google.drive.cleanup-threshold-percent:75.0}")
    private double cleanupThresholdPercent;

    @Value("${google.drive.target-safe-percent:60.0}")
    private double targetSafePercent;

    @Value("${print.pdf.daily-cleanup-enabled:true}")
    private boolean dailyCleanupEnabled;

    /**
     * Checks Google Drive storage quota and purges oldest files ONLY if usage reaches 70-80% threshold.
     * Keeps documents on Google Drive as long as storage is healthy under the threshold.
     */
    @Scheduled(fixedRate = 10 * 60 * 1000, initialDelay = 45 * 1000) // Checks every 10 minutes
    @Transactional
    public void checkAndPurgeGoogleDriveStorageOnThreshold() {
        if (googleDriveService == null || !googleDriveService.isConfigured()) {
            return;
        }

        GoogleDriveService.StorageUsageInfo usage = googleDriveService.getStorageUsage();
        if (usage == null) {
            return;
        }

        double currentPercent = usage.getUsagePercentage();
        System.out.println(String.format("[GoogleDrive Storage] Current: %.2f MB / %.2f MB (%.2f%% used). Purge Threshold: %.1f%%",
                usage.getUsageMb(), usage.getLimitMb(), currentPercent, cleanupThresholdPercent));

        if (currentPercent < cleanupThresholdPercent) {
            // Storage is safe under 70-80% threshold, keep all files on Google Drive!
            return;
        }

        System.out.println(String.format("[GoogleDrive Storage Alert] Storage exceeded threshold (%.2f%% >= %.1f%%)! Initiating FIFO cleanup of oldest files...",
                currentPercent, cleanupThresholdPercent));

        int deletedCount = 0;

        // Step 1: Purge oldest finished / cancelled orders (FIFO - oldest first)
        deletedCount += purgeFileList(repository.findOldestFinishedGoogleDriveFiles());

        // Check if usage dropped below target safe percentage
        if (isStorageSafeAfterPurge()) {
            googleDriveService.emptyTrash();
            System.out.println("[GoogleDrive Storage] Storage recovered below target safe level (" + targetSafePercent + "%). Cleaned " + deletedCount + " file(s).");
            return;
        }

        // Step 2: Purge oldest stale unpaid orders (FIFO)
        deletedCount += purgeFileList(repository.findOldestUnpaidGoogleDriveFiles());

        if (isStorageSafeAfterPurge()) {
            googleDriveService.emptyTrash();
            System.out.println("[GoogleDrive Storage] Storage recovered below target safe level (" + targetSafePercent + "%). Cleaned " + deletedCount + " file(s).");
            return;
        }

        // Step 3: Purge other oldest purgeable non-active orders
        deletedCount += purgeFileList(repository.findOldestPurgeableGoogleDriveFiles());

        googleDriveService.emptyTrash();
        System.out.println("[GoogleDrive Storage] Threshold cleanup finished. Total purged: " + deletedCount + " file(s).");
    }

    private int purgeFileList(List<PdfFile> files) {
        if (files == null || files.isEmpty()) return 0;
        int count = 0;
        for (PdfFile file : files) {
            if (file.getGoogleDriveFileId() != null) {
                googleDriveService.deleteFile(file.getGoogleDriveFileId());
                file.setGoogleDriveFileId(null);
                file.setGoogleDriveWebViewLink(null);
                repository.save(file);
                count++;

                // Check quota every 10 file deletions to stop as soon as safe limit is reached
                if (count % 10 == 0 && isStorageSafeAfterPurge()) {
                    break;
                }
            }
        }
        return count;
    }

    private boolean isStorageSafeAfterPurge() {
        GoogleDriveService.StorageUsageInfo usage = googleDriveService.getStorageUsage();
        if (usage != null) {
            return usage.getUsagePercentage() <= targetSafePercent;
        }
        return false;
    }

    /**
     * Purges database bytea data and associated Google Drive files from finished orders
     * that have passed their retention period.
     */
    @Scheduled(fixedRate = 60 * 1000, initialDelay = 30 * 1000)
    @Transactional
    public void removePdfDataAfterRetention() {

        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusMinutes(retentionMinutes);

        // 1. Clean DB bytea bytecode if any
        int cleanedDbCount =
                repository.clearPdfDataFinishedBefore(
                        cutoff
                );

        // 2. Clean Google Drive files from finished/cancelled orders past retention
        List<PdfFile> finishedDriveFiles = repository.findFinishedGoogleDriveFilesBefore(cutoff);
        if (finishedDriveFiles != null && !finishedDriveFiles.isEmpty()) {
            for (PdfFile file : finishedDriveFiles) {
                if (file.getGoogleDriveFileId() != null) {
                    if (googleDriveService != null && googleDriveService.isConfigured()) {
                        googleDriveService.deleteFile(file.getGoogleDriveFileId());
                    }
                    file.setGoogleDriveFileId(null);
                    file.setGoogleDriveWebViewLink(null);
                    repository.save(file);
                }
            }
        }

        if (cleanedDbCount > 0 || (finishedDriveFiles != null && !finishedDriveFiles.isEmpty())) {
            System.out.println(
                    "Removed PDF storage for finished order(s) older than "
                            + retentionMinutes
                            + " minutes"
            );
        }
    }

    @Scheduled(cron = "${print.pdf.daily-cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void dailyPdfDataReset() {

        if (!dailyCleanupEnabled) {
            return;
        }

        int finishedCount =
                repository.clearPdfDataForFinishedOrders();

        int unpaidCount =
                repository.clearPdfDataForUnpaidOlderThan(
                        LocalDateTime.now().minusDays(1)
                );

        // Check Google Drive storage quota on daily reset
        checkAndPurgeGoogleDriveStorageOnThreshold();

        int total = finishedCount + unpaidCount;

        if (total > 0) {
            System.out.println(
                    "Daily PDF cleanup: removed data from "
                            + finishedCount
                            + " finished and "
                            + unpaidCount
                            + " stale unpaid order(s)"
            );
        }
    }

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 30 * 1000)
    @Transactional
    public void deleteStaleUnpaidOrders() {
        // Delete unpaid/draft orders older than 10 minutes
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        
        List<PdfFile> staleOrders = repository.findByGoogleDriveFileIdIsNotNullAndUploadTimeBefore(cutoff);
        if (staleOrders != null) {
            for (PdfFile p : staleOrders) {
                if ("UNPAID".equals(p.getPaymentStatus()) || "DRAFT".equals(p.getStatus())) {
                    if (p.getGoogleDriveFileId() != null && googleDriveService != null && googleDriveService.isConfigured()) {
                        googleDriveService.deleteFile(p.getGoogleDriveFileId());
                    }
                }
            }
        }

        int deletedCount = repository.deleteUnpaidOrdersOlderThan(cutoff);
        if (deletedCount > 0) {
            System.out.println("Cleaned up " + deletedCount + " stale unpaid/draft order(s) older than 10 minutes.");
        }
    }
}
