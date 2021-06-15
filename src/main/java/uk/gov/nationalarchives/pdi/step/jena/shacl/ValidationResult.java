package uk.gov.nationalarchives.pdi.step.jena.shacl;

public class ValidationResult {

    private StringBuilder errorMessageBuilder = new StringBuilder();
    private Boolean hasErrors = false;
    private Long errorCount = 0L;

    public Boolean hasErrors() {
        return hasErrors;
    }

    public void setHasErrors(final Boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void incrementErrorCount() {
        errorCount++;
    }

    public String getAllErrors() {
        return errorMessageBuilder.toString();
    }

    public void appendError(String errorMessage) {
        errorMessageBuilder.append(errorMessage);
    }

}
