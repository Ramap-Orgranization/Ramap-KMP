package com.peto.ramap.data.auth

import android.app.Activity
import java.lang.ref.WeakReference

object KakaoLoginActivityProvider {
    private var activityReference = WeakReference<Activity>(null)

    fun attach(activity: Activity) {
        activityReference = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (activityReference.get() === activity) activityReference.clear()
    }

    fun requireActivity(): Activity = requireNotNull(activityReference.get()) { "카카오 로그인을 실행할 Activity가 없습니다." }
}
