package com.fiskmods.fsk;

import com.fiskmods.fsk.insn.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

import static com.fiskmods.fsk.insn.Instruction.*;

public class Script
{
    final List<String> lookup;

    private final String[] varNames;
    public final Var[] vars;

    private Runnable assembledScript = () -> { };
    private BiConsumer<String, double[]> outputChannel = (t, u) -> { };

    public Script(List<InsnNode> instructions, List<String> lookup)
    {
        this.lookup = lookup;

        varNames = lookup.toArray(new String[0]);
        vars = new Var[varNames.length];

        for (int i = 0; i < vars.length; ++i)
        {
            vars[i] = new Var(varNames[i], i, 0);
        }

        List<InsnNode> list = instructions;

        for (int i = 0; i < list.size(); ++i)
        {
            if (list.get(i).instruction == NL)
            {
                List<InsnNode> line = list.subList(0, i);
                list = list.subList(i + 1, list.size());
                assembleLine(line);
                i = 0;
            }
        }
    }

    public Var assign(String s, double defVal)
    {
        int i = lookup.indexOf(s);

        if (i > -1)
        {
            vars[i].accept(defVal);
            return vars[i];
        }

        return null;
    }

    public Script run()
    {
        assembledScript.run();
        return this;
    }

    public Script addListener(BiConsumer<String, double[]> listener)
    {
        outputChannel = outputChannel.andThen(listener);
        return this;
    }

    public Script print()
    {
        for (int i = 0; i < vars.length; ++i)
        {
            System.out.println(varNames[i] + ": " + vars[i].getAsDouble());
        }

        return this;
    }

    private void append(Runnable r)
    {
        Runnable r0 = assembledScript;
        assembledScript = () ->
        {
            r0.run();
            r.run();
        };
    }

    private void assembleLine(List<InsnNode> instructions)
    {
        List<Object> assembly = new ArrayList<>();

        if (instructions.size() > 2 && instructions.get(0).instruction == OUT && instructions.get(1) instanceof StringInsnNode)
        {
            List<InsnNode> list = new ArrayList<>(instructions.subList(2, instructions.size()));
            list.add(0, instructions.get(0));
            assembleBody(assembly, list);
            assembly.add(0, ((StringInsnNode) instructions.get(1)).value);
        }
        else
        {
            assembleBody(assembly, instructions);
        }

        if (assembly.size() == 3 && assembly.get(1) == EQ)
        {
            DoubleConsumer l = (DoubleConsumer) assembly.get(0);
            DoubleSupplier r = (DoubleSupplier) assembly.get(2);
            append(() -> l.accept(r.getAsDouble()));
        }
        else if (assembly.size() == 5 && assembly.get(1) == AT && (assembly.get(3) == TO || assembly.get(3) == RTO))
        {
            Var l = (Var) assembly.get(0);
            DoubleSupplier r = (DoubleSupplier) assembly.get(4);
            DoubleSupplier delta = (DoubleSupplier) assembly.get(2);

            if (assembly.get(3) == RTO)
            {
                append(() -> l.accept(FskMath.interpolateRot(l.getAsDouble(), r.getAsDouble(), delta.getAsDouble())));
            }
            else
            {
                append(() -> l.accept(FskMath.interpolate(l.getAsDouble(), r.getAsDouble(), delta.getAsDouble())));
            }
        }
        else if (assembly.size() == 2 && assembly.get(0) instanceof String && assembly.get(1) instanceof DoubleArray)
        {
            String channel = (String) assembly.get(0);
            DoubleArray array = (DoubleArray) assembly.get(1);
            append(() -> outputChannel.accept(channel, array.get()));
        }
    }

    private void assembleBody(List<Object> assembly, List<InsnNode> insnList)
    {
        InsnNode prev = null;
        boolean neg = false;

        for (int i = 0; i < insnList.size(); ++i)
        {
            InsnNode node = insnList.get(i);
            Instruction insn = node.instruction;

            if (node.instruction == NEG)
            {
                neg = true;
                prev = node;
                continue;
            }
            else if (node.instruction.isFunction(f -> f.argNum > 1))
            {
                assembly.add(node.instruction);
                prev = node;
                continue;
            }
            else if (node.instruction.isFunction())
            {
                assembly.add(node.instruction);
                assembly.add(neg);
            }
            else if (node.instruction == DEG)
            {
                assembly.add(MUL);
                assembly.add(new Const(Math.toRadians(1)));
            }
            else if (node instanceof VarInsnNode)
            {
                Var var = vars[((VarInsnNode) node).var];

                if (neg)
                {
                    var = var.invert();
                }

                assembly.add(var);
            }
            else if (node instanceof ConstInsnNode)
            {
                assembly.add(new Const(((ConstInsnNode) node).value));
            }
            else if (node.instruction.isValue())
            {
                if (neg)
                {
                    assembly.add(new Const(-insn.value()));
                }
                else
                {
                    assembly.add(new Const(insn.value()));
                }
            }
            else if (node.instruction == BST && node instanceof BracketInsnNode)
            {
                int index = ((BracketInsnNode) node).index;
                int end = -1;

                for (int j = i + 1; j < insnList.size(); ++j)
                {
                    node = insnList.get(j);

                    if (node.instruction == BND && node instanceof BracketInsnNode && ((BracketInsnNode) node).index == index)
                    {
                        end = j;
                        break;
                    }
                }

                if (end > -1)
                {
                    List<Object> subAssembly = new ArrayList<>();
                    InsnFunction f = null;

                    assembleBody(subAssembly, insnList.subList(i + 1, end));

                    if (prev != null && (prev.instruction == OUT || prev.instruction.isFunction() && (f = prev.instruction.function()).argNum > 1 && subAssembly.size() > 1) && (subAssembly.size() & 1) == 1)
                    {
                        DoubleArray array = compileArray(subAssembly);
                        subAssembly.clear();

                        if (f != null)
                        {
                            if (array.isConstant())
                            {
                                subAssembly.add(new Const(f.apply(array.get())));
                            }
                            else
                            {
                                InsnFunction func = f;
                                subAssembly.add((DoubleSupplier) () -> func.apply(array.get()));
                            }
                        }
                        else
                        {
                            subAssembly.add(array);
                        }

                        assembly.remove(assembly.size() - 1);
                    }

                    if (neg)
                    {
                        assembly.add(new Const(-1));
                        assembly.add(MUL);
                    }

                    assembly.addAll(subAssembly);
                    i = end;
                }
            }
            else if (node.instruction != STR)
            {
                assembly.add(node.instruction);
            }

            prev = node;
            neg = false;
        }

        for (int i = assembly.size() - 1; i >= 0; --i)
        {
            Object obj = assembly.get(i);
            Instruction insn;
            InsnFunction f;

            if (obj instanceof Instruction && (insn = (Instruction) obj).isFunction() && (f = insn.function()).argNum == 1)
            {
                neg = (Boolean) assembly.get(i + 1);
                assembly.remove(i + 1);
                Object next = assembly.get(i + 1);

                if (next instanceof Const)
                {
                    assembly.set(i, new Const((neg ? -1 : 1) * f.apply(((Const) next).value)));
                }
                else if (neg)
                {
                    assembly.set(i, (DoubleSupplier) () -> -f.apply(((DoubleSupplier) next).getAsDouble()));
                }
                else
                {
                    assembly.set(i, (DoubleSupplier) () -> f.apply(((DoubleSupplier) next).getAsDouble()));
                }

                assembly.remove(i + 1);
            }
        }

        for (List<?> list : OP_ORDER)
        {
            for (int i = 0; i < assembly.size(); ++i)
            {
                Object obj = assembly.get(i);

                if (list.contains(obj))
                {
                    Object r = assembly.get(i + 1);

                    if (obj == NOT)
                    {
                        assembly.remove(i);

                        if (r instanceof Const)
                        {
                            assembly.set(i, new Const(((Const) r).value == 1 ? 0 : 1));
                        }
                        else
                        {
                            assembly.set(i, (DoubleSupplier) () -> ((DoubleSupplier) r).getAsDouble() == 1 ? 0 : 1);
                        }

                        continue;
                    }

                    DoubleBinaryOperator op = ((Instruction) obj).operator();
                    Object l = assembly.get(i - 1);
                    assembly.remove(i - 1);
                    assembly.remove(i);

                    if (l instanceof Const && r instanceof Const)
                    {
                        assembly.set(--i, new Const(op.applyAsDouble(((Const) l).value, ((Const) r).value)));
                    }
                    else
                    {
                        assembly.set(--i, (DoubleSupplier) () -> op.applyAsDouble(((DoubleSupplier) l).getAsDouble(), ((DoubleSupplier) r).getAsDouble()));
                    }
                }
            }
        }
    }

    private DoubleArray compileArray(List<Object> subAssembly)
    {
        List<Object> args = new ArrayList<>();
        boolean consts = true;

        for (int j = 0; j < subAssembly.size(); j += 2)
        {
            Object obj = subAssembly.get(j);

            if (!(obj instanceof Const))
            {
                consts = false;
            }

            args.add(obj);
        }

        if (consts)
        {
            double[] array = args.stream().mapToDouble(t -> ((Const) t).value).toArray();
            return new DoubleArray(() -> array, true);
        }
        else
        {
            DoubleSupplier[] args1 = args.toArray(new DoubleSupplier[0]);
            double[] array = new double[args1.length];
            return new DoubleArray(() ->
            {
                for (int j = 0; j < array.length; ++j)
                {
                    array[j] = args1[j].getAsDouble();
                }

                return array;
            }, false);
        }
    }

    private static class Const implements DoubleSupplier
    {
        private final double value;

        private Const(double value)
        {
            this.value = value;
        }

        @Override
        public double getAsDouble()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return String.format("Const[%s]", value);
        }
    }

    private static class DoubleArray
    {
        private final Supplier<double[]> value;
        private final boolean constant;

        private DoubleArray(Supplier<double[]> value, boolean constant)
        {
            this.value = value;
            this.constant = constant;
        }

        public double[] get()
        {
            return value.get();
        }

        public boolean isConstant()
        {
            return constant;
        }

        @Override
        public String toString()
        {
            return String.format("DoubleArray[%s]", value);
        }
    }
}
