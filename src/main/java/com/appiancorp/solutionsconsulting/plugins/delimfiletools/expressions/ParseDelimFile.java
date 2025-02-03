package com.appiancorp.solutionsconsulting.plugins.delimfiletools.expressions;

import com.appiancorp.ps.plugins.typetransformer.AppianTypeFactory;
import com.appiancorp.solutionsconsulting.plugins.delimfiletools.helpers.ParseHelper;
import com.appiancorp.suiteapi.common.paging.PagingInfo;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.type.Type;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;
import com.opencsv.CSVParserBuilder;
import com.opencsv.ICSVParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;


@DelimFileToolsCategory
public class ParseDelimFile {
    private static final Logger LOG = (Logger) LogManager.getLogger(ParseDelimFile.class);

    /**
     * The "Swiss Army Knife" of delimited text file parsing
     *
     * @param typeService             TypeService injected by Appian
     * @param contentService          ContentService injected by Appian
     * @param delimitedFile           ID of Appian Document to parse
     * @param separator               Sets the delimiter to use for separating entries
     * @param hasHeaderRow            If true, the first line of the file contains the column / field names
     * @param escapeChar              Sets the character to use for escaping a separator or quote
     * @param quoteChar               Sets the character to use for quoted elements
     * @param ignoreQuotes            If true, quotations are ignored
     * @param strictQuotes            If true, characters outside the quotes are ignored
     * @param ignoreLeadingWhiteSpace If true, white space in front of a quote in a field is ignored
     * @param includeTotalCount       If true, the results will contain 'totalLines' with the number of lines in the Document
     * @param pagingInfo              An a!pagingInfo() instance. Allows for batching of the file. Sorting not yet supported.
     * @return a Dictionary containing the success value, an error message, and an array of Dictionaries as the values. If a header row is included, value Dictionary key names will be the values of the first row, otherwise they will be the 1-based number of the column.
     */
    @Function
    public TypedValue parseDelimFile(
            TypeService typeService,
            ContentService contentService,
            @Parameter @DocumentDataType Long delimitedFile,

            @Parameter String separator,
            @Parameter Boolean hasHeaderRow,
            @Parameter String escapeChar,
            @Parameter String quoteChar,
            @Parameter Boolean ignoreQuotes,
            @Parameter Boolean strictQuotes,
            @Parameter Boolean ignoreLeadingWhiteSpace,
            @Parameter Boolean includeTotalCount,
            @Parameter(required = false) @Type(namespace = Type.APPIAN_NAMESPACE, name = PagingInfo.LOCAL_PART) PagingInfo pagingInfo
    ) {
        LOG.debug("Executing parseDelimFile - " +
                "delimitedFile: " + delimitedFile +
                ", separator: '" + separator +
                "', hasHeaderRow: " + Boolean.toString(hasHeaderRow) +
                ", escapeChar: '" + escapeChar +
                "', quoteChar: '" + quoteChar +
                "', ignoreQuotes: " + Boolean.toString(ignoreQuotes) +
                ", strictQuotes: " + Boolean.toString(strictQuotes) +
                ", ignoreLeadingWhiteSpace: " + Boolean.toString(ignoreLeadingWhiteSpace) +
                ", includeTotalCount: " + Boolean.toString(includeTotalCount) +
                ", pagingInfo: " + Boolean.toString(pagingInfo != null) +
                ";"
        );

        AppianTypeFactory typeFactory = AppianTypeFactory.newInstance(typeService);

        // Validate the parameters
        String errorMessage = validateParameters(contentService, delimitedFile, separator, escapeChar, quoteChar, pagingInfo);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return ParseHelper.getErrorReturnValue(typeFactory, errorMessage);
        }

        // Set up the CSVParser instance
        ICSVParser parser = new CSVParserBuilder()
                .withSeparator(separator.charAt(0))
                .withEscapeChar(escapeChar.charAt(0))
                .withQuoteChar(quoteChar.charAt(0))
                .withIgnoreQuotations(ignoreQuotes)
                .withStrictQuotes(strictQuotes)
                .withIgnoreLeadingWhiteSpace(ignoreLeadingWhiteSpace)
                .build();

        // Parse the file
        try {
            return ParseHelper.parseFile(contentService, typeFactory, parser, delimitedFile, hasHeaderRow, includeTotalCount, pagingInfo);
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
     * @param separator      Sets the delimiter to use for separating entries
     * @param escapeChar     Sets the character to use for escaping a separator or quote
     * @param quoteChar      Sets the character to use for quoted elements
     * @return A Dictionary if there was an error (null means validation passes)
     */
    private String validateParameters(ContentService contentService, Long delimitedFile, String separator, String escapeChar, String quoteChar, PagingInfo pagingInfo) {
        String filePath = ParseHelper.checkFileExists(contentService, delimitedFile);
        if (filePath != null) return filePath;

        if (separator.isEmpty())
            return "You must supply a delimited separator, e.g. a comma or a tab (=char(9))";
        if (separator.length() > 1)
            return "You must supply a single character as the delimited separator";

        if (escapeChar.isEmpty())
            return "You must supply an escape character to escape your separator if in a cell value, e.g. \\";
        if (escapeChar.length() > 1)
            return "You must supply a single character as the escape character";

        if (quoteChar.isEmpty())
            return "You must supply a quote character to enclose your delimited values, e.g. \" - Note, you can choose to ignore quotes by setting the ignoreQuotes parameter to true.";
        if (quoteChar.length() > 1)
            return "You must supply a single character as the quote character";

        if (pagingInfo != null) {
            if (pagingInfo.getStartIndex() < 1)
                return "The pagingInfo.startIndex must be greater than or equal to 1";
            if (pagingInfo.getBatchSize() == 0)
                return "The pagingInfo.batchSize must be greater than or equal to 1";
        }

        return null;
    }
}
