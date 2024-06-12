package com.goodix.ble.libcomx.event;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class EventDisposer {
    private ArrayList<Event> events = new ArrayList<>(64);

    public void add(Event evt) {
        if (!events.contains(evt)) {
            events.add(evt);
        }
    }

    public void clearListener(Object tag) {
        for (Event event : events) {
            event.clear(tag);
        }
    }

    public void disposeAll(Object tag) {
        for (Event event : events) {
            event.clear(tag);

            if (tag != null) {
                // 包括从父事件中移除自身
                event.clear();
            }
        }
        events.clear();
    }

    public static void clearListener(Object target, Object tag, boolean includeSuperClass) {
        if (target == null) return;

        // auto clear all event
        Class<?> targetClass = target.getClass();
        while (targetClass != null && targetClass.equals(Object.class)) {

            for (Field field : targetClass.getDeclaredFields()) {
                if (field.getDeclaringClass().equals(Event.class)) {
                    try {
                        field.setAccessible(true);
                        Event event = ((Event) field.get(target));
                        if (event != null) {
                            event.clear(tag);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (includeSuperClass) {
                targetClass = targetClass.getSuperclass();
            } else {
                break;
            }
        }
    }
}
