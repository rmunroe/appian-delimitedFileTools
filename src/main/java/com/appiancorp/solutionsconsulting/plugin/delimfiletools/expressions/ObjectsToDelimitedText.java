package com.appiancorp.solutionsconsulting.plugin.delimfiletools.expressions;


import com.appiancorp.ps.plugins.typetransformer.AppianObject;
import com.appiancorp.ps.plugins.typetransformer.AppianTypeFactory;
import com.appiancorp.solutionsconsulting.plugin.delimfiletools.helpers.TypeHelper;
import com.appiancorp.solutionsconsulting.plugin.delimfiletools.helpers.WriteHelper;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.common.exceptions.StorageLimitException;
import com.appiancorp.suiteapi.content.exceptions.DuplicateUuidException;
import com.appiancorp.suiteapi.content.exceptions.InsufficientNameUniquenessException;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;


@DelimFileToolsCategory
public class ObjectsToDelimitedText {

    @Function
    public TypedValue objectsToDelimitedText(TypeService typeService,
                                             @Parameter TypedValue sourceObjects,
                                             @Parameter Boolean autoHeaderRow,
                                             @Parameter String[] headerRow,
                                             @Parameter String separator,
                                             @Parameter String quoteChar,
                                             @Parameter Boolean applyQuotesToAll,
                                             @Parameter String escapeChar,
                                             @Parameter String lineEnd
    ) {
        AppianTypeFactory typeFactory = AppianTypeFactory.newInstance(typeService);
        AppianObject dictionary = (AppianObject) typeFactory.createElement(AppianType.DICTIONARY);

        // Oddly, passing null in for sourceObjects results in an empty String
        if (sourceObjects.getInstanceType() == AppianType.STRING || !TypeHelper.isListDictOrCdt(typeService, sourceObjects)) {
            System.out.println("sourceObjects was null");
            dictionary.put("success", typeFactory.createBoolean(false));
            dictionary.put("errorMessage", typeFactory.createString("sourceObjects must be a valid List of Dictionaries or CDTs."));
            return typeFactory.toTypedValue(dictionary);
        }

        String errorMessage = "";
        if (separator.isEmpty())
            errorMessage = "You must supply a delimited separator, e.g. a comma or a tab (=char(9))";
        if (separator.length() > 1)
            errorMessage = "You must supply a single character as the delimited separator";

        if (escapeChar.isEmpty())
            errorMessage = "You must supply an escape character to escape your separator if in a cell value, e.g. \\";
        if (escapeChar.length() > 1)
            errorMessage = "You must supply a single character as the escape character";

        if (quoteChar.isEmpty())
            errorMessage = "You must supply a quote character to enclose your delimited values, e.g. \" - Note, you can choose to ignore quotes by setting the ignoreQuotes parameter to true.";
        if (quoteChar.length() > 1)
            errorMessage = "You must supply a single character as the quote character";

        if (!errorMessage.isEmpty()) {
            dictionary.put("success", typeFactory.createBoolean(false));
            dictionary.put("errorMessage", typeFactory.createString(errorMessage));
            return typeFactory.toTypedValue(dictionary);
        }

        // Header row
        String[] header;
        if (autoHeaderRow)
            try {
                header = WriteHelper.getFieldNamesFromSourceObjects(typeService, sourceObjects);
            } catch (Exception e) {
                dictionary.put("success", typeFactory.createBoolean(false));
                dictionary.put("errorMessage", typeFactory.createString("Exception: " + e.getLocalizedMessage()));
                return typeFactory.toTypedValue(dictionary);
            }
        else if (headerRow != null && headerRow.length > 0)
            header = headerRow;
        else
            header = null;

        List<String[]> rows = WriteHelper.getValueStringsFromSourceObjects(typeService, sourceObjects);

        // Get memory writer since we're just returning text
        StringWriter writer = new StringWriter();

        try {
            WriteHelper.writeDelimFileToWriter(writer, header, rows, separator.charAt(0), quoteChar.charAt(0), applyQuotesToAll, escapeChar.charAt(0), lineEnd);
        } catch (IOException e) {
            errorMessage = "Caught IOException: " + e.getLocalizedMessage();
        } catch (InvalidContentException e) {
            errorMessage = "Caught InvalidContentException: " + e.getLocalizedMessage();
        } catch (InsufficientNameUniquenessException e) {
            errorMessage = "Caught InsufficientNameUniquenessException: " + e.getLocalizedMessage();
        } catch (DuplicateUuidException e) {
            errorMessage = "Caught DuplicateUuidException: " + e.getLocalizedMessage();
        } catch (StorageLimitException e) {
            errorMessage = "Caught StorageLimitException: " + e.getLocalizedMessage();
        } catch (PrivilegeException e) {
            errorMessage = "Caught PrivilegeException: " + e.getLocalizedMessage();
        }
        if (!errorMessage.isEmpty()) {
            dictionary.put("success", typeFactory.createBoolean(false));
            dictionary.put("errorMessage", typeFactory.createString(errorMessage));
            return typeFactory.toTypedValue(dictionary);
        }

        dictionary.put("success", typeFactory.createBoolean(true));
        dictionary.put("value", typeFactory.createString(writer.toString()));

        return typeFactory.toTypedValue(dictionary);
    }
}
