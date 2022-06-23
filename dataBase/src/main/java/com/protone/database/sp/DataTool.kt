package com.protone.database.sp

import kotlin.reflect.KProperty

class DataTool(val dataProvider: DataProvider){

    fun int(key: String,defValue: Int): Delegate<Int> {
        return object : Delegate<Int>{
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                return dataProvider.getInt(key, defValue)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                dataProvider.setInt(key, value)
            }
        }
    }

    fun long(key: String,defValue: Long): Delegate<Long> {
        return object : Delegate<Long>{
            override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
                return dataProvider.getLong(key, defValue)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
                dataProvider.setLong(key, value)
            }
        }
    }

    fun string(key: String,defValue: String): Delegate<String> {
        return object : Delegate<String>{
            override fun getValue(thisRef: Any?, property: KProperty<*>): String {
                return dataProvider.getString(key, defValue)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
                dataProvider.setString(key,value)
            }
        }
    }

    fun boolean(key: String,defValue: Boolean):Delegate<Boolean>{
        return object : Delegate<Boolean>{
            override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
                return dataProvider.getBoolean(key, defValue)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
                return dataProvider.setBoolean(key, value)
            }
        }
    }

}