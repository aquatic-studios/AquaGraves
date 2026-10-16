package com.aquaticstudios.aquagraves.collection;

public final class LongHashSet {
    private long[] keys;
    private boolean[] used;
    private int mask;
    private int size;
    private int limit;

    public LongHashSet(int expected) {
        allocate(capacityFor(expected));
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(long key) {
        if (size >= limit) grow();

        int index = spread(key) & mask;
        while (used[index]) {
            if (keys[index] == key) return;
            index = (index + 1) & mask;
        }

        keys[index] = key;
        used[index] = true;
        size++;
    }

    public boolean contains(long key) {
        int index = spread(key) & mask;
        while (used[index]) {
            if (keys[index] == key) return true;
            index = (index + 1) & mask;
        }
        return false;
    }

    public void clear() {
        if (size == 0) return;
        java.util.Arrays.fill(used, false);
        size = 0;
    }

    private void grow() {
        final long[] oldKeys = keys;
        final boolean[] oldUsed = used;

        allocate(keys.length << 1);
        size = 0;

        for (int i = 0; i < oldUsed.length; i++) {
            if (oldUsed[i]) add(oldKeys[i]);
        }
    }

    private void allocate(int capacity) {
        this.keys = new long[capacity];
        this.used = new boolean[capacity];
        this.mask = capacity - 1;
        this.limit = capacity >> 1;
    }

    private static int capacityFor(int expected) {
        int capacity = 16;
        while (capacity < (expected << 1) + 2) capacity <<= 1;
        return capacity;
    }

    private static int spread(long key) {
        return (int) ((key * 0x9E3779B97F4A7C15L) >>> 40);
    }
}
