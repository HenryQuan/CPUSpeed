package com.yihengquan.cpuspeed.data

data class CoreFrequency(
    val core: Int,
    val maxFreq: Int = 0,
    val minFreq: Int = 0,
    val curFreq: Int = 0,
)

data class CPUInfo(
    val maxFrequency: Int = 0,
    val minFrequency: Int = 0,
    val currMaxFrequency: Int = 0,
    val currMinFrequency: Int = 0,
    val cpuInfo: String = "Unknown",
    val coreCount: Int = 1,
    val hasData: Boolean = false,
    val coreFrequencies: List<CoreFrequency> = emptyList(),
) {
    val diffFrequency: Int get() = maxOf(maxFrequency - minFrequency, 1)

    val maxPercent: Float
        get() = if (hasData) (currMaxFrequency - minFrequency).toFloat() / diffFrequency else 0f

    val minPercent: Float
        get() = if (hasData) (currMinFrequency - minFrequency).toFloat() / diffFrequency else 0f

    fun calcFrequency(percent: Float): Int {
        return (percent * diffFrequency + minFrequency).toInt()
    }

    fun calcCoreFrequency(percent: Float, coreMin: Int, coreMax: Int): Int {
        val diff = maxOf(coreMax - coreMin, 1)
        return (percent * diff + coreMin).toInt()
    }
}
