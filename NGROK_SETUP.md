# NgRok Setup for Real Device Testing

This app now supports both Android emulator and real device testing with automatic URL configuration.

## Current Configuration

The app is configured in `app/src/main/java/com/jvn/myapplication/config/ApiConfig.kt`

## Setup Instructions

### For Emulator Testing (Default)
- No changes needed
- The app is already configured for emulator testing
- Uses `10.0.2.2` to access localhost from emulator

### For Real Device Testing

1. **Start your backend server** (if not already running)
   ```bash
   # Your backend should be running on localhost:3000
   npm start  # or whatever command starts your server
   ```

2. **Start MinIO server** (if not already running)
   ```bash
   # MinIO should be running on localhost:9000
   minio server ~/minio-data  # or your MinIO start command
   ```

3. **Install ngrok** (if not already installed)
   ```bash
   # Download from https://ngrok.com/download
   # Or install via package manager
   ```

4. **Create two ngrok tunnels**

   **Terminal 1 - API Server:**
   ```bash
   ngrok http 3000
   ```
   Copy the forwarding URL (e.g., `https://abc123.ngrok.io`)

   **Terminal 2 - MinIO Server:**
   ```bash
   ngrok http 9000
   ```
   Copy the forwarding URL (e.g., `https://def456.ngrok.io`)

5. **Update ApiConfig.kt**
   
   Open `app/src/main/java/com/jvn/myapplication/config/ApiConfig.kt` and:
   
   - Change `DEVELOPMENT_MODE = DevelopmentMode.EMULATOR` to `DEVELOPMENT_MODE = DevelopmentMode.REAL_DEVICE`
   - Update `NGROK_API_URL` with your API ngrok URL
   - Update `NGROK_MINIO_URL` with your MinIO ngrok URL

   Example:
   ```kotlin
   const val DEVELOPMENT_MODE = DevelopmentMode.REAL_DEVICE
   
   private const val NGROK_API_URL = "https://abc123.ngrok.io"
   private const val NGROK_MINIO_URL = "https://def456.ngrok.io"
   ```

6. **Build and test on real device**
   - Clean and rebuild the app
   - Install on your real device
   - Images should now load properly!

## Switching Back to Emulator

To switch back to emulator testing:
1. Change `DEVELOPMENT_MODE = DevelopmentMode.REAL_DEVICE` back to `DEVELOPMENT_MODE = DevelopmentMode.EMULATOR`
2. Rebuild the app

## Troubleshooting

- **Images not loading**: Check that your ngrok URLs are correct and active
- **API calls failing**: Verify the API ngrok tunnel is running and the URL is correct
- **Build errors**: Make sure to clean and rebuild after changing configuration

## Why This Works

- **Emulator**: Uses `localhost` mapped to `10.0.2.2` (Android emulator host IP)
- **Real Device**: Uses ngrok tunnels to access your local servers from the internet
- **Automatic**: The app automatically transforms URLs based on the development mode 