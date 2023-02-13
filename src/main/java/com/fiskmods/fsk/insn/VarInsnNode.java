package com.fiskmods.fsk.insn;

public class VarInsnNode extends InsnNode
{
    public final int var;

    public VarInsnNode(int i)
    {
        super(Instruction.VAR);
        var = i;
    }

    @Override
    public boolean isValue(int dir)
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]", instruction, var);
    }
}
