package com.fiskmods.fsk.insn;

import com.fiskmods.fsk.FskMath;

import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;

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
    OUT,

    // Values
    VAR,
    CST,
    STR,
    NEG,
    DEG,
    ZRO(0),
    ONE(1),
    PI(Math.PI),
    E(Math.E),

    // Operators
    ADD(Double::sum, true),
    SUB((l, r) -> l - r, true),
    MUL((l, r) -> l * r, true),
    DIV((l, r) -> l / r, true),
    POW(Math::pow, true),
    MOD((l, r) -> l % r, true),

    // Logical operators
    NOT,
    AND((l, r) -> l == 1 && r == 1 ? 1 : 0, true),
    OR((l, r) -> l == 1 || r == 1 ? 1 : 0, true),
    EQS((l, r) -> l == r ? 1 : 0, true),
    NEQ((l, r) -> l != r ? 1 : 0, true),
    LT((l, r) -> l < r ? 1 : 0, true),
    GT((l, r) -> l > r ? 1 : 0, true),
    LEQ((l, r) -> l <= r ? 1 : 0, true),
    GEQ((l, r) -> l >= r ? 1 : 0, true),

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

    MIN(Math::min),
    MAX(Math::max),
    ATAN2(Math::atan2),
    HYPOT(Math::hypot),
    LOGN(FskMath::logn),
    ROOT(FskMath::root),

    IF(new InsnFunction(3, t -> FskMath.ifElse(t[0], t[1], t[2]))),
    CLAMP(new InsnFunction(3, t -> FskMath.clamp(t[0], t[1], t[2]))),
    ANIMATE(new InsnFunction(3, t -> FskMath.animate(t[0], t[1], t[2]))),

    ANIMATE2(new InsnFunction(5, t -> FskMath.animate(t[0], t[1], t[2], t[3], t[4])));

    public static final List<?>[] OP_ORDER = {Arrays.asList(POW, NOT), Arrays.asList(MUL, DIV, MOD), Arrays.asList(ADD, SUB), Arrays.asList(AND, EQS, NEQ, LT, GT, LEQ, GEQ), Collections.singletonList(OR)};
    public static final Map<String, Instruction> FUNCTIONS;
    public static final List<String> FUNCTION_NAMES;

    public final Type type;
    public final Object object;

    Instruction()
    {
        type = Type.SYNTAX;
        object = null;
    }

    Instruction(Number val)
    {
        type = Type.VALUE;
        object = val;
    }

    Instruction(DoubleBinaryOperator operation, boolean isOperator)
    {
        type = Type.OPERATOR;
        object = operation;
    }

    Instruction(InsnFunction func)
    {
        type = Type.FUNCTION;
        object = func;
    }

    Instruction(DoubleFunction<Double> func)
    {
        this(new InsnFunction(func));
    }

    Instruction(DoubleBinaryOperator func)
    {
        this(new InsnFunction(func));
    }

    public boolean isValue()
    {
        return type == Type.VALUE;
    }

    public boolean isOperator()
    {
        return type == Type.OPERATOR;
    }

    public boolean isFunction()
    {
        return type == Type.FUNCTION;
    }

    public boolean isFunction(Predicate<InsnFunction> p)
    {
        return isFunction() && p.test(function());
    }

    public double value()
    {
        return ((Number) object).doubleValue();
    }

    public DoubleBinaryOperator operator()
    {
        return (DoubleBinaryOperator) object;
    }

    public InsnFunction function()
    {
        return (InsnFunction) object;
    }

    static
    {
        FUNCTIONS = new HashMap<>();
        FUNCTION_NAMES = new LinkedList<>();

        for (Instruction insn : values())
        {
            if (insn.isFunction())
            {
                String s = insn.name().toLowerCase(Locale.ROOT);
                FUNCTIONS.put(s, insn);
                FUNCTION_NAMES.add(s);
            }
        }

        FUNCTION_NAMES.sort((o1, o2) -> o1.startsWith(o2) || o2.startsWith(o1) ? o2.length() - o1.length() : o1.compareTo(o2));
    }

    private enum Type
    {
        SYNTAX,
        VALUE,
        OPERATOR,
        FUNCTION
    }
}
