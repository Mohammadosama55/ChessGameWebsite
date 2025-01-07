package com.chessgame.service;

import com.chessgame.util.DBUtil;
import com.chessgame.dao.TournamentDAO;
import com.chessgame.dao.UserDAO;
import com.chessgame.model.Tournament;
import com.chessgame.model.User;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.image.ImageDataFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;

public class TournamentCertificateService {
    private static final String LOGO_PATH = "/images/chess-logo.png";
    private static final DeviceRgb GOLD_COLOR = new DeviceRgb(212, 175, 55);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private final TournamentDAO tournamentDAO;
    private final UserDAO userDAO;
    
    public TournamentCertificateService() {
        this.tournamentDAO = new TournamentDAO();
        this.userDAO = new UserDAO();
    }
    
    public void generateCertificate(int userId, int tournamentId, HttpServletResponse response) 
            throws SQLException, IOException {
        // Get tournament and user data
        Tournament tournament = tournamentDAO.getTournamentById(tournamentId);
        User user = userDAO.getUserById(userId);
        
        if (tournament == null || user == null) {
            throw new IllegalArgumentException("Tournament or User not found");
        }
        
        TournamentData data = new TournamentData(tournament, user);
        
        // Generate PDF certificate
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4.rotate());
        
        try {
            // Set fonts
            PdfFont titleFont = PdfFontFactory.createFont();
            PdfFont textFont = PdfFontFactory.createFont();
            
            // Add border
            addCertificateBorder(document);
            
            // Add logo
            addLogo(document);
            
            // Add certificate content
            addCertificateContent(document, data, titleFont, textFont);
            
            document.close();
            
            // Send PDF to response
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                             "attachment; filename=tournament_certificate_" + tournamentId + ".pdf");
            response.getOutputStream().write(baos.toByteArray());
            
        } finally {
            if (document != null && !document.isClosed()) {
                document.close();
            }
        }
    }
    
    private void addCertificateBorder(Document document) {
        float margin = 30;
        float width = PageSize.A4.getHeight() - 2 * margin; // Using height because page is rotated
        float height = PageSize.A4.getWidth() - 2 * margin;
        
        Table border = new Table(1);
        border.setWidth(UnitValue.createPointValue(width));
        border.setHeight(UnitValue.createPointValue(height));
        border.setBorder(Border.NO_BORDER);
        border.setMarginTop(margin);
        border.setMarginBottom(margin);
        border.setMarginLeft(margin);
        border.setMarginRight(margin);
        
        Cell cell = new Cell();
        cell.setBorder(Border.NO_BORDER);
        cell.setNextRenderer(new CertificateBorderRenderer(cell));
        
        border.addCell(cell);
        document.add(border);
    }
    
    private void addLogo(Document document) throws IOException {
        URL logoUrl = getClass().getResource(LOGO_PATH);
        Image logo = new Image(ImageDataFactory.create(logoUrl));
        logo.setWidth(100);
        logo.setMarginBottom(20);
        document.add(logo);
    }
    
    private void addCertificateContent(Document document, TournamentData data, 
                                     PdfFont titleFont, PdfFont textFont) {
        // Certificate title
        Paragraph title = new Paragraph("Certificate of Achievement")
            .setFont(titleFont)
            .setFontSize(36)
            .setFontColor(GOLD_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30);
        document.add(title);
        
        // Main text
        Paragraph mainText = new Paragraph()
            .setFont(textFont)
            .setFontSize(18)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        mainText.add("This is to certify that\n\n");
        mainText.add(new Text(data.playerName).setFontSize(24).setBold());
        mainText.add("\n\nhas successfully participated in\n\n");
        mainText.add(new Text(data.tournamentName).setFontSize(24).setBold());
        document.add(mainText);
        
        // Achievement details
        Paragraph details = new Paragraph()
            .setFont(textFont)
            .setFontSize(16)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30);
        details.add("Achieving ");
        details.add(new Text(getPlacementText(data.placement)).setBold());
        details.add(" place with a performance rating of ");
        details.add(new Text(String.valueOf(data.performanceRating)).setBold());
        document.add(details);
        
        // Date
        Paragraph date = new Paragraph("Issued on " + data.tournamentEndDate.format(DATE_FORMATTER))
            .setFont(textFont)
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(50);
        document.add(date);
        
        // Signatures
        Table signatures = new Table(2);
        signatures.setWidth(UnitValue.createPercentValue(80));
        signatures.setMarginLeft(UnitValue.createPercentValue(10));
        
        Cell tournamentDirector = new Cell()
            .setBorder(Border.NO_BORDER)
            .add(new Paragraph("Tournament Director")
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(textFont)
                .setFontSize(14));
        
        Cell chiefArbiter = new Cell()
            .setBorder(Border.NO_BORDER)
            .add(new Paragraph("Chief Arbiter")
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(textFont)
                .setFontSize(14));
        
        signatures.addCell(tournamentDirector);
        signatures.addCell(chiefArbiter);
        document.add(signatures);
    }
    
    private String getPlacementText(int placement) {
        if (placement < 1) return "participation";
        
        String suffix = switch (placement) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        
        return placement + suffix;
    }
    
    private static class TournamentData {
        final String tournamentName;
        final String playerName;
        final int placement;
        final int performanceRating;
        final java.time.LocalDateTime tournamentEndDate;
        
        TournamentData(Tournament tournament, User user) {
            this.tournamentName = tournament.getName();
            this.playerName = user.getUsername();
            this.placement = 1; // Default placement
            this.performanceRating = 1000; // Default performance rating
            this.tournamentEndDate = tournament.getEndDate();
        }
    }
}
