package com.mapclover.stampquest.domain.usecase

import com.mapclover.stampquest.data.model.Stamp
import com.mapclover.stampquest.data.repository.JsonRepository

class UnlockStampUseCase(
    private val repository: JsonRepository
) {

    suspend operator fun invoke(stamp: Stamp) {
        repository.unlockStamp(stamp.id)
    }
}