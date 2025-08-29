package com.hdfcbank.sfmsconsumer.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlSanitizer {
    /**
     * Removes all \n and \r from the XML except inside <sig:XMLSgntrs>...</sig:XMLSgntrs> blocks.
     */
    public static String sanitize(String input) {
        // Regex to match the signature block (non-greedy, DOTALL for multiline)
        Pattern sigPattern = Pattern.compile("(<sig:XMLSgntrs>.*?</sig:XMLSgntrs>)", Pattern.DOTALL);
        Matcher matcher = sigPattern.matcher(input);

        String sigBlock = "";
        if (matcher.find()) {
            sigBlock = matcher.group(1);
            // Remove the signature block from the input
            input = matcher.replaceFirst("___SIG_PLACEHOLDER___");
        }

        // Step 1: Unescape if clearly escaped (Java-style: \\n, \\r, \")
        if (input.contains("\\\\n") || input.contains("\\\\r") || input.contains("\\\"")) {
            input = input
                    .replace("\\\\n", "\n")   // literal \n → real newline
                    .replace("\\\\r", "\r")   // literal \r → real carriage return
                    .replace("\\\"", "\"");   // literal \" → real quote
        }

        // Step 2: Remove *unwanted* literal escape sequences if any remain
        String cleanedXml = input
                .replaceAll("\\\\r\\\\n", "") // literal "\r\n"
                .replaceAll("\\\\n", "")      // literal "\n"
                .replaceAll("\\\\r", "");     // literal "\r"

        //  Important: do NOT remove real \r or \n here unless you
        // want a fully single-line XML. If you do, uncomment:
        // input = input.replaceAll("[\\r\\n]", "");

        // Restore the signature block
        if (!sigBlock.isEmpty()) {
            cleanedXml = cleanedXml.replace("___SIG_PLACEHOLDER___", sigBlock);
        }

        return cleanedXml.trim();
    }
}