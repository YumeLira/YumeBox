package com.github.yumelira.yumebox.service.runtime.util

import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.github.yumelira.yumebox.service.runtime.records.PendingDao
import java.util.*

suspend fun generateProfileUUID(): UUID {
    var result = UUID.randomUUID()

    while (ImportedDao.exists(result) || PendingDao.exists(result)) {
        result = UUID.randomUUID()
    }

    return result
}
