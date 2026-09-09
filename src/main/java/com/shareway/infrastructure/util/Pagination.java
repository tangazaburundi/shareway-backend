package com.shareway.infrastructure.util;

/**
 * Borne la pagination fournie par les clients pour éviter les
 * exfiltrations de masse (size = 10 000 → 100) et les scans profonds.
 */
public final class Pagination {

    public static final int MAX_SIZE = 100;
    public static final int MAX_PAGE = 10000;

    private Pagination() {
    }

    public static int sanitizePage(int page) {
        return Math.max(0, Math.min(page, MAX_PAGE));
    }

    public static int sanitizeSize(int size) {
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    public static int sanitizedSize(int size, int fallback) {
        int s = sanitizeSize(size);
        return s <= 0 ? (fallback > 0 ? Math.min(fallback, MAX_SIZE) : MAX_SIZE) : s;
    }
}