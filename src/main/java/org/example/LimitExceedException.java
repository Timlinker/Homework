package org.example;

public class LimitExceedException extends RuntimeException
{
    public LimitExceedException(String message)
    {
        super(message);
    }
}
