package com.fiskmods.fsk;

public class ScriptScanner
{
    public final String script;
    
    private int scanIndex;
    private int scanLength;
    
    public ScriptScanner(String script)
    {
        this.script = script;
    }
    
    public ScriptScanner scan(int length)
    {
        scanLength = length;
        return this;
    }
    
    public void advance()
    {
        scanIndex += scanLength;
    }
    
    public void backtrack()
    {
        scanIndex -= scanLength;
    }
    
    public int index()
    {
        return scanIndex;
    }
    
    public String address()
    {
        String s = script.substring(0, scanIndex);
        int line = 1, i;
        
        while ((i = s.indexOf('\n')) != -1)
        {
            s = s.substring(i + 1);
            ++line;
        }

        return String.format("line %s, column %s", line, s.length() + 1);
    }
    
    public String trace()
    {
        String s = script;
        int i, index = scanIndex;
        
        if ((i = s.indexOf('\n', index)) > -1)
        {
            s = s.substring(0, i);
        }
        
        if ((i = s.lastIndexOf('\n')) > -1)
        {
            s = s.substring(i + 1);
            index -= i + 1;
        }
        
        int start = Math.max(index - 64, 0);
        String s1 = s.substring(start, Math.min(index + 64, s.length()));
        String s2 = "";

        for (i = 0, s2 += "\n"; i < 64 - index + start; ++i, s2 += " ");
        for (i = 0, s2 += s1 + "\n"; i < 64; ++i, s2 += " ");
        return s2 + "^";
    }
    
    public String fullTrace()
    {
        return address() + trace();
    }
    
    public String currScan()
    {
        return script.substring(scanIndex, scanIndex + scanLength);
    }
}
