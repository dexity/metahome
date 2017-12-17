// Copyright 2011 Alex Dementsov

package com.surfingbits.metahome;

public interface IMapData {
    abstract void set(int index, int value);
    abstract int get(int index);
    abstract int[] getAll();
    abstract void remove(int index);
    abstract boolean isEmpty(int index);
    abstract int size();
}
