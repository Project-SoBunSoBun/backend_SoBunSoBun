package com.sobunsobun.backend.support.exception;

/**
 * 차단 관련 예외
 */
public class BlockException extends BusinessException {

    public BlockException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static BlockException selfNotAllowed() {
        return new BlockException(ErrorCode.BLOCK_SELF_NOT_ALLOWED);
    }

    public static BlockException alreadyBlocked() {
        return new BlockException(ErrorCode.ALREADY_BLOCKED);
    }

    public static BlockException notFound() {
        return new BlockException(ErrorCode.BLOCK_NOT_FOUND);
    }
}
