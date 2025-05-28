package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import model.APIInfo;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmailSender {

    private static final String FROM_EMAIL = "s.aburumman96@gmail.com";
    private static final String PASSWORD = "bstk tgcp lecf wzlb";
    private static final String TO_EMAIL = "saraajoacademy@gmail.com";

    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    public static void sendResultsEmail(List<Map<String, Object>> results, String subject, String allureReportUrl) {
        Session session = createSession();

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject(subject);

            List<File> attachments = new ArrayList<>();
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<html><body style='font-family: Arial, sans-serif;'>");
            emailBody.append("<h2>Execution Results:</h2>");

            if (allureReportUrl != null && !allureReportUrl.isEmpty()) {
                emailBody.append("<p><strong>Full Allure report: </strong>")
                         .append("<a href='").append(allureReportUrl).append("' target='_blank'>Click here to view the Allure report</a></p>");
            }

            results.sort((map1, map2) -> Integer.compare(
                    safeParseInt(map2.get("status_code")),
                    safeParseInt(map1.get("status_code"))
            ));

            appendStatusGroupToEmailBody(emailBody, results, 500, "#f28888", "Failed (5xx)", attachments);
            appendStatusGroupToEmailBody(emailBody, results, 400, "#f0cc97", "Failed (4xx)", attachments);
            appendStatusGroupToEmailBody(emailBody, results, 300, "#edede4", "Failed (3xx)", attachments);
            appendStatusGroupToEmailBody(emailBody, results, 200, "#93cc9a", "Success (2xx)", attachments);

            emailBody.append("</body></html>");

            Multipart multipart = new MimeMultipart();
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(emailBody.toString(), "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

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
        try {
            return obj == null ? -1 : Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void appendStatusGroupToEmailBody(StringBuilder emailBody, List<Map<String, Object>> results,
                                                     int statusCodeGroup, String color, String groupName, List<File> attachments) {
        List<Map<String, Object>> filtered = getResultsByStatusCode(results, statusCodeGroup);
        if (!filtered.isEmpty()) {
            emailBody.append("<h3 style='background-color: ").append(color).append("; padding: 5px;'>").append(groupName).append("</h3>");
            emailBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse; width: 100%;'>")
                     .append("<tr><th>API Name</th><th>Endpoint</th><th>Status Code</th><th>Full Response</th><th>Request cURL</th></tr>");
            for (Map<String, Object> result : filtered) {
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
            String decodedJson = decodeUnicode(jsonResponse);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(decodedJson);

            StringBuilder formatted = new StringBuilder();
            formatted.append("<div style='font-family: monospace; font-size: 13px; white-space: pre-wrap;'>");
            formatJsonKeyValuePairs(root, formatted, "");
            formatted.append("</div>");

            String preview = getFirstNLines(formatted.toString(), 10);
            File pdf = generatePdf(apiName, formatted.toString());
            attachments.add(pdf);

            return "<div style='margin-top: 6px;'>Scroll to view the full API response.</div>" +
                   "<div style='border: 1px solid #ccc; padding: 8px; max-height: 180px; overflow: auto;'>" + preview + "</div>" +
                   "<div><strong>📄 " + pdf.getName() + "</strong></div>";
        } catch (Exception e) {
            return "<div style='margin-top: 6px;'>Could not parse response</div><div>" + jsonResponse + "</div>";
        }
    }

    private static String decodeUnicode(String text) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer decodedText = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(decodedText, String.valueOf((char) Integer.parseInt(matcher.group(1), 16)));
        }
        matcher.appendTail(decodedText);
        return decodedText.toString();
    }

    private static void formatJsonKeyValuePairs(JsonNode node, StringBuilder builder, String indent) {
        if (node.isObject()) {
            node.fields().forEachRemaining(field -> {
                builder.append(indent).append(field.getKey()).append(": ");
                JsonNode value = field.getValue();
                if (value.isValueNode()) {
                    builder.append(value.asText()).append("\n");
                } else {
                    builder.append("\n");
                    formatJsonKeyValuePairs(value, builder, indent + "  ");
                }
            });
        } else if (node.isArray()) {
            int index = 0;
            for (JsonNode item : node) {
                builder.append(indent).append("[").append(index++).append("]:\n");
                formatJsonKeyValuePairs(item, builder, indent + "  ");
            }
        } else {
            builder.append(indent).append(node.asText()).append("\n");
        }
    }

    private static String getFirstNLines(String text, int maxLines) {
        String[] lines = text.split("\n");
        return Arrays.stream(lines).limit(maxLines).collect(Collectors.joining("\n"));
    }

    private static File generatePdf(String fileName, String content) throws IOException {
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        Path tempDir = Files.createTempDirectory("pdf_attachments");
        File pdfFile = new File(tempDir.toFile(), safeFileName + ".pdf");

        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {
            document.add(new Paragraph(content));
        }

        return pdfFile;
    }

    private static String generateCurlCommand(Map<String, Object> result) {
        StringBuilder curl = new StringBuilder("curl -X ");
        curl.append(result.getOrDefault("method", "GET")).append(" '")
            .append(result.getOrDefault("base_url", "")).append(result.getOrDefault("endpoint", "")).append("' ");
        if (result.containsKey("auth_token")) {
            curl.append("-H 'Authorization: Bearer ").append(result.get("auth_token")).append("' ");
        }
        if (result.containsKey("payload")) {
            curl.append("-d '").append(result.get("payload")).append("' ");
        }
        return curl.toString().trim();
    }

    public static void sendFailureEmail(List<Map<String, Object>> results, String allureReportUrl) {
        sendResultsEmail(results, "Alert: API Failure Report", allureReportUrl);
    }

    public static void sendStillFailed(List<Map<String, Object>> results, String allureReportUrl) {
        sendResultsEmail(results, "Alert: APIs Still Failing", allureReportUrl);
    }

    public static void sendBackToNormalSuccess(List<APIInfo> recoveredAPIs, String subject) {
        try {
            Message message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject(subject);

            StringBuilder htmlBody = new StringBuilder();
            htmlBody.append("<html><body style='font-family: Arial, sans-serif;'>");
            htmlBody.append("<h3>✅ Recovered APIs</h3><ul>");
            for (APIInfo api : recoveredAPIs) {
                String duration = formatDuration(api.getFirstFailureTime(), api.getRecoveryTime());
                htmlBody.append(String.format(
                        "<li><b>%s</b> (%s) - Recovery Duration: <i>%s</i></li>",
                        api.getName(), api.getEndpoint(), duration));
            }
            htmlBody.append("</ul></body></html>");

            message.setContent(htmlBody.toString(), "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Recovery email sent successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatDuration(Instant start, Instant end) {
        if (start == null || end == null) return "N/A";
        Duration duration = Duration.between(start, end);
        return String.format("%d min, %d sec", duration.toMinutes(), duration.getSeconds() % 60);
    }
}
