package io.github.arkosammy12.jemu.core.util;

public final class ShiftRegister {

    private final int[] elements;
    private final int elementMask;
    private int head;

    public ShiftRegister(int size, int elementSizeInBits) {
        if (elementSizeInBits < 1) {
            throw new IllegalArgumentException("Element size in bits cannot be 0 or negative!");
        }
        this.elements = new int[size];
        this.elementMask = (1 << elementSizeInBits) - 1;
    }

    public int shiftHead(int shiftInTail) {
        int headValue = this.elements[this.head];
        this.elements[this.head] = shiftInTail & this.elementMask;
        this.head = (this.head + 1) % this.elements.length;
        return headValue;
    }

    public int shiftTail(int shiftInHead) {
        int tail = this.get(this.elements.length - 1);
        this.head = (this.head + this.elements.length - 1) % this.elements.length;
        this.elements[this.head] = shiftInHead & this.elementMask;
        return tail;
    }

    public void set(int index, int value) {
        this.elements[(this.head + index) % this.elements.length] = value & this.elementMask;
    }

    public int get(int index) {
        return this.elements[(this.head + index) % this.elements.length];
    }

}
