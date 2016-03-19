package org.fit.pis;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.fit.cssbox.layout.Viewport;
import org.fit.pis.out.ImageOutput;
import org.fit.pis.out.TextOutput;
import org.xml.sax.SAXException;

public class Main
{
    public static final String home = "/home/greengo/";
    public static double threshold = -1;




    public static void process(Viewport view, AreaCreator c, String imageString, Boolean debug) throws Exception {
        ArrayList<PageArea> areas;
        ArrayList<PageArea> groups;
        ArrayList<PageArea> ungrouped;
        AreaProcessor2 h;
        ImageOutput out;
        TextOutput textOut;

        areas = c.getAreas(view.getRootBox());
        h = new AreaProcessor2(areas, view.getWidth(), view.getHeight());
        if (threshold > 0) h.setThreshold(threshold);
        if (debug != null) h.setDebug(debug);

        groups = h.extractGroups(h.getAreas());
        ungrouped = h.getUngrouped();

        /* For the sake of the right name */
        threshold = h.getThreshold();

        out = new ImageOutput(view, groups, ungrouped);
        out.save(home+imageString+"-boxes-"+threshold+".png");

        textOut = new TextOutput(groups, ungrouped);
        textOut.save(home+imageString+"-boxes-"+threshold+".txt");
    }


    public static void main(String []args) throws Exception, IOException, SAXException
    {
        String urlString;
        String imageString;
        URL url;
        Viewport view;
        Boolean debug = null;
        PageLoader pl;
        AreaCreator c;

        if (args.length < 1)
        {
            System.out.println("./run.sh <address>[ <threshold>[ debug]]");
            return;
        } else if (args.length == 1) {
            threshold = 0.3;
        } else {
            threshold = new Double(args[1]);
            if (args.length > 2) {
                debug = new Boolean(args[2]);
            }
        }
        urlString = args[0];
        System.out.println(urlString);
        imageString = urlString.replaceFirst("https?://", "").replaceFirst("/$", "").replaceAll("/", "-").replaceAll("\\?.*", "");
        if (imageString.length() > 128) imageString = imageString.substring(0, 128);
        url = new URL(urlString);

        pl = new PageLoader(url);
        view = pl.getViewport(new java.awt.Dimension(1000, 600));
        pl.save(imageString+".png");

        c = new AreaCreator(view.getWidth(), view.getHeight());

        process(view, c, imageString, debug);

        System.exit(0); /* Can't just return, as the AWT Thread was created */
    }
}
