package com.protone.database.sp

import android.content.SharedPreferences
import androidx.core.content.edit

class SpProvider(private val sharedPreferences: SharedPreferences) : Sp {
    override fun setInt(key: String, value: Int) {
        sharedPreferences.edit {
            putInt(key,value)
        }
    }

    override fun getInt(key: String, defValue: Int): Int {
        return sharedPreferences.getInt(key,defValue)
    }

    override fun setString(key: String, value: String) {
        sharedPreferences.edit {
            putString(key,value)
        }
    }

    override fun getString(key: String, defValue: String): String {
        return sharedPreferences.getString(key,defValue)!!
    }

    override fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit {
            putBoolean(key,value)
        }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key,defValue)

    }

}

fun SharedPreferences.toSpProvider(): SpProvider {
    return SpProvider(this)
}