package au.edu.utas.todoapp;

import java.text.DateFormat;
import java.util.Calendar;

class Task extends TodoDisplayableItem{
	static final String TAG = "TaskClass";

	private int mId;
	private String mTitle;
	private Goal mGoal;
	private int mUrgency;
	private int mImportance;
	private String mNotes;
	private Calendar mDueDate;
	private Calendar mDueTime;
	private Calendar mDailyPlanner;
	private Calendar mMarkedCompletedDate;
	private Calendar mArchivedDate;

	Task(int i) {
		// If task is new (not in DB yet)
		if (i==-1) {
			mDueDate =  null;
			mDailyPlanner = null;
			mMarkedCompletedDate = null;
			mArchivedDate = null;
		}
		mId = i;
	}

	@Override
	public int getId() {return mId;}

	@Override
	public String getTitle() {return mTitle;}
	void setTitle(String t) {
		this.mTitle = t;
	}

	Goal getGoal() {return mGoal;}
	void setGoal(Goal g) {
		this.mGoal = g;
	}

	int getUrgency() {return mUrgency;}
	void setTaskUrgency(int u) {
		this.mUrgency = u;
	}

	int getImportance() {return mImportance;}
	void setImportance(int i) {
		this.mImportance = i;
	}

	String getNotes() {return mNotes;}
	void setNotes(String n) {
		this.mNotes = n;
	}

	Calendar getDueDate() {return mDueDate;}
	void setDueDate(Calendar d) {
		this.mDueDate = d;
	}

	String getDueDateString() {
		return getDueDateString(getDueDate());
	}

	static String getDueDateString(Calendar c) {
		if (c == null)
			return "";
		else {
			if (c.getTimeInMillis() == 0) {
				return "";
			} else {
				return DateFormat.getDateInstance(DateFormat.FULL).format(c.getTime());
			}
		}
	}

	Calendar getDueTime() {return mDueTime;}
	void setDueTime(Calendar d) {
		this.mDueTime = d;
	}

	String getDueTimeString() {
		return getDueTimeString(getDueTime());
	}

	static String getDueTimeString(Calendar c) {
		if (c == null)
			return "";
		else {
			if (c.getTimeInMillis() == 0) {
				return "";
			} else {
				return DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());
			}
		}
	}

	Calendar getDailyPlanner() {return mDailyPlanner;}
	void setDailyPlanner(Calendar d) {
		this.mDailyPlanner = d;
	}
	Boolean isOnDailyPlanner() {return (mDailyPlanner != null);}

	Calendar getMarkedCompletedDate() {return mMarkedCompletedDate;}
	void setMarkedCompletedDate(Calendar d) {
		this.mMarkedCompletedDate = d;
	}
	Boolean isMarkedCompleted() {return (mMarkedCompletedDate != null);}

	Calendar getArchivedDate() {return mArchivedDate;}
	void setArchivedDate(Calendar d) {
		this.mArchivedDate = d;
	}
	Boolean isArchived() {return (mArchivedDate != null);}

	@Override
	public String getType() {return TAG;}
}
