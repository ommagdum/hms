package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.CustomerHistoryReportDto;
import com.vinayakit.hms.dto.DailyRevenueReportDto;

import com.vinayakit.hms.dto.MonthlyOccupancyReportDto;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
public class ReportExportService {

    public byte[] generateDailyRevenuePdf(List<DailyRevenueReportDto> data, LocalDate start, LocalDate end) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Daily Revenue Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph dateRange = new Paragraph("From "+start+ " to "+end, dateFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            document.add(dateRange);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2,2,2});

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            PdfPCell cell = new PdfPCell(new Phrase("Date", headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            cell = new PdfPCell(new Phrase("Revenue", headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            cell = new PdfPCell(new Phrase("Bookings", headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);

            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (DailyRevenueReportDto dto : data) {
                table.addCell(new Phrase(dto.getDate().format(formatter), dataFont));
                table.addCell(new Phrase("$" + dto.getTotalRevenue().toString(), dataFont));
                table.addCell(new Phrase(String.valueOf(dto.getTotalBookings()), dataFont));
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return out.toByteArray();
    }

    public byte[] generateDailyRevenueExcel(List<DailyRevenueReportDto> data, LocalDate start, LocalDate end) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Daily Revenue");

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"Date", "Revenue", "Bookings"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Data rows
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (DailyRevenueReportDto dto : data) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getDate().format(formatter));
                row.createCell(1).setCellValue(dto.getTotalRevenue().doubleValue());
                row.createCell(2).setCellValue(dto.getTotalBookings());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generating Excel", e);
        }
    }

    public byte[] generateMonthlyOccupancyPdf(MonthlyOccupancyReportDto data) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Monthly Occupancy Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Month
            Font monthFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph monthPara = new Paragraph("Month: " + data.getMonth(), monthFont);
            monthPara.setAlignment(Element.ALIGN_CENTER);
            document.add(monthPara);
            document.add(new Paragraph(" "));

            // Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 2, 2});

            // Headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            PdfPCell cell = new PdfPCell(new Phrase("Metric", headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            cell = new PdfPCell(new Phrase("Occupied Nights", headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            cell = new PdfPCell(new Phrase("Available Nights", headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            cell = new PdfPCell(new Phrase("Occupancy %", headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);

            // Data row
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            table.addCell(new Phrase("Total", dataFont));
            table.addCell(new Phrase(String.valueOf(data.getOccupiedRoomNights()), dataFont));
            table.addCell(new Phrase(String.valueOf(data.getTotalAvailableRoomNights()), dataFont));
            table.addCell(new Phrase(String.format("%.2f%%", data.getOccupancyPercentage()), dataFont));

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        }
        return out.toByteArray();
    }

    public byte[] generateMonthlyOccupancyExcel(MonthlyOccupancyReportDto data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Monthly Occupancy");

            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"Month", "Occupied Nights", "Available Nights", "Occupancy %"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Data row
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(data.getMonth());
            row.createCell(1).setCellValue(data.getOccupiedRoomNights());
            row.createCell(2).setCellValue(data.getTotalAvailableRoomNights());
            row.createCell(3).setCellValue(data.getOccupancyPercentage());

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generating Excel", e);
        }
    }

    public byte[] generateCustomerHistoryPdf(List<CustomerHistoryReportDto> data, Long customerId, String customerName) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Customer Booking History", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Customer info
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph customerInfo = new Paragraph("Customer: " + customerName + " (ID: " + customerId + ")", infoFont);
            customerInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(customerInfo);
            document.add(new Paragraph(" "));

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 2, 2, 2, 2, 2});

            // Headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            String[] headers = {"Booking ID", "Check-In", "Check-Out", "Room", "Total Amount", "Status"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data rows
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (CustomerHistoryReportDto dto : data) {
                table.addCell(new Phrase(String.valueOf(dto.getBookingId()), dataFont));
                table.addCell(new Phrase(dto.getCheckIn().format(formatter), dataFont));
                table.addCell(new Phrase(dto.getCheckOut().format(formatter), dataFont));
                table.addCell(new Phrase(dto.getRoomNumber(), dataFont));
                table.addCell(new Phrase("$" + dto.getTotalAmount().toString(), dataFont));
                table.addCell(new Phrase(dto.getStatus(), dataFont));
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        }
        return out.toByteArray();
    }

    public byte[] generateCustomerHistoryExcel(List<CustomerHistoryReportDto> data, Long customerId, String customerName) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Customer History");

            // Title row (optional)
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Customer: " + customerName + " (ID: " + customerId + ")");
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(2);
            String[] headers = {"Booking ID", "Check-In", "Check-Out", "Room", "Total Amount", "Status"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Data rows
            int rowNum = 3;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (CustomerHistoryReportDto dto : data) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getBookingId());
                row.createCell(1).setCellValue(dto.getCheckIn().format(formatter));
                row.createCell(2).setCellValue(dto.getCheckOut().format(formatter));
                row.createCell(3).setCellValue(dto.getRoomNumber());
                row.createCell(4).setCellValue(dto.getTotalAmount().doubleValue());
                row.createCell(5).setCellValue(dto.getStatus());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generating Excel", e);
        }
    }
}
