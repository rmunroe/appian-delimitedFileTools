package com.appiancorp.solutionsconsulting.plugins.delimfiletools.helpers;

import com.appiancorp.solutionsconsulting.plugins.delimfiletools.exceptions.InvalidCdtException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.common.exceptions.StorageLimitException;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.DuplicateUuidException;
import com.appiancorp.suiteapi.content.exceptions.InsufficientNameUniquenessException;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;
import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * Class for helping with the writing of delimited files, including to Appian's content management store.
 */
public class WriteHelper {
    private static final Logger LOG = (Logger) LogManager.getLogger(WriteHelper.class);

    /**
     * Writes a delimited file and returns an Appian Document ID
     *
     * @param contentService   A ContentService instance injected by Appian
     * @param parentFolder     The ID of the parent folder to save the doc in
     * @param docName          The base name of the document, not including extension
     * @param appendToExisting Whether to overwrite the file or add to the end of it
     * @param extension        The file extension of the document
     * @param headerRow        A String[] listing the header row (skipped if null)
     * @param rows             The List of String[] containing the data to write
     * @param separator        The character to separate fields with, e.g. comma (,)
     * @param quoteChar        The character to enclose a field with, usually double-quotes
     * @param applyQuotesToAll Enclose every field with the quoteChar
     * @param escapeChar       The character used to escape any quoteChar instances found in the data
     * @param lineEnd          The line ending character(s) to write, eg. /n or /r/n
     * @return The ID of the new document
     * @throws IOException                         If an I/O error occurs
     * @throws InvalidContentException
     * @throws InsufficientNameUniquenessException
     * @throws DuplicateUuidException
     * @throws StorageLimitException
     * @throws PrivilegeException
     */
    public static Long writeDelimFile(
            ContentService contentService,
            Long parentFolder,
            String docName,
            Boolean appendToExisting,
            String extension,
            String[] headerRow,
            List<String[]> rows,
            char separator,
            char quoteChar,
            Boolean applyQuotesToAll,
            char escapeChar,
            String lineEnd
    ) throws IOException,
            InvalidContentException,
            InsufficientNameUniquenessException,
            DuplicateUuidException,
            StorageLimitException,
            PrivilegeException {

        LOG.debug("Entering writeDelimFile");

        // TODO: HANDLE APPEND TO EXISTING

        // Create an empty doc in Appian
        Long newDoc = createAppianDocument(contentService, docName, extension, parentFolder);

        // Get an open writer to that empty doc
        FileWriter fileWriter = getWriterForAppianDocument(contentService, newDoc);

        // Write the file to the FileWriter
        writeDelimFileToWriter(fileWriter, headerRow, rows, separator, quoteChar, applyQuotesToAll, escapeChar, lineEnd);

        try {
            closeWriterForAppianDocument(contentService, fileWriter, newDoc);
        } catch (InvalidContentException | PrivilegeException ignored) {
            // This could only happen if the file was deleted or permissions changed during writing
        }

        return newDoc;
    }


    /**
     * Writes a delimited file to the supplied Writer
     *
     * @param writer           An open java.io.Writer
     * @param headerRow        A String[] listing the header row (skipped if null)
     * @param rows             The List of String[] containing the data to write
     * @param separator        The character to separate fields with, e.g. comma (,)
     * @param quoteChar        The character to enclose a field with, usually double-quotes
     * @param applyQuotesToAll Enclose every field with the quoteChar
     * @param escapeChar       The character used to escape any quoteChar instances found in the data
     * @param lineEnd          The line ending character(s) to write, eg. /n or /r/n
     * @throws IOException If an I/O error occurs
     */
    public static void writeDelimFileToWriter(
            Writer writer,
            String[] headerRow,
            List<String[]> rows,
            char separator,
            char quoteChar,
            Boolean applyQuotesToAll,
            char escapeChar,
            String lineEnd
    ) throws IOException,
            InvalidContentException,
            InsufficientNameUniquenessException,
            DuplicateUuidException,
            StorageLimitException,
            PrivilegeException {

        LOG.debug("Entering writeDelimFileToWriter");

        if (applyQuotesToAll == null) applyQuotesToAll = false;

        // Get a CSVWriter instance, ready for writing to the empty doc
        CSVWriter csvWriter = new CSVWriter(writer, separator, quoteChar, escapeChar, lineEnd);

        // Write the header row
        if (headerRow != null && headerRow.length > 0)
            csvWriter.writeNext(headerRow, applyQuotesToAll);

        // Write the rest of the rows
        csvWriter.writeAll(rows, applyQuotesToAll);

        // Cleanup
        csvWriter.close();
    }


    /**
     * Takes the first object (Dictionary or CDT) in the sourceObjects and returns the field names, useful for
     *
     * @param typeService
     * @param sourceObjects
     * @return
     */
    public static String[] getFieldNamesFromSourceObjects(TypeService typeService, TypedValue sourceObjects) throws Exception {
        ArrayList<HashMap<TypedValue, TypedValue>> maps = new ArrayList<HashMap<TypedValue, TypedValue>>();
        try {
            maps = TypeHelper.toMapList(typeService, sourceObjects);
        } catch (InvalidCdtException e) {
            e.printStackTrace();
        }

        if (maps.isEmpty()) throw new Exception("sourceObjects was empty");

        Set<TypedValue> keySet = maps.get(0).keySet();
        ArrayList<String> fieldNames = new ArrayList<>(keySet.size());
        for (TypedValue key : keySet) {
            String fieldName = key.getValue().toString();
            fieldName = fieldName.replaceAll("[_]", " ");
            fieldNames.add(fieldName);
        }

        String[] header = fieldNames.toArray(new String[fieldNames.size()]);
        LOG.debug("Got header values: " + String.join(", ", header));

        return header;
    }


    /**
     * Transforms the values of a list of Dictionary or CDT into a list of string arrays, suitable for writing to the delimited file
     *
     * @param typeService
     * @param sourceObjects
     * @return
     */
    public static List<String[]> getValueStringsFromSourceObjects(TypeService typeService, TypedValue sourceObjects) {
        ArrayList<HashMap<TypedValue, TypedValue>> maps = new ArrayList<HashMap<TypedValue, TypedValue>>();
        try {
            maps = TypeHelper.toMapList(typeService, sourceObjects);
        } catch (InvalidCdtException e) {
            e.printStackTrace();
        }

        LOG.debug(" * Got Map List: count: " + maps.size() + " - " + maps.toString());

        ArrayList<String[]> output = new ArrayList<>();

        for (HashMap<TypedValue, TypedValue> row : maps) {
            LOG.debug("   * Got row: value count: " + row.values().size() + " - " + row.toString());

            ArrayList<String> outRowList = new ArrayList<>();

            for (TypedValue value : row.values()) {
                // TODO: Handle gracefully if a nested type or list is passed
                outRowList.add(value.getValue().toString());
            }

            String[] stringArray = outRowList.toArray(new String[outRowList.size()]);

            output.add(stringArray);
        }

        return output;
    }


    /**
     * Creates a new Document in Appian's content management
     *
     * @param contentService A ContentService instance injected by Appian
     * @param docName        The base name of the document, not including extension
     * @param extension      The file extension of the document
     * @param saveInFolder   The ID of the parent folder to save the doc in
     * @return The ID of the new document
     * @throws InsufficientNameUniquenessException thrown by Appian
     * @throws InvalidContentException             thrown by Appian
     * @throws DuplicateUuidException              thrown by Appian
     * @throws StorageLimitException               thrown by Appian
     * @throws PrivilegeException                  thrown by Appian
     */
    private static Long createAppianDocument(ContentService contentService, String docName, String extension, Long saveInFolder) throws InsufficientNameUniquenessException, InvalidContentException, DuplicateUuidException, StorageLimitException, PrivilegeException {
        Document doc = new Document();
        doc.setName(docName);
        doc.setExtension(extension);
        doc.setParent(saveInFolder);

        Long newDoc = contentService.create(doc, ContentConstants.UNIQUE_NONE);
        LOG.debug("Created new Appian document, id = " + newDoc);

        return newDoc;
    }


    /**
     * Creates a FileWriter instance for an Appian Document
     *
     * @param contentService A ContentService instance injected by Appian
     * @param document       The Appian Document to write to
     * @return an open FileWriter, ready for writing
     * @throws InvalidContentException thrown by Appian
     * @throws IOException             if the named file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened for any other reason
     */
    private static FileWriter getWriterForAppianDocument(ContentService contentService, Long document) throws InvalidContentException, IOException {
        String fileName = contentService.getInternalFilename(document);
        return new FileWriter(fileName);
    }


    /**
     * Cleanly closes the Writer and updates the size in Appian's content management
     *
     * @param contentService
     * @param writer
     * @param document
     * @throws InvalidContentException thrown by Appian
     * @throws PrivilegeException      thrown by Appian
     * @throws IOException             If an I/O error occurs
     */
    private static void closeWriterForAppianDocument(ContentService contentService, FileWriter writer, Long document) throws InvalidContentException, PrivilegeException, IOException {
        writer.close();
        contentService.setSizeOfDocumentVersion(document);
    }
}
