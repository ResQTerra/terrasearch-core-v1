package com.bitchat.android.lidar

import com.bitchat.android.model.LidarFragmentContinue
import com.bitchat.android.model.LidarFragmentEnd
import com.bitchat.android.model.LidarFragmentStart
import com.bitchat.android.model.LidarPacket
import com.bitchat.android.model.LidarScan
import java.util.concurrent.ConcurrentHashMap

/**
 * Reassembles fragmented LIDAR packets into a complete LidarScan.
 * This class is designed to be thread-safe.
 */
class LidarDataReassembler {

    private data class ScanBuffer(
        val totalFragments: Int,
        val fragments: MutableMap<Int, ByteArray> = ConcurrentHashMap()
    )

    private val buffers = ConcurrentHashMap<Pair<Long, Int>, ScanBuffer>()

    /**
     * Processes an incoming LidarPacket and attempts to reassemble the full scan.
     *
     * @param packet The LidarPacket to process.
     * @return A complete LidarScan if all fragments have been received, otherwise null.
     */
    fun processPacket(packet: LidarPacket): LidarScan? {
        val timestamp = Pair(packet.scanTimestampSec, packet.scanTimestampNanos)

        when (packet) {
            is LidarFragmentStart -> {
                buffers[timestamp] = ScanBuffer(packet.totalFragments)
            }
            is LidarFragmentContinue -> {
                buffers[timestamp]?.fragments?.put(packet.fragmentSequence, packet.data)
            }
            is LidarFragmentEnd -> {
                buffers[timestamp]?.fragments?.put(packet.fragmentSequence, packet.data)
                return reassemble(timestamp)
            }
        }
        return null
    }

    private fun reassemble(timestamp: Pair<Long, Int>): LidarScan? {
        val scanBuffer = buffers[timestamp]

        if (scanBuffer != null && scanBuffer.fragments.size == scanBuffer.totalFragments) {
            val sortedFragments = scanBuffer.fragments.toSortedMap().values
            val combinedData = sortedFragments.fold(byteArrayOf()) { acc, bytes -> acc + bytes }

            // Clean up
            buffers.remove(timestamp)

            return LidarScan.fromByteArray(combinedData)
        }
        return null
    }
}