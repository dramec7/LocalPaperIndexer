package exception;

public class McpException extends RuntimeException {

    private final McpErrorCode errorCode;
    private final Object data;

    public McpException(McpErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    public McpException(McpErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.data = null;
    }

    public int getCode() {
        return errorCode.getCode();
    }
}