/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

@file:Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.github.yumelira.yumebox.service.runtime.util

import java.math.BigInteger
import java.net.InetAddress
import kotlin.math.max

data class IncludedRouteSet(val ipv4: List<IPNet>, val ipv6: List<IPNet>)

fun buildIncludedRoutesFromExcludedCidrs(
    cidrs: List<String>,
    includeIpv6: Boolean,
): IncludedRouteSet {
    val parsed = cidrs.mapNotNull(::parseIpCidrOrNull)
    return IncludedRouteSet(
        ipv4 = buildIncludedRoutes(parsed, IPV4_BITS),
        ipv6 = if (includeIpv6) buildIncludedRoutes(parsed, IPV6_BITS) else emptyList(),
    )
}

private data class ParsedCidr(val address: InetAddress, val prefix: Int) {
    val bitSize: Int
        get() = address.address.size * 8
}

private data class AddressRange(val start: BigInteger, val endInclusive: BigInteger)

private fun parseIpCidrOrNull(raw: String): ParsedCidr? {
    val parts = raw.split("/", limit = 2)
    if (parts.size != 2) return null

    val address = runCatching { InetAddress.getByName(parts[0].trim()) }.getOrNull() ?: return null
    val prefix = parts[1].trim().toIntOrNull() ?: return null
    val bitSize = address.address.size * 8
    if (prefix !in 0..bitSize) return null

    return ParsedCidr(address = address, prefix = prefix)
}

private fun buildIncludedRoutes(cidrs: List<ParsedCidr>, bitSize: Int): List<IPNet> {
    val mergedExcluded =
        cidrs
            .asSequence()
            .filter { it.bitSize == bitSize }
            .map(::toRange)
            .sortedBy { it.start }
            .fold(mutableListOf<AddressRange>()) { acc, range ->
                val last = acc.lastOrNull()
                if (last == null) {
                    acc += range
                } else if (range.start <= last.endInclusive + BigInteger.ONE) {
                    acc[acc.lastIndex] =
                        AddressRange(
                            start = last.start,
                            endInclusive = maxOf(last.endInclusive, range.endInclusive),
                        )
                } else {
                    acc += range
                }
                acc
            }

    if (mergedExcluded.isEmpty()) {
        return listOf(IPNet(rootRouteAddress(bitSize), 0))
    }

    val included = mutableListOf<AddressRange>()
    var cursor = BigInteger.ZERO
    val maxAddress = BigInteger.ONE.shiftLeft(bitSize).subtract(BigInteger.ONE)

    mergedExcluded.forEach { range ->
        if (cursor < range.start) {
            included += AddressRange(cursor, range.start - BigInteger.ONE)
        }
        if (range.endInclusive >= maxAddress) {
            cursor = maxAddress + BigInteger.ONE
            return@forEach
        }
        cursor = range.endInclusive + BigInteger.ONE
    }

    if (cursor <= maxAddress) {
        included += AddressRange(cursor, maxAddress)
    }

    return included.flatMap { range -> rangeToCidrs(range, bitSize) }
}

private fun toRange(cidr: ParsedCidr): AddressRange {
    val bitSize = cidr.bitSize
    val hostBits = bitSize - cidr.prefix
    val addressValue = BigInteger(1, cidr.address.address)
    val networkValue =
        if (hostBits == 0) {
            addressValue
        } else {
            addressValue.shiftRight(hostBits).shiftLeft(hostBits)
        }
    val blockSize = BigInteger.ONE.shiftLeft(hostBits)
    return AddressRange(
        start = networkValue,
        endInclusive = networkValue + blockSize - BigInteger.ONE,
    )
}

private fun rangeToCidrs(range: AddressRange, bitSize: Int): List<IPNet> {
    val result = mutableListOf<IPNet>()
    var current = range.start
    while (current <= range.endInclusive) {
        val remaining = range.endInclusive - current + BigInteger.ONE
        val prefixFromAlignment = bitSize - trailingZeroBitCount(current, bitSize)
        val prefixFromRemaining = bitSize - (remaining.bitLength() - 1)
        val prefix = max(prefixFromAlignment, prefixFromRemaining)
        result += IPNet(bigIntegerToAddressString(current, bitSize), prefix)
        current += BigInteger.ONE.shiftLeft(bitSize - prefix)
    }
    return result
}

private fun trailingZeroBitCount(value: BigInteger, bitSize: Int): Int {
    if (value.signum() == 0) return bitSize
    return minOf(bitSize, value.lowestSetBit)
}

private fun bigIntegerToAddressString(value: BigInteger, bitSize: Int): String {
    val byteLength = bitSize / 8
    val raw = value.toByteArray()
    val normalized = ByteArray(byteLength)
    val sourceStart = max(0, raw.size - byteLength)
    val copyLength = raw.size - sourceStart
    System.arraycopy(raw, sourceStart, normalized, byteLength - copyLength, copyLength)
    return InetAddress.getByAddress(normalized).hostAddress
}

private fun rootRouteAddress(bitSize: Int): String {
    return "0.0.0.0".takeIf { bitSize == IPV4_BITS } ?: "::"
}

private const val IPV4_BITS = 32
private const val IPV6_BITS = 128
