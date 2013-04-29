package org.fit.pis;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fit.cssbox.layout.BackgroundImage;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ContentImage;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.TextBox;

import cz.vutbr.web.css.CSSProperty.TextDecoration;

public class AreaCreator
{
    private ArrayList<PageArea> areas;
    private final int pageWidth;
    private final int pageHeight;

    public AreaCreator(int w, int h)
    {
        this.pageWidth = w;
        this.pageHeight = h;
    }

    public ArrayList<PageArea> getAreas(ElementBox root)
    {
        this.areas = new ArrayList<PageArea>();

        this.getAreasSubtree(root, Color.white);

        Collections.sort(this.areas, new AreaSizeComparator());
        return this.areas;
    }

    private void getAreasSubtree(ElementBox root, Color parentBg)
    {
        Box child;
        int i;
        int start, end;
        Color bgColor;

        start = root.getStartChild();
        end = root.getEndChild();
        bgColor = this.getBgColor(root, parentBg);

        if (start == end)
        {
            /* No children */
//            this.getArea(root, parentBg);
            return;
        }
        else if (start+1 == end)
        {
            // figure out if this line of "one child" continues to the bottom of tree
            child = root.getSubBox(start);
            if (child instanceof TextBox)
            {
                this.getTextArea((TextBox)child, bgColor);
                return;
            }
            else
            {
                if (this.hasNoBranches((ElementBox)child))
                {
                    this.getSmallestBox(root, parentBg);
                    return;
                }

                /* If it has branches, not much special to do - we still have to inspect all children */
            }
        }

        /* Recurse to the rest of the tree */
        for (i = root.getStartChild(); i < root.getEndChild(); i++)
        {
            child = root.getSubBox(i);
            if (child instanceof TextBox)
            {
                this.getTextArea((TextBox)child, bgColor);
            }
            else if (child instanceof ReplacedBox)
            {
                this.getImageArea((ReplacedBox)child, child.getAbsoluteContentBounds());
            }
            else
            {
                this.getAreasSubtree((ElementBox)child, bgColor);
            }
        }
    }

    private boolean hasNoBranches(ElementBox root)
    {
        int start, end;
        Box child;

        start = root.getStartChild();
        end = root.getEndChild();

        if (start == end) return true;
        else if (start+1 == end)
        {
            child = root.getSubBox(start);
            if (child instanceof TextBox) return true;
            else return hasNoBranches((ElementBox)child);
        }
        else
        {
            return false;
        }
    }

    private void getSmallestBox(ElementBox root, Color parentBg)
    {
        int start, end;
        Box child;
        Color bgColor;


        start = root.getStartChild();
        end = root.getEndChild();

        if (start == end)
        {
            /* No children - we have to return this one */
            this.getArea(root, parentBg);
        }
        else
        {
            child = root.getSubBox(start);
            bgColor = this.getBgColor(root, parentBg);
            if (child instanceof TextBox)
            {
                this.getTextArea((TextBox)child, bgColor);
            }
            else if (child instanceof ReplacedBox)
            {
                this.getImageArea((ReplacedBox)child, child.getAbsoluteContentBounds());
            }
            else
            {
                if (this.isTransparent(root))
                {
                    getSmallestBox((ElementBox)child, parentBg);
                }
                else
                {
                    this.getArea((ElementBox)child, parentBg);
                }
            }
        }
    }

    private Color getBgColor(ElementBox box, Color parentBg)
    {
        List<BackgroundImage> images;
        BufferedImage bgImg;
        Color color;
        AverageColor imgColor;

        images = box.getBackgroundImages();
        color = box.getBgcolor();
        if (color == null)
        { /* BG is transparent - use the color of the parent */
            color = parentBg;
        }

        if (images != null && images.size() > 0)
        {
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

            return imgColor.mixWithBackground(color);
        }

        return color;
    }

    private boolean isTransparent(ElementBox box)
    {
        List<BackgroundImage> images;
        Color color;

        images = box.getBackgroundImages();
        color = box.getBgcolor();

        return (color == null && (images == null || images.size() == 0));
    }

    private void getTextArea(TextBox box, Color bgColor)
    {
        Color color;
        float []hsb;
        float []bgHsb;
        int white_multiplier;
        int hsb_index;
        PageArea area;
        java.awt.Rectangle pos = box.getAbsoluteContentBounds();

        color = box.getVisualContext().getColor();
        hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        /* DOC: white and grey need special treatment */
        if (hsb[1] == 0)
        {
            if (hsb[2] == 1)
            {
                /* The text is white, we want to get the color of background ... */

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

        area = new PageArea(new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])),
                            pos.x, pos.y, pos.x+pos.width, pos.y+pos.height);
        this.areas.add(area);
    }


    private void getImageArea(ReplacedBox box, java.awt.Rectangle pos)
    {
        AverageColor avg;
        PageArea area;
        ContentImage imgObj = (ContentImage)box.getContentObj();

        avg = new AverageColor(imgObj.getBufferedImage());
        if (avg.getColor() == null) return;

        area = new PageArea(avg.getColor(), pos.x, pos.y, pos.x+pos.width, pos.y+pos.height);
        this.areas.add(area);
    }

    private void getArea(ElementBox box, Color parentBg)
    {
        PageArea area;
        Rectangle rect;
        Color c;
        int t, l, r, b;

        rect = box.getAbsoluteBackgroundBounds(); // background is bounded by content and padding
        l = rect.x;
        t = rect.y;
        r = rect.x+rect.width;
        b = rect.y+rect.height;

        if (l > this.pageWidth || t > this.pageHeight || r < 0 || b < 0) return;

        c = this.getBgColor(box, parentBg);
        if (c != null)
        {
            area = new PageArea(c, l, t, r, b);
            this.areas.add(area);
        }
    }
}