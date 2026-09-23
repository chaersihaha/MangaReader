package com.chaersihaha.mangareader;

import android.app.Activity;
import android.app.AlertDialog;
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
                .setStartDelay(delay)
                .setDuration(260)
                .setInterpolator(
                        new android.view.animation.DecelerateInterpolator(1.6f)
                )
                .start();
    }

    private void animateDialogIn(
            View view
    ) {

        view.setAlpha(0f);
        view.setScaleX(0.96f);
        view.setScaleY(0.96f);
        view.setTranslationY(dp(8));

        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(
                        new android.view.animation.DecelerateInterpolator(1.5f)
                )
                .start();
    }

    private void animateControlIn(
            View view
    ) {

        view.setAlpha(0f);
        view.setTranslationY(dp(20));

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(190)
                .setInterpolator(
                        new android.view.animation.DecelerateInterpolator(1.7f)
                )
                .start();
    }

    private void animateButtonPress(
            final View view
    ) {

        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .alpha(0.68f)
                .setDuration(75)
                .setInterpolator(
                        new android.view.animation.DecelerateInterpolator()
                )
                .start();
    }

    private void animateButtonRelease(
            final View view
    ) {

        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(150)
                .setInterpolator(
                        new android.view.animation.OvershootInterpolator(1.05f)
                )
                .start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 保持当前稳定的全屏方案。
        // 不使用 WindowInsetsController。
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        sp = getSharedPreferences("meta", MODE_PRIVATE);

        booksDir = new File(getFilesDir(), "books");
        booksDir.mkdirs();

        setTitle("GPT漫画");
        showShelf();
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }

    private void base() {

        root = new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(BG);

        setContentView(root);
    }

    private GradientDrawable roundedBg(
            int color,
            int radius
    ) {

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(color);
        bg.setCornerRadius(dp(radius));

        return bg;
    }

    private GradientDrawable strokeBg(
            int color,
            int strokeColor,
            int strokeWidth,
            int radius
    ) {

        GradientDrawable bg =
                roundedBg(color, radius);

        bg.setStroke(
                dp(strokeWidth),
                strokeColor
        );

        return bg;
    }

    private TextView makeLabel(
            String text,
            float size,
            int color
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        return view;
    }

    private void addSpacer(
            LinearLayout parent,
            int height
    ) {

        View spacer =
                new View(this);

        parent.addView(
                spacer,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
    }

    private String getDisplayName(File book) {

        return sp.getString(
                "display_name_" + book.getName(),
                book.getName()
        );
    }

    private void setDisplayName(
            File book,
            String name
    ) {

        sp.edit()
                .putString(
                        "display_name_" + book.getName(),
                        name
                )
                .apply();
    }

    private File getCoverDir(File book) {

        return new File(
                book,
                "cover"
        );
    }

    private File getCoverFile(File book) {

        return new File(
                getCoverDir(book),
                "cover.jpg"
        );
    }

    private boolean hasCover(File book) {

        return getCoverFile(book).exists();
    }

    // =========================================================
    // 书架
    // =========================================================

    private void showShelf() {

        currentBook = null;
        base();

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.VERTICAL
        );

        header.setGravity(
                Gravity.BOTTOM
        );

        header.setPadding(
                dp(20),
                dp(12),
                dp(20),
                dp(12)
        );

        header.setBackgroundColor(BG);

        TextView title =
                makeLabel(
                        "GPT漫画",
                        24,
                        TEXT_PRIMARY
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        TextView subtitle =
                makeLabel(
                        "你的漫画空间",
                        12,
                        TEXT_MUTED
                );

        subtitle.setPadding(
                0,
                dp(3),
                0,
                0
        );

        header.addView(
                subtitle,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(82)
                )
        );

        animateAppear(header, 0);

        FrameLayout shelfContainer =
                new FrameLayout(this);

        shelfContainer.setBackgroundColor(BG);

        root.addView(
                shelfContainer,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        ScrollView shelfScroll =
                new ScrollView(this);

        shelfScroll.setBackgroundColor(BG);
        shelfScroll.setFillViewport(true);
        shelfScroll.setClipToPadding(false);

        shelf =
                new LinearLayout(this);

        shelf.setOrientation(
                LinearLayout.VERTICAL
        );

        shelf.setPadding(
                dp(10),
                dp(4),
                dp(10),
                dp(110)
        );

        shelf.setBackgroundColor(BG);

        shelfScroll.addView(shelf);

        shelfContainer.addView(
                shelfScroll,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        TextView create =
                new TextView(this);

        create.setText("+");
        create.setTextSize(25);
        create.setTextColor(TEXT_PRIMARY);
        create.setGravity(Gravity.CENTER);

        GradientDrawable createBg =
                roundedBg(
                        Color.rgb(42, 42, 46),
                        20
                );

        createBg.setStroke(
                dp(1),
                Color.rgb(62, 62, 67)
        );

        create.setBackground(createBg);

        create.setElevation(dp(8));

        create.setOnTouchListener(
                (v, event) -> {

                    if (
                            event.getAction()
                                    == MotionEvent.ACTION_DOWN
                    ) {

                        animateButtonPress(v);

                    } else if (
                            event.getAction()
                                    == MotionEvent.ACTION_UP
                                    || event.getAction()
                                    == MotionEvent.ACTION_CANCEL
                    ) {

                        animateButtonRelease(v);
                    }

                    return false;
                }
        );

        create.setOnClickListener(
                v -> createBook()
        );

        FrameLayout.LayoutParams createParams =
                new FrameLayout.LayoutParams(
                        dp(58),
                        dp(58),
                        Gravity.BOTTOM | Gravity.END
                );

        createParams.setMargins(
                dp(12),
                dp(12),
                dp(20),
                dp(20)
        );

        shelfContainer.addView(
                create,
                createParams
        );

        animateAppear(create, 180);

        File[] books =
                booksDir.listFiles();

        if (books == null) {

            showEmptyShelf();
            return;
        }

        ArrayList<File> list =
                new ArrayList<>();

        for (File book : books) {

            if (book.isDirectory()) {
                list.add(book);
            }
        }

        if (list.isEmpty()) {

            showEmptyShelf();
            return;
        }

        loadShelfOrder(list);
        rebuildShelf(list);
    }

    private void showEmptyShelf() {

        LinearLayout empty =
                new LinearLayout(this);

        empty.setOrientation(
                LinearLayout.VERTICAL
        );

        empty.setGravity(
                Gravity.CENTER
        );

        TextView icon =
                makeLabel(
                        "+",
                        32,
                        TEXT_MUTED
                );

        icon.setGravity(Gravity.CENTER);

        GradientDrawable iconBg =
                roundedBg(
                        SURFACE,
                        22
                );

        iconBg.setStroke(
                dp(1),
                Color.rgb(48, 48, 52)
        );

        icon.setBackground(iconBg);

        empty.addView(
                icon,
                new LinearLayout.LayoutParams(
                        dp(64),
                        dp(64)
                )
        );

        TextView title =
                makeLabel(
                        "书库还是空的",
                        16,
                        TEXT_PRIMARY
                );

        title.setGravity(Gravity.CENTER);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        titleParams.setMargins(
                dp(20),
                dp(18),
                dp(20),
                dp(4)
        );

        empty.addView(
                title,
                titleParams
        );

        TextView hint =
                makeLabel(
                        "点击右下角 ＋ 添加第一本漫画",
                        13,
                        TEXT_MUTED
                );

        hint.setGravity(Gravity.CENTER);

        empty.addView(
                hint,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        shelf.addView(
                empty,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(340)
                )
        );

        animateAppear(icon, 80);
        animateAppear(title, 150);
        animateAppear(hint, 210);
    }

    private void loadShelfOrder(
            ArrayList<File> list
    ) {

        String saved =
                sp.getString(
                        "shelf_order",
                        ""
                );

        if (saved.isEmpty()) {

            Collections.sort(
                    list,
                    (a, b) ->
                            naturalCompare(
                                    a.getName(),
                                    b.getName()
                            )
            );

            return;
        }

        ArrayList<File> ordered =
                new ArrayList<>();

        Set<String> used =
                new HashSet<>();

        String[] names =
                saved.split("\\|");

        for (String name : names) {

            for (File book : list) {

                if (
                        book.getName()
                                .equals(name)
                ) {

                    ordered.add(book);
                    used.add(name);
                    break;
                }
            }
        }

        ArrayList<File> remaining =
                new ArrayList<>();

        for (File book : list) {

            if (!used.contains(book.getName())) {
                remaining.add(book);
            }
        }

        Collections.sort(
                remaining,
                (a, b) ->
                        naturalCompare(
                                a.getName(),
                                b.getName()
                        )
        );

        ordered.addAll(remaining);

        list.clear();
        list.addAll(ordered);

        saveShelfOrder(list);
    }

    private void saveShelfOrder(
            ArrayList<File> books
    ) {

        StringBuilder order =
                new StringBuilder();

        for (File book : books) {

            if (order.length() > 0) {
                order.append("|");
            }

            order.append(
                    book.getName()
            );
        }

        sp.edit()
                .putString(
                        "shelf_order",
                        order.toString()
                )
                .apply();
    }

    private void rebuildShelf(
            ArrayList<File> books
    ) {

        shelf.removeAllViews();

        for (
                int i = 0;
                i < books.size();
                i += 2
        ) {

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setGravity(
                    Gravity.TOP
            );

            shelf.addView(
                    row,
                    new LinearLayout.LayoutParams(
                            -1,
                            -2
                    )
            );

            addBook(
                    row,
                    books.get(i)
            );

            if (i + 1 < books.size()) {

                addBook(
                        row,
                        books.get(i + 1)
                );

            } else {

                View empty =
                        new View(this);

                row.addView(
                        empty,
                        new LinearLayout.LayoutParams(
                                0,
                                1,
                                1
                        )
                );
            }

            animateAppear(
                    row,
                    (i / 2) * 35L
            );
        }
    }

    private void addBook(
            LinearLayout row,
            final File book
    ) {

        LinearLayout item =
                new LinearLayout(this);

        item.setOrientation(
                LinearLayout.VERTICAL
        );

        item.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        item.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(8)
        );

        GradientDrawable itemBg =
                roundedBg(
                        SURFACE,
                        10
                );

        item.setBackground(itemBg);

        row.addView(
                item,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        ImageView coverView =
                new ImageView(this);

        coverView.setAdjustViewBounds(true);

        coverView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        coverView.setBackgroundColor(
                Color.rgb(18, 18, 20)
        );

        if (hasCover(book)) {

            coverView.setImageURI(
                    Uri.fromFile(
                            getCoverFile(book)
                    )
            );

        } else {

            LinearLayout.LayoutParams
                    noCoverParams =
                    new LinearLayout.LayoutParams(
                            -1,
                            dp(190)
                    );

            noCoverParams.setMargins(
                    dp(2),
                    dp(2),
                    dp(2),
                    dp(2)
            );

            TextView noCover =
                    makeLabel(
                            "暂无封面",
                            13,
                            TEXT_MUTED
                    );

            noCover.setGravity(
                    Gravity.CENTER
            );

            noCover.setBackground(
                    roundedBg(
                            Color.rgb(18, 18, 20),
                            8
                    )
            );

            item.addView(
                    noCover,
                    noCoverParams
            );

            setupBookTouch(
                    item,
                    book
            );

            return;
        }

        LinearLayout.LayoutParams coverParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        coverParams.setMargins(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        item.addView(
                coverView,
                coverParams
        );

        setupBookTouch(
                item,
                book
        );
    }

    private void setupBookTouch(
            final View item,
            final File book
    ) {

        item.setOnClickListener(
                v -> {

                    if (isDraggingBook) {
                        return;
                    }

                    v.animate()
                            .scaleX(0.975f)
                            .scaleY(0.975f)
                            .alpha(0.92f)
                            .setDuration(70)
                            .setInterpolator(
                                    new android.view.animation.DecelerateInterpolator()
                            )
                            .withEndAction(() ->
                                    v.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .alpha(1f)
                                            .setDuration(150)
                                            .setInterpolator(
                                                    new android.view.animation.OvershootInterpolator(
                                                            1.05f
                                                    )
                                            )
                                            .start()
                            )
                            .start();

                    showBookMenu(book);
                }
        );

        item.setOnLongClickListener(
                v -> {

                    isDraggingBook = true;
                    draggingBook = book;

                    v.animate()
                            .scaleX(1.035f)
                            .scaleY(1.035f)
                            .setDuration(120)
                            .setInterpolator(
                                    new android.view.animation.DecelerateInterpolator()
                            )
                            .start();

                    v.setAlpha(0.82f);

                    View.DragShadowBuilder shadow =
                            new View.DragShadowBuilder(v);

                    if (
                            android.os.Build.VERSION.SDK_INT
                                    >= 24
                    ) {

                        v.startDragAndDrop(
                                null,
                                shadow,
                                book,
                                0
                        );

                    } else {

                        v.startDrag(
                                null,
                                shadow,
                                book,
                                0
                        );
                    }

                    return true;
                }
        );

        item.setOnDragListener(
                (v, event) -> {

                    switch (
                            event.getAction()
                    ) {

                        case android.view.DragEvent.ACTION_DRAG_STARTED:

                            return event.getLocalState()
                                    instanceof File;

                        case android.view.DragEvent.ACTION_DRAG_ENTERED:

                            if (
                                    draggingBook != null
                                            && draggingBook != book
                            ) {

                                v.animate()
                                        .scaleX(0.97f)
                                        .scaleY(0.97f)
                                        .setDuration(100)
                                        .start();
                            }

                            return true;

                        case android.view.DragEvent.ACTION_DRAG_EXITED:

                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();

                            return true;

                        case android.view.DragEvent.ACTION_DROP:

                            if (
                                    draggingBook != null
                                            && draggingBook != book
                            ) {

                                swapShelfBooks(
                                        draggingBook,
                                        book
                                );
                            }

                            return true;

                        case android.view.DragEvent.ACTION_DRAG_ENDED:

                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .alpha(1f)
                                    .setDuration(120)
                                    .start();

                            isDraggingBook = false;
                            draggingBook = null;

                            return true;
                    }

                    return true;
                }
        );
    }

    private void swapShelfBooks(
            File fromBook,
            File toBook
    ) {

        File[] files =
                booksDir.listFiles();

        if (files == null) {
            return;
        }

        ArrayList<File> books =
                new ArrayList<>();

        for (File file : files) {

            if (file.isDirectory()) {
                books.add(file);
            }
        }

        loadShelfOrder(books);

        int from =
                books.indexOf(fromBook);

        int to =
                books.indexOf(toBook);

        if (
                from < 0
                        || to < 0
                        || from == to
        ) {

            return;
        }

        Collections.swap(
                books,
                from,
                to
        );

        saveShelfOrder(books);
        rebuildShelf(books);

        isDraggingBook = false;
        draggingBook = null;
    }

    // =========================================================
    // 书籍操作菜单
    // =========================================================

    private void showBookMenu(
            final File book
    ) {

        LinearLayout panel =
                new LinearLayout(this);

        panel.setOrientation(
                LinearLayout.VERTICAL
        );

        panel.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(10)
        );

        panel.setBackground(
                roundedBg(
                        Color.rgb(20, 20, 23),
                        20
                )
        );

        if (hasCover(book)) {

            ImageView cover =
                    new ImageView(this);

            cover.setAdjustViewBounds(true);

            cover.setScaleType(
                    ImageView.ScaleType.FIT_CENTER
            );

            cover.setImageURI(
                    Uri.fromFile(
                            getCoverFile(book)
                    )
            );

            panel.addView(
                    cover,
                    new LinearLayout.LayoutParams(
                            -1,
                            dp(230)
                    )
            );
        }

        TextView name =
                makeLabel(
                        getDisplayName(book),
                        19,
                        TEXT_PRIMARY
                );

        name.setGravity(Gravity.CENTER);

        name.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        name.setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(14)
        );

        panel.addView(
                name,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        TextView read =
                modernAction(
                        "进入阅读",
                        TEXT_PRIMARY,
                        SURFACE_3
                );

        panel.addView(
                read,
                actionParams(52, 5)
        );

        TextView manage =
                modernAction(
                        "管理书本",
                        TEXT_SECONDARY,
                        SURFACE_2
                );

        panel.addView(
                manage,
                actionParams(52, 5)
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(panel)
                        .setNegativeButton(
                                "取消",
                                null
                        )
                        .create();

        read.setOnClickListener(
                v -> {

                    dialog.dismiss();
                    reader(book);
                }
        );

        manage.setOnClickListener(
                v -> {

                    dialog.dismiss();
                    manageBook(book);
                }
        );

        dialog.setOnShowListener(
                d -> {

                    if (dialog.getWindow() != null) {

                        dialog.getWindow()
                                .setBackgroundDrawableResource(
                                        android.R.color.transparent
                                );
                    }
                }
        );

        dialog.show();

        animateDialogIn(panel);
    }

    private LinearLayout.LayoutParams actionParams(
            int height,
            int margin
    ) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(height)
                );

        params.setMargins(
                0,
                dp(margin),
                0,
                dp(margin)
        );

        return params;
    }

    private TextView modernAction(
            String text,
            int textColor,
            int background
    ) {

        TextView view =
                makeLabel(
                        text,
                        15,
                        textColor
                );

        view.setGravity(Gravity.CENTER);

        view.setBackground(
                strokeBg(
                        background,
                        Color.rgb(52, 52, 57),
                        1,
                        12
                )
        );

        view.setOnTouchListener(
                (v, event) -> {

                    if (
                            event.getAction()
                                    == MotionEvent.ACTION_DOWN
                    ) {

                        animateButtonPress(v);

                    } else if (
                            event.getAction()
                                    == MotionEvent.ACTION_UP
                                    || event.getAction()
                                    == MotionEvent.ACTION_CANCEL
                    ) {

                        animateButtonRelease(v);
                    }

                    return false;
                }
        );

        return view;
    }

    // =========================================================
    // 创建书本
    // =========================================================

    private void createBook() {

        int number = 1;

        while (
                new File(
                        booksDir,
                        "书本 " + number
                ).exists()
        ) {

            number++;
        }

        File book =
                new File(
                        booksDir,
                        "书本 " + number
                );

        if (!book.mkdirs()) {

            Toast.makeText(
                    this,
                    "创建书本失败",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        getCoverDir(book).mkdirs();

        // 创建后只返回书架。
        // 不自动进入阅读器。
        showShelf();
    }

    // =========================================================
    // 管理书本
    // =========================================================

    private void manageBook(
            final File book
    ) {

        LinearLayout panel =
                new LinearLayout(this);

        panel.setOrientation(
                LinearLayout.VERTICAL
        );

        panel.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(10)
        );

        panel.setBackground(
                roundedBg(
                        Color.rgb(20, 20, 23),
                        20
                )
        );

        EditText nameInput =
                new EditText(this);

        nameInput.setText(
                getDisplayName(book)
        );

        nameInput.setTextColor(TEXT_PRIMARY);

        nameInput.setHintTextColor(TEXT_MUTED);

        nameInput.setSingleLine(true);

        nameInput.setTextSize(15);

        nameInput.setPadding(
                dp(14),
                0,
                dp(14),
                0
        );

        nameInput.setBackground(
                strokeBg(
                        SURFACE_2,
                        Color.rgb(54, 54, 59),
                        1,
                        12
                )
        );

        panel.addView(
                nameInput,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                )
        );

        TextView rename =
                modernAction(
                        "保存书名",
                        TEXT_PRIMARY,
                        SURFACE_3
                );

        panel.addView(
                rename,
                actionParams(50, 10)
        );

        TextView changeCover =
                modernAction(
                        "更换封面",
                        TEXT_SECONDARY,
                        SURFACE_2
                );

        panel.addView(
                changeCover,
                actionParams(50, 5)
        );

        TextView delete =
                modernAction(
                        "删除书本",
                        Color.rgb(220, 175, 175),
                        Color.rgb(48, 31, 32)
                );

        panel.addView(
                delete,
                actionParams(50, 5)
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(panel)
                        .setNegativeButton(
                                "取消",
                                null
                        )
                        .create();

        rename.setOnClickListener(
                v -> {

                    String newName =
                            nameInput.getText()
                                    .toString()
                                    .trim();

                    if (newName.isEmpty()) {

                        Toast.makeText(
                                this,
                                "书名不能为空",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    setDisplayName(
                            book,
                            newName
                    );

                    dialog.dismiss();
                    showShelf();
                }
        );

        changeCover.setOnClickListener(
                v -> {

                    pendingCoverDisplayName =
                            book.getName();

                    Intent intent =
                            new Intent(
                                    Intent.ACTION_OPEN_DOCUMENT
                            );

                    intent.setType("image/*");

                    intent.addCategory(
                            Intent.CATEGORY_OPENABLE
                    );

                    startActivityForResult(
                            intent,
                            REQ_PICK_COVER
                    );

                    dialog.dismiss();
                }
        );

        delete.setOnClickListener(
                v -> {

                    new AlertDialog.Builder(this)
                            .setTitle("删除书本")
                            .setMessage(
                                    "确定删除这本书以及其中的全部图片吗？"
                            )
                            .setNegativeButton(
                                    "取消",
                                    null
                            )
                            .setPositiveButton(
                                    "删除",
                                    (d, which) -> {

                                        deleteBook(book);

                                        currentBook = null;

                                        // 关键：
                                        // 删除成功后关闭原来的
                                        // “管理书本”窗口。
                                        dialog.dismiss();

                                        showShelf();
                                    }
                            )
                            .show();
                }
        );

        dialog.show();

        animateDialogIn(panel);
    }

    private boolean deleteBook(
            File book
    ) {

        boolean success =
                deleteRecursive(book);

        String id =
                book.getName();

        sp.edit()
                .remove("display_name_" + id)
                .remove("order_" + id)
                .remove("y_" + id)
                .apply();

        removeFromShelfOrder(id);

        return success;
    }

    private void removeFromShelfOrder(
            String bookName
    ) {

        String saved =
                sp.getString(
                        "shelf_order",
                        ""
                );

        if (saved.isEmpty()) {
            return;
        }

        StringBuilder result =
                new StringBuilder();

        String[] names =
                saved.split("\\|");

        for (String name : names) {

            if (
                    name.isEmpty()
                            || name.equals(bookName)
            ) {
                continue;
            }

            if (result.length() > 0) {
                result.append("|");
            }

            result.append(name);
        }

        if (result.length() == 0) {

            sp.edit()
                    .remove("shelf_order")
                    .apply();

        } else {

            sp.edit()
                    .putString(
                            "shelf_order",
                            result.toString()
                    )
                    .apply();
        }
    }

    private boolean deleteRecursive(
            File file
    ) {

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {

                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }

        return file.delete();
    }

    // =========================================================
    // 图片列表
    // =========================================================

    private ArrayList<File> images(
            File book
    ) {

        ArrayList<File> result =
                new ArrayList<>();

        File[] files =
                book.listFiles();

        if (files == null) {
            return result;
        }

        for (File file : files) {

            if (!file.isFile()) {
                continue;
            }

            String name =
                    file.getName()
                            .toLowerCase();

            if (
                    name.endsWith(".jpg")
                            || name.endsWith(".jpeg")
                            || name.endsWith(".png")
                            || name.endsWith(".webp")
                            || name.endsWith(".gif")
            ) {

                result.add(file);
            }
        }

        Collections.sort(
                result,
                (a, b) ->
                        naturalCompare(
                                a.getName(),
                                b.getName()
                        )
        );

        String saved =
                sp.getString(
                        "order_" + book.getName(),
                        ""
                );

        if (saved.isEmpty()) {
            return result;
        }

        ArrayList<File> ordered =
                new ArrayList<>();

        Set<String> used =
                new HashSet<>();

        String[] names =
                saved.split("\\|");

        for (String name : names) {

            for (File file : result) {

                if (
                        file.getName()
                                .equals(name)
                ) {

                    ordered.add(file);
                    used.add(name);
                    break;
                }
            }
        }

        for (File file : result) {

            if (!used.contains(
                    file.getName()
            )) {

                ordered.add(file);
            }
        }

        return ordered;
    }

    private int naturalCompare(
            String a,
            String b
    ) {

        int ia = 0;
        int ib = 0;

        while (
                ia < a.length()
                        && ib < b.length()
        ) {

            char ca = a.charAt(ia);
            char cb = b.charAt(ib);

            if (
                    Character.isDigit(ca)
                            && Character.isDigit(cb)
            ) {

                int sa = ia;
                int sb = ib;

                while (
                        ia < a.length()
                                && Character.isDigit(
                                a.charAt(ia)
                        )
                ) {
                    ia++;
                }

                while (
                        ib < b.length()
                                && Character.isDigit(
                                b.charAt(ib)
                        )
                ) {
                    ib++;
                }

                try {

                    long na =
                            Long.parseLong(
                                    a.substring(
                                            sa,
                                            ia
                                    )
                            );

                    long nb =
                            Long.parseLong(
                                    b.substring(
                                            sb,
                                            ib
                                    )
                            );

                    if (na != nb) {

                        return Long.compare(
                                na,
                                nb
                        );
                    }

                } catch (Exception e) {

                    int compare =
                            a.substring(
                                    sa,
                                    ia
                            ).compareTo(
                                    b.substring(
                                            sb,
                                            ib
                                    )
                            );

                    if (compare != 0) {
                        return compare;
                    }
                }

            } else {

                char la =
                        Character.toLowerCase(ca);

                char lb =
                        Character.toLowerCase(cb);

                if (la != lb) {

                    return Character.compare(
                            la,
                            lb
                    );
                }

                ia++;
                ib++;
            }
        }

        return Integer.compare(
                a.length(),
                b.length()
        );
    }

    // =========================================================
    // 阅读器
    // =========================================================

    private void reader(
            final File book
    ) {

        currentBook = book;
        base();

        FrameLayout readerContainer =
                new FrameLayout(this);

        readerContainer.setBackgroundColor(
                Color.BLACK
        );

        root.addView(
                readerContainer,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setBackgroundColor(
                Color.BLACK
        );

        LinearLayout imageList =
                new LinearLayout(this);

        imageList.setOrientation(
                LinearLayout.VERTICAL
        );

        imageList.setBackgroundColor(
                Color.BLACK
        );

        ArrayList<File> files =
                images(book);

        for (File file : files) {

            ImageView image =
                    new ImageView(this);

            image.setAdjustViewBounds(true);

            image.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            image.setImageURI(
                    Uri.fromFile(file)
            );

            imageList.addView(
                    image,
                    new LinearLayout.LayoutParams(
                            -1,
                            -2
                    )
            );
        }

        scroll.addView(imageList);

        readerContainer.addView(
                scroll,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        // -----------------------------------------------------
        // 阅读器控制层
        // -----------------------------------------------------

        final LinearLayout controls =
                new LinearLayout(this);

        controls.setOrientation(
                LinearLayout.HORIZONTAL
        );

        controls.setGravity(
                Gravity.CENTER
        );

        controls.setPadding(
                dp(7),
                dp(7),
                dp(7),
                dp(7)
        );

        controls.setBackground(
                roundedBg(
                        Color.argb(
                                238,
                                27,
                                27,
                                30
                        ),
                        18
                )
        );

        controls.setElevation(dp(8));

        TextView sort =
                readerButton("图片排序");

        TextView add =
                readerButton("添加图片");

        TextView manage =
                readerButton("管理图片");

        TextView top =
                readerButton("回顶部");

        controls.addView(sort);
        controls.addView(add);
        controls.addView(manage);
        controls.addView(top);

        FrameLayout.LayoutParams controlParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -2,
                        Gravity.BOTTOM
                );

        controlParams.setMargins(
                dp(10),
                dp(10),
                dp(10),
                dp(14)
        );

        readerContainer.addView(
                controls,
                controlParams
        );

        controls.setVisibility(
                View.GONE
        );

        final long[] lastTap =
                {0};

        scroll.setOnTouchListener(
                (v, event) -> {

                    if (
                            event.getAction()
                                    == MotionEvent.ACTION_UP
                    ) {

                        long now =
                                System.currentTimeMillis();

                        if (
                                now - lastTap[0]
                                        < 350
                        ) {

                            if (
                                    controls.getVisibility()
                                            == View.VISIBLE
                            ) {

                                controls.animate()
                                        .alpha(0f)
                                        .translationY(dp(16))
                                        .setDuration(130)
                                        .setInterpolator(
                                                new android.view.animation.AccelerateInterpolator(
                                                        1.2f
                                                )
                                        )
                                        .withEndAction(() -> {

                                            controls.setVisibility(
                                                    View.GONE
                                            );

                                            controls.setTranslationY(0f);
                                        })
                                        .start();

                            } else {

                                animateControlIn(controls);

                                controls.setVisibility(
                                        View.VISIBLE
                                );
                            }
                        }

                        lastTap[0] = now;
                    }

                    return false;
                }
        );

        // 不恢复上次阅读位置。
        // 每次进入 reader() 都从顶部开始。

        sort.setOnClickListener(
                v -> sortImages(book)
        );

        add.setOnClickListener(
                v -> pickImages()
        );

        manage.setOnClickListener(
                v -> manageImages(book)
        );

        top.setOnClickListener(
                v -> scroll.smoothScrollTo(
                        0,
                        0
                )
        );
    }

    private TextView readerButton(
            String text
    ) {

        TextView button =
                makeLabel(
                        text,
                        13,
                        TEXT_PRIMARY
                );

        button.setGravity(
                Gravity.CENTER
        );

        button.setPadding(
                dp(7),
                dp(10),
                dp(7),
                dp(10)
        );

        button.setBackground(
                roundedBg(
                        Color.rgb(43, 43, 47),
                        11
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(43),
                        1
                );

        params.setMargins(
                dp(3),
                0,
                dp(3),
                0
        );

        button.setLayoutParams(params);

        button.setOnTouchListener(
                (v, event) -> {

                    if (
                            event.getAction()
                                    == MotionEvent.ACTION_DOWN
                    ) {

                        animateButtonPress(v);

                    } else if (
                            event.getAction()
                                    == MotionEvent.ACTION_UP
                                    || event.getAction()
                                    == MotionEvent.ACTION_CANCEL
                    ) {

                        animateButtonRelease(v);
                    }

                    return false;
                }
        );

        return button;
    }

    // =========================================================
    // 添加图片
    // =========================================================

    private void pickImages() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("image/*");

        intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                true
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        startActivityForResult(
                intent,
                REQ_ADD_IMAGES
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                resultCode != RESULT_OK
                        || data == null
        ) {

            return;
        }

        if (
                requestCode == REQ_ADD_IMAGES
        ) {

            if (currentBook == null) {
                return;
            }

            if (
                    data.getClipData() != null
            ) {

                int count =
                        data.getClipData()
                                .getItemCount();

                for (
                        int i = 0;
                        i < count;
                        i++
                ) {

                    Uri uri =
                            data.getClipData()
                                    .getItemAt(i)
                                    .getUri();

                    copyImage(
                            uri,
                            currentBook
                    );
                }

            } else if (
                    data.getData() != null
            ) {

                copyImage(
                        data.getData(),
                        currentBook
                );
            }

            reader(currentBook);

        } else if (
                requestCode == REQ_PICK_COVER
        ) {

            if (
                    pendingCoverDisplayName
                            == null
            ) {

                return;
            }

            File book =
                    new File(
                            booksDir,
                            pendingCoverDisplayName
                    );

            copyCover(
                    data.getData(),
                    book
            );

            pendingCoverDisplayName = null;

            showShelf();
        }
    }

    private String getFileName(
            Uri uri
    ) {

        Cursor cursor =
                getContentResolver()
                        .query(
                                uri,
                                null,
                                null,
                                null,
                                null
                        );

        if (cursor != null) {

            try {

                int index =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                if (
                        index >= 0
                                && cursor.moveToFirst()
                ) {

                    String name =
                            cursor.getString(index);

                    if (
                            name != null
                                    && !name.isEmpty()
                    ) {

                        return name;
                    }
                }

            } finally {

                cursor.close();
            }
        }

        return "image_" +
                System.currentTimeMillis() +
                ".jpg";
    }

    private void copyImage(
            Uri uri,
            File book
    ) {

        try {

            String originalName =
                    getFileName(uri);

            String extension = ".jpg";

            int dot =
                    originalName.lastIndexOf('.');

            if (dot >= 0) {

                extension =
                        originalName.substring(dot);
            }

            String baseName =
                    originalName;

            if (dot >= 0) {

                baseName =
                        originalName.substring(
                                0,
                                dot
                        );
            }

            File target =
                    new File(
                            book,
                            originalName
                    );

            int count = 1;

            while (target.exists()) {

                target =
                        new File(
                                book,
                                baseName
                                        + "_"
                                        + count
                                        + extension
                        );

                count++;
            }

            InputStream in =
                    getContentResolver()
                            .openInputStream(uri);

            if (in == null) {
                return;
            }

            FileOutputStream out =
                    new FileOutputStream(target);

            byte[] buffer =
                    new byte[8192];

            int len;

            while (
                    (len = in.read(buffer))
                            != -1
            ) {

                out.write(
                        buffer,
                        0,
                        len
                );
            }

            out.flush();
            out.close();
            in.close();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "图片添加失败",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void copyCover(
            Uri uri,
            File book
    ) {

        if (uri == null) {
            return;
        }

        try {

            getCoverDir(book).mkdirs();

            InputStream in =
                    getContentResolver()
                            .openInputStream(uri);

            if (in == null) {
                return;
            }

            FileOutputStream out =
                    new FileOutputStream(
                            getCoverFile(book)
                    );

            byte[] buffer =
                    new byte[8192];

            int len;

            while (
                    (len = in.read(buffer))
                            != -1
            ) {

                out.write(
                        buffer,
                        0,
                        len
                );
            }

            out.flush();
            out.close();
            in.close();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "封面更换失败",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // 图片排序
    // =========================================================

    private void sortImages(
            File book
    ) {

        manualSortImages(book);
    }

    private void saveImageOrder(
            File book,
            ArrayList<File> list
    ) {

        StringBuilder order =
                new StringBuilder();

        for (File file : list) {

            if (order.length() > 0) {
                order.append("|");
            }

            order.append(
                    file.getName()
            );
        }

        sp.edit()
                .putString(
                        "order_" + book.getName(),
                        order.toString()
                )
                .apply();
    }

    // =========================================================
    // 手动图片排序
    // =========================================================

    private void manualSortImages(
            final File book
    ) {

        final ArrayList<File> order =
                images(book);

        if (order.size() < 2) {

            Toast.makeText(
                    this,
                    "图片数量不足",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final LinearLayout listContainer =
                new LinearLayout(this);

        listContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        listContainer.setPadding(
                dp(6),
                dp(3),
                dp(6),
                dp(6)
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.setBackgroundColor(
                Color.rgb(16, 16, 18)
        );

        scroll.addView(listContainer);

        LinearLayout panel =
                new LinearLayout(this);

        panel.setOrientation(
                LinearLayout.VERTICAL
        );

        panel.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(4)
        );

        panel.setBackgroundColor(
                Color.rgb(20, 20, 23)
        );

        TextView hint =
                makeLabel(
                        "长按图片、文件名或右侧图标拖动调整顺序",
                        13,
                        TEXT_MUTED
                );

        hint.setPadding(
                dp(7),
                dp(1),
                dp(7),
                dp(10)
        );

        panel.addView(
                hint,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        panel.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(430)
                )
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("图片排序")
                        .setView(panel)
                        .setNegativeButton(
                                "取消",
                                null
                        )
                        .setPositiveButton(
                                "完成",
                                null
                        )
                        .create();

        final Runnable[] refresh =
                new Runnable[1];

        final boolean[] firstRender =
                {true};

        refresh[0] =
                new Runnable() {

                    @Override
                    public void run() {

                        listContainer.removeAllViews();

                        for (
                                int i = 0;
                                i < order.size();
                                i++
                        ) {

                            final File file =
                                    order.get(i);

                            final LinearLayout row =
                                    new LinearLayout(
                                            MainActivity.this
                                    );

                            row.setOrientation(
                                    LinearLayout.HORIZONTAL
                            );

                            row.setGravity(
                                    Gravity.CENTER_VERTICAL
                            );

                            row.setPadding(
                                    dp(6),
                                    dp(2),
                                    dp(6),
                                    dp(2)
                            );

                            row.setBackground(
                                    roundedBg(
                                            SURFACE_2,
                                            9
                                    )
                            );

                            // -------------------------------------------------
                            // 序号
                            // -------------------------------------------------

                            TextView number =
                                    makeLabel(
                                            String.valueOf(i + 1),
                                            12,
                                            TEXT_MUTED
                                    );

                            number.setGravity(
                                    Gravity.CENTER
                            );

                            row.addView(
                                    number,
                                    new LinearLayout.LayoutParams(
                                            dp(30),
                                            dp(42)
                                    )
                            );

                            // -------------------------------------------------
                            // 小缩略图
                            // 只用于辨认图片
                            // -------------------------------------------------

                            ImageView thumbnail =
                                    new ImageView(
                                            MainActivity.this
                                    );

                            thumbnail.setScaleType(
                                    ImageView.ScaleType.CENTER_CROP
                            );

                            thumbnail.setImageURI(
                                    Uri.fromFile(file)
                            );

                            thumbnail.setBackground(
                                    roundedBg(
                                            Color.rgb(18, 18, 20),
                                            5
                                    )
                            );

                            thumbnail.setClipToOutline(true);

                            LinearLayout.LayoutParams
                                    thumbnailParams =
                                    new LinearLayout.LayoutParams(
                                            dp(32),
                                            dp(40)
                                    );

                            thumbnailParams.setMargins(
                                    dp(2),
                                    0,
                                    dp(9),
                                    0
                            );

                            row.addView(
                                    thumbnail,
                                    thumbnailParams
                            );

                            // -------------------------------------------------
                            // 文件名
                            // -------------------------------------------------

                            TextView name =
                                    makeLabel(
                                            file.getName(),
                                            14,
                                            TEXT_PRIMARY
                                    );

                            name.setGravity(
                                    Gravity.CENTER_VERTICAL
                            );

                            name.setSingleLine(true);

                            name.setEllipsize(
                                    android.text.TextUtils
                                            .TruncateAt.MIDDLE
                            );

                            row.addView(
                                    name,
                                    new LinearLayout.LayoutParams(
                                            0,
                                            dp(42),
                                            1
                                    )
                            );

                            // -------------------------------------------------
                            // 拖动图标
                            // -------------------------------------------------

                            TextView drag =
                                    makeLabel(
                                            "☰",
                                            18,
                                            TEXT_MUTED
                                    );

                            drag.setGravity(
                                    Gravity.CENTER
                            );

                            row.addView(
                                    drag,
                                    new LinearLayout.LayoutParams(
                                            dp(40),
                                            dp(42)
                                    )
                            );

                            LinearLayout.LayoutParams
                                    rowParams =
                                    new LinearLayout.LayoutParams(
                                            -1,
                                            dp(50)
                                    );

                            rowParams.setMargins(
                                    0,
                                    dp(2),
                                    0,
                                    dp(2)
                            );

                            listContainer.addView(
                                    row,
                                    rowParams
                            );

                            if (firstRender[0]) {

                                animateAppear(
                                        row,
                                        Math.min(
                                                i * 16L,
                                                160L
                                        )
                                );
                            }

                            // -------------------------------------------------
                            // 长按开始拖动
                            // -------------------------------------------------

                            View.OnLongClickListener
                                    startDrag =
                                    v -> {

                                        row.animate()
                                                .scaleX(0.98f)
                                                .scaleY(0.98f)
                                                .alpha(0.65f)
                                                .setDuration(80)
                                                .start();

                                        View.DragShadowBuilder
                                                shadow =
                                                new View.DragShadowBuilder(
                                                        row
                                                );

                                        if (
                                                android.os.Build.VERSION.SDK_INT
                                                        >= 24
                                        ) {

                                            row.startDragAndDrop(
                                                    null,
                                                    shadow,
                                                    file,
                                                    0
                                            );

                                        } else {

                                            row.startDrag(
                                                    null,
                                                    shadow,
                                                    file,
                                                    0
                                            );
                                        }

                                        return true;
                                    };

                            row.setOnLongClickListener(
                                    startDrag
                            );

                            thumbnail.setOnLongClickListener(
                                    startDrag
                            );

                            name.setOnLongClickListener(
                                    startDrag
                            );

                            drag.setOnLongClickListener(
                                    startDrag
                            );

                            // -------------------------------------------------
                            // 拖动目标
                            // -------------------------------------------------

                            row.setOnDragListener(
                                    (v, event) -> {

                                        switch (
                                                event.getAction()
                                        ) {

                                            case android.view.DragEvent
                                                    .ACTION_DRAG_STARTED:

                                                return event
                                                        .getLocalState()
                                                        instanceof File;

                                            case android.view.DragEvent
                                                    .ACTION_DRAG_ENTERED:

                                                File entered =
                                                        (File) event
                                                                .getLocalState();

                                                if (
                                                        entered != file
                                                ) {

                                                    row.animate()
                                                            .scaleX(0.97f)
                                                            .scaleY(0.97f)
                                                            .setDuration(80)
                                                            .start();
                                                }

                                                return true;

                                            case android.view.DragEvent
                                                    .ACTION_DRAG_EXITED:

                                                row.animate()
                                                        .scaleX(1f)
                                                        .scaleY(1f)
                                                        .setDuration(80)
                                                        .start();

                                                return true;

                                            case android.view.DragEvent
                                                    .ACTION_DROP:

                                                File from =
                                                        (File) event
                                                                .getLocalState();

                                                if (
                                                        from != null
                                                                && from != file
                                                ) {

                                                    int fromIndex =
                                                            order.indexOf(
                                                                    from
                                                            );

                                                    int toIndex =
                                                            order.indexOf(
                                                                    file
                                                            );

                                                    if (
                                                            fromIndex >= 0
                                                                    && toIndex >= 0
                                                    ) {

                                                        order.remove(
                                                                fromIndex
                                                        );

                                                        if (
                                                                fromIndex
                                                                        < toIndex
                                                        ) {

                                                            toIndex--;
                                                        }

                                                        order.add(
                                                                toIndex,
                                                                from
                                                        );

                                                        firstRender[0] =
                                                                false;

                                                        refresh[0].run();
                                                    }
                                                }

                                                return true;

                                            case android.view.DragEvent
                                                    .ACTION_DRAG_ENDED:

                                                row.animate()
                                                        .scaleX(1f)
                                                        .scaleY(1f)
                                                        .alpha(1f)
                                                        .setDuration(100)
                                                        .start();

                                                return true;
                                        }

                                        return true;
                                    }
                            );
                        }

                        firstRender[0] = false;
                    }
                };

        refresh[0].run();

        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                saveImageOrder(
                                        book,
                                        order
                                );

                                dialog.dismiss();

                                reader(book);
                            }
                    );
                }
        );

        dialog.show();

        animateDialogIn(panel);
    }

    // =========================================================
    // 管理图片
    // =========================================================

    private void manageImages(
            final File book
    ) {

        LinearLayout panel =
                new LinearLayout(this);

        panel.setOrientation(
                LinearLayout.VERTICAL
        );

        panel.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        panel.setBackgroundColor(
                Color.rgb(20, 20, 23)
        );

        ScrollView scroll =
                new ScrollView(this);

        LinearLayout imageList =
                new LinearLayout(this);

        imageList.setOrientation(
                LinearLayout.VERTICAL
        );

        scroll.addView(imageList);

        panel.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(430)
                )
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("管理图片")
                        .setView(panel)
                        .setNegativeButton(
                                "关闭",
                                null
                        )
                        .create();

        final Runnable[] refreshList =
                new Runnable[1];

        refreshList[0] =
                new Runnable() {

                    @Override
                    public void run() {

                        imageList.removeAllViews();

                        ArrayList<File> current =
                                images(book);

                        for (int i = 0; i < current.size(); i++) {

                            final File file =
                                    current.get(i);

                            final File targetFile =
                                    file;

                            LinearLayout row =
                                    new LinearLayout(
                                            MainActivity.this
                                    );

                            row.setOrientation(
                                    LinearLayout.HORIZONTAL
                            );

                            row.setGravity(
                                    Gravity.CENTER_VERTICAL
                            );

                            row.setPadding(
                                    dp(5),
                                    dp(5),
                                    dp(5),
                                    dp(5)
                            );

                            row.setBackground(
                                    roundedBg(
                                            SURFACE_2,
                                            10
                                    )
                            );

                            ImageView image =
                                    new ImageView(
                                            MainActivity.this
                                    );

                            image.setScaleType(
                                    ImageView.ScaleType.CENTER_CROP
                            );

                            image.setImageURI(
                                    Uri.fromFile(file)
                            );

                            row.addView(
                                    image,
                                    new LinearLayout.LayoutParams(
                                            dp(66),
                                            dp(86)
                                    )
                            );

                            TextView fileName =
                                    makeLabel(
                                            file.getName(),
                                            13,
                                            TEXT_PRIMARY
                                    );

                            fileName.setGravity(
                                    Gravity.CENTER_VERTICAL
                            );

                            fileName.setSingleLine(true);

                            fileName.setEllipsize(
                                    android.text.TextUtils
                                            .TruncateAt.MIDDLE
                            );

                            LinearLayout.LayoutParams
                                    nameParams =
                                    new LinearLayout.LayoutParams(
                                            0,
                                            -1,
                                            1
                                    );

                            nameParams.setMargins(
                                    dp(11),
                                    0,
                                    dp(8),
                                    0
                            );

                            row.addView(
                                    fileName,
                                    nameParams
                            );

                            TextView delete =
                                    modernAction(
                                            "删除",
                                            Color.rgb(
                                                    220,
                                                    175,
                                                    175
                                            ),
                                            Color.rgb(
                                                    48,
                                                    31,
                                                    32
                                            )
                                    );

                            delete.setTextSize(13);

                            row.addView(
                                    delete,
                                    new LinearLayout.LayoutParams(
                                            dp(60),
                                            dp(42)
                                    )
                            );

                            delete.setOnClickListener(
                                    v -> {

                                        if (
                                                targetFile.delete()
                                        ) {

                                            ArrayList<File>
                                                    after =
                                                    images(book);

                                            saveImageOrder(
                                                    book,
                                                    after
                                            );

                                            dialog.dismiss();
                                            reader(book);

                                        } else {

                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "删除失败",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            );

                            LinearLayout.LayoutParams
                                    rowParams =
                                    new LinearLayout.LayoutParams(
                                            -1,
                                            dp(96)
                                    );

                            rowParams.setMargins(
                                    0,
                                    dp(3),
                                    0,
                                    dp(3)
                            );

                            imageList.addView(
                                    row,
                                    rowParams
                            );

                            animateAppear(
                                    row,
                                    Math.min(
                                            i * 16L,
                                            160L
                                    )
                            );
                        }
                    }
                };

        refreshList[0].run();

        dialog.show();

        animateDialogIn(panel);
    }

    // =========================================================
    // 返回键
    // =========================================================

    @Override
    public void onBackPressed() {

        if (currentBook != null) {

            currentBook = null;
            showShelf();

            return;
        }

        super.onBackPressed();
    }
}
