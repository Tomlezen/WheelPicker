package com.tlz.wheelpicker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller

/**
 * Created by Tomlezen.
 * Date: 2017/8/20.
 * Time: 下午2:36.
 */
class WheelPickerView constructor(val ctx: Context, val attrs: AttributeSet) : View(ctx, attrs) {

    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val paintMask = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.LINEAR_TEXT_FLAG)
    private val paintUnit = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.LINEAR_TEXT_FLAG)

    private val rectDrawn = Rect()
    private val rectIndicatorHead = Rect()
    private val rectIndicatorFoot = Rect()
    private val rectCurrentItem = Rect()

    private var unit: String? = null
        set(value) {
            field = value
            if (isInitFinish) {
                computeTextSize()
                requestLayout()
                invalidate()
            }
        }
    private var unitTextColor: Int = 0
        set(value) {
            field = value
            if (isInitFinish) {
                invalidate()
            }
        }
    private var unitTextSize: Int = 0
        set(value) {
            field = value
            if (isInitFinish) {
                computeTextSize()
                requestLayout()
                invalidate()
            }
        }
    private var unitSpace: Int = 0
    private var unitTextWidth: Int = 0
    private var visibleItemCount: Int = 0
        set(count) {
            field = count
            if (isInitFinish) {
                updateVisibleItemCount()
                requestLayout()
            }
        }
    private var drawnItemCount: Int = 0
    private var halfDrawnItemCount: Int = 0
    private var textMaxWidth: Int = 0
    private var textMaxHeight: Int = 0
    private var itemTextColor: Int = 0
        set(color) {
            field = color
            if (isInitFinish) {
                invalidate()
            }
        }
    private var selectedItemTextColor: Int = 0
        set(color) {
            field = color
            if (isInitFinish) {
                computeCurrentItemRect()
                invalidate()
            }
        }
    private var itemTextSize: Int = 0
        set(size) {
            field = size
            paint.textSize = size.toFloat()
            if (isInitFinish) {
                computeTextSize()
                requestLayout()
                invalidate()
            }
        }
    private var itemSelectedTextSize: Int = 0
    private var indicatorSize: Int = 0
        set(size) {
            field = size
            if (isInitFinish) {
                computeIndicatorRect()
                invalidate()
            }
        }
    private var indicatorColor: Int = 0
        set(color) {
            field = color
            if (isInitFinish) {
                invalidate()
            }
        }
    private val maskHeight: Int
    private var itemSpace: Int = 0
        set(space) {
            field = space
            if (isInitFinish) {
                requestLayout()
                invalidate()
            }
        }
    private var itemAlign: Int = 0
        set(align) {
            field = align
            if (isInitFinish) {
                updateItemTextAlign()
                computeDrawnCenter()
                invalidate()
            }
        }
    private var itemHeight: Int = 0
    private var halfItemHeight: Int = 0
    private var halfWheelHeight: Int = 0
    private var selectedItemPosition: Int = 0
    private var currentItemPosition: Int = 0
    private var minFlingY: Int = 0
    private var maxFlingY: Int = 0
    private var minimumVelocity = 50
    private var maximumVelocity = 8000
    private var wheelCenterX: Int = 0
    private var wheelCenterY: Int = 0
    private var drawnCenterX: Int = 0
    private var drawnUnitCenterX: Int = 0
    private var drawnCenterY: Int = 0
    private var scrollOffsetY: Int = 0
    private var lastPointY: Int = 0
    private var downPointY: Int = 0
    private var touchSlop = 8
    private var hasIndicator: Boolean = false
    private var hasAtmospheric: Boolean = false

    private var isClickAction: Boolean = false
    private var isForceFinishScroll: Boolean = false

    private val backgroundColor: Int
    private val backgroundOfSelectedItem: Int

//    private var camera = Camera()
//    private var textMatrix = Matrix()
    private val scroller = Scroller(ctx)
    private var tracker: VelocityTracker? = null
    private var adapter = Adapter()
    private var onItemSelectedListener: OnItemSelectedListener? = null

    private var isInitFinish = false

    private val runnable = object : Runnable {
        override fun run() {
            val itemCount = adapter.itemCount
            if (!isShown || itemCount == 0) {
                return
            }
            if (scroller.isFinished && !isForceFinishScroll) {
                if (itemHeight == 0) return
                var position = (-scrollOffsetY / itemHeight + selectedItemPosition) % itemCount
                position = if (position < 0) position + itemCount else position
                currentItemPosition = position
                onItemSelected()
            }
            if (scroller.computeScrollOffset()) {
                scrollOffsetY = scroller.currY

                val position = (-scrollOffsetY / itemHeight + selectedItemPosition) % itemCount
                onItemSelectedListener?.onCurrentItemOfScroll(this@WheelPickerView, position)

                postInvalidate()
                handler.postDelayed(this, 16)
            }
        }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.WheelPickerView)

        unit = ta.getString(R.styleable.WheelPickerView_scroll_unit)
        unitTextColor = ta.getColor(R.styleable.WheelPickerView_scroll_unit_text_color, Color.parseColor("#b0424242"))
        unitTextSize = ta.getDimensionPixelSize(R.styleable.WheelPickerView_scroll_unit_text_Size, resources.getDimensionPixelSize(R.dimen.wheel_unit_text_size))
        unitSpace = ta.getDimensionPixelSize(R.styleable.WheelPickerView_scroll_unit_space, resources.getDimensionPixelSize(R.dimen.wheel_unit_space))
        itemTextSize = ta.getDimensionPixelSize(R.styleable.WheelPickerView_scroll_item_text_size, resources.getDimensionPixelSize(R.dimen.wheel_item_text_size))
        itemSelectedTextSize = ta.getDimensionPixelSize(R.styleable.WheelPickerView_scroll_item_selected_text_size, resources.getDimensionPixelSize(R.dimen.wheel_item_selected_text_size))
        visibleItemCount = ta.getInt(R.styleable.WheelPickerView_scroll_visible_item_count, 5)
        selectedItemPosition = ta.getInt(R.styleable.WheelPickerView_scroll_selected_item_position, 0)
        selectedItemTextColor = ta.getColor(R.styleable.WheelPickerView_scroll_selected_item_text_color, -1)
        itemTextColor = ta.getColor(R.styleable.WheelPickerView_scroll_item_text_color, Color.parseColor("#b0424242"))
        backgroundColor = ta.getColor(R.styleable.WheelPickerView_scroll_background_color, Color.parseColor("#FFF5F5F5"))
        backgroundOfSelectedItem = ta.getColor(R.styleable.WheelPickerView_scroll_selected_item_background, Color.parseColor("#FFFFFFFF"))
        itemSpace = ta.getDimensionPixelSize(R.styleable.WheelPickerView_scroll_item_space, resources.getDimensionPixelSize(R.dimen.wheel_item_space))
        hasIndicator = ta.getBoolean(R.styleable.WheelPickerView_scroll_indicator, false)
        indicatorColor = ta.getColor(R.styleable.WheelPickerView_scroll_indicator_color, 0xFFDDDDDD.toInt())
        indicatorSize = ta.getDimensionPixelSize(R.styleable.WheelPickerView_scroll_indicator_size, resources.getDimensionPixelSize(R.dimen.wheel_indicator_size))
        hasAtmospheric = ta.getBoolean(R.styleable.WheelPickerView_scroll_atmospheric, false)
        maskHeight = ta.getDimensionPixelSize(R.styleable.WheelPickerView_scroll_mask_height, resources.getDimensionPixelSize(R.dimen.wheel_mask_height))
        itemAlign = ta.getInt(R.styleable.WheelPickerView_scroll_item_align, ALIGN_CENTER)
        ta.recycle()

        paint.textSize = itemTextSize.toFloat()
        paintUnit.color = unitTextColor
        paintUnit.textSize = unitTextSize.toFloat()
        paintUnit.textAlign = Paint.Align.CENTER
        val conf = ViewConfiguration.get(context)
        minimumVelocity = conf.scaledMinimumFlingVelocity
        maximumVelocity = conf.scaledMaximumFlingVelocity
        touchSlop = conf.scaledTouchSlop

        updateVisibleItemCount()
        updateItemTextAlign()
        computeTextSize()

        isClickable = true
    }

    private fun updateVisibleItemCount() {
        if (visibleItemCount < 2) {
            throw ArithmeticException("Wheel's visible item count can not be less than 2!")
        }
        if (visibleItemCount % 2 == 0) {
            visibleItemCount += 1
        }
        drawnItemCount = visibleItemCount + 2
        halfDrawnItemCount = drawnItemCount / 2
    }

    private fun updateItemTextAlign() {
        paint.textAlign = when (itemAlign) {
            ALIGN_LEFT -> Paint.Align.LEFT
            ALIGN_RIGHT -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
    }

    private fun computeTextSize() {
        textMaxHeight = 0
        textMaxWidth = textMaxHeight

        (0..adapter.itemCount - 1)
                .asSequence()
                .map { adapter.getItemText(it) }
                .map { paint.measureText(it).toInt() }
                .forEach { textMaxWidth = Math.max(textMaxWidth, it) }
        if (!TextUtils.isEmpty(unit)) {
            paint.textSize = unitTextSize.toFloat()
            unitTextWidth = paint.measureText(unit).toInt()
            textMaxWidth += unitSpace + unitTextWidth
        }
        val metrics = paint.fontMetrics
        textMaxHeight = (metrics.bottom - metrics.top).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val modeWidth = View.MeasureSpec.getMode(widthMeasureSpec)
        val modeHeight = View.MeasureSpec.getMode(heightMeasureSpec)

        val sizeWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        val sizeHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        var resultWidth = textMaxWidth
        var resultHeight = textMaxHeight * visibleItemCount + itemSpace * (visibleItemCount - 1)

        resultWidth += paddingLeft + paddingRight
        resultHeight += paddingTop + paddingBottom

        resultWidth = measureSize(modeWidth, sizeWidth, resultWidth)
        resultHeight = measureSize(modeHeight, sizeHeight, resultHeight)

        setMeasuredDimension(resultWidth, resultHeight)
    }

    private fun measureSize(mode: Int, sizeExpect: Int, sizeActual: Int): Int {
        var realSize: Int
        if (mode == View.MeasureSpec.EXACTLY) {
            realSize = sizeExpect
        } else {
            realSize = sizeActual
            if (mode == View.MeasureSpec.AT_MOST) {
                realSize = Math.min(realSize, sizeExpect)
            }
        }
        return realSize
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectDrawn.set(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)

        wheelCenterX = rectDrawn.centerX()
        wheelCenterY = rectDrawn.centerY()
        halfWheelHeight = rectDrawn.height() / 2
        itemHeight = rectDrawn.height() / visibleItemCount
        halfItemHeight = itemHeight / 2

//        topLg = LinearGradient(0f, 0f, 0f, maskHeight.toFloat(), 0x00f2f2f2, 0xf2f2f2,
//                Shader.TileMode.MIRROR)
//
//        bottomLg = LinearGradient(0f, (rectDrawn.height() - maskHeight).toFloat(), 0f,
//                rectDrawn.height().toFloat(), 0x00f2f2f2, 0xf2f2f2, Shader.TileMode.MIRROR)

        computeDrawnCenter()
        computeFlingLimitY()
        computeIndicatorRect()
        computeCurrentItemRect()
    }

    private fun computeDrawnCenter() {
        drawnCenterX = when (itemAlign) {
            ALIGN_LEFT -> {
                drawnUnitCenterX = rectDrawn.left + (textMaxWidth - unitTextWidth / 2)
                rectDrawn.left
            }
            ALIGN_RIGHT -> {
                if(unit.isNullOrEmpty()){
                    rectDrawn.right
                }else{
                    drawnUnitCenterX = rectDrawn.right - unitTextWidth
                    rectDrawn.right - unitSpace - unitTextWidth
                }
            }
            else -> {
                drawnUnitCenterX = (textMaxWidth - unitSpace - unitTextWidth) / 2 + unitTextWidth / 2 + unitSpace
                wheelCenterX
            }
        }
        drawnCenterY = (wheelCenterY - (paint.ascent() + paint.descent()) / 2).toInt()
    }

    private fun computeFlingLimitY() {
        val currentItemOffset = selectedItemPosition * itemHeight
        minFlingY = -itemHeight * (adapter.itemCount - 1) + currentItemOffset
        maxFlingY = currentItemOffset
    }

    private fun computeIndicatorRect() {
        if (hasIndicator) {
            val halfIndicatorSize = indicatorSize / 2
            val indicatorHeadCenterY = wheelCenterY + halfItemHeight
            val indicatorFootCenterY = wheelCenterY - halfItemHeight
            rectIndicatorHead.set(rectDrawn.left, indicatorHeadCenterY - halfIndicatorSize, rectDrawn.right, indicatorHeadCenterY + halfIndicatorSize)
            rectIndicatorFoot.set(rectDrawn.left, indicatorFootCenterY - halfIndicatorSize, rectDrawn.right, indicatorFootCenterY + halfIndicatorSize)
        }
    }

    private fun computeCurrentItemRect() {
        if (selectedItemTextColor != -1){
            rectCurrentItem.set(rectDrawn.left, wheelCenterY - halfItemHeight, rectDrawn.right, wheelCenterY + halfItemHeight)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            //绘制整体背景
            paintBg.style = Paint.Style.FILL
            paintBg.color = backgroundColor
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paintBg)

            //绘制选择区域背景
            paintBg.color = backgroundOfSelectedItem
            canvas.drawRect(rectCurrentItem, paintBg)

            val startPos = -scrollOffsetY / itemHeight - halfDrawnItemCount
            val drawnPos = startPos + selectedItemPosition
            var drawnOffsetPos = -halfDrawnItemCount
            (0..drawnItemCount - 1).map { it + drawnPos }.forEach {
                if(isPosInRang(it)){
                    val data = adapter.getItemText(it)
                    val centerY = drawnCenterY + drawnOffsetPos * itemHeight + scrollOffsetY % itemHeight + rectIndicatorHead.height() * 2f

//                canvas?.save()
//                canvas?.getMatrix(textMatrix)
//                camera.save()
//                camera.rotateX((drawnCenterY - centerY) / itemHeight * 20f)
//                camera.getMatrix(textMatrix)
//                textMatrix.preTranslate(-drawnCenterX.toFloat(), -centerY)
//                textMatrix.postTranslate(drawnCenterX.toFloat(), centerY)
//                camera.restore()
//                canvas?.concat(textMatrix)
//                canvas?.restore()
                    paint.color = itemTextColor
                    paint.textSize = itemTextSize.toFloat()
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    if(hasAtmospheric){
                        val alpha = ((drawnCenterY - Math.abs(drawnCenterY - centerY)) * 1.0f / drawnCenterY * 255).toInt()
                        paint.alpha = if (alpha < 0) 0 else alpha
                    }

                    if (selectedItemTextColor != -1) {
                        canvas.save()
                        canvas.clipRect(0, 0, width, rectCurrentItem.top)
                        canvas.drawText(data, drawnCenterX.toFloat(), centerY, paint)
                        canvas.restore()

                        canvas.save()
                        canvas.clipRect(0, rectCurrentItem.bottom, width, height)
                        canvas.drawText(data, drawnCenterX.toFloat(), centerY, paint)
                        canvas.restore()

                        paint.color = selectedItemTextColor
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        paint.textSize = itemSelectedTextSize.toFloat()
                        canvas.save()
                        canvas.clipRect(rectCurrentItem)
                        canvas.drawText(data, drawnCenterX.toFloat(), centerY, paint)
                        canvas.restore()
                    }else{
                        canvas.save()
                        canvas.clipRect(rectDrawn)
                        canvas.drawText(data, drawnCenterX.toFloat(), centerY, paint)
                        canvas.restore()
                    }
                }
                drawnOffsetPos ++
            }

            unit?.let {
                canvas.drawText(unit, drawnCenterX + 100f, drawnCenterY.toFloat(), paintUnit)
            }

            if (hasIndicator) {
                paint.color = indicatorColor
                paint.style = Paint.Style.FILL
                canvas.drawRect(rectIndicatorHead, paint)
                canvas.drawRect(rectIndicatorFoot, paint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scroller.forceFinished(true)
        tracker?.recycle()
        onItemSelectedListener = null
    }

    private fun isPosInRang(position: Int): Boolean {
        return position >= 0 && position < adapter.itemCount
    }

    private fun computeSpace(degree: Int): Int {
        return (Math.sin(Math.toRadians(degree.toDouble())) * halfWheelHeight).toInt()
    }

    private fun computeDepth(degree: Int): Int {
        return (halfWheelHeight - Math.cos(Math.toRadians(degree.toDouble())) * halfWheelHeight).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                if (tracker == null) {
                    tracker = VelocityTracker.obtain()
                } else {
                    tracker?.clear()
                }
                tracker?.addMovement(event)
                lastPointY = event.y.toInt()
                downPointY = lastPointY
                isClickAction = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (Math.abs(downPointY - event.y) < touchSlop) {
                    isClickAction = true
                } else {
                    isClickAction = false
                    tracker?.addMovement(event)
                    var move = event.y - lastPointY
                    if (Math.abs(move) > 1) {
                        if (scrollOffsetY < minFlingY || scrollOffsetY > maxFlingY) {
                            move /= 3f
                        }
                        scrollOffsetY += move.toInt()
                        lastPointY = event.y.toInt()
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (isClickAction) {
                    computePosition(event.x.toInt(), event.y.toInt())
                } else {
                    tracker?.addMovement(event)
                    tracker?.computeCurrentVelocity(1000, maximumVelocity.toFloat())

                    isForceFinishScroll = false
                    val velocity = tracker?.yVelocity?.toInt() ?: 0
                    if (Math.abs(velocity) > minimumVelocity) {
                        scroller.fling(0, scrollOffsetY, 0, velocity, 0, 0, minFlingY, maxFlingY)
                        scroller.finalY = scroller.finalY + computeDistanceToEndPoint(scroller.finalY % itemHeight)
                    } else {
                        if (scrollOffsetY > maxFlingY) {
                            scroller.startScroll(0, scrollOffsetY, 0, -scrollOffsetY + maxFlingY)
                        }else if (scrollOffsetY < minFlingY) {
                            scroller.startScroll(0, scrollOffsetY, 0, minFlingY - scrollOffsetY)
                        }else {
                            scroller.startScroll(0, scrollOffsetY, 0, computeDistanceToEndPoint(scrollOffsetY % itemHeight))
                        }
                    }

                    handler.post(runnable)
                    tracker?.recycle()
                    tracker = null
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                tracker?.recycle()
                tracker = null
            }
        }
        return true
    }

    private fun computePosition(x: Int, y: Int) {
        var distance = y - rectDrawn.height() / 2
        val isMinus = distance < 0
        if (Math.abs(distance) > itemHeight / 2) {
            if (isMinus) {
                distance += itemHeight / 2
            } else {
                distance -= itemHeight / 2
            }
            var count = distance / itemHeight
            if (count > 0 || (count == 0 && !isMinus)) {
                count++
            } else if (count < 0 || (count == 0 && isMinus)) {
                count--
            }
            count += currentItemPosition
            if (count >= 0 && count < adapter.itemCount) {
                scrollTo(count)
            }
        }
    }

    private fun computeDistanceToEndPoint(remainder: Int): Int {
        if (Math.abs(remainder) > halfItemHeight) {
            if (scrollOffsetY < 0) {
                return -itemHeight - remainder
            } else {
                return itemHeight - remainder
            }
        } else {
            return -remainder
        }
    }

    fun scrollTo(itemPosition: Int) {
        if (itemPosition != currentItemPosition) {
            val differencesLines = currentItemPosition - itemPosition
            val newScrollOffsetY = scrollOffsetY + differencesLines * itemHeight

            val va = ValueAnimator.ofInt(scrollOffsetY, newScrollOffsetY)
            va.duration = 300
            va.addUpdateListener { animation ->
                scrollOffsetY = animation.animatedValue as Int
                invalidate()
            }
            va.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentItemPosition = itemPosition
                    onItemSelected()
                }
            })
            va.start()
        }
    }

    private fun onItemSelected() {
        val position = currentItemPosition
        val item = this.adapter.getItem(position)
        onItemSelectedListener?.onItemSelected(this, item, position)
    }

    fun getSelectedItem() = adapter.getItem(selectedItemPosition)

    fun getSelectedItemPosition(): Int {
        return selectedItemPosition
    }

    fun setSelectedItemPosition(position: Int) {
        var _position = Math.min(position, adapter.itemCount - 1)
        _position = Math.max(_position, 0)
        selectedItemPosition = _position
        currentItemPosition = _position
        scrollOffsetY = 0
        computeFlingLimitY()
        requestLayout()
        invalidate()
    }

    fun setAdapter(adapter: Adapter) {
        this.adapter = adapter
        notifyDatasetChanged()
    }

    fun setData(data: List<Any>, defaultSelectedPosition: Int = 0) {
        this.adapter.setData(data)
        setSelectedItemPosition(defaultSelectedPosition)
    }

    fun addData(data: List<Any>) {
        this.adapter.addData(data)
        notifyDatasetChanged()
    }

    fun setOnItemSelectedListener(onItemSelectedListener: OnItemSelectedListener){
        this@WheelPickerView.onItemSelectedListener = onItemSelectedListener
    }

    fun notifyDatasetChanged() {
        if (selectedItemPosition > adapter.itemCount - 1 || currentItemPosition > adapter.itemCount - 1) {
            currentItemPosition = adapter.itemCount - 1
            selectedItemPosition = currentItemPosition
        } else {
            selectedItemPosition = currentItemPosition
        }
        scrollOffsetY = 0
        computeTextSize()
        computeFlingLimitY()
        requestLayout()
        invalidate()
    }

    companion object {
        val ALIGN_CENTER = 0
        val ALIGN_LEFT = 1
        val ALIGN_RIGHT = 2
    }

}