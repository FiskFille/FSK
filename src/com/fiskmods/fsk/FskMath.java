package com.fiskmods.fsk;

public class FskMath
{
    public static double interpolate(double a, double b, double progress)
    {
        return a + (b - a) * progress;
    }

    public static double curveCrests(double d)
    {
        return Math.sin(d * Math.PI / 2);
    }

    public static double curve(double d)
    {
        return (curveCrests(d * 2 - 1) + 1) / 2;
    }

    public static double logn(double base, double d)
    {
        return Math.log(d) / Math.log(base);
    }

    public static double root(double d, double num)
    {
        return Math.pow(d, 1 / num);
    }

    public static double clamp(double d, double min, double max)
    {
        return Math.min(Math.max(d, min), max);
    }
}
