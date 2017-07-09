package com.tlz.wheelpicker

import android.content.Context
import android.util.AttributeSet

/**
 * Created by Lei
 * Date: 2017/4/19
 * Email: t.nianshang@foxmail.com
 * 修改自 https://github.com/webianks/ScrollChoice
 */
class WheelPickerView constructor(context: Context,
    attrs: AttributeSet? = null) : WheelPicker(context, attrs) {

  private var onItemSelectedListener: OnItemSelectedListener? = null

  internal var adapter: WheelPicker.Adapter = WheelPicker.Adapter()

  override var defaultItemPosition: Int = 0
    private set

  init {
    setAdapter(adapter)
  }

  override fun onItemSelected(position: Int, item: Any) {
    val itemText = item as String
    onItemSelectedListener?.onItemSelected(this, position, itemText)
  }

  override fun onItemCurrentScroll(position: Int, item: Any) {}

  fun addItems(data: List<String>, defaultIndex: Int) {
    this.defaultItemPosition = defaultIndex
    adapter.setData(data)
    updateDefaultItem()
  }

  fun setData(data: List<String>){
    adapter.setData(data)
    notifyDatasetChanged()
  }

  fun setOnItemSelectedListener(onItemSelectedListener: OnItemSelectedListener?) {
    this.onItemSelectedListener = onItemSelectedListener
  }

  fun setOnItemSelectedListener(
      block:(WheelPickerView, position: Int, name: String) -> Unit) {
    this.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onItemSelected(scrollChoice: WheelPickerView, position: Int, name: String) {
        block(scrollChoice, position, name)
      }
    }
  }

  private fun updateDefaultItem() {
    setSelectedItemPosition(defaultItemPosition)
  }

  fun getDefaultItemIndex(): Int {
    return defaultItemPosition
  }

  val currentSelection: String
    get() = adapter.getItemText(currentItemPosition)

  interface OnItemSelectedListener {
    fun onItemSelected(scrollChoice: WheelPickerView, position: Int, name: String)
  }

}
