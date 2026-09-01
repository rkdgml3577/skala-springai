package com.example.chatbot.service;

/** 하루 사용량을 다 쓴 경우. 모델을 부르기 전에 던진다. */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
