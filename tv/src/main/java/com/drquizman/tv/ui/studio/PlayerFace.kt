package com.drquizman.tv.ui.studio

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.drquizman.tv.R
import kotlinx.coroutines.delay

enum class PlayerFaceMood { IDLE, TALK, YES, NO }

@Composable
fun PlayerFace(
    avatarIndex: Int,
    mood: PlayerFaceMood,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val i = avatarIndex.mod(10)
    val clip = remember(i, mood) { clipFor(i, mood) }
    var frame by remember(clip) { mutableIntStateOf(0) }
    LaunchedEffect(clip) {
        if (clip.size <= 1) return@LaunchedEffect
        while (true) {
            delay(clip[frame % clip.size].ms)
            frame = (frame + 1) % clip.size
        }
    }
    Image(
        painter = painterResource(clip[frame % clip.size].res),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

private data class FaceHold(val res: Int, val ms: Long)

private fun clipFor(i: Int, mood: PlayerFaceMood): Array<FaceHold> {
    val idle = IdleRes[i]
    val blink = BlinkRes[i]
    val talk = TalkRes[i]
    val talk2 = Talk2Res[i]
    val yes = YesRes[i]
    val yes2 = Yes2Res[i]
    val no = NoRes[i]
    val no2 = No2Res[i]
    return when (mood) {
        PlayerFaceMood.IDLE -> arrayOf(
            FaceHold(idle, 400),
            FaceHold(idle, 280),
            FaceHold(blink, 80),
            FaceHold(idle, 520),
            FaceHold(blink, 70),
            FaceHold(blink, 80),
        )
        PlayerFaceMood.TALK -> arrayOf(
            FaceHold(idle, 90),
            FaceHold(talk, 100),
            FaceHold(talk2, 110),
            FaceHold(talk, 90),
            FaceHold(idle, 80),
            FaceHold(talk2, 120),
        )
        PlayerFaceMood.YES -> arrayOf(
            FaceHold(yes, 140),
            FaceHold(yes2, 160),
            FaceHold(yes, 120),
            FaceHold(yes2, 180),
        )
        PlayerFaceMood.NO -> arrayOf(
            FaceHold(no, 150),
            FaceHold(no2, 170),
            FaceHold(no, 130),
            FaceHold(no2, 190),
        )
    }
}

private val IdleRes = intArrayOf(
    R.drawable.player_avatar_0, R.drawable.player_avatar_1, R.drawable.player_avatar_2,
    R.drawable.player_avatar_3, R.drawable.player_avatar_4, R.drawable.player_avatar_5,
    R.drawable.player_avatar_6, R.drawable.player_avatar_7, R.drawable.player_avatar_8,
    R.drawable.player_avatar_9,
)
private val BlinkRes = intArrayOf(
    R.drawable.player_face_0_blink, R.drawable.player_face_1_blink, R.drawable.player_face_2_blink,
    R.drawable.player_face_3_blink, R.drawable.player_face_4_blink, R.drawable.player_face_5_blink,
    R.drawable.player_face_6_blink, R.drawable.player_face_7_blink, R.drawable.player_face_8_blink,
    R.drawable.player_face_9_blink,
)
private val TalkRes = intArrayOf(
    R.drawable.player_face_0_talk, R.drawable.player_face_1_talk, R.drawable.player_face_2_talk,
    R.drawable.player_face_3_talk, R.drawable.player_face_4_talk, R.drawable.player_face_5_talk,
    R.drawable.player_face_6_talk, R.drawable.player_face_7_talk, R.drawable.player_face_8_talk,
    R.drawable.player_face_9_talk,
)
private val Talk2Res = intArrayOf(
    R.drawable.player_face_0_talk2, R.drawable.player_face_1_talk2, R.drawable.player_face_2_talk2,
    R.drawable.player_face_3_talk2, R.drawable.player_face_4_talk2, R.drawable.player_face_5_talk2,
    R.drawable.player_face_6_talk2, R.drawable.player_face_7_talk2, R.drawable.player_face_8_talk2,
    R.drawable.player_face_9_talk2,
)
private val YesRes = intArrayOf(
    R.drawable.player_face_0_yes, R.drawable.player_face_1_yes, R.drawable.player_face_2_yes,
    R.drawable.player_face_3_yes, R.drawable.player_face_4_yes, R.drawable.player_face_5_yes,
    R.drawable.player_face_6_yes, R.drawable.player_face_7_yes, R.drawable.player_face_8_yes,
    R.drawable.player_face_9_yes,
)
private val Yes2Res = intArrayOf(
    R.drawable.player_face_0_yes2, R.drawable.player_face_1_yes2, R.drawable.player_face_2_yes2,
    R.drawable.player_face_3_yes2, R.drawable.player_face_4_yes2, R.drawable.player_face_5_yes2,
    R.drawable.player_face_6_yes2, R.drawable.player_face_7_yes2, R.drawable.player_face_8_yes2,
    R.drawable.player_face_9_yes2,
)
private val NoRes = intArrayOf(
    R.drawable.player_face_0_no, R.drawable.player_face_1_no, R.drawable.player_face_2_no,
    R.drawable.player_face_3_no, R.drawable.player_face_4_no, R.drawable.player_face_5_no,
    R.drawable.player_face_6_no, R.drawable.player_face_7_no, R.drawable.player_face_8_no,
    R.drawable.player_face_9_no,
)
private val No2Res = intArrayOf(
    R.drawable.player_face_0_no2, R.drawable.player_face_1_no2, R.drawable.player_face_2_no2,
    R.drawable.player_face_3_no2, R.drawable.player_face_4_no2, R.drawable.player_face_5_no2,
    R.drawable.player_face_6_no2, R.drawable.player_face_7_no2, R.drawable.player_face_8_no2, R.drawable.player_face_9_no2,
)
