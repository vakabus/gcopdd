package cz.cuni.mff.d3s.blood.utils.matrix;

public interface MatrixValue<T extends MatrixValue<T>> {
    public T copy();
}
