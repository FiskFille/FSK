package com.fiskmods.fsk.test;

import com.fiskmods.fsk.Compiler;
import com.fiskmods.fsk.Script;
import com.fiskmods.fsk.Var;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class FskTest
{
    public static void main(String[] args) throws Exception
    {
        String text = read(FskTest.class.getResourceAsStream("/test.fsk"));
        // Compile the text read from file into an FSK script object
        Script script = Compiler.compile(text);

        // Assign variable "in" to 4 before the script is executed
        // We can save this Var object to read what the value of "in" is after execution
        Var var = script.assign("in", 4);

        // Set every "out" call to print to console
        script.addListener((channel, data) ->
                System.out.println(channel + ": " + Arrays.toString(data)));

        // Execute the script
        script.run();

        // Convenience function to print all variables to console
        script.print();

        // Double-check our results by printing the value we got from the Var object
        System.out.println("Value of 'in' is: " + var.getAsDouble());
    }

    public static String read(InputStream in) throws Exception
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
        {
            String s = "";
            for (String line; (line = reader.readLine()) != null; s += line + "\n");
            return s;
        }
    }
}
