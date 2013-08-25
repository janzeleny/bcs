package org.fit.pis;

import gnu.trove.TIntProcedure;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.rtree.RTree;

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
    private static final boolean DEBUG = true;
    private final ArrayList<PageArea> areas;

    private final SpatialIndex areaTree;
    private final SpatialIndex groupTree;

    private final HashSet<PageArea> groups;
    private final ArrayList<PageArea> ungrouped;

    public static final double similarityThreshold = 0.2;

    private final int pageWidth;
    private final int pageHeight;

    private final StopWatch time;

    private BufferedWriter log;

    public AreaProcessor2(ArrayList<PageArea> areas, int width, int height) throws IOException
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
        ArrayList<PageArea> pool = new ArrayList<PageArea>();
        ArrayList<PageArea> deleteList = new ArrayList<PageArea>();

        pool.addAll(areas);
        Collections.sort(pool, new AreaSizeComparator());
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
        this.areas.clear();
        for (PageArea a: areas)
        {
            if (a.getChildren().size() == 0)
            {
                a.setParent(null);
                a.getChildren().clear();
//                if (a.getTop() < 180)
//                    System.out.println("areas.add(new PageArea(Color.black,"+a.getLeft()+","+a.getTop()+","+a.getRight()+","+a.getBottom()+"));");
                this.areas.add(a);
                this.areaTree.add(a.getRectangle(), this.areas.size()-1);
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
        FileWriter fstream;

        if (DEBUG) fstream = new FileWriter("/home/greengo/out.txt");
        else fstream = new FileWriter("/dev/null");
        this.log = new BufferedWriter(fstream);

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
        this.log.close();

        return ret;
    }

    private void locateGroups(ArrayList<PageAreaRelation> relations) throws Exception
    {
        PageArea a, b;
        int v1, v2, vsum, groupCnt;
        PageAreaRelation relation;
        PageArea group;
        boolean area_overlap;
        AreaMatch match;
        double threshold;
        double similarity;
        int relCnt = relations.size();
        ArrayList<PageAreaRelation> mtRelations = new ArrayList<PageAreaRelation>();
        ArrayList<PageArea> mergeCandidates = new ArrayList<PageArea>();
        boolean mergeTest;

        this.time.toggle();
        while (relations.size() > 0)
        {
            do {
                relation = relations.get(0);
                relations.remove(0);
                a = relation.getA();
                b = relation.getB();
            } while (relations.size() > 0 && (a.getParent() != null || b.getParent() != null));

            if (relations.size() == 0 && a.getParent() == null && b.getParent() == null) break;

            this.log.write("Picked "+relation.toString()+"\n");

            v1 = this.getAreaCount(a);
            v2 = this.getAreaCount(b);
            vsum = v1 + v2;
            groupCnt = (a.getChildren().size()==0?0:1)+(b.getChildren().size()==0?0:1);

            /* DOC: see graph of d depending on V2, there is a logarithmic dependency */
//            threshold = similarityThreshold/(Math.log10(v1+v2)+1);
            threshold = similarityThreshold;
            similarity = relation.getSimilarity();
            mergeTest = this.mergeTest(relation);
            if (similarity > threshold || !mergeTest)
            {
                if (similarity <= threshold && !mergeTest)
                {
                    this.log.write("Merge attempt failed\n");
                    mtRelations.add(relation);
                }
                else if (similarity >= threshold)
                {
                    this.log.write("Similarity comparison failed: "+similarity+" >= "+threshold+"\n");
                }
                if (relations.size() == 0 && mtRelations.size() < relCnt)
                {
                    relations.addAll(mtRelations);
                    relCnt = relations.size();
                    mtRelations.clear();
                }
                continue;
            }

            group = this.mergeAreas(a, b, relation);
            mergeCandidates.clear();
            this.log.write("Group: "+group.getTop()+"-"+group.getLeft()+"("+group.getWidth()+"x"+group.getHeight()+") - ("+v1+", "+v2+")\n");

            match = new AreaMatch();
            this.groupTree.intersects(group.getRectangle(), match);
            /* It will always overlap with the two areas already in the group */
            if (match.getIds().size() > groupCnt)
            {
                this.reclaim(a);
                this.reclaim(b);
                this.returnChildren(group);
                continue;
            }

            do {
                match = new AreaMatch();
                this.areaTree.intersects(group.getRectangle(), match);
                /* It will always overlap with the two areas already in the group */
                area_overlap = (match.getIds().size() > vsum);

                if (area_overlap)
                {
                    this.log.write("overlap = true; vsum = "+vsum+"; matches = "+match.getIds().size()+"\n");
                    /* First try to include all those overlapping areas in the group */
                    if (!this.growGroup(group, match.getIds(), mergeCandidates))
                    {
                        this.log.write("group grow failed\n");
                        this.reclaim(a);
                        this.reclaim(b);
                        this.returnChildren(group);
                        break;
                    }
                    else
                    {
                        vsum = group.getChildren().size()+mergeCandidates.size();
                        this.log.write("updated vsum: " + vsum+"\n");
                    }
                }
                else
                {
                    this.log.write("overlap = false; vsum = "+vsum+"; matches = "+match.getIds().size()+"\n");
                    if (mergeCandidates.size() > 0)
                    {
                        /* The group can't be expanded more by overlapping children,
                         * try to merge those areas that might be somewhere in between them */
                        this.log.write("trying to merge group "+group.toString()+" and "+mergeCandidates.size()+" candidates\n");
                        if (!this.tryMerge(group, mergeCandidates))
                        {
                            this.log.write("merging failed\n");
                            this.reclaim(a);
                            this.reclaim(b);
                            this.returnChildren(group);
                            area_overlap = true; /* Need to set this for the condition below */
                            break;
                        }
                        mergeCandidates.clear();
                        area_overlap = true; /* Need to set this for the condition below */
                    }
                }
            } while (area_overlap);

            if (!area_overlap)
            {
                /* Now we have to add children completely */
                this.log.write("Final Group: "+group.getTop()+"-"+group.getLeft()+"("+group.getWidth()+"x"+group.getHeight()+")\n");
                this.transferNeighbors(a, b, group);
                this.transferRelations(a, b, group, relations);
                this.groups.remove(a);
                this.groups.remove(b);
                this.groups.add(group);
                this.groupTree.delete(a.getRectangle(), 0);
                this.groupTree.delete(b.getRectangle(), 0);
                this.groupTree.add(group.getRectangle(), 0);
            }

            if (relations.size() == 0 && mtRelations.size() < relCnt)
            {
                relations.addAll(mtRelations);
                relCnt = relations.size();
                mtRelations.clear();
            }
        }
        this.time.toggle();
        System.out.println("Total lookup time: " + this.time.getTotal()/1000000 + " ms");
    }

    private boolean growGroup(PageArea group, ArrayList<Integer> matches, ArrayList<PageArea> mergeCandidates) throws IOException
    {
        boolean merged = true;
        PageArea area;
        ArrayList<PageArea> areas = new ArrayList<PageArea>();
        for (Integer i: matches)
        {
            areas.add(this.areas.get(i));
        }
        Collections.sort(areas, new AreaSizeComparator());
        Collections.reverse(areas);

        /* At the first pass, allow growing group only for areas that are
         * actually bordering/overlapping with different areas in the group */

        /* First identify that all the areas are either in the group or are
         * matching the condition above (overlap/borderline with other areas in the group) */
        while (merged)
        {
            merged = false;
            for (int i = 0 ; i < areas.size() ; i++)
            {
                area = areas.get(i);
                this.log.write("area test for merge: "+area.toString());
                if (area.getParent() == group)
                {
                    this.log.write(" (already in the group)\n");
                    areas.remove(i);
                    i--;
                    continue;
                }
                else if (area.getParent() != null)
                {
                    /* This belongs to another group - that's a show stopper */
                    this.log.write(" (belongs to another group)\n");
                    return false;
                }
                else
                {
                    for (PageArea child: group.getChildren())
                    {
                        if (area.overlaps(child))
                        {
                            merged = true;
                            group.addChild(area);
                            this.log.write(" (merged - overlap)\n");
                            break;
                        }
                    }

                    if (merged == true)
                    {
                        areas.remove(i);
                        i--;
                        break;
                    }
                    else
                    {
                        if (!mergeCandidates.contains(area))
                        {
                            mergeCandidates.add(area);
                        }
                        this.log.write(" (not merged)\n");
                    }
                }
            }
        }

        return true;
    }

    private boolean tryMerge(PageArea group, ArrayList<PageArea> areas) throws IOException
    {
        PageArea tmpArea;
        PageArea mark;
        PageArea tmpGroup = new PageArea(group);
        AreaMatch match;
        int matchCnt = 0;
        int candidateCnt = areas.size();
        boolean merge;
        ArrayList<PageArea> mergeList = new ArrayList<PageArea>();

        for (PageArea area: areas)
        {
            this.log.write("candidate: "+area.toString());
            mark = new PageArea(tmpGroup);
            tmpGroup.addChild(area, true);
            if (group.contains(tmpGroup))
            {
                /* The new area doesn't make the group expand - it can be added */
                this.log.write(" is within the group\n");
                group.addChild(area);
                candidateCnt--;
            }
            else
            {
                match = new AreaMatch();
                tmpGroup.resetRectangle();
                this.areaTree.intersects(tmpGroup.getRectangle(), match);
                matchCnt = match.getIds().size();
                if (matchCnt > group.getChildren().size()+candidateCnt)
                {
                    merge = false;
                    for (Integer i: match.getIds())
                    {
                        tmpArea = this.areas.get(i);
                        if (group.getChildren().contains(tmpArea)) continue;
                        if (areas.contains(tmpArea)) continue;
                        if (tmpArea.getDistanceAbsolute(mark) <= 1)
                        {
                            mergeList.add(area);
                            this.log.write(" brings new adjacent box\n");
                            merge = true;
                            break;
                        }
                    }

                    if (!merge)
                    {
                        this.log.write(" would bring more boxes to the group\n");
                        return false;
                    }
                }
                else
                {
                    /* Adding the area to the group extended the group but
                     * it didn't bring in any new areas */
                    this.log.write(" expands the group but can be included\n");
                    group.addChild(area);
                    candidateCnt--;
                }
            }
        }

        for (PageArea a: mergeList)
        {
            this.log.write("merging "+a.toString()+"\n");
            group.addChild(a);
        }

        return true;
    }

    private void returnChildren(PageArea group)
    {
        for (PageArea child: group.getChildren())
        {
            /* We need to return all areas that have been added to
             * the tmpGroup (not those inherited from the original group)
             * to the pool */
            if (child.getParent() == group)
            {
                child.setParent(null);
            }
        }
    }

    private PageArea mergeAreas(PageArea a, PageArea b, PageAreaRelation rel)
    {
        PageArea group;
        int vert, horiz;

        vert = a.getVEdgeCount()+b.getVEdgeCount();
        horiz = a.getHEdgeCount()+b.getHEdgeCount();
        if (rel.getDirection() == PageAreaRelation.DIRECTION_VERTICAL)
        {
            vert += rel.getCardinality();
        }
        else
        {
            horiz += rel.getCardinality();
        }

        group = new PageArea(a);
        group.setVEdgeCount(vert);
        group.setHEdgeCount(horiz);

        this.mergeChildren(group, a);
        this.mergeChildren(group, b);

        return group;
    }

    private void mergeChildren(PageArea group, PageArea a)
    {
        if (a.getChildren().size() > 0)
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
    }

    private void reclaim(PageArea a)
    {
        if (a.getChildren().size() == 0)
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

    private int getAreaCount(PageArea a)
    {
        if (a.getChildren().size() > 0)
        {
            return a.getChildren().size();
        }
        else
        {
            return 1;
        }
    }

    private boolean mergeTest(PageAreaRelation rel)
    {
        PageArea a, b;
        int direction;
        int aShape, bShape;

        a = rel.getA();
        b = rel.getB();
        direction = rel.getDirection();

        aShape = a.getShape();
        bShape = b.getShape();

        if (direction == PageAreaRelation.DIRECTION_HORIZONTAL)
        {
            if (aShape == bShape)
            {
                if (aShape == PageArea.SHAPE_COLUMN) return mergeTestDensity(a, b, aShape);
                else return true;
            }
            else
            {
                return this.mergeTestAlignment(a, b);
            }
        }
        else
        {
            if (aShape == bShape)
            {
                if (aShape == PageArea.SHAPE_ROW) return mergeTestDensity(a, b, aShape);
                else return true;
            }
            else
            {
                return this.mergeTestAlignment(a, b);
            }
        }
    }

    private boolean mergeTestAlignment(PageArea a, PageArea b)
    {
        PageArea tmpArea;
        AreaMatch match;
        int areaCnt;

        tmpArea = new PageArea(a);
        tmpArea.addChild(b, true);
        areaCnt = this.getAreaCount(a)+this.getAreaCount(b);

        match = new AreaMatch();
        this.areaTree.intersects(tmpArea.getRectangle(), match);
        return (match.getIds().size() <= areaCnt);
    }

    private boolean mergeTestDensity(PageArea a, PageArea b, int shape)
    {
        double densA, densB;
        double ratio;
        int dimA, dimB;
        int cntA, cntB;

        if (shape == PageArea.SHAPE_ROW)
        {
            cntA = a.getVEdgeCount();
            dimA = a.getHeight();
            cntB = b.getVEdgeCount();
            dimB = b.getHeight();
        }
        else if (shape == PageArea.SHAPE_COLUMN)
        {
            cntA = a.getHEdgeCount();
            dimA = a.getWidth();
            cntB = b.getHEdgeCount();
            dimB = b.getWidth();
        }
        else
        {
            return false;
        }

        densA = (double)cntA / dimA;
        densB = (double)cntB / dimB;

        ratio = Math.min(densA, densB)/Math.max(densA, densB);
//        if (ratio <= (double)1/3) return true;
        if (ratio <= 0.5) return true;
        else return false;
    }

    private void transferRelations(PageArea oldGroup1, PageArea oldGroup2, PageArea newGroup, List<PageAreaRelation> relations) throws IOException
    {
        int i;
        PageAreaRelation rel;
        PageAreaRelation bestRel;
        PageArea candidate;
        HashMap<PageArea, PageAreaRelation> tmpRelations = new HashMap<PageArea, PageAreaRelation>();
        HashSet<PageArea> merged = new HashSet<PageArea>();

        merged.add(oldGroup1);
        merged.add(oldGroup2);
        for (PageArea child: newGroup.getChildren())
        {
            merged.add(child);
        }

        for (i = 0; i < relations.size(); i++)
        {
            rel = relations.get(i);

            if (merged.contains(rel.getA())) candidate = rel.getB();
            else if (merged.contains(rel.getB())) candidate = rel.getA();
            else candidate = null;

            if (candidate != null)
            {
                if (merged.contains(candidate))
                {
                    /* This is a corner case that both endpoints
                     * of the relation are in the new group */
                    // TODO: do some recalculations here like H/V edge count
                    this.log.write("relation within a group, removing: "+rel.toString()+"\n");
                    relations.remove(i); /* Using "i" here instead of "rel" boosts perf. (6s -> 2.5s) */
                    i--; // since we removed the relation, we need to scan the one that took its place
                    continue;
                }

                /* Again, just in case ... */
                if (candidate.getParent() != null)
                {
                    this.log.write("parent is not null\n");
                    if (candidate.getParent() == newGroup)
                    {
                        this.log.write("parent is the new group\n");
                        if (rel.getDirection() == PageAreaRelation.DIRECTION_HORIZONTAL)
                        {
                            newGroup.addHEdgeCount(rel.getCardinality());
                        }
                        else
                        {
                            newGroup.addVEdgeCount(rel.getCardinality());
                        }
                        continue;
                    }
                    else
                    {
                        candidate = candidate.getParent();
                    }
                }

                if (tmpRelations.containsKey(candidate))
                {
                    bestRel = tmpRelations.get(candidate);
                    bestRel.addCardinality(rel.getCardinality());
                    bestRel.addSimilarity(rel.getSimilarity());
                }
                else
                {
                    bestRel = new PageAreaRelation(newGroup, candidate, rel.getSimilarity(), rel.getDirection());
                    bestRel.setCardinality(rel.getCardinality());
                    tmpRelations.put(candidate, bestRel);
                }
                this.log.write("remove "+rel.toString()+"\n");
                relations.remove(i); /* Using "i" here instead of "rel" boosts perf. (6s -> 2.5s) */
                i--; // since we removed the relation, we need to scan the one that took its place
            }
        }

        for (Map.Entry<PageArea, PageAreaRelation> entry : tmpRelations.entrySet())
        {
            rel = entry.getValue();
            rel.setSimilarity(rel.getSimilarity()/rel.getCardinality());
            relations.add(rel);
        }


        Collections.sort(relations, new RelationComparator());
    }

    private void transferNeighbors(PageArea oldGroup1, PageArea oldGroup2, PageArea newGroup)
    {
        PageArea area;
        PageAreaRelation rel;
        ArrayList<PageArea> delList = new ArrayList<PageArea>();
        HashMap<PageArea, Integer> recalc = new HashMap<PageArea, Integer>();
        /* children is a hash tab giving information about
         * which areas are children of the group
         * (those should not be added as neighBors of the new group)
         */
        HashMap<PageArea, Integer> children = new HashMap<PageArea, Integer>();
        for (PageArea child: newGroup.getChildren())
        {
            children.put(child, 0);
        }
        children.put(oldGroup1, 0);
        children.put(oldGroup2, 0);

        for (PageArea a: newGroup.getChildren())
        {
            delList.clear();
            /* We can also inspect children of the merged groups - they don't have any neighbors */
            for (Map.Entry<PageArea, PageAreaRelation> entry : a.getNeighbors().entrySet())
            {
                area = entry.getKey();
                rel = entry.getValue();
                delList.add(area);
                recalc.put(area, 0);
                if (!children.containsKey(area))
                {
                    newGroup.addNeighbor(area, rel.getDirection(), rel.getCardinality());
                }
            }

            for (PageArea del: delList)
            {
                del.delNeighbor(a);
            }
        }


        newGroup.calculateNeighborDistances();
        for (PageArea a: recalc.keySet())
        {
            a.calculateNeighborDistances();
        }
    }


    private ArrayList<PageAreaRelation> getAreaGraph(List<PageArea> areas)
    {
        ArrayList<PageAreaRelation> relations = new ArrayList<PageAreaRelation>();
        ArrayList<PageAreaRelation> tmpRelations = new ArrayList<PageAreaRelation>();
        int edge;
        PageArea a, b;
        Rectangle selector;
        double similarity;


        for (int i = 0; i < areas.size(); i++)
        {
            a = areas.get(i);
            /* First go right */
            /* DOC: the a.right+1 is for optimization, originally it was a.left */
            selector = new Rectangle(a.getRight()+1, a.getTop(), this.pageWidth, a.getBottom());
            tmpRelations = this.findRelations(tmpRelations, a, selector, PageAreaRelation.DIRECTION_HORIZONTAL);
            this.processRelations(tmpRelations, relations, true);

            /* Now go down */
            /* DOC: the a.bottom+1 is for optimization, originally it was a.top */
            selector = new Rectangle(a.getLeft(), a.getBottom()+1, a.getRight(), this.pageHeight);
            tmpRelations = this.findRelations(tmpRelations, a, selector, PageAreaRelation.DIRECTION_VERTICAL);
            this.processRelations(tmpRelations, relations, true);

            /* DOC: Now just to be sure, go up and left, but don't add those into the global list, as we already have them */
            /* First left */
            edge = (a.getLeft()>0)?(a.getLeft()-1):0;
            selector = new Rectangle(0, a.getTop(), edge, a.getBottom());
            tmpRelations = this.findRelations(tmpRelations, a, selector, PageAreaRelation.DIRECTION_HORIZONTAL);
            this.processRelations(tmpRelations, relations, false);

            /* And finally up */
            edge = (a.getTop()>0)?(a.getTop()-1):0;
            selector = new Rectangle(a.getLeft(), 0, a.getRight(), edge);
            tmpRelations = this.findRelations(tmpRelations, a, selector, PageAreaRelation.DIRECTION_VERTICAL);
            this.processRelations(tmpRelations, relations, false);
        }

        for (PageArea area: areas)
        {
            area.calculateNeighborDistances();
        }

        /* DOC: we need to compute distance now because we didn't know
         * all the absolute distances before
         */
        for (PageAreaRelation rel: relations)
        {
            a = rel.getA();
            b = rel.getB();
            similarity = a.getSimilarity(b);
            rel.setSimilarity(similarity);
        }

        Collections.sort(relations, new RelationComparator());

        /* DOC: we need to compute distance now because we didn't know
         * all the absolute distances before
         */
//        for (PageAreaRelation rel: relations)
//        {
//            a = rel.getA();
//            b = rel.getB();
//            similarity = a.getSimilarity(b);
//            rel.setSimilarity(similarity);
//        }

        return relations;
    }

    private ArrayList<PageAreaRelation> findRelations(ArrayList<PageAreaRelation> relations, PageArea area, Rectangle selector, int direction)
    {
        AreaMatch match;
        PageArea b;
        PageAreaRelation rel;
        ArrayList<PageAreaRelation> tmpRelations = new ArrayList<PageAreaRelation>();

        match = new AreaMatch();
        this.areaTree.intersects(selector, match);
        for (Integer index: match.getIds())
        {
            b = areas.get(index);
            if (area == b) continue;
            rel = new PageAreaRelation(area, b, 1.0, direction);
            rel.setAbsoluteDistance(area.getDistanceAbsolute(b));
            tmpRelations.add(rel);
        }

        return tmpRelations;
    }

    private void processRelations(ArrayList<PageAreaRelation> batch, ArrayList<PageAreaRelation> all, boolean append)
    {
        double distMark;

        if (batch.size() > 0)
        {
            Collections.sort(batch, new AreaProximityComparator());
            /* DOC: more boxes can have the same distance */
            distMark = batch.get(0).getAbsoluteDistance();
            for (PageAreaRelation r: batch)
            {
                if (r.getAbsoluteDistance() <= distMark)
                {
                    r.getA().addNeighbor(r);
                }
                else
                {
                    break;
                }
            }

            if (append) all.addAll(batch);
            batch.clear();
        }
    }

    public ArrayList<PageArea> getUngrouped()
    {
        return this.ungrouped;
    }
}
