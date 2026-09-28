package com.chaersihaha.mangareader;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    LinearLayout root;
    LinearLayout shelf;
    SharedPreferences sp;
    File booksDir;
    File currentBook;

    private static final int REQ_ADD_IMAGES = 7;
    private static final int REQ_PICK_COVER = 8;

    private String pendingCoverDisplayName = null;

    private File draggingBook = null;
    private boolean isDraggingBook = false;

    // =========================================================
    // 视觉系统
    // =========================================================

    private static final int BG = Color.rgb(14, 14, 16);
    private static final int SURFACE = Color.rgb(22, 22, 25);
    private static final int SURFACE_2 = Color.rgb(29, 29, 33);
    private static final int SURFACE_3 = Color.rgb(36, 36, 41);

    private static final int TEXT_PRIMARY =
            Color.rgb(242, 242, 244);

    private static final int TEXT_SECONDARY =
            Color.rgb(165, 165, 171);

    private static final int TEXT_MUTED =
            Color.rgb(105, 105, 112);

    private static final int ACCENT =
            Color.rgb(214, 214, 220);

    // =========================================================
    // 缩略图内存优化
    // =========================================================

    private Bitmap loadThumbnail(
            File file,
            int targetWidth,
            int targetHeight
    ) {

        if (file == null || !file.exists()) {
            return null;
        }

        BitmapFactory.Options bounds =
                new BitmapFactory.Options();

        bounds.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(
                file.getAbsolutePath(),
                bounds
        );

        if (
                bounds.outWidth <= 0
                        || bounds.outHeight <= 0
        ) {
            return null;
        }

        int sampleSize =
                calculateInSampleSize(
                        bounds.outWidth,
                        bounds.outHeight,
                        targetWidth,
                        targetHeight
                );

        BitmapFactory.Options options =
                new BitmapFactory.Options();

        options.inSampleSize = sampleSize;

        /*
         * 缩略图只用于列表预览，
         * 不需要原图级别的 RGBA 精度。
         * RGB_565 可以减少内存占用。
         */
        options.inPreferredConfig =
                Bitmap.Config.RGB_565;

        try {

            return BitmapFactory.decodeFile(
                    file.getAbsolutePath(),
                    options
            );

        } catch (OutOfMemoryError e) {

            return null;
        }
    }

    private int calculateInSampleSize(
            int width,
            int height,
            int reqWidth,
            int reqHeight
    ) {

        int inSampleSize = 1;

        if (
                reqWidth <= 0
                        || reqHeight <= 0
        ) {
            return 1;
        }

        if (
                height > reqHeight
                        || width > reqWidth
        ) {

            int halfHeight =
                    height / 2;

            int halfWidth =
                    width / 2;

            while (
                    (halfHeight / inSampleSize)
                            >= reqHeight
                            &&
                    (halfWidth / inSampleSize)
                            >= reqWidth
            ) {

                inSampleSize *= 2;
            }
        }

        return Math.max(
                1,
                inSampleSize
        );
    }

    // =========================================================
    // 2.0 动画系统
    // =========================================================

    private void animateAppear(
            View view,
            long delay
    ) {

        view.setAlpha(0f);
        view.setTranslationY(dp(10));

        view.animate()
                .alpha(1f)
                .translationY(0f)
