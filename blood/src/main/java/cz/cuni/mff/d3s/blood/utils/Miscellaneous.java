package cz.cuni.mff.d3s.blood.utils;

import ch.usi.dag.util.Assert;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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

    public static String getCompiledMethodSignature(Object compilationRequest) {
        Object methodObject = crGetMethod(compilationRequest);
        return getSignatureOfMethod(methodObject);
    }


    public static String shortTextHash(String toHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            var bytes = md.digest(toHash.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(Arrays.copyOfRange(bytes, 0, 8));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Hardcoded algorithm names are invalid.", e);
        }
    }


    public static String bytesToHex(byte[] arr) {
        var sb = new StringBuilder();
        for (byte b : arr) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}
