package cz.cuni.mff.d3s.blood.utils;

public final class Miscellaneous {

    /**
     * Disabling creation of instances of this class.
     *
     * @throws UnsupportedOperationException always
     */
    private Miscellaneous() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    /**
     * Calls CompilationRequest.getMethod() and returns its result.
     *
     * @param compilationRequest instance of jdk.vm.ci.code.CompilationRequest
     * @return instance of jdk.vm.ci.meta.ResolvedJavaMethod
     */
    public static Object crGetMethod(Object compilationRequest) {
        try {
            return compilationRequest.getClass().getMethod("getMethod").invoke(compilationRequest);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the signature of a jdk.vm.ci.meta.ResolvedJavaMethod.
     *
     * @param method instance of jdk.vm.ci.meta.ResolvedJavaMethod
     * @return for example
     * "CompilationContextTracker.getSignatureOfMethod(Object)"
     */
    public static String getSignatureOfMethod(Object method) {
        String s = method.toString();
        // so far, I have only seen "HotSpotMethod<...>", but let's be general
        int left = s.indexOf('<');
        int right = s.lastIndexOf('>');
        return s.substring(left + 1, right == -1 ? s.length() : right);
    }
}
