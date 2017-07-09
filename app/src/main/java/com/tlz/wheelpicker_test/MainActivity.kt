package com.tlz.wheelpicker_test

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.wheel_picker

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val dataList = mutableListOf<String>()
    (0..50).map { "test$it" }
        .forEach{
          dataList.add(it)
        }
    wheel_picker.addItems(dataList, 5)
    wheel_picker.setOnItemSelectedListener { _, position, _ ->
      Toast.makeText(this@MainActivity, "Selected position $position", Toast.LENGTH_LONG).show()
    }
  }
}
