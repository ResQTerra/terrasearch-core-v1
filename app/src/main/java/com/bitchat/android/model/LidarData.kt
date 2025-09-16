package com.bitchat.android.model

import java.nio.ByteBuffer

/**
 * Represents a single LIDAR scan, including timestamp, range, and intensity data.
 * This class provides methods for serializing and deserializing the scan data to/from a byte array.
 */
data class LidarScan(
    val timestampSeconds: Long,
    val timestampNanos: Int,
    val ranges: FloatArray,
    val intensities: FloatArray
) {
    /**
     * Serializes the LidarScan object into a byte array for transmission.
     * The binary format is:
     * - Timestamp (seconds): 8 bytes (Long)
     * - Timestamp (nanoseconds): 4 bytes (Int)
     * - Number of points: 2 bytes (Short)
     * - Range data: 4 bytes per point (Float)
     * - Intensity data: 4 bytes per point (Float)
     */
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(8 + 4 + 2 + (ranges.size + intensities.size) * 4)
        buffer.putLong(timestampSeconds)
        buffer.putInt(timestampNanos)
        buffer.putShort(ranges.size.toShort())
        for (range in ranges) {
            buffer.putFloat(range)
        }
        for (intensity in intensities) {
            buffer.putFloat(intensity)
        }
        return buffer.array()
    }

    companion object {
        /**
         * Deserializes a LidarScan object from a byte array.
         */
        fun fromByteArray(data: ByteArray): LidarScan {
            val buffer = ByteBuffer.wrap(data)
            val timestampSeconds = buffer.long
            val timestampNanos = buffer.int
            val numPoints = buffer.short.toInt()
            val ranges = FloatArray(numPoints)
            for (i in 0 until numPoints) {
                ranges[i] = buffer.float
            }
            val intensities = FloatArray(numPoints)
            for (i in 0 until numPoints) {
                intensities[i] = buffer.float
            }
            return LidarScan(timestampSeconds, timestampNanos, ranges, intensities)
        }
    }
}

/**
 * A sealed class representing different types of LIDAR packets used for fragmentation.
 */
sealed class LidarPacket {
    abstract val scanTimestampSec: Long
    abstract val scanTimestampNanos: Int
}

/**
 * A packet indicating the start of a fragmented LIDAR scan transmission.
 */
data class LidarFragmentStart(
    override val scanTimestampSec: Long,
    override val scanTimestampNanos: Int,
    val totalFragments: Int
) : LidarPacket()

/**
 * A packet containing a data chunk of a fragmented LIDAR scan.
 */
data class LidarFragmentContinue(
    override val scanTimestampSec: Long,
    override val scanTimestampNanos: Int,
    val fragmentSequence: Int,
    val data: ByteArray
) : LidarPacket()

/**
 * A packet indicating the end of a fragmented LIDAR scan transmission.
 */
data class LidarFragmentEnd(
    override val scanTimestampSec: Long,
    override val scanTimestampNanos: Int,
    val fragmentSequence: Int,
    val data: ByteArray
) : LidarPacket()