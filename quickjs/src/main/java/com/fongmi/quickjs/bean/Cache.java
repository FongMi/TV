package com.fongmi.quickjs.bean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Cache {

    public String pdfhHtml;
    public String pdfaHtml;
    public Document pdfhDoc;
    public Document pdfaDoc;

    public Document getPdfh(String html) {
        updatePdfh(html);
        return pdfhDoc;
    }

    public Document getPdfa(String html) {
        updatePdfa(html);
        return pdfaDoc;
    }

    private void updatePdfh(String html) {
        if (html.equals(pdfhHtml)) return;
        pdfhDoc = Jsoup.parse(pdfhHtml = html);
    }

    private void updatePdfa(String html) {
        if (html.equals(pdfaHtml)) return;
        pdfaDoc = Jsoup.parse(pdfaHtml = html);
    }
}
