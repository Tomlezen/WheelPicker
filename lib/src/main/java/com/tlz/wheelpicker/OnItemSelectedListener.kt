package com.tlz.wheelpicker

/**
 * Created by Tomlezen.
 * Date: 2017/8/31.
 * Time: 下午2:33.
 */
interface OnItemSelectedListener {
    fun onItemSelected(picker: WheelPickerView, data: Any, position: Int)

    fun onCurrentItemOfScroll(picker: WheelPickerView, position: Int)
}