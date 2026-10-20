package com.drquizman.core.mechanic

import com.drquizman.core.i18n.LocalizedText
import com.drquizman.core.i18n.LocaleManager

import com.drquizman.core.mechanic.bets.BetsLines
import com.drquizman.core.mechanic.bets.BetsMechanic
import com.drquizman.core.mechanic.bomb.BombLines
import com.drquizman.core.mechanic.bomb.BombMechanic
import com.drquizman.core.mechanic.east.EastLines
import com.drquizman.core.mechanic.east.EastMechanic
import com.drquizman.core.mechanic.puzzle.PuzzleLines
import com.drquizman.core.mechanic.puzzle.PuzzleMechanic
import com.drquizman.core.mechanic.who.WhoIsRightLines
import com.drquizman.core.mechanic.who.WhoIsRightMechanic
import com.drquizman.core.model.CompetitionType

/**
 * Реестр конкурсов. Новые механики регистрируются здесь и попадают в состав шоу,
 * если включены. Старт и финал выключить нельзя.
 */
object MechanicCatalog {
    private val specs = linkedMapOf<CompetitionType, MechanicSpec>()
    private val enabled = mutableSetOf<CompetitionType>()
    private var language = LocaleManager.languageTag()

    init { populate() }

    private fun refreshLocale() {
        if (language == LocaleManager.languageTag()) return
        val previousEnabled = enabled.toSet()
        language = LocaleManager.languageTag()
        specs.clear()
        populate()
        enabled.clear()
        enabled.addAll(previousEnabled)
    }

    private fun populate() {
        register(
            MechanicSpec(
                type = CompetitionType.FIRST,
                slot = MechanicSlot.START,
                title = LocalizedText.get("MechanicCatalog.001"),
                slogan = LocalizedText.get("MechanicCatalog.002"),
                intro = {
                    LocalizedText.get("MechanicCatalog.003") +
                        LocalizedText.get("MechanicCatalog.004") +
                        LocalizedText.get("MechanicCatalog.005")
                },
                needsTopicPick = true,
                required = true,
            ),
        )
        register(
            MechanicSpec(
                type = CompetitionType.TRUE_FALSE,
                slot = MechanicSlot.MID,
                title = LocalizedText.get("MechanicCatalog.006"),
                slogan = LocalizedText.get("MechanicCatalog.007"),
                intro = {
                    LocalizedText.get("MechanicCatalog.008")
                },
            ),
        )
        register(
            MechanicSpec(
                type = CompetitionType.SPEED,
                slot = MechanicSlot.MID,
                title = LocalizedText.get("MechanicCatalog.009"),
                slogan = LocalizedText.get("MechanicCatalog.010"),
                intro = {
                    LocalizedText.get("MechanicCatalog.011") +
                        LocalizedText.get("MechanicCatalog.012")
                },
                needsTopicPick = true,
            ),
        )
        register(bombSpec())
        register(whoIsRightSpec())
        register(eastSpec())
        register(betsSpec())
        register(puzzleSpec())
        register(
            MechanicSpec(
                type = CompetitionType.FINAL,
                slot = MechanicSlot.END,
                title = LocalizedText.get("MechanicCatalog.013"),
                slogan = LocalizedText.get("MechanicCatalog.014"),
                intro = {
                    LocalizedText.get("MechanicCatalog.015") +
                        LocalizedText.get("MechanicCatalog.016") +
                        LocalizedText.get("MechanicCatalog.017")
                },
                questionCount = 99,
                showPublicTimer = false,
                required = true,
            ),
        )
    }

    fun bombSpec(): MechanicSpec = MechanicSpec(
        type = CompetitionType.BOMB,
        slot = MechanicSlot.MID,
        title = BombLines.TITLE,
        slogan = BombLines.SLOGAN,
        intro = BombLines::intro,
        needsTopicPick = true,
        questionCount = BombMechanic.QUESTIONS,
        showPublicTimer = false,
        enabledByDefault = true,
        createHandler = { BombMechanic() },
    )

    fun whoIsRightSpec(): MechanicSpec = MechanicSpec(
        type = CompetitionType.WHO_IS_RIGHT,
        slot = MechanicSlot.MID,
        title = WhoIsRightLines.TITLE,
        slogan = WhoIsRightLines.SLOGAN,
        intro = WhoIsRightLines::intro,
        needsTopicPick = true,
        questionCount = WhoIsRightMechanic.CAP,
        enabledByDefault = true,
        createHandler = { WhoIsRightMechanic() },
    )

    fun eastSpec(): MechanicSpec = MechanicSpec(
        type = CompetitionType.EAST,
        slot = MechanicSlot.MID,
        title = EastLines.TITLE,
        slogan = EastLines.SLOGAN,
        intro = EastLines::intro,
        needsTopicPick = true,
        questionCount = EastMechanic.QUESTIONS,
        reverseAnswers = true,
        enabledByDefault = true,
        createHandler = { EastMechanic() },
    )

    fun betsSpec(): MechanicSpec = MechanicSpec(
        type = CompetitionType.BETS,
        slot = MechanicSlot.MID,
        title = BetsLines.TITLE,
        slogan = BetsLines.SLOGAN,
        intro = BetsLines::intro,
        questionCount = BetsMechanic.QUESTIONS,
        perQuestionTopic = true,
        enabledByDefault = true,
        createHandler = { BetsMechanic() },
    )

    fun puzzleSpec(): MechanicSpec = MechanicSpec(
        type = CompetitionType.PUZZLE,
        slot = MechanicSlot.MID,
        title = PuzzleLines.TITLE,
        slogan = PuzzleLines.SLOGAN,
        intro = PuzzleLines::intro,
        needsTopicPick = true,
        questionCount = PuzzleMechanic.QUESTIONS,
        puzzleAnswers = true,
        enabledByDefault = true,
        createHandler = { PuzzleMechanic() },
    )

    fun register(spec: MechanicSpec) {
        specs[spec.type] = spec
        if (spec.enabledByDefault) enabled += spec.type
    }

    operator fun get(type: CompetitionType?): MechanicSpec? =
        type?.let { refreshLocale(); specs[it] }

    fun all(): List<MechanicSpec> { refreshLocale(); return specs.values.toList() }

    fun isEnabled(type: CompetitionType): Boolean = type in enabled

    fun setEnabled(type: CompetitionType, on: Boolean) {
        val spec = specs[type] ?: return
        if (spec.required) return
        if (on) enabled += type else enabled -= type
    }

    fun enabled(slot: MechanicSlot): List<CompetitionType> =
        specs.values.filter { it.slot == slot && it.type in enabled }.map { it.type }

    fun createHandler(type: CompetitionType?): MechanicHandler? =
        this[type]?.createHandler?.invoke()
}
