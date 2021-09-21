package com.fiskmods.fsk.insn;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleFunction;

import com.fiskmods.fsk.FskMath;

public enum Instruction
{
    // Syntax
    EQ,
    AT,
    TO,
    NL,
    BST,
    BND,

    // Values
    VAR,
    CST,
    NEG,
    NUL(0),
    ONE(1),
    PI(Math.PI),
    E(Math.E),

    // Operators
    ADD,
    SUB,
    MUL,
    DIV,
    POW,
    MOD,

    // Logical operators
    NOT,
    AND,
    OR,

    // Functions
    SIN(Math::sin),
    COS(Math::cos),
    TAN(Math::tan),
    ASIN(Math::asin),
    ACOS(Math::acos),
    ATAN(Math::atan),
    LOG(Math::log10),
    SQRT(Math::sqrt),
    SIG(Math::signum),
    SINH(Math::sinh),
    COSH(Math::cosh),
    TANH(Math::tanh),
    ROUND(t -> Double.valueOf(Math.round(t))),
    FLOOR(t -> Double.valueOf(Math.floor(t))),
    CEIL(t -> Double.valueOf(Math.ceil(t))),
    CURVE(t -> Double.valueOf(FskMath.curve(t)));

    public static final Instruction[] OP_ORDER = {POW, MUL, DIV, MOD, ADD, SUB};
    public static final Map<String, Instruction> FUNCTIONS;

    public final DoubleFunction<Double> function;
    public final Double value;

    Instruction(DoubleFunction<Double> func, Double val)
    {
        function = func;
        value = val;
    }

    Instruction(DoubleFunction<Double> func)
    {
        this(func, null);
    }

    Instruction(double val)
    {
        this(null, val);
    }

    Instruction()
    {
        this(null, null);
    }

    static
    {
        FUNCTIONS = new HashMap<>();

        for (Instruction insn : values())
        {
            if (insn.function != null)
            {
                FUNCTIONS.put(insn.name().toLowerCase(Locale.ROOT), insn);
            }
        }
    }
}
