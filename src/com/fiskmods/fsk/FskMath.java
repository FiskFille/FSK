package com.fiskmods.fsk;

import com.fiskmods.fsk.insn.Instruction;

public class FskMath
{
    public static double interpolate(double a, double b, double progress)
    {
        return a + (b - a) * progress;
    }

    public static double curveCrests(double d)
    {
        return Instruction.SIN.function.apply(d * Math.PI / 2);
    }

    public static double curve(double d)
    {
        return (curveCrests(d * 2 - 1) + 1) / 2;
    }

    public static double logn(double base, double d)
    {
        return Math.log(d) / Math.log(base);
    }
}
