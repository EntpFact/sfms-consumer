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

        // Remove all \n and \r from the rest
        String cleaned = input
                .replaceAll("\\\\r\\\\n", "")  // removes literal "\r\n"
                .replaceAll("[\\r\\n]", "");   // removes actual carriage returns and newlines

        // Restore the signature block
        if (!sigBlock.isEmpty()) {
            cleaned = cleaned.replace("___SIG_PLACEHOLDER___", sigBlock);
        }

        return cleaned.trim();
    }
}