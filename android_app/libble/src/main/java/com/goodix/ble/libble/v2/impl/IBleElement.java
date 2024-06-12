package com.goodix.ble.libble.v2.impl;

import java.util.List;
import java.util.UUID;

public interface IBleElement<T> {
    /**
     * @return true-表示这个特征与该包装类匹配上了，并进行了处理
     */
    boolean onDiscovered(T org);

    /**
     * 清除已发现标记
     */
    void clearFoundFlag();

    /**
     * 检查必要的元素是否已经被发现
     */
    boolean checkRequiredUuid();

    /**
     * 将缺失的必要元素列举出来
     */
    void getMissingRequiredUuid(List<UUID> uuids);
}
