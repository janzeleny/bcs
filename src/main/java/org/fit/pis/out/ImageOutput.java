package org.fit.pis.out;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.fit.pis.Output;
import org.fit.pis.PageArea;


public class ImageOutput implements Output {
    private BufferedImage boxImg;
    public ImageOutput(Rectangle view, ArrayList<PageArea> groups, ArrayList<PageArea> ungrouped) {
        Graphics2D g;

        this.boxImg = new BufferedImage((int)view.getWidth(), (int)view.getHeight(), BufferedImage.TYPE_INT_ARGB);
        g = this.boxImg.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, (int)view.getWidth(), (int)view.getHeight());
        g.setColor(Color.black);
        for (PageArea area: groups) {
            g.drawRect(area.getLeft(), area.getTop(), area.getWidth(), area.getHeight());
        }
        g.setColor(Color.red);
        for (PageArea area: ungrouped) {
            g.drawRect(area.getLeft(), area.getTop(), area.getWidth(), area.getHeight());
        }

    }

    @Override
    public void save(String path) {
        File outputfile = new File(path);
        try {
            ImageIO.write(this.boxImg, "png", outputfile);
        } catch (IOException e) {
        }
    }
}
