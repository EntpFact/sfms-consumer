package com.hdfcbank.sfmsconsumer.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlSanitizer {
    /**
     * Removes all \n and \r from the XML except inside <sig:XMLSgntrs>...</sig:XMLSgntrs> blocks.
     * Handles signature blocks with or without attributes.
     * Only the first signature block preserves newlines, subsequent ones have newlines removed.
     * For nested signature blocks, only the outermost preserves newlines.
     */
    public static String sanitize(String input) {
        // Handle null input
        if (input == null) {
            return "";
        }
        
        // Remove all newlines from the input first
        String cleaned = removeNewlines(input);
        
        // Then restore newlines only in the first signature block, but remove newlines from nested signature blocks
        String result = restoreFirstSignatureBlockNewlines(cleaned, input);
        
        // Final trim to remove any remaining whitespace
        return result.trim();
    }
    
    /**
     * Removes all newlines and carriage returns from the given string.
     */
    private static String removeNewlines(String input) {
        return input
                .replaceAll("\\\\r\\\\n", "")  // removes literal "\r\n"
                .replaceAll("[\\r\\n]", "");   // removes actual carriage returns and newlines
    }
    
    /**
     * Restores newlines only in the first signature block found, but removes newlines from nested signature blocks.
     */
    private static String restoreFirstSignatureBlockNewlines(String cleaned, String original) {
        // Regex to match signature blocks in the original input
        Pattern sigPattern = Pattern.compile("(<sig:XMLSgntrs[^>]*>.*?</sig:XMLSgntrs>)", Pattern.DOTALL);
        Matcher originalMatcher = sigPattern.matcher(original);
        
        if (!originalMatcher.find()) {
            return cleaned; // No signature blocks found
        }
        
        // Get the first signature block from the original input
        String firstSigBlock = originalMatcher.group(1);
        
        // Process the first signature block to remove newlines from nested signature blocks
        String processedFirstSigBlock = processSignatureBlockContent(firstSigBlock);
        
        // Find the first signature block in the cleaned input
        Matcher cleanedMatcher = sigPattern.matcher(cleaned);
        if (!cleanedMatcher.find()) {
            return cleaned; // No signature blocks found in cleaned input
        }
        
        // Replace the first signature block in cleaned input with the processed version
        StringBuffer result = new StringBuffer();
        cleanedMatcher.appendReplacement(result, Matcher.quoteReplacement(processedFirstSigBlock));
        cleanedMatcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Processes the content inside a signature block, removing newlines from nested signature blocks.
     */
    private static String processSignatureBlockContent(String sigBlock) {
        // Find nested signature blocks and remove their newlines
        Pattern nestedSigPattern = Pattern.compile("(<sig:XMLSgntrs[^>]*>.*?</sig:XMLSgntrs>)", Pattern.DOTALL);
        Matcher nestedMatcher = nestedSigPattern.matcher(sigBlock);
        
        StringBuffer result = new StringBuffer();
        
        while (nestedMatcher.find()) {
            String nestedSigBlock = nestedMatcher.group(1);
            // Remove newlines from nested signature blocks
            String processedNestedBlock = removeNewlines(nestedSigBlock);
            nestedMatcher.appendReplacement(result, Matcher.quoteReplacement(processedNestedBlock));
        }
        nestedMatcher.appendTail(result);
        
        return result.toString();
    }
}