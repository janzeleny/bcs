package org.fit.pis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.fit.cssbox.layout.BackgroundImage;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.ContentImage;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.TextBox;

import cz.vutbr.web.css.CSSProperty.TextDecoration;

public class PageImage
{
    private final ElementBox root;

    private final BufferedImage img;
    private final Graphics2D g;

    private final Color bgColor;

//    public static final double SIMILARITY_THRESHOLD = 2.3;
    public static final double SIMILARITY_THRESHOLD = 7.5;

    public PageImage(BrowserCanvas canvas)
    {
        this.root = canvas.getRootBox();
        this.img = new BufferedImage(canvas.getViewport().getWidth(), canvas.getViewport().getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.g = img.createGraphics();
        this.bgColor = canvas.getBackground();
    }

    public void draw()
    {
        this.g.setColor(this.bgColor);
        this.g.drawRect(0, 0, img.getWidth(), img.getHeight());
        this.g.fillRect(0, 0, img.getWidth(), img.getHeight());

        this.drawSubtree(this.root);
    }

    public void drawSubtree(ElementBox root)
    {
        Box child;
        int i;

        this.drawBox(root);
        /* Now recurse to the rest of the tree */
        for (i = root.getStartChild(); i < root.getEndChild(); i++)
        {
            child = root.getSubBox(i);
            if (child instanceof TextBox)
            {
                this.drawText((TextBox)child);
            }
            else if (child instanceof ReplacedBox)
            {
                this.drawImage((ReplacedBox)child, child.getAbsoluteContentBounds());
            }
            else
            {
                this.drawSubtree((ElementBox)child);
            }
        }
    }

    private void drawText(TextBox box)
    {
        Color color;
        Color bgColor;
        float []hsb;
        float []bgHsb;
        int white_multiplier;
        int hsb_index;
        Rectangle pos = box.getAbsoluteContentBounds();

        color = box.getVisualContext().getColor();
        hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        /* DOC: white and grey need special treatment */
        if (hsb[1] == 0)
        {
            if (hsb[2] == 1)
            {
                /* The text is white, we want to get the color of background ... */

                bgColor = new Color(this.img.getRGB(box.getContentX(), box.getContentY()));
                bgHsb = Color.RGBtoHSB(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), null);
                hsb[0] = bgHsb[0];

                /* ... we want to slightly modify the initial value (so bold can be actually emphasized) ... */
                hsb[1] = (float)0.2;

                /* ... we want to modify saturation ... */
                hsb_index = 1;
                /* ... and we want to subtract from it for emphasis */
                white_multiplier = -1;
            }
            else
            {
                /* The text is grey - we want to modify brightness ... */
                hsb_index = 2;
                /* ... and we want to subtract from it for emphasis ... */
                white_multiplier = -1;

                if (hsb[2] == 0)
                {
                    /* The color is black, set the initial value higher so bold can be actually emphasized */
                    hsb[2] = (float)0.2;
                }
            }
        }
        else
        {
            /* The text colored - we want to modify saturation ... */
            hsb_index = 1;
            /* ... and we want to add to it for emphasis */
            white_multiplier = 1;
        }

        for (TextDecoration dec: box.getVisualContext().getTextDecoration())
        {
            if (dec == TextDecoration.UNDERLINE)
            {
                hsb[hsb_index] += white_multiplier*0.2;
                break;
            }
        }
        if (box.getVisualContext().getFont().isItalic())
        {
            hsb[hsb_index] -= white_multiplier*0.2;
        }

        if (box.getVisualContext().getFont().isBold())
        {
            hsb[hsb_index] += white_multiplier*0.3;
        }

        if (hsb[hsb_index] > 1.0) hsb[hsb_index] = (float)1.0;
        else if (hsb[hsb_index] < 0.0) hsb[hsb_index] = (float)0.0;

        this.g.setColor(new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])));
        this.g.drawRect(pos.x, pos.y, pos.width, pos.height);
        this.g.fillRect(pos.x, pos.y, pos.width, pos.height);
    }


    private void drawImage(ReplacedBox box, Rectangle pos)
    {
        AverageColor avg;
        ContentImage imgObj = (ContentImage)box.getContentObj();

        avg = new AverageColor(imgObj.getBufferedImage());
        if (avg.getColor() == null) return;

        this.g.setColor(avg.getColor());
        this.g.drawRect(pos.x, pos.y, pos.width, pos.height);
        this.g.fillRect(pos.x, pos.y, pos.width, pos.height);

//        this.g.drawImage(imgObj.getBufferedImage(), pos.x, pos.y, pos.width, pos.height, null);
    }

    private void drawBox(ElementBox box)
    {
        Rectangle rect;
        Color c;

        rect = box.getAbsoluteBackgroundBounds(); // background is bounded by content and padding

        c = this.getBgColor(box);
        if (c != null)
        {

            this.g.setColor(c);
            this.g.drawRect(rect.x, rect.y, rect.width, rect.height);
            this.g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }

//        BufferedImage bgImg;
//        List<BackgroundImage> images;
//        images = box.getBackgroundImages();
//        if (images != null && images.size() > 0)
//        {
//            bgImg = images.get(0).getBufferedImage();
//            this.g.drawImage(bgImg, rect.x, rect.y, rect.width, rect.height, null);
//        }
    }

    private Color getBgColor(ElementBox box)
    {
        List<BackgroundImage> images;
        BufferedImage bgImg;
        Color color;
        AverageColor imgColor;
        Rectangle rect;

        images = box.getBackgroundImages();
        if (images != null && images.size() > 0)
        {
//            bgImg = images.get(0).getBufferedImage();
            bgImg = images.get(images.size()-1).getBufferedImage();
            if (bgImg == null)
            {
                imgColor = new AverageColor(Color.white, 1);
            }
            else
            {
                imgColor = new AverageColor(bgImg);
            }
            if (imgColor.getColor() == null) return null;
            /* DOC: mixing color of bg image with bg
             * - more precise -> if the bg is small compared to the box, it won't be so visual distinct
             * - also consider not mixing (original functionality)
             *   -> gives more distinct outline of the box
             *   -> even if small, the bg image may be used to visually higlight the box
             */
            rect = box.getAbsoluteBackgroundBounds();
            color = box.getBgcolor();
            if (color == null)
            { /* BG is transparent - detect current color on the image */
                color = new Color(this.img.getRGB(rect.x, rect.y));
            }

            return imgColor.mixWithBackground(color);
        }

        return box.getBgcolor();
    }

    public ArrayList<PageArea> getAreas()
    {
        int rowCounter = 0;
        int colCounter;
//        int []line;
        Color pixel;
        Color pixelBefore = null;
        ArrayList<PageArea> areas = new ArrayList<PageArea>();
        PageArea area;
        PageArea openedArea;
        boolean boundingBox;
        double colorDiff;

        List<PageArea> openAreas = new ArrayList<PageArea>();
        List<PageArea> lineStack = new ArrayList<PageArea>();

        while (rowCounter < img.getHeight())
        {
//            line = this.img.getRGB(0, rowCounter, img.getWidth(), 1, null, 0, 1);
            colCounter = 0;
            pixelBefore = null;
            for (colCounter = 0; colCounter < img.getWidth(); colCounter++)
            {
                pixel = new Color(this.img.getRGB(colCounter, rowCounter));
                if (rowCounter == 0 && colCounter == 0)
                {
                    area = new PageArea(this.img, pixel, colCounter, rowCounter);
                    this.saveArea(area, areas, openAreas);
                    lineStack.add(0, area);
                }
                else
                {
                    colorDiff = PageArea.colorDiff(pixel, pixelBefore);
//                    if (colCounter == 0 || !pixel.equals(pixelBefore))
                    if (colCounter == 0 || colorDiff > SIMILARITY_THRESHOLD)
                    {
                        boundingBox = false;
                        area = new PageArea(this.img, pixel, colCounter, rowCounter);
                        if (colCounter != 0)
                        {
                            for (PageArea a: lineStack)
                            {
                                if (area.getBottom() == a.getBottom()+1 &&
                                    area.getTop() == a.getTop()-1 &&
                                    area.getLeft() == a.getLeft()-1 &&
                                    area.getRight() == a.getRight()+1)
                                {
                                    /* This is not a new area, just a bounding box caused by exiting the previous one */
                                    boundingBox = true;
                                    break;
                                }
                            }
                        }
                        if (lineStack.size() > 0)
                        {
                            lineStack.remove(0);
                        }
                        if (!boundingBox)
                        {
                            openedArea = this.getOpened(area, openAreas);
                            if (openedArea == null)
                            {
                                this.saveArea(area, areas, openAreas);
                                openedArea = area;
                            }
                            lineStack.add(0, openedArea);
                        }
                    }
                }

                /* Don't forget to detect what areas were discontinued by this line */
                pixelBefore = pixel;
            }

            lineStack.clear();
            rowCounter++;

            while (openAreas.size() > 0 && openAreas.get(0).getBottom() < rowCounter)
            {
                openAreas.remove(0);
            }
        }

        return areas;
    }

    private PageArea getOpened(PageArea area, List<PageArea> openAreas)
    {
        for (PageArea a: openAreas)
        {
            if (a.equals(area)) return a;
        }

        return null;
    }

    private void saveArea(PageArea area, List<PageArea> areas, List<PageArea> openAreas)
    {
        int i;

        i = 0;
        for (PageArea a: openAreas)
        {
            if (a.getBottom() > area.getBottom())
            {
                break;
            }
            i++;
        }

        openAreas.add(i, area);
        areas.add(area);
    }

    public boolean save(String path)
    {
        File outputfile = new File(path);
        try {
            ImageIO.write(this.img, "png", outputfile);
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
