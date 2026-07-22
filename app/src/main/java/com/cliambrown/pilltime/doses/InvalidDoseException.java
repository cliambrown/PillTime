package com.cliambrown.pilltime.doses;

public class InvalidDoseException extends RuntimeException {
    public InvalidDoseException(String message) {
        super(message);
    }
}
