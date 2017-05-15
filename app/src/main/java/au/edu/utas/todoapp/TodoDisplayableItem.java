package au.edu.utas.todoapp;

/**
 * Created by nfs on 10/05/2017.
 */

public abstract class TodoDisplayableItem {
	abstract public int getId();
	abstract public String getTitle();
	abstract public String getType();

	void removeItem() {

	}
}
