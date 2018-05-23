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

import static java.lang.Math.min;
import static java.lang.Math.pow;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private static int MACROBLOCK_DIM = 8;
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

//        mediaMetadataRetriever.setDataSource("/storage/3137-6430/Android/data/cs.ubbcluj.rosana.cleanphoto/files/Movies/a.mp4");
//        extractFrames();
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
        int height = 0;
        int width = 0;

        List<YUVPixel[][]> luminanceImages = new ArrayList<>();
        for (int sec = 0; sec <= 1500; sec = sec + 500) {

            Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(sec * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);//unit in microsecond
            height = bmFrame.getHeight();
            width = bmFrame.getWidth();

            try {
                out = new FileOutputStream(path + "/img_" + sec + ".png");
                bmFrame.compress(Bitmap.CompressFormat.PNG, 100, out);
                // PNG is a lossless format, the compression factor (100) is ignored

                YUVPixel[][] luminanceImg = ImageConverter.bitmapToYUV(bmFrame,sec);
                luminanceImages.add(luminanceImg);

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

        createImage(luminanceImages,width,height);

    }

    private void createImage(List<YUVPixel[][]> luminanceImages, int width, int height) {
        YUVPixel[][] covered = new YUVPixel[width][height];
        FileOutputStream out = null;

        for (int i = 0; i < height; i = i + MACROBLOCK_DIM) {
            for (int j = 0; j < width; j = j + MACROBLOCK_DIM) {
                //calculam macroblockul (i,j) din imaginea finala
                float INF = 1000000000;
                float minMeanSquaredError = INF;
                int idMinDiffImgPair = -1;
                for (int k = 0; k < 3; k++) { //luminanceImages.size() - 1
                    int n = 0;
                    int MSE = 0;
                    for (int y = i; y < min(i + MACROBLOCK_DIM, height); y++)
                        for (int x = j; x < min(j + MACROBLOCK_DIM, width); x++) {
                            n++;
                            MSE += pow(
                                    (getPixelAt(x, y, luminanceImages.get(k)).getY()
                                            - getPixelAt(x, y, luminanceImages.get(k + 1)).getY()),
                                    2);
                        }
                    MSE /= n;
                    if (MSE < minMeanSquaredError) {
                        minMeanSquaredError = MSE;
                        idMinDiffImgPair = k;
                    }
                }

                for (int y = i; y < min(i + MACROBLOCK_DIM, height); y++)
                    for (int x = j; x < min(j + MACROBLOCK_DIM, width); x++) {
                        YUVPixel firstImgPixel = getPixelAt(x, y, luminanceImages.get(idMinDiffImgPair));
                        YUVPixel secondImgPixel= getPixelAt(x, y, luminanceImages.get(idMinDiffImgPair + 1));

                        YUVPixel meanPixel = getMeanPixel(firstImgPixel,secondImgPixel);
                        int meanColorY = (getPixelAt(x, y, luminanceImages.get(idMinDiffImgPair)).getY()
                                + getPixelAt(x, y, luminanceImages.get(idMinDiffImgPair + 1)).getY()) / 2;

                        covered[x][y] = meanPixel;

                    }
                Log.d("idMin", Integer.toString(idMinDiffImgPair) + "   i: " + i + "  j: " + j + "   minMse: " + minMeanSquaredError);
            }
        }

        Bitmap result = ImageConverter.yuvToRGBBitmap(covered);
        capturedImageView.setImageBitmap(result);

        try {

            out = new FileOutputStream(path + "/covered2.png");
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

    private YUVPixel getMeanPixel(YUVPixel p1, YUVPixel p2) {
        int Y = arithmeticMean(p1.getY(),p2.getY());
        int U = arithmeticMean(p1.getU(),p2.getU());
        int V = arithmeticMean(p1.getV(),p2.getV());
        return new YUVPixel(Y,U,V);
    }

    private int arithmeticMean(int a, int b) {
        return (a+b)/2;
    }

    private Bitmap replaceMovement(Bitmap bitmap, Bitmap result) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap covered = result.copy(bitmap.getConfig(), true);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (result.getPixel(i, j) == Color.MAGENTA)
                    covered.setPixel(i, j, bitmap.getPixel(i, j));
            }
        }

        return covered;
    }

    private Bitmap clearMovement(Bitmap bitmap, int[][] diff) {
        //TODO: check if a similar pixel is not somewhere in the surroundings (taking into account small translations
        //TODO: umbra? =)) pune conditia cu mai mult de 20.. da nu foarte mult

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap result = bitmap.copy(bitmap.getConfig(), true);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (Math.abs(diff[i][j]) > 50)
                    result.setPixel(i, j, Color.MAGENTA);
                Log.i("CLEAR: ", i + "  " + j);
            }
        }

        return result;
    }



    public YUVPixel getPixelAt(int w, int h, YUVPixel[][] matrix) {
        return matrix[w][h];
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

}
