package com.constdigit.zoom;

import com.alee.extended.image.WebImage;
import com.alee.extended.progress.WebStepProgress;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.slider.WebSlider;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.concurrent.*;
import java.util.List;

/*
    Works with gui and threads
 */
public class Runner {
    private BufferedImage zoomedImage;
    private BufferedImage sourceImage;
    private JFrame frame;
    private JLabel sourceImageViewer;
    private JSlider coefficientSlider;
    private static final Color white = new Color(250, 250, 250);
    private static final Color black = new Color(30, 30, 30);

    //TODO: наплодить комментов, залить на гитхаб

    Runner() {
        sourceImage = new BufferedImage(300, 300, BufferedImage.TYPE_3BYTE_BGR);
        for (int i = 0; i < 300; i++)
            for (int j = 0; j < 300; j++)
                sourceImage.setRGB(j, i, Color.darkGray.getRGB());
    }

    private void startThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        //cores = 1;
        ArrayList<Magnifier> tasks = new ArrayList<>();
        ExecutorService service;

        int pos = 0;
        if (sourceImage.getWidth() > sourceImage.getHeight()) {
            while (sourceImage.getWidth() / cores < 5)
                cores--;

            service = Executors.newFixedThreadPool(cores);

            for (int i = 0; i < cores - 1; i++) {
                tasks.add(new Magnifier(sourceImage.getSubimage(pos, 0, sourceImage.getWidth() / cores, sourceImage.getHeight())));
                pos += sourceImage.getWidth() / cores;
            }
            tasks.add(new Magnifier(sourceImage.getSubimage(pos, 0,
                    sourceImage.getWidth() - (sourceImage.getWidth() / cores) * (cores - 1), sourceImage.getHeight())));
        }
        else {
            while (sourceImage.getHeight() / cores < 5)
                cores--;

            service = Executors.newFixedThreadPool(cores);

            for (int i = 0; i < cores - 1; i++) {
                tasks.add(new Magnifier(sourceImage.getSubimage(0, pos, sourceImage.getWidth(), sourceImage.getHeight() / cores)));
                pos += sourceImage.getHeight() / cores;
            }
            tasks.add(new Magnifier(sourceImage.getSubimage(0, pos,
                    sourceImage.getWidth(), sourceImage.getHeight() - (sourceImage.getHeight() / cores) * (cores - 1))));
        }

        try {
            List<Future<BufferedImage>> zoomedParts = service.<BufferedImage>invokeAll(tasks);
            restoreImage(zoomedParts);
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void restoreImage(List<Future<BufferedImage>> zoomedParts) {
        int pos = 0;
        int[] rgbArray = null;
        BufferedImage current = null;
        zoomedImage = new BufferedImage(sourceImage.getWidth() * (Magnifier.zoomCoefficient / 2),
                sourceImage.getHeight() * (Magnifier.zoomCoefficient / 2), BufferedImage.TYPE_3BYTE_BGR);

        if (sourceImage.getWidth() > sourceImage.getHeight()) {
            for (Future<BufferedImage> part : zoomedParts) {
                try {
                    current = part.get();
                    for (int y = 0; y < current.getHeight(); y++)
                        for (int x = 0; x < current.getWidth(); x++)
                            zoomedImage.setRGB(x + pos, y, current.getRGB(x, y));
                    //rgbArray = current.getRGB(0, 0, current.getWidth(), current.getHeight(), null, 0, current.getWidth());
                    //zoomedImage.setRGB(pos, 0, current.getWidth(), current.getHeight(), rgbArray, 0, current.getWidth());
                    pos += current.getWidth();
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                catch (ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
        }
        else {
            for (Future<BufferedImage> part : zoomedParts) {
                try {
                    current = part.get();
                    for (int y = 0; y < current.getHeight(); y++)
                        for (int x = 0; x < current.getWidth(); x++)
                            zoomedImage.setRGB(x, y + pos, current.getRGB(x, y));
                    //rgbArray = current.getRGB(0, 0, current.getWidth(), current.getHeight(), null, 0, current.getWidth());
                    //zoomedImage.setRGB(0, pos, current.getWidth(), current.getHeight(), rgbArray, 0, current.getWidth());
                    pos += current.getHeight();
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                catch (ExecutionException ex) {
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

        //keeps title and menu button
        JPanel bar = new JPanel();
        bar.setLayout(new BorderLayout(10, 10));
        bar.setPreferredSize(new Dimension(620, 100));
        bar.setBackground(black);

        //stepper - show progress
        //JPanel stepper = new JPanel();
        //stepper.setLayout(new BorderLayout(10, 10));
        //stepper.setPreferredSize(new Dimension(620, 60));

        //to open source image
        JPanel opener = new JPanel();
        opener.setLayout(new BorderLayout(10, 10));

        //keeps zoom coefficient chooser and button that start zooming
        JPanel zoomingOptions = new JPanel();
        zoomingOptions.setLayout(new BorderLayout(10, 10));

        //bar components setting
        JLabel menu = new JLabel();
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
        //open.setOpaque(true);

        //zoomingOptions components setting
        JButton zoom = new JButton("ZOOM");
        zoom.setAlignmentX(Component.CENTER_ALIGNMENT);
        zoom.setPreferredSize(new Dimension(300, 40));
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

        //stepper components setting
        //JLabel currentStepIcon = new JLabel();
        //currentStepIcon.setIcon(new ImageIcon("resources/folder-icon.png"));
        //JLabel currentStepText = new JLabel("Select image from file");
        WebStepProgress steps = new WebStepProgress("Step 1: Select image and zoom coefficient", "Step 2: Zoom it", "Step 3: ");
        //steps.addSteps(new WebImage("resources/folder-icon.png"));
        //steps.addSteps(new WebImage("resources/magnifier-icon.png"));
        //steps.addSteps(new WebImage("resources/done-icon.png"));
        steps.setLabelsPosition(SwingConstants.BOTTOM);
        steps.setSelectedStepIndex(2);

        //adding all on frame
        bar.add(BorderLayout.LINE_START, menu);
        bar.add(BorderLayout.CENTER, title);
        opener.add(BorderLayout.PAGE_END, open);
        opener.add(BorderLayout.PAGE_START, sourceImageViewer);
        zoomingOptions.add(BorderLayout.PAGE_START, hint);
        zoomingOptions.add(BorderLayout.CENTER, coefficientSlider);
        zoomingOptions.add(BorderLayout.PAGE_END, zoom);
        //stepper.add(currentStepIcon);
        //stepper.add(currentStepText);
        frame.add(BorderLayout.PAGE_START, bar);
        frame.add(BorderLayout.LINE_START, opener);
        frame.add(BorderLayout.LINE_END, zoomingOptions);
        frame.add(BorderLayout.PAGE_END, steps);
        frame.pack();
        frame.setVisible(true);

        open.addActionListener(new openButtonListener());
        zoom.addActionListener(new zoomButtonListener());
    }

    private class zoomButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Magnifier.zoomCoefficient = coefficientSlider.getValue();
            startThreads();
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
            opener.showOpenDialog(frame);
            if (opener.getSelectedFile() == null)
                return;

            try {
                sourceImage = ImageIO.read(opener.getSelectedFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            //frame.repaint();
            frame.revalidate();
        }
    }

    public static void main(String[] args) {
        Runner runner = new Runner();
        runner.buildGui();
    }
}
