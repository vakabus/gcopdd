package cz.cuni.mff.d3s.blood.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.graalvm.compiler.hotspot.HotSpotCompilationIdentifier;

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
     * Makes a field non-final using an ugly hack.
     *
     * @param targetField The field to be made non-final
     * @throws Exception If the reflective access fails for one of many reasons.
     */
    public static void makeNonFinal(Field targetField) throws Exception {
        // In this method, we use java.lang.reflect.Field
        // to temporarily modify behavior of java.lang.reflect.Field.
        // I did my best to make it readable, but no guarantee.

        // this is a private field on the Field class
        Field modifiersField = Field.class.getDeclaredField("modifiers");

        // we really want isAccessible() and not canAccess()
        // because it is the analog of setAccessible()
        boolean oldState = modifiersField.isAccessible();

        // make the field accessible to reflection for a while
        modifiersField.setAccessible(true);

        // this is effectively `trackCreationPosition.modifiers &= ~Modifier.FINAL`
        // but this is not allowed, as `modifiers` is private in `Field`
        modifiersField.setInt(targetField, targetField.getModifiers() & ~Modifier.FINAL);

        // restore the previous state of java.lang.reflect.Field, whatever it was
        modifiersField.setAccessible(oldState);
    }

    /**
     * Calls HotSpotCompilationIdentifier.getRequest() and returns its result.
     *
     * @param hscid instance of org.graalvm.compiler.hotspot.HotSpotCompilationIdentifier
     * @return instance of jdk.vm.ci.code.CompilationRequest
     */
    public static Object hscidGetRequest(HotSpotCompilationIdentifier hscid) {
        try {
            return hscid.getClass().getMethod("getRequest").invoke(hscid);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
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
