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
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import cz.vutbr.web.css.MediaSpec;

public class Main
{
    public static final String home = "/home/greengo/";
    public static double threshold = -1;

    private static DOMAnalyzer renderPage(URL url) throws Exception, IOException, SAXException
    {
        URLConnection con;
        DocumentSource src = new DefaultDocumentSource(url);

        con = url.openConnection();
        url = con.getURL(); /* Store this (possible redirect happened) */

        DOMSource parser = new DefaultDOMSource(src);
        parser.setContentType(con.getHeaderField("Content-Type"));
        Document doc = parser.parse();

        String encoding = parser.getCharset();

        MediaSpec media = new MediaSpec("screen");

        DOMAnalyzer da = new DOMAnalyzer(doc, url);
        if (encoding == null)
            encoding = da.getCharacterEncoding();
        da.setDefaultEncoding(encoding);
        da.setMediaSpec(media);
        da.attributesToStyles();
        da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT);
        da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT);
        da.addStyleSheet(null, CSSNorm.formsStyleSheet(), DOMAnalyzer.Origin.AGENT);
        da.getStyleSheets();

        src.close();

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

        File outputfile = new File(home+imageName+"-boxes-"+threshold+".png");
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
        if (imageString.length() > 128) imageString = imageString.substring(0, 128);
        url = new URL(urlString);

        da = renderPage(url);

        canvas = new BrowserCanvas(da.getRoot(), da, url);
//        canvas.getConfig().setLoadImages(false);
//        canvas.getConfig().setLoadBackgroundImages(false);
//        canvas.getConfig().setReplaceImagesWithAlt(true);
        canvas.createLayout(new java.awt.Dimension(1000, 600));

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

        /* For the sake of the right name */
        threshold = h.getThreshold();
        drawBoxes(canvas, groups, ungrouped, imageString);
        System.exit(0); /* Can't just return, as the AWT Thread was created */
    }
}
