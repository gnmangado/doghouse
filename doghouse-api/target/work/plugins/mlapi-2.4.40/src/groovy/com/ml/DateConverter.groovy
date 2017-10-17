package com.ml

import java.text.DateFormat
import java.text.SimpleDateFormat

class DateConverter {
    public static final String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z"

    static def rfc1123format(Date date) {
        DateFormat rfc1123Format = new SimpleDateFormat(RFC1123_PATTERN, Locale.US)
        return rfc1123Format.format(date)
    }
    static def rfc1123parse(String date) {
        DateFormat rfc1123Format = new SimpleDateFormat(RFC1123_PATTERN, Locale.US)
        return rfc1123Format.parse(date)
    }
}
