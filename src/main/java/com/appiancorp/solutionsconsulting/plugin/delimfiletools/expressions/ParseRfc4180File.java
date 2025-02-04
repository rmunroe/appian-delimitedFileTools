package com.appiancorp.solutionsconsulting.plugin.delimfiletools.expressions;

import com.appiancorp.ps.plugins.typetransformer.AppianTypeFactory;
import com.appiancorp.solutionsconsulting.plugin.delimfiletools.helpers.ParseHelper;
import com.appiancorp.suiteapi.common.paging.PagingInfo;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.type.Type;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;
import com.opencsv.ICSVParser;
import com.opencsv.RFC4180ParserBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;


@DelimFileToolsCategory
public class ParseRfc4180File {
    private static final Logger LOG = (Logger) LogManager.getLogger(ParseDelimFile.class);


    /**
     * A function to parse an RFC 4180 standard compliant CSV file.
     *
     * @param typeService       TypeService injected by Appian
     * @param contentService    ContentService injected by Appian
     * @param csvFile           ID of Appian Document to parse
     * @param hasHeaderRow      If true, the first line of the file contains the column / field names
     * @param includeTotalCount If true, the results will contain 'totalLines' with the number of lines in the Document
     * @param pagingInfo        An a!pagingInfo() instance. Allows for batching of the file. Sorting not yet supported.
     * @return a Dictionary containing the success value, an error message, and an array of Dictionaries as the values. If a header row is included, value Dictionary key names will be the values of the first row, otherwise they will be the 1-based number of the column.
     * @see <a href="http://ap.pn/2gQaWj0">Wikipedia page on CSV standards</a>
     * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>
     */
    @Function
    public TypedValue parseRfc4180File(
            TypeService typeService,
            ContentService contentService,
            @Parameter @DocumentDataType Long csvFile,
            @Parameter(required = false) Boolean hasHeaderRow,
            @Parameter(required = false) Boolean includeTotalCount,
            @Parameter(required = false) @Type(namespace = Type.APPIAN_NAMESPACE, name = PagingInfo.LOCAL_PART) PagingInfo pagingInfo
    ) {
        LOG.debug("Executing parseRfc4180File - " +
                "csvFile: " + csvFile +
                ", hasHeaderRow: " + Boolean.toString(hasHeaderRow) +
                "', includeTotalCount: " + Boolean.toString(includeTotalCount) +
                "', pagingInfo: " + Boolean.toString(pagingInfo != null) +
                ";"
        );

        AppianTypeFactory typeFactory = AppianTypeFactory.newInstance(typeService);

        // Validate the parameters
        String errorMessage = validateParameters(contentService, csvFile, pagingInfo);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return ParseHelper.getErrorReturnValue(typeFactory, errorMessage);
        }

        // Set up the CSVParser instance
        ICSVParser parser = new RFC4180ParserBuilder().build();

        // Parse the file
        try {
            return ParseHelper.parseFile(contentService, typeFactory, parser, csvFile, hasHeaderRow, includeTotalCount, pagingInfo);
        } catch (InvalidContentException e) {
            errorMessage = "InvalidContentException caught: " + e.getMessage();
            LOG.error(errorMessage);
            return ParseHelper.getErrorReturnValue(typeFactory, errorMessage);
        }
    }


    /**
     * Validates the function arguments
     *
     * @param contentService ContentService injected by Appian
     * @param delimitedFile  ID of Appian Document to parse
     * @return A Dictionary if there was an error (null means validation passes)
     */
    private String validateParameters(ContentService contentService, Long delimitedFile, PagingInfo pagingInfo) {
//        String filePath = ParseHelper.checkFileExists(contentService, delimitedFile);
//        if (filePath != null) return filePath;

        if (pagingInfo != null) {
            if (pagingInfo.getStartIndex() < 1)
                return "The pagingInfo.startIndex must be greater than or equal to 1";
            if (pagingInfo.getBatchSize() == 0)
                return "The pagingInfo.batchSize must be greater than or equal to 1";
        }

        return null;
    }
}
