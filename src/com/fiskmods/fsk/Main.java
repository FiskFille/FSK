package com.fiskmods.fsk;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.fiskmods.fsk.insn.BracketInsnNode;
import com.fiskmods.fsk.insn.ConstInsnNode;
import com.fiskmods.fsk.insn.InsnNode;
import com.fiskmods.fsk.insn.VarInsnNode;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        String file = readFile(new File("test.fsk"));
        Script s = Compiler.compile(file);
        
        byte[] bytes = toBytes(s);
        writeFile(new File("output.txt"), bytes);
        System.out.println(bytes.length);
        
        System.out.println(s.instructions);
//        System.out.println(s.lookup);
        s.run().print();
    }
    
    public static byte[] toBytes(Script s) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        for (String s1 : s.lookup)
        {
            out.write(s1.getBytes(StandardCharsets.UTF_8));
        }

        int insnSize = s.instructions.stream().mapToInt(t -> t instanceof BracketInsnNode || t instanceof VarInsnNode ? 2 : t instanceof ConstInsnNode ? 9 : 1).sum();
        ByteBuffer buf = ByteBuffer.allocate(out.size() + 4 + insnSize);
        buf.putInt(s.lookup.size());
        buf.put(out.toByteArray());
        
        for (InsnNode node : s.instructions)
        {
            buf.put((byte) node.instruction.ordinal());
            
            if (node instanceof BracketInsnNode)
            {
                buf.put((byte) ((BracketInsnNode) node).index);
            }
            else if (node instanceof VarInsnNode)
            {
                buf.put((byte) ((VarInsnNode) node).var);
            }
            else if (node instanceof ConstInsnNode)
            {
                buf.putDouble(((ConstInsnNode) node).value);
            }
        }
        
        return buf.array();
    }
    
    public static void writeFile(File file, byte[] bytes) throws IOException
    {
        if (!file.exists())
        {
            file.createNewFile();
        }
        
        try (FileOutputStream reader = new FileOutputStream(file))
        {
            reader.write(bytes);
        }
    }
    
    public static String readFile(File file) throws IOException
    {
        if (!file.exists())
        {
            file.createNewFile();
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String s = "";
            for (String line; (line = reader.readLine()) != null; s += line + "\n");
            return s;
        }
    }
}
