package com.fiskmods.fsk.insn;

import com.fiskmods.fsk.FskMath;

import java.util.*;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleBiFunction;

public enum Instruction
{
    // Syntax
    EQ,
    AT,
    TO,
    NL,
    BST,
    BND,
    ARG,

    // Values
    VAR,
    CST,
    NEG,
    DEG,
    ZRO(0),
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
    LOG(Math::log),
    LOG10(Math::log10),
    LOG1P(Math::log1p),
    SQRT(Math::sqrt),
    CBRT(Math::cbrt),
    SIGNUM(t -> Math.signum(t)),
    SINH(Math::sinh),
    COSH(Math::cosh),
    TANH(Math::tanh),
    EXP(Math::exp),
    EXPM1(Math::expm1),
    ROUND(t -> Double.valueOf(Math.round(t))),
    FLOOR(Math::floor),
    CEIL(Math::ceil),
    CURVE(FskMath::curve),

    // Bifunctions
    MIN(Math::min),
    MAX(Math::max),
    ATAN2(Math::atan2),
    HYPOT(Math::hypot),
    LOGN(FskMath::logn);

    public static final List<?>[] OP_ORDER = {Collections.singletonList(POW), Arrays.asList(MUL, DIV, MOD), Arrays.asList(ADD, SUB)};
    public static final Map<String, Instruction> FUNCTIONS;
    public static final List<String> FUNCTION_NAMES;

    public final DoubleFunction<Double> function;
    public final ToDoubleBiFunction<Double, Double> bifunction;
    public final Double value;

    Instruction(DoubleFunction<Double> func, ToDoubleBiFunction<Double, Double> bifunc, Double val)
    {
        function = func;
        bifunction = bifunc;
        value = val;
    }

    Instruction(DoubleFunction<Double> func)
    {
        this(func, null, null);
    }

    Instruction(ToDoubleBiFunction<Double, Double> bifunc)
    {
        this(null, bifunc, null);
    }

    Instruction(double val)
    {
        this(null, null, val);
    }

    Instruction()
    {
        this(null, null, null);
    }

    static
    {
        FUNCTIONS = new HashMap<>();
        FUNCTION_NAMES = new LinkedList<>();

        for (Instruction insn : values())
        {
            if (insn.function != null || insn.bifunction != null)
            {
                String s = insn.name().toLowerCase(Locale.ROOT);
                FUNCTIONS.put(s, insn);
                FUNCTION_NAMES.add(s);
            }
        }

        Comparator<String> c = (o1, o2) -> o1.startsWith(o2) || o2.startsWith(o1) ? o2.length() - o1.length() : o1.compareTo(o2);
        FUNCTION_NAMES.sort(c);
    }
}
