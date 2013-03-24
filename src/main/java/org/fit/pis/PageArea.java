package org.fit.pis;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.infomatiq.jsi.Rectangle;

public class PageArea
{
    private Color color;
    private int left;
    private int right;
    private int top;
    private int bottom;

    private int startX;
    private int startY;

    private PageArea parent;
    private ArrayList<PageArea> children;

    private Rectangle rectangle;

    public static final int ALIGNMENT_NONE = 0;
    public static final int ALIGNMENT_LINE = 1;
    public static final int ALIGNMENT_COLUMN = 2;

    public PageArea(BufferedImage img, Color c, int l, int t)
    {
        this.color = c;
        this.left = l;
        this.top = t;
        this.children = null;
        this.rectangle = null;

        discoverBoundaries(img);
    }

    public PageArea(PageArea a)
    {
        this.color = new Color(a.color.getRGB());
        this.left = a.left;
        this.right = a.right;
        this.top = a.top;
        this.bottom = a.bottom;
    }

    private void discoverBoundaries(BufferedImage img)
    {
        this.startX = this.left;
        this.startY = this.top;

        if (this.goUp(img, this.left, this.top) == false)
        { /* 1px vertical line */
            if (this.goRight(img, this.left, this.top) == false)
            {
                this.right = this.left;
                this.goDown(img, this.left, this.top);
            }
        }
        return;
    }

    private boolean goRight(BufferedImage img, int startX, int y)
    {
        boolean ret = false;
        int x = startX;
        Color pixel;

        if (x == img.getWidth()-1) return false;

        pixel = new Color(img.getRGB(x+1, y));
        while (pixel.equals(this.color))
        {
            if (this.goUp(img, x+1, y) == true)
            {
                if (x > this.right) this.right = x;
                return true;
            }
            ret = true;
            x++;
            if (x == img.getWidth()-1) break;
            pixel = new Color(img.getRGB(x+1, y));
        }
        if (x > this.right) this.right = x;
        if (ret == false) return false;
        else return this.goDown(img, x, y);
    }

    private boolean goLeft(BufferedImage img, int startX, int y)
    {
        boolean ret = false;
        int x = startX;
        Color pixel;

        if (x == 0) return false;

        pixel = new Color(img.getRGB(x-1, y));
        while (pixel.equals(this.color))
        {
            if (this.goDown(img, x-1, y) == true)
            {
                if (x < this.left) this.left = x;
                return true;
            }
            ret = true;
            x--;
            if (x == this.startX && y == this.startY)
            {
                if (x < this.left) this.left = x;
                return true;
            }
            if (x == 0) break;
            pixel = new Color(img.getRGB(x-1, y));
        }
        if (x < this.left) this.left = x;
        if (ret == false) return false;
        else return this.goUp(img, x, y);
    }

    private boolean goUp(BufferedImage img, int x, int startY)
    {
        boolean ret = false;
        int y = startY;
        Color pixel;

        if (y == 0) return false;

        pixel = new Color(img.getRGB(x, y-1));
        while (pixel.equals(this.color))
        {
            if (this.goLeft(img, x, y-1) == true)
            {
                if (y < this.top) this.top = y;
                return true;
            }
            ret = true;
            y--;
            if (x == this.startX && y == this.startY)
            {
                if (y < this.top) this.top = y;
                return true;
            }
            if (y == 0) break;
            pixel = new Color(img.getRGB(x, y-1));
        }
        if (y < this.top) this.top = y;
        if (ret == false) return false;
        else return this.goRight(img, x, y);
    }

    private boolean goDown(BufferedImage img, int x, int startY)
    {
        boolean ret = false;
        int y = startY;
        Color pixel;

        if (y == img.getHeight()-1) return false;

        pixel = new Color(img.getRGB(x, y+1));
        while (pixel.equals(this.color))
        {
            if (this.goRight(img, x, y+1) == true)
            {
                if (y > this.bottom) this.bottom = y;
                return true;
            }
            ret = true;
            y++;
            if (y == img.getHeight()-1) break;
            pixel = new Color(img.getRGB(x, y+1));
        }
        if (y > this.bottom) this.bottom = y;
        if (ret == false) return false;
        else return this.goLeft(img, x, y);
    }

    public boolean containsPixel(int x, int y)
    {
        return x >= this.left && x <= this.right &&
               y >= this.top && y <= this.bottom;
    }

    public boolean equals(PageArea obj)
    {
        return this.color.equals(obj.color) &&
               this.left == obj.left &&
               this.right == obj.right &&
               this.top == obj.top &&
               this.bottom == obj.bottom;
    }

    public boolean contains(PageArea obj)
    {
        return this.left <= obj.left &&
               this.right >= obj.right &&
               this.top <= obj.top &&
               this.bottom >= obj.bottom;
    }

    public boolean overlaps(PageArea obj)
    {
        return this.right >= obj.left && this.left <= obj.right && this.bottom >= obj.top && this.top <= obj.bottom;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    public int getLeft()
    {
        return left;
    }

    public void setLeft(int left)
    {
        this.left = left;
        this.rectangle = null;
    }

    public int getRight()
    {
        return right;
    }

    public void setRight(int right)
    {
        this.right = right;
        this.rectangle = null;
    }

    public int getTop()
    {
        return top;
    }

    public void setTop(int top)
    {
        this.top = top;
        this.rectangle = null;
    }

    public int getBottom()
    {
        return bottom;
    }

    public void setBottom(int bottom)
    {
        this.bottom = bottom;
        this.rectangle = null;
    }

    public int getWidth()
    {
        return this.right - this.left + 1;
    }

    public int getHeight()
    {
        return this.bottom - this.top + 1;
    }

    public void addChild(PageArea child)
    {
        this.addChild(child, false);
    }

    public PageArea tryAdd(PageArea a)
    {
        PageArea ret = new PageArea(this);
        ret.addChild(a, true);
        return ret;
    }

    public void addChild(PageArea child, boolean tryout)
    {
        if (this.children == null)
        {
            this.children = new ArrayList<PageArea>();
        }

        this.children.add(child);
        if (!tryout)
        {
            child.setParent(this);
        }

        if (child.getBottom() > this.getBottom()) this.setBottom(child.getBottom());
        if (child.getRight() > this.getRight()) this.setRight(child.getRight());
        if (child.getLeft() < this.getLeft()) this.setLeft(child.getLeft());
        if (child.getTop() < this.getTop()) this.setTop(child.getTop());
    }

    public void delChild(PageArea child)
    {
        // TODO: adjust borders, pattern and reset the rectangle
        this.children.remove(child);
    }

    public List<PageArea> getChildren()
    {
        return this.children;
    }

    public void setParent(PageArea parent)
    {
        this.parent = parent;
    }

    public PageArea getParent()
    {
        return this.parent;
    }

    public double getSimilarityRecursive(PageArea area)
    {
        double bestMatch = 100;
        double similarity;
        double similaritySum;
        int similarityCount;

        if (this.getChildren() != null && this.getChildren().size() > 0)
        {
            if (area.getChildren() != null && area.getChildren().size() > 0)
            {
                /* Both object have children, we need to compare each couple of children */
                similarityCount = 0;
                similaritySum = 0;
                for (PageArea a: this.getChildren())
                {
                    for (PageArea b: area.getChildren())
                    {
                        similarity = a.getSimilarity(b);
                        if (similarity < bestMatch) bestMatch = similarity;
                        similaritySum += similarity;
                        similarityCount++;
                    }
                }
            }
            else
            {
                /* area has no children, we need to compare it to all children of this object */
                similarityCount = 0;
                similaritySum = 0;
                for (PageArea a: this.getChildren())
                {
                    similarity = area.getSimilarity(a);
                    if (similarity < bestMatch) bestMatch = similarity;
                    similaritySum += similarity;
                    similarityCount++;
                }
            }
        }
        else if (area.getChildren() != null && area.getChildren().size() > 0)
        {
            /* area has children, we need to compare each of them to this object */
            similarityCount = 0;
            similaritySum = 0;
            for (PageArea a: area.getChildren())
            {
                similarity = this.getSimilarity(a);
                if (similarity < bestMatch) bestMatch = similarity;
                similaritySum += similarity;
                similarityCount++;
            }
        }
        else
        {
            similarity = this.getSimilarity(area);
            if (similarity < bestMatch) bestMatch = similarity;
            similaritySum = similarity;
            similarityCount = 1;
        }

        return bestMatch;
//        return similaritySum/similarityCount;
    }

    public double getSimilarity(PageArea a)
    {
        double size = getSizeSimilarity(a);
        double color = getColorSimilarity(a)/100; /* Color similarity has a different scale, we need to adjust it */
        double position = getDistance(a);
        double mean, meanDiff, shift = 0;
        double sum;

        if (position == 0.0)
        {
            return 0.0;
        }

        /* DOC: size similarity has lower precedence than color and position similarity
         * - to project that fact, we need to shift the size similarity factor closer
         *   to the mean value */

        mean = (size + color + position)/3;
        /* first lower importance of size similarity */
        /* DOC: we need to do this, otherwise on structured pages, it would incorrectly match some elements
         * (e.g. image with some text for one element, multiple elements listed one under another:
         * in this case it would group images instead of each image with its text)*/
        meanDiff = mean-size;
        shift = meanDiff;
        /*
        TODO: it is also possible to emphasize importance of position similarity,
              but that might give bad results
        meanDiff = mean-position;
        shift -= meanDiff/2;
        */

        sum = size + shift + color + position;

        if (sum > 3) sum = 3;
        else if (sum < 0) sum = 0;

        return sum/3;
    }

    public double getSizeSimilarity(PageArea a)
    {
        double widthRatio;
        double heightRatio;

        if (this == a) return 0;

        /* DOC: size similarity has to be counted separately for width and height and the better
         * value should be then used - if two boxes are size-similar, they will usually have the
         * same size only in one direction, the difference in the other direction one might be
         * significantly higher */

        widthRatio = (double)Math.abs(this.getWidth()-a.getWidth())/(this.getWidth()+ a.getWidth());
        heightRatio = (double)Math.abs(this.getHeight()-a.getHeight())/(this.getHeight() + a.getHeight());

        return Math.min(widthRatio, heightRatio);
    }

    public double getColorSimilarity(PageArea a)
    {
        double colorDistance;

        if (this == a) return 0;

        // TODO: fine-tune the color distance??
        // color distance: 0-100
        colorDistance = colorDiff(this.getColor(), a.getColor());
        if (colorDistance < 0) colorDistance = 0;
        else if (colorDistance > 100) colorDistance = 100;

        return colorDistance;
    }

    public double getDistance(PageArea a)
    {
        int horizontalDistance;
        int verticalDistance;
        int width;
        int height;
        int top, bottom, left, right;

        if (this == a) return 0;

        /* DOC: Position distance: 0 - 1 */
        top = Math.min(this.top, a.top);
        bottom = Math.max(this.bottom, a.bottom);
        height = bottom-top;

        left = Math.min(this.left, a.left);
        right = Math.max(this.right, a.right);
        width = right-left;


        if (this.getAlignment(a) == ALIGNMENT_COLUMN)
        {
            /* DOC: this is important - if one area is smaller and doesn't
             * extend over larger's area left/right, it is considered to be
             * horizontally aligned */
            horizontalDistance = 0;
        }
        else
        {
            int ll, lr, rr, rl;
            ll = Math.abs(a.left-this.left);
            lr = Math.abs(a.left-this.right);
            rr = Math.abs(a.right-this.right);
            rl = Math.abs(a.right-this.left);
            horizontalDistance = Math.min(Math.min(ll, lr), Math.min(rl, rr));
            if (horizontalDistance > 0) horizontalDistance--; /* DOC: We want to get just the space between, subtract one border */
        }

        if (this.getAlignment(a) == ALIGNMENT_LINE)
        {
            /* DOC: this is important - if one area is smaller and doesn't
             * extend over larger's area top/bottom, it is considered to be
             * vertically aligned */
            verticalDistance = 0;
        }
        else
        {
            int tt, tb, bb, bt;
            tt = Math.abs(a.top-this.top);
            tb = Math.abs(a.top-this.bottom);
            bb = Math.abs(a.bottom-this.bottom);
            bt = Math.abs(a.bottom-this.top);
            verticalDistance = Math.min(Math.min(tt, tb), Math.min(bt, bb));
            if (verticalDistance > 0) verticalDistance--; /* DOC: We want to get just the space between, subtract one border */
        }


        /* DOC: this formula will be important to document */
        return Math.sqrt(verticalDistance*verticalDistance+horizontalDistance*horizontalDistance)/
               Math.sqrt(width*width+height*height);
    }

    private int getAlignment(PageArea a)
    {
        if (this.bottom >= a.top && this.top <= a.bottom) return ALIGNMENT_LINE;
        else if (this.right >= a.left && this.left <= a.right) return ALIGNMENT_COLUMN;
        else if (a.getLeft() == this.getLeft()) return ALIGNMENT_COLUMN;
        else if (a.getTop() == this.getTop()) return ALIGNMENT_LINE;
        else if (a.getRight() == this.getRight()) return ALIGNMENT_COLUMN;
        else if (a.getBottom() == this.getBottom()) return ALIGNMENT_LINE;

        else return ALIGNMENT_NONE;
    }

    public static double colorDiff(Color a, Color b)
    {
        /* DOC: computing color difference according to CIE94, threshold value is 2.3 for not noticeable color diff */
        /* DOC: in the end I am using CIE76, it turned out to be more accurate for our purpose (empirically verified), CIE94 is commented out */
        /* DOC: we will probably use higher threshold because we want also similar colors (with noticeable diff) to be merged */
        /* DOC: see http://en.wikipedia.org/wiki/Color_difference for details */
        double []lab;
        double l1, a1, b1;
        double l2, a2, b2;

        // just return some high value to indicate a border of an area
        if (a == null || b == null) return 100;

        lab = rgbToLab(a.getRed(), a.getGreen(), a.getBlue());
        l1 = lab[0]; a1 = lab[1]; b1 = lab[2];
        lab = rgbToLab(b.getRed(), b.getGreen(), b.getBlue());
        l2 = lab[0]; a2 = lab[1]; b2 = lab[2];

        return Math.sqrt(Math.pow(l2-l1, 2)+Math.pow(a2-a1, 2)+Math.pow(b2-b1, 2));

//        // This is CIE94
//        double diff;
//        double part1, part2, part3;
//        double C1, C2, deltaC;
//        double deltaL;
//        double deltaH;
//
//        deltaL = l1-l2;
//        C1 = Math.sqrt(Math.pow(a1, 2)+Math.pow(b1, 2));
//        C2 = Math.sqrt(Math.pow(a2, 2)+Math.pow(b2, 2));
//        deltaC = C1-C2;
//        deltaH = Math.sqrt(Math.pow(a1-a2, 2)+Math.pow(b1-b2, 2)-Math.pow(deltaC, 2));
//
//        part1 = deltaL;
//        part2 = deltaC/(1+0.045*C1);
//        part3 = deltaH/(1+0.015*C1);
//        diff = Math.sqrt(Math.pow((part1), 2)+Math.pow((part2), 2)+Math.pow((part3), 2));
//
//        return diff;
    }

    private static double[] rgbToLab(int R, int G, int B)
    {
        double whiteX = 0.9505;
        double whiteY = 1.0;
        double whiteZ = 1.0888;

        double r, g, b;
        r = (double)R/255;
        g = (double)G/255;
        b = (double)B/255;

        // DOC: we are assuming sRGB -> XYZ (sRGB source model sounds like the best option)
        double x, y, z;
        x = 0.4124564*r + 0.3575761*g + 0.1804375*b;
        y = 0.2126729*r + 0.7151522*g + 0.0721750*b;
        z = 0.0193339*r + 0.1191920*g + 0.9503041*b;

        // XYZ -> Lab
        double xr, yr, zr;
        double fx, fy, fz;
        xr = x/whiteX;
        yr = y/whiteY;
        zr = z/whiteZ;
        fx = (xr>0.008856)?cubeRoot(xr):computeF(xr);
        fy = (yr>0.008856)?cubeRoot(yr):computeF(yr);
        fz = (zr>0.008856)?cubeRoot(zr):computeF(zr);

        double []lab = new double [3];
        lab[0] = 116*fy-16;
        lab[1] = 500*(fx-fy);
        lab[2] = 200*(fy-fz);

        return lab;
    }

    private static double computeF(double x)
    {
        return (903.3*x+16)/116;
    }

    private static double cubeRoot(double x) {
        return Math.pow(x, 1.0/3);
    }

    public Rectangle getRectangle()
    {
        if (this.rectangle == null)
        {
            this.rectangle = new Rectangle(this.left, this.top, this.right, this.bottom);
        }

        return this.rectangle;
    }
}
