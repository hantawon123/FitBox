package com.ssafy.fitbox.util

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ssafy.fitbox.R
import com.ssafy.fitbox.fragment.LoginFragment

object LoginRequiredDialog {

    fun show(fragment: Fragment) {
        val activity = fragment.activity as? AppCompatActivity ?: return
        show(activity)
    }

    fun show(activity: AppCompatActivity) {
        AlertDialog.Builder(activity)
            .setTitle("로그인이 필요한 서비스입니다")
            .setMessage("이 기능을 이용하려면 로그인이 필요합니다. 로그인 화면으로 이동하시겠습니까?")
            .setPositiveButton("로그인하러 가기") { _, _ ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, LoginFragment())
                    .addToBackStack(null)
                    .commit()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
