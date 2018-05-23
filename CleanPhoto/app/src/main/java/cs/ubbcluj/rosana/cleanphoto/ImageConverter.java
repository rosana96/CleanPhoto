package cs.ubbcluj.rosana.cleanphoto;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 * Created by rosana on 16.05.2018.
 */

public class ImageConverter {

    public static YUVPixel[][] bitmapToYUV(Bitmap bitmap, int sec) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d("converter", " "+sec);
        YUVPixel[][] result = new YUVPixel[width][height];
//        Bitmap result = bitmap.copy(bitmap.getConfig(), true);
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++) {
                int color = bitmap.getPixel(j, i);
                result[j][i] = pixelToYUVPixel(color);

//                result.setPixel(i, j, Y);
            }
        }

        return result;
    }

    private static YUVPixel pixelToYUVPixel(int color) {
//           int A = (color >> 24) & 0xff; // or color >>> 24
        int R = (color >> 16) & 0xff;
        int G = (color >> 8) & 0xff;
        int B = (color) & 0xff;

        int Y = (int) (0.299 * R + 0.587 * G + 0.114 * B);
        int U = (int) (-0.147 * R - 0.289 * G + 0.436 * B);
//        int V = (int) (128 + 0.5 * R - 0.4186 * G - 0.0813 * B);
        int V = (int) (0.615*R - 0.515*G - 0.100*B);


        return new YUVPixel(Y, U, V);
    }


    public static Bitmap yuvToRGBBitmap(YUVPixel[][] image) {
        int width = image.length;
        int height = image[0].length;
        Bitmap.Config bitmapConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(width, height, bitmapConfig);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int color = YUVPixelToIntPixel(image[j][i]);
                bitmap.setPixel(j, i, color);
            }
        }
        return bitmap;
    }

    private static int YUVPixelToIntPixel(YUVPixel pixel) {

        int Y = pixel.getY();
        int U = pixel.getU();
        int V = pixel.getV();

//        int B = (int) (1.164 * (Y - 16) + 2.018 * (U - 128));
//        int G = (int) (1.164 * (Y - 16) - 0.813 * (V - 128) - 0.391 * (U - 128));
//        int R = (int) (1.164 * (Y - 16) + 1.596 * (V - 128));

        int R = (int) (Y + 1.140*V);
        int G = (int) (Y - 0.395*U - 0.581*V);
        int B = (int) (Y + 2.032*U);

        return Color.argb(255, R, G, B);
    }
}
