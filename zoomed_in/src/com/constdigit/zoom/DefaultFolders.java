package com.constdigit.zoom;

import java.io.*;

class DefaultFolders implements Serializable{
    private File defaultOpenFolder;
    private File defaultSaveFolder;
    private boolean askedBeforeSaving;
    private static DefaultFolders instance;

    private DefaultFolders() {
        defaultOpenFolder = null;
        defaultSaveFolder = null;
    }

    static DefaultFolders getInstance() {
        if (instance == null)
            return new DefaultFolders();
        else
            return instance;
    }

    void saveSettings() {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("resources/settings.ser"));
            objectOutputStream.writeObject(defaultOpenFolder);
            objectOutputStream.writeObject(defaultSaveFolder);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void restoreSettings() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("resources/settings.ser"));
            defaultOpenFolder = (File) objectInputStream.readObject();
            defaultSaveFolder = (File) objectInputStream.readObject();
        }
        catch (FileNotFoundException ex) {
            defaultOpenFolder = null;
            defaultSaveFolder = null;
        }
        catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    File getDefaultOpenFolder() {
        return defaultOpenFolder;
    }

    File getDefaultSaveFolder() {
        return defaultSaveFolder;
    }

    void setDefaultOpenFolder(File defaultOpenFolder) {
        if (defaultOpenFolder.isDirectory())
            this.defaultOpenFolder = defaultOpenFolder;
    }

    void setDefaultSaveFolder(File defaultSaveFolder) {
        if (defaultSaveFolder.isDirectory())
            this.defaultSaveFolder = defaultSaveFolder;
    }
}
