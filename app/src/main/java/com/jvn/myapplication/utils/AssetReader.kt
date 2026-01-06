package com.jvn.myapplication.utils

import android.content.Context
import com.jvn.myapplication.data.model.Location
import java.io.BufferedReader
import java.io.InputStreamReader

object AssetReader {
    /**
     * Reads the direct4me_locations.csv file from assets and returns a list of Location objects
     */
    fun readLocationsFromAssets(context: Context): List<Location> {
        val locations = mutableListOf<Location>()
        
        try {
            val inputStream = context.assets.open("direct4me_locations.csv")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            
            // Skip header line
            reader.readLine()
            
            // Read each line
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { parseLocationLine(it) }?.let { locations.add(it) }
            }
            
            reader.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return locations
    }
    
    /**
     * Parses a single CSV line (format: ID;Address;Latitude;Longitude)
     */
    private fun parseLocationLine(line: String): Location? {
        return try {
            val parts = line.split(";")
            if (parts.size >= 4) {
                Location(
                    id = parts[0].trim().toInt(),
                    address = parts[1].trim(),
                    latitude = parts[2].trim().toDouble(),
                    longitude = parts[3].trim().toDouble()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

