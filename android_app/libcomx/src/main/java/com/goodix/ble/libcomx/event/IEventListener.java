package com.goodix.ble.libcomx.event;

public interface IEventListener<T> {
    /**
     * Define the event handler.
     *
     * @param src     who emits the event. The src shall define values of evtType.
     * @param evtType the type , or the code, of the event.
     * @param evtData the data of event
     */
    void onEvent(Object src, int evtType, T evtData);
}
