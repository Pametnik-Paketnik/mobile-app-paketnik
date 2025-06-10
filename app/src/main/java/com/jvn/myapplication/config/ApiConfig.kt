package com.jvn.myapplication.config

/**
 * Configuration for API and MinIO base URLs
 * 
 * For development and testing:
 * - EMULATOR: Uses localhost mapped to 10.0.2.2
 * - REAL_DEVICE: Uses ngrok URLs for external access
 * 
 * Instructions:
 * 1. For emulator testing: Set DEVELOPMENT_MODE = DevelopmentMode.EMULATOR
 * 2. For real device testing: 
 *    - Set DEVELOPMENT_MODE = DevelopmentMode.REAL_DEVICE
 *    - Update NGROK_API_URL and NGROK_MINIO_URL with your ngrok URLs
 */
object ApiConfig {
    
    enum class DevelopmentMode {
        EMULATOR,
        REAL_DEVICE
    }
    
    // CHANGE THIS BASED ON YOUR TESTING ENVIRONMENT
    val DEVELOPMENT_MODE = DevelopmentMode.REAL_DEVICE
    
    // Emulator configuration
    private const val EMULATOR_API_BASE = "http://10.0.2.2:3000"
    private const val EMULATOR_MINIO_BASE = "http://10.0.2.2:9000"
    
    // Real device configuration (UPDATE WITH YOUR NGROK URLS)
    private const val NGROK_API_URL = "https://mammoth-regular-hamster.ngrok-free.app"  // Your API ngrok URL
    private const val NGROK_MINIO_URL = "https://39dd-176-76-226-209.ngrok-free.app"  // Replace with your MinIO ngrok URL
    
    // Computed base URLs based on development mode
    val API_BASE_URL: String
        get() = when (DEVELOPMENT_MODE) {
            DevelopmentMode.EMULATOR -> EMULATOR_API_BASE
            DevelopmentMode.REAL_DEVICE -> NGROK_API_URL
        }
    
    val MINIO_BASE_URL: String
        get() = when (DEVELOPMENT_MODE) {
            DevelopmentMode.EMULATOR -> EMULATOR_MINIO_BASE
            DevelopmentMode.REAL_DEVICE -> NGROK_MINIO_URL
        }
    
    /**
     * Transforms an image URL to use the correct base URL for the current development mode
     */
    fun transformImageUrl(originalUrl: String?): String? {
        if (originalUrl.isNullOrEmpty()) return null
        
        return when (DEVELOPMENT_MODE) {
            DevelopmentMode.EMULATOR -> {
                // Transform localhost URLs for emulator
                var imageUrl = originalUrl
                    .replace("localhost:9000", "10.0.2.2:9000")
                    .replace("127.0.0.1:9000", "10.0.2.2:9000")
                
                // Add HTTP protocol if missing
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    imageUrl = "http://$imageUrl"
                }
                
                imageUrl
            }
            DevelopmentMode.REAL_DEVICE -> {
                // Transform to use ngrok URL for real devices
                var imageUrl = originalUrl
                
                // Replace localhost/127.0.0.1 with ngrok URL
                if (imageUrl.contains("localhost:9000") || imageUrl.contains("127.0.0.1:9000")) {
                    // Extract the path part (e.g., "/box-images/...")
                    val pathIndex = imageUrl.indexOf("/box-images")
                    if (pathIndex != -1) {
                        val path = imageUrl.substring(pathIndex)
                        imageUrl = "$NGROK_MINIO_URL$path"
                    } else {
                        // Fallback: just replace the base
                        imageUrl = imageUrl
                            .replace("localhost:9000", NGROK_MINIO_URL.removePrefix("https://").removePrefix("http://"))
                            .replace("127.0.0.1:9000", NGROK_MINIO_URL.removePrefix("https://").removePrefix("http://"))
                    }
                }
                
                // Ensure protocol is present
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    imageUrl = if (NGROK_MINIO_URL.startsWith("https://")) {
                        "https://$imageUrl"
                    } else {
                        "http://$imageUrl"
                    }
                }
                
                imageUrl
            }
        }
    }
    
    /**
     * Get current configuration info for debugging
     */
    fun getConfigInfo(): String {
        return """
            Development Mode: $DEVELOPMENT_MODE
            API Base URL: $API_BASE_URL
            MinIO Base URL: $MINIO_BASE_URL
        """.trimIndent()
    }
} 