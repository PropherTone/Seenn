package com.protone.database.sp

interface Sp {
    fun setInt(key:String,value:Int)
    fun getInt(key:String,defValue:Int): Int
    fun setString(key:String,value:String)
    fun getString(key:String,defValue:String): String
    fun setBoolean(key:String, value:Boolean)
    fun getBoolean(key:String,defValue:Boolean): Boolean
}