package com.bletank.bletank

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener


object LongClickRepeatAdapter {
    /**
     * 連続してボタンを押す間隔のデフォルト値 (ms)
     */
    private const val REPEAT_INTERVAL = 100

    /**
     * Viewに長押し時のリピート処理を付加する。 リピート間隔は100ms。
     *
     * @param view
     * 付加対象のView
     */
    fun bless(view: View) {
        bless(REPEAT_INTERVAL, view)
    }

    /**
     * リピート間隔を指定して、Viewに長押しリピート処理を付加する
     *
     * @param repeatInterval
     * 連続してボタンを押す間隔(ms)
     * @param view
     * 付加対象のView
     */
    fun bless(repeatInterval: Int, view: View) {
        val handler = Handler()
        val isContinue = BooleanWrapper(false)
        val repeatRunnable: Runnable = object : Runnable {
            override fun run() {
                // 連打フラグをみて処理を続けるか判断する
                if (!isContinue.value) {
                    return
                }

                // クリック処理を実行する
                view.performClick()

                // 連打間隔を過ぎた後に、再び自分を呼び出す
                handler.postDelayed(this, repeatInterval.toLong())
            }
        }
        view.setOnLongClickListener(OnLongClickListener {
            isContinue.value = true

            // 長押しをきっかけに連打を開始する
            handler.post(repeatRunnable)
            true
        })

        // タッチイベントを乗っ取る
        view.setOnTouchListener(OnTouchListener { v, event -> // キーから指が離されたら連打をオフにする
            if (event.action == MotionEvent.ACTION_UP) {
                isContinue.value = false
            }
            false
        })
    }

    private class BooleanWrapper(var value: Boolean)
}