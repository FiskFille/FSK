package com.fiskmods.fsk.insn;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.Function;

public class InsnFunction
{
    private final Function<Double[], Double> function;
    public final int argNum;

    public InsnFunction(int args, Function<Double[], Double> func)
    {
        function = func;
        argNum = args;
    }

    public InsnFunction(DoubleFunction<Double> func)
    {
        this(1, t -> func.apply(t[0]));
    }

    public InsnFunction(DoubleBinaryOperator func)
    {
        this(2, t -> func.applyAsDouble(t[0], t[1]));
    }

    public Double apply(Double... t)
    {
        return function.apply(t);
    }
}
