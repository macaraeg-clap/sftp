package com.sample.sftpandroidsample.sftp;

public class SFTPPath {

    private SFTPPath() {
    }

    public static SFTPPath instance() {
        return new SFTPPath();
    }

    private String sourcePath, destinationPath;

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }
}
