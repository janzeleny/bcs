package org.fit.pis;

import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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


public class AreaProcessor
{
    private final ArrayList<PageArea> areas;

    private final SpatialIndex areaTree;
    private final SpatialIndex groupTree;

    private final ArrayList<PageArea> groups;
    private final ArrayList<PageArea> ungrouped;

    public static final double similarityThreshold = 0.05;

    private final int pageWidth;
    private final int pageHeight;

    private final StopWatch time;

    public AreaProcessor(ArrayList<PageArea> areas, int width, int height)
    {
        Collections.sort(areas, new AreaSizeComparator());
        /* Note: we store only leaf areas */
        this.areas = new ArrayList<PageArea>();
        this.areaTree = new RTree();
        this.areaTree.init(null);

        this.groups = new ArrayList<PageArea>();
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
        ArrayList<PageArea> pool = (ArrayList<PageArea>) areas.clone();
        ArrayList<PageArea> deleteList = new ArrayList<PageArea>();

        for (PageArea area: areas) // this can't be pool, because we will modify it in the loop
        {
            // DOC: originally this has been O(n^2) algorithm but it took an extensive amount of time
            //      as an improvement, we first sort the array and then we work with two basic facts
            //      - area can't contain larger or equally large areas -> stop looking for children when the area is reached in the list
            //      - once a child is found, the area is a direct parent (due to the small->large order and the fact that the smallest possible parent is direct)
            //      - once an area has been marked as a child, it can be deleted of the pool (no more direct parents exist for one area)
            //      - all in all, this reduces the worst complexity by > 50% (50% reduction is achieved by the break condition, more via deleting identified areas)
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
                /* DOC: if a leaf node has no siblings, its parent is considered to be leaf instead */
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

    public ArrayList<PageArea> getGroups() throws Exception
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

        relations = this.getAreaGraph(areas);
        this.locateGroups(relations);

        this.ungrouped.clear();
        for (PageArea area: this.areas)
        {
            /* Test this once more to be sure */
            if (area.getParent() == null)
            {
                this.ungrouped.add(area);
            }
        }

        return this.groups;
    }

    private void locateGroups(List<PageAreaRelation> relations) throws Exception
    {
        PageArea a, b;
        PageAreaRelation relation;
        PageArea group;
        boolean overlaps;
        AreaMatch match;

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

            if (a.getSimilarity(b) > similarityThreshold)
            {
                continue;
            }

            group = new PageArea(a);
            group.addChild(a);
            group.addChild(b);

            match = new AreaMatch();
            this.groupTree.intersects(group.getRectangle(), match);
            overlaps = (match.getIds().size() > 0);

            if (overlaps)
            {
                /* DOC: note that this should never happen because we already detect
                 * overlapping when identifying each group -> both a and b should be
                 * already a part of some group. */
                a.setParent(null);
                b.setParent(null);
            }
            else
            {
                /* Now we have to add children completely */
                this.groups.add(group);
                this.locateGroup(group, relations);
                match = new AreaMatch();
                this.areaTree.intersects(group.getRectangle(), match);
                for (Integer index: match.getIds())
                {
                    group.addChild(this.areas.get(index));
                }
                /* Note that this line has to be after locateGroup,
                 * as it is depending on dimensions computed in locateGroup */
                this.groupTree.add(group.getRectangle(), this.groups.size()-1);
            }
        }
        this.time.toggle();
        System.out.println("Total lookup time: " + this.time.getTotal()/1000000 + " ms");
    }

    private void locateGroup(PageArea group, List<PageAreaRelation> relations)
    {
        int i;
        boolean found;
        boolean overlap;
        PageArea tmpArea;
        PageArea inspect;
        PageArea candidate;
        PageAreaRelation rel;
        ArrayList<PageArea> inspectionList = new ArrayList<PageArea>();
        ArrayList<PageArea> candidates = new ArrayList<PageArea>();

        for (PageArea child: group.getChildren())
        {
            inspectionList.add(child);
        }

        while (inspectionList.size() > 0)
        {
            inspect = inspectionList.get(0);
            inspectionList.remove(0);

            /* DOC: First get all candidates from the relation list
             * (i.e. all edges that lead to the "inspect" vertex in the graph)
             */
            candidates.clear();
            for (i = 0; i < relations.size(); i++)
            {
                rel = relations.get(i);

                if (inspect == rel.getA()) candidate = rel.getB();
                else if (inspect == rel.getB()) candidate = rel.getA();
                else candidate = null;

                if (candidate != null)
                {
                    if (candidate.getParent() == null)
                    {
                        candidates.add(candidate);
                    }
                    relations.remove(i); /* Using "i" here instead of "rel" boosts perf. (6s -> 2.5s) */
                    i--; // since we removed the relation, we need to scan the one that took its place
                }
            }

            /* DOC: Now figure out which candidates actually qualify to be part of the group */
            for (i = 0; i < candidates.size(); i++)
            {
                candidate = candidates.get(i);

                if (group.getSimilarityRecursive(candidate) < similarityThreshold)
                {
                    found = false;
                    for (PageArea child: group.getChildren())
                    {
                        if (child == candidate)
                        {
                            found = true;
                            break;
                        }
                    }

                    if (!found)
                    {
                        /* We didn't find the area in children - add it to the list and also add it to the inspection list if necessary */
                        tmpArea = group.tryAdd(candidate);

                        AreaMatch match = new AreaMatch();
                        this.groupTree.intersects(tmpArea.getRectangle(), match);
                        overlap = (match.getIds().size() > 0);

                        if (!overlap)
                        {
                            /* We don't have to use this.addChildToGroup() because this group
                             * is not yet in the list (will be added just after locateGroup() finishes) */
                            group.addChild(candidate);
                            for (PageArea in: inspectionList)
                            {
                                if (in == candidate)
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            /* We want to avoid adding the node to inspection */
                            found = true;
                        }
                    }

                    if (!found)
                    {
                        /* Schedule for inspection */
                        inspectionList.add(candidate);
                    }
                }
            }
        }
    }

    private ArrayList<PageAreaRelation> getAreaGraph(List<PageArea> areas)
    {
        ArrayList<PageAreaRelation> relations = new ArrayList<PageAreaRelation>();
        PageAreaRelation rel;
        PageArea a, b;
        Rectangle selector;
        AreaMatch match;
        double similarity;

        /* DOC: the graph will only contain those relations that are relevant
         * - for each box on the page, that means only relations to other boxes
         *   that are either on the same line as that box or in the same column
         *   as that box
         * - the same line: if box A kept the height but was as wide as the page,
         *   all boxes that intersect with it are on the same line
         * - in the same column: corresponding to the line definition
         * - note that this approach improves performance significantly
         *   (on idnes.cz it brought reduction of locateGroup() time from 77s to 6s)
         */

        for (int i = 0; i < areas.size(); i++)
        {
            a = areas.get(i);
            /* DOC: we scan only to right and down, as top and left directions
             * will be scanned by boxes closer to top-left corner */

            /* First go right */
            selector = new Rectangle(a.getLeft(), a.getTop(), this.pageWidth, a.getBottom());
            match = new AreaMatch();
            this.areaTree.intersects(selector, match);
            for (Integer index: match.getIds())
            {
                b = areas.get(index);
                if (a == b) continue;
                similarity = a.getDistance(b);
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
                similarity = a.getDistance(b);
                rel = new PageAreaRelation(a, b, similarity);
                relations.add(rel);
            }
        }

        Collections.sort(relations, new AreaSimilarityComparator());

        return relations;
    }

    private void addChildToGroup(PageArea group, PageArea mergedArea)
    {
        int index;
        Rectangle rectangle;

        index = this.groups.indexOf(group);
        rectangle = group.getRectangle();
        this.groupTree.delete(rectangle, index);

        group.addChild(mergedArea);

        this.groupTree.add(group.getRectangle(), index);
    }

    public ArrayList<PageArea> getUngrouped()
    {
        return this.ungrouped;
    }
}
