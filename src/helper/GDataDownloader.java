/* Copyright (c) 2010 Richard Chan */
package helper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gdata.client.DocumentQuery;
import com.google.gdata.client.GoogleAuthTokenFactory.UserToken;
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.data.media.MediaSource;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.util.ServiceException;

/**
 * Downloader for Google spreadsheets.
 * 
 * @author wing (Richard Chan)
 *
 */
public class GDataDownloader {

    /**
     * Exception thrown when foldername cannot be found.
     */
    public static class FolderCannotBeFoundException extends Exception {
        private static final long serialVersionUID = 7698117798959297834L;
    }

    private final static String FOLDER_FEED_URI = "http://docs.google.com/feeds/default/private/full/-/folder";
    private final static String SPREADSHEET_ENTRY_URI = "http://spreadsheets.google.com/feeds/spreadsheets/";

    private final DocsService docClient;
    private final SpreadsheetService spreadsheetClient;

    private final String folderUri;

    /**
     * Creates a downloader with the given username/password and foldername.
     */
    public GDataDownloader(String username, String password, String foldername) 
    throws FolderCannotBeFoundException, IOException, ServiceException {
        spreadsheetClient = new SpreadsheetService("mit-6042-compiler");
        docClient = new DocsService("mit-6042-compiler");
        spreadsheetClient.setUserCredentials(username, password);
        docClient.setUserCredentials(username, password);

        folderUri = findFolderUri(foldername);
        if (folderUri == null) {
            throw new FolderCannotBeFoundException();
        }
    }
    
    public GDataDownloader(String username, String password) 
    throws FolderCannotBeFoundException, IOException, ServiceException {
        spreadsheetClient = new SpreadsheetService("mit-6042-compiler");
        docClient = new DocsService("mit-6042-compiler");
        spreadsheetClient.setUserCredentials(username, password);
        docClient.setUserCredentials(username, password);
        folderUri = null;
    }

    /**
     * @return a list of csv filenames of the spreadsheets after downloading all spreadsheets in given folder.
     * Note: if spreadsheet has multiple worksheets, the worksheets will be downloaded as separate files.
     * 
     * @deprecated (Google Docs folders don't seem to work correctly "http://code.google.com/p/gdata-issues/issues/detail?id=1691") 
     */
    protected List<String> downloadSpreadsheets() 
    throws IOException, ServiceException {
        List<String> downloadedFiles = new ArrayList<String>();

        DocumentQuery query = new DocumentQuery(new URL(folderUri + "/-/spreadsheet"));
        DocumentListFeed feed = docClient.getFeed(query, DocumentListFeed.class);
//        System.out.println("downloading " + feed.getEntries().size() + " spreadsheets.");
        
        // swap credentials so doc_client can access spreadsheets.google.com
        UserToken docToken = (UserToken) docClient.getAuthTokenFactory().getAuthToken();
        UserToken spreadsheetsToken = (UserToken) spreadsheetClient.getAuthTokenFactory().getAuthToken();
        docClient.setUserToken(spreadsheetsToken.getValue());
        for (DocumentListEntry entry : feed.getEntries()) {
            if ("spreadsheet".equals(entry.getType())) {
                String title = entry.getTitle().getPlainText();
                downloadSpreadsheetToCSV(entry.getResourceId(), title.replace(' ', '_'), downloadedFiles);
                System.out.println("INFO: spreadsheet [" + title + "][" + entry.getResourceId() + "] downloaded.");
            }
        }
        
        // sets it back
        docClient.setUserToken(docToken.getValue());
        return downloadedFiles;
    }
    
    /**
     * @param spreadsheetKeys extra spreadsheets to download -- they may or may not be in the folder
     * @return a list of csv filenames of the spreadsheets after downloading all spreadsheets in given folder.
     * Also look through spreadsheetKeys and download them if they haven't been downloaded yet.
     * Note: if spreadsheet has multiple worksheets, the worksheets will be downloaded as separate files.
     */
    public List<String> downloadSpreadsheets(String[] spreadsheetKeys)
    throws IOException, ServiceException {
        // HACK: remove method when Google fixed the issue
        Set<String> downloadedSpreadsheetKeys = new HashSet<String>();
        List<String> downloadedFiles = new ArrayList<String>();

        UserToken docToken = (UserToken) docClient.getAuthTokenFactory().getAuthToken();
        UserToken spreadsheetsToken = (UserToken) spreadsheetClient.getAuthTokenFactory().getAuthToken();
        
        if (folderUri != null) {
            DocumentQuery query = new DocumentQuery(new URL(folderUri + "/-/spreadsheet"));
            DocumentListFeed feed = docClient.getFeed(query, DocumentListFeed.class);
            //        System.out.println("downloading " + feed.getEntries().size() + " spreadsheets.");

            // swap credentials so doc_client can access spreadsheets.google.com
            docClient.setUserToken(spreadsheetsToken.getValue());
            for (DocumentListEntry entry : feed.getEntries()) {
                if ("spreadsheet".equals(entry.getType())) {
                    String title = entry.getTitle().getPlainText();
                    downloadSpreadsheetToCSV(entry.getResourceId(), title.replace(' ', '_'), downloadedFiles);
                    System.out.println("INFO: spreadsheet [" + title + "][" + entry.getResourceId() + "] downloaded.");
                    downloadedSpreadsheetKeys.add(entry.getResourceId());
                }
            }
        }
        
        docClient.setUserToken(spreadsheetsToken.getValue());
        for (String spreadsheetKey : spreadsheetKeys) {
            if (!downloadedSpreadsheetKeys.contains(spreadsheetKey)) {
                String title = getSpreadsheetTitle(spreadsheetKey);
                downloadSpreadsheetToCSV(spreadsheetKey, title.replace(' ', '_'), downloadedFiles);
                System.out.println("INFO: spreadsheet [" + title + "][" + spreadsheetKey + "] downloaded.");
            }
        }
        
        // sets it back
        docClient.setUserToken(docToken.getValue());
        return downloadedFiles;
    }

    // return uri of the folder specified or null if not found
    private String findFolderUri(String foldername)
    throws IOException, ServiceException {
        URL folderFeedUri = new URL(FOLDER_FEED_URI);
        DocumentListFeed folderFeed = docClient.getFeed(folderFeedUri, DocumentListFeed.class);

        String folderUri = null;
        for (DocumentListEntry entry : folderFeed.getEntries()) {
            if (foldername.equals(entry.getTitle().getPlainText())) {
                folderUri = ((MediaContent) entry.getContent()).getUri();
                break;
            }
        }
        return folderUri;
    }

    // download spreadsheet to filepath + worksheet index + ".csv"
    private void downloadSpreadsheetToCSV(String resourceId, String filepath, List<String> downloadedFiles)
    throws IOException, MalformedURLException, ServiceException {
        String docId = resourceId.substring(resourceId.lastIndexOf(":") + 1);
        String exportUrl = "http://spreadsheets.google.com/feeds/download/spreadsheets/Export?key=" + docId + "&exportFormat=csv";

        // If exporting to .csv or .tsv, add the gid parameter to specify which sheet to export
        for (int i = 0, j = 0, numWorksheets = getNumWorksheets(resourceId); i < numWorksheets && j < numWorksheets + 100; i++, j++) { // max: 100 retries
            // WARNING: gid is actually not 0 based.. if user has deleted a worksheet, the gid is never reused. Awaiting bug fix.  
            //          http://code.google.com/p/gdata-issues/issues/detail?id=1816&sort=-id&colspec=API%20ID%20Type%20Status%20Priority%20Stars%20Summary
            try {
                downloadFile(exportUrl + "&gid=" + j, filepath + i + ".csv");
            } catch(ServiceException e) { // google's bug...
                i--;
            }
            if (!downloadedFiles.contains(filepath + i + ".csv")) {
                downloadedFiles.add(filepath + i + ".csv");
            }
        }
    }

    // download a file from a given url
    private void downloadFile(String exportUrl, String filepath)
    throws IOException, MalformedURLException, ServiceException {
        MediaContent mc = new MediaContent();
        mc.setUri(exportUrl);
        MediaSource ms = docClient.getMedia(mc);

        InputStream inStream = null;
        FileOutputStream outStream = null;

        try {
            inStream = ms.getInputStream();
            outStream = new FileOutputStream(filepath);
            int c;
            while ((c = inStream.read()) != -1) {
                outStream.write(c);
            }
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.flush();
                outStream.close();
            }
        }
    }

    // get number of worksheets in a spreadsheet
    private int getNumWorksheets(String resourceId) throws MalformedURLException, IOException, ServiceException {
        String docId = resourceId.substring(resourceId.lastIndexOf(":") + 1);
        SpreadsheetEntry spreadsheetEntry = spreadsheetClient.getEntry(new URL(SPREADSHEET_ENTRY_URI + docId), SpreadsheetEntry.class);
        return spreadsheetEntry.getWorksheets().size();
    }
    
    private String getSpreadsheetTitle(String resourceId) throws MalformedURLException, IOException, ServiceException {
        String docId = resourceId.substring(resourceId.lastIndexOf(":") + 1);
        SpreadsheetEntry spreadsheetEntry = spreadsheetClient.getEntry(new URL(SPREADSHEET_ENTRY_URI + docId), SpreadsheetEntry.class);
        return spreadsheetEntry.getTitle().getPlainText();
    }

}
