package com.bitchat.android.lidar

import com.bitchat.android.model.LidarScan
import java.io.InputStream
import java.lang.NumberFormatException

/**
 * Parses LIDAR data from a CSV input stream into a list of LidarScan objects.
 */
class LidarDataParser {

    /**
     * Parses the provided input stream, expecting CSV-formatted LIDAR data.
     *
     * @param inputStream The input stream containing the LIDAR data.
     * @return A list of LidarScan objects.
     */
    fun parse(inputStream: InputStream): List<LidarScan> {
        val reader = inputStream.bufferedReader()
        val scans = mutableListOf<LidarScan>()
        reader.forEachLine { line ->
            val parts = line.split(',')
            if (parts.size > 10) {
                try {
                    val timestampSeconds = parts[0].toLong()
                    val timestampNanos = parts[1].toInt()

                    // Data starts from index 10
                    val measurementData = parts.subList(10, parts.size).map { it.toFloat() }
                    val numPoints = measurementData.size / 2 // Assuming half ranges, half intensities

                    val ranges = measurementData.subList(0, numPoints).toFloatArray()
                    val intensities = measurementData.subList(numPoints, measurementData.size).toFloatArray()

                    scans.add(LidarScan(timestampSeconds, timestampNanos, ranges, intensities))
                } catch (e: NumberFormatException) {
                    // Ignore lines that cannot be parsed
                }
            }
        }
        return scans
    }
}