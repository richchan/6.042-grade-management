/* Copyright (c) 2010 Richard Chan */
package output;

import helper.Helpers;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.GradeType;
import model.Stats;
import model.Student;

public class StaffReport {

    // TODO: use some kind of template engine
    
    private static String TEMPLATE = null;
    
    public static void setup() throws IOException {
        if (TEMPLATE == null)
            TEMPLATE = Helpers.readFile(StaffReport.class, "/template/staff_report.html");
    }
    
    public static void write(
            Student[] students,
            Stats grandTotalStats,
            Writer out) throws IOException {
        setup();
        
        StringBuffer sb = new StringBuffer();
        
        sb.append(Helpers.writeJSVar("mean", Helpers.gradeDecimalformat.format(grandTotalStats.mean * 100.0)));
        sb.append(Helpers.writeJSVar("median", Helpers.gradeDecimalformat.format(grandTotalStats.median * 100.0)));
        sb.append(Helpers.writeJSVar("stddev", Helpers.gradeDecimalformat.format(grandTotalStats.stddev * 100.0)));
        
        sb.append(writeStudentGrades(students));
        
        sb.append(Helpers.writeJSVar("timestamp", 
          Helpers.addSingleQuote("Grades compiled at: " + 
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()))));
        
        out.write(TEMPLATE.replaceFirst("#CONTENT#", sb.toString()));
    }

    private static String writeStudentGrades(Student[] students) throws IOException {
        List<String> cols = new ArrayList<String>();
        
        cols.add("{id: 'email', label: 'email', type: 'string'}");
        cols.add("{id: 'first', label: 'first', type: 'string'}");
        cols.add("{id: 'last', label: 'last', type: 'string'}");
        cols.add("{id: 'ta', label: 'ta', type: 'string'}");

        for (GradeType gt : GradeType.values()) {
            String name = gt.name().toLowerCase();
            cols.add("{id: '" + name + "', label: '" + name + "', type: 'number'}");
        }

        cols.add("{id: 'total', label: 'total', type: 'number'}");
        cols.add("{id: 'rank', label: 'rank', type: 'number'}");
        
        List<String> rows = new ArrayList<String>();
        for (Student s : students) {
            List<String> rowCols = new ArrayList<String>();
            String report_link = "<a id=\"" + s.email + "\" href=\"" + s.email + ".html\" target=\"_blank\">" + s.email  + "</a>";
            
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.addSingleQuote(s.email), 
                    Helpers.addSingleQuote(report_link), 
                    "{style: 'width: 120px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.addSingleQuote(s.first), 
                    null, 
                    "{style: 'width: 120px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.addSingleQuote(s.last), 
                    null, 
                    "{style: 'width: 120px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.addSingleQuote(s.ta), 
                    null, 
                    "{style: 'width: 120px'}")));
            
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.PS)), 
                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.PS))), 
                    "{style: 'width: 100px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.MQ)), 
                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.MQ))), 
                    "{style: 'width: 100px'}")));
//            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
//                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.Q)), 
//                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.Q))), 
//                    "{style: 'width: 100px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.CP)), 
                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.CP))), 
                    "{style: 'width: 100px'}")));
//            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
//                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.R)), 
//                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.R))), 
//                    "{style: 'width: 100px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.RC)), 
                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.RC))), 
                    "{style: 'width: 100px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.T)), 
                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.T))), 
                    "{style: 'width: 100px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.F)), 
                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.typeTotals.get(GradeType.F))), 
                    "{style: 'width: 100px'}")));
            
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    Helpers.gradeDecimalformat.format(s.aux.grandTotal * 100.0), 
                    Helpers.addSingleQuote(Helpers.gradeDecimalformat.format(s.aux.grandTotal * 100.0)), 
                    "{style: 'width: 100px'}")));
            rowCols.add(Helpers.writeJSMap(Helpers.vfp(
                    String.valueOf(s.aux.rank), 
                    null, 
                    "{style: 'width: 100px'}")));
            
            rows.add(Helpers.writeJSMap(new String[] {"c"}, new String[] {
              Helpers.writeJSArray(rowCols.toArray(new String[0]))}));
        }
        
        return Helpers.writeJSVar("students_grades", 
          Helpers.writeJSMap(Helpers.ary2Map(
            new String[] {
              "cols", Helpers.writeJSArray(cols.toArray(new String[0])),
              "rows", Helpers.writeJSArray(rows.toArray(new String[0])),
            })));
    }

}
