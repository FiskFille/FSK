package com.fiskmods.fsk.insn;

public class ConstInsnNode extends InsnNode
{
    public final double value;

    public ConstInsnNode(double d)
    {
        super(Instruction.CST);
        value = d;
    }

    @Override
    public boolean isValue(int dir)
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]", instruction, value);
    }
}
