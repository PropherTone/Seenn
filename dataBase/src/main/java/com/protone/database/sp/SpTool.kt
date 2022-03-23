package com.protone.database.sp

import kotlin.reflect.KProperty

class SpTool(val spProvider: SpProvider){

    fun int(key: String,defValue: Int): Delegate<Int> {
        return object : Delegate<Int>{
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                return spProvider.getInt(key, defValue)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                spProvider.setInt(key, value)
            }
        }
    }

    fun string(key: String,defValue: String): Delegate<String> {
        return object : Delegate<String>{
            override fun getValue(thisRef: Any?, property: KProperty<*>): String {
                return spProvider.getString(key, defValue)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
                spProvider.setString(key,value)
            }
        }
    }

//    fun <C> component(key: String,defValue: String):Delegate<C>{
//        return object  : Delegate<C>{
//            override fun setValue(thisRef: Any?, property: KProperty<*>, value: C) {
//                TODO("Not yet implemented")
//            }
//
//            override fun getValue(thisRef: Any?, property: KProperty<*>): C {
//                TODO("Not yet implemented")
//            }
//        }
//    }

    fun boolean(key: String,defValue: Boolean):Delegate<Boolean>{
        return object : Delegate<Boolean>{
            override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
                return spProvider.getBoolean(key, defValue)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
                return spProvider.setBoolean(key, value)
            }
        }
    }

}