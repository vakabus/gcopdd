package cz.cuni.mff.d3s.blood.utils.matrix;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO documentation
public class Matrix<RowKeyType, ColKeyType, ValueType extends MatrixValue<ValueType>> {

    private final HashMap<RowKeyType, Row> rows;
    private final int initialCapacity;
    private final UnmodifiableRow defaultRow;
    private final ValueType defaultValue;

    public Matrix(int initialCapacity, ValueType defaultValue) {
        this.initialCapacity = initialCapacity;
        rows = new HashMap<>(initialCapacity);
        defaultRow = new UnmodifiableRow();
        this.defaultValue = defaultValue;
    }

    public Matrix(ValueType defaultValue) {
        this.initialCapacity = 0;
        rows = new HashMap<>();
        defaultRow = new UnmodifiableRow();
        this.defaultValue = defaultValue;
    }

    public Row getRow(RowKeyType rowKey) {
        return rows.get(rowKey);
    }

    public UnmodifiableRow getRowOrDefault(RowKeyType rowKey) {
        Row row = getRow(rowKey);
        return (row != null) ? row : defaultRow;
    }

    public Row getOrCreateRow(RowKeyType rowKey) {
        return rows.computeIfAbsent(rowKey, p -> new Row());
    }

    public String toString(Supplier<Stream<RowKeyType>> rowOrderSupplier, Supplier<Stream<ColKeyType>> colOrderSupplier) {
        return rowOrderSupplier.get()
                .map(this::getRowOrDefault)
                .map(row
                        -> colOrderSupplier.get()
                        .map(row::getOrDefault)
                        .map(ValueType::toString)
                        .collect(Collectors.joining(" "))
                )
                .collect(Collectors.joining("\n"));
    }

    public Set<RowKeyType> rowKeys() {
        return rows.keySet();
    }

    public class UnmodifiableRow {

        protected final HashMap<ColKeyType, ValueType> values;

        private UnmodifiableRow() {
            if (initialCapacity == 0) {
                values = new HashMap<>();
            } else {
                values = new HashMap<>(initialCapacity);
            }
        }

        public ValueType get(ColKeyType colKey) {
            return values.get(colKey);
        }

        public ValueType getOrDefault(ColKeyType colKey) {
            return values.getOrDefault(colKey, defaultValue);
        }

        public Stream<ValueType> valuesStream() {
            return values.values().stream();
        }
    }

    public class Row extends UnmodifiableRow {

        private Row() {
        }

        public ValueType getOrCreate(ColKeyType colKey) {
            return values.computeIfAbsent(colKey, p -> defaultValue.copy());
        }
    }
}
