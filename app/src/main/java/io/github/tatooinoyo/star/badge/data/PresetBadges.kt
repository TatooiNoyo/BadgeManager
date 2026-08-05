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
        "SKY-BK-ST-FS-PUB" to TitleAndRemark(R.string.preset_badge_pop_up_book, R.string.preset_badge_cd_15min_dur_20min),
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
        "SKY-PL-ST-ANC-BHOF" to TitleAndRemark(R.string.preset_badge_snowman_body, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-CRE-NT" to TitleAndRemark(R.string.preset_badge_turtle, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-SUM-NF" to TitleAndRemark(R.string.preset_badge_new_year_fireworks, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-SUM-CC" to TitleAndRemark(R.string.preset_badge_princess_carry, R.string.preset_badge_cd_15s),
        "SKY-PL-ST-COB-AU1" to TitleAndRemark(R.string.preset_badge_wings_aurora, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-PRO-LS" to TitleAndRemark(R.string.preset_badge_seesaw_of_love, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-ANC-BH" to TitleAndRemark(R.string.preset_badge_keychain_bear_hug, R.string.preset_badge_cd_15s),
        "SKY-PL-ST-CRE-MF" to TitleAndRemark(R.string.preset_badge_plush_pippi_cat, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-BL-RP" to TitleAndRemark(R.string.preset_badge_rose_path, R.string.preset_badge_cd_15min_dur_20min),
        "SKY-PN-ST-SUM-FF" to TitleAndRemark(R.string.preset_badge_lucky_fireworks, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-POR-HV" to TitleAndRemark(R.string.preset_badge_hermit_valley, R.string.preset_badge_cd_20min),
        "SKY-PN-ST-POR-SD" to TitleAndRemark(R.string.preset_badge_starlight_desert, R.string.preset_badge_cd_20min),
        "SKY-PN-ST-POR-FA" to TitleAndRemark(R.string.preset_badge_forgotten_ark, R.string.preset_badge_cd_30min),
        "SKY-PN-ST-SUM-PPB" to TitleAndRemark(R.string.preset_badge_calla_lily, R.string.preset_badge_cd_15s),
        "SKY-PL-ST-CRE-MNT" to TitleAndRemark(R.string.preset_badge_manatee_plush, R.string.preset_badge_cd_15s),
        "SKY-KC-ST-CB-AC" to TitleAndRemark(R.string.preset_badge_keychain_angry_crab, R.string.preset_badge_cd_15min_dur_15min),
        "SKY-AP-ST-SF-WF" to TitleAndRemark(R.string.preset_badge_winter_feast_scarf, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-BK-ST-HOL-ART" to TitleAndRemark(R.string.preset_badge_artbook_gratitude_nesting, R.string.preset_badge_cd_15min_dur_20min),
        "SKY-FG-ST-KD-TBD" to TitleAndRemark(R.string.preset_badge_thatskykid, R.string.preset_badge_cd_15min_dur_5min_scene),
        "SKY-KC-ST-CB-6" to TitleAndRemark(R.string.preset_badge_keychain_crab_classic, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-COS-FH" to TitleAndRemark(R.string.preset_badge_keychain_fish_hood, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-CRE-DD" to TitleAndRemark(R.string.preset_badge_keychain_dark_dragon_se, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-CRE-MNT" to TitleAndRemark(R.string.preset_badge_keychain_manatee, R.string.preset_badge_cd_3min_dur_3min),
        "SKY-KC-ST-HOL-CC" to TitleAndRemark(R.string.preset_badge_keychain_cradle_carry, R.string.preset_badge_cd_15s_dur_5min),
        "SKY-KC-ST-RB-6" to TitleAndRemark(R.string.preset_badge_keychain_bunny_pin, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-RB-BU" to TitleAndRemark(R.string.preset_badge_keychain_bunny_charm, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-SC1-6" to TitleAndRemark(R.string.preset_badge_keychain_crab_se1, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-KC-ST-SUM-MS" to TitleAndRemark(R.string.preset_badge_keychain_meteor_shower, R.string.preset_badge_cd_15s),
        "SKY-PL-ST-CRE-DE" to TitleAndRemark(R.string.preset_badge_sky_deer_plush, R.string.preset_badge_cd_15s),
        "SKY-PL-ST-CRE-LO" to TitleAndRemark(R.string.preset_badge_little_oreo_plush, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PL-ST-LO-SC" to TitleAndRemark(R.string.preset_badge_little_oreo_plush_se, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PL-ST-MA-38" to TitleAndRemark(R.string.preset_badge_manta_plush_38, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-ANC-DP" to TitleAndRemark(R.string.preset_badge_elder_prairie, R.string.preset_badge_cd_30min),
        "SKY-PN-ST-ANC-GW" to TitleAndRemark(R.string.preset_badge_elder_wasteland, R.string.preset_badge_cd_30min),
        "SKY-PN-ST-ANC-HF" to TitleAndRemark(R.string.preset_badge_elder_forest, R.string.preset_badge_cd_20min),
        "SKY-PN-ST-ANC-ID" to TitleAndRemark(R.string.preset_badge_elder_isle, R.string.preset_badge_cd_30min),
        "SKY-PN-ST-ANC-VK" to TitleAndRemark(R.string.preset_badge_elder_vault, R.string.preset_badge_cd_20min),
        "SKY-PN-ST-ANC-VT" to TitleAndRemark(R.string.preset_badge_elder_valley, R.string.preset_badge_cd_30min),
        "SKY-PN-ST-BL-HT" to TitleAndRemark(R.string.preset_badge_huge_tiny_spell, R.string.preset_badge_cd_15min_dur_20min),
        "SKY-PN-ST-CAP-BM" to TitleAndRemark(R.string.preset_badge_bloom_cape, R.string.preset_badge_cd_30min_dur_30min),
        "SKY-PN-ST-CAP-BP" to TitleAndRemark(R.string.preset_badge_bloom_pin, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-CAP-BW" to TitleAndRemark(R.string.preset_badge_belonging_white_cape, R.string.preset_badge_cd_30min_dur_10min),
        "SKY-PN-ST-CAP-DP" to TitleAndRemark(R.string.preset_badge_dreams_postman_cape, R.string.preset_badge_cd_30min_dur_10min),
        "SKY-PN-ST-CAP-EA" to TitleAndRemark(R.string.preset_badge_enchantment_alchemist_cape, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-CAP-FS" to TitleAndRemark(R.string.preset_badge_feast_snowflake_cape, R.string.preset_badge_cd_15min_dur_30min),
        "SKY-PN-ST-CAP-LP" to TitleAndRemark(R.string.preset_badge_lightseeker_petal_cape, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-CAP-LS" to TitleAndRemark(R.string.preset_badge_lightseeker_cape, R.string.preset_badge_cd_30min_dur_10min),
        "SKY-PN-ST-CAP-RD" to TitleAndRemark(R.string.preset_badge_rhythm_director_cape, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-COS-SS" to TitleAndRemark(R.string.preset_badge_sanctuary_sunglasses, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-COS-WH" to TitleAndRemark(R.string.preset_badge_mischief_witch_hat, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-COS-WM-2021" to TitleAndRemark(R.string.preset_badge_weasel_mask, R.string.preset_badge_cd_30min_dur_10min),
        "SKY-PN-ST-CRE-FC-2021" to TitleAndRemark(R.string.preset_badge_flipped_crab, R.string.preset_badge_cd_1min_dur_5s),
        "SKY-PN-ST-CRE-MA" to TitleAndRemark(R.string.preset_badge_manta_pin, R.string.preset_badge_cd_1min_dur_5s),
        "SKY-PN-ST-EXP-SH" to TitleAndRemark(R.string.preset_badge_moments_side_hug, R.string.preset_badge_cd_15s_dur_5min),
        "SKY-PN-ST-MAS-FT" to TitleAndRemark(R.string.preset_badge_fortune_tiger_mask, R.string.preset_badge_cd_15min_dur_30min),
        "SKY-PN-ST-MAS-GF" to TitleAndRemark(R.string.preset_badge_gratitude_fox_mask, R.string.preset_badge_cd_30min_dur_10min),
        "SKY-PN-ST-MAS-NY" to TitleAndRemark(R.string.preset_badge_new_year_mask, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-MAS-RA" to TitleAndRemark(R.string.preset_badge_rhythm_actor_mask, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-MI-HH" to TitleAndRemark(R.string.preset_badge_harmony_harp, R.string.preset_badge_cd_30min_dur_10min),
        "SKY-PN-ST-MI-RP" to TitleAndRemark(R.string.preset_badge_rhythm_piano, R.string.preset_badge_cd_15min_dur_10min),
        "SKY-PN-ST-MI-SH" to TitleAndRemark(R.string.preset_badge_scout_horn, R.string.preset_badge_cd_30min_dur_10min),
        "SKY-PN-ST-POR-HF" to TitleAndRemark(R.string.preset_badge_hidden_portal_forest, R.string.preset_badge_cd_20min),
        "SKY-PN-ST-POR-SI" to TitleAndRemark(R.string.preset_badge_sanctuary_islands, R.string.preset_badge_cd_20min),
        "SKY-PN-ST-PRO-BT" to TitleAndRemark(R.string.preset_badge_bloom_tea_set, R.string.preset_badge_cd_30min_dur_30min),
        "SKY-PN-ST-PRO-LB" to TitleAndRemark(R.string.preset_badge_love_boat, R.string.preset_badge_cd_15min_dur_30min),
        "SKY-PN-ST-SUM-DF" to TitleAndRemark(R.string.preset_badge_dark_dragon_fireworks, R.string.preset_badge_cd_1min_expires_icon),
        "SKY-PN-ST-SUM-HM" to TitleAndRemark(R.string.preset_badge_day_night, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-SUM-SF" to TitleAndRemark(R.string.preset_badge_bloom_sunflower, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-SUM-WB" to TitleAndRemark(R.string.preset_badge_wisteria_bloom, R.string.preset_badge_cd_15s),
        "SKY-PN-ST-TGC-LO" to TitleAndRemark(R.string.preset_badge_tgc_pin, R.string.preset_badge_cd_12h_dur_5min),
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