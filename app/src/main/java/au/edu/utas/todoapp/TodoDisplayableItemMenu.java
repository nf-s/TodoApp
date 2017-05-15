package au.edu.utas.todoapp;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by nfs on 13/05/2017.
 */

public class TodoDisplayableItemMenu implements PopupMenu.OnMenuItemClickListener, Toolbar.OnMenuItemClickListener, MenuItem.OnMenuItemClickListener {
	protected Context mContext;
	protected TodoDB mDb;
	protected OnMenuItemClickedListener mListener;

	class TaskOptionsMenu extends TodoDisplayableItemMenu {

		private Task mSelectedTask;

		TaskOptionsMenu(Context context, OnMenuItemClickedListener listener, Task t) {
			mContext = context;
			mDb = new TodoDB(mContext);
			mListener = listener;
			mSelectedTask = t;
		}

		void setMenuItems(Menu menu) {
			if (mSelectedTask.isMarkedCompleted())
				menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_mark_uncompleted, 1, R.string.task_options_menu_mark_uncompleted).setOnMenuItemClickListener(this);
			else
				menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_mark_completed, 1, R.string.task_options_menu_mark_completed).setOnMenuItemClickListener(this);

			if (mSelectedTask.isOnDailyPlanner())
				menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_remove_dailyplan, 2, R.string.task_options_menu_remove_from_daily_planner).setOnMenuItemClickListener(this);
			else
				menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_add_dailyplan, 2, R.string.task_options_menu_add_to_daily_planner).setOnMenuItemClickListener(this);

			menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_edit, 3, R.string.task_options_menu_edit).setOnMenuItemClickListener(this);
			menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_delete, 4, R.string.task_options_menu_delete).setOnMenuItemClickListener(this);
		}

		@Override
		public boolean onMenuItemClick(final MenuItem item) {
			switch (item.getItemId()) {
				case R.id.item_popup_menu_delete:
					// Create delete confirm dialog box
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

					builder.setMessage("Are you sure you want to delete " + mSelectedTask.getTitle() + "?")
							.setTitle("Confirm Delete");

					builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDb.removeItem(mSelectedTask);
							Toast.makeText(mContext, "Task Deleted", Toast.LENGTH_SHORT).show();
							mListener.onTodoItemOptionsMenuItemClick(item, mSelectedTask);
						}
					});

					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});

					AlertDialog dialog = builder.create();
					dialog.show();
					break;
				case R.id.item_popup_menu_edit:
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedTask);
					break;
				case R.id.item_popup_menu_add_dailyplan:
					mSelectedTask.setDailyPlanner(Calendar.getInstance());
					mDb.editTask(mSelectedTask);
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedTask);
					break;
				case R.id.item_popup_menu_remove_dailyplan:
					mSelectedTask.setDailyPlanner(null);
					mDb.editTask(mSelectedTask);
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedTask);
					break;
				case R.id.item_popup_menu_mark_completed:
					mSelectedTask.setMarkedCompletedDate(Calendar.getInstance());
					mDb.editTask(mSelectedTask);
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedTask);
					break;
				case R.id.item_popup_menu_mark_uncompleted:
					mSelectedTask.setMarkedCompletedDate(null);
					mDb.editTask(mSelectedTask);
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedTask);
					break;
				default:
					break;
			}

			return true;
		}

	}

	class GoalOptionsMenu extends TodoDisplayableItemMenu {
		private Goal mSelectedGoal;

		GoalOptionsMenu(Context context, OnMenuItemClickedListener listener, Goal g) {
			mContext = context;
			mDb = new TodoDB(mContext);
			mListener = listener;
			mSelectedGoal = g;
		}

		void setMenuItems(Menu menu) {
			//menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_mark_goal_completed, 1, R.string.goal_options_menu_mark_completed);
			////menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_mark_goal_uncompleted, 1, R.string.goal_options_menu_mark_uncompleted);
			//menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_add_goal_dailyplan, 2, R.string.goal_options_menu_add_to_daily_planner);
			//menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_add_goal_dailyplan, 2, R.string.goal_options_menu_add_to_daily_planner);
			menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_edit, 3, R.string.goal_options_menu_edit).setOnMenuItemClickListener(this);
			menu.add(R.id.menu_item_options_group, R.id.item_popup_menu_delete, 4, R.string.goal_options_menu_delete).setOnMenuItemClickListener(this);
		}

		@Override
		public boolean onMenuItemClick(final MenuItem item) {
			switch (item.getItemId()) {
				case R.id.item_popup_menu_delete:
					// Create delete confirm dialog box
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

					builder.setMessage("Are you sure you want to delete " + mSelectedGoal.getTitle() + "?")
							.setTitle("Confirm Delete");

					builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDb.removeItem(mSelectedGoal);
							Toast.makeText(mContext, "Goal Deleted", Toast.LENGTH_SHORT).show();
							mListener.onTodoItemOptionsMenuItemClick(item, mSelectedGoal);
						}
					});

					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});

					AlertDialog dialog = builder.create();
					dialog.show();
					break;
				case R.id.item_popup_menu_edit:
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedGoal);
					break;
				case R.id.item_popup_menu_add_goal_dailyplan:
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedGoal);
					break;
				case R.id.item_popup_menu_mark_goal_completed:
					mListener.onTodoItemOptionsMenuItemClick(item, mSelectedGoal);
					break;

				default:
					break;
			}
			return true;
		}
	}

	interface OnMenuItemClickedListener {
		void onTodoItemOptionsMenuItemClick(MenuItem item, TodoDisplayableItem todoItem);
	}

	void setMenuItems(Menu menu) {
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		return true;
	}
}

