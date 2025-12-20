package io.github.tatooinoyo.star.badge.data

import android.content.Context
import androidx.annotation.StringRes
import io.github.tatooinoyo.star.badge.R

data class TitleAndRemark(@StringRes val titleResId: Int, @StringRes val remarkResId: Int)

object PresetBadges {
    val PRESET_BADGES_MAP = mapOf(
        "SKY-PN-ST-POR-CP" to TitleAndRemark(R.string.preset_badge_valley_of_prophecy, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-SUM-SP" to TitleAndRemark(R.string.preset_badge_legend_of_cloudy_world, R.string.preset_badge_cd_15s),
        "SKY-KC-ST-COB-AI" to TitleAndRemark(R.string.preset_badge_keychain_light_of_love, R.string.preset_badge_cd_3min_dur_3min),
        "SKY-KC-ST-COB-AU" to TitleAndRemark(R.string.preset_badge_keychain_aurora, R.string.preset_badge_cd_3min_dur_3min),
        "SKY-BK-ST-PRO-ART" to TitleAndRemark(R.string.preset_badge_artbook_sky_memories, R.string.preset_badge_cd_15min_dur_20min),
        "SKY-KC-ST-LPP-TF" to TitleAndRemark(R.string.preset_badge_keychain_fox_plushie, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-COB-MM" to TitleAndRemark(R.string.preset_badge_keychain_moomin_doll, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-UM-ST-PRO-LU" to TitleAndRemark(R.string.preset_badge_umbrella_light_seeker, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-PRO-LT" to TitleAndRemark(R.string.preset_badge_lantern, R.string.preset_badge_cd_10min_dur_30min),
        "SKY-FG-ST-PRO-FG-SF1" to TitleAndRemark(R.string.preset_badge_blue_anniversary, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-BL-TS" to TitleAndRemark(R.string.preset_badge_little_one, R.string.preset_badge_cd_15min_dur_20min),
        "SKY-PN-ST-BL-HS" to TitleAndRemark(R.string.preset_badge_big_guy, R.string.preset_badge_cd_15min_dur_20min),
        "SKY-PN-ST-MAS-CB" to TitleAndRemark(R.string.preset_badge_dwarf_badge, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-BL-GR" to TitleAndRemark(R.string.preset_badge_grown_up, R.string.preset_badge_cd_15min_dur_20min),
        "SKY-PN-ST-CAP-MB" to TitleAndRemark(R.string.preset_badge_cape_bat, R.string.preset_badge_cd_20min_dur_15min),
        "SKY-PN-ST-CNT-YIR" to TitleAndRemark(R.string.preset_badge_twin_badge_twisted, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-CNT-YIL" to TitleAndRemark(R.string.preset_badge_twin_badge_harp, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-PL-HC" to TitleAndRemark(R.string.preset_badge_crab_stack, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PL-ST-ANC-BHHS" to TitleAndRemark(R.string.preset_badge_snowman_head, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-CRE-NT" to TitleAndRemark(R.string.preset_badge_turtle, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-SUM-NF" to TitleAndRemark(R.string.preset_badge_new_year_fireworks, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-SUM-CC" to TitleAndRemark(R.string.preset_badge_princess_carry, R.string.preset_badge_cd_15s),
        "SKY-PL-ST-COB-AU1" to TitleAndRemark(R.string.preset_badge_wings_aurora, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-PRO-LS" to TitleAndRemark(R.string.preset_badge_seesaw_of_love, R.string.preset_badge_cd_15min_dur_10min),
    )

    fun getTitle(context: Context, key: String): String {
        val titleAndRemark = PRESET_BADGES_MAP[key]
        return if (titleAndRemark != null) {
            context.getString(titleAndRemark.titleResId)
        } else {
            ""
        }
    }

    fun getRemark(context: Context, key: String): String {
        val titleAndRemark = PRESET_BADGES_MAP[key]
        return if (titleAndRemark != null) {
            context.getString(titleAndRemark.remarkResId)
        } else {
            ""
        }
    }
}