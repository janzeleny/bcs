package org.fit.pis.cssbox;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BoxFactory;
import org.fit.cssbox.layout.BrowserConfig;
import org.fit.cssbox.layout.Viewport;
import org.fit.cssbox.layout.VisualContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import cz.vutbr.web.css.MediaSpec;

public class PageLoader implements Output {
    private URL url;
    private Viewport viewport;
    private ModelRenderer renderer;
    private PrintStream originalOut = null;

    public PageLoader(String urlString) throws MalformedURLException {
        this(new URL(urlString));
    }

    public PageLoader(URL url) {
        this.url = url;
        this.originalOut = null;
    }

    private DOMAnalyzer loadPage() throws Exception, IOException, SAXException
    {
        URLConnection con;
        DocumentSource src = new DefaultDocumentSource(this.url);

        con = this.url.openConnection();
        this.url = con.getURL(); /* Store this (possible redirect happened) */

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

    private Viewport createViewport(Dimension dim) throws IOException, SAXException, Exception {
        DOMAnalyzer da;
        Element root;
        Viewport viewport;
        BoxFactory factory;
        BufferedImage img;
        Graphics2D ig;
        VisualContext ctx;

        da = this.loadPage();
        root = da.getRoot();

        img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        ig = img.createGraphics();

        da.getMediaSpec().setDimensions(dim.width, dim.height);
        da.recomputeStyles();

        factory = new BoxFactory(da, this.url);
        factory.setConfig(new BrowserConfig());
        factory.reset();
        ctx = new VisualContext(null, factory);
        viewport = factory.createViewportTree(root, ig, ctx, dim.width, dim.height);
        viewport.setVisibleRect(new Rectangle(dim));
        viewport.initSubtree();

        viewport.doLayout(dim.width, true, true);

        viewport.updateBounds(dim);

        if ((viewport.getWidth() > dim.width || viewport.getHeight() > dim.height)) {
            img = new BufferedImage(Math.max(viewport.getWidth(), dim.width),
                                    Math.max(viewport.getHeight(), dim.height),
                                    BufferedImage.TYPE_INT_RGB);
            ig = img.createGraphics();
        }

        viewport.absolutePositions();

        ModelRenderer r = new ModelRenderer(img, ig);
        viewport.draw(r);

        this.renderer = r;
        this.viewport = viewport;

        return viewport;
    }

    public Viewport getViewport(Dimension dim) throws IOException, SAXException, Exception {
        if (this.viewport != null) {
            return this.viewport;
        }

        return this.createViewport(dim);
    }

    public ModelRenderer getRenderer() {
        return this.renderer;
    }

    @Override
    public void save(String path) {
        this.renderer.save(path);
    }

    public void restoreOut() {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
    }

    public void silenceOut() {
        originalOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {

            }
        }));
    }
}

