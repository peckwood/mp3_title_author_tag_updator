package org.example;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FolderDifferenceViewer{
    static String leftFolderPath = "D:/nextcloud/audio/music_repository";
    static String rightFolderPath = "F:/audio/music_repository";
    public static void main(String[] args){
        File leftFolder = new File(leftFolderPath);
        File rightFolder = new File(rightFolderPath);
        List<String> leftFileList = new ArrayList<>(601);
        List<String> rightFileList = new ArrayList<>(601);
        getAllFilesFromFolderRecursively(leftFolder, leftFileList);
        getAllFilesFromFolderRecursively(rightFolder, rightFileList);
        List<String> differenceList = leftFileList.stream().filter(filename -> !rightFileList.contains(filename)).collect(Collectors.toList());
        List<String> nonLrcDifferenceFiles = differenceList.stream().filter(filename -> !filename.contains(".lrc")).collect(Collectors.toList());
        System.out.println(differenceList);
        System.out.println(nonLrcDifferenceFiles);
    }

    static void getAllFilesFromFolderRecursively(File folder, List<String> fileList){
        Arrays.stream(folder.listFiles()).forEach(file -> {
            if(!file.isDirectory()){
                fileList.add(file.getName());
            }else{
                getAllFilesFromFolderRecursively(file, fileList);
            }
        });
    }
}
