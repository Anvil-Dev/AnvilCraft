package dev.dubhe.anvilcraft.api.injection;

/**
 * 当一个位于该软件包下的接口的方法未被Mixin等方法实现时抛出<br>
 * 位于该软件包下的接口的每个必须实现的方法都应抛出该异常
 */
public class UnimplementedException extends RuntimeException {
    public UnimplementedException() {
        super();
    }

    public UnimplementedException(String message) {
        super(message);
    }

    public UnimplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnimplementedException(Throwable cause) {
        super(cause);
    }
}
