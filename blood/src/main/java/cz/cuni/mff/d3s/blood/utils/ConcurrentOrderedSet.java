package cz.cuni.mff.d3s.blood.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class ConcurrentOrderedSet<T> {
    private final ConcurrentHashMap<T, Object> elements = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<T> order = new ConcurrentLinkedQueue<>();
    
    public void add(T element) {
        elements.computeIfAbsent(element, x -> order.add(element));
    }

    public Stream<T> stream() {
        return order.stream();
    }
}
