package com.goodix.ble.libcomx.util;

public class StandardDeviation {
    public double sum;
    public double average;
    public double variance;
    public double stdDeviation; // = sqrt(variance);

    public double calc(int[] input, int startPos, int count) {
        if (input == null || count == 0) return 0;

        if (startPos < 0) {
            startPos += input.length;
            if (startPos < 0) startPos = 0;
        }

        if (count < 0) count = input.length;

        int endPos = startPos + count;
        if (endPos > input.length) endPos = input.length;

        if (startPos < endPos) {
            double sum = 0;
            double avg;

            for (int i = startPos; i < endPos; i++) {
                sum += input[i];
            }

            avg = sum / (endPos - startPos);

            this.sum = sum;
            this.average = avg;

            sum = 0;
            for (int i = startPos; i < endPos; i++) {
                double diff = input[i] - average;
                sum += (diff * diff);
            }
            this.variance = sum / (endPos - startPos);
            this.stdDeviation = Math.sqrt(this.variance);

            return this.stdDeviation;
        }

        return 0;
    }

    public double calc(float[] input, int startPos, int count) {
        if (input == null || count == 0) return 0;

        if (startPos < 0) {
            startPos += input.length;
            if (startPos < 0) startPos = 0;
        }

        if (count < 0) count = input.length;

        int endPos = startPos + count;
        if (endPos > input.length) endPos = input.length;

        if (startPos < endPos) {
            double sum = 0;
            double avg;

            for (int i = startPos; i < endPos; i++) {
                sum += input[i];
            }

            avg = sum / (endPos - startPos);

            this.sum = sum;
            this.average = avg;

            sum = 0;
            for (int i = startPos; i < endPos; i++) {
                double diff = input[i] - average;
                sum += (diff * diff);
            }
            this.variance = sum / (endPos - startPos);
            this.stdDeviation = Math.sqrt(this.variance);

            return this.stdDeviation;
        }

        return 0;
    }

    public double calc(double[] input, int startPos, int count) {
        if (input == null || count == 0) return 0;

        if (startPos < 0) {
            startPos += input.length;
            if (startPos < 0) startPos = 0;
        }

        if (count < 0) count = input.length;

        int endPos = startPos + count;
        if (endPos > input.length) endPos = input.length;

        if (startPos < endPos) {
            double sum = 0;
            double avg;

            for (int i = startPos; i < endPos; i++) {
                sum += input[i];
            }

            avg = sum / (endPos - startPos);

            this.sum = sum;
            this.average = avg;

            sum = 0;
            for (int i = startPos; i < endPos; i++) {
                double diff = input[i] - average;
                sum += (diff * diff);
            }
            this.variance = sum / (endPos - startPos);
            this.stdDeviation = Math.sqrt(this.variance);

            return this.stdDeviation;
        }

        return 0;
    }
}
