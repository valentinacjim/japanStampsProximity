package com.mapclover.stampquest.domain.usecase

import com.mapclover.stampquest.data.model.Stamp
import com.mapclover.stampquest.data.repository.KmlRepository

class UnlockStampUseCase(
    private val repository: KmlRepository
) {

    suspend operator fun invoke(stamp: Stamp) {
        repository.unlockStamp(stamp.id)
    }
}