package com.drquizman.core.i18n.en

import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.model.CompetitionType

/** Host lines. Contest copy comes from the mechanic catalog. */
object HostLinesEn {
    fun welcome(): String = listOf(
        "Hello to the beloved and distinguished players and viewers of our medical-intellectual show! Today we’ll find out who among us is the fastest, smartest and sharpest... But why do I keep talking about myself!? Let’s see who will fight for the quiz grand prize today.",
        "Hello, my intellectual friends! We are here to dive into a grand adventure on the waves of knowledge, quick thinking and, for some of you, a little luck! Grab your phones and join the game!",
        "At last I can welcome you again, my dear Quizmaniacs! Today is a historic event — treatment for absolutely everything, live and intellectually! Join in and let’s begin!",
    ).random()

    fun waiting(): String = waitingPool.random()

    private val waitingPool = listOf(
        "What are you waiting for? Join already — a good mood is guaranteed!",
        "How much longer are we waiting for everyone? Did they all go for a medical checkup?",
        "Uhhh... looks like tonight might be a little boring.",
        "Where are you, my patients? The clinic is open and the remotes are waiting!",
        "While nobody joins, I can play with myself. But that is no longer a show — that’s office hours.",
        "Don’t be shy, phones out — today we treat intelligence, no appointment required!",
    )

    fun lobbyChatter(count: Int, ready: Int, previous: String = ""): String {
        val extra = if (count > 0 && ready in 1 until count) {
            listOf("Ready: $ready of $count. Waiting for the rest!")
        } else {
            emptyList()
        }
        val options = waitingPool + extra
        return options.filter { it != previous }.randomOrNull() ?: options.random()
    }

    fun playerJoined(name: String, first: Boolean): String {
        if (first) return "And the first patient to arrive today is $name"
        return listOf(
            "Next player — $name",
            "And now let’s welcome $name",
            "$name, are you sure you didn’t walk into the wrong room? Fine, let’s see what you’ve got.",
            "Ooooh, $name, I really did not expect to see you here!",
            "Hello, $name!",
            "$name, good to see you!",
            "$name, welcome!",
            "Don’t be shy, $name, join us!",
            "$name, have we already met today? Or am I mixing things up?",
            "Hold my bald spot — look who showed up! $name!",
            "Let’s welcome the next player with thunderous applause! $name",
            "$name, I hope today we can get through this without anyone losing their nerves...",
        ).random()
    }

    fun lobbyIdle(count: Int): String = lobbyWaiting(count, 0)

    fun lobbyWaiting(count: Int, ready: Int): String = when {
        count == 0 || ready == 0 -> waiting()
        ready < count -> "Ready: $ready of $count. Waiting for the rest!"
        else -> matchStart()
    }

    fun matchStart(): String =
        "Aaaand everyone is here, and we are ready to begin the treatment... I mean, the battle! A battle where everyone will be a winner, but naturally there can only be one leader! We’ll learn that name very soon. For now — let’s begin!"

    fun matchStartWithGrant(points: Int): String =
        "No starting fortune today — everyone immediately gets $points points, so there is something to lose. Let’s begin!"

    fun intro(type: CompetitionType): String =
        MechanicCatalog[type]?.intro?.invoke() ?: "The show goes on!"

    fun chooseTopic(name: String, lastPlace: Boolean): String = if (lastPlace) {
        listOf(
            "So being in last place doesn’t feel quite so bad, $name, you get to choose the topic.",
            "The player at the back chooses the round topic. $name, that’s you.",
        ).random()
    } else {
        listOf(
            "Who should we trust with choosing the topic this round!? $name, give it a try!",
            "Usually choosing the topic helps you get a better result. Let’s see how it works for you, $name — choose a topic.",
        ).random()
    }

    fun topicPicked(topic: String): String =
        "Round topic — $topic! Let’s go!"

    fun titles(type: CompetitionType): String =
        MechanicCatalog[type]?.title.orEmpty()

    fun allCorrect(): String = listOf(
        "Look at all of you being clever! You really wouldn’t guess at first sight.",
        "Unanimous — and completely correct!",
        "Did you all go to the same school?",
    ).random()

    fun oneCorrect(name: String): String = listOf(
        "Sometimes it pays not to be like everyone else!",
        "Perfect fake-out! Every rival left behind! Toucchhdown!",
        "$name, what, are you the smartest one here?",
    ).random()

    fun allWrong(): String = listOf(
        "Hmm... maybe you’d do better at tug-of-war?",
        "Do you really have to follow the crowd even when everyone is wrong?",
        "Maybe we should start trying?",
    ).random()

    fun mixedResults(): String = listOf(
        "Not a bad start!",
        "I think someone did not even break a sweat!",
        "Like it? Let’s keep going!",
    ).random()

    fun afterFirstThird(): String = listOf(
        "Warm-up over. Moving on to water treatment.",
        "Not a bad start!",
        "I think someone did not even break a sweat!",
        "Like it? Let’s keep going!",
    ).random()

    fun lastStretch(): String = listOf(
        "Final stretch!",
        "Who is sitting in last place back there? Come on, catch up!",
        "Just a little more, just a tiny bit!",
        "Aaaand the final sprint!",
    ).random()

    fun questionFlavor(index: Int, total: Int): String? = when {
        total >= 3 && index == (total / 3).coerceAtLeast(1) -> afterFirstThird()
        total >= 3 && index == total - 2 -> lastStretch()
        else -> null
    }

    fun roundOver(): String = listOf(
        "The round is over! Everyone did great! Let’s look at the standings.",
        "At last, we can sum up the interim results.",
        "Want to know who is leading after this round? I certainly do!",
        "Is it just me, or did the players start falling asleep during the game?",
        "You could say we flew through that in one breath! Who stood out this time?",
    ).random()

    fun standings(ranked: List<String>): String {
        if (ranked.isEmpty()) return roundOver()
        val bits = mutableListOf(roundOver())
        if (ranked.size >= 2) {
            bits += listOf(
                "In last place we have ${ranked.last()}.",
                "Second from last is ${ranked.last()}.",
            ).random()
        }
        val mid = ranked.drop(1).dropLast(1)
        if (mid.isNotEmpty() && ranked.size >= 4) {
            bits += "In the middle of the table: ${mid.joinToString(", ")}."
        }
        if (ranked.size >= 3) {
            bits += "In second place — ${ranked[1]}."
        }
        bits += listOf(
            "In first place — ${ranked.first()}",
            "Leading our quiz — ${ranked.first()}",
            "At the top of the table — ${ranked.first()}",
        ).random()
        return bits.joinToString(" ")
    }

    fun winner(name: String): String = listOf(
        "Aaaand as you already figured out, $name is today’s winner!!! As a prize, the winner receives admiring looks from friends and rivals!",
        "Attention, attention! Breaking news! We have a quiz winner! $name, why aren’t you celebrating? Yessss! Congratulations!",
        "What a game! Incredible tension on our virtual stage! An unbelievable winner! $name, you think this victory goes unpunished!? Ha ha ha.",
        "$name, here you stand on top of Olympus! Is the crown too tight? Oh right, we do not actually have a crown! Ha ha ha.",
        "$name, don’t even think I’m giving you the grand prize! Never, it’s mine, mine and only mine!!!!",
        "Trumpets blare! The crowd goes wild! We’re used to this by now... right, $name?",
    ).random()

    fun backToLobby(winner: String?): String =
        if (winner.isNullOrBlank()) {
            "Another round. Tap “Ready” on your remotes."
        } else {
            "$winner won! If you’re ready for a new game, tap “Ready”."
        }
}
