@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.service.runtime.entity

import com.github.yumelira.yumebox.service.runtime.util.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

@Serializable
data class Selection(
    val uuid: UUID,
    val proxy: String,
    val selected: String,
)
