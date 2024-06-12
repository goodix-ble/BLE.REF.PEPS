package com.goodix.ble.libcomx.collection;

import java.util.AbstractList;
import java.util.Collection;

public class RingArrayList<E> extends AbstractList<E> {
    private Object[] elements;
    private int writeIdx = 0; // the position to place new data
    private int elementCnt = 0;

    public RingArrayList(int maxSize) {
        this.elements = new Object[maxSize];
    }

    /**
     * return the next element which will be overwrite at next adding operation
     */
    public E reuseElement() {
        //noinspection unchecked
        return (E) elements[writeIdx];
    }

    @Override
    public boolean add(E e) {
        elements[writeIdx] = e;
        writeIdx = (writeIdx + 1) % elements.length;
        if (elementCnt < elements.length) {
            elementCnt++;
        }
        return true;
    }

    @Override
    public void add(int index, E element) {
        Object[] tmpHaha = new Object[elements.length];

        // 写入index的
        tmpHaha[index] = element;
        // 填充小于index的部分
        for (int i = 0; i < index; i++) {
            tmpHaha[i] = get(i);
        }
        // 填充大于index的部分
        int copySize;
        if (elementCnt < elements.length) {
            copySize = elementCnt;
        } else {
            copySize = elementCnt - 1;
        }
        for (int i = index; i < copySize; i++) {
            tmpHaha[index + 1] = get(i);
        }

        elements = tmpHaha;
        elementCnt = index + 1 + copySize;
        writeIdx = elementCnt % elements.length;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        final int size = c.size();
        if (elements.length > size) {
            for (E e : c) {
                add(e);
            }
        } else {
            int startPos = size - elements.length;
            final Object[] objects = c.toArray();
            System.arraycopy(objects, startPos, elements, 0, elements.length);
            writeIdx = 0;
            elementCnt = elements.length;
        }
        return true;
    }

    @Override
    public E get(int index) {
        Object result;

        if (elementCnt < elements.length) {
            result = elements[index];
        } else {
            result = elements[(writeIdx + index) % elements.length];
        }

        //noinspection unchecked
        return (E) result;
    }

    @Override
    public int size() {
        return elementCnt;
    }

    @Override
    public void clear() {
        writeIdx = 0;
        elementCnt = 0;
    }

    public void setCapability(int maxSize) {
        Object[] oldArr = this.elements;
        final int oldCount = elementCnt;
        final int oldWriteIdx = writeIdx;

        if (maxSize == oldArr.length) {
            return;
        }

        elements = new Object[maxSize];
        writeIdx = 0;
        elementCnt = 0;

        int startPos = oldCount > maxSize ? oldCount - maxSize : 0;

        for (int i = startPos; i < oldCount; i++) {
            int idx = (oldWriteIdx + i) % oldArr.length;

            elements[writeIdx] = oldArr[idx];
            writeIdx = (writeIdx + 1) % elements.length;
            if (elementCnt < elements.length) {
                elementCnt++;
            }
        }
    }
}
