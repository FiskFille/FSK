package com.fiskmods.fsk;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class Var implements DoubleSupplier, DoubleConsumer, BooleanSupplier, Consumer<Boolean>
{
    public final String name;
    public final int index;

    private double value;

    public Var(String name, int index, double value)
    {
        this.name = name;
        this.index = index;
        this.value = value;
    }

    @Override
    public double getAsDouble()
    {
        return value;
    }

    @Override
    public void accept(double value)
    {
        this.value = value;
    }

    @Override
    public boolean getAsBoolean()
    {
        return getAsDouble() == 1;
    }

    @Override
    public void accept(Boolean value)
    {
        accept(value ? 1 : 0);
    }

    public void not()
    {
        accept(!getAsBoolean());
    }

    public Var invert()
    {
        return new NegVar();
    }

    @Override
    public String toString()
    {
        return String.format("Var[%s|\"%s\"=%s]", index, name, value);
    }

    private class NegVar extends Var
    {
        public NegVar()
        {
            super(Var.this.name, Var.this.index, 0);
        }

        @Override
        public double getAsDouble()
        {
            return -Var.this.getAsDouble();
        }

        @Override
        public void accept(double value)
        {
            Var.this.accept(value);
        }

        @Override
        public Var invert()
        {
            return Var.this;
        }

        @Override
        public String toString()
        {
            return Var.this.toString();
        }
    }
}
