package com.example.iqtestapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME    = "iqtest.db";
    private static final int DB_VERSION = 2;

    public static final String TABLE_RESULTS = "results";
    public static final String COL_ID        = "_id";
    public static final String COL_NAME      = "name";
    public static final String COL_AGE       = "age";
    public static final String COL_GENDER    = "gender";
    public static final String COL_GAME      = "game_name";
    public static final String COL_DURATION  = "duration";
    public static final String COL_RESULT    = "result";

    public DBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_RESULTS + " ("
                + COL_ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME     + " TEXT, "
                + COL_AGE      + " INTEGER, "
                + COL_GENDER   + " TEXT, "
                + COL_GAME     + " TEXT, "
                + COL_DURATION + " INTEGER, "
                + COL_RESULT   + " TEXT"
                + ");";
        db.execSQL(sql);

        db.execSQL("CREATE TABLE IF NOT EXISTS iq_scores (" +
                "name TEXT, " +
                "age INTEGER, " +
                "gender TEXT, " +
                "iq INTEGER, " +
                "PRIMARY KEY(name, age, gender)" +
                ");");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESULTS);
        db.execSQL("DROP TABLE IF EXISTS iq_scores");
        onCreate(db);
    }

    /** Insert one result row. Returns the new row’s _id or -1 on error. */
    public long insertResult(String name,
                             int age,
                             String gender,
                             String game,
                             long durationSeconds,
                             String result) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_NAME,     name);
            cv.put(COL_AGE,      age);
            cv.put(COL_GENDER,   gender);
            cv.put(COL_GAME,     game);
            cv.put(COL_DURATION, durationSeconds);
            cv.put(COL_RESULT,   result);
            return db.insert(TABLE_RESULTS, null, cv);
        } finally {
            db.close();
        }
    }

    /** Return all distinct players (name, age, gender). */
    public List<PlayerInfo> getAllPlayers() {
        List<PlayerInfo> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_NAME + "," + COL_AGE + "," + COL_GENDER +
                        " FROM " + TABLE_RESULTS +
                        " GROUP BY " + COL_NAME + "," + COL_AGE + "," + COL_GENDER,
                null
        );
        while (c.moveToNext()) {
            String n = c.getString(c.getColumnIndexOrThrow(COL_NAME));
            int    a = c.getInt   (c.getColumnIndexOrThrow(COL_AGE));
            String g = c.getString(c.getColumnIndexOrThrow(COL_GENDER));
            out.add(new PlayerInfo(n, a, g));
        }
        c.close();
        db.close();
        return out;
    }

    // In DBHelper.java (replace the existing GameResult & getResultsForPlayer)

    public static class GameResult {
        public final String game;
        public final long   duration;
        public final String result;    // ← new field

        public GameResult(String game, long duration, String result) {
            this.game = game;
            this.duration = duration;
            this.result = result;
        }
    }

    public List<GameResult> getResultsForPlayer(String name) {
        List<GameResult> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_RESULTS,
                new String[]{ COL_GAME, COL_DURATION, COL_RESULT },   // ← include COL_RESULT
                COL_NAME + " = ?",
                new String[]{ name },
                null, null,
                "ROWID ASC"
        );
        while (c.moveToNext()) {
            String game   = c.getString(c.getColumnIndexOrThrow(COL_GAME));
            long   dur    = c.getLong  (c.getColumnIndexOrThrow(COL_DURATION));
            String result = c.getString(c.getColumnIndexOrThrow(COL_RESULT));  // ← pull the result
            out.add(new GameResult(game, dur, result));
        }
        c.close();
        db.close();
        return out;
    }

    public void upsertIqScore(String name, int age, String gender, int iq) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("age", age);
        cv.put("gender", gender);
        cv.put("iq", iq);
        db.insertWithOnConflict("iq_scores", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<PlayerInfo> getAllRankedPlayers() {
        List<PlayerInfo> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name, age, gender, iq FROM iq_scores ORDER BY iq DESC", null);
        while (c.moveToNext()) {
            String n = c.getString(0);
            int a = c.getInt(1);
            String g = c.getString(2);
            int iq = c.getInt(3);
            out.add(new PlayerInfo(n, a, g, iq));
        }
        c.close();
        db.close();
        return out;
    }

    // Extend PlayerInfo to include IQ
    public static class PlayerInfo {
        public final String name;
        public final int age;
        public final String gender;
        public final int iq;

        public PlayerInfo(String name, int age, String gender, int iq) {
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.iq = iq;
        }

        public PlayerInfo(String name, int age, String gender) {
            this(name, age, gender, -1);
        }
    }


}
