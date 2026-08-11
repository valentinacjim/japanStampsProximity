package com.mapclover.stampquest.domain.usecase

import com.mapclover.stampquest.data.model.Stamp
import com.mapclover.stampquest.ui.filters.StampFilters

class FilterStampsUseCase {

    operator fun invoke(
        stamps: List<Stamp>,
        filters: StampFilters,
        unlockedIds: Set<String>
    ): List<Stamp> {

        return stamps.filter { stamp ->

            val categoryOk =
                filters.category == null ||
                        stamp.categoria == filters.category

            val unlockedOk = when (filters.seenStatus) {
                com.mapclover.stampquest.ui.filters.SeenStatus.FOUND -> stamp.id in unlockedIds
                com.mapclover.stampquest.ui.filters.SeenStatus.PENDING -> stamp.id !in unlockedIds
                null -> filters.showUnlocked || stamp.id !in unlockedIds
            }

            val areaOk =
                filters.region == null ||
                        stamp.area == filters.region

            categoryOk && unlockedOk && areaOk
        }
    }
}
