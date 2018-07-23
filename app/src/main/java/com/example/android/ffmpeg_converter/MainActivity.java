package com.example.android.ffmpeg_converter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    public String VIDEO = "video";
    public String IMAGE = "image";
    public boolean IS_VIDEO = false;
    public String IMAGE_STRING = Environment.getExternalStorageDirectory().getPath() + "/Download/huge.jpg";
    public String VIDEO_STRING = Environment.getExternalStorageDirectory().getPath() + "/Download/mp4-2.mp4";
    public long START_TIME;
    private  FilesUtils filesUtils = new FilesUtils();
    private final static int DEFAULT_VIDEO_BITRATE = 5400;
    private final static int DEFAULT_HEIGHT = 720;
    private final static int FRAME_RATE = 30;
    private String DESTINATION_PATH;
    private static final long  MEGABYTE = 1024L * 1024L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button imageButton = findViewById(R.id.bt_compress_image);
        Button videoButton = findViewById(R.id.bt_compress_video);
        Button chooseImageButton = findViewById(R.id.bt_choose_image);
        Button chooseVideoButton = findViewById(R.id.bt_choose_video);

        File destinationPath = new File(Environment.getExternalStorageDirectory().getPath() + "/Download");
        destinationPath.mkdir();
        final File destfile = new File(destinationPath.getAbsolutePath());
        final String destFilePath = destfile.getAbsolutePath();

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }


        imageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try{
                    IMAGE_STRING = URLEncoder.encode(IMAGE_STRING, "UTF-8");

                    URI uri = URI.create(IMAGE_STRING);
                    Log.e("URI" , String.valueOf(uri));

                    Bitmap bmImg = BitmapFactory.decodeFile(uri.getPath());
                    ImageView sourceImage = findViewById(R.id.iv_source);
                    sourceImage.setImageBitmap(bmImg);
                    setImage();

                    String uriString = URLDecoder.decode(String.valueOf(uri), "UTF-8");

                    TextView resultText = findViewById(R.id.tv_result);
                    File file1 = new File(uriString);
                    int[] resolution = filesUtils.imageResolution(file1);
                    resultText.setText("Source File:\nSource FilePath = " + file1.getAbsolutePath() + " ;\nsize = " + bytesToMeg(file1.length()) + " MB\nResolution = " + resolution[0] + ":" + resolution[1]);

                    compress(uriString, destFilePath , IMAGE);
                }catch(Exception e){

                }
            }
        });

        videoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try{
                    VIDEO_STRING = URLEncoder.encode(VIDEO_STRING, "UTF-8");

                    URI uri = URI.create(VIDEO_STRING);
                    Log.e("URI" , String.valueOf(uri));


                    String uriString = URLDecoder.decode(String.valueOf(uri), "UTF-8");

                    setVideo();
                    TextView resultText = findViewById(R.id.tv_result);
                    File file1 = new File(uriString);
                    int[] resolution = filesUtils.videoResolution(file1);
                    resultText.setText("Source File:\nSource FilePath = " + file1.getAbsolutePath() + " ;\nsize = " + bytesToMeg(file1.length()) + " MB\nResolution = " + resolution[0] + ":" + resolution[1] + "\nBitrate = " + resolution[2] );

                    compress(uriString, destFilePath, VIDEO);
                }catch(Exception e){

                }
            }
        });

        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                performFileSearch(IMAGE);

            }
        });

        chooseVideoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                performFileSearch(VIDEO);

            }
        });

//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
    }

    private void setImage(){
        ImageView sourceImage = findViewById(R.id.iv_source);
        sourceImage.setVisibility(View.VISIBLE);
        ImageView destimage = findViewById(R.id.iv_destination);
        destimage.setVisibility(View.VISIBLE);
        ImageView deviderView = findViewById(R.id.imageView);
        deviderView.setVisibility(View.VISIBLE);
    }

    private void setVideo(){
        ImageView sourceImage = findViewById(R.id.iv_source);
        sourceImage.setVisibility(View.INVISIBLE);
        ImageView destimage = findViewById(R.id.iv_destination);
        destimage.setVisibility(View.INVISIBLE);
        ImageView deviderView = findViewById(R.id.imageView);
        deviderView.setVisibility(View.INVISIBLE);
    }

    public static long bytesToMeg(long bytes) {
        return bytes / MEGABYTE ;
    }

    private static final int READ_REQUEST_CODE = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch(String type) {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if(type.equals(IMAGE)){
            String[] mimeTypes = {"image/jpeg"};
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            IS_VIDEO = false;
        }else if(type.equals(VIDEO)){
            String[] mimeTypes = {"video/mp4"};
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            IS_VIDEO = true;
        }

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i("NEW URI", filesUtils.getPath(this, uri));
                if(IS_VIDEO){
                    VIDEO_STRING = filesUtils.getPath(this, uri);
                }else{
                    IMAGE_STRING = filesUtils.getPath(this, uri);
                }
            }
        }
    }


    private void compress(String sourcePath, String destPath, String type){
        String fileDestPath = null;

        if (type.equals(VIDEO)) {
            fileDestPath = destPath + "/VIDEO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";
            DESTINATION_PATH = fileDestPath;
            try{
                int[] resolution = filesUtils.videoResolution(new File(sourcePath));
                int sourceFileHeight  = resolution[0];
                int sourceFileWidth   = resolution[1];
                int sourceFileBitrate = resolution[2];

                if(sourceFileHeight > DEFAULT_HEIGHT ){
                    int newWidth = calculateWidth(sourceFileHeight , sourceFileWidth);
                    if(sourceFileBitrate > DEFAULT_VIDEO_BITRATE){
                        executeVideoCompress(sourcePath, fileDestPath, DEFAULT_HEIGHT, newWidth, DEFAULT_VIDEO_BITRATE);
                    }else{
                        executeVideoCompress(sourcePath, fileDestPath, DEFAULT_HEIGHT, newWidth, sourceFileBitrate);
                    }
                }else if(sourceFileBitrate > DEFAULT_VIDEO_BITRATE) {
                    executeVideoCompress(sourcePath, fileDestPath, sourceFileHeight, sourceFileWidth, DEFAULT_VIDEO_BITRATE);
                }else{
                    TextView resultText = findViewById(R.id.tv_result);
                    resultText.append("\n\n Die Datei wurde nicht weiter komprimiert das sie schon der genünschten größe entrsprucht.");
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }else if(type.equals(IMAGE)){
            fileDestPath = destPath + "/IMAGE_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
            DESTINATION_PATH = fileDestPath;
            int[] resolution = filesUtils.imageResolution(new File(sourcePath));
            int height = resolution[0];
            int width = resolution[1];

            try{
                if(height > DEFAULT_HEIGHT){
                    int newWidth = calculateWidth(height, width);
                    executeImageCompress(sourcePath, fileDestPath, DEFAULT_HEIGHT, newWidth);
                }else{
                    TextView resultText = findViewById(R.id.tv_result);
                    resultText.append("\n\n Die Datei wurde nicht weiter komprimiert das sie schon der genünschten größe entrsprucht.");
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    private void ffmpegLoadBinary(Context context){
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {}

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }
    }

    private void ffmpegExecute(Context context, String[] cmd){
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    ProgressBar proBar = findViewById(R.id.progress_bar);
                    proBar.setVisibility(View.VISIBLE);
                    START_TIME   = System.nanoTime();

                }

                @Override
                public void onProgress(String message) {
                    Log.e("ffmpg", message);
                }

                @Override
                public void onFailure(String message) {
                    Log.e("ffmpg", message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.e("ffmpg", message);
                    File file1 = new File(DESTINATION_PATH);
                    TextView resultText = findViewById(R.id.tv_result);
                    resultText.getText();


                    long endTime   = System.nanoTime();
                    long totalTime = endTime - START_TIME;
                    double seconds = TimeUnit.SECONDS.convert(totalTime, TimeUnit.NANOSECONDS);


                    if(DESTINATION_PATH == null){
                        resultText.append("\n\n Die Datei konnte nicht komprimiert werden.");
                    }else{
                        String size;

                        if(bytesToMeg(file1.length()) == 0){
                            size = "size = " + file1.length() + " bytes";
                        }else{
                            size = "size = " + bytesToMeg(file1.length()) + " MB";
                        }

                        if(IS_VIDEO){

                            if(DESTINATION_PATH.equals(VIDEO_STRING)){
                                resultText.append("\n\n Die Datei wurde nicht weiter komprimiert das sie schon der genünschten größe entrsprucht.");
                            }else{
                                int[] resolution = filesUtils.videoResolution(file1);
                                resultText.append("\n\nCompressed File:\nFilePath = " + file1.getAbsolutePath() + " ;\n" + size + "\nResolution = " + resolution[0] + ":" + resolution[1] + "\nBitrate = " + resolution[2] + "\nCompress time = " + seconds +" sekunden");

                            }
                        }else{
                            int[] resolution = filesUtils.imageResolution(file1);
                            resultText.append("\n\nCompressed File:\nFilePath = " + file1.getAbsolutePath() + " ;\n" + size + "\nResolution = " + resolution[0] + ":" + resolution[1] + "\nCompress time = " + seconds +" sekunden");

                        }
                    }

                    if(DESTINATION_PATH.contains("jpg")){
                        Bitmap bmImg = BitmapFactory.decodeFile(DESTINATION_PATH);
                        ImageView sourceImage = findViewById(R.id.iv_destination);
                        sourceImage.setImageBitmap(bmImg);
                    }

                    ProgressBar proBar = findViewById(R.id.progress_bar);
                    proBar.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onFinish() {}


            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }

    private void executeVideoCompress(String sourcePath, String destPath, int height, int width, int bitrate ){
        Log.i("Compress", "Compress " + sourcePath + " to " + destPath + " Heigth = " + height + " Width = " +  width + " Bitrate = " + bitrate);

        ffmpegLoadBinary(this);
        ffmpegExecute(this, new String[] {"-i", sourcePath, "-s" , height + "x" + width, "-b", bitrate + "k", "-c:a", "copy", "-preset", "ultrafast" , destPath});
    }

    private void executeImageCompress(String sourcePath, String destPath, int height, int width){
        Log.i("Compress", "Compress " + sourcePath + " to " + destPath + " Heigth = " + height + " Width = " +  width);

        ffmpegLoadBinary(this);
        ffmpegExecute(this, new String[] {"-i", sourcePath, "-s" , height + "x" + width, "-c:a", "copy", "-preset", "ultrafast" , destPath});
    }

    private int calculateWidth(int height, int width){

        double relation =  (double )width / (double )height;
        Log.e("CALCULATE" , "Relation " + relation);
        double newWidth = DEFAULT_HEIGHT * relation;

        int newCorrectWidth = ((int)newWidth /2) * 2;

        Log.e("CALCULATE" , "new Width " + newWidth + " Corrected = " + newCorrectWidth);

        return newCorrectWidth;
    }
}
