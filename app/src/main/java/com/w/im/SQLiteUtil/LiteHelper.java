package com.w.im.SQLiteUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LiteHelper extends SQLiteOpenHelper {

    // 数据库信息
    private final static String DATABASE = "Bluetooth.db";
    private final static String TABLE = "connection";
    private static final String COL_0 = "ID";
    private static final String COL_1 = "SERVICEUUID";
    private static final String COL_2 = "CHARACTERUUID";
    private static final String COL_3 = "DESCRIPTORUUID";

    public LiteHelper(Context context) {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                COL_0 + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_1 + " TEXT NOT NULL," +
                COL_2 + " TEXT NOT NULL," +
                COL_3 + " TEXT NOT NULL" +
                ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public boolean insertData(String serviceUUID, String characterUUID, String descriptorUUID) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_1, serviceUUID);
        cv.put(COL_2, characterUUID);
        cv.put(COL_3, descriptorUUID);
        long res = db.insert(TABLE, null, cv);
        return res != -1;
    }

    /**
     * 获取单行信息
     *
     * @param serviceUUID 行id
     * @return 信息
     */
    public Cursor getByServiceUUID(String serviceUUID) {
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE + " WHERE SERVICEUUID='" + serviceUUID + "'";
        return db.rawQuery(query, null);
    }

    public boolean updateData(String id, String serviceUUID, String characterUUID, String descriptorUUID) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_1, serviceUUID);
        cv.put(COL_2, characterUUID);
        cv.put(COL_3, descriptorUUID);
        db.update(TABLE, cv, "ID=?", new String[]{id});
        return true;
    }

    /**
     * @param serviceUUID 删除行service UUID
     * @return 影响行数
     */
    public Integer deleteByServiceUUID(String serviceUUID) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE, "SERVICEUUID=?", new String[]{serviceUUID});
    }

    /**
     * 获取所有行
     *
     * @return 信息
     */
    public Cursor getAll() {
        SQLiteDatabase db = getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE, null);
    }

    public void deleteAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.rawQuery("DELETE FROM " + TABLE, null).close();
    }
}
