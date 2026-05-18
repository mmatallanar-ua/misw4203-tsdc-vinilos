package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.PrizeRepository
import javax.inject.Inject

class FakePrizeRepository @Inject constructor() : PrizeRepository {

    private val prizes = mutableListOf(
        Prize(1, "Mejor Álbum Pop", "Academia Nacional de Artes", "Reconocimiento al mejor álbum de pop del año"),
        Prize(2, "Grabación del Año", "Latin Recording Academy", "Premio a la mejor grabación del año en español"),
    )

    override suspend fun getPrizes(): List<Prize> = prizes.toList()

    override suspend fun createPrize(name: String, description: String, organization: String): Prize {
        val new = Prize(prizes.size + 1, name, description, organization)
        prizes.add(new)
        return new
    }
}
