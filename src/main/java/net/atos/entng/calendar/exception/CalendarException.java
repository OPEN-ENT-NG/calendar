package net.atos.entng.calendar.exception;

public class CalendarException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    
    public CalendarException() {
        
    }
    
    public CalendarException(String message) {
        super(message);
    }
    
    public CalendarException(Throwable cause) {
        super(cause);
    }
    
    public CalendarException(String message, Throwable cause) {
        super(message, cause);
    }
}
