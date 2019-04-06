package cz.cuni.mff.d3s.blood.utils;

/**
 * Generic result type as in more functional languages. Contains success value xor an error value. Forces you to check,
 * because there's no other way to get to those values. It's also more explicit than checked exceptions.
 * <p>
 * <b>Invariants:</b><br/>
 * It's NOT ok for a success value or an error value to be null.
 *
 *
 * @see <a href="https://doc.rust-lang.org/std/result/">Result type in Rust</a>
 *
 * @param <Success>
 * @param <Error>
 */
public final class Result<Success,Error> {

    public static <T,E> Result<T,E> success(T success) {
        if (success == null)
            throw new NullPointerException("Success value in Result can't be null.");

        return new Result<>(success, null);
    }

    public static <T,E> Result<T,E> error(E error) {
        if (error == null)
            throw new NullPointerException("Error value in Result can't be null.");

        return new Result<>(null, error);
    }

    final private Success success;
    final private Error error;
    final private boolean successful;

    private Result(Success success, Error error) {
        this.success = success;
        this.error = error;
        successful = success != null;
    }

    public boolean isOk() {
        return successful;
    }

    public boolean isError() {
        return !successful;
    }

    public Success expect(String errorMsg) {
        if (isError())
            throw new RuntimeException("Result expectation failed! Message: " + errorMsg);

        return success;
    }

    public Error expectError(String errorMsg) {
        if (isOk())
            throw new RuntimeException("Result expectation failed! Message: " + errorMsg);

        return error;
    }

    public Success unwrap() {
        return expect("Unwrap failed!");
    }

    public Error unwrapError() {
        return expectError("unwrapError failed!");
    }

    @FunctionalInterface
    public static interface Function<T, R> extends java.util.function.Function<T, Result<R, Exception>> {

        R checkedApply(T t) throws Exception;

        @Override
        default Result<R, Exception> apply(T t) {
            try {
                return Result.success(checkedApply(t));
            } catch (Exception e) {
                return Result.error(e);
            }
        }
    }
}
