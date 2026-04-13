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
import com.heroconfigmanager.data.model.AvailableSkin
import com.heroconfigmanager.data.model.Skin
import com.heroconfigmanager.data.model.UpgradeSkin
import com.heroconfigmanager.databinding.DialogUpgradeSkinEditorBinding
import com.heroconfigmanager.databinding.FragmentEditorSkinsBinding
import com.heroconfigmanager.databinding.ItemAvailableSkinBinding
import com.heroconfigmanager.databinding.ItemUpgradeSkinBinding

class UpgradeSkinsFragment : Fragment() {

    private var _binding: FragmentEditorSkinsBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: HeroEditorViewModel
    private lateinit var adapter: UpgradeSkinAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditorSkinsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(requireActivity())[HeroEditorViewModel::class.java]

        adapter = UpgradeSkinAdapter(
            onEdit      = { index, u -> showUpgradeDialog(index, u) },
            onDelete    = { index -> vm.deleteUpgradeSkin(index) },
            onStartDrag = { holder -> touchHelper.startDrag(holder) }
        )

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val fromPos = from.bindingAdapterPosition
                val toPos   = to.bindingAdapterPosition
                adapter.notifyItemMoved(fromPos, toPos)
                vm.reorderUpgradeSkins(fromPos, toPos)
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

        vm.upgradeSkins.observe(viewLifecycleOwner) { adapter.submitList(it.toList()) }

        binding.fabAddSkin.text = getString(R.string.add_upgrade_skin)
        binding.fabAddSkin.setOnClickListener { showUpgradeDialog(-1, null) }
    }

    private fun showUpgradeDialog(index: Int, existing: UpgradeSkin?) {
        // Mutable working copy of source skins so user can add/remove before saving
        val sourceSkins = (existing?.availableSkins?.toMutableList()) ?: mutableListOf()
        val dialogBinding = DialogUpgradeSkinEditorBinding.inflate(layoutInflater)
        var editingSourceIndex = -1

        // Pre-fill outer fields
        existing?.let {
            dialogBinding.etUpgradeSkinTitle.setText(it.skinTitle)
            dialogBinding.etUpgradeSkinType.setText(it.skinType)
            dialogBinding.etUpgradeSkinLogo.setText(it.skinLogo)
        }

        // Source skins inline adapter
        lateinit var sourceAdapter: AvailableSkinAdapter
        sourceAdapter = AvailableSkinAdapter(
            items     = sourceSkins,
            onEdit    = { pos, skin ->
                editingSourceIndex = pos
                dialogBinding.etSourceSkinTitle.setText(skin.skinTitle)
                dialogBinding.etSourceSkinType.setText(skin.skinType)
                dialogBinding.etSourceSkinLogo.setText(skin.skinLogo)
                dialogBinding.etSourceSkinZip.setText(skin.zipUrl)
                dialogBinding.btnConfirmSource.text = "Update"
                dialogBinding.cardAddSource.visibility = View.VISIBLE
                dialogBinding.btnAddSourceSkin.isEnabled = false
            },
            onRemove  = { pos ->
                sourceSkins.removeAt(pos)
                sourceAdapter.notifyItemRemoved(pos)
                updateSourceEmptyState(dialogBinding, sourceSkins)
            },
            onStartDrag = { /* long-press already handled by ItemTouchHelper */ }
        )

        dialogBinding.rvSourceSkins.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvSourceSkins.adapter = sourceAdapter

        // ItemTouchHelper for source skins inside the dialog
        val sourceCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val f = from.bindingAdapterPosition; val t = to.bindingAdapterPosition
                val item = sourceSkins.removeAt(f)
                sourceSkins.add(t, item)
                sourceAdapter.notifyItemMoved(f, t)
                return true
            }
            override fun onSwiped(h: RecyclerView.ViewHolder, d: Int) = Unit
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
        ItemTouchHelper(sourceCallback).attachToRecyclerView(dialogBinding.rvSourceSkins)

        updateSourceEmptyState(dialogBinding, sourceSkins)

        // Show/hide the inline add form
        dialogBinding.btnAddSourceSkin.setOnClickListener {
            editingSourceIndex = -1
            clearSourceForm(dialogBinding)
            dialogBinding.btnConfirmSource.text = "Add"
            dialogBinding.cardAddSource.visibility = View.VISIBLE
            dialogBinding.btnAddSourceSkin.isEnabled = false
        }
        dialogBinding.btnCancelSource.setOnClickListener {
            editingSourceIndex = -1
            clearSourceForm(dialogBinding)
            dialogBinding.cardAddSource.visibility = View.GONE
            dialogBinding.btnAddSourceSkin.isEnabled = true
        }
        dialogBinding.btnConfirmSource.setOnClickListener {
            val title = dialogBinding.etSourceSkinTitle.text.toString().trim()
            val type  = dialogBinding.etSourceSkinType.text.toString().trim()
            val logo  = dialogBinding.etSourceSkinLogo.text.toString().trim()
            val zip   = dialogBinding.etSourceSkinZip.text.toString().trim()
            if (title.isNotBlank()) {
                val skin = Skin(skinTitle = title, skinType = type, skinLogo = logo, zipUrl = zip)
                if (editingSourceIndex == -1) {
                    sourceSkins.add(skin)
                    sourceAdapter.notifyItemInserted(sourceSkins.lastIndex)
                } else {
                    sourceSkins[editingSourceIndex] = skin
                    sourceAdapter.notifyItemChanged(editingSourceIndex)
                    editingSourceIndex = -1
                }
                updateSourceEmptyState(dialogBinding, sourceSkins)
                clearSourceForm(dialogBinding)
                dialogBinding.cardAddSource.visibility = View.GONE
                dialogBinding.btnAddSourceSkin.isEnabled = true
            } else {
                dialogBinding.etSourceSkinTitle.error = "Title required"
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add Upgrade Skin" else "Edit Upgrade Skin")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val u = UpgradeSkin(
                    skinTitle      = dialogBinding.etUpgradeSkinTitle.text.toString().trim(),
                    skinType       = dialogBinding.etUpgradeSkinType.text.toString().trim(),
                    skinLogo       = dialogBinding.etUpgradeSkinLogo.text.toString().trim(),
                    availableSkins = sourceSkins.toList()
                )
                if (index == -1) vm.addUpgradeSkin(u) else vm.updateUpgradeSkin(index, u)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSourceEmptyState(b: DialogUpgradeSkinEditorBinding, list: List<*>) {
        b.tvSourceSkinsEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun clearSourceForm(b: DialogUpgradeSkinEditorBinding) {
        b.etSourceSkinTitle.text?.clear()
        b.etSourceSkinType.text?.clear()
        b.etSourceSkinLogo.text?.clear()
        b.etSourceSkinZip.text?.clear()
        b.etSourceSkinTitle.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── Adapters ──────────────────────────────────────────────────────────────────

class UpgradeSkinAdapter(
    private val onEdit:      (Int, UpgradeSkin) -> Unit,
    private val onDelete:    (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : androidx.recyclerview.widget.ListAdapter<UpgradeSkin, UpgradeSkinAdapter.VH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<UpgradeSkin>() {
        override fun areItemsTheSame(a: UpgradeSkin, b: UpgradeSkin) = a.skinTitle == b.skinTitle
        override fun areContentsTheSame(a: UpgradeSkin, b: UpgradeSkin) = a == b
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemUpgradeSkinBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position)
        // Staggered slide-in animation
        val anim = android.view.animation.AnimationUtils.loadAnimation(holder.itemView.context, com.heroconfigmanager.R.anim.slide_in_right)
        anim.startOffset = (position * 30L).coerceAtMost(200L)
        holder.itemView.startAnimation(anim)
    }

    inner class VH(private val b: ItemUpgradeSkinBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root) {

        fun bind(u: UpgradeSkin, index: Int) {
            b.tvUpgradeTitle.text  = u.skinTitle
            b.chipUpgradeType.text = u.skinType.ifBlank { "—" }
            b.chipSourceCount.text = "${u.availableSkins.size} sources"
            Glide.with(b.root)
                .load(u.skinLogo)
                .placeholder(R.color.surface_variant)
                .into(b.imgUpgradeLogo)

            b.btnEditUpgrade.setOnClickListener   { onEdit(index, u) }
            b.btnDeleteUpgrade.setOnClickListener { onDelete(index) }

            b.imgDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                false
            }
        }
    }
}

class AvailableSkinAdapter(
    private val items:       MutableList<AvailableSkin>,
    private val onEdit:      (Int, AvailableSkin) -> Unit,
    private val onRemove:    (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<AvailableSkinAdapter.AVH>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AVH(ItemAvailableSkinBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: AVH, position: Int) =
        holder.bind(items[position], position)

    inner class AVH(private val b: ItemAvailableSkinBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(skin: AvailableSkin, index: Int) {
            b.tvAvailableSkinTitle.text = skin.skinTitle
            b.tvAvailableSkinType.text  = skin.skinType.ifBlank { "—" }
            Glide.with(b.root)
                .load(skin.skinLogo)
                .placeholder(R.color.surface_variant)
                .into(b.imgAvailableSkinLogo)

            b.btnEditAvailableSkin.setOnClickListener { onEdit(index, skin) }
            b.btnRemoveAvailableSkin.setOnClickListener { onRemove(index) }

            b.imgSourceDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                false
            }
        }
    }
}
