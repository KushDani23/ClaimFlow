package com.company.icps.common.constants;

public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // Pagination defaults
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // File upload
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final String[] ALLOWED_FILE_TYPES = {"application/pdf", "image/jpeg", "image/png"};
    public static final String[] ALLOWED_FILE_EXTENSIONS = {".pdf", ".jpg", ".jpeg", ".png"};

    // JWT
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
}
