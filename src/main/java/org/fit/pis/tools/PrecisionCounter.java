package org.fit.pis.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.rtree.RTree;

import gnu.trove.TIntProcedure;

class RectangleMatch implements TIntProcedure {
    private final ArrayList<Integer> ids;

    public RectangleMatch() {
        this.ids = new ArrayList<>();
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

public class PrecisionCounter {
    private static final int PRECISION_POS = 0;
    private static final int RECALL_POS = 1;

    private static final int METRIC_F = 0;
    private static final int METRIC_ARI = 1;

    private ArrayList<Rectangle> groups;
    private ArrayList<Rectangle> boxes;
    private SpatialIndex groupTree;
    private SpatialIndex boxTree;

    private String filePrefix;

    public PrecisionCounter(String groupsFilename) {
        int pos;
        String boxesFilename;

        pos = groupsFilename.lastIndexOf("-groups.txt");
        this.filePrefix = groupsFilename.substring(0, pos);
        boxesFilename = this.filePrefix + "-boxes.txt";

        this.groups = this.parseFile(groupsFilename);
        this.groupTree = new RTree();
        this.groupTree.init(null);
        pos = 0;
        for (Rectangle rec : this.groups) {
            this.groupTree.add(rec, pos);
            pos++;
        }

        this.boxes = this.parseFile(boxesFilename);
        this.boxTree = new RTree();
        this.boxTree.init(null);
        pos = 0;
        for (Rectangle rec : this.boxes) {
            this.boxTree.add(rec, pos);
            pos++;
        }
    }

    public void calculate(int metric) {
        String filename;
        ArrayList<Rectangle> areas;
        double[] fScore;
        double score;
        double bestBcs = 0, bestVips = 0;

        for (int i = 1; i <= 10; i++) {
//            System.out.print(i+" ");
            if (i < 10) {
                filename = this.filePrefix + "-boxes-0." + i + ".txt";
            } else {
                filename = this.filePrefix + "-boxes-1.0.txt";
            }
            areas = this.parseFile(filename);
            if (metric == METRIC_F) {
                fScore = this.getScore(areas);
                score = this.fScore(fScore);
//                System.out.print("BCS: "+fScore[PRECISION_POS]+" "+fScore[RECALL_POS]+" "+score+" | ");
            } else {
                score = this.getAriScore(areas);
//                System.out.print("BCS: "+score+" | ");
            }
            if (score > bestBcs) {
                bestBcs = score;
            }


            filename = this.filePrefix + "-vips-" + i + "-boxes.txt";
            areas = this.parseFile(filename);
            if (metric == METRIC_F) {
                fScore = this.getScore(areas);
                score = this.fScore(fScore);
//                System.out.print("VIPS: "+fScore[PRECISION_POS]+" "+fScore[RECALL_POS]+" "+score);
            } else {
                score = this.getAriScore(areas);
//                System.out.print("VIPS: "+score);
            }
            if (score > bestVips) {
                bestVips = score;
            }
//            System.out.println();
        }
        System.out.println("BCS: "+bestBcs+" | VIPS: "+bestVips);
    }

    private double fScore(double [] score) {
        return 2*(score[PRECISION_POS]*score[RECALL_POS])/(score[PRECISION_POS]+score[RECALL_POS]);
    }

    private double[] getScore(ArrayList<Rectangle> areas) {
        double[] score = new double[2];
        HashSet<Rectangle> selectedBoxes;
        HashSet<Rectangle> groupBoxes;
        HashSet<Rectangle> groupsCovered;
        RectangleMatch groupMatch;
        RectangleMatch boxMatch;
        Rectangle group;
        int coveredCount, hitCount, precPairCount, recPairCount;

        precPairCount = recPairCount = 0;
        /* Precision - how many boxes selected by area are actually within
         * the group which it is compared with */
        score[PRECISION_POS] = 0.0;
        /* Recall - how many boxes from designated group does the area cover */
        score[RECALL_POS] = 0.0;

        groupBoxes = new HashSet<>();
        selectedBoxes = new HashSet<>();
        groupsCovered = new HashSet<>();

        for (Rectangle area : areas) {
            boxMatch = new RectangleMatch();
            this.boxTree.intersects(area, boxMatch);
            selectedBoxes.clear();
            for (Integer boxIndex: boxMatch.getIds()) {
                selectedBoxes.add(this.boxes.get(boxIndex));
            }

            groupMatch = new RectangleMatch();
            this.groupTree.intersects(area, groupMatch);

            for (Integer groupIndex : groupMatch.getIds()) {
                group = this.groups.get(groupIndex);
                groupsCovered.add(group);
                boxMatch = new RectangleMatch();
                this.boxTree.intersects(group, boxMatch);

                groupBoxes.clear();
                coveredCount = hitCount = 0;
                for (Integer boxIndex: boxMatch.getIds()) {
                    Rectangle box = this.boxes.get(boxIndex);
                    groupBoxes.add(box);
                    if (selectedBoxes.contains(box)) {
                        coveredCount++;
                    }
                }

                for (Rectangle box: selectedBoxes) {
                    if (groupBoxes.contains(box)) {
                        hitCount++;
                    }
                }



                if (selectedBoxes.size() == 0 || groupBoxes.size() == 0) {
                    /* Probably some weird error, this should not be happening.
                     * (e.g. user selected bogus group) */
                    continue;
                }

                if (hitCount == 0) {
                    /* Probably just a small overlap caused for example by
                     * imperfect selection of minimal bounding box by human user */
                    continue;
                }

                score[PRECISION_POS] += ((double)hitCount)/selectedBoxes.size();
                score[RECALL_POS] += ((double)coveredCount)/groupBoxes.size();
                precPairCount++;
                recPairCount++;
            }
        }

        for (Rectangle gr: this.groups) {
            if (groupsCovered.contains(gr)) {
                continue;
            }

            /* this group was not covered -> decrease recall
               (by counting recall of this group as 0) */
            recPairCount++;
        }

        score[PRECISION_POS] /= precPairCount;
        score[RECALL_POS] /= recPairCount;

        return score;
    }

    private int getIntersectionSize(Rectangle a, Rectangle b) {
        HashSet<Integer> boxesA, boxesB;
        RectangleMatch aMatch, bMatch;

        aMatch = new RectangleMatch();
        this.boxTree.intersects(a, aMatch);
        boxesA = new HashSet<>(aMatch.getIds());

        bMatch = new RectangleMatch();
        this.boxTree.intersects(b, bMatch);
        boxesB = new HashSet<>(bMatch.getIds());

        boxesA.retainAll(boxesB);

        return boxesA.size();
    }

    /**
     * Adjusted Rand Index score
     *
     * @param detectedAreas
     * @see http://www.otlet-institute.org/wikics/Clustering_Problems.html#toc-Subsection-4.1
     * @return
     */
    private double getAriScore(ArrayList<Rectangle> detectedAreas) {
        int [] detectedSums = new int[detectedAreas.size()];
        int [] selectedSums = new int[this.groups.size()];
        int sum = 0;
        double expected;
        double allComb = 0, selectedComb = 0, detectedComb = 0, sumComb = 0;
        int current;
        Rectangle selected;
        Rectangle detected;

        for (int i = 0; i < this.groups.size(); i++) {
            selected = this.groups.get(i);
            for (int j = 0; j < detectedAreas.size(); j++) {
                detected = detectedAreas.get(j);
                current = this.getIntersectionSize(selected, detected);
                selectedSums[i] += current;
                detectedSums[j] += current;
                sum += current;
                allComb += this.pairCount(current);
            }
        }

        for (int i = 0; i < selectedSums.length; i++) {
            selectedComb += this.pairCount(selectedSums[i]);
        }
        for (int i = 0; i < detectedSums.length; i++) {
            detectedComb += this.pairCount(detectedSums[i]);
        }
        sumComb = this.pairCount(sum);

        expected = selectedComb*detectedComb/sumComb;
        return (allComb-expected)/((selectedComb+detectedComb)/2-expected);
    }

    private double pairCount(int setSize) {
        return setSize*(setSize-1)/2.0;
    }

//    private double getOverlap(Rectangle a, Rectangle b) {
//        double diffX, diffY;
//
//        diffX = Math.max(0, Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX));
//        diffY = Math.max(0, Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY));
//
//        return diffX * diffY;
//    }

    private ArrayList<Rectangle> parseFile(String filename) {
        ArrayList<Rectangle> list;
        BufferedReader reader;
        String line;
        String[] lineArray;
        String[] coords;
        int x, y, w, h;

        list = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(filename));
            while ((line = reader.readLine()) != null) {
                lineArray = line.split(":");
                coords = lineArray[0].split(",");
                x = Integer.parseInt(coords[0]);
                y = Integer.parseInt(coords[1]);
                w = Integer.parseInt(coords[2]);
                h = Integer.parseInt(coords[3]);
                list.add(new Rectangle(x, y, x + w, y + h));
            }
            reader.close();
        } catch (IOException e) {
            list.clear();
        }

        return list;
    }

    public static void main(String[] args) {
        int metric;

        if (args.length < 2) {
            System.out.println("Usage: ./calculate.sh F|ARI <path-to-group-file>");
            System.exit(1);
        }

        if (args[0].toLowerCase().equals("ari")) {
            metric = METRIC_ARI;
        } else {
            metric = METRIC_F;
        }

        PrecisionCounter counter = new PrecisionCounter(args[1]);
        counter.calculate(metric);
    }
}
