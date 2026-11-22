package denys.mazurenko.prepdeckbackend.api;

/**
 * The one shape every endpoint answers with, success or failure alike.
 *
 * <p>A client reads {@code success} first: when it is true, {@code data} holds the result (or is
 * null for an action that returns nothing); when it is false, {@code message} says what went wrong
 * in words fit to show the user, and {@code data} is null.
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
