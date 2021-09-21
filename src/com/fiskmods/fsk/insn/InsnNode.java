package com.fiskmods.fsk.insn;

import static com.fiskmods.fsk.insn.Instruction.*;

public class InsnNode
{
    public final Instruction instruction;

    public InsnNode(Instruction insn)
    {
        instruction = insn;
    }

    public boolean isValue(int dir)
    {
        return instruction.value != null || dir > 0 && instruction.function != null;
    }

    public boolean isOperation()
    {
        return instruction == ADD || instruction == SUB || instruction == MUL || instruction == DIV || instruction == POW || instruction == MOD;
    }

    @Override
    public String toString()
    {
        return instruction.toString();
    }
}
