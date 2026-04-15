package com.heroconfigmanager.ui.hero

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.AvailableSkin
import com.heroconfigmanager.data.model.Skin
import com.heroconfigmanager.data.model.UpgradeSkin
import com.heroconfigmanager.databinding.DialogUpgradeSkinEditorBinding
import com.heroconfigmanager.databinding.FragmentEditorSkinsBinding
import com.heroconfigmanager.databinding.ItemAvailableSkinBinding
import com.heroconfigmanager.databinding.ItemUpgradeSkinBinding
import com.heroconfigmanager.ui.common.loadImageWithSkeleton

class UpgradeSkinsFragment : Fragment() {

    private var _binding: FragmentEditorSkinsBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: HeroEditorViewModel
    private lateinit var adapter: UpgradeSkinAdapter
    private lateinit var touchHelper: ItemTouchHelper

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

        adapter = UpgradeSkinAdapter(
            onEdit = { index, item -> showUpgradeSheet(index, item) },
            onDelete = { index -> vm.deleteUpgradeSkin(index) },
            onStartDrag = { holder -> touchHelper.startDrag(holder) }
        )

        val callback = ReorderItemTouchHelperCallback(
            onMoveItem = { fromPos, toPos -> adapter.moveItem(fromPos, toPos) },
            onDragReleased = { vm.setUpgradeSkins(adapter.snapshot()) }
        )

        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.skinsRecyclerView)

        binding.skinsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.skinsRecyclerView.adapter = adapter

        vm.upgradeSkins.observe(viewLifecycleOwner) { adapter.replaceItems(it.toList()) }

        binding.fabAddSkin.text = getString(R.string.add_upgrade_skin)
        binding.fabAddSkin.setOnClickListener { showUpgradeSheet(-1, null) }
    }

    @Suppress("DEPRECATION")
    private fun showUpgradeSheet(index: Int, existing: UpgradeSkin?) {
        val sourceSkins = existing?.availableSkins?.toMutableList() ?: mutableListOf()
        val sheetBinding = DialogUpgradeSkinEditorBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext(), R.style.ThemeOverlay_HeroConfig_BottomSheetDialog)
        var editingSourceIndex = -1

        dialog.setContentView(sheetBinding.root)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setOnShowListener {
            dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { bottomSheet ->
                    bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    dialog.behavior.isDraggable = true
                    dialog.behavior.expandedOffset = 0
                    dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
        }

        sheetBinding.tvUpgradeSheetTitle.text = getString(
            if (existing == null) R.string.add_upgrade_skin else R.string.edit_upgrade_skin
        )

        existing?.let {
            sheetBinding.etUpgradeSkinTitle.setText(it.skinTitle)
            sheetBinding.etUpgradeSkinType.setText(it.skinType)
            sheetBinding.etUpgradeSkinLogo.setText(it.skinLogo)
        }

        lateinit var sourceAdapter: AvailableSkinAdapter
        lateinit var sourceTouchHelper: ItemTouchHelper
        sourceAdapter = AvailableSkinAdapter(
            items = sourceSkins,
            onEdit = { pos, skin ->
                editingSourceIndex = pos
                sheetBinding.etSourceSkinTitle.setText(skin.skinTitle)
                sheetBinding.etSourceSkinType.setText(skin.skinType)
                sheetBinding.etSourceSkinLogo.setText(skin.skinLogo)
                sheetBinding.etSourceSkinZip.setText(skin.zipUrl)
                sheetBinding.btnConfirmSource.text = getString(R.string.save)
                showSourceEditor(sheetBinding)
            },
            onRemove = { pos ->
                sourceSkins.removeAt(pos)
                sourceAdapter.notifyItemRemoved(pos)
                updateSourceSectionState(sheetBinding, sourceSkins)
            },
            onStartDrag = { holder -> sourceTouchHelper.startDrag(holder) }
        )

        sheetBinding.rvSourceSkins.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.rvSourceSkins.adapter = sourceAdapter

        val sourceCallback = ReorderItemTouchHelperCallback(
            onMoveItem = { fromPos, toPos ->
                if (fromPos !in sourceSkins.indices || toPos !in sourceSkins.indices || fromPos == toPos) {
                    false
                } else {
                    val item = sourceSkins.removeAt(fromPos)
                    sourceSkins.add(toPos, item)
                    sourceAdapter.notifyItemMoved(fromPos, toPos)
                    true
                }
            }
        )
        sourceTouchHelper = ItemTouchHelper(sourceCallback)
        sourceTouchHelper.attachToRecyclerView(sheetBinding.rvSourceSkins)

        updateSourceSectionState(sheetBinding, sourceSkins)

        sheetBinding.btnAddSourceSkin.setOnClickListener {
            editingSourceIndex = -1
            clearSourceForm(sheetBinding)
            sheetBinding.btnConfirmSource.text = getString(R.string.add)
            showSourceEditor(sheetBinding)
        }

        sheetBinding.btnCancelSource.setOnClickListener {
            editingSourceIndex = -1
            clearSourceForm(sheetBinding)
            hideSourceEditor(sheetBinding)
        }

        sheetBinding.btnConfirmSource.setOnClickListener {
            val title = sheetBinding.etSourceSkinTitle.text.toString().trim()
            val type = sheetBinding.etSourceSkinType.text.toString().trim()
            val logo = sheetBinding.etSourceSkinLogo.text.toString().trim()
            val zip = sheetBinding.etSourceSkinZip.text.toString().trim()

            if (title.isBlank()) {
                sheetBinding.etSourceSkinTitle.error = "Title required"
                return@setOnClickListener
            }

            val skin = Skin(skinTitle = title, skinType = type, skinLogo = logo, zipUrl = zip)
            if (editingSourceIndex == -1) {
                sourceSkins.add(skin)
                sourceAdapter.notifyItemInserted(sourceSkins.lastIndex)
            } else {
                sourceSkins[editingSourceIndex] = skin
                sourceAdapter.notifyItemChanged(editingSourceIndex)
                editingSourceIndex = -1
            }
            updateSourceSectionState(sheetBinding, sourceSkins)
            clearSourceForm(sheetBinding)
            hideSourceEditor(sheetBinding)
        }

        sheetBinding.btnCloseUpgradeSheet.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnCancelUpgradeSheet.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnSaveUpgradeSheet.setOnClickListener {
            val title = sheetBinding.etUpgradeSkinTitle.text.toString().trim()
            if (title.isBlank()) {
                sheetBinding.etUpgradeSkinTitle.error = "Title required"
                return@setOnClickListener
            }

            val item = UpgradeSkin(
                skinTitle = title,
                skinType = sheetBinding.etUpgradeSkinType.text.toString().trim(),
                skinLogo = sheetBinding.etUpgradeSkinLogo.text.toString().trim(),
                availableSkins = sourceSkins.toList()
            )
            if (index == -1) vm.addUpgradeSkin(item) else vm.updateUpgradeSkin(index, item)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateSourceSectionState(
        binding: DialogUpgradeSkinEditorBinding,
        list: List<*>,
    ) {
        binding.tvSourceSkinsEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.chipSourceCountSummary.text = binding.root.context.getString(
            R.string.sources_count_format,
            list.size
        )
    }

    private fun showSourceEditor(binding: DialogUpgradeSkinEditorBinding) {
        binding.cardAddSource.visibility = View.VISIBLE
        binding.btnAddSourceSkin.isEnabled = false
        binding.sheetScroll.post {
            binding.cardAddSource.doOnLayout {
                val extraOffset = binding.root.resources.getDimensionPixelSize(R.dimen.field_spacing) * 3
                val targetScroll = (
                    binding.cardAddSource.bottom -
                        binding.sheetScroll.height +
                        binding.sheetScroll.paddingBottom +
                        extraOffset
                    ).coerceAtLeast(binding.cardAddSource.top - extraOffset)
                binding.sheetScroll.smoothScrollTo(0, targetScroll)
            }
        }
    }

    private fun hideSourceEditor(binding: DialogUpgradeSkinEditorBinding) {
        binding.cardAddSource.visibility = View.GONE
        binding.btnAddSourceSkin.isEnabled = true
    }

    private fun clearSourceForm(binding: DialogUpgradeSkinEditorBinding) {
        binding.etSourceSkinTitle.text?.clear()
        binding.etSourceSkinType.text?.clear()
        binding.etSourceSkinLogo.text?.clear()
        binding.etSourceSkinZip.text?.clear()
        binding.etSourceSkinTitle.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class UpgradeSkinAdapter(
    private val onEdit: (Int, UpgradeSkin) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<UpgradeSkinAdapter.VH>() {

    private val items = mutableListOf<UpgradeSkin>()

    fun replaceItems(updated: List<UpgradeSkin>) {
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

    fun snapshot(): List<UpgradeSkin> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemUpgradeSkinBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        val anim = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_right)
        anim.startOffset = (position * 30L).coerceAtMost(200L)
        holder.itemView.startAnimation(anim)
    }

    inner class VH(private val binding: ItemUpgradeSkinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UpgradeSkin) {
            binding.tvUpgradeTitle.text = item.skinTitle
            binding.chipUpgradeType.text = item.skinType.ifBlank { "Unlabeled" }
            binding.chipSourceCount.text = binding.root.context.getString(
                R.string.sources_count_format,
                item.availableSkins.size
            )
            binding.tvUpgradePreview.text = if (item.availableSkins.isEmpty()) {
                binding.root.context.getString(R.string.upgrade_sources_preview_empty)
            } else {
                val preview = item.availableSkins
                    .take(3)
                    .joinToString(", ") { source -> source.skinTitle.ifBlank { "Untitled" } }
                binding.root.context.getString(R.string.upgrade_sources_preview, preview)
            }

            loadImageWithSkeleton(
                binding.skeletonUpgradeLogo,
                binding.imgUpgradeLogo,
                item.skinLogo
            )

            binding.btnEditUpgrade.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEdit(position, items[position])
                }
            }
            binding.btnDeleteUpgrade.setOnClickListener {
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

class AvailableSkinAdapter(
    private val items: MutableList<AvailableSkin>,
    private val onEdit: (Int, AvailableSkin) -> Unit,
    private val onRemove: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<AvailableSkinAdapter.AVH>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AVH(ItemAvailableSkinBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: AVH, position: Int) {
        holder.bind(items[position])
    }

    inner class AVH(private val binding: ItemAvailableSkinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AvailableSkin) {
            binding.tvAvailableSkinTitle.text = item.skinTitle
            binding.chipAvailableSkinType.text = item.skinType.ifBlank { "Unlabeled" }
            binding.chipAvailableZip.text = if (item.zipUrl.isBlank()) {
                binding.root.context.getString(R.string.source_zip_missing)
            } else {
                binding.root.context.getString(R.string.source_zip_ready)
            }
            binding.tvAvailableSkinMeta.text = item.zipUrl.toReadableHost(
                binding.root.context.getString(R.string.source_host_unknown)
            )

            loadImageWithSkeleton(
                binding.skeletonAvailableSkinLogo,
                binding.imgAvailableSkinLogo,
                item.skinLogo
            )

            binding.btnEditAvailableSkin.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEdit(position, items[position])
                }
            }
            binding.btnRemoveAvailableSkin.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemove(position)
                }
            }
            binding.imgSourceDragHandle.setOnTouchListener { _, event ->
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

private fun String.toReadableHost(fallback: String): String {
    return runCatching {
        Uri.parse(this).host
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: fallback
}
