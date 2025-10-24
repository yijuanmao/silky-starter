package com.silky.starter.excel.core.exception;

/**
 * Excel导出异常
 *
 * @author zy
 * @date 2025-10-24 11:46
 **/
public class ExcelExportException extends RuntimeException {

    private static final long serialVersionUID = -1928875175130453714L;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public ExcelExportException() {
    }

    public ExcelExportException(String message) {
        super(message);
        this.message = message;
    }

    public ExcelExportException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
