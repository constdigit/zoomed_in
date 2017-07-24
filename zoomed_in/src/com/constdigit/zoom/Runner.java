package com.constdigit.zoom;

import com.alee.extended.image.WebImage;
import com.alee.extended.progress.WebStepProgress;
import com.alee.laf.WebLookAndFeel;
import com.alee.utils.filefilter.ImageFilesFilter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
    Works with gui and threads
 */
public class Runner extends JFrame {

    private BufferedImage zoomedImage;
    private BufferedImage sourceImage;
    private DefaultFolders settings;
    private boolean isZoomingStarted;
    //GUI components
    private JButton zoom;
    private JCheckBox multithreading, splittingByThreshold;
    private JLabel sourceImageViewer, menu;
    private JPanel bar;
    private JSlider coefficientSlider;
    private WebStepProgress steps;

    private static final Color white = new Color(250, 250, 250);
    private static final Color black = new Color(30, 30, 30);

    private Runner() {
        super("Zoomed In");
        isZoomingStarted = false;
        settings = DefaultFolders.getInstance();
        settings.restoreSettings();
        sourceImage = new BufferedImage(300, 300, BufferedImage.TYPE_3BYTE_BGR);
        try {
            //sourceImage = ImageIO.read(new File("resources/picture.png"));
            sourceImage = ImageIO.read(this.getClass().getResource("/resources/picture.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //generates a list of tasks and runs required number of threads
    private void startThreads() {
        int cores = multithreading.isSelected() ? Runtime.getRuntime().availableProcessors() : 1;
        ArrayList<Magnifier> tasks = new ArrayList<>();
        ExecutorService service;

        int pos = 0;
        //each thread works with its part of the image
        //splits image on columns if it is wide image
        if (sourceImage.getWidth() > sourceImage.getHeight()) {
            //need at least 5 pixels
            while (sourceImage.getWidth() / cores < 5)
                cores--;

            service = Executors.newFixedThreadPool(cores);

            //generates tasks
            for (int i = 0; i < cores - 1; i++) {
                tasks.add(new Magnifier(sourceImage.getSubimage(pos, 0, sourceImage.getWidth() / cores, sourceImage.getHeight())));
                pos += sourceImage.getWidth() / cores;
            }
            //remainder of the division
            tasks.add(new Magnifier(sourceImage.getSubimage(pos, 0,
                    sourceImage.getWidth() - (sourceImage.getWidth() / cores) * (cores - 1), sourceImage.getHeight())));
        }
        //splits image on rows if it is high image
        else {
            //need at least 5 pixels
            while (sourceImage.getHeight() / cores < 5)
                cores--;

            service = Executors.newFixedThreadPool(cores);

            //generates tasks
            for (int i = 0; i < cores - 1; i++) {
                tasks.add(new Magnifier(sourceImage.getSubimage(0, pos, sourceImage.getWidth(), sourceImage.getHeight() / cores)));
                pos += sourceImage.getHeight() / cores;
            }
            //remainder of the division
            tasks.add(new Magnifier(sourceImage.getSubimage(0, pos,
                    sourceImage.getWidth(), sourceImage.getHeight() - (sourceImage.getHeight() / cores) * (cores - 1))));
        }

        try {
            //start threads and wait for them to complete
            List<Future<BufferedImage>> zoomedParts = service.<BufferedImage>invokeAll(tasks);
            restoreImage(zoomedParts);
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    //collects in parts an zoomed image
    private void restoreImage(List<Future<BufferedImage>> zoomedParts) {
        int pos = 0;
        BufferedImage current;
        //new size
        zoomedImage = new BufferedImage(sourceImage.getWidth() * (Magnifier.zoomCoefficient / 2),
                sourceImage.getHeight() * (Magnifier.zoomCoefficient / 2), BufferedImage.TYPE_3BYTE_BGR);

        //moves by width
        if (sourceImage.getWidth() > sourceImage.getHeight()) {
            for (Future<BufferedImage> part : zoomedParts) {
                try {
                    current = part.get();
                    for (int y = 0; y < current.getHeight(); y++)
                        for (int x = 0; x < current.getWidth(); x++)
                            zoomedImage.setRGB(x + pos, y, current.getRGB(x, y));
                    pos += current.getWidth();
                }
                catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
        }
        //moves by height
        else {
            for (Future<BufferedImage> part : zoomedParts) {
                try {
                    current = part.get();
                    for (int y = 0; y < current.getHeight(); y++)
                        for (int x = 0; x < current.getWidth(); x++)
                            zoomedImage.setRGB(x, y + pos, current.getRGB(x, y));
                    pos += current.getHeight();
                }
                catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void buildGui() {
        //set LaF
        WebLookAndFeel.install ();
        //init frame
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        getContentPane().setBackground(white);

        //keeps title and menu button
        bar = new JPanel();
        bar.setLayout(new BorderLayout(10, 10));
        bar.setPreferredSize(new Dimension(620, 100));
        bar.setBackground(black);

        //to open source image
        JPanel opener = new JPanel();
        opener.setLayout(new BorderLayout(10, 10));
        opener.setBackground(white);

        //keeps zoom coefficient chooser and button that start zooming
        JPanel zoomingOptions = new JPanel();
        zoomingOptions.setLayout(new BorderLayout(10, 10));
        zoomingOptions.setBackground(white);

        //bar components setting
        menu = new JLabel();
        menu.setIcon(new ImageIcon(this.getClass().getResource("/resources/hamburger-icon.png")));
        JLabel title = new JLabel("Zoomed In");
        title.setForeground(white);
        title.setFont(new Font("Sans-Serif", Font.PLAIN, 42));

        //opener components setting
        sourceImageViewer = new JLabel(new ImageIcon(sourceImage));
        JButton open = new JButton("OPEN");
        open.setPreferredSize(new Dimension(300, 40));
        open.setFont(new Font("Sans-Serif", Font.PLAIN, 16));

        //zoomingOptions components setting
        zoom = new JButton("ZOOM");
        zoom.setPreferredSize(new Dimension(300, 40));
        zoom.setFont(new Font("Sans-Serif", Font.PLAIN, 16));
        zoom.setEnabled(false);
        JLabel sliderTip = new JLabel("Select zoom coefficient:");
        sliderTip.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        coefficientSlider = new JSlider(JSlider.HORIZONTAL, 4, 16, 4);
        coefficientSlider.setMajorTickSpacing(4);
        coefficientSlider.setSnapToTicks(true);
        coefficientSlider.setPaintTicks(true);
        coefficientSlider.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        //create the label table
        Hashtable labelTable = new Hashtable();
        labelTable.put(4, new JLabel("x4") );
        labelTable.put(8, new JLabel("x8") );
        labelTable.put(12, new JLabel("x12") );
        labelTable.put( 16, new JLabel("x16") );
        coefficientSlider.setLabelTable( labelTable );
        coefficientSlider.setPaintLabels(true);
        JPanel sliderPanel = new JPanel(new BorderLayout(10, 10));
        sliderPanel.setBackground(white);
        sliderPanel.add(BorderLayout.PAGE_START, sliderTip);
        sliderPanel.add(BorderLayout.PAGE_END, coefficientSlider);
        //check boxes
        JLabel checkBoxesTip = new JLabel("Uncheck this if you have some artifacts:");
        checkBoxesTip.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        multithreading = new JCheckBox("Use multithreading");
        multithreading.setSelected(true);
        multithreading.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        splittingByThreshold = new JCheckBox("Use splitting by threshold");
        splittingByThreshold.setSelected(true);
        splittingByThreshold.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        JPanel checkBoxesPanel = new JPanel();
        checkBoxesPanel.setBackground(white);
        checkBoxesPanel.setLayout(new BoxLayout(checkBoxesPanel, BoxLayout.Y_AXIS));
        checkBoxesPanel.add( checkBoxesTip);
        checkBoxesPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        checkBoxesPanel.add( multithreading);
        checkBoxesPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        checkBoxesPanel.add( splittingByThreshold);

        //stepper settings
        steps = new WebStepProgress();
        steps.addSteps(new WebImage(this.getClass().getResource("/resources/folder-icon.png")));
        steps.addSteps(new WebImage(this.getClass().getResource("/resources/magnifier-icon.png")));
        steps.addSteps(new WebImage(this.getClass().getResource("/resources/done-icon.png")));
        steps.setLabelsPosition(SwingConstants.BOTTOM);
        steps.setSelectionEnabled(false);
        steps.setBackground(white);

        //adding all on frame
        bar.add(BorderLayout.LINE_START, menu);
        bar.add(BorderLayout.CENTER, title);
        opener.add(BorderLayout.PAGE_END, open);
        opener.add(BorderLayout.PAGE_START, sourceImageViewer);
        zoomingOptions.add(BorderLayout.PAGE_START, sliderPanel);
        zoomingOptions.add(BorderLayout.CENTER, checkBoxesPanel);
        zoomingOptions.add(BorderLayout.PAGE_END, zoom);
        add(BorderLayout.PAGE_START, bar);
        add(BorderLayout.LINE_START, opener);
        add(BorderLayout.LINE_END, zoomingOptions);
        add(BorderLayout.PAGE_END, steps);
        pack();
        setVisible(true);

        menu.addMouseListener(new menuButtonListener());
        open.addActionListener(new openButtonListener());
        zoom.addActionListener(new zoomButtonListener());
    }

    private void putImageOnFrame() {

        sourceImageViewer.setSize(300, 300);
        //adjust size
        Image dimg = sourceImage;
        int w = sourceImage.getWidth(), h = sourceImage.getHeight();
        if (w > sourceImageViewer.getWidth()) {
            double imgWidth = sourceImage.getWidth();
            double lblWidth = sourceImageViewer.getWidth();
            h = (int)((lblWidth / imgWidth) * (double) sourceImage.getHeight());
            dimg = sourceImage.getScaledInstance(sourceImageViewer.getWidth(), h, Image.SCALE_SMOOTH);
        }
        if (h > sourceImageViewer.getHeight()) {
            double imgHeight = sourceImage.getHeight();
            double lblHeight = sourceImageViewer.getHeight();
            w = (int)((lblHeight / imgHeight) * (double) sourceImage.getWidth());
            dimg = sourceImage.getScaledInstance(w, sourceImageViewer.getHeight(), Image.SCALE_SMOOTH);
        }
        sourceImageViewer.setIcon(new ImageIcon(dimg));
        zoom.setEnabled(true);
        //go to step 2
        steps.setSelectedStepIndex(1);
        revalidate();
    }

    private void saveAndShowZoomedImage() {
        JFileChooser saver = new JFileChooser(settings.getDefaultSaveFolder());
        int option = saver.showSaveDialog(Runner.this);
        File dist = saver.getSelectedFile();
        //try to save
        if (option == JFileChooser.APPROVE_OPTION && dist != null) {
            try {
                dist = new File(dist.toString() + ".png");
                ImageIO.write(zoomedImage, "png", dist);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        else
            return;

        //show saved image
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.OPEN)) {
            try {
                desktop.open(dist);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class menuButtonListener implements MouseListener {
        @Override
        public void mouseEntered(MouseEvent mouseEvent) {
            menu.setIcon(new ImageIcon(this.getClass().getResource("/resources/selected-hamburger-icon.png")));
            revalidate();
        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            menu.setIcon(new ImageIcon(this.getClass().getResource("/resources/hamburger-icon.png")));
            revalidate();
        }

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            generateMenu();
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            generateMenu();
        }

        private void generateMenu() {
            JPopupMenu popupMenu = new JPopupMenu();
            ActionListener actionListener = new menuItemsListener();

            JMenuItem back = new JMenuItem(new ImageIcon(this.getClass().getResource("/resources/back-arrow.png")));
            back.setPreferredSize(new Dimension(200, 90));
            popupMenu.add(back);
            popupMenu.addSeparator();

            JMenuItem item = new JMenuItem("Open from URL");
            item.setFont(new Font("Sans-Serif", Font.PLAIN, 22));
            item.setPreferredSize(new Dimension(300, 85));
            item.addActionListener(actionListener);
            popupMenu.add(item);

            item = new JMenuItem("Default open folder");
            item.setFont(new Font("Sans-Serif", Font.PLAIN, 22));
            item.setPreferredSize(new Dimension(300, 85));
            item.addActionListener(actionListener);
            popupMenu.add(item);

            item = new JMenuItem("Default save folder");
            item.setFont(new Font("Sans-Serif", Font.PLAIN, 22));
            item.setPreferredSize(new Dimension(300, 85));
            item.addActionListener(actionListener);
            popupMenu.add(item);

            item = new JMenuItem("About");
            item.setFont(new Font("Sans-Serif", Font.PLAIN, 22));
            item.setPreferredSize(new Dimension(300, 85));
            item.addActionListener(actionListener);
            popupMenu.add(item);

            item = new JMenuItem("Exit");
            item.setFont(new Font("Sans-Serif", Font.PLAIN, 22));
            item.setPreferredSize(new Dimension(300, 85));
            item.addActionListener(actionListener);
            popupMenu.add(item);

            popupMenu.show(bar, 0, 0);
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            //action is not required
        }
    }

    private class menuItemsListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            switch (actionEvent.getActionCommand()) {
                case "Open from URL" :  openFromUrl(); break;
                case "Default open folder" : setDefaultOpenFolder(); break;
                case "Default save folder" : setDefaultSaveFolder(); break;
                case "About" : showAbout(); break;
                case "Exit" : System.exit(0);
            }
        }

        private void openFromUrl() {
            //JOptionPane inputUrl = new JOptionPane("Open from URL", JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, new ImageIcon("resources/url-icon.png"));
            String url = (String) JOptionPane.showInputDialog(Runner.this, "URL:", "Open from URL",
                    JOptionPane.PLAIN_MESSAGE, new ImageIcon(this.getClass().getResource("/resources/url-icon.png")), null, null);
            try {
                sourceImage = ImageIO.read(new URL(url));
            }
            catch (IOException ex) {
                ex.printStackTrace();
                return;
            }

            putImageOnFrame();
        }

        private void setDefaultOpenFolder() {
            JFileChooser directoryChooser = new JFileChooser();
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            directoryChooser.setApproveButtonText("Select");
            if (directoryChooser.showOpenDialog(Runner.this) != JFileChooser.APPROVE_OPTION) return;
            settings.setDefaultOpenFolder(directoryChooser.getSelectedFile());
            settings.saveSettings();
        }

        private void setDefaultSaveFolder() {
            JFileChooser directoryChooser = new JFileChooser();
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            directoryChooser.setApproveButtonText("Select");
            if (directoryChooser.showOpenDialog(Runner.this) != JFileChooser.APPROVE_OPTION) return;
            settings.setDefaultSaveFolder(directoryChooser.getSelectedFile());
            settings.saveSettings();
        }

        private void showAbout() {
            JFrame about = new JFrame("About");
            about.add(new JLabel(new ImageIcon(this.getClass().getResource("/resources/about.png"))));
            about.pack();
            about.setVisible(true);
        }
    }

    private class zoomButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            //get selected zoom coefficient
            Magnifier.zoomCoefficient = coefficientSlider.getValue();
            Magnifier.isSplittingEnable = splittingByThreshold.isSelected();

            //frame goes to wait
            isZoomingStarted = true;
            repaint();
            setEnabled(false);

            //all processing takes place here
            startThreads();

            //frame awakening
            setEnabled(true);
            isZoomingStarted = false;
            repaint();

            //work is done
            steps.setSelectedStepIndex(2);
            saveAndShowZoomedImage();
        }
    }

    private class openButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            //open image from file
            JFileChooser opener = new JFileChooser(settings.getDefaultOpenFolder());
            opener.setFileFilter(new ImageFilesFilter());

            //nothing selected
            if (opener.showOpenDialog(Runner.this) != JFileChooser.APPROVE_OPTION) return;
            if (opener.getSelectedFile() == null) return;

            try {
                sourceImage = ImageIO.read(opener.getSelectedFile());
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }

            if (sourceImage.getWidth() < 10 && sourceImage.getHeight() < 10) {
                JOptionPane.showMessageDialog(Runner.this, "Image should be larger than 10x10","Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            putImageOnFrame();
        }
    }

    @Override
    public void paint(Graphics g) {
        if (isZoomingStarted) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Gray it out.
            Composite urComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, .5f));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setComposite(urComposite);
        }
        else
            super.paint(g);
    }

    public static void main(String[] args) {
        Runner runner = new Runner();
        runner.buildGui();
    }
}
