package com.appiancorp.solutionsconsulting.plugin.delimfiletools.smartservices;

import com.appiancorp.solutionsconsulting.plugin.delimfiletools.helpers.WriteHelper;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.common.exceptions.StorageLimitException;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.DuplicateUuidException;
import com.appiancorp.suiteapi.content.exceptions.InsufficientNameUniquenessException;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.*;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;


@PaletteInfo(paletteCategory = "Custom Services", palette = "Delimited File Tools")
@Order({
        "parentFolder",
        "outputFileName",
        "appendToExisting",
        "sourceObjects",
        "autoHeaderRow",
        "headerRow",
        "fieldSeparator",
        "quoteCharacter",
        "applyQuotesToAll",
        "escapeCharacter",
        "lineEndingStyle"
})
public class WriteDelimitedFile extends AppianSmartService {

    private static final Logger LOG = (Logger) LogManager.getLogger(WriteDelimitedFile.class);

    private SmartServiceContext smartServiceCtx;
    private ContentService contentService;
    private TypeService typeService;

    private TypedValue sourceObjects;
    private Long parentFolder;
    private Boolean appendToExisting = false;
    private String docName;
    private Boolean autoHeaderRow = false;
    private String[] headerRow;
    private String separator;
    private String quoteChar;
    private Boolean applyQuotesToAll = false;
    private String escapeChar;
    private String lineEndings;

    private Boolean success;
    private String errorMessage;
    private Long outputFile;


    @Override
    public void run() throws SmartServiceException {
        LOG.debug("Running WriteDelimitedFile");

        char cSeparator = this.separator.toCharArray()[0];
        char cQuoteChar = this.quoteChar.toCharArray()[0];
        char cEscapeChar = this.escapeChar.toCharArray()[0];

        // Resolve the extension
        String extension = (cSeparator == ',') ? "csv" : "txt";
        if (docName.matches("\\.\\w+$")) {
            String[] parts = docName.split("\\.");          // split on dot
            extension = parts[parts.length - 1];            // get last element
            docName = docName.replaceFirst("\\.\\w+$", ""); // trim extension
        }

        // Determine line endings from enumeration value
        String lineEnd = (Objects.equals(this.lineEndings, "dos")) ? "\r\n" : "\n";

        // Header row
        String[] header;
        if (this.autoHeaderRow != null && this.autoHeaderRow)
            try {
                header = WriteHelper.getFieldNamesFromSourceObjects(typeService, sourceObjects);
            } catch (Exception e) {
                this.success = false;
                this.errorMessage = "Exception: " + e.getLocalizedMessage();
                return;
            }
        else if (this.headerRow != null && this.headerRow.length > 0)
            header = this.headerRow;
        else
            header = null;

        // Get the values of each object as strings
        List<String[]> data = WriteHelper.getValueStringsFromSourceObjects(typeService, sourceObjects);

        try {
            this.outputFile = WriteHelper.writeDelimFile(
                    this.contentService,
                    this.parentFolder,
                    this.docName,
                    this.appendToExisting,
                    extension,
                    header,
                    data,
                    cSeparator,
                    cQuoteChar,
                    this.applyQuotesToAll,
                    cEscapeChar,
                    lineEnd
            );
        } catch (IOException e) {
            this.success = false;
            this.errorMessage = "An IOException exception was thrown: " + e.getLocalizedMessage();
            return;
        } catch (StorageLimitException e) {
            this.success = false;
            this.errorMessage = "Appian returned a StorageLimitException exception: " + e.getLocalizedMessage();
            return;
        } catch (InsufficientNameUniquenessException e) {
            this.success = false;
            this.errorMessage = "Appian returned a InsufficientNameUniquenessException exception: " + e.getLocalizedMessage();
            return;
        } catch (InvalidContentException e) {
            this.success = false;
            this.errorMessage = "Appian returned a InvalidContentException exception: " + e.getLocalizedMessage();
            return;
        } catch (PrivilegeException e) {
            this.success = false;
            this.errorMessage = "Appian returned a PrivilegeException exception: " + e.getLocalizedMessage();
            return;
        } catch (DuplicateUuidException e) {
            this.success = false;
            this.errorMessage = "Appian returned a DuplicateUuidException exception: " + e.getLocalizedMessage();
            return;
        }

        this.success = true;
    }


    public WriteDelimitedFile(SmartServiceContext smartServiceCtx, ContentService contentService, TypeService typeService) {
        super();
        this.smartServiceCtx = smartServiceCtx;
        this.contentService = contentService;
        this.typeService = typeService;
    }


    public void onSave(MessageContainer messages) {
    }


    public void validate(MessageContainer messages) {
        if (this.separator.length() != 1) messages.addError("Separator", "separator.singlechar");
        if (this.quoteChar.length() != 1) messages.addError("Quote Character", "quoteChar.singlechar");
        if (this.escapeChar.length() != 1) messages.addError("Escape Character", "escapeChar.singlechar");
    }


    @Input(required = Required.ALWAYS)
    @Name("parentFolder")
    @FolderDataType
    public void setParentFolder(Long val) {
        this.parentFolder = val;
    }

    @Input(required = Required.ALWAYS)
    @Name("outputFileName")
    public void setDocName(String val) {
        this.docName = val;
    }

    @Input(required = Required.ALWAYS, defaultValue = "true")
    @Name("appendToExisting")
    public void setAppendToExisting(Boolean val) {
        this.appendToExisting = val;
    }

    @Input(required = Required.ALWAYS)
    @Name("sourceObjects")
    public void setSourceObjects(TypedValue val) {
        this.sourceObjects = val;
    }

    @Input(required = Required.ALWAYS, defaultValue = "true")
    @Name("autoHeaderRow")
    public void setAutoHeaderRow(Boolean val) {
        this.autoHeaderRow = val;
    }

    @Input(required = Required.OPTIONAL)
    @Name("headerRow")
    public void setHeaderRow(String[] val) {
        this.headerRow = val;
    }

    @Input(required = Required.ALWAYS, defaultValue = ",")
    @Name("fieldSeparator")
    public void setSeparator(String val) {
        this.separator = val;
    }

    @Input(required = Required.ALWAYS, defaultValue = "\"")
    @Name("quoteCharacter")
    public void setQuoteChar(String val) {
        this.quoteChar = val;
    }

    @Input(required = Required.ALWAYS, defaultValue = "false")
    @Name("applyQuotesToAll")
    public void setApplyQuotesToAll(Boolean val) {
        this.applyQuotesToAll = val;
    }

    @Input(required = Required.ALWAYS, defaultValue = "\\")
    @Name("escapeCharacter")
    public void setEscapeChar(String val) {
        this.escapeChar = val;
    }

    @Input(required = Required.ALWAYS, enumeration = "line-endings", defaultValue = "unix")
    @Name("lineEndingStyle")
    public void setLineEndings(String val) {
        this.lineEndings = val;
    }


    @Name("success")
    public Boolean getSuccess() {
        return this.success;
    }

    @Name("errorMessage")
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Name("outputFile")
    @DocumentDataType
    public Long getOutputFile() {
        return this.outputFile;
    }
}