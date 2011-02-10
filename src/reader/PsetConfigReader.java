/* Copyright (c) 2010 Richard Chan */
package reader;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import exception.ReaderException;

import reader.common.KeyDataReader;

public class PsetConfigReader extends KeyDataReader {

    private static final String PS = "PS";
    private static final String SRC = "MQ";

    public class PsetConfigRow {
        public final String ps;
        public final String src;
        public PsetConfigRow(String ps, String src) {
            this.ps = ps;
            this.src = src;
        }
    }
    
    /**
     * @throws ReaderException if sheet missing keys or IOException.
     */
    public PsetConfigReader(Reader r) throws ReaderException {
        super(r);
        
        try {
            Collection<String> keys = getKeys();
            if (!(keys.contains(PS) &&
                  keys.contains(SRC))) {
                throw new ReaderException("PsetConfig missing keys.", true);
            }
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
    /**
     * @throws ReaderException if row missing PS or MQ or IOException.
     */
    public PsetConfigRow getNextRow() throws ReaderException {
        try {
            Map<String, String> row = getNextDataRow();
            if (row == null) return null;
            if (row.containsKey(PS) && row.containsKey(SRC)) {
                return new PsetConfigRow(row.get(PS).toUpperCase(), row.get(SRC).toUpperCase());
            }
            throw new ReaderException("PsetConfig missing keys.", false);
        } catch(IOException e) {
            throw new ReaderException(e.getMessage(), true);
        }
    }
    
}
