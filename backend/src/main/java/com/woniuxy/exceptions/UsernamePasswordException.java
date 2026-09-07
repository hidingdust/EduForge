package com.woniuxy.exceptions;

// 自定义的异常：一般情况下定义为运行时异常
public class UsernamePasswordException extends RuntimeException{
    public UsernamePasswordException(String message){
        super(message);
    }
}