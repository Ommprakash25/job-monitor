package com.jobmonitor.sending;

import java.util.Locale;

/** Guesses a first name from an email address. Always editable afterwards. */
public final class NameGuesser {

    private NameGuesser() {
    }

    /** "priya.s@example.com" becomes "Priya". Returns "" when nothing looks like a name. */
    public static String guess(String email) {
        String local = email.substring(0, email.indexOf('@'));
        for (String part : local.split("[.\\d]")) {
            if (part.matches("[a-zA-Z]+")) {
                return capitalize(part);
            }
        }
        return "";
    }

    public static String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.ROOT);
    }
}
