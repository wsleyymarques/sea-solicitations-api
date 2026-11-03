package br.com.sea.solicitations.solicitation.application.port.in;

public enum SearchSortField {
    SUBMITTED_AT("submittedAt"),
    CREATED_AT("createdAt"),
    PREFERRED_DATE("preferredDate");

    private final String fieldName;

    SearchSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }
}
