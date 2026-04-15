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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.Skin
import com.heroconfigmanager.databinding.DialogSkinEditorBinding
import com.heroconfigmanager.databinding.FragmentEditorSkinsBinding
import com.heroconfigmanager.databinding.ItemSkinBinding
import com.heroconfigmanager.ui.common.loadImageWithSkeleton

class SkinsFragment : Fragment() {

    private var _binding: FragmentEditorSkinsBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: HeroEditorViewModel
    private lateinit var adapter: SkinAdapter
    private lateinit var touchHelper: ItemTouchHelper
    private var hasAnimatedList = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditorSkinsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(requireActivity())[HeroEditorViewModel::class.java]

        adapter = SkinAdapter(
            onEdit = { index, skin -> showSkinDialog(index, skin) },
            onDelete = { index -> vm.deleteSkin(index) },
            onStartDrag = { holder -> touchHelper.startDrag(holder) }
        )

        val callback = ReorderItemTouchHelperCallback(
            onMoveItem = { fromPos, toPos -> adapter.moveItem(fromPos, toPos) },
            onDragReleased = { vm.setSkins(adapter.snapshot()) }
        )
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.skinsRecyclerView)

        binding.skinsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.skinsRecyclerView.adapter = adapter

        vm.skins.observe(viewLifecycleOwner) { skins ->
            adapter.replaceItems(skins.toList())
            if (!hasAnimatedList) {
                hasAnimatedList = true
                binding.skinsRecyclerView.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                )
            }
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
                    skinType = dialogBinding.etSkinType.text.toString().trim(),
                    skinLogo = dialogBinding.etSkinLogo.text.toString().trim(),
                    zipUrl = dialogBinding.etZipUrl.text.toString().trim()
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
    private val onEdit: (Int, Skin) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<SkinAdapter.SkinVH>() {

    private val items = mutableListOf<Skin>()

    fun replaceItems(updated: List<Skin>) {
        items.clear()
        items.addAll(updated)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int): Boolean {
        if (from !in items.indices || to !in items.indices || from == to) {
            return false
        }
        val moved = items.removeAt(from)
        items.add(to, moved)
        notifyItemMoved(from, to)
        return true
    }

    fun snapshot(): List<Skin> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SkinVH(ItemSkinBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SkinVH, position: Int) {
        holder.bind(items[position])
        val anim = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_right)
        anim.startOffset = (position * 30L).coerceAtMost(200L)
        holder.itemView.startAnimation(anim)
    }

    inner class SkinVH(private val binding: ItemSkinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(skin: Skin) {
            binding.tvSkinTitle.text = skin.skinTitle
            binding.chipSkinType.text = skin.skinType.ifBlank { "-" }
            loadImageWithSkeleton(binding.skeletonSkinLogo, binding.imgSkinLogo, skin.skinLogo)

            binding.btnEditSkin.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEdit(position, items[position])
                }
            }
            binding.btnDeleteSkin.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDelete(position)
                }
            }
            binding.imgDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN &&
                    bindingAdapterPosition != RecyclerView.NO_POSITION
                ) {
                    onStartDrag(this)
                    return@setOnTouchListener true
                }
                false
            }
        }
    }
}
