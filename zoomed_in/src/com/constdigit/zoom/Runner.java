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
public class Runner {

    private BufferedImage zoomedImage;
    private BufferedImage sourceImage;
    //GUI components
    private JFrame frame;
    private JLabel sourceImageViewer;
    private JSlider coefficientSlider;
    private WebStepProgress steps;
    private JButton zoom;
    private JLabel menu;
    private JPanel bar;
    private static final Color white = new Color(250, 250, 250);
    private static final Color black = new Color(30, 30, 30);

    private Runner() {
        sourceImage = new BufferedImage(300, 300, BufferedImage.TYPE_3BYTE_BGR);
        try {
            sourceImage = ImageIO.read(new File("resources/picture.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //generates a list of tasks and runs required number of threads
    private void startThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
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
        frame = new JFrame("Zoomed In");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setResizable(false);
        frame.getContentPane().setBackground(white);

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
        menu.setIcon(new ImageIcon("resources/hamburger-icon.png"));
        JLabel title = new JLabel("Zoomed In");
        title.setForeground(white);
        title.setFont(new Font("Sans-Serif", Font.PLAIN, 42));

        //opener components setting
        sourceImageViewer = new JLabel(new ImageIcon(sourceImage));
        sourceImageViewer.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton open = new JButton("OPEN");
        open.setAlignmentX(Component.CENTER_ALIGNMENT);
        open.setPreferredSize(new Dimension(300, 40));

        //zoomingOptions components setting
        zoom = new JButton("ZOOM");
        zoom.setAlignmentX(Component.CENTER_ALIGNMENT);
        zoom.setPreferredSize(new Dimension(300, 40));
        zoom.setEnabled(false);
        JLabel hint = new JLabel("Select zoom coefficient:");
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        coefficientSlider = new JSlider(JSlider.HORIZONTAL, 4, 16, 4);
        coefficientSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
        coefficientSlider.setMajorTickSpacing(4);
        coefficientSlider.setSnapToTicks(true);
        coefficientSlider.setPaintTicks(true);
        //create the label table
        Hashtable labelTable = new Hashtable();
        labelTable.put(4, new JLabel("x4") );
        labelTable.put(8, new JLabel("x8") );
        labelTable.put(12, new JLabel("x12") );
        labelTable.put( 16, new JLabel("x16") );
        coefficientSlider.setLabelTable( labelTable );
        coefficientSlider.setPaintLabels(true);

        //stepper settings
        steps = new WebStepProgress();
        steps.addSteps(new WebImage("resources/folder-icon.png"));
        steps.addSteps(new WebImage("resources/magnifier-icon.png"));
        steps.addSteps(new WebImage("resources/done-icon.png"));
        steps.setLabelsPosition(SwingConstants.BOTTOM);
        steps.setBackground(white);

        //adding all on frame
        bar.add(BorderLayout.LINE_START, menu);
        bar.add(BorderLayout.CENTER, title);
        opener.add(BorderLayout.PAGE_END, open);
        opener.add(BorderLayout.PAGE_START, sourceImageViewer);
        zoomingOptions.add(BorderLayout.PAGE_START, hint);
        zoomingOptions.add(BorderLayout.CENTER, coefficientSlider);
        zoomingOptions.add(BorderLayout.PAGE_END, zoom);
        frame.add(BorderLayout.PAGE_START, bar);
        frame.add(BorderLayout.LINE_START, opener);
        frame.add(BorderLayout.LINE_END, zoomingOptions);
        frame.add(BorderLayout.PAGE_END, steps);
        frame.pack();
        frame.setVisible(true);

        menu.addMouseListener(new menuButtonListener());
        open.addActionListener(new openButtonListener());
        zoom.addActionListener(new zoomButtonListener());
    }

    private class menuButtonListener implements MouseListener {
        @Override
        public void mouseEntered(MouseEvent mouseEvent) {
            menu.setIcon(new ImageIcon("resources/selected-hamburger-icon.png"));
            frame.revalidate();
        }

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem back = new JMenuItem(new ImageIcon("resources/back-arrow.png"));
            back.setPreferredSize(new Dimension(200, 90));
            popupMenu.add(back);
            popupMenu.addSeparator();
            popupMenu.add(new JMenuItem("Open with URL"));
            popupMenu.add(new JMenuItem("Default open folder"));
            popupMenu.add(new JMenuItem("Default save folder"));
            popupMenu.add(new JMenuItem("About"));
            popupMenu.add(new JMenuItem("Exit"));
            popupMenu.show(bar, 0, 0);
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {

        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            menu.setIcon(new ImageIcon("resources/hamburger-icon.png"));
            frame.revalidate();
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
        }
    }

    private class zoomButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            //get selected zoom coefficient
            Magnifier.zoomCoefficient = coefficientSlider.getValue();
            startThreads();
            //work is done
            steps.setSelectedStepIndex(2);
            //show result in new frame (in future - it would be saved in file or import in cloud)
            JFrame resultImage = new JFrame("Zoomed");
            resultImage.add(BorderLayout.CENTER, new JLabel(new ImageIcon(zoomedImage)));
            resultImage.pack();
            resultImage.setVisible(true);
        }
    }

    private class openButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            JFileChooser opener = new JFileChooser();
            opener.setFileFilter(new ImageFilesFilter());
            if (opener.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
                return;
            if (opener.getSelectedFile() == null)
                return;

            try {
                sourceImage = ImageIO.read(opener.getSelectedFile());
            } catch (IOException e) {
                e.printStackTrace();
            }

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
            frame.revalidate();
        }
    }

    public static void main(String[] args) {
        Runner runner = new Runner();
        runner.buildGui();
    }
}
