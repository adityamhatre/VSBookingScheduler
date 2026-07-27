package com.adityamhatre.bookingscheduler.enums

import com.adityamhatre.bookingscheduler.Application
import com.adityamhatre.bookingscheduler.R

enum class Accommodation(val readableName: String, val calendarId: String) {
    BUNGALOW_3_1(
        "Bungalow (3 + 1)",
        Application.getApplicationContext().getString(R.string.bungalow_3_1_id)
    ),
    SPECIAL_ROOM_1(
        "Aaram Room 4",
        Application.getApplicationContext().getString(R.string.special_room_1_id)
    ),
    SPECIAL_ROOM_2(
        "Aaram Room 5",
        Application.getApplicationContext().getString(R.string.special_room_2_id)
    ),
    ROOM_1_VIHAR(
        "Deluxe Room 1 (Vihar)",
        Application.getApplicationContext().getString(R.string.room_1_id)
    ),
    ROOM_2_VISHAVA(
        "Deluxe Room 2 (Vishava)",
        Application.getApplicationContext().getString(R.string.room_2_id)
    ),
    ROOM_3_VISHRAM(
        "Deluxe Room 3 (Vishram)",
        Application.getApplicationContext().getString(R.string.room_3_id)
    ),
    ROOM_4_VISHRANT(
        "Deluxe Room 4 (Vishrant)",
        Application.getApplicationContext().getString(R.string.room_4_id)
    ),
    NIVANT("Nivant", Application.getApplicationContext().getString(R.string.nivant_id)),
    DORMITORY_SOBAT(
        "Dormitory (Sobat)",
        Application.getApplicationContext().getString(R.string.dormitory_1)
    ),
    DORMITORY_SANGAT(
        "Dormitory (Sangat)",
        Application.getApplicationContext().getString(R.string.dormitory_2)
    ),
    BUNGALOW_5_1(
        "Bungalow (5 + 1)",
        Application.getApplicationContext().getString(R.string.bungalow_3_1_id)
    ),
    BIG_LAWN("Kumudini Lawn", Application.getApplicationContext().getString(R.string.big_lawn)),
    AARAM_LAWN(
        "Lawn in front of Aaram Bungalow",
        Application.getApplicationContext().getString(R.string.aaram_lawn)
    ),
    FOUR_ROOM_LAWN(
        "Lawn in front of Four rooms",
        Application.getApplicationContext().getString(R.string.four_room_lawn)
    ),
    NIVANT_ASHTAKON_LAWN(
        "Ramai Lawn",
        Application.getApplicationContext().getString(R.string.nivant_ashtakon_lawn)
    ),
    NEW_ASHTAKON_LAWN(
        "Lawn near new ashtakon area",
        Application.getApplicationContext().getString(R.string.new_ashtakon_lawn)
    ),
    ONE_DAY(
        "One Day",
        Application.getApplicationContext().getString(R.string.one_day)
    ),
    PREMIUM_ROOM_SAANJ(
        "Premium Room Saanj",
        Application.getApplicationContext().getString(R.string.premium_room_saanj)

    ),
    PREMIUM_ROOM_SUGANDH(
        "Premium Room Sugandh",
        Application.getApplicationContext().getString(R.string.premium_room_sugandh)
    ),
    PREMIUM_ROOM_SUKHAD(
        "Premium Room Sukhad",
        Application.getApplicationContext().getString(R.string.premium_room_sukhad)
    ),
    PREMIUM_ROOM_SUMAN(
        "Premium Room Suman",
        Application.getApplicationContext().getString(R.string.premium_room_suman)
    ),
    PREMIUM_ROOM_SUREKH(
        "Premium Room Surekh",
        Application.getApplicationContext().getString(R.string.premium_room_surekh)
    ),
    LAWN_INFRONT_OF_PREMIUM_ROOMS(
        "Lawn in front of premium rooms",
        Application.getApplicationContext().getString(R.string.lawn_premium_room)
    ),
    SHREE_SWAMI_SAMARTH_BANQUET_HALL(
        "Shree Swami Samarth Banquet Hall",
        Application.getApplicationContext().getString(R.string.swami_samarth_hall)
    ),
    SUMANGAL_SANGAL_SABHAGRUH(
        "Sumangal Sangal Sabhagruh",
        Application.getApplicationContext().getString(R.string.sumangal_sangal_sabhagruh_id)
    );

    companion object {
        fun allForGettingBookings() = values().filter { it !in arrayOf(BUNGALOW_5_1) }.map { it }
        fun all() = values().filter { it !in arrayOf(ONE_DAY, BUNGALOW_5_1) }.map { it }
        fun from(key: String) = all().first { key.startsWith(it.calendarId) }
        fun byReadableName(text: String) = all().first { it.readableName == text }
        fun bungalow51List(accommodations: Set<Accommodation>): Set<Accommodation> {
            val returnSet = mutableSetOf<Accommodation>()
            returnSet.addAll(accommodations)

            if (accommodations.contains(BUNGALOW_3_1) && accommodations.contains(SPECIAL_ROOM_1) && accommodations.contains(
                    SPECIAL_ROOM_2
                )
            ) {
                returnSet.remove(BUNGALOW_3_1)
                returnSet.remove(SPECIAL_ROOM_1)
                returnSet.remove(SPECIAL_ROOM_2)
                returnSet.add(BUNGALOW_5_1)
            }
            return returnSet.toSet()
        }

        fun isWholeResort(accommodations: Set<Accommodation>) = all().size == accommodations.size
    }

}
