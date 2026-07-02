package com.example.certificateportal.suggestion;

public enum SuggestionCategory {
    FREE("자유"),
    SUGGESTION("건의");

    private final String displayName;

    SuggestionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
