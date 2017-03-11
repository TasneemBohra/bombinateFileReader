package com.tasneembohra.bombinate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Assumptions :
 * 1. A word does not contain any special characters
 * 2. All special characters will be ignored except SPACE eg: The and Th@e are same as @ will be ignored.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int FILE_SELECT_CODE = 0;
    private static final int REQUEST_READ_PERMISSION = 0;
    private View mView;
    private HashMap<String, Integer> wordMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mView = getWindow().getDecorView().getRootView();
        findViewById(R.id.readBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_PERMISSION);
                } else {
                    showFileChooser();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_PERMISSION &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showFileChooser();
        }
    }

    /**
     * Show file chooser
     */
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Snackbar.make(mView, "Please download file manager", Snackbar.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
                Uri uri = data.getData();
                Log.d(TAG, "File Uri: " + uri.toString());
                // Get the path
                String path = getPath(this, uri);
                Log.d(TAG, "onActivityResult: path :" + path);
                readFromFile(path);
                ((AppCompatTextView)findViewById(R.id.fileNameTV)).setText(new File(path).getName());
                ((AppCompatTextView)findViewById(R.id.textTV)).setText(getMapValuesInSortedOrder());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get file path
     * @param context
     * @param uri
     * @return
     * @throws URISyntaxException
     */
    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.Images.Media.DATA };
            try {
                Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Assumptions :
     * 1. A word does not contain any special characters
     * 2. All special characters will be ignored except SPACE eg: The and Th@e are same as @ will be ignored.
     * @param file @{@link String}
     */
    private void readFromFile(String file) {
        try {
            if (".txt".equals(file.substring(file.length() - 4, file.length()))) {
                FileInputStream inputStream = new FileInputStream(file);
                if ( inputStream != null ) {
                    int i;
                    StringBuilder word = null;
                    wordMap = new HashMap<>();
                    while ((i = inputStream.read()) != -1) {
                        if ((i >= 65 && i <= 90) || (i >= 97 && i <= 122) ) {
                            // If char is an alphabet
                            if (word == null) word = new StringBuilder();
                            word.append((char)i);
                        } else if (i == 32) {
                            // If character is space
                            setWordToMap(word);
                            word = null;
                        }
                    }
                    // For last char
                    setWordToMap(word);
                    inputStream.close();
                }
            } else {
                Snackbar.make(mView, "Please select only .txt file", Snackbar.LENGTH_LONG).show();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
    }

    /**
     * Set word to map
     * @param word  @{@link StringBuilder}
     */
    private void setWordToMap(StringBuilder word) {
        if (word != null) {
            if (wordMap.containsKey(word.toString())) {
                wordMap.put(word.toString(), wordMap.get(word.toString()) + 1);
            } else {
                wordMap.put(word.toString(), 1);
            }
        }
    }

    /**
     * Print and get sorted map
     * @return @{@link String}
     */
    private String getMapValuesInSortedOrder() {
        StringBuilder builder = new StringBuilder();
        List<Map.Entry<String, Integer>> list = new LinkedList<>(wordMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        int count = 1;
        for (Map.Entry<String, Integer> entry : list) {
            if (entry.getValue() >= count) {
                builder.append(count).append(" - ");
                count = count + 9;
                builder.append(count).append(" :\n\n");
                count ++;
            }
            builder.append("\t\t").append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n\n");
        }
        return builder.toString();
    }
}
