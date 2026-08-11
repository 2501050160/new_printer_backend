# Google Drive Document Storage Setup Guide (100% Free)

This project allows documents uploaded by users (Web UI and WhatsApp Bot) to be stored in **Google Drive** and automatically deleted after **1 day (24 hours)**.

---

## 1. Create a Free Google Cloud Project & Enable Google Drive API

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project (e.g. `University-Cloud-Print`).
3. In the search bar at the top, type **Google Drive API** and click **Enable**.

---

## 2. Create a Service Account Key (JSON)

1. Go to **IAM & Admin** → **Service Accounts** ([direct link](https://console.cloud.google.com/iam-admin/serviceaccounts)).
2. Click **Create Service Account**.
   - **Service account name**: `cloud-print-storage`
   - Click **Create and Continue**, then click **Done**.
3. Click on the newly created Service Account email.
4. Go to the **Keys** tab at the top.
5. Click **Add Key** → **Create new key** → Choose **JSON** → Click **Create**.
6. A `.json` file will download to your computer.

---

## 3. Configure the Backend with the Key

### Option A: Place the JSON file (Local Development & Server)
1. Rename the downloaded file to `google-service-account.json`.
2. Move it to the `printer_backend/credentials/` folder:
   ```
   printer_backend/credentials/google-service-account.json
   ```

### Option B: Using Environment Variables (Cloud Deployment e.g., Render / Railway / Docker)
Set the environment variable `GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON` with the raw contents of the JSON key, or `GOOGLE_DRIVE_SERVICE_ACCOUNT_BASE64` with the base64-encoded string.

---

## 4. (Optional) Share a Specific Google Drive Folder with the Service Account

If you want the files saved in a specific folder in your personal or university Google Drive:
1. Open [Google Drive](https://drive.google.com).
2. Create a folder named `University_Cloud_Print_Uploads`.
3. Right-click the folder → **Share**.
4. Paste the Service Account's email address (e.g., `cloud-print-storage@your-project.iam.gserviceaccount.com`) and give it **Editor** permissions.
5. Copy the Folder ID from the browser URL (`drive.google.com/drive/folders/YOUR_FOLDER_ID`) and set it in `application.properties`:
   ```properties
   google.drive.folder-id=YOUR_FOLDER_ID
   ```
*(If no folder ID is provided, the backend will automatically create and manage its own upload folder).*

---

## 5. Automatic 1-Day (24-Hour) Cleanup

The backend includes a background scheduler (`PdfCleanupService`) that:
- Runs automatically every 15 minutes and during the daily reset (2:00 AM).
- Permanently purges and deletes files older than 24 hours from Google Drive.
- Deletes files from Google Drive as soon as an order is fulfilled past the retention window.
