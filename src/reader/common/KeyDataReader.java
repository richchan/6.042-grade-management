/* Copyright (c) 2010 Richard Chan */
package reader.common;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;



import exception.ReaderException;

public class KeyDataReader {
    
    private final String KEY_ROW_STARTS_WITH = "META";
    private final String DATA_ROW_STARTS_WITH = "DATA";
    
    private final CSVScanReader reader;
    
    private Map<String, Integer> columnKeys = null;
    
    public KeyDataReader(Reader r) {
        reader = new CSVScanReader(r);
    }
    
    /**
     * @return All keys in this sheet. In upper case.
     * @throws ReaderException if no KEY row can be found.
     */
    public Collection<String> getKeys() throws IOException, ReaderException {
        if (columnKeys == null) {
            String[] keyRow = reader.scanNextRow(KEY_ROW_STARTS_WITH);
            if (keyRow == null) {
                throw new ReaderException("KEY row cannot be found.", true);
            } else {
                columnKeys = new HashMap<String, Integer>();
                for (int i = keyRow.length - 1; i > 0; i--) {
                    String key = keyRow[i];
                    if (key != null && !("".equals(key)))
                        columnKeys.put(key.trim().toUpperCase(), i);
                }
            }
        }
        return columnKeys.keySet();
    }
    
    /**
     * @return map from column keys to values.
     * @throws ReaderException if called before getKeys().
     */
    public Map<String, String> getNextDataRow() throws IOException, ReaderException {
        if (columnKeys == null) {
            throw new ReaderException("Called before getKeys()", true);
        }
        Map<String, String> dataRow = new HashMap<String, String>();
        String[] nextRow = reader.scanNextRow(DATA_ROW_STARTS_WITH);
        if (nextRow == null) return null;
        for (Entry<String,Integer> e : columnKeys.entrySet()) {
            int column = e.getValue();
            if (nextRow.length > column) {
                String value = nextRow[column];
                if (null != value && !value.trim().equals(""))
                    dataRow.put(e.getKey(), value);
            }
        }
        return dataRow;
    }
    
}
