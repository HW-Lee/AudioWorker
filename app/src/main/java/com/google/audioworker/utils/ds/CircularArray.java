package com.google.audioworker.utils.ds;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class CircularArray<T> implements List<T> {
    private Object[] raw;
    private int size;
    private int len;
    private int head;

    private static class CircularArrayIterator<T> implements Iterator<T> {
        private int cur;
        CircularArray<T> arr;

        CircularArrayIterator(CircularArray<T> a) {
            arr = a;
            cur = 0;
        }

        @Override
        public boolean hasNext() {
            return cur < arr.size();
        }

        @Override
        public T next() {
            return arr.get(cur++);
        }
    }

    public CircularArray(int size) {
        this.size = size;
        this.len = 0;
        this.head = 0;
        this.raw = new Object[size];
    }

    @Override
    public int size() {
        return this.len;
    }

    @Override
    public boolean isEmpty() {
        return this.len == 0;
    }

    @Override
    public boolean contains(@Nullable Object o) {
        for (Object each : raw) {
            if (each.equals(o)) return true;
        }
        return false;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return new CircularArrayIterator<>(this);
    }

    @Nullable
    @Override
    public Object[] toArray() {
        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) arr[i] = get(i);

        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T1> T1[] toArray(@Nullable T1[] a) {
        if (a == null) return null;

        ArrayList<T1> arr = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            try {
                arr.add((T1) get(i));
            } catch (ClassCastException e) {
                e.printStackTrace();
                arr.add(null);
            }
        }

        return arr.toArray(a);
    }

    @Override
    public boolean add(T t) {
        if (size == 0) return true;

        raw[(head + len) % size] = t;
        if (len < size) {
            len++;
        } else {
            head++;
            head %= size;
        }

        return true;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        return false;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        for (T each : c) {
            add(each);
        }

        return true;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        head = 0;
        len = 0;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
        if (index >= len) return null;

        try {
            return (T) raw[(head + index) % size];
        } catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public T set(int index, T element) {
        if (index >= len) return null;

        raw[(head + index) % size] = element;
        return element;
    }

    @Override
    public void add(int index, T element) {
        set(index, element);
    }

    @Override
    public T remove(int index) {
        if (index >= len) return null;

        T e = get(index);

        for (int i = index; i < len - 1; i++) {
            raw[(head + i) % size] = raw[(head + i + 1) % size];
        }
        len--;
        return e;
    }

    @Override
    public int indexOf(@Nullable Object o) {
        for (int i = 0; i < len; i++) {
            if (get(i).equals(o)) return i;
        }

        return -1;
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return -1;
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        return null;
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return null;
    }

    @NonNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        ArrayList<T> list = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) list.add(get(i));

        return list;
    }
}
