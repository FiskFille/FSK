package com.fiskmods.fsk.insn;

public class StringInsnNode extends InsnNode
{
    public final String value;

    public StringInsnNode(String s)
    {
        super(Instruction.STR);
        value = s;
    }

    @Override
    public boolean isValue(int dir)
    {
        return false;
    }

    @Override
    public String toString()
    {
        return String.format("%s[\"%s\"]", instruction, value);
    }
}
