/* Copyright (c) 2010 Richard Chan */
package output;

import helper.Helpers;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Median;

public class ProblemStatsReport {

    // TODO: use some kind of template engine
    
    private static String TEMPLATE = null;
    
    public static void setup() throws IOException {
        if (TEMPLATE == null)
            TEMPLATE = Helpers.readFile(ProblemStatsReport.class, "/template/problem_stats_report.html");
    }
    
    public static void write(
            String key,
            Map<String, List<Double>> subkeyGrades,
            Writer out) throws IOException {
        setup();
        
        StringBuffer sb = new StringBuffer();
        sb.append(Helpers.writeJSVar("name", Helpers.addSingleQuote(key)));
        sb.append(Helpers.writeJSVar("timestamp", 
          Helpers.addSingleQuote("Grades compiled at: " + 
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()))));
        
        for (Entry<String, List<Double>> e : subkeyGrades.entrySet()) {
            Collections.sort(e.getValue());
            sb.append(writeGradesForSubkey(e.getKey(), e.getValue()));
        }
        
        out.write(TEMPLATE.replaceFirst("#CONTENT#", sb.toString()));
    }

    private static String writeGradesForSubkey(String key, List<Double> grades) {
        StringBuffer sb = new StringBuffer();
        
        Map<Double, Integer> gradeCounts = countGrades(grades);
        
        double[] gradesAry = new double[grades.size()];
        for (int i = grades.size() - 1; i >= 0; i--) {
            gradesAry[i] = grades.get(i);
        }
        double mean = new Mean().evaluate(gradesAry);
        double median = new Median().evaluate(gradesAry);
        double stddev = new StandardDeviation(false).evaluate(gradesAry);
        double count = gradesAry.length;
        
        sb.append(Helpers.writeJSAssign("titles['" + key + "']",
                Helpers.addSingleQuote(
                        "count = " + count +
                        "; mean = " + Helpers.gradeDecimalformat.format(mean) +
                        "; median = " + Helpers.gradeDecimalformat.format(median) +
                        "; stddev = " + Helpers.gradeDecimalformat.format(stddev))));
        
        List<String> cols = new ArrayList<String>();
        cols.add("{id: 'point', label: 'id', type: 'string'}");
        cols.add("{id: 'count', label: 'count', type: 'number'}");
        
        List<String> rows = new ArrayList<String>();
        
        List<Double> gradeCountSorted = new ArrayList<Double>(gradeCounts.keySet());
        Collections.sort(gradeCountSorted);
        
        for (Double subkey : gradeCountSorted) {
            rows.add(Helpers.writeJSMap(new String[] {"c"}, new String[] {
              Helpers.writeJSArray(new String[] {
                Helpers.writeJSMap(Helpers.vfp(
                    Helpers.addSingleQuote(String.valueOf(subkey)), null, null
                  )),
                Helpers.writeJSMap(Helpers.vfp(
                    String.valueOf(gradeCounts.get(subkey)), null, null      
                  )),
              })
            }));
        }
        
        sb.append(Helpers.writeJSAssign("grades['" + key + "']", 
          Helpers.writeJSMap(Helpers.ary2Map(
            new String[] {
              "cols", Helpers.writeJSArray(cols.toArray(new String[0])),
              "rows", Helpers.writeJSArray(rows.toArray(new String[0])),
            }))));
        
        return sb.toString();
    }
    
    private static Map<Double, Integer> countGrades(List<Double> sorted) {
        Map<Double, Integer> counts = new HashMap<Double, Integer>();
        double max = Math.ceil(sorted.get(sorted.size()-1));
        for (double i = 0; i <= max; i += 0.5) {
            counts.put(i, 0);
        }
        for (Double d : sorted) {
            double value = Math.ceil(d * 2.0f) / 2.0f;
            counts.put(d, counts.get(value) + 1);
        }
        return counts;
    }
    
}
