package com.heroconfigmanager.ui.hero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.Skin
import com.heroconfigmanager.databinding.DialogSkinEditorBinding
import com.heroconfigmanager.databinding.FragmentEditorSkinsBinding
import com.heroconfigmanager.databinding.ItemSkinBinding

class SkinsFragment : Fragment() {

    private var _binding: FragmentEditorSkinsBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: HeroEditorViewModel
    private lateinit var adapter: SkinAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditorSkinsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(requireActivity())[HeroEditorViewModel::class.java]

        adapter = SkinAdapter(
            onEdit        = { index, skin -> showSkinDialog(index, skin) },
            onDelete      = { index -> vm.deleteSkin(index) },
            onStartDrag   = { holder -> touchHelper.startDrag(holder) }
        )

        // Drag-and-drop via long-press OR handle touch
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val fromPos = from.bindingAdapterPosition
                val toPos   = to.bindingAdapterPosition
                adapter.notifyItemMoved(fromPos, toPos)
                vm.reorderSkins(fromPos, toPos)
                return true
            }
            override fun onSwiped(holder: RecyclerView.ViewHolder, dir: Int) = Unit
            override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(holder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    holder?.itemView?.let {
                        it.animate().scaleX(1.05f).scaleY(1.05f).alpha(0.85f).translationZ(10f).setDuration(150).start()
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                }
            }
            override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
                super.clearView(rv, holder)
                holder.itemView.animate().scaleX(1f).scaleY(1f).alpha(1f).translationZ(0f).setDuration(150).start()
            }
        }
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.skinsRecyclerView)

        binding.skinsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.skinsRecyclerView.adapter = adapter

        vm.skins.observe(viewLifecycleOwner) {
            adapter.submitList(it.toList())
            binding.skinsRecyclerView.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            )
        }

        binding.fabAddSkin.setOnClickListener { showSkinDialog(-1, null) }
    }

    private fun showSkinDialog(index: Int, existing: Skin?) {
        val dialogBinding = DialogSkinEditorBinding.inflate(layoutInflater)
        existing?.let {
            dialogBinding.etSkinTitle.setText(it.skinTitle)
            dialogBinding.etSkinType.setText(it.skinType)
            dialogBinding.etSkinLogo.setText(it.skinLogo)
            dialogBinding.etZipUrl.setText(it.zipUrl)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add Skin" else "Edit Skin")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val skin = Skin(
                    skinTitle = dialogBinding.etSkinTitle.text.toString().trim(),
                    skinType  = dialogBinding.etSkinType.text.toString().trim(),
                    skinLogo  = dialogBinding.etSkinLogo.text.toString().trim(),
                    zipUrl    = dialogBinding.etZipUrl.text.toString().trim()
                )
                if (index == -1) vm.addSkin(skin) else vm.updateSkin(index, skin)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SkinAdapter(
    private val onEdit:      (Int, Skin) -> Unit,
    private val onDelete:    (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : androidx.recyclerview.widget.ListAdapter<Skin, SkinAdapter.SkinVH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Skin>() {
        override fun areItemsTheSame(a: Skin, b: Skin) = a.skinTitle == b.skinTitle
        override fun areContentsTheSame(a: Skin, b: Skin) = a == b
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SkinVH(ItemSkinBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SkinVH, position: Int) {
        holder.bind(getItem(position), position)
        // Staggered slide-in animation
        val anim = android.view.animation.AnimationUtils.loadAnimation(holder.itemView.context, com.heroconfigmanager.R.anim.slide_in_right)
        anim.startOffset = (position * 30L).coerceAtMost(200L)
        holder.itemView.startAnimation(anim)
    }

    inner class SkinVH(private val b: ItemSkinBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root) {

        fun bind(skin: Skin, index: Int) {
            b.tvSkinTitle.text  = skin.skinTitle
            b.chipSkinType.text = skin.skinType.ifBlank { "—" }
            Glide.with(b.root)
                .load(skin.skinLogo)
                .placeholder(R.color.surface_variant)
                .into(b.imgSkinLogo)

            b.btnEditSkin.setOnClickListener   { onEdit(index, skin) }
            b.btnDeleteSkin.setOnClickListener { onDelete(index) }

            // Drag starts on touch-down of the handle
            b.imgDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }
}
