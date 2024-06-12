package com.goodix.ble.libcomx.ptmodel;

import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.task.TaskQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Product Test Case : 产测测试用例
 * 一般地，通过继承的方式在子类的构造函数中创建具体的测试步骤和判断逻辑。
 * 通过public属性显式持有测试步骤和关键测试动作。这样便于外部对其进行访问。
 * 通过 {@link Task#setParameter} 和 {@link Task#getParameter} 设置一些特有的标记。
 * <p>
 * 也可以，通过组合的方式动态的在外部为用例添加步骤。
 *
 * @param <CTX> 测试用例执行时的上下文，可以是DUT，也可以是其他的数据结构。内部的测试步骤通过CTX确定要操作的对象
 */
public class PtCase<CTX> extends TaskQueue {
    private ArrayList<PtStep> stepList = new ArrayList<>(8);
    public final CTX targetCtx;

    public PtCase(final CTX targetCtx) {
        this.targetCtx = targetCtx;
        // Case 中的 Step 列表，默认只要出现一个异常就中止执行。
        // setAbortOnException(true); 因为这个函数只能调用一次，所以不能在这里设置
    }

    /**
     * 创建测试步骤
     *
     * @param stepName  测试步骤的名称。为null时表示采用Judge的名称或使用自动生成的名称
     * @param criterion 测试步骤通过该标准判断是否通过。
     *                  传入null则没有自动判断的能力，需要手动设置pass的值，常用于需要进行多维度判断的情况
     */
    public final PtStep createStep(String stepName, PtCriterion criterion) {
        final PtJudge judge = new PtJudge(criterion);
        final PtStep step = new PtStep(judge);

        stepList.add(step);
        addTask(step);

        if (stepName == null) {
            if (judge.name == null) {
                judge.name = getName() + "#" + stepList.size();
                //} else {
                //    judge.name = judge.name;
            }
        } else {
            judge.name = stepName;
        }
        step.setName(judge.name);
        return step;
    }

    /**
     * 采用判断标准的名称作为测试步骤的名称
     */
    public final PtStep createStep(PtCriterion criterion) {
        return createStep(criterion != null ? criterion.name : null, criterion);
    }

    /**
     * 用于在测试步骤执行前，或执行后添加独立的动作
     */
    public final <T extends ITask> T addExtraAction(T actonTask) {
        this.addTask(actonTask);
        return actonTask;
    }


    public final boolean isTested(boolean all) {
        if (all) {
            // 判断是否全部都测试过时，只要有一个没有测试，就返回false
            for (PtStep step : stepList) {
                if (!step.getJudge().isTested()) {
                    return false;
                }
            }
            return true;
        } else {
            // 判断是否有测试过时，只要有一个测试了，就返回true
            for (PtStep step : stepList) {
                if (step.getJudge().isTested()) {
                    return true;
                }
            }
        }
        return false;
    }

    public final boolean isPass() {
        for (PtStep step : stepList) {
            if (!step.getJudge().isPass()) {
                return false;
            }
        }
        return true;
    }

    public final int getStepCount() {
        return stepList.size();
    }

    public final PtStep getStep(int idx) {
        return stepList.get(idx);
    }

    /**
     * Override this function to add more results.
     */
    public void getResults(List<PtJudge> results) {
        if (results == null) {
            return;
        }
        for (PtStep step : stepList) {
            final PtJudge judge = step.getJudge();
            if (judge.name == null) {
                judge.name = step.getName();
            }
            results.add(judge);
        }
    }
}
