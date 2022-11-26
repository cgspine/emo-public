package cn.qhplus.emo.config

interface ConfigStorage {
    fun readBool(name: String, versionRelated: Boolean, default: Boolean): Boolean
    fun writeBool(name: String, versionRelated: Boolean, value: Boolean)
    fun readInt(name: String, versionRelated: Boolean, default: Int): Int
    fun writeInt(name: String, versionRelated: Boolean, value: Int)
    fun readLong(name: String, versionRelated: Boolean, default: Long): Long
    fun writeLong(name: String, versionRelated: Boolean, value: Long)
    fun readFloat(name: String, versionRelated: Boolean, default: Float): Float
    fun writeFloat(name: String, versionRelated: Boolean, value: Float)
    fun readDouble(name: String, versionRelated: Boolean, default: Double): Double
    fun writeDouble(name: String, versionRelated: Boolean, value: Double)
    fun readString(name: String, versionRelated: Boolean, default: String): String
    fun writeString(name: String, versionRelated: Boolean, value: String)
    fun flush()
}