/* Copyright (c) 2010 Richard Chan */
package logic;

import helper.GDataDownloader;
import helper.GDataDownloader.FolderCannotBeFoundException;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import output.ProblemStatsReport;

import model.Student;
import reader.ClasslistReader;
import reader.ClasslistReader.ClasslistRow;
import reader.ProblemGradesReader;
import reader.ProblemGradesReader.ProblemGradesheetRow;

import com.google.gdata.util.ServiceException;

import exception.ReaderException;

public class ProblemsStatsMgmt {
    
    // hack to make it easier to set params.. fix later?
    private static String LOGIN;
    private static String PASSWORD;
    private static String CLASSLIST_SPREADSHEET;
    private static  String[] PROBLEM_GRADE_SPREADSHEETS;
    
    public static void main(String[] args) {
        if (args.length == 4) {
            LOGIN = args[0];
            PASSWORD = args[1];
            CLASSLIST_SPREADSHEET = args[2];
            PROBLEM_GRADE_SPREADSHEETS = args[3].split(",");
        } else {
            System.err.println("Require arguments: [login] [password] [classlist spreadsheet key] [problem grades spreadsheet keys (comma separated)]");
            return;
        }
        
        ProblemsStatsMgmt psm = new ProblemsStatsMgmt();
        try {
            psm.download(PROBLEM_GRADE_SPREADSHEETS, CLASSLIST_SPREADSHEET);
            psm.read();
            psm.output();
            System.out.println("INFO: DONE!");
        } catch (Exception e) {
            System.out.println("CRIT: error msg = " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private final Map<String, Student> students = new HashMap<String, Student>();
    private final Map<String, Map<String, List<Double>>> gradeSets = new HashMap<String, Map<String, List<Double>>>();
    
    // data source related
    private GDataDownloader downloader = null;
    private List<String> filenames = null;
    private String classlistFilename = null;
    
    // readers
    private ClasslistReader classlistReader = null;
    
    public void download(String[] spreadsheets, String classlistSpreadsheet) throws FolderCannotBeFoundException, IOException, ServiceException {
        downloader = new GDataDownloader(LOGIN, PASSWORD);
        filenames = downloader.downloadSpreadsheets(spreadsheets);
        
        List<String> classlistFilenames = downloader.downloadSpreadsheets(new String[]{ classlistSpreadsheet });
        if (classlistFilenames.size() == 1) {
            classlistFilename = classlistFilenames.get(0);
        } else {
            throw new RuntimeException("Cannot find classlist.");
        }
    }

    public void read() throws ReaderException {
        // class list
        try {
            classlistReader = new ClasslistReader(new FileReader(classlistFilename));
        } catch (FileNotFoundException e) {
            throw new ReaderException(e.getMessage(), true);
        }        
        if (classlistReader == null) {
            throw new RuntimeException("Classlist cannot be found.");
        }
        for (ClasslistRow row = classlistReader.getNextRow(); row != null; row = classlistReader.getNextRow()) {
            students.put(row.student.email, row.student);
        }

        // remove dropped students
        for (Iterator<Student> it = students.values().iterator(); it.hasNext();) {
            Student s = it.next();
            if (s.isDropped)
                it.remove();
        }
        
        // problem grade spreadsheet
        for (String f : filenames) {
            try {
                ProblemGradesReader problemGradesheetReader = new ProblemGradesReader(new FileReader(f));
                processProblemGradesheet(f, problemGradesheetReader, students);
            } catch (FileNotFoundException e) {
                throw new ReaderException(e.getMessage(), true);
            }
        }
    }
    
    private void output() throws IOException {
        for (Entry<String, Map<String, List<Double>>> e : gradeSets.entrySet()) {
            FileWriter fstream = new FileWriter(e.getKey() + ".html");
            BufferedWriter out = new BufferedWriter(fstream);
            ProblemStatsReport.write(e.getKey(), e.getValue(), out);
            out.close();
        }
    }

    private void processProblemGradesheet(String f,
            ProblemGradesReader problemGradesheetReader,
            Map<String, Student> students2) throws ReaderException {
        ProblemGradesheetRow row = null;
        do {
            try {
                row = problemGradesheetReader.getNextRow();
                if (row != null) {
                    Student student = students.get(row.email);
                    if (student == null || student.isDropped) {
                        // ignore for dropped student
                    } else {
                        for (Entry<String, Double> e : row.grades.entrySet()) {
                            String key = getSetKey(e.getKey());
                            List<Double> keyGrades = getOrInitGrades(key, e.getKey());
                            keyGrades.add(e.getValue());
                        }
                    }
                }
            } catch (ReaderException e) {
                if (e.severe) throw e;
                else System.out.println("WARN: [" + f + "]: " + e.getMessage());
                continue;
            }
            
        } while(row != null);
        
    }
    
    private static String getSetKey(String key) {
        return key.substring(0, key.lastIndexOf("."));
    }
    
    private List<Double> getOrInitGrades(String key, String problemKey) {
        Map<String, List<Double>> m = gradeSets.get(key);
        if (m == null) {
            m = new HashMap<String, List<Double>>();
            gradeSets.put(key, m);
        }
        List<Double> l = m.get(problemKey);
        if (l == null) {
            l = new ArrayList<Double>();
            m.put(problemKey, l);
        }
        return l;
    }
}
