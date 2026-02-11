package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 표준화된 API 응답 래퍼
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "표준 API 응답")
public class ApiResponse<T> {

    @Schema(description = "상태", example = "success")
    private String status;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private int code;

    @Schema(description = "응답 데이터")
    private T data;

    @Schema(description = "메시지", example = "작업 완료")
    private String message;

    @Schema(description = "에러 코드", example = "CHAT_ROOM_NOT_FOUND")
    private String error;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status("success")
                .code(200)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "작업 완료");
    }

    // 에러 응답
    public static <T> ApiResponse<T> error(int code, String error, String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .code(code)
                .error(error)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> badRequest(String error, String message) {
        return error(400, error, message);
    }

    public static <T> ApiResponse<T> unauthorized(String error, String message) {
        return error(401, error, message);
    }

    public static <T> ApiResponse<T> forbidden(String error, String message) {
        return error(403, error, message);
    }

    public static <T> ApiResponse<T> notFound(String error, String message) {
        return error(404, error, message);
    }

    public static <T> ApiResponse<T> conflict(String error, String message) {
        return error(409, error, message);
    }

    public static <T> ApiResponse<T> serverError(String error, String message) {
        return error(500, error, message);
    }
}
