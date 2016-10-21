/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by user on 2016-09-27.
 */
public class SQLiteHandler extends SQLiteOpenHelper {
    private static final String TAG = SQLiteHandler.class.getSimpleName();

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name;
    private static final String DATABASE_NAME = "android_api";

    // Login table name
    private static final String TABLE_USER = "USERS";

    // Ip table name
    private static final String TABLE_IP = "IPS";

    // poi table name 장소 검색 기록 저장용 테이블
    private static final String TABLE_POI = "POI";

    // 로그인시 통합되는 poi table name 장소 검색 기록 저장용 테이블
    private static final String TABLE_TEMP_POI = "TEMP_POI";

    // track table name 경로 로그 저장용 테이블
    private static final String TABLE_TRACK = "TRACK";

    // 로그인시 통합되는 track table name 경로 로그 저장용 테이블
    private static final String TABLE_TEMp_TRACK = "TEMP_TRACK";

    // poi 즐겨찾기용 table name
    private static final String TABLE_BOOKMARK_POI = "BOOKMARK_POI";

    // track 즐겨찾기용 table name , 유저가 즐겨찾기한 경로들 저장용 테이블
    private static final String TABLE_BOOKMARK_TRACK = "BOOKMARK_TRACK";

    // 테이블 공통 컬럼
    public static final String KEY_ID = "id";
    public static final String KEY_CREATED_AT = "created_at";   // 생성한 시각
    public static final String KEY_UPDATED_AT = "updated_at";   // 수정한 시각
    public static final String KEY_LAST_USED_AT = "last_used_at";   // 마지막으로 사용한 시각

    // Login Table Columns names
    public static final String KEY_EMAIL = "email";
    public static final String KEY_GOOGLE_EMAIL = "googleemail";
    public static final String KEY_KAKAO_ID = "kakaoID";    // 영어로 된 아이디가 아닌 그냥 숫자
    public static final String KEY_KAKAO_NICK_NAME = "kakaoNickName";   // 카카오 닉네임
    public static final String KEY_FACEBOOK_ID = "facebookID"; // 페이스북 유저 아이디 (숫자)
    public static final String KEY_FACEBOOK_NAME = "facebookname";  // 페이스북에서 쓰는 사용자 이름

    // Poi Table Columns names (즐겨찾기 포함)
    public static final String KEY_POI_NAME = "poiName"; // 장소 이름
    public static final String KEY_POI_LAT_LNG = "poiLatLng";   // 장소 좌표

    // Track Table Columns names 경로 테이블
    public static final String KEY_START_POI_NAME = "start_poi_name"; // 출발 장소 이름
    public static final String KEY_START_POI_LAT_LNG = "start_poi_lat_lng"; // 출발 장소 좌표
    public static final String KEY_DEST_POI_NAME = "dest_poi_name"; // 도착 장소 이름
    public static final String KEY_DEST_POI_LAT_LNG = "dest_poi_lat_lng"; // 도착 장소 좌표


    // Ip table Columns names
    private static final String KEY_IP = "ip";

    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOGIN_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_USER + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_EMAIL + " TEXT UNIQUE,"
                + KEY_GOOGLE_EMAIL + " TEXT UNIQUE,"
                + KEY_KAKAO_ID + " TEXT UNIQUE,"
                + KEY_KAKAO_NICK_NAME + " TEXT UNIQUE,"
                + KEY_FACEBOOK_ID + " TEXT UNIQUE,"
                + KEY_FACEBOOK_NAME + " TEXT UNIQUE,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")"; // ip 서버 테이블.. 사실 현재로선 고정값이기 때문에 의미없다. 이전에는 가상서버라 매번 ip가 달라져서 놓았었음.
        db.execSQL(CREATE_LOGIN_TABLE);

        Log.d(TAG, "Database tables created");

        String CREATE_SERVER_IP_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_IP + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_IP + " TEXT UNIQUE,"
                + KEY_CREATED_AT + " TEXT" + ")";

        db.execSQL(CREATE_SERVER_IP_TABLE);

        Log.d(TAG, "ip tables created");

        // 장소 테이블
        String CREATE_POI_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_POI + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_POI_NAME + " TEXT,"
                + KEY_POI_LAT_LNG + " TEXT UNIQUE,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";

        db.execSQL(CREATE_POI_TABLE);

        Log.d(TAG, "poi tables created");

        // 경로 테이블
        String CREATE_TRACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TRACK + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_START_POI_NAME + " TEXT,"
                + KEY_START_POI_LAT_LNG + " TEXT,"
                + KEY_DEST_POI_NAME + " TEXT,"
                + KEY_DEST_POI_LAT_LNG + " TEXT,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";

        db.execSQL(CREATE_TRACK_TABLE);

        Log.d(TAG, "track tables created");

        // 유저 로그인시 임시 POI 테이블 (로그아웃 하면 삭제됨)
        String CREATE_TEMP_POI_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TEMP_POI + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_POI_NAME + " TEXT,"
                + KEY_POI_LAT_LNG + " TEXT UNIQUE,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";
        db.execSQL(CREATE_TEMP_POI_TABLE);
        Log.d(TAG, "temp poi tables created");

        // 유저 로그인시 임시 경로 테이블 (로그아웃 하면 삭제됨)
        String CREATE_TEMP_TRACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TEMp_TRACK + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_START_POI_NAME + " TEXT,"
                + KEY_START_POI_LAT_LNG + " TEXT,"
                + KEY_DEST_POI_NAME + " TEXT,"
                + KEY_DEST_POI_LAT_LNG + " TEXT,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";
        db.execSQL(CREATE_TEMP_TRACK_TABLE);

        Log.d(TAG, "temp track tables created");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IP);

        // Create tables again
        onCreate(db);
    }

    /**
     * Storing user details in database
     */
    public void addUser(UserType userType, String email, String created_at) {    // userType : 구글 유저인지 자체사이트 회원인지 구별하는 타입
        if (!checkIfUserExists(email, userType)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            switch (userType) {
                case BIKENAVI:
                    values.put(KEY_EMAIL, email); // 이메일
                    Log.d(TAG, "values.put email : " + email);
                    break;
                case GOOGLE:
                    values.put(KEY_GOOGLE_EMAIL, email); // 이메일
                    Log.d(TAG, "values.put googleemail : " + email);
                    break;
                case FACEBOOK:
                    values.put(KEY_FACEBOOK_NAME, email); // 이름
                    Log.d(TAG, "values.put fbname : " + email);
                    break;
                case KAKAO:
                    values.put(KEY_KAKAO_NICK_NAME, email); // 닉네임
                    Log.d(TAG, "values.put kakaonick : " + email);
                    break;
            }


            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);
            // Inserting Row
            long id = db.insert(TABLE_USER, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New user inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "user already existed in sqlite: " + email);
        }
    }

    /**
     * Storing ip detailes in database
     */
    public void addIp(String ip) {
        if (!checkIfIPExists(ip)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_IP, ip); // ip
            Log.d(TAG, "values.put ip : " + ip);

            String created_at = getDateTime();
            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);
            // Inserting Row
            long id = db.insert(TABLE_IP, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New ip inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "ip already existed in sqlite: " + ip);
        }
    }

    public boolean checkIfIPExists(String ip) {
        return checkIfExists(KEY_ID, TABLE_IP, KEY_IP, ip);
    }

    public boolean checkIfUserExists(String email, UserType userType) {
        boolean result = false;
        switch (userType) {
            case BIKENAVI:  //자체사이트
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_EMAIL, email);
                break;
            case GOOGLE:
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_GOOGLE_EMAIL, email);
                break;
            case FACEBOOK:
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_FACEBOOK_NAME, email);
                break;
            case KAKAO:
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_KAKAO_NICK_NAME, email);
                break;
        }
        return result;
    }

    public boolean checkIfExists(String fieldObjectId, String tableName, String fieldObjectName, String objectName) {
        boolean recordExists = false;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + fieldObjectId + " FROM " + tableName + " WHERE " + fieldObjectName + " = '" + objectName + "'", null);

        if (cursor != null) {

            if (cursor.getCount() > 0) {
                recordExists = true;
            }
        }

        cursor.close();
        db.close();

        return recordExists;
    }

    // searchTerm과 관련된 레코드 읽기
    public List<String> read(String searchTerm) {
        List<String> recordList = new ArrayList<String>();

        // select query
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM " + TABLE_IP);
        sql.append(" WHERE " + KEY_IP + " LIKE '%" + searchTerm + "%'");

        SQLiteDatabase db = this.getWritableDatabase();

        // execute the query
        Cursor cursor = db.rawQuery(sql.toString(), null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                String ipAddress = cursor.getString(cursor.getColumnIndex(KEY_IP));
                recordList.add(ipAddress);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // return the list of records
        return recordList;
    }

    /**
     * 현재 시간 반환
     *
     * @return 현재시간
     */
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * Getting user data from database
     */
    public HashMap<String, String> getUserDetails(UserType userType) {
        HashMap<String, String> user = new HashMap<String, String>();
        String selectQuery = "SELECT * FROM " + TABLE_USER;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
//            for (int i = 0; i < cursor.getColumnCount(); ++i) {
//                Log.d(TAG, "i : " + i + ":" + cursor.getColumnName(i));
//            }
            switch (userType) {
                case BIKENAVI:
                    user.put(KEY_EMAIL, cursor.getString(cursor.getColumnIndex(KEY_EMAIL)));
                    break;
                case FACEBOOK:
                    user.put(KEY_FACEBOOK_NAME, cursor.getString(cursor.getColumnIndex(KEY_FACEBOOK_NAME)));
                    break;
                case KAKAO:
                    user.put(KEY_KAKAO_NICK_NAME, cursor.getString(cursor.getColumnIndex(KEY_KAKAO_NICK_NAME)));
                    break;
                case GOOGLE:
                    user.put(KEY_GOOGLE_EMAIL, cursor.getString(cursor.getColumnIndex(KEY_GOOGLE_EMAIL)));
                    break;
            }

            user.put("created_at", cursor.getString(cursor.getColumnIndex(KEY_CREATED_AT)));
        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching user from Sqlite: " + user.toString());

        return user;
    }

    /**
     * Getting ip data from database
     */
    public HashMap<String, String> getIpDetails() {
        HashMap<String, String> ip = new HashMap<String, String>();
        String selectQuery = "SELECT * FROM " + TABLE_IP;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            ip.put("ip", cursor.getString(1));
            ip.put("created_at", cursor.getString(2));
            Log.d(TAG, "ip : " + cursor.getString(1));
        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching ip from Sqlite: " + ip.toString());

        return ip;
    }

    /**
     * Recreate database Delete All tables and create them again
     */
    public void deleteUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_USER, null, null);
        db.close();

        Log.d(TAG, "Deleted all user info from sqlite");
    }

    /**
     * Recreate database Delete All tables and create them again
     */
    public void deleteIPs() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_IP, null, null);
        db.close();

        Log.d(TAG, "Deleted all server ip info from sqlite");
    }

    /**
     * Storing poi detailes in database
     */
    public void addPOI(String poiName, String poiLatLng) {
        if (!checkIfIPExists(poiLatLng)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_POI_NAME, poiName); // 장소이름
            Log.d(TAG, "values.put poiName : " + poiName);

            String created_at = getDateTime();
            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);

            values.put(KEY_UPDATED_AT, created_at); // created_at 값 복사 , 어차피 같은 시각이므로
            Log.d(TAG, "values.put updated_at: " + created_at);

            values.put(KEY_LAST_USED_AT, created_at); // created_at 값 복사
            Log.d(TAG, "values.put last_used_at: " + created_at);

            // Inserting Row
            long id = db.insert(TABLE_POI, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New poi inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "poiLatLng already existed in sqlite: " + poiLatLng);
        }
    }

    public enum UserType {BIKENAVI, GOOGLE, FACEBOOK, KAKAO}   // 유저정보,
}
