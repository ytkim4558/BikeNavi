/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nagnek.bikenavi.POI;
import com.nagnek.bikenavi.Track;
import com.nagnek.bikenavi.User;

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
    public static final String KEY_POI_ID = "poiID"; // 장소 아이디
    public static final String KEY_POI_NAME = "poiName"; // 장소 이름
    public static final String KEY_POI_ADDRESS = "poiAddress"; // 장소 이름
    public static final String KEY_POI_LAT_LNG = "poiLatLng";   // 장소 좌표
    // Track Table Columns names 경로 테이블
    public static final String KEY_START_POI_ID = "start_poi_id"; // 출발 장소 id
    public static final String KEY_DEST_POI_ID = "dest_poi_id"; // 도착 장소 id
    public static final String KEY_JSON_STOP_POI_ID_ARRAY = "json_stop_poi_id_list"; // 경유지 장소 id 리스트 json

    public static final String KEY_TRACK_ID = "trackID"; // 경로 아이디

    private static final String TAG = SQLiteHandler.class.getSimpleName();
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name;
    private static final String DATABASE_NAME = "android_api";
    // Login table name
    private static final String TABLE_USER = "USERS";

    // 비로그인 유저가 검색한 경로 테이블
    private static final String TABLE_USER_TRACK = "USER_TRACK";

    // Ip table name
    private static final String TABLE_IP = "IPS";
    // poi table name 장소 저장용 테이블
    private static final String TABLE_POI = "POI";

    // poi table name 장소 기록 저장용 테이블
    private static final String TABLE_USER_POI = "USER_POI";

    // track table name 경로 저장용 테이블
    private static final String TABLE_TRACK = "TRACK";
    // poi 즐겨찾기용 table name
    private static final String TABLE_BOOKMARK_POI = "BOOKMARK_POI";
    // track 즐겨찾기용 table name , 유저가 즐겨찾기한 경로들 저장용 테이블
    private static final String TABLE_BOOKMARK_TRACK = "BOOKMARK_TRACK";
    // Ip table Columns names
    private static final String KEY_IP = "ip";

    private static SQLiteHandler mSqliteHandler;

    private SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized SQLiteHandler getInstance(Context context) {
        // application Context를 사용한다, 이것은 액티비티의 context를 뜻하지 않게 leak 되지 않게 한다.

        if (mSqliteHandler == null) {
            mSqliteHandler = new SQLiteHandler(context.getApplicationContext());
        }
        return mSqliteHandler;
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
                + KEY_POI_ADDRESS + " TEXT,"
                + KEY_POI_LAT_LNG + " TEXT UNIQUE,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";

        db.execSQL(CREATE_POI_TABLE);

        Log.d(TAG, "poi tables created");

        // 경로 테이블
        String CREATE_TRACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TRACK + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_START_POI_ID + " INTEGER,"
                + KEY_DEST_POI_ID + " INTEGER,"
                + KEY_JSON_STOP_POI_ID_ARRAY + " TEXT,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";

        db.execSQL(CREATE_TRACK_TABLE);

        Log.d(TAG, "track tables created");

        // 북마크된 장소 ID 테이블
        String CREATE_BOOKMARK_POI_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKMARK_POI + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_POI_ID + " INTEGER,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";

        db.execSQL(CREATE_BOOKMARK_POI_TABLE);

        // 북마크된 경로 ID 테이블
        String CREATE_BOOKMARK_TRACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKMARK_TRACK + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_TRACK_ID + " INTEGER,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")";

        db.execSQL(CREATE_BOOKMARK_TRACK_TABLE);

        Log.d(TAG, "bookmark track tables created");

        String CREATE_LOCAL_USER_TRACK = "CREATE TABLE IF NOT EXISTS " + TABLE_USER_TRACK + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_TRACK_ID + " INTEGER,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")"; // 경로 테이블
        db.execSQL(CREATE_LOCAL_USER_TRACK);

        Log.d(TAG, "유저가 검색한 경로 테이블");

        String CREATE_LOCAL_USER_POI = "CREATE TABLE IF NOT EXISTS " + TABLE_USER_POI + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_POI_ID + " INTEGER,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_UPDATED_AT + " TEXT,"
                + KEY_LAST_USED_AT + " TEXT" + ")"; // 경로 테이블
        db.execSQL(CREATE_LOCAL_USER_POI);

        Log.d(TAG, "유저가 검색한 경로 테이블");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_IP);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_POI);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACK);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARK_POI);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARK_TRACK);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_POI);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_TRACK);

            // Create tables again
            onCreate(db);
        }
    }

    /**
     * Storing user details in database
     */
    public void addUser(UserType userType, User user, String created_at, String updated_at, String last_used_at) {    // userType : 구글 유저인지 자체사이트 회원인지 구별하는 타입
        if (!checkIfUserExists(user, userType)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            switch (userType) {
                case BIKENAVI:
                    values.put(KEY_EMAIL, user.bike_navi_email); // 이메일
                    Log.d(TAG, "values.put email : " + user.bike_navi_email);
                    break;
                case GOOGLE:
                    values.put(KEY_GOOGLE_EMAIL, user.google_email); // 이메일
                    Log.d(TAG, "values.put googleemail : " + user.google_email);
                    break;
                case FACEBOOK:
                    values.put(KEY_FACEBOOK_NAME, user.facebook_user_name); // 이름
                    values.put(KEY_FACEBOOK_ID, user.facebook_id); // 아이디
                    Log.d(TAG, "values.put fbname : " + user.facebook_user_name);
                    Log.d(TAG, "values.put fbid : " + user.facebook_id);
                    break;
                case KAKAO:
                    values.put(KEY_KAKAO_NICK_NAME, user.kakaoNickName); // 닉네임
                    values.put(KEY_KAKAO_ID, user.kakao_id); // 카카오아이디
                    Log.d(TAG, "values.put kakaonick : " + user.kakaoNickName);
                    Log.d(TAG, "values.put kakaoID : " + user.kakao_id);
                    break;
            }

            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);

            values.put(KEY_UPDATED_AT, updated_at); // updated_at
            Log.d(TAG, "values.put updated_at: " + updated_at);

            values.put(KEY_LAST_USED_AT, last_used_at); //last_used_at
            Log.d(TAG, "values.put last_used_at: " + last_used_at);

            // Inserting Row
            long id = db.insert(TABLE_USER, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New user inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "user already existed in sqlite: ");
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

    public boolean checkIfUserExists(User user, UserType userType) {
        boolean result = false;
        switch (userType) {
            case BIKENAVI:  //자체사이트
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_EMAIL, user.bike_navi_email);
                break;
            case GOOGLE:
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_GOOGLE_EMAIL, user.google_email);
                break;
            case FACEBOOK:
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_FACEBOOK_ID, user.facebook_id);
                break;
            case KAKAO:
                result = checkIfExists(KEY_ID, TABLE_USER, KEY_KAKAO_ID, user.kakao_id);
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

    public boolean checkIfExists(String fieldObjectId, String tableName, String fieldObjectName, int objectName) {
        boolean recordExists = false;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + fieldObjectId + " FROM " + tableName + " WHERE " + fieldObjectName + " = " + objectName, null);

        if (cursor != null) {

            if (cursor.getCount() > 0) {
                recordExists = true;
            }
        }

        cursor.close();
        db.close();

        return recordExists;
    }

    public boolean checkIfExists(String fieldObjectId, String tableName, String fieldObjectName1, int objectName1, String fieldObjectName2, int objectName2, String fieldObjectName3, String objectName3) {
        boolean recordExists = false;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        if (objectName3 != null) {
            cursor = db.rawQuery("SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = " + objectName1 + " AND " + fieldObjectName2 + " = " + objectName2 + " AND " + fieldObjectName3 + " = '" + objectName3 + "'", null);
            Log.d(TAG, "SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = " + objectName1 + " AND " + fieldObjectName2 + " = " + objectName2 + " AND " + fieldObjectName3 + " = '" + objectName3);
        } else {
            cursor = db.rawQuery("SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = " + objectName1 + " AND " + fieldObjectName2 + " = " + objectName2, null);
            Log.d(TAG, "SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = " + objectName1 + " AND " + fieldObjectName2 + " = " + objectName2);
        }
        // SELECT 필드아이디 FROM 테이블이름 WHERE 필드명 = '변수명'

        if (cursor != null) {

            if (cursor.getCount() > 0) {
                recordExists = true;
            }
        }

        cursor.close();
        db.close();

        return recordExists;
    }

    public boolean checkIfExists(String fieldObjectId, String tableName, String fieldObjectName1, String objectName1, String fieldObjectName2, String objectName2, String fieldObjectName3, String objectName3) {
        boolean recordExists = false;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        if (objectName3 != null) {
            cursor = db.rawQuery("SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = '" + objectName1 + "'" + " AND " + fieldObjectName2 + " = '" + objectName2 + "'" + " AND " + fieldObjectName3 + " = '" + objectName3 + "'", null);
            Log.d(TAG, "SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = '" + objectName1 + "'" + " AND " + fieldObjectName2 + " = '" + objectName2 + "'" + " AND " + fieldObjectName3 + " = '" + objectName3);
        } else {
            cursor = db.rawQuery("SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = '" + objectName1 + "'" + " AND " + fieldObjectName2 + " = '" + objectName2 + "'", null);
            Log.d(TAG, "SELECT " + fieldObjectId + " FROM " + tableName + " WHERE "
                    + fieldObjectName1 + " = '" + objectName1 + "'" + " AND " + fieldObjectName2 + " = '" + objectName2 + "'" + " AND " + fieldObjectName3 + " = '" + objectName3);
        }
        // SELECT 필드아이디 FROM 테이블이름 WHERE 필드명 = '변수명'

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
    public HashMap<String, String> getUserNickname(UserType userType) {
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
     * 이미 로그인한 유저 정보를 db에서 가져옴. 유저 맞춤형 리스트를 요청하는 용도
     */
    public HashMap<String, String> getLoginedUserDetails(UserType userType) {
        HashMap<String, String> user = new HashMap<String, String>();
        String selectQuery = "SELECT * FROM " + TABLE_USER;

        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null) {
            Cursor cursor = db.rawQuery(selectQuery, null);
            // Move to first row
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    switch (userType) {
                        case BIKENAVI:
                            user.put(KEY_EMAIL, cursor.getString(cursor.getColumnIndex(KEY_EMAIL)));
                            break;
                        case FACEBOOK:
                            user.put(KEY_FACEBOOK_ID, cursor.getString(cursor.getColumnIndex(KEY_FACEBOOK_ID)));
                            break;
                        case KAKAO:
                            user.put(KEY_KAKAO_ID, cursor.getString(cursor.getColumnIndex(KEY_KAKAO_ID)));
                            break;
                        case GOOGLE:
                            user.put(KEY_GOOGLE_EMAIL, cursor.getString(cursor.getColumnIndex(KEY_GOOGLE_EMAIL)));
                            break;
                    }
                }
                cursor.close();
            }
            db.close();
        }
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
     * 비로그인 유저가 검색했던 POI 기록 저장하는 함수
     */
    public void addLocalUserPOI(POI poi) {
        if (!checkIfUserPOIExists(poi)) {
            int poiId = getPOIID(poi);
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_POI_ID, poiId); // 장소 아이디
            Log.d(TAG, "values.put poiId : " + poiId);

            String created_at = getDateTime();
            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);

            values.put(KEY_UPDATED_AT, created_at); // created_at 값 복사 , 어차피 같은 시각이므로
            Log.d(TAG, "values.put updated_at: " + created_at);

            values.put(KEY_LAST_USED_AT, created_at); // created_at 값 복사
            Log.d(TAG, "values.put last_used_at: " + created_at);

            // Inserting Row
            long id = db.insert(TABLE_USER_POI, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New userpoi inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "poiLatLng already existed in sqlite: " + poi.latLng);
        }
    }

    /**
     * Storing poi detailes in database
     */
    public void addPOI(POI poi) {
        if (!checkIfPOIExists(poi.latLng)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_POI_NAME, poi.name); // 장소이름
            Log.d(TAG, "values.put poiName : " + poi.name);

            values.put(KEY_POI_ADDRESS, poi.address); // 장소주소
            Log.d(TAG, "values.put poiName : " + poi.address);

            values.put(KEY_POI_LAT_LNG, poi.latLng); // 장소좌표
            Log.d(TAG, "values.put poiLatLng : " + poi.latLng);

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
            Log.d(TAG, "poiLatLng already existed in sqlite: " + poi.latLng);
        }
    }

    public boolean checkIfUserPOIExists(POI poi) {
        Integer id = getPOIID(poi);
        if (id != null) {
            return checkIfExists(KEY_ID, TABLE_USER_POI, KEY_POI_ID, id);
        } else {
            return false;
        }
    }

    public boolean checkIfPOIExists(String latLng) {
        return checkIfExists(KEY_ID, TABLE_POI, KEY_POI_LAT_LNG, latLng);
    }

    // 사용한 시각 업데이트 (좌표)
    public void updateLastUsedAtUserPOI(POI poi) {
        if (checkIfUserPOIExists(poi)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            String last_used_at = getDateTime();

            values.put(KEY_LAST_USED_AT, last_used_at);
            Log.d(TAG, "values.put last_used_at: " + last_used_at);

            // Updating Row
            long id = db.update(TABLE_USER_POI, values, KEY_POI_ID + " = " + getPOIID(poi), null);
            db.close(); // Closing database connection

            Log.d(TAG, "New poi update on sqlite: " + id);
        } else {
            Log.d(TAG, "user poi not existed in sqlite: ");
        }
    }

    // 사용한 시각 업데이트 (좌표)
    public void updateLastUsedAtPOI(String latLng) {
        if (checkIfPOIExists(latLng)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            String last_used_at = getDateTime();

            values.put(KEY_LAST_USED_AT, last_used_at);
            Log.d(TAG, "values.put last_used_at: " + last_used_at);

            // Updating Row
            long id = db.update(TABLE_POI, values, KEY_POI_LAT_LNG + " = '" + latLng + "'", null);
            db.close(); // Closing database connection

            Log.d(TAG, "New poi update on sqlite: " + id);
        } else {
            Log.d(TAG, "poiLatLng not existed in sqlite: " + latLng);
        }
    }

    public String getLastUsedAtUsingUserPOI(POI poi) {
        String lastUsedAt = null;

        String POI_LAST_USED_AT_SELECT_QUERY =
                "SELECT " + KEY_LAST_USED_AT + " FROM " + TABLE_USER_POI + " WHERE " + KEY_POI_ID + " = " + getPOIID(poi);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(POI_LAST_USED_AT_SELECT_QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                lastUsedAt = cursor.getString(cursor.getColumnIndex(KEY_LAST_USED_AT));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return lastUsedAt;
    }

    // 사용한 시각 내림차순으로 정렬됨.
    public List<POI> getAllLocalUserPOI() {

        List<POI> poiDetails = new ArrayList<>();

        String POI_DETAIL_SELECT_QUERY_ORDER_BY_LAST_USED_AT =
                "SELECT " + KEY_POI_ID + " FROM " + TABLE_USER_POI + " ORDER BY " + KEY_LAST_USED_AT + " DESC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(POI_DETAIL_SELECT_QUERY_ORDER_BY_LAST_USED_AT, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    int poiID = cursor.getInt(cursor.getColumnIndex(KEY_POI_ID));

                    String POI_SELECT_QUERY =
                            "SELECT * FROM " + TABLE_POI + " WHERE " + KEY_ID + " = " + poiID;

                    SQLiteDatabase db2 = getReadableDatabase();
                    Cursor cursor2 = db2.rawQuery(POI_SELECT_QUERY, null);

                    try {
                        if (cursor2.moveToFirst()) {
                            POI poi = new POI();
                            poi.name = cursor2.getString(cursor2.getColumnIndex(KEY_POI_NAME));
                            poi.address = cursor2.getString(cursor2.getColumnIndex(KEY_POI_ADDRESS));
                            poi.latLng = cursor2.getString(cursor2.getColumnIndex(KEY_POI_LAT_LNG));

                            poiDetails.add(poi);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error while trying to get posts from database", e);
                    } finally {
                        if (cursor2 != null && !cursor2.isClosed()) {
                            cursor2.close();
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return poiDetails;
    }

    /**
     * poi 정보 삭제
     */
    public void deleteUserPOIRow(POI poi) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();
            db.execSQL("delete from " + TABLE_USER_POI + " where " + KEY_POI_ID + " = " + getPOIID(poi));
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error while tryign to delete user detail", e);
        } finally {
            db.endTransaction();
        }
    }

    // poiID를 이용하여 poiName 가져오기
    public String getPOINameUsingPOIID(int poiID) {
        String poiName = null;

        String POI_NAME_SELECT_QUERY =
                "SELECT " + KEY_POI_NAME + " FROM " + TABLE_POI + " WHERE " + KEY_ID + " = " + poiID;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(POI_NAME_SELECT_QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                poiName = cursor.getString(cursor.getColumnIndex(KEY_POI_NAME));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return poiName;
    }

    public Integer getPOIID(POI poi) {
        Integer poiId = null;

        String POI_ID_SELECT_QUERY =
                "SELECT " + KEY_ID + " FROM " + TABLE_POI + " WHERE " + KEY_POI_LAT_LNG + " = '" + poi.latLng + "'";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(POI_ID_SELECT_QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                poiId = cursor.getInt(cursor.getColumnIndex(KEY_ID));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return poiId;
    }

    /**
     * 경로 검색 관련
     */

    /**
     * Storing track detailes in database
     */
    public void addTrack(Track track) {
        Gson gson = new Gson();
        if (!checkIfTrackExists(track)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_START_POI_ID, getPOIID(track.startPOI)); // 출발 장소 POI id
            Log.d(TAG, "values.put 출발 POI id : " + getPOIID(track.startPOI));

            values.put(KEY_DEST_POI_ID, getPOIID(track.destPOI)); // 도착 장소 POI id
            Log.d(TAG, "values.put 도착 POI id : " + getPOIID(track.destPOI));

            values.put(KEY_JSON_STOP_POI_ID_ARRAY, gson.toJson(track.stop_poi_list)); // 경유지 장소 POI 리스트
            Log.d(TAG, "values.put 경유지 POI 리스트 : " + gson.toJson(track.stop_poi_list));

            String created_at = getDateTime();
            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);

            values.put(KEY_UPDATED_AT, created_at); // created_at 값 복사 , 어차피 같은 시각이므로
            Log.d(TAG, "values.put updated_at: " + created_at);

            values.put(KEY_LAST_USED_AT, created_at); // created_at 값 복사
            Log.d(TAG, "values.put last_used_at: " + created_at);

            // Inserting Row
            long id = db.insert(TABLE_TRACK, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New track inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "trackinfo already existed in sqlite: " + gson.toJson(track));
        }
    }

    /**
     * Storing local_user_track detailes in database , 비로그인 유저가 검색한 경로 테이블
     */
    public void addLocalUserTrack(Track track) {
        Gson gson = new Gson();
        if (!checkIfUserTrackExists(track)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_TRACK_ID, getTrackIDUsingTrack(track)); // 출발 장소 POI id
            Log.d(TAG, "values.put 트랙 id : " + getTrackIDUsingTrack(track));

            String created_at = getDateTime();
            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);

            values.put(KEY_UPDATED_AT, created_at); // created_at 값 복사 , 어차피 같은 시각이므로
            Log.d(TAG, "values.put updated_at: " + created_at);

            values.put(KEY_LAST_USED_AT, created_at); // created_at 값 복사
            Log.d(TAG, "values.put last_used_at: " + created_at);

            // Inserting Row
            long id = db.insert(TABLE_USER_TRACK, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New track inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "trackinfo already existed in sqlite: " + gson.toJson(track));
        }
    }

    public boolean checkIFBookmarkedTrackExists(Track track) {
        return checkIfExists(KEY_ID, TABLE_BOOKMARK_TRACK, KEY_TRACK_ID, getTrackIDUsingTrack(track));
    }
    /**
     * Storing bookmarked track id in database
     */
    public void addBookmarkedTrack(Track track) {
        Gson gson = new Gson();
        if (!checkIFBookmarkedTrackExists(track)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_TRACK_ID, getTrackIDUsingTrack(track));
            Log.d(TAG, "values.put 경로 아이디 : " + getTrackIDUsingTrack(track));

            String created_at = getDateTime();
            values.put(KEY_CREATED_AT, created_at); // created_at
            Log.d(TAG, "values.put created_at: " + created_at);

            values.put(KEY_UPDATED_AT, created_at); // created_at 값 복사 , 어차피 같은 시각이므로
            Log.d(TAG, "values.put updated_at: " + created_at);

            values.put(KEY_LAST_USED_AT, created_at); // created_at 값 복사
            Log.d(TAG, "values.put last_used_at: " + created_at);

            // Inserting Row
            long id = db.insert(TABLE_BOOKMARK_TRACK, null, values);
            db.close(); // Closing database connection

            Log.d(TAG, "New track inserted into sqlite: " + id);
        } else {
            Log.d(TAG, "trackinfo already existed in sqlite: " + gson.toJson(track));
        }
    }

    public void updateBookmarkedTrack(Track track) {
        Gson gson = new Gson();
        if (checkIFBookmarkedTrackExists(track)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            String last_used_at = getDateTime();

            values.put(KEY_LAST_USED_AT, last_used_at); // 현재 시각
            Log.d(TAG, "values.put last_used_at: " + last_used_at);

            // Inserting Row
            long id = db.update(TABLE_BOOKMARK_TRACK, values, KEY_TRACK_ID + " = " + getTrackIDUsingTrack(track), null);
            db.close(); // Closing database connection

            Log.d(TAG, "update track into sqlite: " + id);
        } else {
            Log.d(TAG, "trackinfo not existed in sqlite: " + gson.toJson(track));
        }
    }

    // 출발지, 도착지, 경유지 리스트들을 보고 이미 있었는지 확인
    public boolean checkIfUserTrackExists(Track track) {
        int trackID = getTrackIDUsingTrack(track); // 출발장소 POI id
        Log.d(TAG, "track id: " + trackID);
        return checkIfExists(KEY_ID, TABLE_USER_TRACK, KEY_TRACK_ID, trackID);
    }


    // 출발지, 도착지, 경유지 리스트들을 보고 이미 있었는지 확인
    public boolean checkIfTrackExists(Track track) {
        Gson gson = new Gson();
        int startPOIID = getPOIID(track.startPOI); // 출발장소 POI id
        Log.d(TAG, "출발장소 POI id: " + startPOIID);
        int destPOIID = getPOIID(track.destPOI);   // 도착장소 POI id
        Log.d(TAG, "도착장소 POI id: " + destPOIID);
        String gsonStopPOIList;
        if (track.stop_poi_list == null) {
            gsonStopPOIList = null;
        } else {
            gsonStopPOIList = gson.toJson(track.stop_poi_list); // 경유지 POI 리스트
        }
        Log.d(TAG, "Json으로 변환된 경유지 POI 리스트 : " + gsonStopPOIList);
        return checkIfExists(KEY_ID, TABLE_TRACK, KEY_START_POI_ID, startPOIID, KEY_DEST_POI_ID, destPOIID, KEY_JSON_STOP_POI_ID_ARRAY, gsonStopPOIList);
    }

    // 사용한 시각 업데이트 (좌표)
    public void updateLastUsedAtUserTrack(Track track) {
        Gson gson = new Gson();
        if (checkIfUserTrackExists(track)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            String last_used_at = getDateTime();

            values.put(KEY_LAST_USED_AT, last_used_at);
            Log.d(TAG, "values.put last_used_at: " + last_used_at);
            // Updating Row
            int id = db.update(TABLE_USER_TRACK, values,
                    KEY_TRACK_ID + " = " + getTrackIDUsingTrack(track), null);

            db.close(); // Closing database connection

            Log.d(TAG, "track update on sqlite: " + id);
        } else {
            Log.d(TAG, "track not existed in sqlite: " + gson.toJson(track));
        }
    }

    // 사용한 시각 업데이트 (경로)
    public void updateLastUsedAtTrack(Track track) {
        Gson gson = new Gson();
        if (checkIfTrackExists(track)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            String last_used_at = getDateTime();

            values.put(KEY_LAST_USED_AT, last_used_at);
            Log.d(TAG, "values.put last_used_at: " + last_used_at);

            // Updating Row
            long id = db.update(TABLE_TRACK, values,
                    KEY_ID + " = " + getTrackIDUsingTrack(track), null);

            db.close(); // Closing database connection

            Log.d(TAG, "track update on sqlite: " + id);
        } else {
            Log.d(TAG, "track not existed in sqlite: " + gson.toJson(track));
        }
    }

    // id 가져오기
    public int getTrackIDUsingTrack(Track track) {
        int trackID = -1;
        Gson gson = new Gson();

        String TRACK_LAST_USED_AT_SELECT_QUERY = null;
        if (track.stop_poi_list != null) {
            TRACK_LAST_USED_AT_SELECT_QUERY =
                    "SELECT " + KEY_ID + " FROM " + TABLE_TRACK + " WHERE " + KEY_START_POI_ID + " = " + getPOIID(track.startPOI) + " AND " +
                            KEY_DEST_POI_ID + " = " + getPOIID(track.destPOI) + " AND " + KEY_JSON_STOP_POI_ID_ARRAY + " = '" + gson.toJson(track.stop_poi_list) + "'";
        } else {
            TRACK_LAST_USED_AT_SELECT_QUERY =
                    "SELECT " + KEY_ID + " FROM " + TABLE_TRACK + " WHERE " + KEY_START_POI_ID + " = " + getPOIID(track.startPOI) + " AND " +
                            KEY_DEST_POI_ID + " = " + getPOIID(track.destPOI);
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(TRACK_LAST_USED_AT_SELECT_QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                trackID = cursor.getInt(cursor.getColumnIndex(KEY_ID));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return trackID;
    }

    // 사용한 시각 가져오기
    public String getLastUsedAtUsingTrack(Track track) {
        String lastUsedAt = null;
        Gson gson = new Gson();

        String TRACK_LAST_USED_AT_SELECT_QUERY = null;
        if (track.stop_poi_list != null) {
            TRACK_LAST_USED_AT_SELECT_QUERY =
                    "SELECT " + KEY_LAST_USED_AT + " FROM " + TABLE_TRACK + " WHERE " + KEY_START_POI_ID + " = '" + getPOIID(track.startPOI) + "'" + " AND " +
                            KEY_DEST_POI_ID + " = '" + getPOIID(track.destPOI) + "'" + " AND " + KEY_JSON_STOP_POI_ID_ARRAY + " = '" + gson.toJson(track.stop_poi_list) + "'";
        } else {
            TRACK_LAST_USED_AT_SELECT_QUERY =
                    "SELECT " + KEY_LAST_USED_AT + " FROM " + TABLE_TRACK + " WHERE " + KEY_START_POI_ID + " = '" + getPOIID(track.startPOI) + "'" + " AND " +
                            KEY_DEST_POI_ID + " = " + getPOIID(track.destPOI);
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(TRACK_LAST_USED_AT_SELECT_QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                lastUsedAt = cursor.getString(cursor.getColumnIndex(KEY_LAST_USED_AT));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return lastUsedAt;
    }

    // 사용한 시각 내림차순으로 정렬됨. 로컬 db에서 비로그인 유저가 검색한 기록 정보
    public List<Track> getAllLocalUserTrack() {
        Log.d(TAG, "getAllLocalUserTrack()");
        Gson gson = new Gson();
        List<Track> trackDetails = new ArrayList<>();

        String TRACK_DETAIL_SELECT_QUERY_ORDER_BY_LAST_USED_AT =
                "SELECT " + KEY_TRACK_ID + " FROM " + TABLE_USER_TRACK + " ORDER BY " + KEY_LAST_USED_AT + " DESC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(TRACK_DETAIL_SELECT_QUERY_ORDER_BY_LAST_USED_AT, null);

        try {
            if (cursor.moveToFirst()) {
                do {

                    int trackID = cursor.getInt(cursor.getColumnIndex(KEY_TRACK_ID));

                    Log.d(TAG, "track ID : " + trackID);

                    String TRACK_SELECT_QUERY =
                            "SELECT * FROM " + TABLE_TRACK + " WHERE " + KEY_ID + " = " + trackID;

                    SQLiteDatabase db2 = getReadableDatabase();
                    Cursor cursor2 = db2.rawQuery(TRACK_SELECT_QUERY, null);

                    try {
                        if (cursor2.moveToFirst()) {
                            Track track = new Track();
                            track.startPOI = getPOIUsingPOIID(cursor2.getInt(cursor2.getColumnIndex(KEY_START_POI_ID)));
                            track.destPOI = getPOIUsingPOIID(cursor2.getInt(cursor2.getColumnIndex(KEY_DEST_POI_ID)));
                            track.stop_poi_list = getPOIList((ArrayList<Integer>) gson.fromJson(cursor2.getString(cursor2.getColumnIndex(KEY_JSON_STOP_POI_ID_ARRAY)), new TypeToken<List<Integer>>() {
                            }.getType()));

                            trackDetails.add(track);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error while trying to get posts from database", e);
                    } finally {
                        if (cursor2 != null && !cursor2.isClosed()) {
                            cursor2.close();
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return trackDetails;
    }

    public ArrayList<POI> getPOIList(ArrayList<Integer> poiIdList) {
        if (poiIdList == null) {
            return null;
        }
        ArrayList<POI> poiList = null;
        if (poiIdList.size() > 0) {
            poiList = new ArrayList<POI>();
        }
        for (int poiid : poiIdList) {
            poiList.add(getPOIUsingPOIID(poiid));
        }
        return poiList;
    }

    public POI getPOIUsingPOIID(int poiID) {
        POI poi = null;

        String POI_SELECT_QUERY =
                "SELECT * FROM " + TABLE_POI + " WHERE " + KEY_ID + " = " + poiID;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(POI_SELECT_QUERY, null);

        Log.d(TAG, POI_SELECT_QUERY);

        try {
            if (cursor.moveToFirst()) {
                poi = new POI();
                poi.name = cursor.getString(cursor.getColumnIndex(KEY_POI_NAME));
                poi.address = cursor.getString(cursor.getColumnIndex(KEY_POI_ADDRESS));
                poi.latLng = cursor.getString(cursor.getColumnIndex(KEY_POI_LAT_LNG));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return poi;
    }

    // 북마크된 경로를 생성한 시각 내림차순으로 정렬됨.
    public List<Track> getAllBookmarkedTrack() {
        Log.d(TAG, "getAllBookmarkedTrack()");
        Gson gson = new Gson();
        List<Track> bookmarkedTrackDetails = new ArrayList<>();

        String BOOKMARKED_TRACK_DETAIL_SELECT_QUERY_ORDER_BY_CREATED_AT =
                "SELECT " + KEY_TRACK_ID + " FROM " + TABLE_BOOKMARK_TRACK + " ORDER BY " + KEY_CREATED_AT + " DESC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(BOOKMARKED_TRACK_DETAIL_SELECT_QUERY_ORDER_BY_CREATED_AT, null);

        try {
            if (cursor.moveToFirst()) {
                do {

                    int id = cursor.getInt(cursor.getColumnIndex(KEY_TRACK_ID));
                    String TRACK_DETAIL_SELECT = "SELECT * FROM " + TABLE_TRACK + " WHERE " + KEY_ID + " = " + id;

                    SQLiteDatabase db2 = getReadableDatabase();
                    Cursor cursor2 = db2.rawQuery(TRACK_DETAIL_SELECT, null);

                    try {
                        if (cursor2.moveToFirst()) {
                            Track track = new Track();
                            track.startPOI = getPOIUsingPOIID(cursor2.getInt(cursor2.getColumnIndex(KEY_START_POI_ID)));
                            track.destPOI = getPOIUsingPOIID(cursor2.getInt(cursor2.getColumnIndex(KEY_DEST_POI_ID)));
                            track.stop_poi_list = getPOIList((ArrayList<Integer>) gson.fromJson(cursor2.getString(cursor2.getColumnIndex(KEY_JSON_STOP_POI_ID_ARRAY)), new TypeToken<List<Integer>>() {
                            }.getType()));

                            bookmarkedTrackDetails.add(track);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error while trying to get posts from database while track detail", e);
                    } finally {
                        if (cursor2 != null && !cursor2.isClosed()) {
                            cursor2.close();
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get posts from database while get bookmarked track", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return bookmarkedTrackDetails;
    }


    /**
     * bookmark track 정보 삭제
     */
    public void deleteBookmarkedTrackRow(Track track) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();
            db.execSQL("delete from " + TABLE_BOOKMARK_TRACK + " where " + KEY_TRACK_ID + " = " + getTrackIDUsingTrack(track));
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error while tryign to delete user detail", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * user_track 정보 삭제
     */
    public void deleteUserTrackRow(Track track) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();
            db.execSQL("delete from " + TABLE_USER_TRACK + " where " + KEY_TRACK_ID + " = " + getTrackIDUsingTrack(track));
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error while tryign to delete user_track detail", e);
        } finally {
            db.endTransaction();
        }
    }

    public enum UserType {BIKENAVI, GOOGLE, FACEBOOK, KAKAO}   // 유저정보,
}