package com.eat868299

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.umeng.analytics.MobclickAgent

class UmengAnalyticsBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "UmengAnalyticsBridge"

    @ReactMethod
    fun track(eventName: String, properties: ReadableMap?) {
        if (eventName.isEmpty()) return
        val ctx = reactApplicationContext
        if (properties != null && properties.toHashMap().isNotEmpty()) {
            val map = HashMap<String, String>()
            val iter = properties.keySetIterator()
            while (iter.hasNextKey()) {
                val key = iter.nextKey()
                map[key] = when (properties.getType(key)) {
                    ReadableType.String -> properties.getString(key) ?: ""
                    ReadableType.Number -> properties.getDouble(key).let {
                        if (it == Math.floor(it)) it.toLong().toString() else it.toString()
                    }
                    ReadableType.Boolean -> properties.getBoolean(key).toString()
                    else -> ""
                }
            }
            MobclickAgent.onEvent(ctx, eventName, map)
        } else {
            MobclickAgent.onEvent(ctx, eventName)
        }
    }

    @ReactMethod
    fun onProfileSignIn(userId: String) {
        if (userId.isEmpty()) return
        MobclickAgent.onProfileSignIn(userId)
    }

    @ReactMethod
    fun onProfileSignOff() {
        MobclickAgent.onProfileSignOff()
    }
}
