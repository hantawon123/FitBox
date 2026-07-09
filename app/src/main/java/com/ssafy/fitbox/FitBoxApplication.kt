package com.ssafy.fitbox

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class FitBoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(
            context = this,
            appKey = "8d08719d6d666e32b8ab74ff24dea1e8"
        )
    }
}