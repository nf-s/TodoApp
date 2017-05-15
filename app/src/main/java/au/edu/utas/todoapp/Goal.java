package au.edu.utas.todoapp;

import java.util.Calendar;

class Goal extends TodoDisplayableItem {
	static final String TAG = "GoalClass";

	private int mId;
	private String mTitle;
	private String mNotes;
	private int mColour;
	private Calendar mArchivedDate;

	public Goal(int i) {
		this(i, "", 0);
	}

	public Goal(int i, String t, int c) {
		if (i==-1) {
			mArchivedDate = null;
		}
		mId = i;
		mTitle = t;
		mColour = c;
	}

	@Override
	public int getId() {return mId;}

	int getColour() {return mColour;}
	void setColour(int c) {
		this.mColour = c;
	}

	int getColourOpaque() {
		return getColourAlpha(0xFF);
	}
	int getColourAlpha(int alpha) {
		return (alpha<<24)+getColour();
	}

	public String getNotes() {return mNotes;}
	void setNotes(String n) {
		this.mNotes = n;
	}

	@Override
	public String getTitle() {return mTitle;}
	void setTitle(String t) {
		this.mTitle = t;
	}

	Calendar getArchivedDate() {return mArchivedDate;}
	void setArchivedDate(Calendar d) {
		this.mArchivedDate = d;
	}

	@Override
	public String getType() {return TAG;}


}
