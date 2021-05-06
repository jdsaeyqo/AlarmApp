package com.example.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //뷰 초기화
        initOnOffButton()
        initChangeAlarmTimeButton()

        //1 데이터 가져오기

        val model = fetchDataFromSharedPreferences()
        renderView(model)
        //2 뷰에 데이터를 그려주기

    }

    private fun initOnOffButton() {
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {

            //데이터 확인
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)

            //온오프 에 따라 작업 처리


            if (newModel.onOff) {
                //온 -> 알람 등록
                val calender = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY,newModel.hour)
                    set(Calendar.MINUTE,newModel.minute)

                    if(before(Calendar.getInstance())){
                        add(Calendar.DATE,1)
                    }
                }

                val alarmManager = getSystemService(Context.ALARM_SERVICE)as AlarmManager
                val intent = Intent(this,AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE,intent
                ,PendingIntent.FLAG_UPDATE_CURRENT)

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calender.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )


            } else {
                //오프 -> 알람 제거
                cancelAlarm()
            }


        }
    }

    private fun initChangeAlarmTimeButton() {
        val changeAlarmTimeButton = findViewById<Button>(R.id.changeAlaramTimeButton)
        changeAlarmTimeButton.setOnClickListener {

            val calender = Calendar.getInstance()

            TimePickerDialog(this, { picker, hour, minute ->

                //데이터 저장
                //뷰 업데이트
                val model = saveAlarmModel(hour, minute, false)
                renderView(model)
                //기존 알람 삭제
                cancelAlarm()

            }, calender.get(Calendar.HOUR_OF_DAY), calender.get(Calendar.MINUTE), false)
                .show()

        }
    }

    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean,
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }


        return model
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue

        )

        //보정 예외처리

        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)

        if ((pendingIntent == null) and alarmModel.onOff) {
            //알람은 꺼져있는데, 데이터는 켜져있는 경우
            alarmModel.onOff = false

        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            //알람은 켜져있는데. 데이터는 꺼져있는 경우
            // 알람 취소
            pendingIntent.cancel()
        }

        return alarmModel


    }

    private fun renderView(model: AlarmDisplayModel) {
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }

        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }

        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            tag = model

        }

    }

    private fun cancelAlarm(){
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()
    }

    companion object {
        private const val SHARED_PREFERENCES_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }
}