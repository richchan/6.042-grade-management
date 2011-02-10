/* Copyright (c) 2010 Richard Chan */
package model;

import helper.Helpers;
import exception.ModelException;

public class Grade {
    
    public static Helpers.CallbackWithReturn<GradeType, Double> defaultGradeValueSetter = null;
    
    public final String key;
    public final GradeType gradeType;
    public final boolean isExcused;
    public double value;
    public final double initialValue;
    public boolean dropped = false;
    
    /**
     * @throws ModelException if grade type is invalid.
     */
    public Grade(String key, double value, boolean isExcused, boolean isDropped) throws ModelException {
        this.key = key;
        this.gradeType = Constants.getGradeType(key);
        if (this.gradeType == null) {
            throw new ModelException("Grade type invalid: " + key);
        }
        this.value = value;
        this.initialValue = value;
        this.isExcused = isExcused;
    }
    
    /**
     * @throws ModelException if grade type is invalid or if NaN.
     */
    public Grade(String key, String gradeString) throws ModelException {
        this.key = key;
        this.gradeType = Constants.getGradeType(key);
        if (this.gradeType == null) {
            throw new ModelException("Grade type invalid: " + key);
        }
        if ("E".equals(gradeString)) {
            if (defaultGradeValueSetter == null) {
                this.value = 0.0;
            } else {
                this.value = defaultGradeValueSetter.call(gradeType);
            }
            this.isExcused = true;
        } else {
            try {
                this.value = Double.parseDouble(gradeString);
            } catch(NumberFormatException e) {
                throw new ModelException("Grade value not a number: " + gradeString);
            }
            this.isExcused = false;
        }
        this.initialValue = this.value;
    }
    
}
