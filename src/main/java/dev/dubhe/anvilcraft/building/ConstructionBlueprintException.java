package dev.dubhe.anvilcraft.building;

/**
 * 结构蓝图导入与任务操作的可展示错误。reason 是稳定的翻译键后缀,
 * 调用方将 reason 映射为本地化消息,detail 为附加说明。
 */
public class ConstructionBlueprintException extends Exception {
    private final String reason;

    public ConstructionBlueprintException(String reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    public ConstructionBlueprintException(String reason, String detail, Throwable cause) {
        super(detail, cause);
        this.reason = reason;
    }

    public String reason() {
        return this.reason;
    }

    public String detail() {
        return this.getMessage() == null ? "" : this.getMessage();
    }
}
