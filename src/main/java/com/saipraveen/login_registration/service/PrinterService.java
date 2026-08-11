package com.saipraveen.login_registration.service;

import java.awt.print.PrinterJob;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Sides;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.PdfFile;

@Service
public class PrinterService {

    public void printPdf(PdfFile pdf) {
        if (pdf == null || pdf.getPdfData() == null) {
            return;
        }

        try (PDDocument document = Loader.loadPDF(pdf.getPdfData())) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setJobName((pdf.getOrderId() != null ? pdf.getOrderId() : "Order") + " - " + pdf.getFileName());

            Integer copies = pdf.getCopies();
            printerJob.setCopies(copies == null || copies < 1 ? 1 : copies);
            printerJob.setPageable(new PDFPageable(document));

            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            if (Boolean.TRUE.equals(pdf.getDoubleSided())) {
                attributes.add(Sides.DUPLEX);
            } else {
                attributes.add(Sides.ONE_SIDED);
            }

            printerJob.print(attributes);
            System.out.println("PRINT COMMAND SENT (Duplex=" + pdf.getDoubleSided() + ") FOR ORDER " + pdf.getOrderId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}