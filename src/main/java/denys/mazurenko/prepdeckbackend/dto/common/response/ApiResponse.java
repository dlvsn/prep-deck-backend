package denys.mazurenko.prepdeckbackend.dto.common.response;

/**
 * The one shape every endpoint answers with, success or failure alike.
 *
 * <p>A client reads {@code success} first: when it is true, {@code data} holds the result (or is
 * null for an action that returns nothing); when it is false, {@code message} is a key of the
 * client's i18n files saying what went wrong, and {@code data} is null or holds what the client
 * needs to act on it ("2 attempts left", the email still to confirm).
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

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}
