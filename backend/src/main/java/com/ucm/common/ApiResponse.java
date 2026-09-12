package com.ucm.common;

/** 统一响应体 */
public class ApiResponse<T> {

    private int code;       // 0 成功，非 0 失败
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(1, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
