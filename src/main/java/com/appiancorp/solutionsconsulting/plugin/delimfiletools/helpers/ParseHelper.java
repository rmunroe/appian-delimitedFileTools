package com.appiancorp.solutionsconsulting.plugin.delimfiletools.helpers;

import com.appiancorp.ps.plugins.typetransformer.AppianList;
import com.appiancorp.ps.plugins.typetransformer.AppianObject;
import com.appiancorp.ps.plugins.typetransformer.AppianTypeFactory;
import com.appiancorp.suiteapi.common.paging.PagingInfo;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypedValue;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.io.*;


/**
 * Class for helping with the parsing of delimited files found in Appian's content management store.
 */
public class ParseHelper {
    private static final Logger LOG = (Logger) LogManager.getLogger(ParseHelper.class);

    private final static int MAX_ROWS = 10000;

    public static File getFileFromAppian(ContentService cs, Long documentId) {
        try {
            // Read the document content
            try (InputStream inputStream = cs.getDocumentInputStream(documentId)) {
                File temporaryDocument = File.createTempFile("temp", ".text");
                temporaryDocument.deleteOnExit();

                FileUtils.copyInputStreamToFile(inputStream, temporaryDocument);
                return temporaryDocument;
            }
        } catch (Exception e) {
            // Handle any exceptions
            LOG.error("Error accessing document ", e);
            return null;
        }
    }

    /**
     * The primary function for parsing a delimited text file from an Appian Document. Requires a valid ICVSParser instance.
     *
     * @param contentService    ContentService injected by Appian
     * @param typeFactory       An instance of the TypeTransformer's AppianTypeFactory, build using the TypeService injected by Appian
     * @param parser            A valid ICSVParser instance
     * @param delimitedFile     ID of Appian Document to parse
     * @param hasHeaderRow      If true, the first line of the file contains the column / field names
     * @param includeTotalCount If true, the results will contain 'totalLines' with the number of lines in the Document
     * @param pagingInfo        An a!pagingInfo() instance. Allows for batching of the file. Sorting not yet supported.
     * @return The Appian dictionary containing the parse results.
     * @throws InvalidContentException If the Appian document is not found
     */
    public static TypedValue parseFile(ContentService contentService, AppianTypeFactory typeFactory, ICSVParser parser, Long delimitedFile, Boolean hasHeaderRow, Boolean includeTotalCount, PagingInfo pagingInfo)
            throws InvalidContentException {
        AppianList values = typeFactory.createList(AppianType.DICTIONARY);

        int totalLines = -1;
        if (includeTotalCount)
            totalLines = countLinesInFile(getFileFromAppian(contentService, delimitedFile));

        int maxRows = MAX_ROWS;
        try {
            // First, get the Dictionary field names
            String[] firstLine = getReader(parser, getFileFromAppian(contentService, delimitedFile), 0).readNext();
            String[] fieldNames;
            if (hasHeaderRow) {
                // Use first row as headers
                fieldNames = firstLine;
                for (int i = 0; i < fieldNames.length; i++) {
                    fieldNames[i] = fieldNames[i].replaceAll("[^a-zA-Z0-9]", "_");
                }
            } else {
                // Use 1-based integers as names, e.g. index(local!dict, "1", null)
                fieldNames = new String[firstLine.length];
                for (int i = 0; i < firstLine.length; i++)
                    fieldNames[i] = "c" + Integer.toString(i + 1);
            }

            int skipLines = 0;
            if (hasHeaderRow) skipLines++;

            // Handle paging of read
            if (pagingInfo != null) {
                skipLines += pagingInfo.getStartIndex() - 1; // subtract 1 because skipLines is 0 based
                if (pagingInfo.getBatchSize() > 0)           // account for -1 to equal max
                    maxRows = pagingInfo.getBatchSize();
            }

            CSVReader reader = getReader(parser, getFileFromAppian(contentService, delimitedFile), skipLines);
            String[] row;

            // Parse!
            int rowCount = 0;
            while ((row = reader.readNext()) != null && rowCount < maxRows) {
                AppianObject element = (AppianObject) typeFactory.createElement(AppianType.DICTIONARY);

                for (int i = 0; i < row.length; i++)
                    element.put(fieldNames[i], typeFactory.createString(row[i]));

                values.add(element);
                rowCount++;
            }

            return getSuccessReturnValue(typeFactory, values, rowCount, totalLines);

        } catch (IOException e) {
            String errorMessage = "IOException: " + e.getMessage();
            LOG.error(errorMessage);
            return getErrorReturnValue(typeFactory, errorMessage);
        } catch (CsvValidationException e) {
            String errorMessage = "CsvValidationException: " + e.getMessage();
            LOG.error(errorMessage);
            return getErrorReturnValue(typeFactory, errorMessage);
        }
    }


    /**
     * @param parser     A valid ICSVParser instance
     * @param appianFile The File returned from ParseHelper.getFileFromAppian()
     * @param skipLines  The number of lines off the top to skip
     * @return An open CSVReader
     * @throws FileNotFoundException Only thrown if Appian resolves a Document to a physical file, but that file does not exist (extremely rare, if ever)
     */
    private static CSVReader getReader(ICSVParser parser, File appianFile, int skipLines) throws FileNotFoundException {
        return new CSVReaderBuilder(new FileReader(appianFile))
                .withCSVParser(parser)
                .withSkipLines(skipLines)
                .build();
    }


    /**
     * @param typeFactory An instance of the TypeTransformer's AppianTypeFactory, build using the TypeService injected by Appian
     * @param values      The list of dictionaries if success == true
     * @param rowsParsed  The number of rows parsed and returned in the current operation
     * @param totalLines  The total number of lines in the file
     * @return Dictionary
     */
    private static TypedValue getSuccessReturnValue(AppianTypeFactory typeFactory, AppianList values, int rowsParsed, int totalLines) {
        LOG.debug("getSuccessReturnValue: rowsParsed: " + rowsParsed + ";");
        return getReturnValueDictionary(typeFactory, true, null, values, rowsParsed, totalLines);
    }


    /**
     * @param typeFactory  An instance of the TypeTransformer's AppianTypeFactory, build using the TypeService injected by Appian
     * @param errorMessage The reason for success == false
     * @return Dictionary
     */
    public static TypedValue getErrorReturnValue(AppianTypeFactory typeFactory, String errorMessage) {
        LOG.debug("getErrorReturnValue: errorMessage: " + errorMessage + ";");
        return getReturnValueDictionary(typeFactory, false, errorMessage, null, 0, 0);
    }


    /**
     * @param typeFactory  An instance of the TypeTransformer's AppianTypeFactory, build using the TypeService injected by Appian
     * @param success      True if no errors
     * @param errorMessage The reason for success == false
     * @param values       The list of dictionaries if success == true
     * @param linesParsed  The number of rows parsed and returned in the current operation
     * @param totalLines   The total number of lines in the file
     * @return Dictionary
     */
    private static TypedValue getReturnValueDictionary(AppianTypeFactory typeFactory, Boolean success, String errorMessage, AppianList values, int linesParsed, int totalLines) {
        LOG.debug("getReturnValueDictionary: success: " + Boolean.toString(success) + ", errorMessage: " + errorMessage + ";");

        AppianObject dictionary = (AppianObject) typeFactory.createElement(AppianType.DICTIONARY);

        dictionary.put("success", typeFactory.createBoolean(success));

        if (success) {
            dictionary.put("values", values);
            dictionary.put("linesParsed", typeFactory.createLong(Integer.toUnsignedLong(linesParsed)));

            if (totalLines > -1)
                dictionary.put("totalLines", typeFactory.createLong(Integer.toUnsignedLong(totalLines)));
        } else {
            dictionary.put("errorMessage", typeFactory.createString(errorMessage));
        }

        return typeFactory.toTypedValue(dictionary);
    }


//    /**
//     * @param contentService ContentService injected by Appian
//     * @param delimitedFile  ID of Appian Document to parse
//     * @return A String error message if the file does not exist
//     */
//    public static String checkFileExists(ContentService contentService, Long delimitedFile) {
//        try {
//            String filePath = contentService.getInternalFilename(delimitedFile);
//            File file = new File(filePath);
//            if (!file.exists())
//                return "There was a problem reading the delimitedFile passed: According to Java, the file does not exist. File Path: " + filePath;
//        } catch (InvalidContentException e) {
//            return "The delimitedFile passed was not a valid Appian Document";
//        }
//        return null;
//    }


    /**
     * @param appianFile The File returned from ParseHelper.getFileFromAppian()
     * @return The number of lines in the file, or -1 if there was any error.
     */
    public static int countLinesInFile(File appianFile) {
        int count;
        FileReader reader;
        try {
            reader = new FileReader(appianFile);
        } catch (FileNotFoundException e) {
            LOG.error("FileNotFoundException caught: " + e.getMessage());
            return -1;
        }

        LineNumberReader lnr = new LineNumberReader(reader);

        // Jump to the end of the file
        try {
            //noinspection ResultOfMethodCallIgnored
            lnr.skip(Long.MAX_VALUE);
        } catch (IOException e) {
            LOG.error("IOException caught: " + e.getMessage());
            return -1;
        }

        // Get the "current" line number (the last line) PLUS ONE
        count = lnr.getLineNumber() + 1;

        try {
            // Clean up
            lnr.close();
            reader.close();
        } catch (Exception ignored) {
        }

        return count;
    }


    /**
     * Reads lines and return a dictionary of status and values.
     *
     * @param typeFactory
     * @param filePath
     * @param startLine
     * @param lineCount
     * @return
     */
    public static TypedValue readLinesInFile(AppianTypeFactory typeFactory,  String filePath, int startLine, int lineCount) {
        AppianObject dictionary = (AppianObject) typeFactory.createElement(AppianType.DICTIONARY);

        if (lineCount == -1) lineCount = Integer.MAX_VALUE; // Make lineCount big

        FileReader reader;
        try {
            reader = new FileReader(new File(filePath));
        } catch (FileNotFoundException e) {
            String errorMessage = "FileNotFoundException caught: " + e.getMessage();
            LOG.error(errorMessage);
            dictionary.put("success", typeFactory.createBoolean(false));
            dictionary.put("errorMessage", typeFactory.createString(errorMessage));
            return typeFactory.toTypedValue(dictionary);
        }

        AppianList lines = typeFactory.createList(AppianType.STRING);

        try (BufferedReader br = new BufferedReader(reader)) {
            String line;

            // Skip the lines until the startLine, accounting for Appian's 1-basedness
            for (int i = 0; i < startLine - 1; i++) {
                br.readLine();
            }

            for (int i = 0; i < lineCount; i++) {
                if ((line = br.readLine()) != null)
                    lines.add(typeFactory.createString(line));
                else
                    break;
            }

        } catch (IOException e) {
            String errorMessage = "IOException caught: " + e.getMessage();
            LOG.error(errorMessage);
            dictionary.put("success", typeFactory.createBoolean(false));
            dictionary.put("errorMessage", typeFactory.createString(errorMessage));
            return typeFactory.toTypedValue(dictionary);
        }

        dictionary.put("success", typeFactory.createBoolean(true));
        dictionary.put("values", lines);

        return typeFactory.toTypedValue(dictionary);
    }
}
