package org.fit.pis.out;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.fit.pis.Output;
import org.fit.pis.PageArea;

public class TextOutput implements Output {
    private String text;

    public TextOutput(ArrayList<PageArea> areas) {
        this.text = "";

        for (PageArea area: areas) {
            this.text += area.getLeft()+","+area.getTop()+","+area.getWidth()+","+area.getHeight()+":"+area.getColor().getRGB()+"\n";
        }
    }

    @Override
    public void save(String path) {
        FileWriter fstream;
        try {
            fstream = new FileWriter(path);
            fstream.write(this.text);
            fstream.close();
        } catch (IOException e) {
        }
    }
}
