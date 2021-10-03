import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CorrectDateTime {

    public static final String TIME = "(HH:mm:ss) ";
    public static final String DATE = "d MMMM, yy  HH:mm";

    protected static String getCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(TIME);
        return dtf.format(LocalDateTime.now());
    }

    protected static String getDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE);
        return dtf.format(LocalDateTime.now());
    }
}
