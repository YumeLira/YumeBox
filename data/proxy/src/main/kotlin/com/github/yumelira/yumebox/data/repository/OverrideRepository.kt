package com.github.yumelira.yumebox.data.repository

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.remote.ServiceClient

class OverrideRepository(
    private val context: Context
) {
    suspend fun loadPersist(): Result<ConfigurationOverride> =
        query(Clash.OverrideSlot.Persist)

    suspend fun savePersist(override: ConfigurationOverride): Result<Unit> =
        save(Clash.OverrideSlot.Persist, override)

    suspend fun clearPersist(): Result<Unit> =
        clear(Clash.OverrideSlot.Persist)

    suspend fun update(
        slot: Clash.OverrideSlot,
        transform: (ConfigurationOverride) -> ConfigurationOverride
    ): Result<ConfigurationOverride> {
        val current = query(slot).getOrElse { return Result.failure(it) }
        val updated = transform(current)
        val saveResult = save(slot, updated)
        if (saveResult.isFailure) {
            return Result.failure(saveResult.exceptionOrNull() ?: IllegalStateException("保存失败"))
        }
        return Result.success(updated)
    }

    suspend fun updatePersist(
        transform: (ConfigurationOverride) -> ConfigurationOverride
    ): Result<ConfigurationOverride> =
        update(Clash.OverrideSlot.Persist, transform)

    suspend fun updateSession(
        transform: (ConfigurationOverride) -> ConfigurationOverride
    ): Result<ConfigurationOverride> =
        update(Clash.OverrideSlot.Session, transform)

    private suspend fun query(slot: Clash.OverrideSlot): Result<ConfigurationOverride> =
        runCatching {
            ServiceClient.connect(context)
            ServiceClient.clash().queryOverride(slot)
        }

    private suspend fun save(slot: Clash.OverrideSlot, override: ConfigurationOverride): Result<Unit> =
        runCatching {
            ServiceClient.connect(context)
            ServiceClient.clash().patchOverride(slot, override)
        }

    private suspend fun clear(slot: Clash.OverrideSlot): Result<Unit> =
        runCatching {
            ServiceClient.connect(context)
            ServiceClient.clash().clearOverride(slot)
        }
}
