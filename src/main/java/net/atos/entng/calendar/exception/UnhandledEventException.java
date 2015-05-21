package net.atos.entng.calendar.exception;

public class UnhandledEventException extends CalendarException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public UnhandledEventException(String message) {
        super(message);
    }
    
    public UnhandledEventException(Throwable cause) {
        super(cause);
    }
    
    public UnhandledEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
