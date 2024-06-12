package com.goodix.ble.gr.toolbox.common.chart;

import com.github.mikephil.charting.data.Entry;
import com.goodix.ble.libcomx.collection.RingArrayList;

public class LineChartScrollData extends RingArrayList<Entry> {

    float xOffset = 0f;
    float xScale = 1f;

    public LineChartScrollData(int maxSize) {
        super(maxSize);
    }

    public LineChartScrollData setOffset(float xOffset, float xScale) {
        this.xOffset = xOffset;
        this.xScale = xScale;
        return this;
    }

    @Override
    public Entry get(int index) {
        Entry entry = super.get(index);
        entry.setX(index * xScale + xOffset);
        return entry;
    }

    public Entry getBySuper(int index) {
        return super.get(index);
    }

    public void add(float val) {
        Entry entry = reuseElement();
        if (entry == null) {
            entry = new Entry(0, val);
        }
        entry.setY(val);
        add(entry);
    }
}
