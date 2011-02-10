/* Copyright (c) 2010 Richard Chan */
package logic;

import helper.GDataDownloader;
import helper.GDataDownloader.FolderCannotBeFoundException;
import helper.Helpers;
import helper.Helpers.CallbackWithException;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Median;

import output.StaffReport;
import output.StudentReport;

import model.Constants;
import model.Grade;
import model.GradeType;
import model.Stats;
import model.Student;
import model.StudentAux;
import reader.ClasslistReader;
import reader.ClasslistReader.ClasslistRow;
import reader.GradesheetReader;
import reader.GradesheetReader.GradesheetRow;
import reader.PsetConfigReader;
import reader.PsetConfigReader.PsetConfigRow;

import com.google.gdata.util.ServiceException;

import exception.ModelException;
import exception.ReaderException;

public class GradesMgmt {

    // hack to make it easier to set params.. fix later?
    private static String LOGIN;
    private static String PASSWORD;
    private static String FOLDER_NAME;
    
    public static void main(String[] args) {
        
        if (args.length == 3) {
            LOGIN = args[0];
            PASSWORD = args[1];
            FOLDER_NAME = args[2];
        } else {
            System.err.println("Require arguments: [login] [password] [folder_name]");
            return;
        }
        
        boolean isDropping = true;
        boolean isSoftPsetGrade = true;
        GradesMgmt gm = new GradesMgmt(isDropping, isSoftPsetGrade, 2.0);
        try {
            gm.download();
            gm.read();
            gm.compute();
            gm.output();
            System.out.println("INFO: DONE!");
        } catch (Exception e) {
            System.out.println("CRIT: error msg = " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // states
    private final Map<String, Student> students = new HashMap<String, Student>();
    private final Map<String, String> psetMakeupSrc = new HashMap<String, String>();
    private final Set<String> allGradeKeys = new HashSet<String>();
    private final Set<String> completedGradeKeys = new HashSet<String>();
    private final Map<GradeType, Integer> numGradeTypes = new HashMap<GradeType, Integer>();
    private final Map<GradeType, Stats> gradeTypeStats = new HashMap<GradeType, Stats>();
    private Stats grandTotalStats = null;
    
    private final boolean isDropping;
    private final boolean isSoftPsetGrade;
    
    // data source related
    private GDataDownloader downloader = null;
    private List<String> filenames = null;
    
    // readers
    private ClasslistReader classlistReader = null;
    private PsetConfigReader psetConfigReader = null;
    
    public GradesMgmt(boolean isDropping, boolean isSoftPsetGrade, final double gradeForExcusedCP) {
        this.isDropping = isDropping;
        this.isSoftPsetGrade = isSoftPsetGrade;
        Grade.defaultGradeValueSetter = new Helpers.CallbackWithReturn<GradeType, Double>() {
            public Double call(GradeType t) {
                if (t == GradeType.CP)
//                if (t == GradeType.R)
                    return gradeForExcusedCP;
                else
                    return 0.0;
            }
        };
        
        // initializing numGradeTypes
        for (GradeType t : GradeType.values()) {
            numGradeTypes.put(t, 0);
        }
    }
    
    public void download() throws FolderCannotBeFoundException, IOException, ServiceException {
        downloader = new GDataDownloader(LOGIN, PASSWORD, FOLDER_NAME);
        filenames = downloader.downloadSpreadsheets(new String[]{}); // place extra spreadsheets in {}
    }
    
    public void read() throws ReaderException {
        // class list
        Helpers.filterInPlace("CLASSLIST", filenames, true, new CallbackWithException<String, ReaderException>() {
            public void call(String obj) throws ReaderException {
                try {
                    classlistReader = new ClasslistReader(new FileReader(obj));
                } catch (FileNotFoundException e) {
                    throw new ReaderException(e.getMessage(), true);
                }
            }
        });
        if (classlistReader == null) {
            throw new RuntimeException("Classlist cannot be found.");
        }
        for (ClasslistRow row = classlistReader.getNextRow(); row != null; row = classlistReader.getNextRow()) {
            students.put(row.student.email, row.student);
        }
        
        // ps config
        if (isSoftPsetGrade) {
            Helpers.filterInPlace("PSET-CONFIG", filenames, true, new CallbackWithException<String, ReaderException>() {
                public void call(String obj) throws ReaderException {
                    try {
                        psetConfigReader = new PsetConfigReader(new FileReader(obj));
                    } catch (FileNotFoundException e) {
                        throw new ReaderException(e.getMessage(), true);
                    }
                }
            });
            if (psetConfigReader == null) {
                throw new RuntimeException("Pset-config sheet cannot be found.");
            }
            for (PsetConfigRow row = psetConfigReader.getNextRow(); row != null; row = psetConfigReader.getNextRow()) {
                psetMakeupSrc.put(row.ps, row.src);
            }
        }
        
        // grade sheets
        Helpers.filterInPlace("GRADESHEET", filenames, false, new CallbackWithException<String, ReaderException>() {
            public void call(String obj) throws ReaderException {
                try {
                    GradesheetReader gradesheetReader = new GradesheetReader(obj, new FileReader(obj));
                    processGradesheet(obj, gradesheetReader, students, allGradeKeys);
                } catch (FileNotFoundException e) {
                    throw new ReaderException(e.getMessage(), true);
                }
            }
        });
        
        // remove dropped students
        for (Iterator<Student> it = students.values().iterator(); it.hasNext();) {
            Student s = it.next();
            if (s.isDropped)
                it.remove();
        }
    }
    
    public void compute() {
        String finalGradeKey = null;
        
        // filter out completed keys
        completedGradeKeys.addAll(allGradeKeys);
        Set<String> keysToBeRemoved = new HashSet<String>();
        for (Student s : students.values()) {
            for (Iterator<String> it = completedGradeKeys.iterator(); it.hasNext();) {
                String key = it.next();
                if (s.rawGrades.get(key) == null) {
                    keysToBeRemoved.add(key);
                    System.out.println("WARN: Student [" + s.email + "] missing grade [" + key + "].");
                }
            }
        }
        completedGradeKeys.removeAll(keysToBeRemoved);
        
        // get number of each grade types in system
        for (String key : completedGradeKeys) {
            GradeType type = Constants.getGradeType(key);
            Integer num = numGradeTypes.get(type);
            numGradeTypes.put(type, (num == null) ? 1 : num + 1);
            if (type == GradeType.F) {
                if (finalGradeKey == null) {
                    finalGradeKey = key;
                } else {
                    System.out.println("WARN: More than one final grades found!! " + finalGradeKey + " and " + key);
                }
            }
        }
        
        // decrement number of grade types for dropping if necessary
        if (isDropping) {
            Integer num = numGradeTypes.get(GradeType.CP);
//            Integer num = numGradeTypes.get(GradeType.R);
            int min = num == null ? 0 : Math.max(num - 2, 0);
            numGradeTypes.put(GradeType.CP, min);
//            numGradeTypes.put(GradeType.R, min);
            
            num = numGradeTypes.get(GradeType.PS);
            min = num == null ? 0 : Math.max(num - 1, 0);
            numGradeTypes.put(GradeType.PS, min);
            
            num = numGradeTypes.get(GradeType.MQ);
            min = num == null ? 0 : Math.max(num - 1, 0);
            numGradeTypes.put(GradeType.MQ, min);
        }
        
        // compute the totals for each student
        for (Student s : students.values()) {
            computeStudentGrades(s, numGradeTypes, completedGradeKeys, psetMakeupSrc, finalGradeKey, isDropping, isSoftPsetGrade);
        }
        
        // compute stats
        for (final GradeType type : GradeType.values()) {
            Stats s = new Stats();
            
            double[] values = new double[students.size()];
            Student[] sorted = new Student[students.size()];
            int i = 0;
            for (Student student : students.values()) {
                Double t = student.aux.typeTotals.get(type);
                sorted[i] = student;
                values[i++] = (t == null ? 0.0 : t);
            }
            s.mean = new Mean().evaluate(values);
            s.median = new Median().evaluate(values);
            s.stddev = new StandardDeviation(false).evaluate(values);
            Arrays.sort(sorted, new Comparator<Student>() {
                public int compare(Student a, Student b) {
                    Double at = a.aux.typeTotals.get(type);
                    if (at == null) at = 0.0;
                    Double bt = b.aux.typeTotals.get(type);
                    if (bt == null) bt = 0.0;
                    return (int) (at * 1000.0 - bt * 1000.0);
                }
            });
            s.sorted = sorted;
            
            gradeTypeStats.put(type, s);
        }
        
        // grand total's stats
        grandTotalStats = new Stats();
        double[] values = new double[students.size()];
        Student[] sorted = new Student[students.size()];
        int i = 0;
        for (Student student : students.values()) {
            sorted[i] = student;
            values[i++] = student.aux.grandTotal;
        }
        grandTotalStats.mean = new Mean().evaluate(values);
        grandTotalStats.median = new Median().evaluate(values);
        grandTotalStats.stddev = new StandardDeviation(false).evaluate(values);
        Arrays.sort(sorted, new Comparator<Student>() {
            public int compare(Student a, Student b) {
                return (int) (a.aux.grandTotal * 1000000.0 - b.aux.grandTotal * 1000000.0);
            }
        });
        grandTotalStats.sorted = sorted;
        for (int j = sorted.length - 1; j >= 0; j--) {
            sorted[j].aux.rank = sorted.length - j - 1;
        }
    }
    
    public void output() throws IOException {
        for (Student student : grandTotalStats.sorted) {
            FileWriter fstream = new FileWriter(student.email + ".html");
            BufferedWriter out = new BufferedWriter(fstream);
            StudentReport.write(student, isSoftPsetGrade, gradeTypeStats, completedGradeKeys, grandTotalStats, numGradeTypes, out);
            out.close();
        }
        
        FileWriter fstream = new FileWriter("staff.html");
        BufferedWriter out = new BufferedWriter(fstream);
        StaffReport.write(grandTotalStats.sorted, grandTotalStats, out);
        out.close();
    }

    private static void processGradesheet(String f, GradesheetReader gradesheetReader,
            Map<String, Student> students, Set<String> allGradeKeys) throws ReaderException {
        GradesheetRow row = null;
        do {
            try {
                row = gradesheetReader.getNextRow();
                if (row != null) {
                    Student student = students.get(row.email);
                    if (student == null) {
                        System.out.println("WARN: [" + f + "]: student [" + row.email + "] unknown.");
                    } else {
                        if (!student.isDropped) {
                            for (Grade g : row.grades.values()) {
                                student.addGrade(g.key, g);
                                allGradeKeys.add(g.key);
                            }
                        }
                    }
                }
            } catch (ReaderException e) {
                if (e.severe) throw e;
                else System.out.println("WARN: [" + f + "]: " + e.getMessage());
                continue;
            } catch (ModelException e) {
                System.out.println("WARN: [" + f + "]: " + e.getMessage());
                continue;
            }
        } while(row != null);
    }
    
    private static void computeStudentGrades(Student student, 
            Map<GradeType, Integer> numGradeTypes,
            Set<String> completedGrades,
            Map<String, String> psetMakeupSrc, 
            String finalGradeKey, 
            boolean isDropping, boolean isSoftPsetGrade) {
        
        StudentAux aux = new StudentAux();
        aux.actualGrades = new HashMap<String, Grade>(student.rawGrades);
        
        // remove incomplete grades
        for (Iterator<Grade> it = aux.actualGrades.values().iterator(); it.hasNext();) {
            Grade g = it.next();
            if (!completedGrades.contains(g.key)) {
                it.remove();
            }
        }
        
        // fix soft pset grades
        if (isSoftPsetGrade) {
            Grade finalGrade = (finalGradeKey != null) ? aux.actualGrades.get(finalGradeKey) : null;
            for (Iterator<Grade> it = aux.actualGrades.values().iterator(); it.hasNext();) {
                Grade g = it.next();
                if (GradeType.PS == g.gradeType) {
                    double grade = g.value;
                    double makeUpMax = Math.min(20.0 - grade, 10.0);
                    
                    String src = psetMakeupSrc.get(g.key);
                    if (null != src) {
                        Grade srcGrade = aux.actualGrades.get(src);
                        if (null != srcGrade) {
                            if (srcGrade.gradeType == GradeType.F) {
                                double bonus = srcGrade.value / 100.0 * makeUpMax * 0.5;
                                grade += bonus;
                            } else if (srcGrade.gradeType == GradeType.MQ) {
//                            } else if (srcGrade.gradeType == GradeType.Q) {
                                double bonus = srcGrade.value / 20.0 * makeUpMax * 0.5;
                                grade += bonus;
                            } else {
                                System.out.println("WARN: Unable to process makeup grade from key: " + src);
                            }
                        } else {
                            System.out.println("WARN: Unable to process makeup grade from key: " + src + ": cannot be found");
                        }
                    }
                    
                    if (null != finalGrade) {
                        double bonus = finalGrade.value / 100.0 * makeUpMax * 0.5;
                        grade += bonus;
                    }
                    
                    g.value = grade;
                }
            }
        }
        
        // remove dropped grades
        if (isDropping) {
            dropLowestGradeForType(aux, GradeType.PS);
            dropLowestGradeForType(aux, GradeType.MQ);
            dropLowestGradeForType(aux, GradeType.CP);
            dropLowestGradeForType(aux, GradeType.CP);
//            dropLowestGradeForType(aux, GradeType.R);
//            dropLowestGradeForType(aux, GradeType.R);
        }
        
        // get totals by type
        for (Grade g : aux.actualGrades.values()) {
            if (g.dropped) continue;
            Double t = aux.typeTotals.get(g.gradeType);
            t = (t == null) ? g.value : (t + g.value);
            aux.typeTotals.put(g.gradeType, t);
        }
        
        // get grand total
        aux.grandTotal = 0.0;
        for (Entry<GradeType, Double> e : aux.typeTotals.entrySet()) {
            GradeType t = e.getKey();
            double max = Constants.getGradeTypeMax(t) * numGradeTypes.get(t);
            if (max > 0) {
                double delta = e.getValue() / max * Constants.getGradeTypeWeight(t);
                aux.grandTotal += delta;
            }
        }
        
        // link auxiliary data
        student.aux = aux;
    }
    
    private static void dropLowestGradeForType(StudentAux aux, GradeType type) {
        String lowest = getLowestGradeForType(aux, type);
        if (lowest != null) {
            aux.actualGrades.get(lowest).dropped = true;
            // aux.actualGrades.remove(lowest);
        }
    }
    
    private static String getLowestGradeForType(StudentAux aux, GradeType type) {
        Grade lowest = null;
        for (Grade g : aux.actualGrades.values()) {
            if (g.gradeType == type && g.dropped == false) {
                if (lowest == null || g.value < lowest.value) {
                    lowest = g;
                }
            }
        }
        return lowest == null ? null : lowest.key;
    }
    
}
