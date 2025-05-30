package com.example.myapplication.keep

class TestClass {
    val publicField = "public data"
    private val privateField = "private data"

    fun publicMethod() = "public method called"
    private fun privateMethod() = "private method called"

    fun getPrivateData() = privateField
    fun callPrivateMethod() = privateMethod()
}