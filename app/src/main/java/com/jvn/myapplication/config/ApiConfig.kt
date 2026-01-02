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
    val DEVELOPMENT_MODE = DevelopmentMode.EMULATOR
    
    // Emulator configuration
    private const val EMULATOR_API_BASE = "http://10.0.2.2:3000"
    private const val EMULATOR_MINIO_BASE = "http://10.0.2.2:9000"
    
    // Real device configuration (UPDATE WITH YOUR NGROK URLS)
    private const val NGROK_API_URL = "http://vitos-macbook-pro:3000"  // Your API ngrok URL
    private const val NGROK_MINIO_URL = "http://vitos-macbook-pro:9000"  // Replace with your MinIO ngrok URL
    
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
                var imageUrl = originalUrl
                
                // Check if this is a backend API URL (new format)
                val isBackendApiUrl = imageUrl.contains("/api/public/boxes/") || 
                                     imageUrl.contains("localhost:3000") || 
                                     imageUrl.contains("127.0.0.1:3000")
                
                if (isBackendApiUrl) {
                    // Transform backend API URLs for emulator
                    imageUrl = imageUrl
                        .replace("localhost:3000", "10.0.2.2:3000")
                        .replace("127.0.0.1:3000", "10.0.2.2:3000")
                } else {
                    // Transform MinIO URLs for emulator (old format, backward compatibility)
                    imageUrl = imageUrl
                        .replace("localhost:9000", "10.0.2.2:9000")
                        .replace("127.0.0.1:9000", "10.0.2.2:9000")
                }
                
                // Add HTTP protocol if missing
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    imageUrl = "http://$imageUrl"
                }
                
                imageUrl
            }
            DevelopmentMode.REAL_DEVICE -> {
                var imageUrl = originalUrl
                
                // Check if this is a backend API URL (new format)
                val isBackendApiUrl = imageUrl.contains("/api/public/boxes/") || 
                                     imageUrl.contains("localhost:3000") || 
                                     imageUrl.contains("127.0.0.1:3000")
                
                if (isBackendApiUrl) {
                    // Transform backend API URLs to use ngrok API URL
                    if (imageUrl.contains("localhost:3000") || imageUrl.contains("127.0.0.1:3000")) {
                        // Extract the path part (e.g., "/api/public/boxes/...")
                        val pathIndex = imageUrl.indexOf("/api")
                        if (pathIndex != -1) {
                            val path = imageUrl.substring(pathIndex)
                            imageUrl = "$NGROK_API_URL$path"
                        } else {
                            // Fallback: just replace the base
                            val baseUrl = NGROK_API_URL.removePrefix("https://").removePrefix("http://")
                            imageUrl = imageUrl
                                .replace("localhost:3000", baseUrl)
                                .replace("127.0.0.1:3000", baseUrl)
                        }
                    }
                } else {
                    // Transform MinIO URLs (old format, backward compatibility)
                    if (imageUrl.contains("localhost:9000") || imageUrl.contains("127.0.0.1:9000")) {
                        // Extract the path part (e.g., "/box-images/...")
                        val pathIndex = imageUrl.indexOf("/box-images")
                        if (pathIndex != -1) {
                            val path = imageUrl.substring(pathIndex)
                            imageUrl = "$NGROK_MINIO_URL$path"
                        } else {
                            // Fallback: just replace the base
                            val baseUrl = NGROK_MINIO_URL.removePrefix("https://").removePrefix("http://")
                            imageUrl = imageUrl
                                .replace("localhost:9000", baseUrl)
                                .replace("127.0.0.1:9000", baseUrl)
                        }
                    }
                }
                
                // Ensure protocol is present
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    // Determine protocol based on which URL we're using
                    val baseUrl = if (isBackendApiUrl) NGROK_API_URL else NGROK_MINIO_URL
                    imageUrl = if (baseUrl.startsWith("https://")) {
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