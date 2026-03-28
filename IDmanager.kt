package com.example.a451_app
import android.content.Context
import java.util.UUID


class IDmanager(context: Context) {
    private val pref = context.getSharedPreferences("Userprefs",Context.MODE_PRIVATE)
    fun checkId() : String {
        var  id = pref.getString("device_id", "base") ?: "base"
            if (id == "base")
                id = (UUID.randomUUID()).toString()
            pref.edit().putString("device_id", id).apply()

        return id

    }

}
