package cz.cuni.mff.d3s.blood.utils;

public interface MatrixValue<T extends MatrixValue<T>> {
    public T copy();
}
