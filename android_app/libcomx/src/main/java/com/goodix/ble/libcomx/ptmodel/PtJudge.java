package com.goodix.ble.libcomx.ptmodel;

import com.goodix.ble.libcomx.util.HexStringBuilder;

import java.util.ArrayList;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class PtJudge {
    // 判断标准
    private PtCriterion ref;

    // config
    public String name;
    private ArrayList<PtJudge> subJudgeList = null;
    /**
     * 表示采用逻辑或，在多维度判断时，只要有一个维度通过就算通过。否则，需要全部维度都通过才算通过
     */
    public boolean logicalOr;
    /**
     * 表示是否对判定结果进行逻辑取反，对单维度判断和多维度判断都有用。
     */
    public boolean logicalNot;

    // result
    public String value;
    public long timestamp;
    private Boolean pass; // 因为这个有较复杂的逻辑，所以不作为public
    /**
     * 记录异常信息，说明是什么异常导致了不能做出判断。详细数据可以记录到note中。
     */
    public String exception;
    /**
     * 记录有助于诊断问题的详细信息。
     * 非必须。
     */
    public String note;

    PtJudge(PtCriterion ref) {
        this.ref = ref;
        if (ref != null) {
            this.name = ref.name;
            this.logicalNot = ref.logicalNot;
        }
    }

    public boolean judge(String val) {
        this.value = val;
        this.timestamp = System.currentTimeMillis();

        // 先判断多维度的情况
        // 没有测试的子项，会被认定为失败
        if (isComplex()) {
            if (logicalOr) {
                // 有一个成功就都定为成功
                pass = false;
                for (PtJudge judge : subJudgeList) {
                    if (judge.isPass()) {
                        pass = true;
                        break;
                    }
                }
            } else {
                // 有一个失败就都定为失败
                pass = true;
                for (PtJudge judge : subJudgeList) {
                    if (!judge.isPass()) {
                        pass = false;
                        break;
                    }
                }
            }

            // 判断是否取反
            if (logicalNot) {
                pass = !pass;
            }

        } else if (val != null && ref != null) {
            if (ref.valueExact != null) {
                pass = val.equals(ref.valueExact);
            } else if (ref.valueMin != null || ref.valueMax != null) {
                pass = true;
                // 判断是否是小数
                if (ref.valuePrecision == null) {
                    // 整数
                    long value = Long.parseLong(val);
                    if (ref.valueMin != null && value < ref.valueMin) {
                        pass = false;
                    }
                    if (ref.valueMax != null && value > ref.valueMax) {
                        pass = false;
                    }
                } else {
                    float value = Float.parseFloat(val);
                    if (ref.valueMin != null && (int) (value * ref.valuePrecision) < ref.valueMin) {
                        pass = false;
                    }
                    if (ref.valueMax != null && (int) (value * ref.valuePrecision) > ref.valueMax) {
                        pass = false;
                    }
                }
            }

            // 判断是否取反
            if (pass != null && logicalNot) {
                pass = !pass;
            }
        }

        return isPass();
    }

    public boolean judge(int val) {
        return judge(String.valueOf(val));
    }

    public boolean judge(long val) {
        return judge(String.valueOf(val));
    }

    public boolean judge(float val) {
        return judge(String.valueOf(val));
    }

    public void reset() {
        value = null;
        timestamp = 0;
        pass = null;
        exception = null;
        note = null;
        if (isComplex()) {
            for (PtJudge judge : subJudgeList) {
                judge.reset();
            }
        }
    }

    public void setPass(Boolean pass) {
        this.pass = pass;
        if (isComplex()) {
            for (PtJudge judge : subJudgeList) {
                judge.setPass(pass);
            }
        }
    }

    public boolean hasValue() {
        return value != null;
    }

    public boolean isTested() {
        return pass != null || exception != null;
    }

    public boolean isPass() {
        return pass != null && pass;
    }

    /**
     * 获取报告标题
     * 综合名称、取值范围、单位
     */
    public String getReportCaption() {
        HexStringBuilder sb = new HexStringBuilder(128);
        sb.a(name);
        sb.a("(");
        if (logicalNot) {
            sb.a("NOT ");
        }
        if (ref != null) {
            if (ref.valueExact != null) {
                sb.a(ref.valueExact);
            } else if (ref.valueMin != null || ref.valueMax != null) {
                if (ref.valueMin != null) {
                    sb.getStringBuilder().append(ref.valueMin);
                } else {
                    sb.a("N");
                }
                sb.a("--");
                if (ref.valueMax != null) {
                    sb.getStringBuilder().append(ref.valueMax);
                } else {
                    sb.a("N");
                }
            }
            if (ref.valueUnit != null) {
                sb.a(ref.valueUnit);
            }
        }
        sb.a(")");
        return sb.toString();
    }

    /**
     * 获取报告内容
     * 综合pass、实际值、异常
     */
    public String getReportContent() {
        HexStringBuilder sb = new HexStringBuilder(128);
        sb.a(isPass() ? "PASS" : "FAIL");
        if (exception != null) {
            sb.a("(").a(exception).a(")");
        } else {
            sb.a("(");
            if (isComplex()) {
                for (int i = 0; i < subJudgeList.size(); i++) {
                    sb.a("(").a(subJudgeList.get(i).name).a(":").a(subJudgeList.get(i).value).a(")");
                }
            } else {
                sb.a(value);
            }
            sb.a(")");
        }
        return sb.toString();
    }

    public PtCriterion getCriterion() {
        return ref;
    }

    public void setCriterion(PtCriterion criterion) {
        this.ref = criterion;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 多维度判断功能
    ///////////////////////////////////////////////////////////////////////////
    // 进行组合判断
    private boolean isComplex() {
        return subJudgeList != null && !subJudgeList.isEmpty();
    }

    public PtJudge createSubJudge(PtCriterion ref) {
        if (subJudgeList == null) {
            subJudgeList = new ArrayList<>(8);
        }
        final PtJudge judge = new PtJudge(ref);
        subJudgeList.add(judge);
        return judge;
    }

    public int getSubJudgeCount() {
        return subJudgeList.size();
    }


    public PtJudge getSubJudge(int pos) {
        if (pos < subJudgeList.size()) {
            return subJudgeList.get(pos);
        }
        return null;
    }
}
