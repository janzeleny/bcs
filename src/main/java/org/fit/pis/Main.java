package org.fit.pis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Main
{
    public static final String home = "/home/greengo/";

    private static DOMAnalyzer renderPage(URL url) throws Exception, IOException, SAXException
    {
        URLConnection con;
        DocumentSource docSource;

        con = url.openConnection();
        url = con.getURL(); /* Store this (possible redirect happened) */
        docSource = new DefaultDocumentSource(url);

        DefaultDOMSource parser = new DefaultDOMSource(docSource);
        parser.setContentType(con.getHeaderField("Content-Type"));
        Document doc = parser.parse(); //doc represents the obtained DOM

        DOMAnalyzer da = new DOMAnalyzer(doc, url);
        da.attributesToStyles(); //convert the HTML presentation attributes to inline styles
        da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the standard style sheet
        da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the additional style sheet
        da.getStyleSheets(); //load the author style sheets

        return da;
    }

    private static void drawImage(BrowserCanvas canvas, String pageName)
    {
        PageImage img;

        img = new PageImage(canvas);
        img.draw();
        img.save(home+pageName+".png");
    }

    private static void drawBoxes(BrowserCanvas canvas, ArrayList<PageArea> groups, ArrayList<PageArea> ungrouped, String imageName)
    {
        BufferedImage boxImg;
        Graphics2D g;

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

        File outputfile = new File(home+imageName+"-boxes.png");
        try {
            ImageIO.write(boxImg, "png", outputfile);
        } catch (IOException e) {
        }
    }

    public static void main(String []args) throws Exception, IOException, SAXException
    {
        String urlString;
        String imageString;
        DOMAnalyzer da;
        URL url;
        BrowserCanvas canvas;
        ArrayList<PageArea> areas;
        ArrayList<PageArea> groups;
        ArrayList<PageArea> ungrouped;
        double threshold = -1;
        boolean debug = false;
        boolean debugSet = false;

        if (args.length < 1)
        {
            System.out.println("./run.sh <address>[ <threshold>[ debug]]");
            return;
        }
        else if (args.length > 1)
        {
            threshold = new Double(args[1]);
            if (args.length > 2)
            {
                debugSet = true;
                debug = new Boolean(args[2]);
            }
        }
        urlString = args[0];
        System.out.println(urlString);
        imageString = urlString.replaceFirst("https?://", "").replaceFirst("/$", "").replaceAll("/", "-").replaceAll("\\?.*", "");
        url = new URL(urlString);

        da = renderPage(url);
        canvas = new BrowserCanvas(da.getRoot(), da, new java.awt.Dimension(1000, 600), url);

        drawImage(canvas, imageString);

        AreaProcessor2 h;
        AreaCreator c;

        c = new AreaCreator(canvas.getViewport().getWidth(), canvas.getViewport().getHeight());
        areas = c.getAreas(canvas.getRootBox());

        h = new AreaProcessor2(areas, canvas.getViewport().getWidth(), canvas.getViewport().getHeight());
        if (threshold > 0) h.setThreshold(threshold);
        if (debugSet) h.setDebug(debug);

        groups = h.extractGroups(h.getAreas());
        ungrouped = h.getUngrouped();

        drawBoxes(canvas, groups, ungrouped, imageString);
    }
}
