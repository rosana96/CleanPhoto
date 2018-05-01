package cs.ubbcluj.rosana.cleanphoto;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_VIDEO_CAPTURE = 1;
    ImageView capturedImageView;
    String path;
    private FFmpegMediaMetadataRetriever mediaMetadataRetriever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        capturedImageView = (ImageView) findViewById(R.id.capturedimage);

        mediaMetadataRetriever = new FFmpegMediaMetadataRetriever();

        File fileList[] = this.getApplicationContext().getExternalFilesDirs(Environment.DIRECTORY_MOVIES);
        path = fileList[1].getAbsolutePath();
        Log.d("PATH", path);

        dispatchTakeVideoIntent();
    }

    private void dispatchTakeVideoIntent() {
//        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
//
//            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
//                    LocationService.MY_PERMISSION_ACCESS_COURSE_LOCATION );
//        }

        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        Uri videoUri = Uri.fromFile(new File(path + "/a.mp4"));
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = intent.getData();
            Log.d("AICI", videoUri.getPath());

            mediaMetadataRetriever.setDataSource(videoUri.getPath());

            extractFrames();
        }
    }

    private void extractFrames() {
        String time = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInMSec = Long.parseLong(time);

        FileOutputStream out = null;

        List<Bitmap> luminanceImages = new ArrayList<>();
        for (int sec = 0; sec <= timeInMSec; sec = sec + 500) {

            Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(sec * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);//unit in microsecond

            try {
                out = new FileOutputStream(path + "/img_" + sec + ".png");
                bmFrame.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored

                Bitmap luminanceImg = convertToYUV(bmFrame);
                luminanceImages.add(luminanceImg);
                if (sec == 0) {
                    capturedImageView.setImageBitmap(luminanceImg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (luminanceImages.size() > 1) {
            int[][] diff = computeDifference(luminanceImages.get(0), luminanceImages.get(1));
            writeMatrix(path + "/diff.txt", diff);
            clearMovement(luminanceImages.get(0), diff);
        }
    }

    private void clearMovement(Bitmap bitmap, int[][] diff) {
        //TODO: check if a similar pixel is not somewhere in the surroundings (taking into account small transaltions
        //TODO: umbra? =)) pune conditia cu mai mult de 20.. da nu foarte mult

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap result = bitmap.copy(bitmap.getConfig(), true);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (Math.abs(diff[i][j]) > 20)
                    result.setPixel(i, j, Color.WHITE);
            }
        }

        FileOutputStream out = null;
        try {
             out = new FileOutputStream(path + "/cleared.png");
            result.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public Bitmap convertToYUV(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap result = bitmap.copy(bitmap.getConfig(), true);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = bitmap.getPixel(i, j);
//                int A = (color >> 24) & 0xff; // or color >>> 24
                int R = (color >> 16) & 0xff;
                int G = (color >> 8) & 0xff;
                int B = (color) & 0xff;

                int Y = (int) (0.299 * R + 0.587 * G + 0.114 * B);
                int U = (int) (-0.147 * R - 0.289 * G + 0.436 * B);
                result.setPixel(i, j, Y);
            }
        }
        return result;
    }

    public int[][] computeDifference(Bitmap img1, Bitmap img2) {
        int width = img1.getWidth();
        int height = img1.getHeight();
        int[][] matrix = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                matrix[i][j] = img1.getPixel(i, j) - img2.getPixel(i, j);
            }
        }
        return matrix;
    }


    void writeMatrix(String filename, int[][] matrix) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    bw.write(matrix[i][j] + ((j == matrix[i].length - 1) ? "" : ","));
                }
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
        }
    }


//
//    static public void encodeYUV420SP(byte[] yuv420sp, int[] rgba,
//                                      int width, int height) {
//        final int frameSize = width * height;
//
//        int[] U, V;
//        U = new int[frameSize];
//        V = new int[frameSize];
//
//        final int uvwidth = width / 2;
//
//        int r, g, b, y, u, v;
//        for (int j = 0; j < height; j++) {
//            int index = width * j;
//            for (int i = 0; i < width; i++) {
//
//                r = Color.red(rgba[index]);
//                g = Color.green(rgba[index]);
//                b = Color.blue(rgba[index]);
//
//                // rgb to yuv
//                y = (66 * r + 129 * g + 25 * b + 128) >> 8 + 16;
//                u = (-38 * r - 74 * g + 112 * b + 128) >> 8 + 128;
//                v = (112 * r - 94 * g - 18 * b + 128) >> 8 + 128;
//
//                // clip y
//                yuv420sp[index] = (byte) ((y < 0) ? 0 : ((y > 255) ? 255 : y));
//                U[index] = u;
//                V[index++] = v;
//            }
//        }
//    }
}
