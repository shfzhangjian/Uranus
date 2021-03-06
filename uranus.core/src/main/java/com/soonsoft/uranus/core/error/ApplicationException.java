package com.soonsoft.uranus.core.error;

/**
 * 应用程序异常，继承自Exception
 * 用于描述应用程序错误，带有错误码
 */
public class ApplicationException extends Exception {

    private static final long serialVersionUID = 9060800082887148768L;

    private String errorCode;

    public ApplicationException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApplicationException(String errorCode, String message, Throwable e) {
        super(message, e);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}