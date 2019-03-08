package com.w.im;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.w.im.SQLiteUtil.LiteHelper;

public class SettingsActivity extends AppCompatActivity {

    private LiteHelper db = null;
    // 是否保存
    private boolean saved = false;

    private Button saveBtn = null;
    // 服务器信息
    private EditText address = null;
    private EditText port = null;
    private EditText username = null;
    private EditText password = null;
    private EditText token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = new LiteHelper(this);

        address = findViewById(R.id.address);
        port = findViewById(R.id.port);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        token = findViewById(R.id.token);
        saveBtn = findViewById(R.id.save);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 地址端口必填
                if (address.getText().toString().equals(String.valueOf(""))) {
                    address.setError("Required");
                    return;
                }
                if (port.getText().toString().equals(String.valueOf(""))) {
                    port.setError("Required");
                    return;
                }

                String ADDRESS = address.getText().toString();
                String PORT = port.getText().toString();
                String USERNAME = username.getText().toString();
                String PASSWORD = password.getText().toString();
                String TOKEN = token.getText().toString();

                // 只保存一条
                Cursor cursor = db.getAll();
                if (cursor.getCount() == 0) {
//                    db.insertData(ADDRESS, PORT, USERNAME, PASSWORD, TOKEN);
                } else {
//                    db.updateData("1", ADDRESS, PORT, USERNAME, PASSWORD, TOKEN);
                }

                saved = true;
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (saved) super.onBackPressed();
        // 如果未保存, 询问是否放弃保存
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("WARNING:");
        builder.setMessage("Exit without saving.");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
