package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmailSender {

    public static void sendResultsEmail(List<Map<String, Object>> results, String subject, String allureReportUrl) {
        final String username = "s.aburumman96@gmail.com";
        final String password = "bstk tgcp lecf wzlb";
        String to = "saraajoacademy@gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            List<File> attachments = new ArrayList<>();

            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<html><body style='font-family: Arial, sans-serif;'>");
            emailBody.append("<h2>Execution Results:</h2>");

            // Add Allure report clickable link instead of ZIP attachment
            if (allureReportUrl != null && !allureReportUrl.isEmpty()) {
                emailBody.append("<p><strong>Full Allure report: </strong>")
                         .append("<a href='").append(allureReportUrl).append("' target='_blank'>Click here to view the Allure report</a></p>");
            }

            // Sort results by status_code descending
            results.sort((map1, map2) -> {
                int code1 = safeParseInt(map1.get("status_code"));
                int code2 = safeParseInt(map2.get("status_code"));
                return Integer.compare(code2, code1);
            });

            appendStatusGroupToEmailBody(emailBody, results, 500, "#f28888", "Failed (5xx)", attachments);
            appendStatusGroupToEmailBody(emailBody, results, 400, "#f0cc97", "Failed (4xx)", attachments);
            appendStatusGroupToEmailBody(emailBody, results, 300, "#edede4", "Failed (3xx)", attachments);
            appendStatusGroupToEmailBody(emailBody, results, 200, "#93cc9a", "Success (2xx)", attachments);

            emailBody.append("</body></html>");

            Multipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(emailBody.toString(), "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // Attach generated PDFs
            for (File attachment : attachments) {
                if (attachment != null && attachment.exists()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(attachment);
                    multipart.addBodyPart(attachmentPart);
                }
            }

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("Email sent successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int safeParseInt(Object obj) {
        if (obj == null) return -1;
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void appendStatusGroupToEmailBody(StringBuilder emailBody, List<Map<String, Object>> results, int statusCodeGroup, String color, String groupName, List<File> attachments) {
        List<Map<String, Object>> filteredResults = getResultsByStatusCode(results, statusCodeGroup);

        if (!filteredResults.isEmpty()) {
            emailBody.append("<h3 style='background-color: ").append(color).append("; padding: 5px;'>").append(groupName).append("</h3>");
            emailBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse; width: 100%; margin-left: auto; margin-right: auto;'>");
            emailBody.append("<tr><th style='background-color: #f2f2f2;'>API Name</th>")
                    .append("<th style='background-color: #f2f2f2;'>Endpoint</th>")
                    .append("<th style='background-color: #f2f2f2;'>Status Code</th>")
                    .append("<th style='background-color: #f2f2f2;'>Full Response</th>")
                    .append("<th style='background-color: #f2f2f2;'>Request cURL</th></tr>");

            for (Map<String, Object> result : filteredResults) {
                appendApiDetailsToEmailBody(emailBody, result, attachments);
            }

            emailBody.append("</table>");
        }
    }

    private static List<Map<String, Object>> getResultsByStatusCode(List<Map<String, Object>> results, int group) {
        return results.stream().filter(result -> {
            int code = safeParseInt(result.get("status_code"));
            switch (group) {
                case 500:
                    return code >= 500;
                case 400:
                    return code >= 400 && code < 500;
                case 300:
                    return code >= 300 && code < 400;
                case 200:
                    return code >= 200 && code < 300;
                default:
                    return false;
            }
        }).collect(Collectors.toList());
    }

    private static void appendApiDetailsToEmailBody(StringBuilder emailBody, Map<String, Object> result, List<File> attachments) {
        String apiName = (String) result.getOrDefault("api", "N/A");
        String endpoint = (String) result.getOrDefault("endpoint", "N/A");
        String statusCode = String.valueOf(result.getOrDefault("status_code", "N/A"));
        String responseRaw = (String) result.getOrDefault("response", "N/A");

        String formattedResponse = decodeAndFormatFullResponse(responseRaw, apiName, attachments);
        String curl = generateCurlCommand(result);

        emailBody.append("<tr>")
                .append("<td>").append(apiName).append("</td>")
                .append("<td>").append(endpoint).append("</td>")
                .append("<td>").append(statusCode).append("</td>")
                .append("<td>").append(formattedResponse).append("</td>")
                .append("<td style='white-space: pre-wrap; font-family: monospace;'>").append(curl).append("</td>")
                .append("</tr>");
    }

    private static String decodeAndFormatFullResponse(String jsonResponse, String apiName, List<File> attachments) {
        try {
            // Decode Unicode escape sequences if needed
            String decodedJson = decodeUnicode(jsonResponse);

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(decodedJson);

            // Format JSON as key-value pairs (you need to have this method implemented)
            StringBuilder formatted = new StringBuilder();
            formatted.append("<div style='font-family: monospace; font-size: 13px; white-space: pre-wrap; direction: ltr; text-align: left;'>");
            formatJsonKeyValuePairs(root, formatted, "");
            formatted.append("</div>");

            // Get preview - first N lines of the formatted HTML (you can adjust line count)
            String preview = getFirstNLines(formatted.toString(), 10);

            // Generate PDF from full formatted HTML
            File pdf = generatePdf(apiName, formatted.toString());
            attachments.add(pdf);
            String fileName = pdf.getName();

            // Build email-safe HTML with preview and attachment info
            StringBuilder emailHtml = new StringBuilder();
            
            emailHtml.append("<div style='margin-top: 6px; font-size: 12px; color: #388e3c;'>");
            emailHtml.append("Scroll to view the full API response.");
            emailHtml.append("</div>");

            // Preview block with monospace style
            emailHtml.append("<div style='font-family: monospace; font-size: 13px; white-space: pre-wrap; direction: ltr; text-align: left; border: 1px solid #ccc; padding: 8px; max-height: 180px; overflow: auto;'>");
            emailHtml.append(preview);
            emailHtml.append("</div>");

            // Attachment info with icon and file name
            emailHtml.append("<div style='margin-top: 12px; font-size: 14px;'>");
            emailHtml.append("<span style='font-size:16px;'>📄</span> <strong>").append(fileName).append("</strong>");
            emailHtml.append("</div>");

           

            return emailHtml.toString();

        } catch (Exception e) {
        	   // Build email-safe HTML with preview and attachment info
            StringBuilder emailHtmlFailed = new StringBuilder();

            emailHtmlFailed.append("<div style='margin-top: 6px; font-size: 12px; color: #d32f2f;'>");
            emailHtmlFailed.append("Scroll to view the full API response.");
            emailHtmlFailed.append("</div>");

            // Preview block with monospace style
            emailHtmlFailed.append("<div style='font-family: monospace; font-size: 13px; white-space: pre-wrap; direction: ltr; text-align: left; border: 1px solid #ccc; padding: 8px; max-height: 180px; overflow: auto;'>");
            emailHtmlFailed.append(jsonResponse);
            emailHtmlFailed.append("</div>");

        
           

            return emailHtmlFailed.toString();

            
        }
    }

    private static String getFirstNLines(String text, int maxLines) {
        String[] lines = text.split("\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(maxLines, lines.length); i++) {
            builder.append(lines[i]).append("\n");
        }
        return builder.toString().trim();
    }

    private static String decodeUnicode(String text) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer decodedText = new StringBuffer();

        while (matcher.find()) {
            String unicodeValue = matcher.group(1);
            char decodedChar = (char) Integer.parseInt(unicodeValue, 16);
            matcher.appendReplacement(decodedText, String.valueOf(decodedChar));
        }
        matcher.appendTail(decodedText);
        return decodedText.toString();
    }

    private static String generateCurlCommand(Map<String, Object> result) {
        String baseUrl = (String) result.getOrDefault("base_url", "");
        String endpoint = (String) result.getOrDefault("endpoint", "");
        String authToken = (String) result.getOrDefault("auth_token", "");
        String payload = (String) result.getOrDefault("payload", "");
        String method = (String) result.getOrDefault("method", "GET");

        StringBuilder curl = new StringBuilder();
        curl.append("curl '").append(baseUrl).append(endpoint).append("' \\\n")
            .append("-H 'Authorization: Bearer ").append(authToken).append("' \\\n")
            .append("-H 'Content-Type: application/json' \\\n");

        if (payload != null && !payload.isEmpty()) {
            curl.append("-d '").append(payload).append("' \\\n");
        }

        curl.append("-X ").append(method);

        return curl.toString();
    }

    private static void formatJsonKeyValuePairs(JsonNode node, StringBuilder formatted, String indent) {
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode value = node.get(key);
                formatted.append(indent).append("<strong>").append(key).append("</strong>: ");
                if (value.isValueNode()) {
                    formatted.append(value.asText()).append("<br>");
                } else {
                    formatted.append("<br>");
                    formatJsonKeyValuePairs(value, formatted, indent + "&nbsp;&nbsp;&nbsp;&nbsp;");
                }
            }
        } else if (node.isArray()) {
            int i = 0;
            for (JsonNode element : node) {
                formatted.append(indent).append("[").append(i).append("]: <br>");
                formatJsonKeyValuePairs(element, formatted, indent + "&nbsp;&nbsp;&nbsp;&nbsp;");
                i++;
            }
        } else {
            formatted.append(indent).append(node.asText()).append("<br>");
        }
    }

    private static File generatePdf(String apiName, String content) {
        try {
            String safeName = apiName.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
            Path pdfPath = Paths.get("target/reports/" + safeName + "_fullResponse.pdf");
            Files.createDirectories(pdfPath.getParent());

            PdfWriter writer = new PdfWriter(pdfPath.toString());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            // Add content as plain text; you can customize styling here if needed
            document.add(new Paragraph(content.replaceAll("<br>", "\n").replaceAll("&nbsp;", " ")));
            document.close();

            return pdfPath.toFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
