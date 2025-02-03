package com.appiancorp.solutionsconsulting.plugins.delimfiletools.expressions;

import com.appiancorp.ps.plugins.typetransformer.AppianObject;
import com.appiancorp.ps.plugins.typetransformer.AppianTypeFactory;
import com.appiancorp.solutionsconsulting.plugins.delimfiletools.helpers.ParseHelper;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;


@DelimFileToolsCategory
public class ReadLinesFromTextFile {
    private static final Logger LOG = (Logger) LogManager.getLogger(ReadLinesFromTextFile.class);

    /**
     * Reads lines in a text file. Returns a dictionary with status and values.
     *
     * @param contentService ContentService injected by Appian
     * @param textFile       ID of Appian Document to read
     * @return A dictionary containing the results.
     */
    @Function
    public TypedValue readLinesFromTextFile(TypeService typeService,
                                            ContentService contentService,
                                            @Parameter @DocumentDataType Long textFile,
                                            @Parameter int startLine,
                                            @Parameter int lineCount) {
        String filePath;

        AppianTypeFactory typeFactory = AppianTypeFactory.newInstance(typeService);

        try {
            filePath = contentService.getInternalFilename(textFile);
        } catch (InvalidContentException e) {
            String errorMessage = "InvalidContentException caught: " + e.getMessage();
            LOG.error(errorMessage);
            AppianObject dict = (AppianObject) typeFactory.createElement(AppianType.DICTIONARY);
            dict.put("success", typeFactory.createBoolean(false));
            dict.put("errorMessage", typeFactory.createString(errorMessage));
            return typeFactory.toTypedValue(dict);
        }

        return ParseHelper.readLinesInFile(typeFactory, filePath, startLine, lineCount);
    }
}
