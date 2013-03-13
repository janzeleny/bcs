package org.fit.pis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.demo.DOMSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Main
{
    public static void main(String []args) throws Exception, IOException, SAXException
    {
        URL url;
        InputStream is;
        URLConnection con;
        PageImage img;
        BrowserCanvas canvas;

        url = new URL("http://www.idnes.cz");
        con = url.openConnection();
        is = con.getInputStream();
        url = con.getURL(); /* Store this (possible redirect happened) */

        DOMSource parser = new DOMSource(is);
        parser.setContentType(con.getHeaderField("Content-Type"));
        Document doc = parser.parse(); //doc represents the obtained DOM

        DOMAnalyzer da = new DOMAnalyzer(doc, url);
        da.attributesToStyles(); //convert the HTML presentation attributes to inline styles
        da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the standard style sheet
        da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the additional style sheet
        da.getStyleSheets(); //load the author style sheets


        canvas = new BrowserCanvas(da.getRoot(), da, new java.awt.Dimension(1000, 600), url);
        img = new PageImage(canvas);
        img.draw();
        img.save("/home/greengo/idnes.png");

        ArrayList<PageArea> areas;
        ArrayList<PageArea> leaves;
        ArrayList<PageArea> groups;
        ArrayList<PageArea> ungrouped;
        AreaProcessor h;
        BufferedImage boxImg;
        Graphics2D g;

        areas = img.getAreas();
        h = new AreaProcessor(areas, canvas.getViewport().getWidth(), canvas.getViewport().getHeight());
        leaves = h.getAreas();
        groups = h.extractGroups(h.getAreas());
        ungrouped = h.getUngrouped();

        boxImg = new BufferedImage(canvas.getViewport().getWidth(), canvas.getViewport().getHeight(), BufferedImage.TYPE_INT_ARGB);
        g = boxImg.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, canvas.getViewport().getWidth(), canvas.getViewport().getHeight());
        g.setColor(Color.black);
        for (PageArea area: groups)
        {
            g.drawRect(area.getLeft(), area.getTop(), area.getWidth(), area.getHeight());
        }
        g.setColor(Color.red);
        for (PageArea area: ungrouped)
        {
            g.drawRect(area.getLeft(), area.getTop(), area.getWidth(), area.getHeight());
        }

        File outputfile = new File("/home/greengo/idnes-boxes.png");
        try {
            ImageIO.write(boxImg, "png", outputfile);
        } catch (IOException e) {
        }
    }
}
