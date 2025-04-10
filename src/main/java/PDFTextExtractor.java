package main.java;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFTextExtractor {
    public static String extractText(PDDocument document) throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        StringBuilder fullText = new StringBuilder();

        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            pdfStripper.setStartPage(i);
            pdfStripper.setEndPage(i);
            fullText.append(pdfStripper.getText(document));
        }

        return fullText.toString();
    }

    public static String extractFirstMatch(String text, String pattern, int group) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return matcher.find() ? matcher.group(group) : null;
    }
}