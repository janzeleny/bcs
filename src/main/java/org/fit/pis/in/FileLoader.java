package org.fit.pis.in;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.fit.pis.PageArea;

public class FileLoader {
    ArrayList<PageArea> areas;

    public FileLoader(String path) throws IOException {
        BufferedReader r;
        String [] lineArray;
        String [] coordinates;
        String line;
        PageArea area;
        Color color;
        int left, top, width, height;

        this.areas = new ArrayList<>();

        r = new BufferedReader(new FileReader(path));
        while ((line = r.readLine()) != null) {
            lineArray = line.split(":");
            coordinates = lineArray[0].split(",");
            color = new Color(Integer.parseInt(lineArray[1]), true);

            left = Integer.parseInt(coordinates[0]);
            top = Integer.parseInt(coordinates[1]);
            width = Integer.parseInt(coordinates[2]);
            height = Integer.parseInt(coordinates[3]);
            area = new PageArea(color, left, top, left+width, top+height);
            this.areas.add(area);
        }

        r.close();
    }

    public ArrayList<PageArea> getAreas() {
        return this.areas;
    }

    public void save(String path) {
        Rectangle r;
        Graphics2D g;
        BufferedImage img;

        r = this.getPageDimensions();

        img = new BufferedImage((int)r.getWidth(), (int)r.getHeight(), BufferedImage.TYPE_INT_ARGB);
        g = img.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, (int)r.getWidth(), (int)r.getHeight());
        for (PageArea area: this.areas) {
            g.setColor(area.getColor());
            g.fillRect(area.getLeft(), area.getTop(), area.getWidth(), area.getHeight());
        }

        File outputfile = new File(path);
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {
        }
    }

    public Rectangle getPageDimensions() {
        int width = 0, height = 0;

        for (PageArea a: this.areas) {
            if (a.getRight() > width) width = a.getRight();
            if (a.getBottom() > height) height = a.getBottom();
        }

        return new Rectangle(width, height);
    }
}
