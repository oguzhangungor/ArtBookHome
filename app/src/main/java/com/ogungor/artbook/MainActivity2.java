package com.ogungor.artbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLSyntaxErrorException;

public class MainActivity2 extends AppCompatActivity {
    ImageView imageView;
    EditText artNameText, painterNameText, yearText;
    Button button;
    Bitmap selectedImage;
    SQLiteDatabase database;
    Button updateBtn;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        artNameText = findViewById(R.id.artNameText);
        painterNameText = findViewById(R.id.painterNameText);
        yearText = findViewById(R.id.yearText);
        button = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);
        updateBtn = findViewById(R.id.updateBtn);


        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if (info.matches("new")) {
            artNameText.setText("");
            painterNameText.setText("");
            yearText.setText("");
            button.setVisibility(View.VISIBLE);

            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.select);
            imageView.setImageBitmap(selectImage);

        } else {
            int artId = intent.getIntExtra("artId", 1);
            button.setVisibility(View.INVISIBLE);
            artNameText.setEnabled(false);
            painterNameText.setEnabled(false);
            yearText.setEnabled(false);
            imageView.setEnabled(false);

            Cursor cursor = database.rawQuery("select * from arts where id =?", new String[]{String.valueOf(artId)});
            int artNameIx = cursor.getColumnIndex("artname");
            int painterNameIx = cursor.getColumnIndex("paintername");
            int yearIx = cursor.getColumnIndex("year");
            int imageIx = cursor.getColumnIndex("image");

            while (cursor.moveToNext()) {

                artNameText.setText(cursor.getString(artNameIx));
                painterNameText.setText(cursor.getString(painterNameIx));
                yearText.setText(cursor.getString(yearIx));

                byte[] byteArray = cursor.getBlob(imageIx);
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                imageView.setImageBitmap(bitmap);
            }
            cursor.close();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.art_edit, menu);
        MenuItem item = menu.findItem(R.id.edit_art);
        if (info.matches("new")) {
            item.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        updateBtn.setVisibility(View.VISIBLE);
        artNameText.setEnabled(true);
        painterNameText.setEnabled(true);
        yearText.setEnabled(true);
        imageView.setEnabled(true);

        return super.onOptionsItemSelected(item);
    }

    public void selectImage(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery, 2);
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            Uri imageData = data.getData();
            try {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageData);
                        selectedImage = ImageDecoder.decodeBitmap(source);
                        imageView.setImageBitmap(selectedImage);
                    } else {
                        selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                        imageView.setImageBitmap(selectedImage);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save(View view) {

        if (emptyController()) {
            Toast.makeText(this, getString(R.string.not_empty), Toast.LENGTH_LONG).show();
        } else {

            String artName = artNameText.getText().toString();
            String painterName = painterNameText.getText().toString();
            String year = yearText.getText().toString();
            Bitmap smallImage = makeSmallImage(selectedImage, 150);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
            byte[] byteArray = outputStream.toByteArray();


            try {
                database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

                String stringSql = "INSERT INTO arts (artname,paintername,year,image) VALUES (?,?,?,?)";
                SQLiteStatement sqLiteStatement = database.compileStatement(stringSql);
                sqLiteStatement.bindString(1, artName);
                sqLiteStatement.bindString(2, painterName);
                sqLiteStatement.bindString(3, year);
                sqLiteStatement.bindBlob(4, byteArray);
                sqLiteStatement.execute();

            } catch (Exception e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(MainActivity2.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

    }

    public Bitmap makeSmallImage(Bitmap image, Integer maximumSize) {
        int widht = image.getWidth();
        int heigth = image.getHeight();
        float bitmapRatio = (float) widht / (float) heigth;
        if (bitmapRatio > 1) {
            widht = maximumSize;
            heigth = (int) (widht / bitmapRatio);
        } else {
            heigth = maximumSize;
            widht = (int) (heigth * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, widht, heigth, true);
    }

    public void update(View view) {

        if (emptyController()) {
            Toast.makeText(this, getString(R.string.not_empty), Toast.LENGTH_LONG).show();
        } else {
            Intent intent = getIntent();
            int idIx = intent.getIntExtra("artId", 1);
            String ardIdIx = String.valueOf(idIx);
            String artName = artNameText.getText().toString();
            String painterName = painterNameText.getText().toString();
            String year = yearText.getText().toString();
            Bitmap smallImage = makeSmallImage(selectedImage, 150);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
            String string = "update arts set artname=?,paintername=?,year=?,image=? where id=?";

            SQLiteStatement sqLiteStatement = database.compileStatement(string);
            sqLiteStatement.bindString(1, artName);
            sqLiteStatement.bindString(2, painterName);
            sqLiteStatement.bindString(3, year);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.bindString(5, (ardIdIx));
            sqLiteStatement.execute();

            intent = new Intent(MainActivity2.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    public boolean emptyController() {
        boolean emptyControl = artNameText.getText().toString().matches("") || painterNameText.getText().toString().matches("")
                || yearText.getText().toString().matches("") || selectedImage == null;
        return emptyControl;
    }
}