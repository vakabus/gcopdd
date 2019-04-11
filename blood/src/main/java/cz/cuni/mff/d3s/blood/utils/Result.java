package cz.cuni.mff.d3s.blood.utils;

import java.util.function.Function;

/**
 * Generic result type as in more functional languages. Contains success value
 * xor an error value. Forces you to check, because there's no other way to get
 * to those values. It's also more explicit than checked exceptions.
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
public final class Result<Success, Error> {

    public static <T, E> Result<T, E> success(T success) {
        if (success == null) {
            throw new NullPointerException("Success value in Result can't be null.");
        }

        return new Result<>(success, null);
    }

    public static <T, E> Result<T, E> error(E error) {
        if (error == null) {
            throw new NullPointerException("Error value in Result can't be null.");
        }

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
        if (isError()) {
            throw new RuntimeException("Result expectation failed! Message: " + errorMsg);
        }

        return success;
    }

    public Error expectError(String errorMsg) {
        if (isOk()) {
            throw new RuntimeException("Result expectation failed! Message: " + errorMsg);
        }

        return error;
    }

    public Success unwrap() {
        return expect("Unwrap failed!");
    }

    public Error unwrapError() {
        return expectError("unwrapError failed!");
    }

    /**
     * This is an extension to the functional api. It allows your lambda
     * function to throw an exception, that will be eventually caught and
     * returned as Result.error. If the lambda function returns normally, the
     * returned value will be wrapped in Result.success.
     * {@link java.lang.Error}s and other
     * non-{@link Exception} {@link Throwable}s are not caught.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface
    public interface CheckedFunction<T, R> extends Function<T, Result<R, Exception>> {

        /**
         * This is the method that is to be implemented by the lambda function.
         * It is allowed to throw an Exception.
         *
         * @param t the function argument
         * @return the function result (will be wrapped in Result.success)
         * @throws Exception reason of potential failure (will be wrapped in
         * Result.error)
         */
        R checkedApply(T t) throws Exception;

        /**
         * This method is safe to call or use in a pipeline. It never throws an
         * exception.
         *
         * @param t this will be passed to
         * {@link #checkedApply(java.lang.Object)}
         * @return a {@link Result} that represents the result of
         * {@link #checkedApply(java.lang.Object)}
         */
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
