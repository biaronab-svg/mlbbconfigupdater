package com.heroconfigmanager.ui.hero

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.heroconfigmanager.data.model.Hero
import com.heroconfigmanager.data.model.Skin
import com.heroconfigmanager.data.model.UpgradeSkin

class HeroEditorViewModel : ViewModel() {

    var heroName: String = ""
    var heroTitle: String = ""
    var heroType: String = ""
    var heroLogo: String = ""
    var heroSplash: String = ""
    var restoreUrl: String = ""

    val skins = MutableLiveData<MutableList<Skin>>(mutableListOf())
    val upgradeSkins = MutableLiveData<MutableList<UpgradeSkin>>(mutableListOf())

    fun loadHero(hero: Hero?) {
        hero ?: return
        heroName = hero.hero
        heroTitle = hero.heroTitle
        heroType = hero.heroType
        heroLogo = hero.heroLogo
        heroSplash = hero.heroSplash
        restoreUrl = hero.restoreUrl
        skins.value = hero.skins.toMutableList()
        upgradeSkins.value = hero.upgradeSkins.toMutableList()
    }

    fun addSkin(skin: Skin) {
        skins.value = (skins.value ?: mutableListOf()).also { it.add(skin) }
    }

    fun updateSkin(index: Int, skin: Skin) {
        val list = skins.value ?: return
        list[index] = skin
        skins.value = list
    }

    fun deleteSkin(index: Int) {
        val list = skins.value ?: return
        list.removeAt(index)
        skins.value = list
    }

    fun setSkins(updated: List<Skin>) {
        skins.value = updated.toMutableList()
    }

    fun addUpgradeSkin(item: UpgradeSkin) {
        upgradeSkins.value = (upgradeSkins.value ?: mutableListOf()).also { it.add(item) }
    }

    fun updateUpgradeSkin(index: Int, item: UpgradeSkin) {
        val list = upgradeSkins.value ?: return
        list[index] = item
        upgradeSkins.value = list
    }

    fun deleteUpgradeSkin(index: Int) {
        val list = upgradeSkins.value ?: return
        list.removeAt(index)
        upgradeSkins.value = list
    }

    fun setUpgradeSkins(updated: List<UpgradeSkin>) {
        upgradeSkins.value = updated.toMutableList()
    }

    fun buildHero(): Hero? {
        if (heroName.isBlank()) return null
        return Hero(
            hero = heroName.trim(),
            heroTitle = heroTitle.trim(),
            heroType = heroType.trim(),
            heroLogo = heroLogo.trim(),
            heroSplash = heroSplash.trim(),
            restoreUrl = restoreUrl.trim(),
            skins = skins.value?.toList() ?: emptyList(),
            upgradeSkins = upgradeSkins.value?.toList() ?: emptyList()
        )
    }
}
