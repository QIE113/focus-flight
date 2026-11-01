package com.zhuanshi.feixing.data

data class FlightRecord(
    val fromCode: String,
    val fromName: String,
    val toCode: String,
    val toName: String,
    val minutes: Int,
    val finishedAt: Long
)
