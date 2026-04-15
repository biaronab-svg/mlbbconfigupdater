package com.heroconfigmanager.ui.hero

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.heroconfigmanager.R
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DEFAULT_DRAG_HANDLE_ALPHA = 0.55f
private const val ACTIVE_DRAG_HANDLE_ALPHA = 1f
private const val DRAG_SCALE = 1.04f
private const val DRAG_ALPHA = 0.9f
private const val DRAG_TRANSLATION_Z = 12f

class ReorderItemTouchHelperCallback(
    private val onMoveItem: (from: Int, to: Int) -> Boolean,
    private val onDragReleased: () -> Unit = {},
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    override fun isLongPressDragEnabled(): Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION || from == to) {
            return false
        }
        return onMoveItem(from, to)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean = current.itemViewType == target.itemViewType

    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.08f

    override fun chooseDropTarget(
        selected: RecyclerView.ViewHolder,
        dropTargets: MutableList<RecyclerView.ViewHolder>,
        curX: Int,
        curY: Int,
    ): RecyclerView.ViewHolder? {
        val defaultTarget = super.chooseDropTarget(selected, dropTargets, curX, curY)
        if (defaultTarget != null || dropTargets.isEmpty()) {
            return defaultTarget
        }

        val selectedTop = selected.itemView.top + curY
        val selectedBottom = selected.itemView.bottom + curY
        val firstTarget = dropTargets.minByOrNull { it.itemView.top }
        val lastTarget = dropTargets.maxByOrNull { it.itemView.bottom }

        return when {
            firstTarget != null && selectedTop <= firstTarget.itemView.top -> firstTarget
            lastTarget != null && selectedBottom >= lastTarget.itemView.bottom -> lastTarget
            else -> null
        }
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder?.itemView?.let { itemView ->
                itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                setDraggingState(itemView, true)
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        setDraggingState(viewHolder.itemView, false)
        onDragReleased()
    }

    override fun interpolateOutOfBoundsScroll(
        recyclerView: RecyclerView,
        viewSize: Int,
        viewSizeOutOfBounds: Int,
        totalSize: Int,
        msSinceStartScroll: Long,
    ): Int {
        if (viewSizeOutOfBounds == 0) {
            return 0
        }

        val direction = if (viewSizeOutOfBounds > 0) 1 else -1
        val distanceRatio = (abs(viewSizeOutOfBounds).toFloat() / viewSize.coerceAtLeast(1))
            .coerceIn(0f, 1f)
        val timeRatio = (msSinceStartScroll / 500f).coerceIn(0f, 1f)
        val density = recyclerView.resources.displayMetrics.density
        val speed = (10f + (34f * distanceRatio) + (22f * timeRatio)) * density
        return (direction * speed).roundToInt()
    }

    private fun setDraggingState(itemView: View, isDragging: Boolean) {
        itemView.animate().cancel()
        itemView.animate()
            .scaleX(if (isDragging) DRAG_SCALE else 1f)
            .scaleY(if (isDragging) DRAG_SCALE else 1f)
            .alpha(if (isDragging) DRAG_ALPHA else 1f)
            .translationZ(if (isDragging) DRAG_TRANSLATION_Z else 0f)
            .setDuration(140L)
            .start()

        itemView.findViewById<View?>(R.id.imgDragHandle)?.animate()?.alpha(
            if (isDragging) ACTIVE_DRAG_HANDLE_ALPHA else DEFAULT_DRAG_HANDLE_ALPHA
        )?.setDuration(120L)?.start()

        itemView.findViewById<View?>(R.id.imgSourceDragHandle)?.animate()?.alpha(
            if (isDragging) ACTIVE_DRAG_HANDLE_ALPHA else DEFAULT_DRAG_HANDLE_ALPHA
        )?.setDuration(120L)?.start()
    }
}
