package com.chaersihaha.mangareader;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = getSharedPreferences("meta", MODE_PRIVATE);

        booksDir = new File(
                getFilesDir(),
                "books"
        );

        booksDir.mkdirs();

        showShelf();
    }

    private void fullscreen() {
        if (Build.VERSION.SDK_INT >= 30) {

            WindowInsetsController controller =
                    getWindow().getInsetsController();

            if (controller != null) {

                controller.hide(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    private int dp(int value) {
        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    private TextView tv(String text, int size) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER_VERTICAL);

        view.setPadding(
                dp(16),
                dp(12),
                dp(16),
                dp(12)
        );

        return view;
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

        String key =
                "display_name_" +
                        book.getName();

        String name =
                sp.getString(
                        key,
                        null
                );

        if (
                name == null ||
                        name.trim().isEmpty()
        ) {
            return book.getName();
        }

        return name;
    }

    private void setDisplayName(
            File book,
            String name
    ) {

        String key =
                "display_name_" +
                        book.getName();

        if (
                name == null ||
                        name.trim().isEmpty()
        ) {

            sp.edit()
                    .remove(key)
                    .apply();

        } else {

            sp.edit()
                    .putString(
                            key,
                            name.trim()
                    )
                    .apply();
        }
    }

    private File getCoverDir(File book) {

        File dir =
                new File(
                        book,
                        "cover"
                );

        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    private File getCoverFile(File book) {

        return new File(
                getCoverDir(book),
                "cover.jpg"
        );
    }

    private boolean hasCover(File book) {

        File file =
                getCoverFile(book);

        return file.exists()
                && file.isFile()
                && file.length() > 0;
    }

    private void showShelf() {

        currentBook = null;

        base();

        TextView title =
                tv(
                        "MangaReader",
                        26
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(64)
                )
        );

        TextView create =
                tv(
                        "＋ 创建书本",
                        20
                );

        create.setGravity(
                Gravity.CENTER
        );

        create.setBackgroundColor(
                Color.DKGRAY
        );

        root.addView(
                create,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(56)
                )
        );

        create.setOnClickListener(
                v -> createBook()
        );

        shelf =
                new LinearLayout(this);

        shelf.setOrientation(
                LinearLayout.VERTICAL
        );

        shelf.setBackgroundColor(
                Color.BLACK
        );

        ScrollView shelfScroll =
                new ScrollView(this);

        shelfScroll.setBackgroundColor(
                Color.BLACK
        );

        shelfScroll.addView(shelf);

        root.addView(
                shelfScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        File[] books =
                booksDir.listFiles();

        if (books != null) {

            ArrayList<File> list =
                    new ArrayList<>();

            for (File book : books) {

                if (book.isDirectory()) {
                    list.add(book);
                }
            }

            Collections.sort(
                    list,
                    (a, b) ->
                            naturalCompare(
                                    a.getName(),
                                    b.getName()
                            )
            );

            for (File book : list) {
                addBook(book);
            }
        }
    }

    private void addBook(File book) {

        LinearLayout item =
                new LinearLayout(this);

        item.setOrientation(
                LinearLayout.HORIZONTAL
        );

        item.setGravity(
                Gravity.CENTER_VERTICAL
        );

        item.setBackgroundColor(
                Color.rgb(
                        30,
                        30,
                        30
                )
        );

        item.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(80)
                );

        itemParams.setMargins(
                dp(4),
                dp(4),
                dp(4),
                dp(4)
        );

        shelf.addView(
                item,
                itemParams
        );

        ImageView coverView =
                new ImageView(this);

        coverView.setAdjustViewBounds(true);

        coverView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        coverView.setBackgroundColor(
                Color.BLACK
        );

        LinearLayout.LayoutParams coverParams =
                new LinearLayout.LayoutParams(
                        dp(56),
                        dp(64)
                );

        coverParams.setMargins(
                0,
                0,
                dp(12),
                0
        );

        item.addView(
                coverView,
                coverParams
        );

        if (hasCover(book)) {

            coverView.setImageURI(
                    Uri.fromFile(
                            getCoverFile(book)
                    )
            );
        }

        TextView nameView =
                tv(
                        "📖 " +
                                getDisplayName(book),
                        18
                );

        nameView.setPadding(
                0,
                0,
                0,
                0
        );

        item.addView(
                nameView,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        item.setOnClickListener(
                v -> reader(book)
        );

        item.setOnLongClickListener(
                v -> {

                    manageBook(book);

                    return true;
                }
        );
    }

    private void createBook() {

        File[] existing =
                booksDir.listFiles();

        int number = 1;

        if (existing != null) {

            Set<Integer> used =
                    new HashSet<>();

            for (File file : existing) {

                if (
                        file.isDirectory()
                                && file.getName()
                                .startsWith("书本 ")
                ) {

                    try {

                        used.add(
                                Integer.parseInt(
                                        file.getName()
                                                .substring(3)
                                                .trim()
                                )
                        );

                    } catch (Exception ignored) {
                    }
                }
            }

            while (used.contains(number)) {
                number++;
            }
        }

        File book =
                new File(
                        booksDir,
                        "书本 " + number
                );

        book.mkdirs();

        getCoverDir(book);

        showShelf();
        reader(book);
    }

    private void manageBook(final File book) {

        LinearLayout panel =
                new LinearLayout(this);

        panel.setOrientation(
                LinearLayout.VERTICAL
        );

        panel.setPadding(
                dp(20),
                dp(16),
                dp(20),
                dp(8)
        );

        panel.setBackgroundColor(
                Color.BLACK
        );

        TextView label =
                tv(
                        "书名",
                        14
                );

        label.setTextColor(
                Color.LTGRAY
        );

        panel.addView(label);

        final EditText nameEdit =
                new EditText(this);

        nameEdit.setText(
                getDisplayName(book)
        );

        nameEdit.setTextColor(
                Color.WHITE
        );

        nameEdit.setHint(
                "输入书名"
        );

        nameEdit.setHintTextColor(
                Color.GRAY
        );

        nameEdit.setBackgroundColor(
                Color.DKGRAY
        );

        nameEdit.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        nameEdit.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        panel.addView(
                nameEdit,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        View spacer1 =
                new View(this);

        panel.addView(
                spacer1,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(16)
                )
        );

        TextView coverBtn =
                tv(
                        "更换封面",
                        16
                );

        coverBtn.setGravity(
                Gravity.CENTER
        );

        coverBtn.setBackgroundColor(
                Color.rgb(
                        60,
                        60,
                        60
                )
        );

        panel.addView(
                coverBtn,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)
                )
        );

        coverBtn.setOnClickListener(
                v -> {

                    pendingCoverDisplayName =
                            nameEdit
                                    .getText()
                                    .toString();

                    currentBook = book;

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
                }
        );

        View spacer2 =
                new View(this);

        panel.addView(
                spacer2,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(12)
                )
        );

        TextView deleteBtn =
                tv(
                        "删除书本",
                        16
                );

        deleteBtn.setGravity(
                Gravity.CENTER
        );

        deleteBtn.setBackgroundColor(
                Color.rgb(
                        120,
                        40,
                        40
                )
        );

        panel.addView(
                deleteBtn,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)
                )
        );

        final AlertDialog[] manageDialog =
                new AlertDialog[1];

        deleteBtn.setOnClickListener(
                v -> {

                    new AlertDialog.Builder(this)
                            .setTitle(
                                    "确认删除"
                            )
                            .setMessage(
                                    "确定删除《" +
                                            getDisplayName(book) +
                                            "》吗？\n此操作不可恢复。"
                            )
                            .setPositiveButton(
                                    "删除",
                                    (d, w) -> {

                                        boolean success =
                                                deleteBook(book);

                                        if (
                                                manageDialog[0] != null
                                        ) {
                                            manageDialog[0]
                                                    .dismiss();
                                        }

                                        if (success) {

                                            showShelf();

                                            Toast.makeText(
                                                    this,
                                                    "书本已删除",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                        } else {

                                            Toast.makeText(
                                                    this,
                                                    "删除失败",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            )
                            .setNegativeButton(
                                    "取消",
                                    null
                            )
                            .show();
                }
        );

        manageDialog[0] =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "管理书本"
                        )
                        .setView(panel)
                        .setPositiveButton(
                                "保存",
                                (d, w) -> {

                                    setDisplayName(
                                            book,
                                            nameEdit
                                                    .getText()
                                                    .toString()
                                    );

                                    pendingCoverDisplayName =
                                            null;

                                    showShelf();
                                }
                        )
                        .setNegativeButton(
                                "取消",
                                (d, w) -> {

                                    pendingCoverDisplayName =
                                            null;
                                }
                        )
                        .create();

        manageDialog[0].show();
    }

    private boolean deleteBook(File book) {

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

        return success;
    }

    private boolean deleteRecursive(File file) {

        if (!file.exists()) {
            return true;
        }

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {

                for (File child : children) {

                    if (!deleteRecursive(child)) {
                        return false;
                    }
                }
            }
        }

        return file.delete();
    }

    private ArrayList<File> images(File book) {

        File[] files =
                book.listFiles();

        ArrayList<File> result =
                new ArrayList<>();

        if (files != null) {

            for (File file : files) {

                if (
                        file.isFile()
                                && file.getName()
                                .matches(
                                        "(?i).*\\.(jpg|jpeg|png|webp|gif)$"
                                )
                ) {
                    result.add(file);
                }
            }
        }

        ArrayList<File> natural =
                new ArrayList<>(
                        result
                );

        Collections.sort(
                natural,
                new Comparator<File>() {
                    @Override
                    public int compare(
                            File a,
                            File b
                    ) {

                        return naturalCompare(
                                a.getName(),
                                b.getName()
                        );
                    }
                }
        );

        String saved =
                sp.getString(
                        "order_" +
                                book.getName(),
                        ""
                );

        if (saved.isEmpty()) {
            return natural;
        }

        ArrayList<File> ordered =
                new ArrayList<>();

        Set<String> used =
                new HashSet<>();

        String[] names =
                saved.split("\\|");

        for (String name : names) {

            for (File file : natural) {

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

        for (File file : natural) {

            if (
                    !used.contains(
                            file.getName()
                    )
            ) {
                ordered.add(file);
            }
        }

        return ordered;
    }

    private int naturalCompare(
            String a,
            String b
    ) {

        String[] x =
                a.split(
                        "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"
                );

        String[] y =
                b.split(
                        "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"
                );

        for (
                int i = 0;
                i < Math.min(
                        x.length,
                        y.length
                );
                i++
        ) {

            try {

                int p =
                        Integer.parseInt(
                                x[i]
                        );

                int q =
                        Integer.parseInt(
                                y[i]
                        );

                if (p != q) {

                    return Integer.compare(
                            p,
                            q
                    );
                }

            } catch (Exception e) {

                int c =
                        x[i].compareToIgnoreCase(
                                y[i]
                        );

                if (c != 0) {
                    return c;
                }
            }
        }

        return Integer.compare(
                x.length,
                y.length
        );
    }

    private void reader(File book) {

        currentBook = book;

        base();

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setBackgroundColor(
                Color.BLACK
        );

        LinearLayout imagesBox =
                new LinearLayout(this);

        imagesBox.setOrientation(
                LinearLayout.VERTICAL
        );

        imagesBox.setBackgroundColor(
                Color.BLACK
        );

        scrollView.addView(
                imagesBox
        );

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        LinearLayout controls =
                new LinearLayout(this);

        controls.setOrientation(
                LinearLayout.HORIZONTAL
        );

        controls.setGravity(
                Gravity.CENTER
        );

        controls.setBackgroundColor(
                Color.DKGRAY
        );

        controls.setVisibility(
                View.GONE
        );

        TextView sortButton =
                tv(
                        "图片排序",
                        13
                );

        TextView addButton =
                tv(
                        "添加图片",
                        13
                );

        TextView manageButton =
                tv(
                        "管理图片",
                        13
                );

        TextView topButton =
                tv(
                        "回顶部",
                        13
                );

        sortButton.setGravity(
                Gravity.CENTER
        );

        addButton.setGravity(
                Gravity.CENTER
        );

        manageButton.setGravity(
                Gravity.CENTER
        );

        topButton.setGravity(
                Gravity.CENTER
        );

        controls.addView(
                sortButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        controls.addView(
                addButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        controls.addView(
                manageButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        controls.addView(
                topButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        root.addView(
                controls,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                )
        );

        ArrayList<File> files =
                images(book);

        for (File file : files) {

            addImage(
                    imagesBox,
                    file
            );
        }

        sortButton.setOnClickListener(
                v -> sortMenu(book)
        );

        addButton.setOnClickListener(
                v -> pick(book)
        );

        manageButton.setOnClickListener(
                v -> manageImages(book)
        );

        topButton.setOnClickListener(
                v ->
                        scrollView.smoothScrollTo(
                                0,
                                0
                        )
        );

        final long[] lastTap =
                {0};

        scrollView.setOnTouchListener(
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

                            controls.setVisibility(
                                    controls.getVisibility()
                                            == View.VISIBLE
                                            ? View.GONE
                                            : View.VISIBLE
                            );
                        }

                        lastTap[0] = now;
                    }

                    return false;
                }
        );

        scrollView.post(
                () -> {

                    int position =
                            sp.getInt(
                                    "y_" +
                                            book.getName(),
                                    0
                            );

                    scrollView.scrollTo(
                            0,
                            position
                    );
                }
        );

        scrollView.setOnScrollChangeListener(
                (v, x, y, oldX, oldY) -> {

                    sp.edit()
                            .putInt(
                                    "y_" +
                                            book.getName(),
                                    y
                            )
                            .apply();
                }
        );
    }

    private void addImage(
            LinearLayout box,
            File file
    ) {

        ImageView image =
                new ImageView(this);

        image.setAdjustViewBounds(true);

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setImageURI(
                Uri.fromFile(file)
        );

        box.addView(
                image,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );
    }

    private void pick(File book) {

        currentBook = book;

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
                resultCode != RESULT_OK ||
                        data == null ||
                        currentBook == null
        ) {
            return;
        }

        try {

            if (
                    requestCode ==
                            REQ_ADD_IMAGES
            ) {

                if (
                        data.getClipData() != null
                ) {

                    for (
                            int i = 0;
                            i <
                                    data.getClipData()
                                            .getItemCount();
                            i++
                    ) {

                        copyImage(
                                data.getClipData()
                                        .getItemAt(i)
                                        .getUri(),
                                currentBook,
                                false
                        );
                    }

                } else if (
                        data.getData() != null
                ) {

                    copyImage(
                            data.getData(),
                            currentBook,
                            false
                    );
                }

                reader(currentBook);

            } else if (
                    requestCode ==
                            REQ_PICK_COVER
            ) {

                if (
                        data.getData() != null
                ) {

                    if (
                            pendingCoverDisplayName
                                    != null
                    ) {

                        setDisplayName(
                                currentBook,
                                pendingCoverDisplayName
                        );
                    }

                    copyImage(
                            data.getData(),
                            currentBook,
                            true
                    );

                    pendingCoverDisplayName =
                            null;

                    showShelf();
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "操作失败: " +
                            e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();

        } finally {

            if (
                    requestCode ==
                            REQ_PICK_COVER
            ) {
                pendingCoverDisplayName =
                        null;
            }
        }
    }

    private void copyImage(
            Uri uri,
            File book,
            boolean isCover
    ) throws Exception {

        String name;

        if (isCover) {

            name =
                    "cover.jpg";

        } else {

            name =
                    "image_" +
                            System.currentTimeMillis() +
                            ".jpg";
        }

        Cursor cursor =
                getContentResolver().query(
                        uri,
                        null,
                        null,
                        null,
                        null
                );

        if (cursor != null) {

            int index =
                    cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                    );

            if (
                    cursor.moveToFirst()
                            && index >= 0
            ) {

                String original =
                        cursor.getString(
                                index
                        );

                if (
                        original != null
                                && !original.isEmpty()
                                && !isCover
                ) {

                    name = original;
                }
            }

            cursor.close();
        }

        File output;

        if (isCover) {

            output =
                    getCoverFile(book);

        } else {

            output =
                    new File(
                            book,
                            name
                    );
        }

        InputStream input =
                getContentResolver()
                        .openInputStream(uri);

        if (input == null) {

            throw new Exception(
                    "无法打开图片"
            );
        }

        try (
                InputStream in = input;
                FileOutputStream outputStream =
                        new FileOutputStream(
                                output
                        )
        ) {

            byte[] buffer =
                    new byte[8192];

            int length;

            while (
                    (length =
                            in.read(buffer)) > 0
            ) {

                outputStream.write(
                        buffer,
                        0,
                        length
                );
            }
        }
    }

    private void sortMenu(File book) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "图片排序"
                )
                .setItems(
                        new String[]{
                                "自动数字排序",
                                "手动排序"
                        },
                        (dialog, which) -> {

                            if (which == 0) {

                                sp.edit()
                                        .remove(
                                                "order_" +
                                                        book.getName()
                                        )
                                        .apply();

                                reader(book);

                            } else {

                                manualSort(book);
                            }
                        }
                )
                .show();
    }

    private void manualSort(File book) {

        ArrayList<File> files =
                images(book);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        list.setBackgroundColor(
                Color.BLACK
        );

        for (File file : files) {

            addSortItem(
                    list,
                    files,
                    file,
                    book
            );
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.setBackgroundColor(
                Color.BLACK
        );

        scroll.addView(list);

        new AlertDialog.Builder(this)
                .setTitle(
                        "手动排序"
                )
                .setMessage(
                        "长按图片名称，然后拖动调整顺序"
                )
                .setView(scroll)
                .setPositiveButton(
                        "完成",
                        (dialog, which) -> {

                            saveOrder(
                                    book,
                                    files
                            );

                            reader(book);
                        }
                )
                .setNegativeButton(
                        "取消",
                        null
                )
                .show();
    }

    private void addSortItem(
            LinearLayout list,
            ArrayList<File> files,
            File file,
            File book
    ) {

        TextView item =
                tv(
                        file.getName(),
                        15
                );

        item.setBackgroundColor(
                Color.DKGRAY
        );

        item.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                );

        params.setMargins(
                dp(4),
                dp(2),
                dp(4),
                dp(2)
        );

        list.addView(
                item,
                params
        );

        item.setOnLongClickListener(
                v -> {

                    View.DragShadowBuilder shadow =
                            new View.DragShadowBuilder(
                                    item
                            );

                    item.startDragAndDrop(
                            null,
                            shadow,
                            file,
                            0
                    );

                    return true;
                }
        );

        item.setOnDragListener(
                (v, event) -> {

                    if (
                            event.getAction()
                                    ==
                                    android.view.DragEvent
                                            .ACTION_DROP
                    ) {

                        File dragged =
                                (File)
                                        event.getLocalState();

                        int from =
                                files.indexOf(
                                        dragged
                                );

                        int to =
                                files.indexOf(
                                        file
                                );

                        if (
                                from >= 0
                                        && to >= 0
                                        && from != to
                        ) {

                            Collections.swap(
                                    files,
                                    from,
                                    to
                            );

                            rebuildSortList(
                                    list,
                                    files,
                                    book
                            );
                        }

                        return true;
                    }

                    return true;
                }
        );
    }

    private void rebuildSortList(
            LinearLayout list,
            ArrayList<File> files,
            File book
    ) {

        list.removeAllViews();

        for (File file : files) {

            addSortItem(
                    list,
                    files,
                    file,
                    book
            );
        }
    }

    private void saveOrder(
            File book,
            ArrayList<File> files
    ) {

        StringBuilder order =
                new StringBuilder();

        for (File file : files) {

            if (order.length() > 0) {
                order.append("|");
            }

            order.append(
                    file.getName()
            );
        }

        sp.edit()
                .putString(
                        "order_" +
                                book.getName(),
                        order.toString()
                )
                .apply();
    }

    private void manageImages(
            final File book
    ) {

        final LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        list.setBackgroundColor(
                Color.BLACK
        );

        list.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setBackgroundColor(
                Color.BLACK
        );

        scroll.addView(list);

        /*
         * 关键修复：
         * 不能直接在 refreshList 自己的初始化过程中
         * 使用 refreshList.run()。
         *
         * 用数组先创建引用，再给引用赋值，
         * 这样 Runnable 内部就可以安全调用自己。
         */
        final Runnable[] refreshList =
                new Runnable[1];

        refreshList[0] =
                new Runnable() {

                    @Override
                    public void run() {

                        list.removeAllViews();

                        ArrayList<File> files =
                                images(book);

                        if (files.isEmpty()) {

                            TextView empty =
                                    tv(
                                            "当前没有图片",
                                            16
                                    );

                            empty.setGravity(
                                    Gravity.CENTER
                            );

                            list.addView(
                                    empty,
                                    new LinearLayout.LayoutParams(
                                            -1,
                                            dp(100)
                                    )
                            );

                            return;
                        }

                        for (
                                final File file :
                                files
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

                            row.setBackgroundColor(
                                    Color.rgb(
                                            40,
                                            40,
                                            40
                                    )
                            );

                            row.setPadding(
                                    dp(8),
                                    dp(6),
                                    dp(8),
                                    dp(6)
                            );

                            LinearLayout.LayoutParams rowParams =
                                    new LinearLayout.LayoutParams(
                                            -1,
                                            -2
                                    );

                            rowParams.setMargins(
                                    0,
                                    dp(3),
                                    0,
                                    dp(3)
                            );

                            list.addView(
                                    row,
                                    rowParams
                            );

                            TextView name =
                                    tv(
                                            file.getName(),
                                            14
                                    );

                            name.setPadding(
                                    0,
                                    0,
                                    0,
                                    0
                            );

                            row.addView(
                                    name,
                                    new LinearLayout.LayoutParams(
                                            0,
                                            -2,
                                            1
                                    )
                            );

                            TextView del =
                                    tv(
                                            "删除",
                                            14
                                    );

                            del.setGravity(
                                    Gravity.CENTER
                            );

                            del.setBackgroundColor(
                                    Color.rgb(
                                            140,
                                            40,
                                            40
                                    )
                            );

                            del.setPadding(
                                    dp(12),
                                    dp(8),
                                    dp(12),
                                    dp(8)
                            );

                            row.addView(
                                    del,
                                    new LinearLayout.LayoutParams(
                                            -2,
                                            -2
                                    )
                            );

                            del.setOnClickListener(
                                    v -> {

                                        new AlertDialog.Builder(
                                                MainActivity.this
                                        )
                                                .setTitle(
                                                        "确认删除"
                                                )
                                                .setMessage(
                                                        "确定删除这张图片吗？\n"
                                                                + file.getName()
                                                )
                                                .setPositiveButton(
                                                        "删除",
                                                        (d, w) -> {

                                                            boolean deleted =
                                                                    file.delete();

                                                            if (
                                                                    deleted
                                                                            || !file.exists()
                                                            ) {

                                                                cleanOrderAfterDelete(
                                                                        book,
                                                                        file.getName()
                                                                );

                                                                Toast.makeText(
                                                                        MainActivity.this,
                                                                        "图片已删除",
                                                                        Toast.LENGTH_SHORT
                                                                ).show();

                                                                /*
                                                                 * 这里也同步改成数组引用。
                                                                 */
                                                                refreshList[0].run();

                                                            } else {

                                                                Toast.makeText(
                                                                        MainActivity.this,
                                                                        "删除失败",
                                                                        Toast.LENGTH_SHORT
                                                                ).show();
                                                            }
                                                        }
                                                )
                                                .setNegativeButton(
                                                        "取消",
                                                        null
                                                )
                                                .show();
                                    }
                            );
                        }
                    }
                };

        refreshList[0].run();

        new AlertDialog.Builder(this)
                .setTitle(
                        "管理图片"
                )
                .setView(scroll)
                .setPositiveButton(
                        "完成",
                        (d, w) ->
                                reader(book)
                )
                .setNegativeButton(
                        "关闭",
                        (d, w) ->
                                reader(book)
                )
                .show();
    }

    private void cleanOrderAfterDelete(
            File book,
            String deletedName
    ) {

        String key =
                "order_" +
                        book.getName();

        String saved =
                sp.getString(
                        key,
                        ""
                );

        if (saved.isEmpty()) {
            return;
        }

        StringBuilder newOrder =
                new StringBuilder();

        String[] names =
                saved.split("\\|");

        for (String name : names) {

            if (
                    !name.equals(deletedName)
                            && !name.isEmpty()
            ) {

                if (
                        newOrder.length() > 0
                ) {
                    newOrder.append("|");
                }

                newOrder.append(name);
            }
        }

        if (
                newOrder.length() == 0
        ) {

            sp.edit()
                    .remove(key)
                    .apply();

        } else {

            sp.edit()
                    .putString(
                            key,
                            newOrder.toString()
                    )
                    .apply();
        }
    }

    @Override
    public void onBackPressed() {

        if (currentBook != null) {

            currentBook = null;

            showShelf();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        fullscreen();
    }
}
