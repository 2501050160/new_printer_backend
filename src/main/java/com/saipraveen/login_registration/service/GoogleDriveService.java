package com.saipraveen.login_registration.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import jakarta.annotation.PostConstruct;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "University Cloud Print";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    @Value("${google.drive.enabled:true}")
    private boolean enabled;

    @Value("${google.drive.credentials-path:credentials/google-service-account.json}")
    private String credentialsPath;

    @Value("${google.drive.folder-id:}")
    private String configuredFolderId;

    @Value("${google.drive.folder-name:University_Cloud_Print_Uploads}")
    private String defaultFolderName;

    @Value("${google.drive.storage-capacity-mb:15360}")
    private long storageCapacityMb;

    private Drive driveService;
    private String targetFolderId;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            System.out.println("[GoogleDriveService] Google Drive storage is disabled via configuration.");
            return;
        }

        try {
            InputStream credentialsStream = resolveCredentialsStream();
            if (credentialsStream == null) {
                System.out.println("[GoogleDriveService] NOTICE: No Google Service Account credentials found at '" 
                        + credentialsPath + "' or environment variables (GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON / GOOGLE_DRIVE_SERVICE_ACCOUNT_BASE64).");
                System.out.println("[GoogleDriveService] System will temporarily store files locally/in DB until credentials are provided.");
                return;
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(SCOPES);

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            this.driveService = new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Initialize or find upload folder
            this.targetFolderId = resolveTargetFolder();
            this.initialized = true;
            System.out.println("[GoogleDriveService] Successfully authenticated and connected to Google Drive! Target Folder ID: " + targetFolderId);

        } catch (Exception e) {
            System.err.println("[GoogleDriveService] Failed to initialize Google Drive client: " + e.getMessage());
            this.initialized = false;
        }
    }

    private InputStream resolveCredentialsStream() {
        // 1. Check raw JSON from Environment Variable
        String rawJson = System.getenv("GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON");
        if (rawJson != null && !rawJson.trim().isEmpty()) {
            return new ByteArrayInputStream(rawJson.trim().getBytes(StandardCharsets.UTF_8));
        }

        // 2. Check Base64 encoded JSON from Environment Variable
        String base64Json = System.getenv("GOOGLE_DRIVE_SERVICE_ACCOUNT_BASE64");
        if (base64Json != null && !base64Json.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(base64Json.trim());
                return new ByteArrayInputStream(decoded);
            } catch (Exception e) {
                System.err.println("[GoogleDriveService] Failed to decode GOOGLE_DRIVE_SERVICE_ACCOUNT_BASE64: " + e.getMessage());
            }
        }

        // 3. Check File Path
        if (credentialsPath != null && !credentialsPath.trim().isEmpty()) {
            File file = new File(credentialsPath);
            if (file.exists() && file.canRead()) {
                try {
                    return new FileInputStream(file);
                } catch (Exception e) {
                    System.err.println("[GoogleDriveService] Could not read file from path: " + credentialsPath);
                }
            }

            // Check as classpath resource
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(credentialsPath);
            if (resourceStream != null) {
                return resourceStream;
            }
        }

        return null;
    }

    private String resolveTargetFolder() {
        if (configuredFolderId != null && !configuredFolderId.trim().isEmpty()) {
            return configuredFolderId.trim();
        }

        try {
            // Search for existing folder with defaultFolderName
            String query = "mimeType = 'application/vnd.google-apps.folder' and name = '" 
                    + defaultFolderName + "' and trashed = false";
            FileList result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute();

            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                return result.getFiles().get(0).getId();
            }

            // Create new folder if not found
            com.google.api.services.drive.model.File folderMetadata = new com.google.api.services.drive.model.File();
            folderMetadata.setName(defaultFolderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");

            com.google.api.services.drive.model.File folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            System.out.println("[GoogleDriveService] Created new Google Drive folder: " + defaultFolderName + " (ID: " + folder.getId() + ")");
            return folder.getId();

        } catch (Exception e) {
            System.err.println("[GoogleDriveService] Warning: Could not resolve or create target folder: " + e.getMessage());
            return null; // Will upload to root drive if folder creation fails
        }
    }

    public boolean isConfigured() {
        return enabled && initialized && driveService != null;
    }

    /**
     * Uploads a document to Google Drive.
     * 
     * @param fileName Original or target file name
     * @param mimeType MIME type (e.g. application/pdf)
     * @param fileBytes Raw file bytes
     * @return com.google.api.services.drive.model.File containing id and webViewLink
     * @throws IOException on upload failure
     */
    public com.google.api.services.drive.model.File uploadFile(String fileName, String mimeType, byte[] fileBytes) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Drive Service is not initialized or configured.");
        }

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(fileName);
        
        if (targetFolderId != null) {
            fileMetadata.setParents(Collections.singletonList(targetFolderId));
        }

        ByteArrayContent mediaContent = new ByteArrayContent(
                mimeType != null ? mimeType : "application/pdf", 
                fileBytes
        );

        return driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink, webContentLink, size, createdTime")
                .execute();
    }

    /**
     * Downloads file bytes from Google Drive by file ID.
     * 
     * @param fileId Google Drive file ID
     * @return byte[] file content
     * @throws IOException on download failure
     */
    public byte[] downloadFile(String fileId) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Drive Service is not initialized or configured.");
        }
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("Google Drive fileId cannot be null or empty.");
        }

        try (InputStream is = driveService.files().get(fileId).executeMediaAsInputStream()) {
            return is.readAllBytes();
        }
    }

    /**
     * Permanently deletes a file from Google Drive.
     * 
     * @param fileId Google Drive file ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteFile(String fileId) {
        if (!isConfigured() || fileId == null || fileId.trim().isEmpty()) {
            return false;
        }

        try {
            driveService.files().delete(fileId).execute();
            System.out.println("[GoogleDriveService] Successfully deleted file from Google Drive: " + fileId);
            return true;
        } catch (Exception e) {
            System.err.println("[GoogleDriveService] Failed to delete file " + fileId + " from Google Drive: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves current Google Drive storage usage and percentage.
     */
    public StorageUsageInfo getStorageUsage() {
        if (!isConfigured()) {
            return null;
        }

        try {
            com.google.api.services.drive.model.About about = driveService.about()
                    .get()
                    .setFields("storageQuota, user")
                    .execute();

            if (about != null && about.getStorageQuota() != null) {
                com.google.api.services.drive.model.About.StorageQuota quota = about.getStorageQuota();
                long usage = quota.getUsage() != null ? quota.getUsage() : 0L;
                long limit = quota.getLimit() != null ? quota.getLimit() : 0L;

                // Fallback to configured capacity limit (e.g. 15GB) if service account returns 0/unlimited
                if (limit <= 0) {
                    limit = storageCapacityMb * 1024L * 1024L;
                }

                double percentage = limit > 0 ? ((double) usage / (double) limit) * 100.0 : 0.0;
                long usageInTrash = quota.getUsageInDriveTrash() != null ? quota.getUsageInDriveTrash() : 0L;

                return new StorageUsageInfo(usage, limit, percentage, usageInTrash);
            }
        } catch (Exception e) {
            System.err.println("[GoogleDriveService] Failed to retrieve storage quota: " + e.getMessage());
        }

        return null;
    }

    /**
     * Empties Google Drive trash to permanently free up quota.
     */
    public void emptyTrash() {
        if (!isConfigured()) {
            return;
        }
        try {
            driveService.files().emptyTrash().execute();
            System.out.println("[GoogleDriveService] Successfully emptied Google Drive trash.");
        } catch (Exception e) {
            System.err.println("[GoogleDriveService] Could not empty trash: " + e.getMessage());
        }
    }

    public static class StorageUsageInfo {
        private final long usageBytes;
        private final long limitBytes;
        private final double usagePercentage;
        private final long usageInTrashBytes;

        public StorageUsageInfo(long usageBytes, long limitBytes, double usagePercentage, long usageInTrashBytes) {
            this.usageBytes = usageBytes;
            this.limitBytes = limitBytes;
            this.usagePercentage = usagePercentage;
            this.usageInTrashBytes = usageInTrashBytes;
        }

        public long getUsageBytes() {
            return usageBytes;
        }

        public long getLimitBytes() {
            return limitBytes;
        }

        public double getUsagePercentage() {
            return usagePercentage;
        }

        public long getUsageInTrashBytes() {
            return usageInTrashBytes;
        }

        public double getUsageMb() {
            return usageBytes / (1024.0 * 1024.0);
        }

        public double getLimitMb() {
            return limitBytes / (1024.0 * 1024.0);
        }

        @Override
        public String toString() {
            return String.format("%.2f MB / %.2f MB (%.2f%% used)", getUsageMb(), getLimitMb(), usagePercentage);
        }
    }
}
