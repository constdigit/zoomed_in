package com.constdigit.zoom;

import java.util.ArrayList;

/*
    Represents part of source image via pixels array
 */
class PixelArray {
    //image size
    private int width;
    private int height;
    //coordinates relatively of source image
    private int mX;
    private int mY;

    private double[][] colorValues;

    PixelArray(int w, int h, int x, int y) {
        width = w;
        height = h;
        mX = x;
        mY = y;
        colorValues = new double[height][width];
    }

    double[] getXCoords() {
        double[] xCoords = new double[height];
        for (int i = 0; i < height; i++)
            xCoords[i] = mX + i;
        return xCoords;
    }

    double[] getYCoords() {
        double[] yCoords = new double[width];
        for (int i = 0; i < width; i++)
            yCoords[i] = mY + i;
        return yCoords;
    }

   double[][] getColorValues() {
        return colorValues;
    }

    void set(int x, int y, double color) {
        colorValues[x][y] = color;
    }

    double get(int x, int y) {
        return colorValues[x][y];
    }

    int getWidth() {
        return width;
    }

    int getHeight() {return height;}

    int getX() {
        return mX;
    }

    int getY() {
        return mY;
    }

    int size() {
        return width * height;
    }
}
