/* Copyright (c) 2010 Richard Chan */
package reader;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import model.Student;

import exception.ReaderException;

import reader.common.KeyDataReader;

public class ClasslistReader extends KeyDataReader {

    private static final String EMAIL = "EMAIL";
    private static final String FIRST = "FIRST";
    private static final String LAST = "LAST";
    private static final String STATUS = "STATUS";
    private static final String TA = "TA";

    public class ClasslistRow {
        public final Student student;
        public ClasslistRow(String email, String first, String last, String ta, boolean isDropped) {
            this.student = new Student(email, first, last, ta, isDropped);
        }
    }
    
    /**
     * @throws ReaderException if sheet missing keys or IOException.
     */
    public ClasslistReader(Reader r) throws ReaderException {
        super(r);
        try {
            Collection<String> keys = getKeys();
            if (!(keys.contains(EMAIL) &&
                  keys.contains(FIRST) &&
                  keys.contains(LAST) &&
                  keys.contains(TA) &&
                  keys.contains(STATUS))) {
                throw new ReaderException("Classlist missing keys.", true);
            }
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
    /**
     * @throws ReaderException if row missing EMAIL, FIRST, or LAST or IOException.
     */
    public ClasslistRow getNextRow() throws ReaderException {
        try {
            Map<String, String> row = getNextDataRow();
            if (row == null) return null;
            if (row.containsKey(EMAIL) &&
              row.containsKey(FIRST) &&
              row.containsKey(LAST) &&
              row.containsKey(TA)) {
                String s = row.get(STATUS);
                if (s == null) s = "";
                return new ClasslistRow(row.get(EMAIL), row.get(FIRST), row.get(LAST), row.get(TA), "DROP".equals(s.trim().toUpperCase()));
            }
            throw new ReaderException("Classlist has row with missing columns.", false);
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
}
