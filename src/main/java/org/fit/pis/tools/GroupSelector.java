package org.fit.pis.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

class ImageCanvas extends JPanel implements MouseListener, MouseMotionListener {
    private static final int MARGIN = 20;
    private static final long serialVersionUID = -5407645749183556971L;

    private int x1, x2, y1, y2;
    private int x, y, w, h;
    public boolean isNewRect = true;

    private BufferedImage image;

    private ArrayList<Rectangle> rectangles;

    public ImageCanvas(BufferedImage image) {
        this.image = image;
        rectangles = new ArrayList<>();

        addMouseListener(this);
        addMouseMotionListener(this);

        this.setFocusable(true);
        this.requestFocusInWindow();

        setPreferredSize(new Dimension(image.getWidth()+2*MARGIN, image.getHeight()+2*MARGIN));
    }

    public void addRectangle() {
        this.addRectangle(this.getLeft(), this.getTop(), this.w, this.h);
    }

    public void addRectangle(int x, int y, int w, int h) {
        if (x < 0) {
            w += x;
            x = 0;
        }

        if (y < 0) {
            h += y;
            y = 0;
        }

        if (w > this.image.getWidth()) {
            w = this.image.getWidth();
        }

        if (h > this.image.getHeight()) {
            h = this.image.getHeight();
        }
        this.rectangles.add(new Rectangle(x, y, w, h));
    }

    public ArrayList<Rectangle> getRectangles() {
        return this.rectangles;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.white);
        g.fillRect(0, 0, image.getWidth()+2*MARGIN, image.getHeight()+2*MARGIN);
        g.drawImage(image, MARGIN, MARGIN, null);

//        setPreferredSize(new Dimension(image.getWidth()+2*MARGIN, image.getHeight()+2*MARGIN));
        revalidate();
    }

    @Override
    public void paint( final Graphics g ) {
        super.paint( g ); // clear the frame surface

        int width = this.x1 - this.x2;
        int height = this.y1 - this.y2;

        this.w = Math.abs( width );
        this.h = Math.abs( height );
        this.x = width < 0 ? this.x1
            : this.x2;
        this.y = height < 0 ? this.y1
            : this.y2;

        g.setColor(Color.orange);
        for (Rectangle rec: this.rectangles) {
            g.drawRect(rec.x+MARGIN, rec.y+MARGIN, rec.width, rec.height);
        }

        if ( !this.isNewRect ) {
            g.setColor(Color.green);
            g.drawRect(this.x+MARGIN, this.y+MARGIN, this.w, this.h);
        }

    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.x1 = e.getX()-MARGIN;
        this.y1 = e.getY()-MARGIN;
        this.isNewRect = true;
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        this.x2 = e.getX()-MARGIN;
        this.y2 = e.getY()-MARGIN;
        repaint();
    }

    @Override
    public void mouseDragged( MouseEvent e ) {
        this.x2 = e.getX()-MARGIN;
        this.y2 = e.getY()-MARGIN;

        this.isNewRect = false;

        repaint();
    }

    public int getTop() {
        return Math.min(this.y1, this.y2);
    }

    public int getLeft() {
        return Math.min(this.x1, this.x2);
    }

    public int getW() {
        return this.w;
    }

    public int getH() {
        return this.h;
    }

    @Override
    public void mouseMoved(MouseEvent arg0) {

    }

    @Override
    public void mouseClicked(MouseEvent arg0) {

    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }
}

public class GroupSelector
{
    public static GroupSelector selector;

    protected JFrame mainWindow = null;  //  @jve:decl-index=0:visual-constraint="67,17"
    protected JPanel mainPanel = null;
    protected JPanel urlPanel = null;
    protected JPanel contentPanel = null;
    protected JPanel statusPanel = null;
    protected JTextField statusText = null;
    protected JTextField urlText = null;
    protected JButton openButton = null;
    protected JButton exportButton = null;
    protected JButton addButton = null;
    protected JScrollPane contentScroll = null;
    protected JPanel contentCanvas = null;
    protected String filePath;
    JFileChooser fc;


    public void displayImage(File file)
    {
        BufferedImage image;

        try {
            this.filePath = file.getPath();
            image = ImageIO.read(file);
        } catch (IOException e1) {
            return;
        }
        contentCanvas = new ImageCanvas(image);
        contentScroll.setViewportView(contentCanvas);
        mainWindow.setSize((int)contentCanvas.getPreferredSize().getWidth()+25,800);
    }

    //===========================================================================

    /**
     * This method initializes jFrame
     *
     * @return javax.swing.JFrame
     */
    public JFrame getMainWindow()
    {
        if (mainWindow == null)
        {
            mainWindow = new JFrame();
            mainWindow.setTitle("Group Selector");
            mainWindow.setVisible(true);
            mainWindow.setBounds(new Rectangle(0, 0, 583, 251));
            mainWindow.setContentPane(getMainPanel());
            mainWindow.addWindowListener(new java.awt.event.WindowAdapter()
            {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    mainWindow.setVisible(false);
                    System.exit(0);
                }
            });
        }
        return mainWindow;
    }

    /**
     * This method initializes jContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMainPanel()
    {
        if (mainPanel == null)
        {
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridy = -1;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.gridx = -1;
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints11.weighty = 1.0;
            gridBagConstraints11.gridx = 0;
            gridBagConstraints11.weightx = 1.0;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.weightx = 1.0;
            gridBagConstraints3.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints3.gridwidth = 1;
            gridBagConstraints3.gridy = 3;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridy = 1;
            mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            mainPanel.add(getUrlPanel(), gridBagConstraints);
            mainPanel.add(getContentPanel(), gridBagConstraints11);
            mainPanel.add(getStatusPanel(), gridBagConstraints3);
        }
        return mainPanel;
    }

    /**
     * This method initializes jPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getUrlPanel()
    {
        if (urlPanel == null)
        {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.gridx = 1;
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridx = 3;
            gridBagConstraints7.insets = new java.awt.Insets(4,0,5,7);
            gridBagConstraints7.gridy = 1;
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.gridy = 1;
            gridBagConstraints6.weightx = 1.0;
            gridBagConstraints6.insets = new java.awt.Insets(0,5,0,5);
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.gridy = 1;
            gridBagConstraints5.anchor = java.awt.GridBagConstraints.CENTER;
            gridBagConstraints5.insets = new java.awt.Insets(0,6,0,0);
            gridBagConstraints5.gridx = 1;
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridy = 1;
            gridBagConstraints4.anchor = java.awt.GridBagConstraints.CENTER;
            gridBagConstraints4.insets = new java.awt.Insets(0,6,0,0);
            gridBagConstraints4.gridx = 0;
            urlPanel = new JPanel();
            urlPanel.setLayout(new GridBagLayout());
            urlPanel.add(getOpenButton(), gridBagConstraints4);
            urlPanel.add(getExportButton(), gridBagConstraints5);
            urlPanel.add(getUrlText(), gridBagConstraints6);
            urlPanel.add(getAddButton(), gridBagConstraints7);
        }
        return urlPanel;
    }

    /**
     * This method initializes jPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getContentPanel()
    {
        if (contentPanel == null)
        {
            GridLayout gridLayout1 = new GridLayout();
            gridLayout1.setRows(1);
            contentPanel = new JPanel();
            contentPanel.setLayout(gridLayout1);
            contentPanel.add(getContentScroll(), null);
        }
        return contentPanel;
    }

    /**
     * This method initializes jPanel2
     *
     * @return javax.swing.JPanel
     */
    private JPanel getStatusPanel()
    {
        if (statusPanel == null)
        {
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.insets = new java.awt.Insets(0,7,0,0);
            gridBagConstraints4.gridy = 2;
            statusPanel = new JPanel();
            statusPanel.setLayout(new GridBagLayout());
            statusPanel.add(getStatusText(), gridBagConstraints4);
        }
        return statusPanel;
    }

    /**
     * This method initializes jTextField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getStatusText()
    {
        if (statusText == null)
        {
            statusText = new JTextField();
            statusText.setEditable(false);
            statusText.setText("Application ready.");
        }
        return statusText;
    }

    /**
     * This method initializes jTextField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getUrlText()
    {
        if (urlText == null)
        {
            urlText = new JTextField();
        }
        return urlText;
    }

    /**
     * This method initializes jButton
     *
     * @return javax.swing.JButton
     */
    private JButton getAddButton()
    {
        if (addButton == null)
        {
            addButton = new JButton();
            addButton.setText("Add");
            addButton.addActionListener(new java.awt.event.ActionListener()
            {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    ImageCanvas canvas = (ImageCanvas)contentCanvas;
                    canvas.isNewRect = true;
                    canvas.repaint();
                    canvas.addRectangle();
                    statusText.setText(canvas.getLeft()+","+canvas.getTop()+","+canvas.getW()+","+canvas.getH());
                }
            });
        }
        return addButton;
    }

    /**
     * This method initializes jButton
     *
     * @return javax.swing.JButton
     */
    private JButton getExportButton()
    {
        if (exportButton == null)
        {
            exportButton = new JButton();
            exportButton.setText("Save groups");
            exportButton.addActionListener(new java.awt.event.ActionListener()
            {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    try {
                        String path;
                        int pos;

                        path = GroupSelector.this.filePath;
                        pos = path.lastIndexOf('.');
                        path = path.substring(0, pos)+"-groups.txt";

                        FileWriter fstream = new FileWriter(path);
                        ImageCanvas c = (ImageCanvas)GroupSelector.this.contentCanvas;
                        for (Rectangle rec: c.getRectangles()) {
                            fstream.write(rec.x+","+rec.y+","+rec.width+","+rec.height+"\n");
                        }
                        fstream.close();
                    } catch (IOException e1) {
                        return;
                    }
                }
            });
        }
        return exportButton;
    }

    /**
     * This method initializes jButton
     *
     * @return javax.swing.JButton
     */
    private JButton getOpenButton()
    {
        if (openButton == null)
        {
            fc = new JFileChooser();
            openButton = new JButton();
            openButton.setText("Open ...");
            openButton.addActionListener(new java.awt.event.ActionListener()
            {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    //In response to a button click:
                    int returnVal = fc.showOpenDialog(mainPanel);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        statusText.setText("Open: "+file.getPath()+"\n");
                        displayImage(file);
                    }
                }
            });
        }
        return openButton;
    }

    /**
     * This method initializes jScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getContentScroll()
    {
        if (contentScroll == null)
        {
            contentScroll = new JScrollPane();
            contentScroll.setViewportView(getContentCanvas());
            contentScroll.setAutoscrolls(true);
            contentScroll.getVerticalScrollBar().setUnitIncrement(20);
            contentScroll.getHorizontalScrollBar().setUnitIncrement(20);
            contentScroll.addComponentListener(new java.awt.event.ComponentAdapter()
            {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e)
                {
                    if (contentCanvas != null && contentCanvas instanceof ImageCanvas)
                    {
                        contentScroll.repaint();
                    }
                }
            });

        }
        return contentScroll;
    }

    /**
     * This method initializes jPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getContentCanvas()
    {
        if (contentCanvas == null)
        {
            contentCanvas = new JPanel();
        }
        return contentCanvas;
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        selector = new GroupSelector();
        JFrame main = selector.getMainWindow();
        main.setSize(1200,800);
        main.setVisible(true);
    }
}
