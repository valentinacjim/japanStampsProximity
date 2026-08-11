package com.mapclover.stampquest.domain.usecase

import com.mapclover.stampquest.data.model.Stamp
import com.mapclover.stampquest.ui.filters.StampFilters
import org.junit.Test
import org.junit.Assert.assertEquals

class FilterStampsUseCaseTest {
    private val useCase = FilterStampsUseCase()
    private val tokyoFood = stamp(id = "tokyo-food", area = "Tokyo", category = "Food")
    private val kyotoCulture = stamp(id = "kyoto-culture", area = "Kyoto", category = "Culture")

    @Test
    fun `hides already found stamps when requested`() {
        val result = useCase(
            stamps = listOf(tokyoFood, kyotoCulture),
            filters = StampFilters(showUnlocked = false),
            unlockedIds = setOf(tokyoFood.id)
        )

        assertEquals(listOf(kyotoCulture), result)
    }

    @Test
    fun `combines area and category filters`() {
        val result = useCase(
            stamps = listOf(tokyoFood, kyotoCulture),
            filters = StampFilters(region = "Tokyo", category = "Food"),
            unlockedIds = emptySet()
        )

        assertEquals(listOf(tokyoFood), result)
    }

    private fun stamp(id: String, area: String, category: String) = Stamp(
        id = id,
        nombreJp = "",
        nombreEn = id,
        direccion = "",
        url = "",
        tieneSello = "",
        categoria = category,
        area = area,
        lat = 35.0,
        lon = 139.0
    )
}
