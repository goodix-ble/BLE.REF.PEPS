package com.goodix.ble.libcomx.ptmodel;

public final class PtCriterion {
    /**
     * 用于唯一表示该标准的默认名称
     */
    public String name;

    /**
     * 表示测试结果应该在某一个测试范围，超出这个范围就表示FAIL
     * 配合valuePrecision来表示小数，valuePrecision表示该范围被放大了多少倍
     */
    public Long valueMin;
    public Long valueMax;
    /**
     * 取值的精度，10表示精确到小数点后1位，100表示精确到小数点后2位
     * 实际值会乘以这个值，再转化为整数再检测是否满足范围
     */
    public Long valuePrecision;

    /**
     * 指定测试结果的精确值，不等于该值就表示FAIL。
     * 当同时指定了取值范围的时候，忽略取值范围。
     */
    public String valueExact;

    /**
     * 表示对判断结果取反。例如，valueExact="1" 的时候，valueActual不为"1"才表示PASS。
     */
    public boolean logicalNot;

    /**
     * 测试值的单位，可以为NULL，表示无量纲的数值
     */
    public String valueUnit;

    /**
     * 对测试值的描述，用于帮助
     */
    public String settingDesc;

    /**
     * 对输入进行提示。为NULL时，不提示
     */
    public String settingHint;

    /**
     * 采用正则表达式对输入进行校验。为NULL时，不校验
     */
    public String settingValidate;

    public PtCriterion copy(PtCriterion that) {
        if (that != null) {
            this.name = that.name;
            this.valueMin = that.valueMin;
            this.valueMax = that.valueMax;
            this.valuePrecision = that.valuePrecision;
            this.valueExact = that.valueExact;
            this.logicalNot = that.logicalNot;
            this.valueUnit = that.valueUnit;
            this.settingDesc = that.settingDesc;
            this.settingHint = that.settingHint;
            this.settingValidate = that.settingValidate;
        }
        return this;
    }
}
