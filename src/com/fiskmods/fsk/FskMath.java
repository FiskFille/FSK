package com.fiskmods.fsk;

public class FskMath
{
    public static double interpolate(double a, double b, double progress)
    {
        return a + (b - a) * progress;
    }

    public static double curveCrests(double d)
    {
        return Math.sin(d * Math.PI / 2);
    }

    public static double curve(double d)
    {
        return (curveCrests(d * 2 - 1) + 1) / 2;
    }

    public static double ifElse(double condition, double a, double b)
    {
        return condition == 1 ? a : b;
    }

    public static double logn(double base, double d)
    {
        return Math.log(d) / Math.log(base);
    }

    public static double root(double d, double num)
    {
        return Math.pow(d, 1 / num);
    }

    public static double clamp(double d, double min, double max)
    {
        return Math.min(Math.max(d, min), max);
    }

    public static double animate(double frame, double duration, double frameStart)
    {
        return frame >= frameStart && frame <= frameStart + duration ? (frame - frameStart) / duration : 0;
    }

    public static double animate(double frame, double duration, double frameStart, double fadeIn, double fadeOut)
    {
        fadeIn = clamp(fadeIn, 0, duration);
        fadeOut = clamp(fadeOut, 0, duration - fadeIn);

        if (frame >= frameStart && frame <= frameStart + duration)
        {
            double pos = frame - frameStart;

            if (pos > fadeIn && frame < duration - fadeOut)
            {
                return 1;
            }
            else if (pos > fadeIn)
            {
                frame = frameStart + duration - pos;
                fadeIn = fadeOut;
            }

            return animate(frame, duration, frameStart) * duration / fadeIn;
        }

        return 0;
    }

    public static double wrapAngleTo180(double value)
    {
        value %= 360;

        if (value >= 180)
        {
            value -= 360;
        }

        if (value < -180)
        {
            value += 360;
        }

        return value;
    }
}
