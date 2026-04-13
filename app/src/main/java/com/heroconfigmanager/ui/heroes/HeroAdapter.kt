package com.heroconfigmanager.ui.heroes

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.Hero
import com.heroconfigmanager.databinding.ItemHeroBinding

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
            binding.tvHeroName.text  = hero.hero
            binding.tvHeroTitle.text = hero.heroTitle

            binding.chipSkinsCount.text   = "${hero.skins.size} Skins"
            binding.chipUpgradesCount.text = "${hero.upgradeSkins.size} Upgrades"

            Glide.with(binding.root)
                .load(hero.heroSplash)
                .placeholder(R.color.surface_variant)
                .into(binding.imgHeroSplash)

            Glide.with(binding.root)
                .load(hero.heroLogo)
                .placeholder(R.color.surface_variant)
                .centerCrop()
                .into(binding.imgHeroLogo)

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
