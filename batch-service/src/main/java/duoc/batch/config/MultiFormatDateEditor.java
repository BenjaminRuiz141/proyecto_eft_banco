package duoc.batch.config;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MultiFormatDateEditor extends PropertyEditorSupport {

    private static final String[] FORMATS = new String[] {
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "dd/MM/yyyy"
    };

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().isEmpty()) {
            setValue(null);
            return;
        }

        String cleaned = text.trim();
        for (String format : FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                Date parsedDate = sdf.parse(cleaned);
                setValue(parsedDate);
                return;
            } catch (ParseException ignored) {
                // Intentar con el siguiente patron de fecha
            }
        }

        throw new IllegalArgumentException("Formato de fecha invalido en registro batch: " + text);
    }

    @Override
    public String getAsText() {
        Date value = (Date) getValue();
        return (value != null ? new SimpleDateFormat("yyyy-MM-dd").format(value) : "");
    }
}
