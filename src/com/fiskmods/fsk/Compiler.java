package com.fiskmods.fsk;

import static com.fiskmods.fsk.insn.Instruction.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fiskmods.fsk.insn.*;

public class Compiler
{
    private static final Pattern VAR_ = Pattern.compile("^(\\{(.+?)})");
    private static final Pattern CONST_ = Pattern.compile("^((?:\\d*\\.\\d+|\\d+)(?:'|)).*");
    private static final Pattern STRING_ = Pattern.compile("^(\"(.*?)\")");
    private static final Pattern FUNC_;

    static
    {
        StringJoiner s = new StringJoiner("|");
        FUNCTION_NAMES.forEach(s::add);
        FUNC_ = Pattern.compile("^(" + s + ")(?:\\W|\\d|$).*");
    }

    private final List<String> lookup = new LinkedList<>();
    private final List<InsnNode> instructions = new LinkedList<>();

    private final LinkedList<InsnNode> lineInsn = new LinkedList<>();

    private ScriptScanner scanner;
    private int bracketIndex = -1;
    private int interp;

    private final Stack<Func> currFunc = new Stack<>();

    private Compiler()
    {
    }

    public static Script compile(String script) throws CompilerException
    {
        return new Compiler().compileScript(script);
    }

    private Script compileScript(String script) throws CompilerException
    {
        scanner = new ScriptScanner(script);
        checkBrackets(script);

        for (String line : script.split("\n"))
        {
            int i = line.indexOf(';');

            if (i > -1) // Add to global index the length of the removed comment
            {
                scanner.scan(line.substring(i).length()).advance();
                line = line.substring(0, i);
            }

            i = instructions.size();
            lineInsn.clear();
            compileLine(line);

            if (interp == 1)
            {
                scanner.backtrack();
                unexpectedToken("'->'");
            }

            interp = 0;

            if (i != instructions.size())
            {
                if (!lineInsn.getLast().isValue(-1))
                {
                    scanner.backtrack();
                    unexpectedToken("value (ending token invalid)");
                }

                addInstruction(NL);
            }

            scanner.scan(1).advance(); // Add 1 for the removed nl
        }

        Reconstructor.reconstruct(instructions);
        return new Script(instructions, lookup);
    }

    private void checkBrackets(String script) throws CompilerException
    {
        int balance = 0;
        int line = 1;

        for (int i = 0; i < script.length(); ++i)
        {
            if (script.charAt(i) == '\n')
            {
                ++line;
            }
            else if (script.charAt(i) == '(')
            {
                ++balance;
            }
            else if (script.charAt(i) == ')')
            {
                if (--balance < 0)
                {
                    break;
                }
            }
        }

        if (balance > 0)
        {
            throw new CompilerException(String.format("Unbalanced brackets: missing %s closing ')' on line %s", balance, line));
        }
        else if (balance < 0)
        {
            throw new CompilerException(String.format("Unbalanced brackets: missing %s opening '(' on line %s", -balance, line));
        }
    }

    private void compileLine(String line) throws CompilerException
    {
        int start, balance = 0;

        while ((start = line.indexOf('(')) > -1)
        {
            compileSection(line.substring(0, start));
            line = line.substring(start);

            for (int i = 0; i < line.length(); ++i)
            {
                if (line.charAt(i) == '(')
                {
                    ++balance;
                }
                else if (line.charAt(i) == ')' && --balance == 0)
                {
                    int j = ++bracketIndex;
                    scanner.scan(1);
                    addInstruction(new BracketInsnNode(BST, j));
                    scanner.advance();
                    compileLine(line.substring(1, i));
                    scanner.scan(1);

                    if (!lineInsn.getLast().isValue(-1))
                    {
                        unexpectedToken("value");
                    }

                    addInstruction(new BracketInsnNode(BND, j));
                    scanner.advance();

                    if (!currFunc.isEmpty() && j == currFunc.peek().index)
                    {
                        if (currFunc.peek().args > 1)
                        {
                            scanner.backtrack();
                            incorrectArgs();
                        }

                        currFunc.pop();
                    }

                    line = line.substring(i + 1);
                    break;
                }
            }
        }

        compileSection(line);
    }

    private void compileSection(String line) throws CompilerException
    {
        int i = 0;

        while (i < line.length())
        {
            String s = line.substring(i);
            int j = i;

            if ((i += compileVar(s)) - j != 0) continue;
            if ((i += compileConst(s)) - j != 0) continue;
            if ((i += compileString(s)) - j != 0) continue;
            if ((i += compileFunc(s)) - j != 0) continue;

            // if ((i += compileKeyword(s, "false", ZERO)) - j != 0) continue;
            // if ((i += compileKeyword(s, "true", ONE)) - j != 0) continue;
            if ((i += compileKeyword(s, "pi", PI)) - j != 0) continue;
            if ((i += compileInterpTo(s)) - j != 0) continue;
            if ((i += compileOut(s)) - j != 0) continue;

            char c = line.charAt(i);
            scanner.scan(1);

            if (c == '=')
            {
                if (lineInsn.size() > 1 && !lineInsn.get(1).isOperation())
                {
                    illegalToken();
                }

                addInstruction(EQ);
            }
            else if (c == '@')
            {
                if (lineInsn.size() > 1)
                {
                    illegalToken();
                }

                addInstruction(AT);
                interp = 1;
            }
            else if (c == '\'')
            {
                if (lineInsn.size() > 1 && !lineInsn.getLast().isValue(-1) && lineInsn.getLast().instruction != DEG)
                {
                    illegalToken();
                }

                addInstruction(DEG);
            }
            else if (c == ',')
            {
                if (lineInsn.size() > 1 && !lineInsn.getLast().isValue(-1) || currFunc.isEmpty() && lineInsn.getFirst().instruction != OUT)
                {
                    illegalToken();
                }

                if (!currFunc.isEmpty() && --currFunc.peek().args <= 0)
                {
                    incorrectArgs();
                }

                addInstruction(ARG);
            }
            else if (c == 'e') addInstruction(E);
            else if (c == '+') addInstruction(ADD);
            else if (c == '-') addInstruction(SUB);
            else if (c == '*') addInstruction(MUL);
            else if (c == '/') addInstruction(DIV);
            else if (c == '^') addInstruction(POW);
            else if (c == '%') addInstruction(MOD);
            else if (c != ' ' && c != '\t' && c != '\r')
            {
                for (i = 0; i < s.length(); ++i)
                {
                    if (i + 1 >= s.length() || !s.substring(i, i + 1).matches("\\w"))
                    {
                        scanner.scan(i);
                        throw new CompilerException(String.format("Unknown token '%s' at ", s.substring(0, i + 1)) + scanner.fullTrace());
                    }
                }

                throw new CompilerException(String.format("Unknown token '%s' at ", c) + scanner.fullTrace());
            }

            scanner.advance();
            ++i;
        }
    }

    private int compileMatch(Matcher m, MatcherConsumer c) throws CompilerException
    {
        if (m.find())
        {
            int i = m.group(1).length();
            scanner.scan(i);
            c.accept(m);
            scanner.advance();

            return i;
        }

        return 0;
    }

    private int compileKeyword(String s, String keyword, Instruction insn, ExceptionRunnable r) throws CompilerException
    {
        if (s.startsWith(keyword))
        {
            int i = keyword.length();
            scanner.scan(i);
            r.run();
            addInstruction(insn);
            scanner.advance();

            return i;
        }

        return 0;
    }

    private int compileKeyword(String s, String keyword, Instruction insn) throws CompilerException
    {
        return compileKeyword(s, keyword, insn, () ->
        {
        });
    }

    private int compileVar(String s) throws CompilerException
    {
        return compileMatch(VAR_.matcher(s), m ->
        {
            String s1 = m.group(2);
            int i;

            if ((i = lookup.indexOf(s1)) < 0)
            {
                i = lookup.size();
                lookup.add(s1);
            }

            addInstruction(new VarInsnNode(i));
        });
    }

    private int compileConst(String s) throws CompilerException
    {
        return compileMatch(CONST_.matcher(s), m ->
        {
            String s1 = m.group(1);
            double d;

            if (s1.endsWith("'"))
            {
                d = Math.toRadians(Double.parseDouble(s1.substring(0, s1.length() - 1)));
            }
            else
            {
                d = Double.parseDouble(s1);
            }

            if (d == 0)
            {
                addInstruction(ZRO);
            }
            else if (d == 1)
            {
                addInstruction(ONE);
            }
            else
            {
                addInstruction(new ConstInsnNode(d));
            }
        });
    }

    private int compileString(String s) throws CompilerException
    {
        return compileMatch(STRING_.matcher(s), m ->
        {
            if (lineInsn.isEmpty() || lineInsn.getLast().instruction != OUT)
            {
                illegalToken();
            }

            addInstruction(new StringInsnNode(m.group(2)));
        });
    }

    private int compileFunc(String s) throws CompilerException
    {
        return compileMatch(FUNC_.matcher(s), m ->
        {
            Instruction insn = FUNCTIONS.get(m.group(1));
            InsnFunction f;

            addInstruction(insn);

            if (insn.isFunction() && (f = insn.function()).argNum > 1)
            {
                currFunc.push(new Func(insn, bracketIndex + 1, f.argNum));
            }
        });
    }

    private int compileInterpTo(String s) throws CompilerException
    {
        return compileKeyword(s, "->", TO, () ->
        {
            if (interp != 1 || scanner.script.substring(0, scanner.index()).chars().reduce(0, (res, t) -> res + (t == '(' ? 1 : t == ')' ? -1 : 0)) > 0)
            {
                illegalToken();
            }
            else if (!lineInsn.getLast().isValue(-1))
            {
                unexpectedToken("value");
            }

            interp = 2;
        });
    }

    private int compileOut(String s) throws CompilerException
    {
        return compileKeyword(s, "out", OUT, () ->
        {
            if (!lineInsn.isEmpty())
            {
                illegalToken();
            }
        });
    }

    private void unexpectedToken(String expected) throws CompilerException
    {
        throw new CompilerException(String.format("Unexpected token '%s' at %s: expected ", scanner.currScan(), scanner.address()) + expected + scanner.trace());
    }

    private void illegalToken() throws CompilerException
    {
        throw new CompilerException(String.format("Illegal token '%s' at %s", scanner.currScan(), scanner.address()) + scanner.trace());
    }

    private void incorrectArgs() throws CompilerException
    {
        throw new CompilerException(String.format("Incorrect number of arguments for function '%s' at %s: expected ", currFunc.peek().instruction, scanner.address()) + currFunc.peek().expectedArgs + scanner.trace());
    }

    private void addInstruction(InsnNode node) throws CompilerException
    {
        if ((node.instruction != VAR && node.instruction != OUT) && lineInsn.isEmpty())
        {
            unexpectedToken("variable or statement");
        }
        else if (lineInsn.size() == 1)
        {
            if (lineInsn.getFirst().instruction == OUT)
            {
                if (node.instruction != STR)
                {
                    unexpectedToken("string");
                }
            }
            else if (node.instruction != EQ && node.instruction != AT && !node.isOperation())
            {
                unexpectedToken("assignment");
            }
        }
        else if (lineInsn.size() == 2)
        {
            if (lineInsn.getFirst().instruction == OUT)
            {
                if (node.instruction != BST)
                {
                    unexpectedToken("'('");
                }
            }
            else if (node.instruction != EQ && lineInsn.getLast().instruction != EQ && lineInsn.getLast().instruction != AT)
            {
                unexpectedToken("assignment");
            }
            else if (lineInsn.getLast().instruction == AT && node.instruction != SUB && !node.isValue(1))
            {
                unexpectedToken("value");
            }
        }
        else if (node.instruction != BST && lineInsn.size() > 1 && lineInsn.getLast().instruction.isFunction(f -> f.argNum > 1))
        {
            if (lineInsn.getLast().isValue(1))
            {
                incorrectArgs();
            }

            unexpectedToken("'('");
        }
        else if (node.instruction != SUB && node.isOperation() && !lineInsn.getLast().isValue(-1))
        {
            unexpectedToken("value");
        }

        instructions.add(node);
        lineInsn.add(node);
    }

    private void addInstruction(Instruction insn) throws CompilerException
    {
        addInstruction(new InsnNode(insn));
    }

    private interface MatcherConsumer
    {
        void accept(Matcher m) throws CompilerException;
    }

    private interface ExceptionRunnable
    {
        void run() throws CompilerException;
    }

    private static class Func
    {
        private final Instruction instruction;
        private final int index, expectedArgs;
        private int args;

        public Func(Instruction instruction, int index, int args)
        {
            this.instruction = instruction;
            this.index = index;
            this.args = expectedArgs = args;
        }
    }
}
