package com.goodix.ble.libcomx.file;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libcomx.util.HexStringParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A simple settings serializer/deserializer based on java.util.Properties.
 * Support field types:
 * # Primitive: boolean, byte, short, int, long, float, double
 * # Array: byte[]
 * # Wrapper: Integer, Float, Double, String
 * # List: ArrayList, List
 */
public class SettingsStorage {
    private final String TAG = getClass().getSimpleName();
    private final String LIST_HEADER = "LISTx";

    private File storageFile;
    private Object settings;
    private ILogger logger;

    public SettingsStorage(Object settings, File storageFile) {
        this.settings = settings;
        this.storageFile = storageFile;
        if (settings == null) {
            throw new IllegalArgumentException("Must bind with a nonnull object.");
        }
        if (storageFile == null) {
            throw new IllegalArgumentException("Storage file is null.");
        }
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }

    public boolean load() {
        final ILogger log = logger;

        // 从文件加载数据
        File json = storageFile;

        if (json == null) {
            return false;
        }

        if (!json.exists()) {
            if (log != null) {
                log.w(TAG, "File does not exist: " + json.getAbsolutePath());
            }
            return false;
        }

        Properties prop = new Properties();

        // 从文件读取全部参数
        try {
            FileReader reader = new FileReader(json);
            prop.load(reader);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (log != null) {
                log.e(TAG, "Load Exception: " + e.getMessage(), e);
            }
            return false;
        }

        // 迭代拷贝全部参数到对象上
        synchronized (this) {
            processAllFields(settings, null, prop, false, log);
        }
        return true;
    }

    public boolean store() {
        final ILogger log = logger;
        // 从文件加载数据
        File json = storageFile;

        if (json == null) {
            return false;
        }

        if (!json.exists()) {
            try {
                if (!json.createNewFile()) {
                    if (log != null) {
                        log.e(TAG, "Failed to create JSON: " + json.getAbsolutePath());
                    }
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (log != null) {
                    log.e(TAG, "Failed to create JSON: " + json.getAbsolutePath(), e);
                }
                return false;
            }
        }

        Properties prop = new Properties();

        // 迭代读取全部参数
        synchronized (this) {
            processAllFields(settings, null, prop, true, log);
        }

        // 写入到文件
        try {
            // 先写到缓存中，然后排序，最后再保存到文件，然文件具有更好的可读性
            ByteArrayOutputStream memFile = new ByteArrayOutputStream(16 * 1024);
            prop.store(new PrintWriter(memFile), null);

            // 然后排序，最后再保存到文件，然文件具有更好的可读性
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(memFile.toByteArray())));
            ArrayList<String> lines = new ArrayList<>(1024);
            String line;
            while ((line = lineReader.readLine()) != null) {
                lines.add(line);
            }
            Collections.sort(lines);

            // 最后再保存到文件，然文件具有更好的可读性
            BufferedWriter writer = new BufferedWriter(new FileWriter(json));
            for (String s : lines) {
                writer.write(s);
                writer.newLine();
            }

            writer.close();
            lineReader.close();
            memFile.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 需要进一步加入迭代，处理自定义类和数组
    private void processAllFields(Object target, String prefix, Properties prop, boolean store, final ILogger log) {
        if (target == null || prop == null) {
            return;
        }

        Field[] fields = target.getClass().getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            // 组成字段名
            if (prefix != null) {
                fieldName = prefix + "." + fieldName;
            }

            // 静态字段和常量都不处理
            final int mod = field.getModifiers();
            if (Modifier.isStatic(mod)) {
                if (log != null) {
                    log.v(TAG, "skip static field: " + fieldName);
                }
                continue;
            }
            if (Modifier.isFinal(mod)) {
                if (log != null) {
                    log.v(TAG, "skip final field: " + fieldName);
                }
                continue;
            }
            if (Modifier.isTransient(mod)) {
                if (log != null) {
                    log.v(TAG, "skip transient field: " + fieldName);
                }
                continue;
            }

            // 如果是 SettingsInJson 自身，也不处理
            Class<?> fieldType = field.getType();
            if (SettingsStorage.class.equals(fieldType)) {
                if (log != null) {
                    log.v(TAG, "skip SettingsStorage field: " + fieldName);
                }
                continue;
            }

            // 保存或加载
            field.setAccessible(true);
            if (store) {
                onStoreField(target, field, fieldName, fieldType, prop, log);
            } else {
                // 判断是否存在内容
                // 从Field的角度出发，主动在Property中寻找。而非从Property到Field
                // 如果存在对应的值，就尝试根据Field的类型将其转换过来
                onLoadField(target, field, fieldName, fieldType, prop, log);
            }
        }
    }

    private void onLoadField(Object obj, Field field, String fullName, Class<?> fieldType, Properties prop, ILogger log) {
        String val = prop.getProperty(fullName, null);
        try {
            if (val != null) {
                // 以下分支判断需要 val 不为空
                if (boolean.class.equals(fieldType))
                    field.setBoolean(obj, Boolean.parseBoolean(val));
                else if (byte.class.equals(fieldType))
                    field.setByte(obj, Byte.parseByte(val));
                else if (short.class.equals(fieldType))
                    field.setShort(obj, Short.parseShort(val));
                else if (int.class.equals(fieldType))
                    field.setInt(obj, Integer.parseInt(val));
                else if (long.class.equals(fieldType))
                    field.setLong(obj, Integer.parseInt(val));
                else if (float.class.equals(fieldType))
                    field.setFloat(obj, Float.parseFloat(val));
                else if (double.class.equals(fieldType))
                    field.setDouble(obj, Double.parseDouble(val));
                else if (Integer.class.equals(fieldType))
                    field.set(obj, Integer.parseInt(val));
                else if (Float.class.equals(fieldType))
                    field.set(obj, Float.parseFloat(val));
                else if (Double.class.equals(fieldType))
                    field.set(obj, Double.parseDouble(val));
                else if (String.class.equals(fieldType) || CharSequence.class.equals(fieldType))
                    field.set(obj, val);
                else if (byte[].class.equals(fieldType)) {
                    if (val.startsWith("0x") && val.length() > 2) {
                        byte[] dat = new byte[(val.length() - 2 + 1) / 2];
                        int actualLength = HexStringParser.parse(val, dat, 0, dat.length);
                        if (actualLength != dat.length) {
                            byte[] dat2 = new byte[actualLength];
                            System.arraycopy(dat, 0, dat2, 0, actualLength);
                            dat = dat2;
                        }
                        field.set(obj, dat);
                    }
                } else if (List.class.isAssignableFrom(fieldType)) {
                    // 如果为list的话，val就一定为空，因为要加入下标才能获得正确的值
                    onLoadList(obj, field, fullName, fieldType, prop, log);
                } else {
                    // 对于未知类型，进行迭代处理。val也一定为空，需要加入.xxx才能得到值
                    processAllFields(field.get(obj), fullName, prop, false, log);
                    val = "__OBJ__";
                }

                if (log != null) {
                    log.v(TAG, "load field: " + fullName + " = " + val);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (log != null) {
                log.w(TAG, "Failed to copy " + fullName + ": " + e.getMessage());
            }
        }
    }

    private void onStoreField(Object obj, Field field, String fullName, Class<?> fieldType, Properties prop, ILogger log) {
        String val = null;
        boolean otherValType = false;
        try {
            // 判断是否存在内容
            if (boolean.class.equals(fieldType))
                val = String.valueOf(field.getBoolean(obj));
            else if (byte.class.equals(fieldType))
                val = String.valueOf(field.getByte(obj));
            else if (short.class.equals(fieldType))
                val = String.valueOf(field.getShort(obj));
            else if (int.class.equals(fieldType))
                val = String.valueOf(field.getInt(obj));
            else if (long.class.equals(fieldType))
                val = String.valueOf(field.getLong(obj));
            else if (float.class.equals(fieldType))
                val = String.valueOf(field.getFloat(obj));
            else if (double.class.equals(fieldType))
                val = String.valueOf(field.getDouble(obj));
            else if (Integer.class.equals(fieldType))
                val = field.get(obj) != null ? String.valueOf(field.get(obj)) : null;
            else if (Float.class.equals(fieldType))
                val = field.get(obj) != null ? String.valueOf(field.get(obj)) : null;
            else if (Double.class.equals(fieldType))
                val = field.get(obj) != null ? String.valueOf(field.get(obj)) : null;
            else if (String.class.equals(fieldType) || CharSequence.class.equals(fieldType))
                val = (String) field.get(obj);
            else if (List.class.isAssignableFrom(fieldType)) {
                onStoreList((List) field.get(obj), fullName, prop, log);
                otherValType = true;
            } else if (byte[].class.equals(fieldType)) {
                byte[] dat = (byte[]) field.get(obj);
                if (dat != null) {
                    val = new HexStringBuilder(dat.length * 2 + 2).Ox().put(dat).toString();
                }
            } else {
                // 对于未知类型，进行迭代处理
                processAllFields(field.get(obj), fullName, prop, true, log);
                otherValType = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (val != null) {
            prop.setProperty(fullName, val);
            if (log != null) {
                log.v(TAG, "store field: " + fullName);
            }
        } else {
            if (log != null) {
                if (otherValType) {
                    log.v(TAG, "store field: " + fullName);
                } else {
                    log.v(TAG, "Faild to store field: " + fullName);
                }
            }
        }
    }

    private void onLoadList(Object obj, Field field, String fullName, Class<?> fieldType, Properties prop, ILogger log) throws IllegalAccessException {
        String listSize = prop.getProperty(fullName);
        if (listSize == null || listSize.length() <= LIST_HEADER.length()) {
            return;
        }

        List gg = (List) field.get(obj);
        if (gg == null) {
            if (ArrayList.class.equals(fieldType)) {
                gg = new ArrayList();
            } else {
                try {
                    gg = (List) fieldType.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (log != null) {
                        log.w(TAG, "Failed to create List instance for field: " + fullName);
                    }
                }
            }
        }
        if (gg != null) {
            gg.clear(); // 先清除原来的数据
            String elementClazz = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].toString().replace("class ", "");
            int elementCnt = Integer.parseInt(listSize.replace(LIST_HEADER, "").trim());
            for (int idx = 0; idx < elementCnt; idx++) {
                String elementName = fullName + "." + idx;
                String elementValue = prop.getProperty(elementName, null);
                Object val = null;
                try {
                    if (elementValue == null) {
                        // 为null时，有可能是自定义类
                        Object elementObj = obj.getClass().getClassLoader().loadClass(elementClazz).newInstance();
                        processAllFields(elementObj, elementName, prop, false, log);
                        val = elementObj;
                    } else {
                        // 能够直接有值的肯定是基本数据类型
                        if (Boolean.class.getName().equals(elementClazz)) {
                            val = Boolean.parseBoolean(elementValue);
                        } else if (Integer.class.getName().equals(elementClazz)) {
                            val = Integer.parseInt(elementValue);
                        } else if (Float.class.getName().equals(elementClazz)) {
                            val = Float.parseFloat(elementValue);
                        } else if (Double.class.getName().equals(elementClazz)) {
                            val = Double.parseDouble(elementValue);
                        } else if (String.class.getName().equals(elementClazz)) {
                            val = elementValue;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (log != null) {
                        log.w(TAG, "Failed to load list element: " + elementName + " = " + elementValue + " -> " + e.getMessage());
                    }
                }
                if (val != null) {
                    //noinspection unchecked
                    gg.add(val);
                    if (log != null) {
                        log.v(TAG, "load list element: " + elementName + " = " + val);
                    }
                }
            }
        }
    }

    private void onStoreList(List list, String fullName, Properties prop, ILogger log) {
        if (list != null && !list.isEmpty()) {
            // 先写入大小
            int size = list.size();
            prop.setProperty(fullName, LIST_HEADER + size);
            // 再写入各元素
            for (int idx = 0; idx < size; idx++) {
                Object elementObj = list.get(idx);
                Class<?> elementClazz = elementObj.getClass();
                String elementName = fullName + "." + idx;
                String elementValue = null;
                try {
                    if (Boolean.class.equals(elementClazz)) {
                        elementValue = String.valueOf(elementObj);
                    } else if (Integer.class.equals(elementClazz)) {
                        elementValue = String.valueOf(elementObj);
                    } else if (Float.class.equals(elementClazz)) {
                        elementValue = String.valueOf(elementObj);
                    } else if (Double.class.equals(elementClazz)) {
                        elementValue = String.valueOf(elementObj);
                    } else if (String.class.equals(elementClazz)) {
                        elementValue = (String) elementObj;
                    } else {
                        // 如果不是基本类型，就按自定义类来处理
                        processAllFields(elementObj, elementName, prop, true, log);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (log != null) {
                        log.w(TAG, "Failed to store list element: " + elementName + " = " + elementObj + " -> " + e.getMessage());
                    }
                }
                if (elementValue != null) {
                    prop.setProperty(elementName, elementValue);
                    if (log != null) {
                        log.v(TAG, "store list element: " + elementName + " = " + elementObj);
                    }
                }
            }
        }
    }
}
