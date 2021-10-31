package com.fiskmods.fsk.insn;

import com.fiskmods.fsk.FskMath;

import java.util.*;
import java.util.function.DoubleFunction;

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
    DEG,
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
    ASIN(Math::asin),
    ACOS(Math::acos),
    ATAN(Math::atan),
    LOG(Math::log10),
    SQRT(Math::sqrt),
    SIGNUM(Math::signum),
    SINH(Math::sinh),
    COSH(Math::cosh),
    TANH(Math::tanh),
    SIN(Math::sin),
    COS(Math::cos),
    TAN(Math::tan),
    ROUND(t -> Double.valueOf(Math.round(t))),
    FLOOR(t -> Double.valueOf(Math.floor(t))),
    CEIL(t -> Double.valueOf(Math.ceil(t))),
    CURVE(t -> Double.valueOf(FskMath.curve(t)));

    public static final List<?>[] OP_ORDER = {Collections.singletonList(POW), Arrays.asList(MUL, DIV, MOD), Arrays.asList(ADD, SUB)};
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
        FUNCTIONS = new LinkedHashMap<>();

        for (Instruction insn : values())
        {
            if (insn.function != null)
            {
                FUNCTIONS.put(insn.name().toLowerCase(Locale.ROOT), insn);
            }
        }
    }
}
