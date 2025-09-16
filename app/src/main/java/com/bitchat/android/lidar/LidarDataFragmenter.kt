package com.bitchat.android.lidar

import com.bitchat.android.model.LidarFragmentContinue
import com.bitchat.android.model.LidarFragmentEnd
import com.bitchat.android.model.LidarFragmentStart
import com.bitchat.android.model.LidarPacket
import com.bitchat.android.model.LidarScan

/**
 * Fragments a LidarScan into a series of LidarPackets for transmission.
 *
 * @property maxFragmentSize The maximum size of the data payload in each fragment.
 */
class LidarDataFragmenter(private val maxFragmentSize: Int = 240) {

    /**
     * Takes a LidarScan, serializes it, and splits it into a list of LidarPackets.
     *
     * @param scan The LidarScan to fragment.
     * @return A list of LidarPackets representing the fragmented scan.
     */
    fun fragment(scan: LidarScan): List<LidarPacket> {
        val data = scan.toByteArray()
        val fragments = mutableListOf<LidarPacket>()
        val chunks = data.asSequence().chunked(maxFragmentSize).toList()
        val totalFragments = chunks.size

        // Create Start packet
        fragments.add(LidarFragmentStart(
            scanTimestampSec = scan.timestampSeconds,
            scanTimestampNanos = scan.timestampNanos,
            totalFragments = totalFragments
        ))

        // Create Continue packets for all but the last chunk
        chunks.forEachIndexed { index, chunk ->
            if (index < chunks.size - 1) {
                fragments.add(LidarFragmentContinue(
                    scanTimestampSec = scan.timestampSeconds,
                    scanTimestampNanos = scan.timestampNanos,
                    fragmentSequence = index,
                    data = chunk.toByteArray()
                ))
            }
        }

        // Create End packet for the last chunk
        chunks.lastOrNull()?.let {
             fragments.add(LidarFragmentEnd(
                scanTimestampSec = scan.timestampSeconds,
                scanTimestampNanos = scan.timestampNanos,
                fragmentSequence = chunks.size - 1,
                data = it.toByteArray()
            ))
        }

        return fragments
    }
}