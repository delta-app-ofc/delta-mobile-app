package com.aula.delta_mobile_app.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.aula.delta_mobile_app.model.MonthlyGoal;
import java.time.YearMonth;
import java.util.Map;

public final class SQLiteGoalStore extends SQLiteOpenHelper implements MonthlyGoalStorage.Store {
    public static final String DATABASE = "delta_monthly_goals.db";
    private static SQLiteGoalStore instance;
    private final Context context;

    public static synchronized SQLiteGoalStore getInstance(Context context) {
        if (instance == null) instance = new SQLiteGoalStore(context.getApplicationContext());
        return instance;
    }

    public SQLiteGoalStore(Context context) {
        super(context, DATABASE, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE monthly_goals (user_id INTEGER NOT NULL CHECK(user_id > 0), property_id INTEGER NOT NULL CHECK(property_id > 0), month TEXT NOT NULL, liters TEXT NOT NULL, PRIMARY KEY(user_id, property_id, month))");
        for (Map.Entry<String, ?> entry : context.getSharedPreferences(SharedPreferencesGoalStore.PREFERENCES, Context.MODE_PRIVATE).getAll().entrySet()) {
            if (!(entry.getValue() instanceof String)) continue;
            ContentValues values;
            try { values = scope(entry.getKey()); }
            catch (IllegalArgumentException | java.time.DateTimeException invalid) { continue; }
            values.put("liters", (String) entry.getValue());
            db.insertOrThrow("monthly_goals", null, values);
        }
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Versão do banco de metas não suportada."); }

    @Override
    public String read(String key) {
        ContentValues values = scope(key);
        try (Cursor cursor = getReadableDatabase().query("monthly_goals", new String[] { "liters" }, "user_id = ? AND property_id = ? AND month = ?", arguments(values), null, null, null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    @Override
    public boolean write(String key, String value) {
        ContentValues values = scope(key);
        values.put("liters", MonthlyGoal.parse(value).toPlainString());
        return getWritableDatabase().insertWithOnConflict("monthly_goals", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    private ContentValues scope(String key) {
        String[] parts = key.split(":", -1);
        if (parts.length != 4 || !"goal".equals(parts[0])) throw new IllegalArgumentException();
        int user = Integer.parseInt(parts[1]), property = Integer.parseInt(parts[2]);
        YearMonth month = YearMonth.parse(parts[3]);
        if (!MonthlyGoal.key(user, property, month).equals(key)) throw new IllegalArgumentException();
        ContentValues values = new ContentValues();
        values.put("user_id", user);
        values.put("property_id", property);
        values.put("month", month.toString());
        return values;
    }

    private String[] arguments(ContentValues values) {
        return new String[] { values.getAsString("user_id"), values.getAsString("property_id"), values.getAsString("month") };
    }
}
