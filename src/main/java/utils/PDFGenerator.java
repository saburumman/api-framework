package utils;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.FileOutputStream;

public class PDFGenerator {

    public static String generateApiResponsePDF(String apiName, String jsonFormattedResponse) {
        try {
            String safeName = apiName.replaceAll("[^a-zA-Z0-9]", "_");
            String filePath = "responses/" + safeName + "_response.pdf";

            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            document.add(new Paragraph("API Response for: " + apiName));
            document.add(new Paragraph(jsonFormattedResponse));

            document.close();

            // Return public-facing link if hosted, or file path
            return "https://yourdomain.com/responses/" + safeName + "_response.pdf";
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
