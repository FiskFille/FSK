package com.fiskmods.fsk;

import static com.fiskmods.fsk.insn.Instruction.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import com.fiskmods.fsk.insn.BracketInsnNode;
import com.fiskmods.fsk.insn.ConstInsnNode;
import com.fiskmods.fsk.insn.InsnFunction;
import com.fiskmods.fsk.insn.InsnNode;
import com.fiskmods.fsk.insn.Instruction;
import com.fiskmods.fsk.insn.VarInsnNode;

public class Script
{
    final List<String> lookup;
    final List<InsnNode> instructions;

    private final String[] varNames;
    private final Var[] vars;

    private Runnable assembledScript;

    public Script(List<InsnNode> instructions, List<String> lookup)
    {
        this.instructions = instructions;
        this.lookup = lookup;

        varNames = lookup.toArray(new String[0]);
        vars = new Var[varNames.length];

        for (int i = 0; i < vars.length; ++i)
        {
            vars[i] = new Var(varNames[i], i, 0);
        }

        assembleLine();
    }

    public Var[] getVars()
    {
        return vars;
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

    public Script print()
    {
        for (int i = 0; i < vars.length; ++i)
        {
            System.out.println(varNames[i] + ": " + vars[i].getAsDouble());
        }

        return this;
    }

    private Script assembleLine()
    {
        List<InsnNode> list = instructions;
        assembledScript = () ->
        {
        };

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

        return this;
    }

    private void assembleLine(List<InsnNode> instructions)
    {
        List<Object> assembly = new ArrayList<>();
        assembleBody(assembly, instructions);

        if (assembly.size() == 3 && assembly.get(1) == EQ)
        {
            DoubleConsumer l = (DoubleConsumer) assembly.get(0);
            DoubleSupplier r = (DoubleSupplier) assembly.get(2);
            Runnable runnable = assembledScript;
            assembledScript = () ->
            {
                runnable.run();
                l.accept(r.getAsDouble());
            };
        }
        else if (assembly.size() == 5 && assembly.get(1) == AT && assembly.get(3) == TO)
        {
            Var l = (Var) assembly.get(0);
            DoubleSupplier r = (DoubleSupplier) assembly.get(4);
            DoubleSupplier delta = (DoubleSupplier) assembly.get(2);
            Runnable runnable = assembledScript;
            assembledScript = () ->
            {
                runnable.run();
                l.accept(FskMath.interpolate(l.getAsDouble(), r.getAsDouble(), delta.getAsDouble()));
            };
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
                    InsnFunction f;

                    assembleBody(subAssembly, insnList.subList(i + 1, end));

                    if (prev != null && prev.instruction.isFunction() && (f = prev.instruction.function()).argNum > 1 && subAssembly.size() > 1 && (subAssembly.size() & 1) == 1)
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

                        subAssembly.clear();

                        if (consts)
                        {
                            Double[] array = args.stream().map(t -> ((Const) t).value).toArray(Double[]::new);
                            subAssembly.add(new Const(f.apply(array)));
                        }
                        else
                        {
                            DoubleSupplier[] args1 = args.toArray(new DoubleSupplier[0]);
                            Double[] array = new Double[args1.length];
                            subAssembly.add((DoubleSupplier) () ->
                            {
                                for (int j = 0; j < array.length; ++j)
                                {
                                    array[j] = args1[j].getAsDouble();
                                }

                                return f.apply(array);
                            });
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
            else
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
                Object next = assembly.get(i + 1);

                if (next instanceof Const)
                {
                    assembly.set(i, new Const(f.apply(((Const) next).value)));
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
                    Instruction op = (Instruction) obj;
                    Object l = assembly.get(i - 1);
                    Object r = assembly.get(i + 1);
                    assembly.remove(i - 1);
                    assembly.remove(i);

                    if (l instanceof Const && r instanceof Const)
                    {
                        assembly.set(--i, new Const(operate(op, ((Const) l).value, ((Const) r).value)));
                    }
                    else
                    {
                        assembly.set(--i, (DoubleSupplier) () -> operate(op, ((DoubleSupplier) l).getAsDouble(), ((DoubleSupplier) r).getAsDouble()));
                    }
                }
            }
        }
    }

    private double operate(Instruction insn, double l, double r)
    {
        switch (insn)
        {
        case ADD:
            return l + r;
        case SUB:
            return l - r;
        case MUL:
            return l * r;
        case DIV:
            return l / r;
        case POW:
            return Math.pow(l, r);
        case MOD:
            return l % r;
        // case AND:
        // return l == 1 && r == 1 ? 1 : 0;
        // case OR:
        // return l == 1 || r == 1 ? 1 : 0;
        default:
            return 0;
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
}
