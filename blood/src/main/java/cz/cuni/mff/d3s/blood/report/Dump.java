package cz.cuni.mff.d3s.blood.report;

public interface Dump {

    public String getName();
    
    public byte[] getData();

    public static Dump instantiate(Class<? extends Dump> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
