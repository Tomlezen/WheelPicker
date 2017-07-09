package com.tlz.wheelpicker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import java.util.Locale

/**
 * Created by Lei
 * Date: 2017/4/19
 * Email: t.nianshang@foxmail.com
 */
abstract class WheelPicker constructor(context: Context,
    attrs: AttributeSet? = null) : View(context, attrs) {

  private val sHandler = Handler()
  private val paintBackground: Paint

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.LINEAR_TEXT_FLAG)
  private val maskPaint: Paint
  private val scroller = Scroller(getContext())
  private var tracker: VelocityTracker? = null

  private var onItemSelectedListener: OnItemSelectedListener? = null
  private var onWheelChangeListener: OnWheelChangeListener? = null

  private val rectDrawn: Rect
  private val rectIndicatorHead: Rect
  private val rectIndicatorFoot: Rect
  private val rectCurrentItem: Rect

  private var topLg: LinearGradient? = null
  private var bottomLg: LinearGradient? = null

  private var adapter: BaseAdapter = Adapter()
  private var maxWidthText: String? = null

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
  var currentItemPosition: Int = 0
    private set
  private var minFlingY: Int = 0
  private var maxFlingY: Int = 0
  private var minimumVelocity = 50
  private var maximumVelocity = 8000
  private var wheelCenterX: Int = 0
  private var wheelCenterY: Int = 0
  private var drawnCenterX: Int = 0
  private var drawnCenterY: Int = 0
  private var scrollOffsetY: Int = 0
  private var textMaxWidthPosition: Int = 0
  private var lastPointY: Int = 0
  private var downPointY: Int = 0
  private var touchSlop = 8

  private var hasIndicator: Boolean = false
  private var hasAtmospheric: Boolean = false

  private var isClick: Boolean = false
  private var isForceFinishScroll: Boolean = false

  private val backgroundColor: Int
  private val backgroundOfSelectedItem: Int

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
        if (null != onWheelChangeListener) {
          onWheelChangeListener?.onWheelSelected(position)
          onWheelChangeListener?.onWheelScrollStateChanged(SCROLL_STATE_IDLE)
        }
      }
      if (scroller.computeScrollOffset()) {
        onWheelChangeListener?.onWheelScrollStateChanged(SCROLL_STATE_SCROLLING)

        scrollOffsetY = scroller.currY

        val position = (-scrollOffsetY / itemHeight + selectedItemPosition) % itemCount
        onItemSelectedListener?.onCurrentItemOfScroll(this@WheelPicker, position)
        onItemCurrentScroll(position, adapter.getItem(position))

        postInvalidate()
        sHandler.postDelayed(this, 16)
      }
    }
  }

  init {
    val a = context.obtainStyledAttributes(attrs, R.styleable.WheelPicker)

    itemTextSize = a.getDimensionPixelSize(R.styleable.WheelPicker_scroll_item_text_size,
        resources.getDimensionPixelSize(R.dimen.wheel_item_text_size))
    itemSelectedTextSize = a.getDimensionPixelSize(
        R.styleable.WheelPicker_scroll_item_selected_text_size,
        resources.getDimensionPixelSize(R.dimen.wheel_item_selected_text_size))
    visibleItemCount = a.getInt(R.styleable.WheelPicker_scroll_visible_item_count, 5)
    selectedItemPosition = a.getInt(R.styleable.WheelPicker_scroll_selected_item_position, 0)
    textMaxWidthPosition = a.getInt(R.styleable.WheelPicker_scroll_maximum_width_text_position, -1)
    maxWidthText = a.getString(R.styleable.WheelPicker_scroll_maximum_width_text)
    selectedItemTextColor = a.getColor(R.styleable.WheelPicker_scroll_selected_item_text_color, -1)
    itemTextColor = a.getColor(R.styleable.WheelPicker_scroll_item_text_color, 0xb0424242.toInt())
    backgroundColor = a.getColor(R.styleable.WheelPicker_scroll_background_color,
        0xFFF5F5F5.toInt())
    backgroundOfSelectedItem = a.getColor(R.styleable.WheelPicker_scroll_selected_item_background,
        0xFFFFFFFF.toInt())
    itemSpace = a.getDimensionPixelSize(R.styleable.WheelPicker_scroll_item_space,
        resources.getDimensionPixelSize(R.dimen.wheel_item_space))
    hasIndicator = a.getBoolean(R.styleable.WheelPicker_scroll_indicator, false)
    indicatorColor = a.getColor(R.styleable.WheelPicker_scroll_indicator_color, 0xFFDDDDDD.toInt())
    indicatorSize = a.getDimensionPixelSize(R.styleable.WheelPicker_scroll_indicator_size,
        resources.getDimensionPixelSize(R.dimen.wheel_indicator_size))
    hasAtmospheric = a.getBoolean(R.styleable.WheelPicker_scroll_atmospheric, false)
    maskHeight = a.getDimensionPixelSize(R.styleable.WheelPicker_scroll_mask_height,
        resources.getDimensionPixelSize(R.dimen.wheel_kask_height))
    itemAlign = a.getInt(R.styleable.WheelPicker_scroll_item_align, ALIGN_CENTER)
    a.recycle()

    updateVisibleItemCount()

    paintBackground = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    paint.textSize = itemTextSize.toFloat()

    maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    updateItemTextAlign()

    computeTextSize()


    val conf = ViewConfiguration.get(getContext())
    minimumVelocity = conf.scaledMinimumFlingVelocity
    maximumVelocity = conf.scaledMaximumFlingVelocity
    touchSlop = conf.scaledTouchSlop
    rectDrawn = Rect()

    rectIndicatorHead = Rect()
    rectIndicatorFoot = Rect()

    rectCurrentItem = Rect()

    isClickable = true

    isInitFinish = true
  }

  private fun updateVisibleItemCount() {
    if (visibleItemCount < 2)
      throw ArithmeticException("Wheel's visible item count can not be less than 2!")

    if (visibleItemCount % 2 == 0) visibleItemCount += 1
    drawnItemCount = visibleItemCount + 2
    halfDrawnItemCount = drawnItemCount / 2
  }

  private fun computeTextSize() {
    textMaxHeight = 0
    textMaxWidth = textMaxHeight

    if (isPosInRang(textMaxWidthPosition)) {
      textMaxWidth = paint.measureText(adapter.getItemText(textMaxWidthPosition)).toInt()
    } else if (!TextUtils.isEmpty(maxWidthText)) {
      textMaxWidth = paint.measureText(maxWidthText).toInt()
    } else {
      val itemCount = adapter.itemCount
      (0..itemCount - 1)
          .asSequence()
          .map { adapter.getItemText(it) }
          .map { paint.measureText(it).toInt() }
          .forEach { textMaxWidth = Math.max(textMaxWidth, it) }
    }
    val metrics = paint.fontMetrics
    textMaxHeight = (metrics.bottom - metrics.top).toInt()
  }

  private fun updateItemTextAlign() {
    when (itemAlign) {
      ALIGN_LEFT -> paint.textAlign = Paint.Align.LEFT
      ALIGN_RIGHT -> paint.textAlign = Paint.Align.RIGHT
      else -> paint.textAlign = Paint.Align.CENTER
    }
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
      if (mode == View.MeasureSpec.AT_MOST) realSize = Math.min(realSize, sizeExpect)
    }
    return realSize
  }

  override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
    rectDrawn.set(paddingLeft, paddingTop, width - paddingRight,
        height - paddingBottom)

    wheelCenterX = rectDrawn.centerX()
    wheelCenterY = rectDrawn.centerY()

    computeDrawnCenter()

    halfWheelHeight = rectDrawn.height() / 2

    itemHeight = rectDrawn.height() / visibleItemCount
    halfItemHeight = itemHeight / 2

    topLg = LinearGradient(0f, 0f, 0f, maskHeight.toFloat(), 0x00f2f2f2, 0xf2f2f2,
        Shader.TileMode.MIRROR)

    bottomLg = LinearGradient(0f, (rectDrawn.height() - maskHeight).toFloat(), 0f,
        rectDrawn.height().toFloat(), 0x00f2f2f2, 0xf2f2f2, Shader.TileMode.MIRROR)

    computeFlingLimitY()

    computeIndicatorRect()

    computeCurrentItemRect()
  }

  private fun computeDrawnCenter() {
    when (itemAlign) {
      ALIGN_LEFT -> drawnCenterX = rectDrawn.left
      ALIGN_RIGHT -> drawnCenterX = rectDrawn.right
      else -> drawnCenterX = wheelCenterX
    }
    drawnCenterY = (wheelCenterY - (paint.ascent() + paint.descent()) / 2).toInt()
  }

  private fun computeFlingLimitY() {
    val currentItemOffset = selectedItemPosition * itemHeight
    minFlingY = -itemHeight * (adapter.itemCount - 1) + currentItemOffset
    maxFlingY = currentItemOffset
  }

  private fun computeIndicatorRect() {
    if (!hasIndicator) return
    val halfIndicatorSize = indicatorSize / 2
    val indicatorHeadCenterY = wheelCenterY + halfItemHeight
    val indicatorFootCenterY = wheelCenterY - halfItemHeight
    rectIndicatorHead.set(rectDrawn.left, indicatorHeadCenterY - halfIndicatorSize, rectDrawn.right,
        indicatorHeadCenterY + halfIndicatorSize)
    rectIndicatorFoot.set(rectDrawn.left, indicatorFootCenterY - halfIndicatorSize, rectDrawn.right,
        indicatorFootCenterY + halfIndicatorSize)
  }

  private fun computeCurrentItemRect() {
    if (selectedItemTextColor == -1) return
    rectCurrentItem.set(rectDrawn.left, wheelCenterY - halfItemHeight, rectDrawn.right,
        wheelCenterY + halfItemHeight)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    //防止内存泄漏
    sHandler.removeCallbacks(runnable)
    scroller.forceFinished(true)
    tracker?.recycle()
    onItemSelectedListener = null
    onWheelChangeListener = null
  }

  override fun onDraw(canvas: Canvas) {
    onWheelChangeListener?.onWheelScrolled(scrollOffsetY)
    val drawnDataStartPos = -scrollOffsetY / itemHeight - halfDrawnItemCount

    paintBackground.color = backgroundColor
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paintBackground)

    paintBackground.color = backgroundOfSelectedItem
    paintBackground.style = Paint.Style.FILL
    canvas.drawRect(rectCurrentItem, paintBackground)

    var drawnDataPos = drawnDataStartPos + selectedItemPosition
    var drawnOffsetPos = -halfDrawnItemCount
    while (drawnDataPos < drawnDataStartPos + selectedItemPosition + drawnItemCount) {
      var data = ""

      if (isPosInRang(drawnDataPos)) data = adapter.getItemText(drawnDataPos)

      paint.color = itemTextColor
      paint.textSize = itemTextSize.toFloat()
      paint.style = Paint.Style.FILL
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

      val mDrawnItemCenterY = drawnCenterY + drawnOffsetPos * itemHeight +
          scrollOffsetY % itemHeight + rectIndicatorHead.height() * 2

      if (hasAtmospheric) {
        var alpha = ((drawnCenterY - Math.abs(
            drawnCenterY - mDrawnItemCenterY)) * 1.0f / drawnCenterY * 255).toInt()
        alpha = if (alpha < 0) 0 else alpha
        paint.alpha = alpha
      }

      if (selectedItemTextColor != -1) {
        canvas.save()
        canvas.clipRect(0, 0, width, rectCurrentItem.top)
        canvas.drawText(data, drawnCenterX.toFloat(), mDrawnItemCenterY.toFloat(), paint)
        canvas.restore()

        canvas.save()
        canvas.clipRect(0, rectCurrentItem.bottom, width, height)
        canvas.drawText(data, drawnCenterX.toFloat(), mDrawnItemCenterY.toFloat(), paint)
        canvas.restore()

        paint.color = selectedItemTextColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = itemSelectedTextSize.toFloat()

        canvas.save()
        canvas.clipRect(rectCurrentItem)
        canvas.drawText(data, drawnCenterX.toFloat(), mDrawnItemCenterY.toFloat(), paint)
        canvas.restore()

      } else {
        canvas.save()
        canvas.clipRect(rectDrawn)
        canvas.drawText(data, drawnCenterX.toFloat(), mDrawnItemCenterY.toFloat(), paint)
        canvas.restore()
      }
      drawnDataPos++
      drawnOffsetPos++
    }

    if (hasIndicator) {
      paint.color = indicatorColor
      paint.style = Paint.Style.FILL
      canvas.drawRect(rectIndicatorHead, paint)
      canvas.drawRect(rectIndicatorFoot, paint)
    }

    if (topLg != null) {
      maskPaint.shader = topLg
      canvas.drawRect(0f, 0f, rectDrawn.width().toFloat(), maskHeight.toFloat(), maskPaint)
    }

    if (bottomLg != null) {
      maskPaint.shader = bottomLg
      canvas.drawRect(0f, (rectDrawn.height() - maskHeight).toFloat(), rectDrawn.width().toFloat(),
          rectDrawn.height().toFloat(), maskPaint)
    }
  }

  private fun isPosInRang(position: Int): Boolean {
    return position >= 0 && position < adapter.itemCount
  }

  private fun computeSpace(degree: Int): Int {
    return (Math.sin(Math.toRadians(degree.toDouble())) * halfWheelHeight).toInt()
  }

  private fun computeDepth(degree: Int): Int {
    return (halfWheelHeight - Math.cos(
        Math.toRadians(degree.toDouble())) * halfWheelHeight).toInt()
  }

  @SuppressLint("ObsoleteSdkInt")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        if (null != parent) parent.requestDisallowInterceptTouchEvent(true)
        if (tracker == null) {
          tracker = VelocityTracker.obtain()
        } else {
          tracker?.clear()
        }
        tracker?.addMovement(event)
        if (!scroller.isFinished) {
//          scroller.abortAnimation()
//          isForceFinishScroll = true
        }
        lastPointY = event.y.toInt()
        downPointY = lastPointY
      }
      MotionEvent.ACTION_MOVE -> {
        if (Math.abs(downPointY - event.y) < touchSlop) {
          isClick = true
        } else {
          isClick = false
          tracker?.addMovement(event)
          onWheelChangeListener?.onWheelScrollStateChanged(SCROLL_STATE_DRAGGING)

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
        parent.requestDisallowInterceptTouchEvent(false)
        if (isClick) {
          computePosition(event.x.toInt(), event.y.toInt())
        } else {
          tracker?.addMovement(event)

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            tracker?.computeCurrentVelocity(1000, maximumVelocity.toFloat())
          } else {
            tracker?.computeCurrentVelocity(1000)
          }

          isForceFinishScroll = false
          val velocity = tracker?.yVelocity?.toInt() ?: 0
          if (Math.abs(velocity) > minimumVelocity) {
            scroller.fling(0, scrollOffsetY, 0, velocity, 0, 0, minFlingY, maxFlingY)
            scroller.finalY = scroller.finalY + computeDistanceToEndPoint(
                scroller.finalY % itemHeight)
          } else {
            if (scrollOffsetY > maxFlingY)
              scroller.startScroll(0, scrollOffsetY, 0, -scrollOffsetY + maxFlingY)
            else if (scrollOffsetY < minFlingY)
              scroller.startScroll(0, scrollOffsetY, 0, minFlingY - scrollOffsetY)
            else
              scroller.startScroll(0, scrollOffsetY, 0,
                  computeDistanceToEndPoint(scrollOffsetY % itemHeight))
          }

          sHandler.post(runnable)
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
    onItemSelected(position, item)
  }

  protected abstract fun onItemSelected(position: Int, item: Any)

  protected abstract fun onItemCurrentScroll(position: Int, item: Any)

//  private fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
//    onItemSelectedListener = listener
//  }

  fun getSelectedItemPosition(): Int {
    return selectedItemPosition
  }

  fun setSelectedItemPosition(position: Int) {
    var position = position
    position = Math.min(position, adapter.itemCount - 1)
    position = Math.max(position, 0)
    selectedItemPosition = position
    currentItemPosition = position
    scrollOffsetY = 0
    computeFlingLimitY()
    requestLayout()
    invalidate()
  }

  abstract val defaultItemPosition: Int

  fun setAdapter(adapter: Adapter) {
    this.adapter = adapter
    notifyDatasetChanged()
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

  fun setOnWheelChangeListener(listener: OnWheelChangeListener) {
    onWheelChangeListener = listener
  }

  var maximumWidthText: String?
    get() = maxWidthText
    set(text) {
      if (null == text) throw NullPointerException("Maximum width text can not be null!")
      maxWidthText = text
      computeTextSize()
      requestLayout()
      invalidate()
    }

  var maximumWidthTextPosition: Int
    get() = textMaxWidthPosition
    set(position) {
      if (!isPosInRang(position)) {
        throw ArrayIndexOutOfBoundsException(
            "Maximum width text Position must in [0, " + adapter.itemCount + "), but current is " + position)
      }
      textMaxWidthPosition = position
      computeTextSize()
      requestLayout()
      invalidate()
    }

  fun setIndicator(hasIndicator: Boolean) {
    this.hasIndicator = hasIndicator
    computeIndicatorRect()
    invalidate()
  }

  fun hasIndicator(): Boolean {
    return hasIndicator
  }

  fun setAtmospheric(hasAtmospheric: Boolean) {
    this.hasAtmospheric = hasAtmospheric
    invalidate()
  }

  fun hasAtmospheric(): Boolean {
    return hasAtmospheric
  }

  var typeface: Typeface?
    get() {
      return paint.typeface
    }
    set(tf) {
      paint.typeface = tf
      computeTextSize()
      requestLayout()
      invalidate()
    }

  val currentLocale: Locale
    @TargetApi(Build.VERSION_CODES.N)
    get() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return resources.configuration.locales.get(0)
      } else {
        return resources.configuration.locale
      }
    }

  interface BaseAdapter {

    val itemCount: Int

    fun getItem(position: Int): Any

    fun getItemText(position: Int): String
  }

  interface OnItemSelectedListener {
    fun onItemSelected(picker: WheelPicker, data: Any, position: Int)

    fun onCurrentItemOfScroll(picker: WheelPicker, position: Int)
  }

  interface OnWheelChangeListener {
    fun onWheelScrolled(offset: Int)

    fun onWheelSelected(position: Int)

    fun onWheelScrollStateChanged(state: Int)
  }

  class Adapter : BaseAdapter {

    private val data = mutableListOf<Any>()

    override val itemCount: Int
      get() = data.size

    override fun getItem(position: Int): Any {
      val itemCount = itemCount
      return data[(position + itemCount) % itemCount]
    }

    override fun getItemText(position: Int): String {
      return data[position].toString()
    }

    fun setData(data: List<Any>) {
      this.data.clear()
      this.data.addAll(data)
    }

    fun addData(data: List<Any>) {
      this.data.addAll(data)
    }
  }

  companion object {

    val SCROLL_STATE_IDLE = 0
    val SCROLL_STATE_DRAGGING = 1
    val SCROLL_STATE_SCROLLING = 2

    val ALIGN_CENTER = 0
    val ALIGN_LEFT = 1
    val ALIGN_RIGHT = 2
  }

}
