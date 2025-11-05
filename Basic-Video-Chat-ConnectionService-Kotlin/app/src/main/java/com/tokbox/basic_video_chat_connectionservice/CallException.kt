package com.tokbox.basic_video_chat_connectionservice

class CallException(message: String, val code: Int = 0) : Exception(message)