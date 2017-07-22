package com.constdigit.zoom;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.Callable;

import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolatingFunction;

/*
    Processes part of the whole image in a separate thread
 */
public class Magnifier implements Callable<BufferedImage> {
    //supporting
    private enum RGB {RED, GREEN, BLUE}
    private static final byte THRESHOLD = 20;
    static int zoomCoefficient = 4;
    static boolean isSplittingEnable;
    private int sourceWidth;
    private int sourceHeight;
    private RGB currentColorInProcess;

    //split image by color
    private PixelArray red;
    private PixelArray green;
    private PixelArray blue;

    //splines for each color
    private ArrayList<PiecewiseBicubicSplineInterpolatingFunction> redSplines;
    private ArrayList<PiecewiseBicubicSplineInterpolatingFunction> greenSplines;
    private ArrayList<PiecewiseBicubicSplineInterpolatingFunction> blueSplines;

    private Stack<PixelArray> stack;

    Magnifier(BufferedImage source) {
        Color color;
        sourceWidth = source.getWidth();
        sourceHeight = source.getHeight();
        red = new PixelArray(sourceWidth, sourceHeight, 0, 0);
        green = new PixelArray(sourceWidth, sourceHeight, 0, 0);
        blue = new PixelArray(sourceWidth, sourceHeight, 0, 0);
        redSplines = new ArrayList<>();
        greenSplines = new ArrayList<>();
        blueSplines = new ArrayList<>();
        stack = new Stack<>();

        //init pixels arrays
        for (int i = 0; i < sourceHeight; i++)
            for (int j = 0; j < sourceWidth; j++) {
                color = new Color(source.getRGB(j, i));
                red.set(i, j, color.getRed());
                green.set(i, j, color.getGreen());
                blue.set(i, j, color.getBlue());
            }
    }

    //returns true if teastable has pixels with a difference in values greater than threshold
    private boolean isDifference(PixelArray testable) {
        double min = testable.get(0, 0);
        double current;
        //remembers the smallest value and moves to the end
        for (int i = 0; i < testable.getHeight(); i++)
            for (int j = 0; j < testable.getWidth(); j++) {
                current = testable.get(i, j);
                if (Math.abs(current - min) > THRESHOLD)
                    return true;
                if (current < min)
                    min = current;
            }
        return false;
    }

    //splitting by threshold
    private void splitting() {
        PixelArray currentPart;
        PixelArray temp;
        int width, height;
        while (!stack.isEmpty()) {
            currentPart = stack.pop();
            width = currentPart.getWidth();
            height = currentPart.getHeight();
            //splines requires minimum 5 knots
            if ((width >= 10 || height >= 10) && (width > 5 && height > 5)) {
                if(isDifference(currentPart)) {
                    if (height >= width) {
//                        * * * * *    * * * * *
//                        *       *    *       *
//                        *       * => * * * * *
//                        *       * => * * * * *
//                        *       *    *       *
//                        * * * * *    * * * * *
                        temp = new PixelArray(width, height / 2, currentPart.getX(), currentPart.getY());
                        for (int i = 0; i < height / 2; i++)
                            for (int j = 0; j < width; j++)
                                temp.set(i, j, currentPart.get(i, j));
                        stack.push(temp);

                        temp = new PixelArray(width, height - height / 2, currentPart.getX() + height / 2, currentPart.getY());
                        for (int i = height / 2; i < height; i++)
                            for (int j = 0; j < width; j++)
                                temp.set(i - height / 2, j, currentPart.get(i, j));
                        stack.push(temp);
                    }
                    else {
//                        * * * * * * * *    * * * * * * * *
//                        *             *    *     * *     *
//                        *             * => *     * *     *
//                        *             * => *     * *     *
//                        *             *    *     * *     *
//                        * * * * * * * *    * * * * * * * *
                        temp = new PixelArray(width / 2, height, currentPart.getX(), currentPart.getY());
                        for (int i = 0; i < height; i++)
                            for (int j = 0; j < width / 2; j++)
                                temp.set(i, j, currentPart.get(i, j));
                        stack.push(temp);

                        temp = new PixelArray(width - width / 2, height, currentPart.getX(), currentPart.getY() + width / 2);
                        for (int i = 0; i < height; i++)
                            for (int j = width / 2; j < width; j++)
                                temp.set(i, j - width / 2, currentPart.get(i, j));
                        stack.push(temp);
                    }
                    continue;
                }
            }
            interpolate(currentPart);
        }
    }

    //adds another spline to array
    private void interpolate(PixelArray colorArray) {
        double[] xCoords = colorArray.getXCoords();
        double[] yCoords = colorArray.getYCoords();
        double[][] colors = colorArray.getColorValues();
        PiecewiseBicubicSplineInterpolatingFunction pbsif = new PiecewiseBicubicSplineInterpolatingFunction(xCoords, yCoords, colors);
        switch (currentColorInProcess) {
            case RED : redSplines.add(pbsif); break;
            case GREEN : greenSplines.add(pbsif); break;
            case BLUE : blueSplines.add(pbsif); break;
        }
    }

    //returns correct pixel value from given coordinates
    private int validValue(double x, double y) {

        ArrayList<PiecewiseBicubicSplineInterpolatingFunction> splines = null;
        switch (currentColorInProcess) {
            case RED : splines = redSplines; break;
            case GREEN : splines = greenSplines; break;
            case BLUE : splines = blueSplines; break;
        }

        double temp = x;
        //coordinates can go beyond the spline so needs to check all 4 options for find their true location
        for (int i = 0; i < 4; i++) {
            for (PiecewiseBicubicSplineInterpolatingFunction spline : splines)
                if (spline.isValidPoint(x, y))
                    return (int) spline.value(x, y);

            switch (i) {
                case 0 : x = Math.round(x); break;
                case 1 : x = temp; y = Math.round(y); break;
                case 2 : x = Math.round(x); y = Math.round(y); break;
            }
        }

        return 0;
    }

    public BufferedImage call() {

        if (isSplittingEnable) {
            //interpolate red
            stack.push(red);
            currentColorInProcess = RGB.RED;
            splitting();
            stack.clear();

            //interpolate green
            stack.push(green);
            currentColorInProcess = RGB.GREEN;
            splitting();
            stack.clear();

            //interpolate blue
            stack.push(blue);
            currentColorInProcess = RGB.BLUE;
            splitting();
        }
        else {
            currentColorInProcess = RGB.RED;
            interpolate(red);
            currentColorInProcess = RGB.GREEN;
            interpolate(green);
            currentColorInProcess = RGB.BLUE;
            interpolate(blue);
        }

        //generates zoomed pixel array
        int zoomedWidth = sourceWidth * (zoomCoefficient / 2);
        int zoomedHeight = sourceHeight * (zoomCoefficient / 2);
        int[][] zoomedArray = new int[zoomedHeight][zoomedWidth];
        int r, g, b;
        double stepAlongWidth = (double) (sourceWidth - 1) / (double) (zoomedWidth - 1);
        double stepAlongHeight = (double) (sourceHeight - 1) / (double) (zoomedHeight - 1);
        double x, y;

        for (int i = 0; i < zoomedHeight; i++) {
            for (int j = 0; j < zoomedWidth; j++) {
                x = stepAlongHeight * i;
                y = stepAlongWidth * j;

                if (x > sourceHeight - 1) x = Math.floor(x);
                if (y > sourceWidth - 1) y = Math.floor(y);

                currentColorInProcess = RGB.RED;
                r = validValue(x, y);
                currentColorInProcess = RGB.GREEN;
                g = validValue(x, y);
                currentColorInProcess = RGB.BLUE;
                b = validValue(x, y);

                if (r <= 0) r = 0;
                if (g <= 0) g = 0;
                if (b <= 0) b = 0;

                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;

                zoomedArray[i][j] = new Color(r, g, b).getRGB();
            }
        }

        //collects image from array
        BufferedImage zoomed = new BufferedImage(zoomedWidth, zoomedHeight, BufferedImage.TYPE_3BYTE_BGR);
        for (int i = 0; i < zoomedHeight; i++)
            for (int j = 0; j < zoomedWidth; j++) {
                zoomed.setRGB(j, i, zoomedArray[i][j]);
            }

        return zoomed;
    }
}
