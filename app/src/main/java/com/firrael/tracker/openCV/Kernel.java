package com.firrael.tracker.openCV;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

/**
 * Created by railag on 20.02.2018.
 */

public enum Kernel {

    TINY(2),
    SMALL(3),
    MEDIUM(10),
    LARGE(50);

    Kernel(int power) {
        this.power = power;
    }

    public void increase() {
        power++;
    }

    public void increase(int increment) {
        power += increment;
    }

    public Mat generate() {
        Mat kernel = new Mat(1, power, CvType.CV_8UC1, Scalar.all(255));
        return kernel;
    }

    private int power;

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }
}
