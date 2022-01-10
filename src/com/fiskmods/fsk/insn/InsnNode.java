package com.fiskmods.fsk.insn;

import static com.fiskmods.fsk.insn.Instruction.DEG;
import static com.fiskmods.fsk.insn.Instruction.NOT;

public class InsnNode
{
    public final Instruction instruction;

    public InsnNode(Instruction insn)
    {
        instruction = insn;
    }

    public boolean isValue(int dir)
    {
        return instruction.isValue() || dir > 0 && (instruction.isFunction() || instruction == NOT) || dir < 0 && instruction == DEG;
    }

    @Override
    public String toString()
    {
        return instruction.toString();
    }
}
