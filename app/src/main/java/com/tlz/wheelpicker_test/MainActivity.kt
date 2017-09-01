package com.tlz.wheelpicker_test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.tlz.wheelpicker.Adapter
import com.tlz.wheelpicker.OnItemSelectedListener
import com.tlz.wheelpicker.WheelPickerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  private val adapter = Adapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val dataList = mutableListOf<String>()
    (0..50).map { "test$it" }
        .forEach{
          dataList.add(it)
        }
//    wheel_picker.addItems(dataList, 5)
//    wheel_picker.setOnItemSelectedListener { _, position, _ ->
//      Toast.makeText(this@MainActivity, "Selected position $position", Toast.LENGTH_LONG).show()
//    }
//    wheel_picker.setAdapter(adapter)
//    adapter.setData(dataList, 5)
//    wheel_picker.setSelectedItemPosition(0)
    wheel_picker.unit = "年"
    wheel_picker.setData(dataList, 5)
    wheel_picker.notifyDatasetChanged()
    wheel_picker.setOnItemSelectedListener(object : OnItemSelectedListener{
      override fun onCurrentItemOfScroll(picker: WheelPickerView, position: Int) {
        Log.d("MainActivity", "scroll position = $position")
      }

      override fun onItemSelected(picker: WheelPickerView, data: Any, position: Int) {
        Log.d("MainActivity", "selected position = $position")
      }

    })
  }
}
