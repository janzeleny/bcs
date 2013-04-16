package org.fit.pis;

import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.rtree.RTree;


class AreaSizeComparator implements Comparator<PageArea> {
    @Override
    public int compare(PageArea a, PageArea b) {
        int sizeA, sizeB;

        sizeA = a.getWidth()*a.getHeight();
        sizeB = b.getWidth()*b.getHeight();
        return sizeA-sizeB;
    }
}

class AreaTopComparator implements Comparator<PageArea> {
    @Override
    public int compare(PageArea a, PageArea b) {
        return a.getTop()-b.getTop();
    }
}

class AreaSimilarityComparator implements Comparator<PageAreaRelation> {
    @Override
    public int compare(PageAreaRelation a, PageAreaRelation b) {
        double diff = a.getSimilarity() - b.getSimilarity();

        if (diff > 0) return 1;
        else if (diff < 0) return -1;
        else return 0;
    }
}

class AreaMatch implements TIntProcedure
{
    private final ArrayList<Integer> ids;

    public AreaMatch()
    {
        this.ids = new ArrayList<Integer>();
    }

    @Override
    public boolean execute(int id) {
        ids.add(id);
        return true;
    }

    public ArrayList<Integer> getIds() {
        return ids;
    }
};



public class AreaProcessor2
{
    private final ArrayList<PageArea> areas;

    private final SpatialIndex areaTree;
    private final SpatialIndex groupTree;

    private final HashSet<PageArea> groups;
    private final ArrayList<PageArea> ungrouped;

    public static final double similarityThreshold = 0.05;

    private final int pageWidth;
    private final int pageHeight;

    private final StopWatch time;

    public AreaProcessor2(ArrayList<PageArea> areas, int width, int height)
    {
        Collections.sort(areas, new AreaSizeComparator());
        /* Note: we store only leaf areas */
        this.areas = new ArrayList<PageArea>();
        this.areaTree = new RTree();
        this.areaTree.init(null);

        this.groups = new HashSet<PageArea>();
        this.groupTree = new RTree();
        this.groupTree.init(null);

        this.ungrouped = new ArrayList<PageArea>();

        this.pageHeight = width;
        this.pageWidth = height;

        this.time = new StopWatch(true);

        this.buildHierarchy(areas);
    }

    private void buildHierarchy(ArrayList<PageArea> areas)
    {
        @SuppressWarnings("unchecked")
        ArrayList<PageArea> pool = (ArrayList<PageArea>) areas.clone();
        ArrayList<PageArea> deleteList = new ArrayList<PageArea>();

        for (PageArea area: areas) // this can't be pool, because we will modify it in the loop
        {
            for (PageArea a: deleteList)
            {
                pool.remove(a);
            }
            deleteList.clear();

            for (PageArea a: pool)
            {
                if (area == a) break;
                if (!area.contains(a)) continue;

                area.addChild(a);
                deleteList.add(a);
            }
        }
        this.extractLeafAreas(areas);
    }

    public ArrayList<PageArea> getAreas()
    {
        return this.areas;
    }

    private void extractLeafAreas(ArrayList<PageArea> areas)
    {
        PageArea parent;

        for (PageArea a: areas)
        {
            if (a.getChildren() == null || a.getChildren().size() == 0)
            {
                parent = a;
                while (parent.getParent() != null && parent.getParent().getChildren().size() == 1)
                {
                    parent = parent.getParent();
                }
                parent.setParent(null);
                if (parent.getChildren() != null)
                {
                    parent.getChildren().clear();
                }
                this.areas.add(parent);
                this.areaTree.add(parent.getRectangle(), this.areas.size()-1);
            }
        }
    }

    public HashSet<PageArea> getGroups() throws Exception
    {
        if (this.groups.isEmpty())
        {
            if (!this.areas.isEmpty())
            {
                this.extractGroups(this.areas);
            }
        }

        return this.groups;
    }

    public ArrayList<PageArea> extractGroups(List<PageArea> areas) throws Exception
    {
        ArrayList<PageAreaRelation> relations;
        ArrayList<PageArea> ret = new ArrayList<PageArea>();

        relations = this.getAreaGraph(areas);
        this.locateGroups(relations);

        this.ungrouped.clear();
        for (PageArea group: groups)
        {
            ret.add(group);
        }

        for (PageArea area: this.areas)
        {
            if (area.getParent() == null)
            {
                this.ungrouped.add(area);
            }
        }


        return ret;
    }

    private void locateGroups(List<PageAreaRelation> relations) throws Exception
    {
        PageArea a, b;
        int v1, v2;
        PageAreaRelation relation;
        PageArea group;
        boolean group_overlap;
        boolean area_overlap;
        AreaMatch match;
        double threshold;

        this.time.toggle();
        while (relations.size() > 0)
        {
            do {
                relation = relations.get(0);
                relations.remove(0);
                a = relation.getA();
                b = relation.getB();
            } while (relations.size() > 0 && (a.getParent() != null || b.getParent() != null));

            if (relations.size() == 0) break;

            /* DOC: see graph of d depending on V2, there is a logarithmic dependency */
            v1 = (a.getChildren() == null || a.getChildren().size() == 0)?1:a.getChildren().size();
            v2 = (b.getChildren() == null || b.getChildren().size() == 0)?1:b.getChildren().size();
            threshold = similarityThreshold*(Math.log10(v1+v2)+1);
            if (a.getSimilarityFromGraph(b, relation.getSimilarity()) > threshold)
            {
                continue;
            }

            group = this.mergeAreas(a, b, relation.getSimilarity());

            match = new AreaMatch();
            this.groupTree.intersects(group.getRectangle(), match);
            group_overlap = (match.getIds().size() > 0);
            match = new AreaMatch();
            this.areaTree.intersects(group.getRectangle(), match);
            area_overlap = (match.getIds().size() > 2); /* It will always overlap with the two areas already in the group */

            if (group_overlap || area_overlap)
            {
                this.reclaim(a);
                this.reclaim(b);
            }
            else
            {
                /* Now we have to add children completely */
                this.transferRelations(a, b, group, relations);
                this.groups.remove(a);
                this.groups.remove(b);
                this.groups.add(group);
                this.groupTree.delete(a.getRectangle(), 0);
                this.groupTree.delete(b.getRectangle(), 0);
                this.groupTree.add(group.getRectangle(), 0);
            }
        }
        this.time.toggle();
        System.out.println("Total lookup time: " + this.time.getTotal()/1000000 + " ms");
    }

    private PageArea mergeAreas(PageArea a, PageArea b, double x)
    {
        int e, e1, e2;
        double m;
        PageArea group;

        e1 = (a.getChildren() != null)?a.getChildren().size():0;
        e2 = (b.getChildren() != null)?b.getChildren().size():0;
        e = a.getEdgeCount()+b.getEdgeCount();
        e += ((e1 > 0)?e1:1)*((e2 > 0)?e2:1);

        m = getMergedM(a, b, x);

        group = new PageArea(a);
        group.setEdgeCount(e);
        group.setMeanDistance(m);

        if (a.getChildren() != null && a.getChildren().size() > 0)
        {
            for (PageArea child: a.getChildren())
            {
                group.addChild(child);
            }
        }
        else
        {
            group.addChild(a);
        }

        if (b.getChildren() != null && b.getChildren().size() > 0)
        {
            for (PageArea child: b.getChildren())
            {
                group.addChild(child);
            }
        }
        else
        {
            group.addChild(b);
        }

        return group;
    }

    private void reclaim(PageArea a)
    {
        if (a.getChildren() == null || a.getChildren().size() == 0)
        {
            a.setParent(null);
        }
        else
        {
            for (PageArea child: a.getChildren())
            {
                child.setParent(a);
            }
        }
    }

    double getMergedM(PageArea a, PageArea b, double x)
    {
        int v1, v2, e1, e2;
        double m1, m2;

        if (a.getChildren() != null && a.getChildren().size() > 0)
        {
            v1 = a.getChildren().size();
        }
        else
        {
            v1 = 0;
        }

        if (b.getChildren() != null && b.getChildren().size() > 0)
        {
            v2 = b.getChildren().size();
        }
        else
        {
            v2 = 0;
        }

        e1 = a.getEdgeCount();
        e2 = b.getEdgeCount();
        m1 = a.getMeanDistance();
        m2 = b.getMeanDistance();

        return (e1*m1+(v1-1)*v2*m1 + v1*v2*x + v1*(v2-1)*m2+e2*m2)/(e1+v1*v2+e2);
    }

    private void transferRelations(PageArea oldGroup1, PageArea oldGroup2, PageArea newGroup, List<PageAreaRelation> relations)
    {
        int i;
        PageAreaRelation rel;
        PageArea candidate;
        HashMap<PageArea, Double> neighbours = new HashMap<PageArea, Double>();
        double val;

        for (i = 0; i < relations.size(); i++)
        {
            rel = relations.get(i);

            if (oldGroup1 == rel.getA()) candidate = rel.getB();
            else if (oldGroup1 == rel.getB()) candidate = rel.getA();
            else candidate = null;

            if (candidate == null)
            {
                if (oldGroup2 == rel.getA()) candidate = rel.getB();
                else if (oldGroup2 == rel.getB()) candidate = rel.getA();
                else continue;
            }

            if (candidate != null)
            {
                /* Just to be sure, test the candidate for both old groups
                 * (code above can change in the future) */
                if (candidate != oldGroup1 && candidate != oldGroup2)
                {
                    if (candidate.getParent() != null)
                    {
                        candidate = candidate.getParent();
                    }

                    if (neighbours.containsKey(candidate))
                    {
                        val = neighbours.get(candidate);
                    }
                    else
                    {
                        val = 100;
                    }

                    if (rel.getSimilarity() < val)
                    {
                        neighbours.put(candidate, rel.getSimilarity());
                    }
                }

                relations.remove(i); /* Using "i" here instead of "rel" boosts perf. (6s -> 2.5s) */
                i--; // since we removed the relation, we need to scan the one that took its place
            }
        }

        for (Map.Entry<PageArea, Double> entry : neighbours.entrySet())
        {
            rel = new PageAreaRelation(newGroup, entry.getKey(), entry.getValue());
            relations.add(rel);
        }

        Collections.sort(relations, new AreaSimilarityComparator());
    }

    private ArrayList<PageAreaRelation> getAreaGraph(List<PageArea> areas)
    {
        ArrayList<PageAreaRelation> relations = new ArrayList<PageAreaRelation>();
        PageAreaRelation rel;
        PageArea a, b;
        Rectangle selector;
        AreaMatch match;
        double similarity;


        for (int i = 0; i < areas.size(); i++)
        {
            a = areas.get(i);
            /* First go right */
            selector = new Rectangle(a.getLeft(), a.getTop(), this.pageWidth, a.getBottom());
            match = new AreaMatch();
            this.areaTree.intersects(selector, match);
            for (Integer index: match.getIds())
            {
                b = areas.get(index);
                if (a == b) continue;
                similarity = a.getSimilarity(b);
                if (similarity > similarityThreshold) continue;
                rel = new PageAreaRelation(a, b, similarity);
                relations.add(rel);
            }

            /* Now go down */
            selector = new Rectangle(a.getLeft(), a.getTop(), a.getRight(), this.pageHeight);
            match = new AreaMatch();
            this.areaTree.intersects(selector, match);
            for (Integer index: match.getIds())
            {
                b = areas.get(index);
                if (a == b) continue;
                similarity = a.getSimilarity(b);
                if (similarity > similarityThreshold) continue;
                rel = new PageAreaRelation(a, b, similarity);
                relations.add(rel);
            }
        }

        Collections.sort(relations, new AreaSimilarityComparator());

        return relations;
    }

    public ArrayList<PageArea> getUngrouped()
    {
        return this.ungrouped;
    }
}
