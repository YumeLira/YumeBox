package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConfigurationOverride

class OverrideRepository {

    fun loadPersist(): Result<ConfigurationOverride> {
        return query(Clash.OverrideSlot.Persist)
    }

    fun savePersist(override: ConfigurationOverride): Result<Unit> {
        return save(Clash.OverrideSlot.Persist, override)
    }

    fun clearPersist(): Result<Unit> {
        return clear(Clash.OverrideSlot.Persist)
    }

    fun update(
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

    fun updatePersist(transform: (ConfigurationOverride) -> ConfigurationOverride): Result<ConfigurationOverride> {
        return update(Clash.OverrideSlot.Persist, transform)
    }

    fun updateSession(transform: (ConfigurationOverride) -> ConfigurationOverride): Result<ConfigurationOverride> {
        return update(Clash.OverrideSlot.Session, transform)
    }

    private fun query(slot: Clash.OverrideSlot): Result<ConfigurationOverride> {
        return try {
            Result.success(Clash.queryOverride(slot))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun save(slot: Clash.OverrideSlot, override: ConfigurationOverride): Result<Unit> {
        return try {
            Clash.patchOverride(slot, override)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun clear(slot: Clash.OverrideSlot): Result<Unit> {
        return try {
            Clash.clearOverride(slot)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
