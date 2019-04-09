package cz.cuni.mff.d3s.blood.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Miscellaneous {

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
}
