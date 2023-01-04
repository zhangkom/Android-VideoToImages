package com.polarxiong.videotoimages;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URISyntaxException;


public class MainActivity extends Activity implements VideoToFrames.Callback {
//    private static final int REQUEST_FOR_VIDEO_FILE = 1000;

    private static final int REQUEST_CODE_GET_FILE_PATH = 1;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private OutputImageFormat outputImageFormat;
    private MainActivity self = this;
    private String outputDir;
    private String inputPath;
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            String str = (String) msg.obj;
            updateInfo(str);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initImageFormatSpinner();

        final Button buttonFilePathInput = (Button) findViewById(R.id.button_file_path_input);
        buttonFilePathInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
                } else {
                    getFilePath(REQUEST_CODE_GET_FILE_PATH);
                }
            }
        });

        final Button buttonStart = (Button) findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextOutputFolder = (EditText) findViewById(R.id.folder_created);
                outputDir = Environment.getExternalStorageDirectory() + "/" + editTextOutputFolder.getText().toString();
                EditText editTextInputFilePath = (EditText) findViewById(R.id.file_path_input);
                String inputFilePath = editTextInputFilePath.getText().toString();
                VideoToFrames videoToFrames = new VideoToFrames();
                videoToFrames.setCallback(self);
                try {
                    videoToFrames.setSaveFrames(outputDir, outputImageFormat);
                    updateInfo("运行中...");
                    videoToFrames.decode(inputFilePath);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getFilePath(REQUEST_CODE_GET_FILE_PATH);
                } else {
                    Toast.makeText(this, "需要开启文件读写权限", Toast.LENGTH_SHORT).show();
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initImageFormatSpinner() {
        Spinner barcodeFormatSpinner = (Spinner) findViewById(R.id.image_format);
        ArrayAdapter<OutputImageFormat> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, OutputImageFormat.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        barcodeFormatSpinner.setAdapter(adapter);
        barcodeFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                outputImageFormat = OutputImageFormat.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getFilePath(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "选择视频文件"), requestCode);
        } else {
            new AlertDialog.Builder(this).setTitle("未找到文件管理器")
                    .setMessage("请安装文件管理器以选择文件")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GET_FILE_PATH && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
//                inputPath = data.getData().getPath();
//                tv_input.setText(inputPath);

                try {
                    inputPath = Util.getFilePath(this, data.getData());
                    Log.e("java", "inputPath:" + inputPath);

                    int id = R.id.file_path_input;
                    EditText editText = (EditText) findViewById(id);
                    editText.setText(inputPath);

                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(inputPath);
                    String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    File f = new File(inputPath);
                    long fileSize = f.length();

                    String before = "inputPath:" + inputPath + "\n" + "width:" + width + "\n" + "height:" + height + "\n" + "bitrate:" + bitrate + "\n"
                            + "fileSize:" + Formatter.formatFileSize(MainActivity.this, fileSize) + "\n" + "duration(ms):" + duration;
                    //tv_input.setText(before);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
//        int id = 0;
//        Log.e("java", "requestCode:" + requestCode);
//        switch (requestCode) {
//            case REQUEST_CODE_GET_FILE_PATH:
//                id = R.id.file_path_input;
//                break;
//        }
//        if (resultCode == Activity.RESULT_OK) {
//            EditText editText = (EditText) findViewById(id);
//            String curFileName = getRealPathFromURI(data.getData());
//            Log.e("java", "curFileName:" + curFileName);
//            editText.setText(curFileName);
//        }
    }

    private void updateInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    public void onDecodeFrame(int index) {
        Message msg = handler.obtainMessage();
        msg.obj = "运行中...第" + index + "帧";
        handler.sendMessage(msg);
    }

    public void onFinishDecode() {
        Message msg = handler.obtainMessage();
        msg.obj = "完成！所有图片已存储到" + outputDir;
        handler.sendMessage(msg);
    }

    private String getRealPathFromURI(Uri contentURI) {
        Log.e("java", "contentURI:" + contentURI);
        String result;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentURI, proj, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}
