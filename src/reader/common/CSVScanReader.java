/* Copyright (c) 2010 Richard Chan */
package reader.common;

import java.io.IOException;
import java.io.Reader;

import au.com.bytecode.opencsv.CSVReader;

public class CSVScanReader {
    
    private final CSVReader reader;

    public CSVScanReader(Reader r) {
        reader = new CSVReader(r);
    }
    
    public String[] scanNextRow(String startsWith) throws IOException {
        for (String[] row = reader.readNext(); row != null; row = reader.readNext()) {
            if (row.length > 0 && row[0].trim().equals(startsWith))
                return row;
        }
        return null;
    }
    
}
