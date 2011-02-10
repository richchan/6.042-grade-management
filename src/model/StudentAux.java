/* Copyright (c) 2010 Richard Chan */
package model;

import java.util.HashMap;
import java.util.Map;

public class StudentAux {
    public Map<String, Grade> actualGrades = null;
    public final Map<GradeType, Double> typeTotals = new HashMap<GradeType, Double>();
    public double grandTotal = 0;
    public int rank = -1;
    
    public StudentAux() {
        for (GradeType t : GradeType.values()) {
            typeTotals.put(t, 0.0);
        }
    }
}