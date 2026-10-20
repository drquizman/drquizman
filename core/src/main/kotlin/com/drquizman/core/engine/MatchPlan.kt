package com.drquizman.core.engine

import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.mechanic.MechanicSlot
import com.drquizman.core.model.CompetitionType

/** Как собрали шоу в меню: при новой игре из лобби набор раздаётся заново. */
sealed class ShowPlan {
    data object Standard : ShowPlan()
    data object Long : ShowPlan()
    data class Custom(val selected: Collection<CompetitionType>) : ShowPlan()

    fun deal(): List<CompetitionType> = when (this) {
        Standard -> MatchPlan.standard()
        Long -> MatchPlan.long()
        is Custom -> MatchPlan.custom(selected)
    }
}

/** Состав шоу: старт, середина из включённых механик, финал. */
object MatchPlan {
    const val STANDARD_ROUNDS = 3
    const val LONG_ROUNDS = 6
    const val MIN_ROUNDS = 2
    const val MAX_ROUNDS = 6

    val midPool: List<CompetitionType>
        get() = MechanicCatalog.enabled(MechanicSlot.MID)

    fun standard(): List<CompetitionType> = lineup(STANDARD_ROUNDS, randomSingleMid = true)

    fun long(): List<CompetitionType> {
        val midSlots = LONG_ROUNDS - 2
        val pool = midPool.shuffled()
        if (pool.isEmpty()) return listOf(CompetitionType.FIRST, CompetitionType.FINAL)
        val mids = List(midSlots) { i -> pool[i % pool.size] }
        return listOf(CompetitionType.FIRST) + mids + listOf(CompetitionType.FINAL)
    }

    /** Отмеченные механики: старт и финал на местах, середину перемешиваем. */
    fun custom(selected: Collection<CompetitionType>): List<CompetitionType> {
        val want = selected.toSet()
        if (want.isEmpty()) return emptyList()
        val start = MechanicCatalog.all().filter { it.slot == MechanicSlot.START && it.type in want }.map { it.type }
        val mid = MechanicCatalog.all().filter { it.slot == MechanicSlot.MID && it.type in want }.map { it.type }.shuffled()
        val end = MechanicCatalog.all().filter { it.slot == MechanicSlot.END && it.type in want }.map { it.type }
        return start + mid + end
    }

    fun lineup(count: Int, randomSingleMid: Boolean = false): List<CompetitionType> {
        val n = count.coerceIn(MIN_ROUNDS, MAX_ROUNDS)
        val midSlots = n - 2
        val mids = when {
            midSlots <= 0 -> emptyList()
            midSlots == 1 && randomSingleMid -> listOf(midPool.random())
            else -> {
                val pool = midPool.shuffled()
                if (pool.isEmpty()) emptyList()
                else List(midSlots) { i -> pool[i % pool.size] }
            }
        }
        return listOf(CompetitionType.FIRST) + mids + listOf(CompetitionType.FINAL)
    }
}
