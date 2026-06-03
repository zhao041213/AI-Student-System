package com.ikunmanager.mcp.dto;

public class ToolResult<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ToolResult<T> ok(T data) {
        ToolResult<T> result = new ToolResult<>();
        result.success = true;
        result.message = "OK";
        result.data = data;
        return result;
    }

    public static <T> ToolResult<T> error(String message) {
        ToolResult<T> result = new ToolResult<>();
        result.success = false;
        result.message = message;
        result.data = null;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
