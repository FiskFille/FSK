package com.fiskmods.fsk;

import static com.fiskmods.fsk.insn.Instruction.*;

import java.util.ArrayList;
import java.util.List;

import com.fiskmods.fsk.insn.BracketInsnNode;
import com.fiskmods.fsk.insn.ConstInsnNode;
import com.fiskmods.fsk.insn.InsnNode;

public class Reconstructor
{
    public static void reconstruct(List<InsnNode> instructions)
    {
        List<InsnNode> insn = new ArrayList<>(instructions);
        instructions.clear();

        for (int i = 0; i < insn.size(); ++i)
        {
            if (insn.get(i).instruction == NL)
            {
                List<InsnNode> line = insn.subList(0, i + 1);
                insn = insn.subList(i + 1, insn.size());
                instructions.addAll(reconstructLine(new ArrayList<>(line)));
                i = 0;
            }
        }
    }

    private static List<InsnNode> reconstructLine(List<InsnNode> line)
    {
        for (int i = 0; i < line.size(); ++i)
        {
            InsnNode node = line.get(i);

            if (i + 1 < line.size())
            {
                InsnNode next = line.get(i + 1);

                if (next.instruction == EQ && node.isOperation())
                {
                    List<InsnNode> bracket;
                    line.remove(i);
                    line.add(i + 1, new BracketInsnNode(BST, -1));
                    line.add(i + 1, node);
                    line.addAll(i + 1, bracket = new ArrayList<>(line.subList(0, i)));
                    line.add(line.size() - 1, new BracketInsnNode(BND, -1));
                    i += 1 + bracket.size();
                }
                else if (node.instruction == SUB && next.isValue(1))
                {
                    int j = i;

                    while (--j > -1 && line.get(j).instruction == SUB);
                    node = line.get(j);

                    if (node.isOperation() || node.isValue(-1))
                    {
                        ++j;
                    }

                    line.subList(j + 1, i + 1).clear();
                    j = i - j;
                    i -= j;

                    if (j % 2 == 1)
                    {
                        if (next instanceof ConstInsnNode)
                        {
                            line.set(i + 1, new ConstInsnNode(-((ConstInsnNode) next).value));
                        }
                        else
                        {
                            line.add(++i, new InsnNode(NEG));
                        }
                    }
                }
                else if (next.isValue(1) && node.isValue(-1))
                {
                    ConstInsnNode cst;

                    if (next instanceof ConstInsnNode && (cst = (ConstInsnNode) next).value < 0)
                    {
                        line.set(i + 1, new ConstInsnNode(-cst.value));
                        line.add(i + 1, new InsnNode(SUB));
                    }
                    else
                    {
                        line.add(i + 1, new InsnNode(MUL));
                    }
                }
            }
        }

        return line;
    }
}
