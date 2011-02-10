/* Copyright (c) 2010 Richard Chan */
package model;

public class Constants {

    public static Double getGradeTypeMax(GradeType t) {
        switch (t) {
        case CP:
//        case R:
            return 2.0;
        case F:
            return 100.0;
        case MQ:
//        case Q:
            return 20.0;
        case PS:
//            return 100.0;
            return 20.0;
        case RC:
            return 19.0;
        case T:
            return 1.0;
        }
        return -1.0;
    }

    public static Double getGradeTypeWeight(GradeType t) {
        switch (t) {
        case CP:
//        case R:
            return 0.20;
        case F:
            return 0.25;
//            return 0.35;
        case MQ:
//        case Q:
            return 0.20;
//            return 0.25;
        case PS:
            return 0.25;
//            return 0.30;
        case RC:
            return 0.05;
        case T:
            return 0.05;
        }
        return 0.0;
    }
    
    public static String getGradeTypeName(GradeType t) {
        switch (t) {
        case CP:
//        case R:
            return "Class participation";
        case F:
            return "Final Exam";
        case MQ:
//        case Q:
            return "Miniquiz";
        case PS:
            return "Problem Set";
        case RC:
            return "Reading Comments";
        case T:
            return "Tutorial";
        }
        return "";
    }

    public static GradeType getGradeType(String key) {
        for (GradeType t : GradeType.values()) {
            if (key.startsWith(t.name())) {
                return t;
            }
        }
        return null;
    }

}
