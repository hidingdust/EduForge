//package com.woniuxy.utils;
//
//import org.springframework.http.HttpStatus;
//
//import java.util.HashMap;
//
///**
// * @Do: 统一响应体对象
// **/
//public class ResponseResult extends HashMap {
//    //枚举类，用于定义ResponseEntity的key
//    public enum ResponseStatus{
//        //code 响应编码
//        CODE("code"),
//        //message 响应消息
//        MSG("msg"),
//        //data响应体
//        DATA("data");
//        private final String value;
//        ResponseStatus(String value) {
//            this.value = value;
//        }
//        public String value(){
//            return this.value;
//        }
//    }
//    private static final String CODE= ResponseStatus.CODE.value();//当做map中的key
//    private static final String MSG= ResponseStatus.MSG.value();
//    private static final String OBJ = ResponseStatus.DATA.value();
//
//    public ResponseResult(Object obj) {
//        super.put(CODE, HttpStatus.OK.value());
//        super.put(MSG,"执行成功");
//        super.put(OBJ,obj);
//    }
//    public ResponseResult(int code, String message ){
//        super.put(CODE,code);
//        super.put(MSG,message);
//    }
//    public ResponseResult(int code, String message, Object data) {
//        super.put(CODE,code);
//        super.put(MSG,message);
//        if (data != null) {
//            super.put(OBJ,data);
//        }
//    }
//
//    public static final ResponseResult SUCCESS=new ResponseResult(HttpStatus.OK.value(),"执行成功！");
//    public static final ResponseResult ERROR=new ResponseResult(HttpStatus.INTERNAL_SERVER_ERROR.value(),"执行失败！");
//    public static final ResponseResult TIME_OUT=new ResponseResult(HttpStatus.REQUEST_TIMEOUT.value(),"降级成功");
//
//    public ResponseResult putKey(String key, Object obj){
//        super.put(key,obj);
//        return  this;
//    }
//}

package com.woniuxy.utils;

import org.springframework.http.HttpStatus;

import java.util.HashMap;

/**
 * @Do: 统一响应体对象
 **/
public class ResponseResult extends HashMap {
    //枚举类，用于定义ResponseEntity的key
    public enum ResponseStatus {
        //code 响应编码
        CODE("code"),
        //message 响应消息
        MSG("msg"),
        //data响应体
        DATA("data");
        private final String value;

        ResponseStatus(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    private static final String CODE = ResponseStatus.CODE.value();//当做map中的key
    private static final String MSG = ResponseStatus.MSG.value();
    private static final String OBJ = ResponseStatus.DATA.value();

    // ============原有构造器【保留，兼容旧代码】============
    public ResponseResult(Object obj) {
        super.put(CODE, HttpStatus.OK.value());
        super.put(MSG, "执行成功");
        super.put(OBJ, obj);
    }

    public ResponseResult(int code, String message) {
        super.put(CODE, code);
        super.put(MSG, message);
    }

    public ResponseResult(int code, String message, Object data) {
        super.put(CODE, code);
        super.put(MSG, message);
        if (data != null) {
            super.put(OBJ, data);
        }
    }

    // ============内置常量【保留】============
    public static final ResponseResult SUCCESS = new ResponseResult(HttpStatus.OK.value(), "执行成功！");
    public static final ResponseResult ERROR = new ResponseResult(HttpStatus.INTERNAL_SERVER_ERROR.value(), "执行失败！");
    public static final ResponseResult TIME_OUT = new ResponseResult(HttpStatus.REQUEST_TIMEOUT.value(), "降级成功");

    public ResponseResult putKey(String key, Object obj) {
        super.put(key, obj);
        return this;
    }

    // =====================新增静态快捷方法（重点）=====================

    /**
     * 成功响应，自定义消息 + 数据
     */
    public static ResponseResult success(String msg, Object data) {
        return new ResponseResult(HttpStatus.OK.value(), msg, data);
    }

    /**
     * 成功响应，自定义消息，无data
     */
    public static ResponseResult success(String msg) {
        return new ResponseResult(HttpStatus.OK.value(), msg, null);
    }

    /**
     * 成功响应，默认消息，携带数据
     */
    public static ResponseResult success(Object data) {
        return new ResponseResult(data);
    }

    /**
     * 失败响应：默认500，自定义提示信息（最常用！）
     */
    public static ResponseResult fail(String msg) {
        return new ResponseResult(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
    }

    /**
     * 失败响应：自定义状态码 + 自定义消息
     */
    public static ResponseResult fail(int code, String msg) {
        return new ResponseResult(code, msg, null);
    }

    /**
     * 失败响应：自定义状态码 + 消息 + 附加数据
     */
    public static ResponseResult fail(int code, String msg, Object data) {
        return new ResponseResult(code, msg, data);
    }
}