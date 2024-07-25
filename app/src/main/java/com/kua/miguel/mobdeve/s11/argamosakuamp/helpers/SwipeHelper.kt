package com.kua.miguel.mobdeve.s11.argamosakuamp.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.LinkedList

abstract class SwipeHelper(
    private val recyclerView: RecyclerView
) : ItemTouchHelper.SimpleCallback(
    0, // Change this to 0 to disable dragging
    ItemTouchHelper.LEFT
) {
    private var swipedPosition = -1
    private val buttonsBuffer: MutableMap<Int, List<UnderlayButton>> = mutableMapOf()
    private val recoverQueue = LinkedList<Int>()
    private var swipeThreshold = 1.8f // Change button size

    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { _, event ->
        if (swipedPosition < 0) return@OnTouchListener false
        buttonsBuffer[swipedPosition]?.forEach { it.handle(event) }
        if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
            recoverQueue.add(swipedPosition)
            swipedPosition = -1
            recoverSwipedItem()
        }
        true
    }

    init {
        recyclerView.setOnTouchListener(touchListener)
    }

    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val position = recoverQueue.poll() ?: return
            recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    private fun drawButtons(
        canvas: Canvas,
        itemView: View,
        buttons: List<UnderlayButton>,
        dX: Float
    ) {
        var right = itemView.right.toFloat()
        val dButtonWidth = (-1) * dX / buttons.size // Calculate button width based on swipe distance

        for (button in buttons) {
            val left = right - dButtonWidth
            button.draw(
                canvas,
                RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat())
            )

            right = left
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val position = viewHolder.adapterPosition
        var translationX = dX
        val itemView = viewHolder.itemView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                if (!buttonsBuffer.containsKey(position)) {
                    buttonsBuffer[position] = instantiateUnderlayButton(position)
                }

                val buttons = buttonsBuffer[position] ?: return
                if (buttons.isEmpty()) return

                val maxDX = swipeThreshold * buttons.size * buttons[0].width
                translationX = Math.max(-maxDX, dX)
                drawButtons(c, itemView, buttons, translationX)
            }
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            translationX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (swipedPosition != position) recoverQueue.add(swipedPosition)
        swipedPosition = position
        recoverSwipedItem()
    }

    abstract fun instantiateUnderlayButton(position: Int): List<UnderlayButton>

    interface UnderlayButtonClickListener {
        fun onClick()
    }

    class UnderlayButton(
        private val context: Context,
        @DrawableRes private val drawableRes: Int,
        textSize: Float,
        @ColorRes private val colorRes: Int,
        private val clickListener: UnderlayButtonClickListener
    ) {
        private val paint = Paint()
        private var clickableRegion: RectF? = null
        private val textSizeInPixel: Float = textSize * context.resources.displayMetrics.density // dp to px
        private val horizontalPadding = 50.0f
        val width: Float
        private var lastClickTime: Long = 0L
        private val CLICK_DEBOUNCE_DELAY = 300L // Delay in milliseconds

        init {
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT
            val titleBounds = Rect()
            paint.getTextBounds("", 0, 0, titleBounds)
            width = titleBounds.width().toFloat() + 2 * horizontalPadding
        }

        fun draw(canvas: Canvas, rect: RectF) {
            // Draw background
            paint.color = ContextCompat.getColor(context, colorRes)
            canvas.drawRect(rect, paint)

            // Draw image
            val drawable = ContextCompat.getDrawable(context, drawableRes)
            drawable?.let {
                val iconSize = Math.min(rect.height(), rect.width() / 2)
                val left = rect.left + (rect.width() - iconSize) / 2
                val top = rect.top + (rect.height() - iconSize) / 2
                it.setBounds(left.toInt(), top.toInt(), (left + iconSize).toInt(), (top + iconSize).toInt())
                it.draw(canvas)
            }

            clickableRegion = rect
        }

        fun handle(event: MotionEvent) {
            clickableRegion?.let {
                if (event.action == MotionEvent.ACTION_UP && it.contains(event.x, event.y)) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > CLICK_DEBOUNCE_DELAY) {
                        lastClickTime = currentTime
                        clickListener.onClick()
                    }
                }
            }
        }
    }
}