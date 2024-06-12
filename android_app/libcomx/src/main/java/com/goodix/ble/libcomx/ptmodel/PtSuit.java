package com.goodix.ble.libcomx.ptmodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Product Test Suit : 产测测试套件
 * 模型对象树：Suit -> Case -> Step -> Judge [ -> Judge] -> Criterion
 */
public final class PtSuit {
    private ArrayList<PtCase> testCaseList = new ArrayList<>(32);

    public void abort() {
        for (PtCase testCase : testCaseList) {
            testCase.abort();
        }
    }

    public void addCase(PtCase testCase) {
        testCaseList.add(testCase);
    }

    public PtCase getCase(int pos) {
        if (pos < testCaseList.size()) {
            return testCaseList.get(pos);
        }
        return null;
    }

    public int getCaseCount() {
        return testCaseList.size();
    }


    public <T extends PtCase> T getCase(Class<T> caseClass) {
        if (caseClass != null) {
            for (PtCase testCase : testCaseList) {
                if (caseClass.equals(testCase.getClass())) {
                    //noinspection unchecked
                    return (T) testCase;
                }
            }
        }
        return null;
    }

    public <T extends PtCase> T getCase(String name) {
        if (name != null) {
            for (PtCase testCase : testCaseList) {
                if (name.equals(testCase.getName())) {
                    //noinspection unchecked
                    return (T) testCase;
                }
            }
        }
        return null;
    }

    public boolean isTesting() {
        for (PtCase testCase : testCaseList) {
            if (testCase.isStarted()) {
                return true;
            }
        }
        return false;
    }


    public boolean isTested(boolean all) {
        if (all) {
            // 判断是否全部都测试过时，只要有一个没有测试，就返回false
            for (PtCase testCase : testCaseList) {
                // 有些测试用例测试过，但中止了，导致一些Step为未测试状态，如果这里为all=true就会导致误判定套件没有都测试
                if (!testCase.isTested(false)) {
                    return false;
                }
            }
            return true;
        } else {
            // 判断是否有测试过时，只要有一个测试了，就返回true
            for (PtCase testCase : testCaseList) {
                if (testCase.isTested(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPass() {
        for (PtCase testCase : testCaseList) {
            if (!testCase.isPass()) {
                return false;
            }
        }
        return true;
    }

    public List<PtJudge> getResults(List<PtJudge> results) {
        if (results == null) {
            results = new ArrayList<>(testCaseList.size() * 8);
        }
        for (PtCase testCase : testCaseList) {
            testCase.getResults(results);
        }
        return results;
    }
}
