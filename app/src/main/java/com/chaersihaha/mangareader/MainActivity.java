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
import java.util.Comparator;
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

    // 书架拖动排序
    private File draggingBook = null;
    private boolean isDraggingBook = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏顶部状态栏
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
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void base() {

        root = new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.BLACK
        );

        setContentView(root);
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
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(20),
                dp(10),
                dp(20),
                dp(8)
        );

        header.setBackgroundColor(
                Color.BLACK
        );

        TextView title =
                new TextView(this);

        title.setText("GPT漫画");
        title.setTextSize(21);
        title.setTextColor(Color.WHITE);

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
                new TextView(this);

        subtitle.setText("我的书库");
        subtitle.setTextSize(12);
        subtitle.setTextColor(
                Color.rgb(125, 125, 125)
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
                        dp(68)
                )
        );

        FrameLayout shelfContainer =
                new FrameLayout(this);

        shelfContainer.setBackgroundColor(
                Color.BLACK
        );

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

        shelfScroll.setBackgroundColor(
                Color.BLACK
        );

        shelfScroll.setFillViewport(true);
        shelfScroll.setClipToPadding(false);

        shelf =
                new LinearLayout(this);

        shelf.setOrientation(
                LinearLayout.VERTICAL
        );

        shelf.setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(100)
        );

        shelf.setBackgroundColor(
                Color.BLACK
        );

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
        create.setTextSize(27);
        create.setTextColor(Color.WHITE);
        create.setGravity(Gravity.CENTER);

        GradientDrawable createBg =
                new GradientDrawable();

        createBg.setColor(
                Color.rgb(42, 42, 42)
        );

        createBg.setShape(
                GradientDrawable.OVAL
        );

        create.setBackground(createBg);

        create.setElevation(
                dp(10)
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
                dp(18),
                dp(18)
        );

        shelfContainer.addView(
                create,
                createParams
        );

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

        TextView empty =
                new TextView(this);

        empty.setText(
                "书库还是空的\n\n点击右下角 ＋ 添加第一本漫画"
        );

        empty.setTextSize(14);

        empty.setTextColor(
                Color.rgb(105, 105, 105)
        );

        empty.setGravity(
                Gravity.CENTER
        );

        shelf.addView(
                empty,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(280)
                )
        );
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

        ordered.addAll(
                remaining
        );

        list.clear();
        list.addAll(ordered);

        // 清理已经不存在的书本 ID
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

            row.setPadding(
                    dp(2),
                    dp(3),
                    dp(2),
                    dp(3)
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
                dp(5)
        );

        GradientDrawable itemBg =
                new GradientDrawable();

        itemBg.setColor(
                Color.BLACK
        );

        itemBg.setCornerRadius(
                dp(7)
        );

        item.setBackground(
                itemBg
        );

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

        coverView.setAdjustViewBounds(
                true
        );

        coverView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        coverView.setBackgroundColor(
                Color.BLACK
        );

        if (hasCover(book)) {

            coverView.setImageURI(
                    Uri.fromFile(
                            getCoverFile(book)
                    )
            );

        } else {

            TextView noCover =
                    new TextView(this);

            noCover.setText(
                    "暂无封面"
            );

            noCover.setTextSize(14);

            noCover.setTextColor(
                    Color.rgb(
                            115,
                            115,
                            115
                    )
            );

            noCover.setGravity(
                    Gravity.CENTER
            );

            GradientDrawable noCoverBg =
                    new GradientDrawable();

            noCoverBg.setColor(
                    Color.rgb(
                            20,
                            20,
                            20
                    )
            );

            noCoverBg.setCornerRadius(
                    dp(7)
            );

            noCover.setBackground(
                    noCoverBg
            );

            item.addView(
                    noCover,
                    new LinearLayout.LayoutParams(
                            -1,
                            dp(170)
                    )
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
                0,
                0,
                0,
                dp(3)
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
                            .start();

                    v.setAlpha(0.82f);

                    View.DragShadowBuilder shadow =
                            new View.DragShadowBuilder(v);

                    if (android.os.Build.VERSION.SDK_INT >= 24) {

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

        saveShelfOrder(
                books
        );

        rebuildShelf(
                books
        );

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
                dp(18),
                dp(18),
                dp(18),
                dp(12)
        );

        panel.setBackgroundColor(
                Color.BLACK
        );

        ImageView cover =
                new ImageView(this);

        cover.setAdjustViewBounds(
                true
        );

        cover.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        if (hasCover(book)) {

            cover.setImageURI(
                    Uri.fromFile(
                            getCoverFile(book)
                    )
            );

            panel.addView(
                    cover,
                    new LinearLayout.LayoutParams(
                            -1,
                            dp(220)
                    )
            );
        }

        TextView name =
                new TextView(this);

        name.setText(
                getDisplayName(book)
        );

        name.setTextSize(19);
        name.setTextColor(Color.WHITE);

        name.setGravity(
                Gravity.CENTER
        );

        name.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        name.setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(12)
        );

        panel.addView(
                name,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        TextView read =
                new TextView(this);

        read.setText(
                "进入阅读"
        );

        read.setTextSize(17);
        read.setTextColor(Color.WHITE);

        read.setGravity(
                Gravity.CENTER
        );

        GradientDrawable readBg =
                new GradientDrawable();

        readBg.setColor(
                Color.rgb(
                        55,
                        55,
                        55
                )
        );

        readBg.setCornerRadius(
                dp(8)
        );

        read.setBackground(
                readBg
        );

        LinearLayout.LayoutParams readParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                );

        readParams.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        panel.addView(
                read,
                readParams
        );

        TextView manage =
                new TextView(this);

        manage.setText(
                "管理书本"
        );

        manage.setTextSize(17);

        manage.setTextColor(
                Color.rgb(
                        220,
                        220,
                        220
                )
        );

        manage.setGravity(
                Gravity.CENTER
        );

        GradientDrawable manageBg =
                new GradientDrawable();

        manageBg.setColor(
                Color.rgb(
                        35,
                        35,
                        35
                )
        );

        manageBg.setCornerRadius(
                dp(8)
        );

        manage.setBackground(
                manageBg
        );

        LinearLayout.LayoutParams manageParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                );

        manageParams.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        panel.addView(
                manage,
                manageParams
        );

        final AlertDialog[] bookDialog =
                new AlertDialog[1];

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(panel)
                        .setNegativeButton(
                                "取消",
                                null
                        )
                        .create();

        bookDialog[0] = dialog;

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

        showShelf();
        reader(book);
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
                dp(18),
                dp(18),
                dp(18),
                dp(12)
        );

        panel.setBackgroundColor(
                Color.BLACK
        );

        EditText nameInput =
                new EditText(this);

        nameInput.setText(
                getDisplayName(book)
        );

        nameInput.setTextColor(
                Color.WHITE
        );

        nameInput.setHintTextColor(
                Color.GRAY
        );

        nameInput.setSingleLine(true);

        nameInput.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        panel.addView(
                nameInput,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        TextView rename =
                new TextView(this);

        rename.setText(
                "保存书名"
        );

        rename.setTextSize(16);
        rename.setTextColor(Color.WHITE);

        rename.setGravity(
                Gravity.CENTER
        );

        GradientDrawable renameBg =
                new GradientDrawable();

        renameBg.setColor(
                Color.rgb(
                        50,
                        50,
                        50
                )
        );

        renameBg.setCornerRadius(
                dp(8)
        );

        rename.setBackground(
                renameBg
        );

        panel.addView(
                rename,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(50)
                )
        );

        TextView changeCover =
                new TextView(this);

        changeCover.setText(
                "更换封面"
        );

        changeCover.setTextSize(16);

        changeCover.setTextColor(
                Color.rgb(
                        220,
                        220,
                        220
                )
        );

        changeCover.setGravity(
                Gravity.CENTER
        );

        GradientDrawable coverBg =
                new GradientDrawable();

        coverBg.setColor(
                Color.rgb(
                        35,
                        35,
                        35
                )
        );

        coverBg.setCornerRadius(
                dp(8)
        );

        changeCover.setBackground(
                coverBg
        );

        LinearLayout.LayoutParams coverParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(50)
                );

        coverParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        panel.addView(
                changeCover,
                coverParams
        );

        TextView delete =
                new TextView(this);

        delete.setText(
                "删除书本"
        );

        delete.setTextSize(16);

        delete.setTextColor(
                Color.rgb(
                        210,
                        210,
                        210
                )
        );

        delete.setGravity(
                Gravity.CENTER
        );

        GradientDrawable deleteBg =
                new GradientDrawable();

        deleteBg.setColor(
                Color.rgb(
                        55,
                        35,
                        35
                )
        );

        deleteBg.setCornerRadius(
                dp(8)
        );

        delete.setBackground(
                deleteBg
        );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(50)
                );

        deleteParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        panel.addView(
                delete,
                deleteParams
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

                    intent.setType(
                            "image/*"
                    );

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
                                        showShelf();
                                    }
                            )
                            .show();
                }
        );

        dialog.show();
    }

    private boolean deleteBook(
            File book
    ) {

        boolean success =
                deleteRecursive(book);

        String id =
                book.getName();

        sp.edit()
                .remove(
                        "display_name_" + id
                )
                .remove(
                        "order_" + id
                )
                .remove(
                        "y_" + id
                )
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
                    .remove(
                            "shelf_order"
                    )
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

            image.setAdjustViewBounds(
                    true
            );

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

        final LinearLayout controls =
                new LinearLayout(this);

        controls.setOrientation(
                LinearLayout.HORIZONTAL
        );

        controls.setGravity(
                Gravity.CENTER
        );

        controls.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        GradientDrawable controlsBg =
                new GradientDrawable();

        controlsBg.setColor(
                Color.argb(
                        225,
                        25,
                        25,
                        25
                )
        );

        controlsBg.setCornerRadius(
                dp(12)
        );

        controls.setBackground(
                controlsBg
        );

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
                dp(8),
                dp(8),
                dp(8),
                dp(12)
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

                                controls.setVisibility(
                                        View.GONE
                                );

                            } else {

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

        int savedY =
                sp.getInt(
                        "y_" + book.getName(),
                        0
                );

        scroll.post(
                () -> scroll.scrollTo(
                        0,
                        savedY
                )
        );

        scroll.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

                    sp.edit()
                            .putInt(
                                    "y_" + book.getName(),
                                    scrollY
                            )
                            .apply();
                }
        );

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
                new TextView(this);

        button.setText(text);
        button.setTextSize(13);
        button.setTextColor(Color.WHITE);

        button.setGravity(
                Gravity.CENTER
        );

        button.setPadding(
                dp(6),
                dp(10),
                dp(6),
                dp(10)
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

        intent.setType(
                "image/*"
        );

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

            pendingCoverDisplayName =
                    null;

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

                if (index >= 0
                        && cursor.moveToFirst()) {

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

            String extension =
                    ".jpg";

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
                    new FileOutputStream(
                            target
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

        ArrayList<File> list =
                images(book);

        Collections.sort(
                list,
                (a, b) ->
                        naturalCompare(
                                a.getName(),
                                b.getName()
                        )
        );

        saveImageOrder(
                book,
                list
        );

        reader(book);
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
            File book
    ) {

        ArrayList<File> list =
                images(book);

        if (list.size() < 2) {

            Toast.makeText(
                    this,
                    "图片数量不足",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String[] names =
                new String[list.size()];

        for (
                int i = 0;
                i < list.size();
                i++
        ) {

            names[i] =
                    list.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("图片排序")
                .setItems(
                        names,
                        null
                )
                .setNegativeButton(
                        "取消",
                        null
                )
                .show();
    }

    // =========================================================
    // 管理图片
    // =========================================================

    private void manageImages(
            final File book
    ) {

        final ArrayList<File> list =
                images(book);

        LinearLayout panel =
                new LinearLayout(this);

        panel.setOrientation(
                LinearLayout.VERTICAL
        );

        panel.setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10)
        );

        panel.setBackgroundColor(
                Color.BLACK
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

                        for (
                                File file : current
                        ) {

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
                                    dp(4),
                                    dp(5),
                                    dp(4),
                                    dp(5)
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
                                            dp(70),
                                            dp(90)
                                    )
                            );

                            TextView fileName =
                                    new TextView(
                                            MainActivity.this
                                    );

                            fileName.setText(
                                    file.getName()
                            );

                            fileName.setTextSize(13);

                            fileName.setTextColor(
                                    Color.WHITE
                            );

                            fileName.setGravity(
                                    Gravity.CENTER_VERTICAL
                            );

                            LinearLayout.LayoutParams
                                    nameParams =
                                    new LinearLayout.LayoutParams(
                                            0,
                                            -1,
                                            1
                                    );

                            nameParams.setMargins(
                                    dp(10),
                                    0,
                                    dp(8),
                                    0
                            );

                            row.addView(
                                    fileName,
                                    nameParams
                            );

                            TextView delete =
                                    new TextView(
                                            MainActivity.this
                                    );

                            delete.setText(
                                    "删除"
                            );

                            delete.setTextSize(14);

                            delete.setTextColor(
                                    Color.WHITE
                            );

                            delete.setGravity(
                                    Gravity.CENTER
                            );

                            row.addView(
                                    delete,
                                    new LinearLayout.LayoutParams(
                                            dp(60),
                                            dp(45)
                                    )
                            );

                            delete.setOnClickListener(
                                    v -> {

                                        if (
                                                file.delete()
                                        ) {

                                            ArrayList<File>
                                                    after =
                                                    images(book);

                                            saveImageOrder(
                                                    book,
                                                    after
                                            );

                                            refreshList[0]
                                                    .run();
                                        }
                                    }
                            );

                            imageList.addView(
                                    row,
                                    new LinearLayout.LayoutParams(
                                            -1,
                                            dp(100)
                                    )
                            );
                        }
                    }
                };

        refreshList[0].run();

        dialog.show();
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
