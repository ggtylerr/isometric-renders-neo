package com.glisco.isometricrenders.exo.impl;

public class ExoStateException extends IllegalStateException {

    public ExoStateException(String s) {
        super(s);
    }

    public ExoStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExoStateException(Throwable cause) {
        super(cause);
    }
}
