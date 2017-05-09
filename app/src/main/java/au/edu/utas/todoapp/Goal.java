package au.edu.utas.todoapp;

class Goal {
	static final String TAG = "GoalClass";

	private int mId;
	private String mTitle;
	private int mColour;

	public Goal(int i, String t, int c) {
		mId = i;
		mTitle = t;
		mColour = c;
	}

	Goal(int i) {
		mId = i;
	}

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


	String getTitle() {return mTitle;}
	void setTitle(String t) {
		this.mTitle = t;
	}
}
