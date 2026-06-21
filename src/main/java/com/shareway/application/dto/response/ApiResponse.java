package com.shareway.application.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>(); r.success = true; r.data = data; return r;
    }
    public static <T> ApiResponse<T> ok(T data, String message) {
        ApiResponse<T> r = ok(data); r.message = message; return r;
    }
    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> r = new ApiResponse<>(); r.success = false; r.errorCode = code; r.message = message; return r;
    }
    public static ApiResponse<Void> noContent(String message) {
        ApiResponse<Void> r = new ApiResponse<>(); r.success = true; r.message = message; return r;
    }
}
