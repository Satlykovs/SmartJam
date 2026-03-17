package com.smartjam.smartjamanalyzer.infrastructure.visualizer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.port.DebugVisualizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("debug")
@Slf4j
public class ImageIoDebugVisualizer implements DebugVisualizer {

    @Override
    public void generateHeatmap(AnalysisResult result, String filename) {

        double[][] matrix = result.costMatrix();

        if (matrix == null) {
            log.warn("Heatmap requested but costMatrix is null (not in debug mode?)");
            return;
        }

        java.util.List<int[]> path = result.warpingPath();

        int n = matrix.length;
        int m = matrix[0].length;

        int plotW = 1200;
        int plotH = 1200;
        int marginL = 140; // Чуть больше для длинных чисел на Y
        int marginR = 150;
        int marginT = 80;
        int marginB = 140;

        BufferedImage img =
                new BufferedImage(plotW + marginL + marginR, plotH + marginT + marginB, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(5, 5, 10));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        double[] allValues =
                Arrays.stream(matrix).flatMapToDouble(Arrays::stream).toArray();
        Arrays.sort(allValues);
        double maxVal = allValues[(int) (allValues.length * 0.95)];
        double scaleX = (double) plotW / m;
        double scaleY = (double) plotH / n;

        // 1. РИСУЕМ МАТРИЦУ
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                float ratio = (float) Math.pow(matrix[i][j] / maxVal, 1.2);
                ratio = Math.clamp(ratio, 0.0f, 1.0f);
                g.setColor(getMagmaColor(ratio));

                int px = marginL + (int) (j * scaleX);
                int py = marginT + plotH - (int) ((i + 1) * scaleY);
                g.fillRect(px, py, (int) Math.ceil(scaleX), (int) Math.ceil(scaleY));
            }
        }

        // 2. АДАПТИВНАЯ СЕТКА И ЗАСЕЧКИ
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        int tickStepX = calculateTickStep(m);
        int tickStepY = calculateTickStep(n);

        // По оси X (Student)
        for (int j = 0; j <= m; j += tickStepX) {
            int x = marginL + (int) (j * scaleX);
            g.setColor(new Color(255, 255, 255, 40)); // Полупрозрачная сетка
            g.drawLine(x, marginT, x, marginT + plotH);
            g.setColor(Color.WHITE);
            g.drawLine(x, marginT + plotH, x, marginT + plotH + 10); // Засечка
            g.drawString(String.valueOf(j), x - 15, marginT + plotH + 30);
        }

        // По оси Y (Teacher)
        for (int i = 0; i <= n; i += tickStepY) {
            int y = marginT + plotH - (int) (i * scaleY);
            g.setColor(new Color(255, 255, 255, 40));
            g.drawLine(marginL, y, marginL + plotW, y);
            g.setColor(Color.WHITE);
            g.drawLine(marginL - 10, y, marginL, y); // Засечка
            g.drawString(String.valueOf(i), marginL - 50, y + 5);
        }

        // 3. РИСУЕМ ПУТЬ
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < path.size() - 1; k++) {
            int[] p1 = path.get(k);
            int[] p2 = path.get(k + 1);
            g.drawLine(
                    marginL + (int) (p1[1] * scaleX + scaleX / 2),
                    marginT + plotH - (int) (p1[0] * scaleY + scaleY / 2),
                    marginL + (int) (p2[1] * scaleX + scaleX / 2),
                    marginT + plotH - (int) (p2[0] * scaleY + scaleY / 2));
        }

        // 4. COLORBAR С ЗАСЕЧКАМИ
        int cbW = 30;
        int cbX = marginL + plotW + 40;
        for (int y = 0; y < plotH; y++) {
            float r = (float) y / plotH;
            g.setColor(getMagmaColor(1.0f - r));
            g.drawLine(cbX, marginT + y, cbX + cbW, marginT + y);
        }
        g.setColor(Color.WHITE);
        g.drawRect(cbX, marginT, cbW, plotH);
        // Засечки на шкале (каждые 20%)
        for (float r = 0; r <= 1.0; r += 0.25f) {
            int y = marginT + (int) ((1.0 - r) * plotH);
            g.drawLine(cbX + cbW, y, cbX + cbW + 8, y);
            g.drawString(String.format("%.1f", r * maxVal), cbX + cbW + 12, y + 5);
        }

        // 5. ПОДПИСИ ОСЕЙ
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g.drawString("STUDENT TIMELINE (Frames) ->", marginL + plotW / 2 - 200, marginT + plotH + 90);

        AffineTransform orig = g.getTransform();
        g.translate(50, marginT + plotH / 2 + 150);
        g.rotate(-Math.PI / 2);
        g.drawString("TEACHER TIMELINE (Frames) ->", 0, 0);
        g.setTransform(orig);

        g.dispose();
        try {
            ImageIO.write(img, "png", new File(filename));
        } catch (Exception e) {
            log.error("Save failed", e);
        }
    }

    /** Рассчитывает красивый шаг для засечек */
    private int calculateTickStep(int total) {
        if (total <= 0) return 100;
        int rawStep = total / 8; // Хотим примерно 8-10 делений
        if (rawStep < 5) return 5;
        if (rawStep < 20) return 20;
        if (rawStep < 100) return 100;
        if (rawStep < 500) return 500;
        return 1000;
    }

    /** Плавная интерполяция палитры Magma */
    private Color getMagmaColor(float ratio) {
        // Опорные цвета (от черного к ярко-желтому)
        if (ratio < 0.25f) return lerpColor(new Color(0, 0, 5), new Color(60, 15, 110), ratio / 0.25f);
        if (ratio < 0.50f) return lerpColor(new Color(60, 15, 110), new Color(190, 40, 60), (ratio - 0.25f) / 0.25f);
        if (ratio < 0.75f) return lerpColor(new Color(190, 40, 60), new Color(250, 150, 40), (ratio - 0.50f) / 0.25f);
        return lerpColor(new Color(250, 150, 40), new Color(255, 255, 180), (ratio - 0.75f) / 0.25f);
    }

    private Color lerpColor(Color c1, Color c2, float fraction) {
        int r = (int) (c1.getRed() + fraction * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + fraction * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + fraction * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }
}
