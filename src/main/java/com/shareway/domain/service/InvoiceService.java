package com.shareway.domain.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.shareway.application.dto.response.RideResponse;
import com.shareway.domain.model.PricingConfig;
import com.shareway.domain.model.Trip;
import com.shareway.domain.repository.PricingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final PricingConfigRepository pricingConfigRepository;

    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Color BORDER = new Color(229, 231, 235);
    private static final Color WHITE = Color.WHITE;
    private static final Color HEADER_BG = new Color(37, 99, 235);
    private static final Color ROW_ALT = new Color(249, 250, 251);
    private static final Color TOTAL_BG = new Color(239, 246, 255);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY);
    private static final Font FONT_CONTACT = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
    private static final Font FONT_TOTAL_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
    private static final Font FONT_TOTAL_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_MUTED);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, WHITE);
    private static final Font FONT_TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
    private static final Font FONT_TABLE_CELL_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);

    public byte[] generateInvoice(RideResponse ride) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 30, 30, 25, 25);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            addHeader(document, ride);
            addRideInfoSection(document, ride);
            addRouteSection(document, ride);
            addPricingTable(document, ride);
            addPaymentSection(document, ride);
            addFooter(document);
            document.close();
        } catch (Exception e) {
            log.error("Error generating invoice for ride {}: {}", ride.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la generation de la facture", e);
        }
        return baos.toByteArray();
    }

    private void addHeader(Document document, RideResponse ride) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{55, 45});
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("SHAREWAY", FONT_TITLE));
        Paragraph tagline = new Paragraph("Plateforme de Covoiturage", FONT_CONTACT);
        tagline.setSpacingBefore(1);
        leftCell.addElement(tagline);
        Paragraph contact1 = new Paragraph("Email: sharewaybdi@gmail.com", FONT_CONTACT);
        contact1.setSpacingBefore(2);
        leftCell.addElement(contact1);
        Paragraph contact2 = new Paragraph("WhatsApp: +33 7 80 73 93 84", FONT_CONTACT);
        contact2.setSpacingBefore(1);
        leftCell.addElement(contact2);
        headerTable.addCell(leftCell);

        String invoiceNo = "N\u00ba SW-" + ride.getId().substring(0, Math.min(8, ride.getId().length())).toUpperCase();
        String dateStr = ride.getCompletedAt() != null
                ? ride.getCompletedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : ride.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        Paragraph invoiceTitle = new Paragraph("FACTURE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK));
        invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(invoiceTitle);
        Paragraph invoiceNoP = new Paragraph(invoiceNo, FONT_LABEL);
        invoiceNoP.setAlignment(Element.ALIGN_RIGHT);
        invoiceNoP.setSpacingBefore(3);
        rightCell.addElement(invoiceNoP);
        Paragraph dateP = new Paragraph("Emis le: " + dateStr, FONT_LABEL);
        dateP.setAlignment(Element.ALIGN_RIGHT);
        dateP.setSpacingBefore(1);
        rightCell.addElement(dateP);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    private void addRideInfoSection(Document document, RideResponse ride) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Informations de la course", FONT_SECTION);
        sectionTitle.setSpacingBefore(6);
        sectionTitle.setSpacingAfter(4);
        document.add(sectionTitle);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{30, 70});

        addInfoRow(infoTable, "Chauffeur", ride.getDriverFirstName() + " " + ride.getDriverLastName());
        addInfoRow(infoTable, "Vehicule", buildVehicleString(ride));
        addInfoRow(infoTable, "Plaque", ride.getDriverVehiclePlate() != null ? ride.getDriverVehiclePlate() : "N/A");
        addInfoRow(infoTable, "Passager", ride.getPassengerFirstName() + " " + ride.getPassengerLastName());
        addInfoRow(infoTable, "Passagers", String.valueOf(ride.getPassengerCount()));
        addInfoRow(infoTable, "Statut", getDisplayStatus(ride.getStatus()));

        document.add(infoTable);
        document.add(Chunk.NEWLINE);
    }

    private void addRouteSection(Document document, RideResponse ride) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Itineraire", FONT_SECTION);
        sectionTitle.setSpacingBefore(6);
        sectionTitle.setSpacingAfter(4);
        document.add(sectionTitle);

        PdfPTable routeTable = new PdfPTable(2);
        routeTable.setWidthPercentage(100);
        routeTable.setWidths(new float[]{30, 70});

        addInfoRow(routeTable, "Depart", ride.getPickupAddress() != null ? ride.getPickupAddress() : "N/A");
        addInfoRow(routeTable, "Arrivee", ride.getDestinationAddress() != null ? ride.getDestinationAddress() : "N/A");

        String distStr = ride.getEstimatedDistanceKm() != null
                ? ride.getEstimatedDistanceKm().setScale(1, RoundingMode.HALF_UP) + " km" : "N/A";
        addInfoRow(routeTable, "Distance", distStr);

        if (ride.getStartedAt() != null) {
            addInfoRow(routeTable, "Debut", ride.getStartedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        if (ride.getCompletedAt() != null) {
            addInfoRow(routeTable, "Fin", ride.getCompletedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        document.add(routeTable);
        document.add(Chunk.NEWLINE);
    }

    private void addPricingTable(Document document, RideResponse ride) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Detail tarifaire", FONT_SECTION);
        sectionTitle.setSpacingBefore(6);
        sectionTitle.setSpacingAfter(4);
        document.add(sectionTitle);

        String currency = ride.getCurrency() != null ? ride.getCurrency() : "FBU";
        Trip.Currency currencyEnum = parseCurrency(currency);
        PricingConfig config = pricingConfigRepository.findByCurrencyAndActiveTrue(currencyEnum)
                .orElse(getDefaultConfig(currencyEnum));

        BigDecimal distanceKm = ride.getEstimatedDistanceKm() != null ? ride.getEstimatedDistanceKm() : BigDecimal.ZERO;
        BigDecimal basePrice = config.getBasePrice();
        BigDecimal pricePerKm = config.getPricePerKm();
        BigDecimal minimumPrice = config.getMinimumPrice();
        BigDecimal platformFeePercent = config.getPlatformFeePercent();

        BigDecimal distanceCost = distanceKm.multiply(pricePerKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = basePrice.add(distanceCost);

        if (subtotal.compareTo(minimumPrice) < 0) {
            subtotal = minimumPrice;
        }

        BigDecimal platformFeeAmount = subtotal.multiply(platformFeePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal tvaRate = new BigDecimal("18");
        BigDecimal tvaAmount = subtotal.multiply(tvaRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(platformFeeAmount).add(tvaAmount);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55, 20, 25});
        table.setSpacingBefore(5);

        addTableHeaderRow(table, "Designation", "Taux", "Montant");
        addTableRow(table, "Prix de base", "", formatPrice(basePrice, currency), false);
        addTableRow(table, "Distance (" + distanceKm.setScale(1, RoundingMode.HALF_UP) + " km x " + formatPrice(pricePerKm, currency) + "/km)", "", formatPrice(distanceCost, currency), true);

        if (ride.getSurgeMultiplier() != null && ride.getSurgeMultiplier().compareTo(BigDecimal.ONE) > 0) {
            String surgePct = "+" + ride.getSurgeMultiplier().subtract(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "%";
            addTableRow(table, "Surge (x" + ride.getSurgeMultiplier() + ")", surgePct, "", true);
        }

        if (subtotal.compareTo(minimumPrice) == 0 && subtotal.compareTo(basePrice.add(distanceCost)) > 0) {
            addTableRow(table, "Minimum applique", "", formatPrice(minimumPrice, currency), false);
        }

        addTableRow(table, "Sous-total HT", "", formatPrice(subtotal, currency), true);
        addTableRow(table, "Frais de plateforme (" + platformFeePercent + "%)", platformFeePercent + "%", formatPrice(platformFeeAmount, currency), false);
        addTableRow(table, "TVA (18%)", "18%", formatPrice(tvaAmount, currency), true);

        document.add(table);

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(100);
        totalTable.setWidths(new float[]{70, 30});
        totalTable.setSpacingBefore(8);

        PdfPCell totalLabelCell = new PdfPCell(new Paragraph("TOTAL A PAYER", FONT_TOTAL_LABEL));
        totalLabelCell.setBackgroundColor(TOTAL_BG);
        totalLabelCell.setBorderColor(BORDER);
        totalLabelCell.setBorderWidth(1);
        totalLabelCell.setPadding(6);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Paragraph(formatPrice(total, currency), FONT_TOTAL_VALUE));
        totalValueCell.setBackgroundColor(TOTAL_BG);
        totalValueCell.setBorderColor(BORDER);
        totalValueCell.setBorderWidth(1);
        totalValueCell.setPadding(6);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(totalValueCell);

        document.add(totalTable);
    }

    private void addPaymentSection(Document document, RideResponse ride) throws DocumentException {
        String currency = ride.getCurrency() != null ? ride.getCurrency() : "FBU";

        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        paymentTable.setWidths(new float[]{50, 50});

        String paymentStatus = ride.getPaymentStatus() != null ? ride.getPaymentStatus() : "PENDING";
        addInfoRow(paymentTable, "Paiement", getDisplayPaymentStatus(paymentStatus));

        if (ride.getDriverEarnings() != null) {
            addInfoRow(paymentTable, "Revenu chauffeur", formatPrice(ride.getDriverEarnings(), currency));
        }
        if (ride.getPlatformFeeAmount() != null) {
            addInfoRow(paymentTable, "Frais plateforme", formatPrice(ride.getPlatformFeeAmount(), currency));
        }

        document.add(paymentTable);
        document.add(Chunk.NEWLINE);
    }

    private void addFooter(Document document) throws DocumentException {
        addSeparator(document);
        Paragraph footer = new Paragraph("Merci pour votre confiance. ShareWay - Plateforme de Covoiturage", FONT_SMALL);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);

        Paragraph legal = new Paragraph("Facture generee automatiquement. Pour toute question, contactez sharewaybdi@gmail.com", FONT_SMALL);
        legal.setAlignment(Element.ALIGN_CENTER);
        legal.setSpacingBefore(4);
        document.add(legal);
    }

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(1);
        cell.setBorderWidthBottom(1f);
        cell.setBorderColorBottom(BORDER);
        sep.addCell(cell);
        document.add(sep);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, FONT_VALUE));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(2);
        table.addCell(valueCell);
    }

    private void addTableHeaderRow(PdfPTable table, String col1, String col2, String col3) {
        PdfPCell h1 = new PdfPCell(new Paragraph(col1, FONT_TABLE_HEADER));
        h1.setBackgroundColor(HEADER_BG);
        h1.setPadding(8);
        h1.setBorderColor(BORDER);
        table.addCell(h1);

        PdfPCell h2 = new PdfPCell(new Paragraph(col2, FONT_TABLE_HEADER));
        h2.setBackgroundColor(HEADER_BG);
        h2.setPadding(8);
        h2.setHorizontalAlignment(Element.ALIGN_CENTER);
        h2.setBorderColor(BORDER);
        table.addCell(h2);

        PdfPCell h3 = new PdfPCell(new Paragraph(col3, FONT_TABLE_HEADER));
        h3.setBackgroundColor(HEADER_BG);
        h3.setPadding(8);
        h3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        h3.setBorderColor(BORDER);
        table.addCell(h3);
    }

    private void addTableRow(PdfPTable table, String col1, String col2, String col3, boolean alternate) {
        Color bg = alternate ? ROW_ALT : WHITE;
        PdfPCell c1 = new PdfPCell(new Paragraph(col1, FONT_TABLE_CELL));
        c1.setBackgroundColor(bg);
        c1.setPadding(4);
        c1.setBorderColor(BORDER);
        c1.setBorderWidth(0.5f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Paragraph(col2, FONT_TABLE_CELL_MUTED));
        c2.setBackgroundColor(bg);
        c2.setPadding(4);
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        c2.setBorderColor(BORDER);
        c2.setBorderWidth(0.5f);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Paragraph(col3, FONT_TABLE_CELL));
        c3.setBackgroundColor(bg);
        c3.setPadding(4);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setBorderColor(BORDER);
        c3.setBorderWidth(0.5f);
        table.addCell(c3);
    }

    private String buildVehicleString(RideResponse ride) {
        StringBuilder sb = new StringBuilder();
        if (ride.getDriverVehicleColor() != null) sb.append(ride.getDriverVehicleColor()).append(" ");
        if (ride.getDriverVehicleBrand() != null) sb.append(ride.getDriverVehicleBrand()).append(" ");
        if (ride.getDriverVehicleModel() != null) sb.append(ride.getDriverVehicleModel());
        return sb.toString().trim().isEmpty() ? "N/A" : sb.toString().trim();
    }

    private String getDisplayStatus(String status) {
        if (status == null) return "Inconnu";
        return switch (status) {
            case "COMPLETED" -> "Terminee";
            case "IN_PROGRESS" -> "En cours";
            case "CANCELLED" -> "Annulee";
            case "EXPIRED" -> "Expiree";
            case "ACCEPTED" -> "Acceptee";
            case "DRIVER_FOUND" -> "Chauffeur trouve";
            case "DRIVER_EN_ROUTE" -> "Chauffeur en route";
            case "ARRIVED" -> "Chauffeur arrive";
            case "SEARCHING" -> "Recherche en cours";
            default -> status;
        };
    }

    private String getDisplayPaymentStatus(String status) {
        if (status == null) return "En attente";
        return switch (status) {
            case "PAID" -> "Paye";
            case "CAPTURED" -> "Paye";
            case "PENDING" -> "En attente";
            case "AUTHORIZED" -> "Autorise";
            case "FAILED" -> "Echoue";
            case "REFUNDED" -> "Rembourse";
            case "REFUSED" -> "Paiement refuse";
            default -> status;
        };
    }

    private String formatPrice(BigDecimal price, String currency) {
        return price.setScale(2, RoundingMode.HALF_UP) + " " + currency;
    }

    private Trip.Currency parseCurrency(String currency) {
        if (currency == null || currency.isBlank()) return Trip.Currency.FBU;
        try {
            return Trip.Currency.valueOf(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Trip.Currency.FBU;
        }
    }

    private PricingConfig getDefaultConfig(Trip.Currency currency) {
        return switch (currency) {
            case FBU -> PricingConfig.builder()
                    .currency(Trip.Currency.FBU)
                    .basePrice(new BigDecimal("1000"))
                    .pricePerKm(new BigDecimal("350"))
                    .pricePerMin(new BigDecimal("50"))
                    .minimumPrice(new BigDecimal("1500"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
            case USD -> PricingConfig.builder()
                    .currency(Trip.Currency.USD)
                    .basePrice(new BigDecimal("1.00"))
                    .pricePerKm(new BigDecimal("0.35"))
                    .pricePerMin(new BigDecimal("0.05"))
                    .minimumPrice(new BigDecimal("1.50"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
            case EUR -> PricingConfig.builder()
                    .currency(Trip.Currency.EUR)
                    .basePrice(new BigDecimal("0.90"))
                    .pricePerKm(new BigDecimal("0.30"))
                    .pricePerMin(new BigDecimal("0.04"))
                    .minimumPrice(new BigDecimal("1.35"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
            case FRW -> PricingConfig.builder()
                    .currency(Trip.Currency.FRW)
                    .basePrice(new BigDecimal("500"))
                    .pricePerKm(new BigDecimal("175"))
                    .pricePerMin(new BigDecimal("25"))
                    .minimumPrice(new BigDecimal("750"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
        };
    }

    // ════════════════════════════════════════════════════════════════
    // TICKET DE CAISSE (80mm thermal)
    // ════════════════════════════════════════════════════════════════

    private static final float MM_TO_PT = 2.835f;
    private static final float RECEIPT_WIDTH_MM = 80f;
    private static final float RECEIPT_MARGIN_MM = 5f;

    private static final Font RF_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
    private static final Font RF_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_DARK);
    private static final Font RF_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_DARK);
    private static final Font RF_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_DARK);
    private static final Font RF_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_DARK);
    private static final Font RF_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
    private static final Font RF_TINY = FontFactory.getFont(FontFactory.HELVETICA, 6, TEXT_MUTED);

    public byte[] generateReceipt(RideResponse ride) {
        float pageWidth = RECEIPT_WIDTH_MM * MM_TO_PT;
        float margin = RECEIPT_MARGIN_MM * MM_TO_PT;
        Rectangle receiptSize = new Rectangle(pageWidth, 842);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(receiptSize, margin, margin, margin, margin);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addReceiptCentered(document, "SHAREWAY", RF_TITLE);
            addReceiptCentered(document, "Plateforme de Covoiturage", RF_SMALL);
            addReceiptCentered(document, "sharewaybdi@gmail.com", RF_TINY);
            addReceiptCentered(document, "WhatsApp: +33 7 80 73 93 84", RF_TINY);
            addReceiptLine(document);

            String invoiceNo = "SW-" + ride.getId().substring(0, Math.min(8, ride.getId().length())).toUpperCase();
            String dateStr = ride.getCompletedAt() != null
                    ? ride.getCompletedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : ride.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            addReceiptLeftRight(document, "Facture:", invoiceNo, RF_LABEL, RF_BOLD);
            addReceiptLeftRight(document, "Date:", dateStr, RF_LABEL, RF_BOLD);
            addReceiptLine(document);

            addReceiptCentered(document, "ITINERAIRE", RF_BOLD);
            addReceiptLeftRight(document, "De:", truncate(ride.getPickupAddress(), 30), RF_LABEL, RF_BOLD);
            addReceiptLeftRight(document, "A:", truncate(ride.getDestinationAddress(), 30), RF_LABEL, RF_BOLD);
            String dist = ride.getEstimatedDistanceKm() != null
                    ? ride.getEstimatedDistanceKm().setScale(1, RoundingMode.HALF_UP) + " km" : "N/A";
            addReceiptLeftRight(document, "Distance:", dist, RF_LABEL, RF_BOLD);
            addReceiptLine(document);

            addReceiptCentered(document, "CHAUFFEUR", RF_BOLD);
            addReceiptLeftRight(document, "Nom:", ride.getDriverFirstName() + " " + ride.getDriverLastName(), RF_LABEL, RF_BOLD);
            if (ride.getDriverVehiclePlate() != null) {
                addReceiptLeftRight(document, "Plaque:", ride.getDriverVehiclePlate(), RF_LABEL, RF_BOLD);
            }
            addReceiptLine(document);

            String currency = ride.getCurrency() != null ? ride.getCurrency() : "FBU";
            Trip.Currency currencyEnum = parseCurrency(currency);
            PricingConfig config = pricingConfigRepository.findByCurrencyAndActiveTrue(currencyEnum)
                    .orElse(getDefaultConfig(currencyEnum));

            BigDecimal distanceKm = ride.getEstimatedDistanceKm() != null ? ride.getEstimatedDistanceKm() : BigDecimal.ZERO;
            BigDecimal basePrice = config.getBasePrice();
            BigDecimal pricePerKm = config.getPricePerKm();
            BigDecimal minimumPrice = config.getMinimumPrice();
            BigDecimal platformFeePercent = config.getPlatformFeePercent();

            BigDecimal distanceCost = distanceKm.multiply(pricePerKm).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = basePrice.add(distanceCost);
            if (subtotal.compareTo(minimumPrice) < 0) {
                subtotal = minimumPrice;
            }

            BigDecimal platformFeeAmount = subtotal.multiply(platformFeePercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal tvaRate = new BigDecimal("18");
            BigDecimal tvaAmount = subtotal.multiply(tvaRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(platformFeeAmount).add(tvaAmount);

            addReceiptCentered(document, "TARIFICATION", RF_BOLD);
            addReceiptLeftRight(document, "Prix de base:", formatPrice(basePrice, currency), RF_LABEL, RF_BOLD);
            addReceiptLeftRight(document, "Distance:", formatPrice(distanceCost, currency), RF_LABEL, RF_BOLD);

            if (ride.getSurgeMultiplier() != null && ride.getSurgeMultiplier().compareTo(BigDecimal.ONE) > 0) {
                addReceiptLeftRight(document, "Surge:", "x" + ride.getSurgeMultiplier(), RF_LABEL, RF_BOLD);
            }

            if (subtotal.compareTo(minimumPrice) == 0 && subtotal.compareTo(basePrice.add(distanceCost)) > 0) {
                addReceiptLeftRight(document, "Minimum:", formatPrice(minimumPrice, currency), RF_LABEL, RF_BOLD);
            }

            addReceiptLine(document);
            addReceiptLeftRight(document, "Sous-total HT:", formatPrice(subtotal, currency), RF_LABEL, RF_BOLD);
            addReceiptLeftRight(document, "Frais (" + platformFeePercent + "%):", formatPrice(platformFeeAmount, currency), RF_LABEL, RF_BOLD);
            addReceiptLeftRight(document, "TVA (18%):", formatPrice(tvaAmount, currency), RF_LABEL, RF_BOLD);
            addReceiptLine(document);

            addReceiptLeftRight(document, "TOTAL:", formatPrice(total, currency), RF_TOTAL, RF_TOTAL);

            addReceiptLine(document);
            String paymentStatus = ride.getPaymentStatus() != null ? ride.getPaymentStatus() : "PENDING";
            addReceiptLeftRight(document, "Paiement:", getDisplayPaymentStatus(paymentStatus), RF_LABEL, RF_BOLD);

            if (ride.getDriverEarnings() != null) {
                addReceiptLeftRight(document, "Revenu:", formatPrice(ride.getDriverEarnings(), currency), RF_LABEL, RF_BOLD);
            }

            addReceiptLine(document);
            addReceiptCentered(document, "Merci pour votre confiance!", RF_SMALL);
            addReceiptCentered(document, "ShareWay", RF_TINY);

            document.close();
        } catch (Exception e) {
            log.error("Error generating receipt for ride {}: {}", ride.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la generation du ticket", e);
        }
        return baos.toByteArray();
    }

    private void addReceiptCentered(Document document, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(1);
        p.setSpacingAfter(1);
        document.add(p);
    }

    private void addReceiptLeftRight(Document document, String left, String right, Font leftFont, Font rightFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55, 45});
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        table.getDefaultCell().setPadding(0);

        PdfPCell lc = new PdfPCell(new Paragraph(left, leftFont));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPadding(0);
        table.addCell(lc);

        PdfPCell rc = new PdfPCell(new Paragraph(right, rightFont));
        rc.setBorder(Rectangle.NO_BORDER);
        rc.setPadding(0);
        rc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(rc);

        table.setSpacingBefore(0);
        table.setSpacingAfter(0);
        document.add(table);
    }

    private void addReceiptLine(Document document) throws DocumentException {
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(1);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(TEXT_MUTED);
        sep.addCell(cell);
        sep.setSpacingBefore(3);
        sep.setSpacingAfter(3);
        document.add(sep);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "N/A";
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }
}
