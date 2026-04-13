package com.heroconfigmanager.data.model

import com.google.gson.annotations.SerializedName

data class HeroConfig(
    @SerializedName("configVersion") val configVersion: String = "1.0.0",
    @SerializedName("configNote")    val configNote: String = "",
    @SerializedName("roles")         val roles: Roles = Roles()
)

data class Roles(
    @SerializedName("Marksman") val marksman: List<Hero> = emptyList(),
    @SerializedName("Fighter")  val fighter:  List<Hero> = emptyList(),
    @SerializedName("Assassin") val assassin: List<Hero> = emptyList(),
    @SerializedName("Tank")     val tank:     List<Hero> = emptyList(),
    @SerializedName("Mage")     val mage:     List<Hero> = emptyList(),
    @SerializedName("Support")  val support:  List<Hero> = emptyList()
) {
    fun heroesFor(role: String): List<Hero> = when (role) {
        "Marksman" -> marksman
        "Fighter"  -> fighter
        "Assassin" -> assassin
        "Tank"     -> tank
        "Mage"     -> mage
        "Support"  -> support
        else       -> emptyList()
    }

    fun withRole(role: String, heroes: List<Hero>): Roles = when (role) {
        "Marksman" -> copy(marksman = heroes)
        "Fighter"  -> copy(fighter  = heroes)
        "Assassin" -> copy(assassin = heroes)
        "Tank"     -> copy(tank     = heroes)
        "Mage"     -> copy(mage     = heroes)
        "Support"  -> copy(support  = heroes)
        else       -> this
    }

    fun totalHeroes(): Int =
        marksman.size + fighter.size + assassin.size + tank.size + mage.size + support.size
}

data class Hero(
    @SerializedName("hero")          val hero:       String       = "",
    @SerializedName("hero_logo")     val heroLogo:   String       = "",
    @SerializedName("hero_splash")   val heroSplash: String       = "",
    @SerializedName("hero_title")    val heroTitle:  String       = "",
    @SerializedName("hero_type")     val heroType:   String       = "",
    @SerializedName("restore_url")   val restoreUrl: String       = "",
    @SerializedName("skins")         val skins:      List<Skin>        = emptyList(),
    @SerializedName("upgrade_skins") val upgradeSkins: List<UpgradeSkin> = emptyList()
)

data class Skin(
    @SerializedName("skin_title") val skinTitle: String = "",
    @SerializedName("zip_url")    val zipUrl:    String = "",
    @SerializedName("skin_logo")  val skinLogo:  String = "",
    @SerializedName("skin_type")  val skinType:  String = ""
)

// available_skins entries share the same shape as Skin
typealias AvailableSkin = Skin

data class UpgradeSkin(
    @SerializedName("skin_title")      val skinTitle:      String              = "",
    @SerializedName("skin_logo")       val skinLogo:       String              = "",
    @SerializedName("skin_type")       val skinType:       String              = "",
    @SerializedName("available_skins") val availableSkins: List<AvailableSkin> = emptyList()
)

val HERO_ROLES = listOf("Marksman", "Fighter", "Assassin", "Tank", "Mage", "Support")
