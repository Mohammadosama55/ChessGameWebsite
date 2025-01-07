package com.chessgame.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.renderer.CellRenderer;
import com.itextpdf.layout.renderer.DrawContext;

public class CertificateBorderRenderer extends CellRenderer {
    private static final DeviceRgb GOLD_COLOR = new DeviceRgb(212, 175, 55);
    
    public CertificateBorderRenderer(Cell modelElement) {
        super(modelElement);
    }
    
    @Override
    public void drawBorder(DrawContext drawContext) {
        // Draw decorative border
        Rectangle rect = getOccupiedAreaBBox();
        PdfCanvas canvas = drawContext.getCanvas();
        
        // Set border color and width
        canvas.setStrokeColor(GOLD_COLOR);
        canvas.setLineWidth(2);
        
        // Draw outer rectangle
        canvas.rectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        canvas.stroke();
        
        // Draw inner rectangle (margin of 5 points)
        float margin = 5;
        canvas.rectangle(
            rect.getX() + margin,
            rect.getY() + margin,
            rect.getWidth() - 2 * margin,
            rect.getHeight() - 2 * margin
        );
        canvas.stroke();
    }
}
