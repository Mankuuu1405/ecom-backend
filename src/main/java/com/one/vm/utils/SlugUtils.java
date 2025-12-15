package com.one.vm.utils;

public final class SlugUtils {

    private SlugUtils() {}

    public static String from(String input) {
        return input == null ? null :
                input.toLowerCase()
                        .trim()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
    }
}

