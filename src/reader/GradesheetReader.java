/* Copyright (c) 2010 Richard Chan */
package reader;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import model.Grade;
import reader.common.KeyDataReader;
import exception.ModelException;
import exception.ReaderException;

public class GradesheetReader extends KeyDataReader {
    
    private static final String EMAIL = "EMAIL";
    private final String f;

    public class GradesheetRow {
        public final String email;
        public final Map<String, Grade> grades;
        public GradesheetRow(String email, Map<String, Grade> grades) {
            this.email = email;
            this.grades = grades;
        }
    }
    
    /**
     * @throws ReaderException if sheet missing keys or IOException.
     */
    public GradesheetReader(String f, Reader r) throws ReaderException {
        super(r);
        this.f = f;
        try {
            Collection<String> keys = getKeys();
            if (!(keys.contains(EMAIL))) {
                throw new ReaderException("Gradesheet missing keys.", true);
            }
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
    /**
     * @throws ReaderException if row missing EMAIL or IOException.
     */
    public GradesheetRow getNextRow() throws ReaderException {
        try {
            Map<String, String> row = getNextDataRow();
            if (row == null) return null;
            if (row.containsKey(EMAIL)) {
                Map<String, Grade> grades = new HashMap<String, Grade>();
                for (String gradeKey : row.keySet()) {
                    if (!EMAIL.equals(gradeKey)) {
                        try {
                            grades.put(gradeKey, new Grade(gradeKey, row.get(gradeKey)));
                        } catch (ModelException e) {
                            System.out.println("WARN: [" + f + "]: " + e.getMessage());
                            continue;
                        }
                    }
                }
                return new GradesheetRow(row.get(EMAIL), grades);
            }
            throw new ReaderException("Gradesheet has row with missing columns.", false);
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
}
