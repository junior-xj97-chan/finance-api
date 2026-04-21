package com.finance.api.common.exception;

import com.finance.api.common.BizCode;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final BizCode bizCode;

    public BusinessException(BizCode bizCode) {
        super(bizCode.getMessage());
        this.bizCode = bizCode;
    }

    public BusinessException(BizCode bizCode, String message) {
        super(message);
        this.bizCode = bizCode;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.bizCode = BizCode.FAIL;
    }
}
