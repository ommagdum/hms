package com.vinayakit.hms.service;


import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.vinayakit.hms.dto.InvoiceDto;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final BookingRepository bookingRepository;
    private final JavaMailSender mailSender;

    @Value("${invoice.tax-rate}")
    private double taxRate;

    @Value("${invoice.company.name}")
    private String companyName;

    @Value("${invoice.company.email}")
    private String companyEmail;

    @Value("${invoice.company.phone}")
    private String companyPhone;

    @Value("${invoice.company.address}")
    private String companyAddress;

    // Generate invoice data from booking
    public InvoiceDto generateInvoiceData(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        long nights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
        BigDecimal subtotal = booking.getTotalAmount(); // already price * nights
        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(taxRate / 100.0))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount);

        String invoiceNumber = "INV-" + booking.getBookingId() + "-" + LocalDate.now().getYear();

        InvoiceDto dto = new InvoiceDto();
        dto.setBookingId(booking.getBookingId());
        dto.setInvoiceNumber(invoiceNumber);
        dto.setIssueDate(LocalDate.now());

        dto.setCustomerName(booking.getCustomer().getName());
        dto.setCustomerEmail(booking.getCustomer().getEmail());
        dto.setCustomerPhone(booking.getCustomer().getPhone());

        dto.setRoomNumber(booking.getRoom().getRoomNumber());
        dto.setRoomType(booking.getRoom().getRoomType());
        dto.setCheckIn(booking.getCheckIn());
        dto.setCheckOut(booking.getCheckOut());
        dto.setNights((int) nights);

        dto.setSubtotal(subtotal);
        dto.setTaxRate(BigDecimal.valueOf(taxRate));
        dto.setTaxAmount(taxAmount);
        dto.setTotalAmount(total);

        dto.setCompanyName(companyName);
        dto.setCompanyEmail(companyEmail);
        dto.setCompanyPhone(companyPhone);
        dto.setCompanyAddress(companyAddress);

        return dto;
    }

    // Generate PDF from invoice data
    public ByteArrayInputStream generateInvoicePdf(Long bookingId) {
        InvoiceDto invoice = generateInvoiceData(bookingId);
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Company & Invoice details
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            document.add(new Paragraph(invoice.getCompanyName(), infoFont));
            document.add(new Paragraph(invoice.getCompanyAddress(), infoFont));
            document.add(new Paragraph("Email: " + invoice.getCompanyEmail(), infoFont));
            document.add(new Paragraph("Phone: " + invoice.getCompanyPhone(), infoFont));
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), infoFont));
            document.add(new Paragraph("Date: " + invoice.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), infoFont));
            document.add(new Paragraph("\n"));

            // Customer details
            document.add(new Paragraph("Bill To:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph(invoice.getCustomerName(), infoFont));
            document.add(new Paragraph(invoice.getCustomerEmail(), infoFont));
            document.add(new Paragraph(invoice.getCustomerPhone(), infoFont));
            document.add(new Paragraph("\n"));

            // Booking details table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 2, 2, 2, 2});

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            addTableHeader(table, "Room", headerFont);
            addTableHeader(table, "Check-In", headerFont);
            addTableHeader(table, "Check-Out", headerFont);
            addTableHeader(table, "Nights", headerFont);
            addTableHeader(table, "Price/Night", headerFont);

            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            table.addCell(new Phrase(invoice.getRoomNumber(), dataFont));
            table.addCell(new Phrase(invoice.getCheckIn().toString(), dataFont));
            table.addCell(new Phrase(invoice.getCheckOut().toString(), dataFont));
            table.addCell(new Phrase(String.valueOf(invoice.getNights()), dataFont));
            table.addCell(new Phrase("$" + invoice.getSubtotal().divide(BigDecimal.valueOf(invoice.getNights()), 2, RoundingMode.HALF_UP).toString(), dataFont));

            document.add(table);
            document.add(new Paragraph("\n"));

            // Totals table
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            totalTable.addCell(new Phrase("Subtotal:", dataFont));
            totalTable.addCell(new Phrase("$" + invoice.getSubtotal().toString(), dataFont));
            totalTable.addCell(new Phrase("Tax (" + invoice.getTaxRate() + "%):", dataFont));
            totalTable.addCell(new Phrase("$" + invoice.getTaxAmount().toString(), dataFont));
            totalTable.addCell(new Phrase("Total:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            totalTable.addCell(new Phrase("$" + invoice.getTotalAmount().toString(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));

            document.add(totalTable);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }

    @Async
    public void sendInvoiceEmail(Long bookingId, String recipientEmail) {
        try {
            // If recipientEmail is null, fetch from booking
            if (recipientEmail == null) {
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
                recipientEmail = booking.getCustomer().getEmail();
            }

            ByteArrayInputStream pdf = generateInvoicePdf(bookingId);
            InvoiceDto invoice = generateInvoiceData(bookingId);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            helper.setSubject("Your Invoice from " + companyName);
            helper.setText("Dear " + invoice.getCustomerName() + ",\n\nPlease find attached your invoice for booking #" + bookingId + ".\n\nThank you for choosing " + companyName + ".\n\nRegards,\n" + companyName);

            helper.addAttachment("invoice-" + bookingId + ".pdf", () -> pdf);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}