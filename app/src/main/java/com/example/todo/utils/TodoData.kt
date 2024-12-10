package com.example.todo.utils

data class TodoData(
    val taskId : String = "",
    var task : String = "",
    var infoTask : String = "",
    var kategoriTask : String = "",
    var time : String = "",
    val checked : Boolean = false)
