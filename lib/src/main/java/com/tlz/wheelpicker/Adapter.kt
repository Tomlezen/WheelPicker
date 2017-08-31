package com.tlz.wheelpicker

/**
 * Created by Tomlezen.
 * Date: 2017/8/20.
 * Time: 下午7:55.
 */
class Adapter: BaseAdapter {

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

interface BaseAdapter {

    val itemCount: Int

    fun getItem(position: Int): Any

    fun getItemText(position: Int): String
}