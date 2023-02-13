package com.fiskmods.fsk.insn;

public class BracketInsnNode extends InsnNode
{
    public final int index;

    public BracketInsnNode(Instruction insn, int i)
    {
        super(insn);
        index = i;
    }

    @Override
    public boolean isValue(int dir)
    {
        return dir > 0 && instruction == Instruction.BST || dir < 0 && instruction == Instruction.BND;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]", instruction, index);
    }
}
