package org.example;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class App{
    public static void main(String[] args) throws Exception{
        String newOutputFolderPath = "D:\\nextcloud\\audio\\music_repository\\processed\\";
        File repoFolder = new File("D:\\nextcloud\\audio\\music_repository");
        File[] mp3FileArray = repoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        List<File> mp3FileList = Arrays.asList(mp3FileArray);

        File chineseSongRepoFolder = new File("D:\\nextcloud\\audio\\music_repository\\chinese");
        File[] chineseMp3FileArray = chineseSongRepoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        List<File> chineseMp3FileList = Arrays.asList(chineseMp3FileArray);

        File foreignSongRepoFolder = new File("D:\\nextcloud\\audio\\music_repository\\foreign");
        File[] foreignMp3FileArray = foreignSongRepoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        List<File> foreignMp3FileList = Arrays.asList(foreignMp3FileArray);

        File processedSongRepoFolder = new File("D:\\nextcloud\\audio\\music_repository\\processed");
        File[] processedMp3FileArray = processedSongRepoFolder.listFiles((dirFolder, fileName) -> fileName.contains(".mp3"));
        List<File> processedMp3FileList = Arrays.asList(processedMp3FileArray);

        List<File> allMp3FileList = new ArrayList<>(mp3FileList.size());
        allMp3FileList.addAll(mp3FileList);
        allMp3FileList.addAll(chineseMp3FileList);
        allMp3FileList.addAll(foreignMp3FileList);
        allMp3FileList.addAll(processedMp3FileList);

        //System.out.println(mp3FileList.toString());
        for(File mp3File : allMp3FileList){
            Mp3File mp3file = null;
            try{
                mp3file = new Mp3File(mp3File.getAbsolutePath());
            }catch(Exception e){
                System.out.println(mp3File.getName() + " " + e.toString());
                markFileUnprocessed(mp3File);
                continue;
            }

            String filename = mp3File.getName();

            boolean hasId3v1Tag = mp3file.hasId3v1Tag();
            String titleV1 = null;
            String authorV1 = null;
            ID3v1 id3v1Tag = null;
            if(hasId3v1Tag){
                id3v1Tag = mp3file.getId3v1Tag();
                titleV1 = id3v1Tag.getTitle();
                authorV1 = id3v1Tag.getArtist();
            }

            boolean hasId3v2Tag = mp3file.hasId3v2Tag();
            String titleV2 = null;
            String authorV2 = null;
            ID3v2 id3v2Tag = null;
            if(mp3file.hasId3v2Tag()){
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

            if(title2VIsEmpty && !title1VIsEmpty){
                //titleV1 = new String(titleV1.getBytes("gb2312"), "utf-8");
                titleV1 = new String(titleV1.getBytes(StandardCharsets.ISO_8859_1), "gbk");
                System.out.println("-title from V1: " + titleV1);

                titleNeedsV1UpdateV2 = true;
            }
            if(author2VIsEmpty && !author1VIsEmpty){
                authorV1 = new String(authorV1.getBytes(StandardCharsets.ISO_8859_1), "gbk");
                System.out.println("author from V1: " + authorV1);
                authorNeedsV1UpdateV2 = true;
            }

            //TODO: uncomment
            if(1 != 1 && (titleNeedsV1UpdateV2 || authorNeedsV1UpdateV2)){
                showSeperator = true;
                ID3v2 newId3v2Tag;
                if(mp3file.hasId3v2Tag()){
                    newId3v2Tag = mp3file.getId3v2Tag();
                }else{
                    // mp3 does not have an ID3v2 tag, let's create one..
                    newId3v2Tag = new ID3v24Tag();
                    mp3file.setId3v2Tag(newId3v2Tag);
                }
                mp3file.removeId3v1Tag();
                newId3v2Tag.setTitle(titleV1);
                newId3v2Tag.setArtist(authorV1);
                mp3file.save(newOutputFolderPath + filename);
                System.out.println("V1 > V2: " + filename);
                System.out.println("new V2 title: " + mp3file.getId3v2Tag().getTitle());
                System.out.println("new V2 author: " + mp3file.getId3v2Tag().getArtist());
                System.out.println("------------------------------------");
                //没有V2 title和author, 从文件名中取
            }else if(author2VIsEmpty || title2VIsEmpty){
                String[] ignoredList = {"Geek Music - Sailor Moon_ Opening  Theme_ Moonlight Densetsu.mp3"
                };
                showSeperator = true;
                System.out.println("update from filename: " + filename);
                if(filename.contains(" - ")){
                    if(filename.contains("(") || filename.contains("feat")){
                        System.out.println("filename contains '(' or 'feat'");
                        markFileUnprocessed(mp3File);
                    }else if(filename.contains("_unprocessed")){
                        System.out.println("filename contains '_unprocessed'");
                    }else{
                        String filenameWithoutExtension = filename.substring(0, filename.length() - 4);
                        String[] splitedFilename = filenameWithoutExtension.split(" - ");
                        if(splitedFilename.length != 2){
                            System.out.println("file name length not right");
                            markFileUnprocessed(mp3File);
                        }else{
                            String newAuthor = splitedFilename[0].trim();
                            String newTitle = splitedFilename[1].trim();
                            System.out.println("new title: [" + newTitle + "]");
                            System.out.println("new author: [" + newAuthor + "]");
                            //TODO update its tags in a new method
                        }
                    }
                }else{
                    System.out.println("file doesn't have ' - '");
                    markFileUnprocessed(mp3File);
                }
            }

            if(showSeperator){
                System.out.println("file: " + filename);
                System.out.println("=======================================");
            }


        }
//        String[] mp3FilenameArray = repoFolder.list((dirFolder, fileName) -> fileName.contains(".mp3"));
//        List<String> mp3FilenameList = Arrays.asList(mp3FilenameArray);
//        mp3FilenameList.stream().forEach(System.out::println);

    }

    /**
     * 标记文件为待手工处理
     * @param mp3file
     * @throws Exception
     */
    private static void markFileUnprocessed(File mp3file) throws Exception{
        String filename = mp3file.getName();
        String filenameWithoutExtension = filename.substring(0, filename.length() - 4);
        String extension = filename.substring(filename.length() - 4);
        String newFilename = filenameWithoutExtension + "_unprocessed" + extension;
        System.out.println("markFileUnprocessed: " + newFilename);
        //TODO uncomment
//        boolean success = mp3file.renameTo(new File(mp3file.getParentFile(), ));
//        if(!success){
//            throw new Exception("mark file unprocessed failed: " + mp3file.getName());
//        }


    }

}
