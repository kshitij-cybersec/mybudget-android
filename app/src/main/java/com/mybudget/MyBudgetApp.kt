package com.mybudget

import android.app.Application

class MyBudgetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}
