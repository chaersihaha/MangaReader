package com.chaersihaha.mangareader;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.provider.OpenableColumns;import android.database.Cursor;import android.view.*;import android.widget.*;import android.graphics.drawable.ColorDrawable;import java.io.*;import java.util.*;

public class MainActivity extends Activity {
 LinearLayout root, shelf; SharedPreferences sp; File booksDir;
 public void onCreate(Bundle b){super.onCreate(b);sp=getSharedPreferences("meta",0);booksDir=new File(getFilesDir(),"books");booksDir.mkdirs();showShelf();}
 TextView tv(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setPadding(28,24,28,24);return t;}
 void base(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);setContentView(root);}
 void showShelf(){base(); TextView title=tv("MangaReader",26);root.addView(title); TextView add=tv("＋ 创建书本",20);root.addView(add);add.setOnClickListener(v->createBook()); shelf=new LinearLayout(this);shelf.setOrientation(LinearLayout.VERTICAL);root.addView(shelf,new LinearLayout.LayoutParams(-1,0,1)); File[] bs=booksDir.listFiles();if(bs!=null)for(File d:bs)if(d.isDirectory())addBook(d);}
 void addBook(File d){TextView b=tv("📖 "+d.getName(),20);shelf.addView(b);b.setOnClickListener(v->reader(d));}
 void createBook(){String n="书本 "+(booksDir.listFiles()==null?1:booksDir.listFiles().length+1);File d=new File(booksDir,n);d.mkdirs();showShelf();reader(d);}
 void reader(File book){base(); ScrollView sv=new ScrollView(this);sv.setBackgroundColor(Color.BLACK);LinearLayout imgs=new LinearLayout(this);imgs.setOrientation(LinearLayout.VERTICAL);imgs.setBackgroundColor(Color.BLACK);sv.addView(imgs);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1)); TextView hidden=tv("图片排序    添加图片    返回到最上面",16);hidden.setGravity(Gravity.CENTER);hidden.setBackgroundColor(Color.DKGRAY);hidden.setVisibility(View.GONE);root.addView(hidden,new LinearLayout.LayoutParams(-1,70));
  ArrayList<File> fs=images(book);for(File f:fs) addImage(imgs,f);
  sv.setOnTouchListener(new View.OnTouchListener(){long last=0;public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==1){long now=System.currentTimeMillis();if(now-last<350){hidden.setVisibility(hidden.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}last=now;}return false;}});
  hidden.setOnClickListener(v->{ new AlertDialog.Builder(this).setItems(new String[]{"图片排序","添加图片","返回到最上面"},(d,w)->{if(w==0)sortMenu(book);else if(w==1)pick(book);else sv.smoothScrollTo(0,0);}).show();});
  if(fs.size()>0)sv.post(()->{int pos=sp.getInt("y_"+book.getName(),0);sv.scrollTo(0,pos);});
  sv.setOnScrollChangeListener((v,x,y,ox,oy)->sp.edit().putInt("y_"+book.getName(),y).apply());
 }
 ArrayList<File> images(File d){File[] a=d.listFiles();ArrayList<File> l=new ArrayList<>();if(a!=null)for(File f:a)if(f.isFile()&&f.getName().matches("(?i).*\\.(jpg|jpeg|png|webp|gif)$"))l.add(f);Collections.sort(l,(x,y)->natural(x.getName(),y.getName()));return l;}
 int natural(String a,String b){String[] x=a.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");String[] y=b.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");for(int i=0;i<Math.min(x.length,y.length);i++){try{int p=Integer.parseInt(x[i]),q=Integer.parseInt(y[i]);if(p!=q)return Integer.compare(p,q);}catch(Exception e){int c=x[i].compareToIgnoreCase(y[i]);if(c!=0)return c;}}return Integer.compare(x.length,y.length);}
 void addImage(LinearLayout box,File f){ImageView iv=new ImageView(this);iv.setAdjustViewBounds(true);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setImageURI(Uri.fromFile(f));box.addView(iv,new LinearLayout.LayoutParams(-1,-2));}
 void pick(File book){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,7);current=book;}
 File current; protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==7&&c==RESULT_OK&&d!=null){try{if(d.getClipData()!=null)for(int i=0;i<d.getClipData().getItemCount();i++)copy(d.getClipData().getItemAt(i).getUri(),current);else copy(d.getData(),current);}catch(Exception e){}reader(current);}}
 void copy(Uri u,File book)throws Exception{String name="image_"+System.currentTimeMillis()+".jpg";Cursor c=getContentResolver().query(u,null,null,null,null);if(c!=null){int ix=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&ix>=0)name=c.getString(ix);c.close();}File out=new File(book,name);try(InputStream in=getContentResolver().openInputStream(u);OutputStream o=new FileOutputStream(out)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)o.write(buf,0,n);}}
 void sortMenu(File book){new AlertDialog.Builder(this).setTitle("图片排序").setItems(new String[]{"自动数字排序","手动排序（下一版加入）"},(d,w)->{if(w==0){reader(book);}}).show();}
}
