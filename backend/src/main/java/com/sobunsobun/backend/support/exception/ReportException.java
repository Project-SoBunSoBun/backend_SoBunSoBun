package com.sobunsobun.backend.support.exception;

/**
 * 신고 관련 예외
 */
public class ReportException extends BusinessException {

    public ReportException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static ReportException selfNotAllowed() {
        return new ReportException(ErrorCode.REPORT_SELF_NOT_ALLOWED);
    }

    public static ReportException targetNotFound() {
        return new ReportException(ErrorCode.REPORT_TARGET_NOT_FOUND);
    }

    public static ReportException alreadyReported() {
        return new ReportException(ErrorCode.REPORT_ALREADY_REPORTED);
    }

    public static ReportException notFound() {
        return new ReportException(ErrorCode.REPORT_NOT_FOUND);
    }
}
