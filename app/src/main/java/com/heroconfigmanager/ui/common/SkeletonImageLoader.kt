package com.heroconfigmanager.ui.common

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.faltenreich.skeletonlayout.SkeletonLayout
import com.heroconfigmanager.R

fun loadImageWithSkeleton(
    skeleton: SkeletonLayout,
    imageView: ImageView,
    model: Any?,
    centerCrop: Boolean = true,
) {
    val placeholder = ColorDrawable(
        ContextCompat.getColor(imageView.context, R.color.surface_variant)
    )

    Glide.with(imageView).clear(imageView)
    skeleton.showSkeleton()

    val data = when (model) {
        is String -> model.trim().takeIf { it.isNotEmpty() }
        else -> model
    }

    if (data == null) {
        imageView.setImageDrawable(placeholder)
        skeleton.showOriginal()
        return
    }

    var request: RequestBuilder<Drawable> = Glide.with(imageView)
        .load(data)
        .placeholder(placeholder)
        .error(placeholder)
        .dontAnimate()
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                skeleton.showOriginal()
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                skeleton.showOriginal()
                return false
            }
        })

    if (centerCrop) {
        request = request.centerCrop()
    }

    request.into(imageView)
}
