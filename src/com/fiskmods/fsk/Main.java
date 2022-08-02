package com.fiskmods.fsk;

import java.io.*;
import java.util.Arrays;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        String file = readFile(new File("test.fsk"));
        Script s = Compiler.compile(file);
        s.addListener((t, u) -> System.out.println(t + ": " + Arrays.toString(u)));
        s.run().print();
    }

    public static String readFile(File file) throws IOException
    {
        if (file.exists() || file.createNewFile())
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
                String s = "";
                for (String line; (line = reader.readLine()) != null; s += line + "\n");
                return s;
            }
        }

        throw new FileNotFoundException(file.toString());
    }
}
