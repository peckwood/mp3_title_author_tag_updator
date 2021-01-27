package org.example;

import com.mpatric.mp3agic.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class App{
    static String repositoryFolderPath = "D:\\nextcloud\\audio\\music_repository\\";
    static String normalSongFolderPath = repositoryFolderPath;
    static String chineseSongFolderPath = repositoryFolderPath + "\\chinese";
    static String foreignSongFolderPath = repositoryFolderPath + "\\foreign";

    static String outputRepositoryFolderPath = "F:\\audio\\music_repository";
    static String normalProcessedFolderPath = outputRepositoryFolderPath + "\\";
    static String chineseProcessedFolderPath = outputRepositoryFolderPath + "\\chinese\\";
    static String foreignProcessedFolderPath = outputRepositoryFolderPath + "\\foreign\\";

    static String[] ignoredList = {"Geek Music - Sailor Moon_ Opening  Theme_ Moonlight Densetsu.mp3"

    };
    static List<String> unprocessedFileNames = new ArrayList<>();

    public static void main(String[] args) throws Exception{
        File normalSongRepoFolder = new File(normalSongFolderPath);
        File chineseSongRepoFolder = new File(chineseSongFolderPath);
        File foreignSongRepoFolder = new File(foreignSongFolderPath);

        File normalProcessedFolder = new File(normalProcessedFolderPath);
        File chineseProcessedFolder = new File(chineseProcessedFolderPath);
        File foreignProcessedFolder = new File(foreignProcessedFolderPath);

        deleteNotEmptyFolder(normalProcessedFolder);
        normalProcessedFolder.mkdirs();
        chineseProcessedFolder.mkdirs();
        foreignProcessedFolder.mkdirs();


        FilenameFilter mp3FilenameFilter = (dirFolder, fileName) -> fileName.contains(".mp3");

        File[] mp3FileArray = normalSongRepoFolder.listFiles(mp3FilenameFilter);
        List<File> normalSongList = Arrays.asList(mp3FileArray);
        File[] chineseMp3FileArray = chineseSongRepoFolder.listFiles(mp3FilenameFilter);
        List<File> chineseMp3FileList = Arrays.asList(chineseMp3FileArray);
        File[] foreignMp3FileArray = foreignSongRepoFolder.listFiles(mp3FilenameFilter);
        List<File> foreignMp3FileList = Arrays.asList(foreignMp3FileArray);

        process(normalSongList, normalProcessedFolderPath);
        process(chineseMp3FileList, chineseProcessedFolderPath);
        process(foreignMp3FileList, foreignProcessedFolderPath);


        System.out.println("unprocessed: " + unprocessedFileNames);
        //allMp3FileList.addAll(processedMp3FileList);

        //System.out.println(mp3FileList.toString());

//        String[] mp3FilenameArray = repoFolder.list((dirFolder, fileName) -> fileName.contains(".mp3"));
//        List<String> mp3FilenameList = Arrays.asList(mp3FilenameArray);
//        mp3FilenameList.stream().forEach(System.out::println);

    }

    private static void process(List<File> mp3FileList, String processedFolderPath) throws Exception{
        for(File mp3File : mp3FileList){
            String filename = mp3File.getName();
//            if(!filename.contains("处处吻")){
//                continue;
//            }

            Mp3File mp3file = null;
            try{
                mp3file = new Mp3File(mp3File.getAbsolutePath());
            }catch(Exception e){
                copyFileToUnprocessedFolder(mp3File, e);
                printSeparator(true, mp3File.getName());
                continue;
            }

            boolean hasId3v1Tag = mp3file.hasId3v1Tag();
            String titleV1 = null;
            String authorV1 = null;
            ID3v1 id3v1Tag = null;
            if (hasId3v1Tag) {
                id3v1Tag = mp3file.getId3v1Tag();
                titleV1 = id3v1Tag.getTitle();
                authorV1 = id3v1Tag.getArtist();
            }

            boolean hasId3v2Tag = mp3file.hasId3v2Tag();
            String titleV2 = null;
            String authorV2 = null;
            ID3v2 id3v2Tag = null;
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
                titleV2 = mp3file.getId3v2Tag().getTitle();
                authorV2 = mp3file.getId3v2Tag().getArtist();
            }

            boolean title2VIsEmpty = titleV2 == null || titleV2.trim().equals("");
            boolean title1VIsEmpty = titleV1 == null || titleV1.trim().equals("");
            boolean author2VIsEmpty = authorV2 == null || authorV2.trim().equals("");
            boolean author1VIsEmpty = authorV1 == null || authorV1.trim().equals("");
            boolean titleNeedsV1UpdateV2 = false;
            boolean authorNeedsV1UpdateV2 = false;

            boolean showSeperator = false;

            if (title2VIsEmpty && !title1VIsEmpty) {
                //titleV1 = new String(titleV1.getBytes("gb2312"), "utf-8");
                titleV1 = new String(titleV1.getBytes(StandardCharsets.ISO_8859_1), "gbk");
                System.out.println("-title from V1: " + titleV1);

                titleNeedsV1UpdateV2 = true;
            }
            if (author2VIsEmpty && !author1VIsEmpty) {
                authorV1 = new String(authorV1.getBytes(StandardCharsets.ISO_8859_1), "gbk");
                System.out.println("author from V1: " + authorV1);
                authorNeedsV1UpdateV2 = true;
            }

            if ((titleNeedsV1UpdateV2 || authorNeedsV1UpdateV2)) {
                showSeperator = true;
                ID3v2 newId3v2Tag;
                if (mp3file.hasId3v2Tag()) {
                    newId3v2Tag = mp3file.getId3v2Tag();
                } else {
                    // mp3 does not have an ID3v2 tag, let's create one..
                    newId3v2Tag = new ID3v24Tag();
                    mp3file.setId3v2Tag(newId3v2Tag);
                }
                mp3file.removeId3v1Tag();
                System.out.println("V1 转为 V2: " + filename);
                try{
                    fillTitleAndAuthorId3v2(newId3v2Tag, mp3file, titleV1, authorV1, processedFolderPath, filename);
                }catch(Exception e){
                    copyFileToUnprocessedFolder(mp3File, e);
                }
                // 没有V2 title和author, 从文件名中取
            } else if (author2VIsEmpty || title2VIsEmpty) {
                showSeperator = true;
                System.out.println("update from filename: " + filename);

                for(String ignored : ignoredList){
                    // 略过此文件
                    if (ignored.contains(filename)) {
                        copyFileToUnprocessedFolder(mp3File, new Exception("file in ignored list"));
                    } else {
                        if (filename.contains(" - ")) {
                            if (filename.contains("feat")) {
                                copyFileToUnprocessedFolder(mp3File, new Exception("filename contains 'feat'"));
                                //待手工处理
                            } else if (filename.contains("_unprocessed")) {
                                System.out.println("filename contains '_unprocessed'");
                            } else {
                                String filenameWithoutExtension = filename.substring(0, filename.length() - 4);
                                String[] splitFilename = filenameWithoutExtension.split(" - ");
                                if (splitFilename.length != 2) {
                                    copyFileToUnprocessedFolder(mp3File, new Exception("file name length not right"));
                                } else {
                                    String newAuthor = splitFilename[0].trim();
                                    String newTitle = splitFilename[1].trim();
                                    System.out.println("new title from filename: [" + newTitle + "]");
                                    System.out.println("new author from filename: [" + newAuthor + "]");
                                    ID3v2 newId3v2Tag = new ID3v24Tag();
                                    try{
                                        fillTitleAndAuthorId3v2(newId3v2Tag, mp3file, newTitle, newAuthor, processedFolderPath, filename);
                                    }catch(Exception e){
                                        copyFileToUnprocessedFolder(mp3File, e);
                                    }
                                }
                            }
                        } else {
                            copyFileToUnprocessedFolder(mp3File, new Exception("file doesn't have ' - '"));
                        }
                    }
                }
            }//end else if


            printSeparator(showSeperator, filename);

        }
    }

    /**
     * 把待手工处理的文件复制到待处理文件夹
     * @param mp3file
     * @throws Exception
     */
    private static void copyFileToUnprocessedFolder(File mp3file, Exception e) throws Exception{
        String filename = mp3file.getName();
        //String filenameWithoutExtension = filename.substring(0, filename.length() - 4);
        //String extension = filename.substring(filename.length() - 4);
        //String newFilename = filenameWithoutExtension + "_unprocessed" + extension;
        System.out.println("unprocessed: " + filename);
        System.out.println("exception: " + e == null ? "" : e.getMessage());
        unprocessedFileNames.add(mp3file.getName());
    }

    private static void fillTitleAndAuthorId3v2(ID3v2 newId3v2Tag, Mp3File mp3file, String title, String artist, String newOutputFolderPath, String filename) throws IOException, NotSupportedException{
        title = title.trim();
        artist = artist.trim();
        if ("ADELE".equals(artist)) {
            artist = "Adele";
        }
        mp3file.setId3v2Tag(newId3v2Tag);
        newId3v2Tag.setTitle(title);
        newId3v2Tag.setArtist(artist);
        mp3file.save(newOutputFolderPath + filename);

        System.out.println("V2 title: [" + mp3file.getId3v2Tag().getTitle() + "]");
        System.out.println("V2 author: [" + mp3file.getId3v2Tag().getArtist() + "]");
        System.out.println("------------------------------------");
    }

    /**
     * print filename and separator
     * @param showSeparator
     */
    private static void printSeparator(boolean showSeparator, String filename){
        if (showSeparator) {
            System.out.println("file: " + filename);
            System.out.println("=======================================");
            System.out.println();
        }
    }

    /**
     * 删除非空文件夹
     * @param folderToDelete
     * @throws Exception
     */
    private static void deleteNotEmptyFolder(File folderToDelete) throws Exception{
        for(File file : folderToDelete.listFiles()){
            if (file.isDirectory()) {
                deleteNotEmptyFolder(file);
            } else {
                file.delete();
            }
        }
        folderToDelete.delete();
    }

}
