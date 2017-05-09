package au.edu.utas.todoapp;

import java.util.Date;

class Task {
	static final String TAG = "TaskClass";

	private int mId;
	private String mTitle;
	private Goal mGoal;
	private Date mDueDate;
	private int mUrgency;
	private int mImportance;
	private String mNotes;
	private Date mDailyPlanner;

	Task(int i) {
		mId = i;
	}

	public int getId() {return mId;}

	String getTitle() {return mTitle;}
	void setTitle(String t) {
		this.mTitle = t;
	}
	Goal getGoal() {return mGoal;}
	void setGoal(Goal g) {
		this.mGoal = g;
	}

	Date getDueDate() {return mDueDate;}
	void setDueDate(Date d) {
		this.mDueDate = d;
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

	Date getDailyPlanner() {return mDailyPlanner;}
	void setDailyPlanner(Date d) {
		this.mDailyPlanner = d;
	}


}
