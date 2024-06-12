package com.goodix.ble.gr.toolbox.common.sig;

import android.util.SparseArray;

@SuppressWarnings("PointlessArithmeticExpression")
public class AppearanceValues {
    private final static SparseArray<String> VALUES = new SparseArray<>(256);

    public static String get(int id) {
        if (VALUES.size() == 0) {
            synchronized (AppearanceValues.class) {
                if (VALUES.size() == 0) {
                    init();
                }
            }
        }
        String appearance = VALUES.get(id);
        if (appearance == null) {
            // 尝试获取Category的类别
            int category = id & 0b1111_1111_1100_0000;
            int subCategory = id & 0b11_1111; // 低6bit
            appearance = VALUES.get(category);
            if (appearance == null) {
                appearance = "Unknown(0x" + Integer.toHexString(id).toUpperCase() + ")";
            } else {
                appearance = appearance + "(Sub-category: " + subCategory + ")(RFU)";
            }
        }
        return appearance;
    }

    private static void init() {
        // UPDATE: 2020/12/1
        VALUES.put(0 + 0, "Unknown");

        VALUES.put(64 + 0, "Generic Phone");

        VALUES.put(128 + 0, "Generic Computer");

        VALUES.put(192 + 0, "Generic Watch");
        VALUES.put(192 + 1, "Watch: Sports Watch");

        VALUES.put(256 + 0, "Generic Clock");

        VALUES.put(320 + 0, "Generic Display");

        VALUES.put(384 + 0, "Generic Remote Control");

        VALUES.put(448 + 0, "Generic Eye-glasses");

        VALUES.put(512 + 0, "Generic Tag");

        VALUES.put(576 + 0, "Generic Keyring");

        VALUES.put(640 + 0, "Generic Media Player");

        VALUES.put(704 + 0, "Generic Barcode Scanner");

        VALUES.put(768 + 0, "Generic Thermometer");
        VALUES.put(768 + 1, "Thermometer: Ear");

        VALUES.put(832 + 0, "Generic Heart Rate Sensor");
        VALUES.put(832 + 1, "Heart Rate Sensor: Heart Rate Belt");

        VALUES.put(896 + 0, "Generic Blood Pressure");
        VALUES.put(896 + 1, "Blood Pressure: Arm");
        VALUES.put(896 + 2, "Blood Pressure: Wrist");

        VALUES.put(960 + 0, "Generic Human Interface Device (HID)");
        VALUES.put(960 + 1, "Keyboard");
        VALUES.put(960 + 2, "Mouse");
        VALUES.put(960 + 3, "Joystick");
        VALUES.put(960 + 4, "Gamepad");
        VALUES.put(960 + 5, "Digitizer Tablet");
        VALUES.put(960 + 6, "Card Reader");
        VALUES.put(960 + 7, "Digital Pen");
        VALUES.put(960 + 8, "Barcode Scanner");

        VALUES.put(1024 + 0, "Generic Glucose Meter");

        VALUES.put(1088 + 0, "Generic: Running Walking Sensor");
        VALUES.put(1088 + 1, "Running Walking Sensor: In-Shoe");
        VALUES.put(1088 + 2, "Running Walking Sensor: On-Shoe");
        VALUES.put(1088 + 3, "Running Walking Sensor: On-Hip");

        VALUES.put(1152 + 0, "Generic: Cycling");
        VALUES.put(1152 + 1, "Cycling: Cycling Computer");
        VALUES.put(1152 + 2, "Cycling: Speed Sensor");
        VALUES.put(1152 + 3, "Cycling: Cadence Sensor");
        VALUES.put(1152 + 4, "Cycling: Power Sensor");
        VALUES.put(1152 + 5, "Cycling: Speed and Cadence Sensor");

        VALUES.put(1216 + 0, "Generic Control Device");
        VALUES.put(1216 + 1, "Switch");
        VALUES.put(1216 + 2, "Multi-switch");
        VALUES.put(1216 + 3, "Button");
        VALUES.put(1216 + 4, "Slider");
        VALUES.put(1216 + 5, "Rotary");
        VALUES.put(1216 + 6, "Touch-panel");

        VALUES.put(1280 + 0, "Generic Network Device");
        VALUES.put(1280 + 1, "Access Point");

        VALUES.put(1344 + 0, "Generic Sensor");
        VALUES.put(1344 + 1, "Motion Sensor");
        VALUES.put(1344 + 2, "Air Quality Sensor");
        VALUES.put(1344 + 3, "Temperature Sensor");
        VALUES.put(1344 + 4, "Humidity Sensor");
        VALUES.put(1344 + 5, "Leak Sensor");
        VALUES.put(1344 + 6, "Smoke Sensor");
        VALUES.put(1344 + 7, "Occupancy Sensor");
        VALUES.put(1344 + 8, "Contact Sensor");
        VALUES.put(1344 + 9, "Carbon Monoxide Sensor");
        VALUES.put(1344 + 10, "Carbon Dioxide Sensor");
        VALUES.put(1344 + 11, "Ambient Light Sensor");
        VALUES.put(1344 + 12, "Energy Sensor");
        VALUES.put(1344 + 13, "Color Light Sensor");
        VALUES.put(1344 + 14, "Rain Sensor");
        VALUES.put(1344 + 15, "Fire Sensor");
        VALUES.put(1344 + 16, "Wind Sensor");
        VALUES.put(1344 + 17, "Proximity Sensor");
        VALUES.put(1344 + 18, "Multi-Sensor");

        VALUES.put(1408 + 0, "Generic Light Fixtures");
        VALUES.put(1408 + 1, "Wall Light");
        VALUES.put(1408 + 2, "Ceiling Light");
        VALUES.put(1408 + 3, "Floor Light");
        VALUES.put(1408 + 4, "Cabinet Light");
        VALUES.put(1408 + 5, "Desk Light");
        VALUES.put(1408 + 6, "Troffer Light");
        VALUES.put(1408 + 7, "Pendant Light");
        VALUES.put(1408 + 8, "In-ground Light");
        VALUES.put(1408 + 9, "Flood Light");
        VALUES.put(1408 + 10, "Underwater Light");
        VALUES.put(1408 + 11, "Bollard with Light");
        VALUES.put(1408 + 12, "Pathway Light");
        VALUES.put(1408 + 13, "Garden Light");
        VALUES.put(1408 + 14, "Pole-top Light");
        VALUES.put(1408 + 15, "Spotlight");
        VALUES.put(1408 + 16, "Linear Light");
        VALUES.put(1408 + 17, "Street Light");
        VALUES.put(1408 + 18, "Shelves Light");
        VALUES.put(1408 + 19, "High-bay / Low-bay Light");
        VALUES.put(1408 + 20, "Emergency Exit Light");

        VALUES.put(1472 + 0, "Generic Fan");
        VALUES.put(1472 + 1, "Ceiling Fan");
        VALUES.put(1472 + 2, "Axial Fan");
        VALUES.put(1472 + 3, "Exhaust Fan");
        VALUES.put(1472 + 4, "Pedestal Fan");
        VALUES.put(1472 + 5, "Desk Fan");
        VALUES.put(1472 + 6, "Wall Fan");

        VALUES.put(1536 + 0, "Generic HVAC");
        VALUES.put(1536 + 1, "Thermostat");

        VALUES.put(1600 + 0, "Generic Air Conditioning");
        VALUES.put(1664 + 0, "Generic Humidifier");

        VALUES.put(1728 + 0, "Generic Heating");
        VALUES.put(1728 + 1, "Radiator");
        VALUES.put(1728 + 2, "Boiler");
        VALUES.put(1728 + 3, "Heat Pump");
        VALUES.put(1728 + 4, "Infrared Heater");
        VALUES.put(1728 + 5, "Radiant Panel Heater");
        VALUES.put(1728 + 6, "Fan Heater");
        VALUES.put(1728 + 7, "Air Curtain");

        VALUES.put(1792 + 0, "Generic Access Control");
        VALUES.put(1792 + 1, "Access Door");
        VALUES.put(1792 + 2, "Garage Door");
        VALUES.put(1792 + 3, "Emergency Exit Door");
        VALUES.put(1792 + 4, "Access Lock");
        VALUES.put(1792 + 5, "Elevator");
        VALUES.put(1792 + 6, "Window");
        VALUES.put(1792 + 7, "Entrance Gate");

        VALUES.put(1856 + 0, "Generic Motorized Device");
        VALUES.put(1856 + 1, "Motorized Gate");
        VALUES.put(1856 + 2, "Awning");
        VALUES.put(1856 + 3, "Blinds or Shades");
        VALUES.put(1856 + 4, "Curtains");
        VALUES.put(1856 + 5, "Screen");

        VALUES.put(1920 + 0, "Generic Power Device");
        VALUES.put(1920 + 1, "Power Outlet");
        VALUES.put(1920 + 2, "Power Strip");
        VALUES.put(1920 + 3, "Plug");
        VALUES.put(1920 + 4, "Power Supply");
        VALUES.put(1920 + 5, "LED Driver");
        VALUES.put(1920 + 6, "Fluorescent Lamp Gear");
        VALUES.put(1920 + 7, "HID Lamp Gear");

        VALUES.put(1984 + 0, "Generic Light Source");
        VALUES.put(1984 + 1, "Incandescent Light Bulb");
        VALUES.put(1984 + 2, "LED Bulb");
        VALUES.put(1984 + 3, "HID Lamp");
        VALUES.put(1984 + 4, "Fluorescent Lamp");
        VALUES.put(1984 + 5, "LED Array");
        VALUES.put(1984 + 6, "Multi-Color LED Array");

        VALUES.put(3136 + 0, "Generic Pulse Oximeter");
        VALUES.put(3136 + 1, "Fingertip");
        VALUES.put(3136 + 2, "Wrist Worn");

        VALUES.put(3200 + 0, "Generic: Weight Scale");

        VALUES.put(5184 + 0, "Generic Outdoor Sports Activity");
        VALUES.put(5184 + 1, "Location Display Device");
        VALUES.put(5184 + 2, "Location and Navigation Display Device");
        VALUES.put(5184 + 3, "Location Pod");
        VALUES.put(5184 + 4, "Location and Navigation Pod");
    }
}
