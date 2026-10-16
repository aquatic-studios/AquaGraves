package com.aquaticstudios.aquagraves.collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class LongIndex<V> {
    private static final Object[] EMPTY_VALUES = new Object[1];
    private static final long[] EMPTY_KEYS = new long[1];

    private static final class Table {
        private final long[] keys;
        private final Object[] values;
        private final int mask;

        private Table(long[] keys, Object[] values, int mask) {
            this.keys = keys;
            this.values = values;
            this.mask = mask;
        }
    }

    private final Map<Long, V> source = new HashMap<>();
    private volatile Table table = new Table(EMPTY_KEYS, EMPTY_VALUES, 0);
    private volatile int count;

    @Nullable
    @SuppressWarnings("unchecked")
    public V get(long key) {
        final Table snapshot = table;
        final Object[] values = snapshot.values;

        int index = spread(key) & snapshot.mask;
        while (true) {
            final Object value = values[index];
            if (value == null) return null;
            if (snapshot.keys[index] == key) return (V) value;
            index = (index + 1) & snapshot.mask;
        }
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public synchronized void put(long key, @NotNull V value) {
        source.put(key, value);
        rebuild();
    }

    public synchronized boolean remove(long key, @NotNull V expected) {
        if (!source.remove(key, expected)) return false;
        rebuild();
        return true;
    }

    public synchronized void clear() {
        source.clear();
        table = new Table(EMPTY_KEYS, EMPTY_VALUES, 0);
        count = 0;
    }

    public int size() {
        return count;
    }

    private void rebuild() {
        final int capacity = capacityFor(source.size());
        final long[] keys = new long[capacity];
        final Object[] values = new Object[capacity];
        final int mask = capacity - 1;

        for (Map.Entry<Long, V> entry : source.entrySet()) {
            final long key = entry.getKey();

            int index = spread(key) & mask;
            while (values[index] != null) index = (index + 1) & mask;

            keys[index] = key;
            values[index] = entry.getValue();
        }

        table = new Table(keys, values, mask);
        count = source.size();
    }

    private static int capacityFor(int size) {
        int capacity = 2;
        while (capacity < (size << 1) + 2) capacity <<= 1;
        return capacity;
    }

    private static int spread(long key) {
        return (int) ((key * 0x9E3779B97F4A7C15L) >>> 40);
    }
}
