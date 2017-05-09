package au.edu.utas.todoapp;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by nfs on 2/05/2017.
 */

class TodoDB {

	private static final String TAG = "TodoDB";

	private static final String DATABASE_NAME 		= "todo";
	private static final int 	DATABASE_VERSION 	= 1;
	private static final String TASK_TABLE 		= "tasks";
	private static final String GOAL_TABLE 		= "goals";
	private static final String TMQ_TABLE 		= "tmq";

	/*
	 * Some constant definitions that will be used in the application to look
	 * up data from the field names used in the database.
	 */
	static final String KEY_TASK_ROWID = "_id";
	static final String KEY_TASK_GOALID = "goal_id";
	static final String KEY_TASK_TITLE = "title";
	static final String KEY_TASK_DUEDATE = "due_date";
	static final String KEY_TASK_IMPORTANCE = "importance";
	static final String KEY_TASK_URGENCY = "urgency";
	static final String KEY_TASK_NOTES = "notes";
	static final String KEY_TASK_DAILYPLANNER_DATE = "dailyplanner_date";

	static final String KEY_GOAL_ROWID = "_id";
	static final String KEY_GOAL_TITLE = "title";
	static final String KEY_GOAL_COL = "colour";
	/*
	 * Database creation (SQL statement).
	 */

	private static final String TASK_TABLE_CREATE = "create table "
			+ TASK_TABLE
			+ " (" + KEY_TASK_ROWID + " integer primary key autoincrement, "
			+ KEY_TASK_GOALID + " integer not null, "
			+ KEY_TASK_TITLE + " string not null, "
			+ KEY_TASK_DUEDATE + " long not null, "
			+ KEY_TASK_IMPORTANCE + " tinyint not null, "
			+ KEY_TASK_URGENCY + " tinyint not null, "
			+ KEY_TASK_NOTES + " TEXT not null, "
			+ KEY_TASK_DAILYPLANNER_DATE + " int," +
			"FOREIGN KEY("+ KEY_TASK_GOALID +") REFERENCES "+GOAL_TABLE+"("+ KEY_GOAL_ROWID +"));";

	private static final String GOAL_TABLE_CREATE = "create table "
			+ GOAL_TABLE
			+ " (" + KEY_GOAL_ROWID + " integer primary key autoincrement, "
			+ KEY_GOAL_TITLE + " string not null, "
			+ KEY_GOAL_COL + " integer not null);";

	private SQLiteDatabase mDb;
	private TodoDBHelper mDbHelper;
	private final Context mContext;

	TodoDB(Context context) {
		mContext = context;
		mDbHelper = new TodoDBHelper(context);
		mDb = mDbHelper.getWritableDatabase();
	}

	void close() {
		//Log.d(TAG, "close");
		if (mDb != null) {
			mDb.close();
			mDb = null;	// This is important as garbage collection may
			// not have occured by the time the Activities
			// need to know whether they should recreated the
			// DB.
		}
	} // close

	Task getTask(int id) {
		Log.d("mDb", "getTask: "+id);
		Cursor c = mDb.query(TASK_TABLE, null, KEY_TASK_ROWID + "=" + id,
				null, null, null, null);
		Task t = null;

		if (c != null) {
			if (c.moveToNext()) {
				t = cursorToTask(c);
			}
			c.close(); //Cursor management.
			c = null;
		}
		return t;
	}

	ArrayList<Task> getAllTasks() {
		ArrayList<Task> taskList = new ArrayList<Task>();
		Cursor c = mDb.query(TASK_TABLE, null, null, null, null, null, null);

		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				Task t = cursorToTask(c);
				taskList.add(t);

				c.moveToNext();
			}
			c.close(); //Cursor management.
			c = null;
		}

		return taskList;
	}

	Goal getGoal(int id) {
		Cursor c = mDb.query(GOAL_TABLE, null, KEY_GOAL_ROWID + "=" + id,
				null, null, null, null);

		Goal g = null;
		if (c != null) {
			if (c.moveToNext()) {
				g = cursorToGoal(c);
			}
			c.close(); //Cursor management.
			c = null;
		}
		return g;
	} // getProperty

	ArrayList<Goal> getAllGoals() {
		ArrayList<Goal> goalList = new ArrayList<Goal>();
		Cursor c = mDb.query(GOAL_TABLE, null, null, null, null, null, null);

		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				Goal g = cursorToGoal(c);
				goalList.add(g);
				c.moveToNext();
			}
			c.close(); //Cursor management.
			c = null;
		}

		return goalList;
	}

	void addTask(Task t) {
		ContentValues taskValues = new ContentValues();

		// Build our property values
		taskValues.put(KEY_TASK_TITLE, t.getTitle());
		taskValues.put(KEY_TASK_GOALID, t.getGoal().getId());
		taskValues.put(KEY_TASK_DUEDATE, t.getDueDate().getTime());
		taskValues.put(KEY_TASK_IMPORTANCE, t.getImportance());
		taskValues.put(KEY_TASK_URGENCY, t.getUrgency());
		taskValues.put(KEY_TASK_NOTES, t.getNotes());
		taskValues.put(KEY_TASK_DAILYPLANNER_DATE, t.getDailyPlanner().getTime());

		// Insert them into the database
		mDb.insert(TASK_TABLE, null, taskValues);
	}

	void addGoal(Goal g) {
		ContentValues goalValues = new ContentValues();

		// Build our property values
		goalValues.put(KEY_GOAL_COL, g.getColour());
		goalValues.put(KEY_GOAL_TITLE, g.getTitle());

		// Insert them into the database
		mDb.insert(GOAL_TABLE, null, goalValues);
	}

	void editTask(Task t) {
		ContentValues taskValues = new ContentValues();

		// Build our property values
		taskValues.put(KEY_TASK_TITLE, t.getTitle());
		taskValues.put(KEY_TASK_GOALID, t.getGoal().getId());
		taskValues.put(KEY_TASK_DUEDATE, t.getDueDate().getTime());
		taskValues.put(KEY_TASK_IMPORTANCE, t.getImportance());
		taskValues.put(KEY_TASK_URGENCY, t.getUrgency());
		taskValues.put(KEY_TASK_NOTES, t.getNotes());
		taskValues.put(KEY_TASK_DAILYPLANNER_DATE, t.getDailyPlanner().getTime());

		// Insert them into the database
		try {
			mDb.update(TASK_TABLE, taskValues, KEY_TASK_ROWID + "=" + t.getId(), null);
		}catch (SQLiteException e) {
			Log.e(TAG, "SQLiteException: "+e.getMessage());
		}
	}

	void removeTask(int id) {
		try {
			mDb.delete(TASK_TABLE, KEY_TASK_ROWID + "=" + id, null);
		} catch (SQLiteException e) {
			Log.e(TAG, "SQLiteException: "+e.getMessage());
		}
	}

	//https://stackoverflow.com/questions/7363112/best-way-to-work-with-dates-in-android-sqlite
	Task cursorToTask(Cursor c) {
		Task t;
		t = new Task(c.getInt(c.getColumnIndex(KEY_TASK_ROWID)));

		t.setTitle(c.getString(c.getColumnIndex(KEY_TASK_TITLE)));
		t.setNotes(c.getString(c.getColumnIndex(KEY_TASK_NOTES)));
		t.setImportance(c.getInt(c.getColumnIndex(KEY_TASK_IMPORTANCE)));
		t.setTaskUrgency(c.getInt(c.getColumnIndex(KEY_TASK_URGENCY)));
		t.setDueDate(new Date(c.getLong(c.getColumnIndex(KEY_TASK_DUEDATE))));
		t.setDailyPlanner(new Date(c.getLong(c.getColumnIndex(KEY_TASK_DAILYPLANNER_DATE))));
		t.setGoal(getGoal(c.getInt(c.getColumnIndex(KEY_TASK_GOALID))));

		return t;
	}

	//https://stackoverflow.com/questions/7363112/best-way-to-work-with-dates-in-android-sqlite
	Goal cursorToGoal(Cursor c) {
		Goal g;
		g = new Goal(c.getInt(c.getColumnIndex(KEY_GOAL_ROWID)));
		g.setTitle(c.getString(c.getColumnIndex(KEY_GOAL_TITLE)));
		g.setColour(c.getInt(c.getColumnIndex(KEY_GOAL_COL)));

		return g;
	}

	private static class TodoDBHelper extends SQLiteOpenHelper {
		// If you change the database schema, you must increment the database version.
		private static TodoDBHelper mInstance = null;

		private TodoDBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public static TodoDBHelper getInstance(Context context) {
			if (mInstance == null) {
				mInstance = new TodoDBHelper(context);
			}
			return mInstance;
		}

		public void onCreate(SQLiteDatabase db) {
			Log.d("TODO", GOAL_TABLE_CREATE);
			db.execSQL(GOAL_TABLE_CREATE);
			db.execSQL(TASK_TABLE_CREATE);
			Log.d("TODO", "CREATED DB");
		}
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// This database is only a cache for online data, so its upgrade policy is
			// to simply to discard the data and start over
			reset(db);
		}
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onUpgrade(db, oldVersion, newVersion);
		}

		void reset(SQLiteDatabase db) {
			//
			db.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + GOAL_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TMQ_TABLE);
			onCreate(db);
		} // reset
	}
}

