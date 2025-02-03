package com.appiancorp.solutionsconsulting.plugins.delimfiletools.expressions;

import com.appiancorp.solutionsconsulting.plugins.delimfiletools.helpers.ParseHelper;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;


@DelimFileToolsCategory
public class CountLinesInTextFile {
    private static final Logger LOG = (Logger) LogManager.getLogger(CountLinesInTextFile.class);

    /**
     * Counts the lines in a text file, returning 0 or higher. Returns -1 on error.
     *
     * @param contentService ContentService injected by Appian
     * @param textFile  ID of Appian Document to count
     * @return The number of lines in the file, or -1 if there was any error.
     */
    @Function
    public int countLinesInTextFile(
            ContentService contentService,
            @Parameter @DocumentDataType Long textFile) {

        String filePath;
        try {
            filePath = contentService.getInternalFilename(textFile);
        } catch (InvalidContentException e) {
            LOG.error("InvalidContentException caught: " + e.getMessage());
            return -1;
        }

        return ParseHelper.countLinesInFile(filePath);
    }
}