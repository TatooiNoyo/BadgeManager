package io.github.tatooinoyo.star.badge.utils.update

/**
 * 比较语义化版本号（忽略前导 v/V，忽略 -pre 等后缀的数字段）。
 * @return >0 表示 a 更新，0 相等，<0 表示 a 更旧
 */
object VersionComparator {
    fun compare(a: String, b: String): Int {
        val pa = parse(a)
        val pb = parse(b)
        val size = maxOf(pa.size, pb.size)
        for (i in 0 until size) {
            val av = pa.getOrElse(i) { 0 }
            val bv = pb.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    fun isNewer(remote: String, local: String): Boolean = compare(remote, local) > 0

    private fun parse(raw: String): List<Int> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        if (cleaned.isBlank()) return listOf(0)
        return cleaned.split('.').map { part ->
            part.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }
}
