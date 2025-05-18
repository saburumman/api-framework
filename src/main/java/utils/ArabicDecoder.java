package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArabicDecoder {

    public static void main(String[] args) {
        // Sample JSON with Unicode-encoded Arabic characters
        String jsonResponse = "{\"status\":true,\"message\":\"Success\",\"data\":{\"current_page\":1,\"data\":[{\"id\":4,\"downloads\":7,\"name\":\"General questions on physics\",\"slug\":\"general-questions-on-physics\",\"description\":\"General questions on physics\",\"keywords\":\"General questions on physics\",\"subject\":{\"id\":\"\",\"name\":\"\\u0641\\u064a\\u0632\\u064a\\u0627\\u0621 \\u0627\\u0644\\u0635\\u0641 \\u0627\\u0644\\u0639\\u0627\\u0634\\u0631\"}}}}";

        try {
            // Process the response to decode Unicode-encoded text
            String decodedResponse = decodeUnicodeJson(jsonResponse);
            System.out.println("Decoded Response: " + decodedResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String decodeUnicodeJson(String jsonResponse) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonResponse);

        // Recursively decode all Unicode escape sequences in the JSON
        recursivelyDecodeJsonNode(rootNode);

        // Return the processed JSON as a string
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    private static void recursivelyDecodeJsonNode(JsonNode node) {
        if (node.isObject()) {
            // If it's an object, loop through all fields and recursively decode them
            node.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = node.get(fieldName);
                recursivelyDecodeJsonNode(fieldValue);
            });
        } else if (node.isArray()) {
            // If it's an array, loop through all elements and recursively decode them
            for (JsonNode arrayElement : node) {
                recursivelyDecodeJsonNode(arrayElement);
            }
        } else if (node.isTextual()) {
            // If it's a textual node, decode the Unicode
            String text = node.asText();
            // Decode Unicode characters
            String decodedText = decodeUnicode(text);
            ((com.fasterxml.jackson.databind.node.TextNode) node).findValue(decodedText);
        }
    }

    private static String decodeUnicode(String text) {
        // Regular expression to match Unicode escape sequences
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(text);

        // StringBuffer to hold the decoded text
        StringBuffer decodedText = new StringBuffer();

        // Process each match and replace with corresponding character
        while (matcher.find()) {
            String unicodeValue = matcher.group(1);
            char decodedChar = (char) Integer.parseInt(unicodeValue, 16);
            matcher.appendReplacement(decodedText, String.valueOf(decodedChar));
        }

        // Append any remaining part of the string that doesn't contain Unicode
        matcher.appendTail(decodedText);

        return decodedText.toString();
    }
}
