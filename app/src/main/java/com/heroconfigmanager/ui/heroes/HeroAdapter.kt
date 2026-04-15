package com.heroconfigmanager.ui.heroes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.Hero
import com.heroconfigmanager.databinding.ItemHeroBinding
import com.heroconfigmanager.ui.common.loadImageWithSkeleton

class HeroAdapter(
    private val onEdit:   (Hero) -> Unit,
    private val onDelete: (Hero) -> Unit,
) : ListAdapter<Hero, HeroAdapter.HeroViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeroViewHolder {
        val binding = ItemHeroBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeroViewHolder, position: Int) {
        holder.bind(getItem(position))
        // Staggered slide-in animation per item
        val anim = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_right)
        anim.startOffset = (position * 40L).coerceAtMost(300L)
        holder.itemView.startAnimation(anim)
    }

    inner class HeroViewHolder(private val binding: ItemHeroBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(hero: Hero) {
            val context = binding.root.context

            binding.tvHeroName.text  = hero.hero
            binding.tvHeroTitle.text = hero.heroTitle

            binding.chipHeroType.isVisible = hero.heroType.isNotBlank()
            if (hero.heroType.isNotBlank()) {
                binding.chipHeroType.text = hero.heroType
            }

            binding.chipSkinsCount.text = context.getString(R.string.skins_count_format, hero.skins.size)
            binding.chipUpgradesCount.text = context.getString(R.string.upgrades_count_format, hero.upgradeSkins.size)

            loadImageWithSkeleton(binding.skeletonHeroSplash, binding.imgHeroSplash, hero.heroSplash)
            loadImageWithSkeleton(binding.skeletonHeroLogo, binding.imgHeroLogo, hero.heroLogo)

            binding.tvHeroTitle.visibility = if (hero.heroTitle.isBlank()) View.GONE else View.VISIBLE
            binding.btnEditHero.setOnClickListener   { onEdit(hero)   }
            binding.btnDeleteHero.setOnClickListener { onDelete(hero) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Hero>() {
            override fun areItemsTheSame(old: Hero, new: Hero) = old.hero == new.hero
            override fun areContentsTheSame(old: Hero, new: Hero) = old == new
        }
    }
}
