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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    

        sp = getSharedPreferences("meta", MODE_PRIVATE);
        booksDir = new File(getFilesDir(), "books");
        booksDir.mkdirs();

        showShelf();
    }

    private void fullscreen() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.hide(
                    WindowInsets.Type.statusBars() |
                    WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            fullscreen();
        }
    }

    private int dp(int value) {
        return (int) (
            value * getResources().getDisplayMetrics().density + 0.5f
        );
    }    private TextView tv(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(20), dp(12), dp(20), dp(12));
        return view;
    }

    private void base() {
        fullscreen();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        setContentView(root);
    }

    private void showShelf() {
        base();

        TextView title = tv("MangaReader", 26);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(
            title,
            new LinearLayout.LayoutParams(
                -1,
                dp(64)
            )
        );

        TextView create = tv("＋ 创建书本", 20);
        create.setGravity(Gravity.CENTER);
        create.setBackgroundColor(Color.DKGRAY);
        root.addView(
            create,
            new LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        );

        create.setOnClickListener(v -> createBook());

        shelf = new LinearLayout(this);
        shelf.setOrientation(LinearLayout.VERTICAL);
        shelf.setBackgroundColor(Color.BLACK);

        ScrollView shelfScroll = new ScrollView(this);
        shelfScroll.setBackgroundColor(Color.BLACK);
        shelfScroll.addView(shelf);

        root.addView(
            shelfScroll,
            new LinearLayout.LayoutParams(
                -1,
                0,
                1
            )
        );

        File[] books = booksDir.listFiles();

        if (books != null) {
            for (File book : books) {
                if (book.isDirectory()) {
                    addBook(book);
                }
            }
        }
    }

    private void addBook(File book) {
        TextView item = tv("📖 " + book.getName(), 20);
        item.setGravity(Gravity.CENTER_VERTICAL);

        shelf.addView(
            item,
            new LinearLayout.LayoutParams(
                -1,
                dp(64)
            )
        );

        item.setOnClickListener(v -> reader(book));
    }

    private void createBook() {
        File[] existing = booksDir.listFiles();
        int number = existing == null ? 1 : existing.length + 1;

        File book = new File(
            booksDir,
            "书本 " + number
        );

        book.mkdirs();

        showShelf();
        reader(book);
    }    private ArrayList<File> images(File book) {
        File[] files = book.listFiles();
        ArrayList<File> result = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() &&
                    file.getName().matches("(?i).*\\.(jpg|jpeg|png|webp|gif)$")) {
                    result.add(file);
                }
            }
        }

        ArrayList<File> natural = new ArrayList<>(result);

        Collections.sort(
            natural,
            new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return naturalCompare(
                        a.getName(),
                        b.getName()
                    );
                }
            }
        );

        String saved = sp.getString(
            "order_" + book.getName(),
            ""
        );

        if (saved.isEmpty()) {
            return natural;
        }

        ArrayList<File> ordered = new ArrayList<>();
        Set<String> used = new HashSet<>();

        String[] names = saved.split("\\|");

        for (String name : names) {
            for (File file : natural) {
                if (file.getName().equals(name)) {
                    ordered.add(file);
                    used.add(name);
                    break;
                }
            }
        }

        for (File file : natural) {
            if (!used.contains(file.getName())) {
                ordered.add(file);
            }
        }

        return ordered;
    }

    private int naturalCompare(String a, String b) {
        String[] x = a.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] y = b.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            try {
                int p = Integer.parseInt(x[i]);
                int q = Integer.parseInt(y[i]);

                if (p != q) {
                    return Integer.compare(p, q);
                }
            } catch (Exception e) {
                int c = x[i].compareToIgnoreCase(y[i]);

                if (c != 0) {
                    return c;
                }
            }
        }

        return Integer.compare(x.length, y.length);
    }    private void reader(File book) {
        currentBook = book;
        base();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.BLACK);

        LinearLayout images = new LinearLayout(this);
        images.setOrientation(LinearLayout.VERTICAL);
        images.setBackgroundColor(Color.BLACK);

        scrollView.addView(images);

        root.addView(
            scrollView,
            new LinearLayout.LayoutParams(
                -1,
                0,
                1
            )
        );

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setBackgroundColor(Color.DKGRAY);
        controls.setVisibility(View.GONE);

        TextView sortButton = tv("图片排序", 14);
        TextView addButton = tv("添加图片", 14);
        TextView topButton = tv("返回到最上面", 14);

        sortButton.setGravity(Gravity.CENTER);
        addButton.setGravity(Gravity.CENTER);
        topButton.setGravity(Gravity.CENTER);

        controls.addView(
            sortButton,
            new LinearLayout.LayoutParams(
                0,
                dp(56),
                1
            )
        );

        controls.addView(
            addButton,
            new LinearLayout.LayoutParams(
                0,
                dp(56),
                1
            )
        );

        controls.addView(
            topButton,
            new LinearLayout.LayoutParams(
                0,
                dp(56),
                1
            )
        );

        root.addView(
            controls,
            new LinearLayout.LayoutParams(
                -1,
                dp(56)
            )
        );

        ArrayList<File> files = images(book);

        for (File file : files) {
            addImage(images, file);
        }

        sortButton.setOnClickListener(v -> sortMenu(book));

        addButton.setOnClickListener(v -> pick(book));

        topButton.setOnClickListener(v -> {
            scrollView.smoothScrollTo(0, 0);
        });

        final long[] lastTap = {0};

        scrollView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                long now = System.currentTimeMillis();

                if (now - lastTap[0] < 350) {
                    controls.setVisibility(
                        controls.getVisibility() == View.VISIBLE
                            ? View.GONE
                            : View.VISIBLE
                    );
                }

                lastTap[0] = now;
            }

            return false;
        });

        if (!files.isEmpty()) {
            scrollView.post(() -> {
                int position = sp.getInt(
                    "y_" + book.getName(),
                    0
                );

                scrollView.scrollTo(0, position);
            });
        }

        scrollView.setOnScrollChangeListener(
            (v, x, y, oldX, oldY) -> {
                sp.edit()
                    .putInt(
                        "y_" + book.getName(),
                        y
                    )
                    .apply();
            }
        );
    }    private void addImage(LinearLayout box, File file) {
        ImageView image = new ImageView(this);

        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageURI(Uri.fromFile(file));

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

        Intent intent = new Intent(
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
            7
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
            requestCode == 7 &&
            resultCode == RESULT_OK &&
            data != null &&
            currentBook != null
        ) {
            try {
                if (data.getClipData() != null) {
                    for (
                        int i = 0;
                        i < data.getClipData().getItemCount();
                        i++
                    ) {
                        copyImage(
                            data.getClipData()
                                .getItemAt(i)
                                .getUri(),
                            currentBook
                        );
                    }
                } else if (data.getData() != null) {
                    copyImage(
                        data.getData(),
                        currentBook
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            reader(currentBook);
        }
    }

    private void copyImage(
        Uri uri,
        File book
    ) throws Exception {
        String name =
            "image_" +
            System.currentTimeMillis() +
            ".jpg";

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
                cursor.moveToFirst() &&
                index >= 0
            ) {
                name = cursor.getString(index);
            }

            cursor.close();
        }

        File output = new File(
            book,
            name
        );

        try (
            InputStream input =
                getContentResolver()
                    .openInputStream(uri);

            FileOutputStream outputStream =
                new FileOutputStream(output)
        ) {
            byte[] buffer = new byte[8192];
            int length;

            while (
                (length = input.read(buffer)) > 0
            ) {
                outputStream.write(
                    buffer,
                    0,
                    length
                );
            }
        }
    }    private void sortMenu(File book) {
        new AlertDialog.Builder(this)
            .setTitle("图片排序")
            .setItems(
                new String[]{
                    "自动数字排序",
                    "手动排序"
                },
                (dialog, which) -> {
                    if (which == 0) {
                        sp.edit()
                            .remove("order_" + book.getName())
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
        ArrayList<File> files = images(book);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.BLACK);

        for (File file : files) {
            addSortItem(
                list,
                files,
                file,
                book
            );
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(list);

        new AlertDialog.Builder(this)
            .setTitle("手动排序")
            .setMessage("长按图片名称，然后拖动调整顺序")
            .setView(scroll)
            .setPositiveButton(
                "完成",
                (dialog, which) -> {
                    saveOrder(book, files);
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
        TextView item = tv(
            file.getName(),
            16
        );

        item.setBackgroundColor(Color.DKGRAY);
        item.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                -1,
                dp(56)
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

        item.setOnLongClickListener(v -> {
            View.DragShadowBuilder shadow =
                new View.DragShadowBuilder(item);

            item.startDragAndDrop(
                null,
                shadow,
                file,
                0
            );

            return true;
        });

        item.setOnDragListener(
            (v, event) -> {
                if (
                    event.getAction() ==
                
android.view.DragEvent.ACTION_DROP
                ) {
                    File dragged =
                        (File) event.getLocalState();

                    int from =
                        files.indexOf(dragged);

                    int to =
                        files.indexOf(file);

                    if (
                        from >= 0 &&
                        to >= 0 &&
                        from != to
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
    }    private void rebuildSortList(
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

            order.append(file.getName());
        }

        sp.edit()
            .putString(
                "order_" + book.getName(),
                order.toString()
            )
            .apply();
    }

    @Override
    public void onBackPressed() {
        if (currentBook != null) {
            currentBook = null;
            showShelf();
        } else {
            super.onBackPressed();
        }
    }    @Override
    protected void onResume() {
        super.onResume();
        fullscreen();
    }
}
