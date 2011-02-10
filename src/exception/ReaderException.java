/* Copyright (c) 2010 Richard Chan */
package exception;

public class ReaderException extends Exception {
    private static final long serialVersionUID = 1886406035151977242L;
    public final boolean severe;
    public ReaderException(String msg, boolean severe) {
        super(msg);
        this.severe = severe;
    }
}
