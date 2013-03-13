package org.fit.pis;

public class PageAreaRelation
{
    private PageArea a;
    private PageArea b;
    private double similarity;

    public PageAreaRelation(PageArea a, PageArea b, double similarity)
    {
        this.a = a;
        this.b = b;
        this.similarity = similarity;
    }

    public PageArea getA()
    {
        return a;
    }

    public void setA(PageArea a)
    {
        this.a = a;
    }

    public PageArea getB()
    {
        return b;
    }

    public void setB(PageArea b)
    {
        this.b = b;
    }

    public double getSimilarity()
    {
        return similarity;
    }

    public void setSimilarity(double similarity)
    {
        this.similarity = similarity;
    }
}
