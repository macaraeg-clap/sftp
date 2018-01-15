package com.sample.sftpandroidsample.sftp;

public class SFTPSavedFile {

    private SFTPSavedFile() {
    }

    public static SFTPSavedFile instance() {
        return new SFTPSavedFile();
    }

    private String file, fileSize;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }
}
