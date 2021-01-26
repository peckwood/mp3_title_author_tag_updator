package org.example;

import com.mpatric.mp3agic.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class App{
    static String processedFolderPath = "D:\\audio\\music_repository\\processed\\";
    static String unprocessedFolderPath = "D:\\audio\\music_repository\\unprocessed\\";
    static File repoFolder = new File("D:\\nextcloud\\audio\\music_repository");

    public static void main(String[] args) throws Exception{

        File processedFolder = new File(processedFolderPath);
        File unprocessedFolder = new File(unprocessedFolderPath);

        //删除2个文件夹
        if (processedFolder.exists() && !deleteNotEmptyFolder(processedFolder)) {
            throw new Exception("无法删除processedFolder");
        }
        if (unprocessedFolder.exists() && !deleteNotEmptyFolder(unprocessedFolder)) {
            throw new Exception("无法删除unprocessedFolder");
        }
        if (!processedFolder.mkdirs()) {
            throw new Exception("无法创建processedFolder");
        }
        if (!unprocessedFolder.mkdirs()) {
            throw new Exception("无法创建unprocessedFolder");
        }


        File[] mp3FileArray = repoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        List<File> mp3FileList = Arrays.asList(mp3FileArray);

        File chineseSongRepoFolder = new File("D:\\nextcloud\\audio\\music_repository\\chinese");
        File[] chineseMp3FileArray = chineseSongRepoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        List<File> chineseMp3FileList = Arrays.asList(chineseMp3FileArray);

        File foreignSongRepoFolder = new File("D:\\nextcloud\\audio\\music_repository\\foreign");
        File[] foreignMp3FileArray = foreignSongRepoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        List<File> foreignMp3FileList = Arrays.asList(foreignMp3FileArray);

        //File processedSongRepoFolder = new File("D:\\nextcloud\\audio\\music_repository\\processed");
        //File[] processedMp3FileArray = processedSongRepoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        //List<File> processedMp3FileList = Arrays.asList(processedMp3FileArray);

        List<File> allMp3FileList = new ArrayList<>(mp3FileList.size());
        allMp3FileList.addAll(mp3FileList);
        allMp3FileList.addAll(chineseMp3FileList);
        allMp3FileList.addAll(foreignMp3FileList);
        //allMp3FileList.addAll(processedMp3FileList);

        //System.out.println(mp3FileList.toString());
        for(File mp3File : allMp3FileList){
            Mp3File mp3file = null;
            try{
                mp3file = new Mp3File(mp3File.getAbsolutePath());
            }catch(Exception e){
                System.out.println(mp3File.getName() + " " + e.toString());
                copyFileToUnprocessedFolder(mp3File);
                printSeparator(true, mp3File.getName());
                continue;
            }

            String filename = mp3File.getName();

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
                newId3v2Tag.setTitle(titleV1);
                newId3v2Tag.setArtist(authorV1);
                mp3file.save(processedFolderPath + filename);
                System.out.println("V1 转为 V2: " + filename);
                fillTitleAndAuthorId3v2(newId3v2Tag, mp3file, titleV1, authorV1, processedFolderPath, filename);
                // 没有V2 title和author, 从文件名中取
            } else if (author2VIsEmpty || title2VIsEmpty) {
                String[] ignoredList = {"Geek Music - Sailor Moon_ Opening  Theme_ Moonlight Densetsu.mp3"
                };
                showSeperator = true;
                System.out.println("update from filename: " + filename);

                for(String ignored : ignoredList){
                    // 略过此文件
                    if (ignored.contains(filename)) {
                        copyFileToUnprocessedFolder(mp3File);
                    } else {
                        if (filename.contains(" - ")) {
                            if (filename.contains("feat")) {
                                System.out.println("filename contains 'feat'");
                                copyFileToUnprocessedFolder(mp3File);
                                //待手工处理
                            } else if (filename.contains("_unprocessed")) {
                                System.out.println("filename contains '_unprocessed'");
                            } else {
                                String filenameWithoutExtension = filename.substring(0, filename.length() - 4);
                                String[] splitFilename = filenameWithoutExtension.split(" - ");
                                if (splitFilename.length != 2) {
                                    System.out.println("file name length not right");
                                    copyFileToUnprocessedFolder(mp3File);
                                } else {
                                    String newAuthor = splitFilename[0].trim();
                                    String newTitle = splitFilename[1].trim();
                                    System.out.println("new title from filename: [" + newTitle + "]");
                                    System.out.println("new author from filename: [" + newAuthor + "]");
                                    ID3v2 newId3v2Tag = new ID3v24Tag();

                                    fillTitleAndAuthorId3v2(newId3v2Tag, mp3file, newTitle, newAuthor, processedFolderPath, filename);
                                }
                            }
                        } else {
                            System.out.println("file doesn't have ' - '");
                            copyFileToUnprocessedFolder(mp3File);
                        }
                    }
                }
            }//end else if


            printSeparator(showSeperator, filename);

        }
//        String[] mp3FilenameArray = repoFolder.list((dirFolder, fileName) -> fileName.contains(".mp3"));
//        List<String> mp3FilenameList = Arrays.asList(mp3FilenameArray);
//        mp3FilenameList.stream().forEach(System.out::println);

    }

    /**
     * 把待手工处理的文件复制到待处理文件夹
     * @param mp3file
     * @throws Exception
     */
    private static void copyFileToUnprocessedFolder(File mp3file) throws Exception{
        String filename = mp3file.getName();
        //String filenameWithoutExtension = filename.substring(0, filename.length() - 4);
        //String extension = filename.substring(filename.length() - 4);
        //String newFilename = filenameWithoutExtension + "_unprocessed" + extension;
        System.out.println("unprocessed: " + filename);

        File newDestionation = new File(unprocessedFolderPath + filename);
        Files.copy(mp3file.toPath(), newDestionation.toPath());
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
     * 删除非空文件夹
     * @param folder
     * @return
     */
    private static boolean deleteNotEmptyFolder(File folder){
        Arrays.stream(folder.listFiles()).forEach(File::delete);
        return folder.delete();
    }

    /**
     * print filename and separator
     * @param showSeperator
     */
    private static void printSeparator(boolean showSeperator, String filename){
        if (showSeperator) {
            System.out.println("file: " + filename);
            System.out.println("=======================================");
            System.out.println();
        }
    }

}
