/* Copyright (c) 2010 Richard Chan */
package reader;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import reader.common.KeyDataReader;
import exception.ModelException;
import exception.ReaderException;

public class ProblemGradesReader extends KeyDataReader {
    
    private static final String EMAIL = "EMAIL";

    public class ProblemGradesheetRow {
        public final String email;
        public final Map<String, Double> grades;
        public ProblemGradesheetRow(String email, Map<String, Double> grades) {
            this.email = email;
            this.grades = grades;
        }
    }
    
    /**
     * @throws ReaderException if sheet missing keys or IOException.
     */
    public ProblemGradesReader(Reader r) throws ReaderException {
        super(r);
        
        try {
            Collection<String> keys = getKeys();
            if (!(keys.contains(EMAIL))) {
                throw new ReaderException("Problem grade sheet missing keys.", true);
            }
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
    /**
     * @throws ReaderException if row missing EMAIL or IOException.
     * @throws ModelException if grade key incorrect or value NaN.
     */
    public ProblemGradesheetRow getNextRow() throws ReaderException {
        try {
            Map<String, String> row = getNextDataRow();
            if (row == null) return null;
            if (row.containsKey(EMAIL)) {
                Map<String, Double> grades = new HashMap<String, Double>();
                for (String gradeKey : row.keySet()) {
                    if (!EMAIL.equals(gradeKey)) {
                        try {
                            grades.put(gradeKey, Double.valueOf(row.get(gradeKey)));
                        } catch(NumberFormatException e) {
                            System.out.println("WARN: Problem grade sheet contains NaN grade (" + row.get(gradeKey) + ") for row " + row.get(EMAIL) + ".");
                        }
                    }
                }
                return new ProblemGradesheetRow(row.get(EMAIL), grades);
            }
            throw new ReaderException("Gradesheet has row with missing columns.", false);
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
}
