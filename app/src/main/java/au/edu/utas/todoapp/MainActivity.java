package au.edu.utas.todoapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.LinearLayout;

// TODO FIX on save state stuff

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TodoFragmentListener {
	static final String TAG = "MainActivity";

	static final String EXTRA_SELECTED_ITEM_ID ="au.edu.utas.todoapp.selected_task";
	static final String EXTRA_SELECTED_ITEM_TYPE ="au.edu.utas.todoapp.interact_task_or_goal";
	static final String EXTRA_INVOKED_BY="au.edu.utas.todoapp.invoked_by";
	static final String EXTRA_PARENT_ITEM_ID = "au.edu.utas.todoapp.parent_id";

	private Context mContext;
	private TaskListFragment mTaskListFragment;

	private TodoDB mDb = null;

	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private NavigationView mNavigationView;
	private Toolbar mToolbar;
	private FloatingActionButton mNewTaskFab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();

		mDb = new TodoDB(mContext);

		initToolbar();
		initNewTaskFAB();
		initMainFragment();
		initDrawer();
		// Set current checked item - All Tasks
		mNavigationView.setCheckedItem(R.id.drawer_menu_all_tasks);
	}

	@Override
	public void onResume() {
		super.onResume();

	}

	/**
	 *  Called when the activity is no longer in use.
	 *  Note order: onStop() -> onDestroyView() -> onDestroy() -> onDetach()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mDb.close(); // Close the database to avoid memory leaks.
	}

	// ------------------------------------------------------------------------
	// NAVIGATION METHODS (HANDLES SELECTING TOOLBAR & NAVIGATION DRAWER MENU ITEMS)
	// ------------------------------------------------------------------------

	/**
	 *  Creates options menu in toolbar for item (Task or Goal)
	 *  Menu item selection is handled with onOptionsItemSelected()
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	/**
	 *  Handles all toolbar item selection
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	/**
	 *  Handles navigation item selection except goal items
	 */
	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {

		mDrawerLayout.closeDrawer(GravityCompat.START);

		// Uncheck all menu items
		int size = mNavigationView.getMenu().size();
		for (int i = 0; i < size; i++) {
			mNavigationView.getMenu().getItem(i).setChecked(false);
		}

		// Check selected menu item
		item.setChecked(true);
		switch (item.getItemId()) {
			case R.id.drawer_menu_today: {
				return true;
			}
			case R.id.drawer_menu_tmq: {
				return true;
			}
			case R.id.drawer_menu_all_tasks: {
				updateTaskListFragment(TaskListFragment.newInstance());
				setNewTaskFAB(-1);
				resetPrimaryCol();
				return true;
			}
			case R.id.drawer_menu_settings: {
				return true;
			}
			case R.id.drawer_menu_help: {
				return true;
			}
			case R.id.drawer_menu_new_goal: {
				newTaskEditDialog(getSupportFragmentManager(), TAG, Goal.TAG);
				return true;
			}
			default:
				return false;
		}
	}

	/**
	 *  Handle NavigationDrawer goal item selection
	 *  filters tasks in TaskListFragment
	 */
	private void handleGoalNavigationItemSelected(final MenuItem goalMenuItem, final int goalId) {
		goalMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				updateTaskListFragment(
						TaskListFragment.newInstance(TodoDB.KEY_TASK_GOALID, Integer.toString(goalId), "", "", false));
				setNewTaskFAB(goalId);
				return false;
			}
		});
	}

	/**
	 *  Handle NavigationDrawer "info" button clicks - i.e. circle with 'i' button
	 *  eg. used to view Goals
	 */
	private void handleGoalInfoNavigationItemSelected(final ImageButton imgButton, final String itemType, final int itemId) {
		imgButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (itemType.equals(Goal.TAG)) {
					newTaskViewDialog(getSupportFragmentManager(), TAG, Goal.TAG, itemId);
				}
				mDrawerLayout.closeDrawer(GravityCompat.START);
			}
		});
	}

	// ------------------------------------------------------------------------
	// UI INITIALISATION METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Initialises Toolbar
	 */
	private void initToolbar() {
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		// This is needed because of the transparent status bar - which increases the height of the application
		mToolbar.setPadding(0, getStatusBarHeight(getResources()),0,0);
		setSupportActionBar(mToolbar);
	}

	/**
	 *  Initialises Floating Action Button for adding a new task
	 */
	private void initNewTaskFAB() {
		mNewTaskFab = (FloatingActionButton) findViewById(R.id.fab);
		setNewTaskFAB(-1);
	}

	/**
	 *  Sets New task FAB if a goal has been selected
	 *  This sets the goal for the new task
	 *  and changes colour of FAB to the goal colour
	 */
	private void setNewTaskFAB(final int parentId) {
		mNewTaskFab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_add_white_48dp, null));
		mNewTaskFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				newTaskEditDialog(getSupportFragmentManager(), TAG, Task.TAG, -1, parentId);
			}
		});
	}

	/**
	 *  Initialises Main Fragment (TaskListFragment)
	 */
	private void initMainFragment() {
		mTaskListFragment = TaskListFragment.newInstance();
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mTaskListFragment).commit();
	}

	/**
	 *  Initialises Navigation Drawer
	 */
	private void initDrawer() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open,  R.string.drawer_close);

		// Tie DrawerLayout events to the ActionBarToggle
		mDrawerToggle.syncState();
		mDrawerLayout.addDrawerListener(mDrawerToggle);

		mNavigationView = (NavigationView) findViewById(R.id.nav_view);
		mNavigationView.setNavigationItemSelectedListener(this);

		// Turns off default icon tint
		mNavigationView.setItemIconTintList(null);

		// Add header to Navdrawer
		// NOTE: can also be done in XML with 	app:headerLayout="@layout/drawer_header" in NavigationView
		View headerLayout = mNavigationView.inflateHeaderView(R.layout.drawer_header);
		headerLayout.findViewById(R.id.drawer_header_layout).setPadding(0,getStatusBarHeight(getResources()),0,0);

		createGoalDrawerMenuItems();

		// If the drawer is open and the user clicks the background to the right of the drawer - close the drawer
		findViewById(R.id.drawer_background).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
					mDrawerLayout.closeDrawer(GravityCompat.START);
			}
		});
	}

	/**
	 *  Creates navigation drawer items for every goal
	 *  Also creates event handles for each item
	 */
	private void createGoalDrawerMenuItems() {
		// Create goal drawer menu items
		Menu drawerMenu = mNavigationView.getMenu();

		int currentGoalMenuOrderId = 1;
		for (Goal g: mDb.getAllGoals()) {
			// Creates new drawer menu item with new view id and an order id of 1000+currentMenuId
			MenuItem goalMenuItem = drawerMenu.add(R.id.drawer_menu_group_goals, View.generateViewId(),
					1000+currentGoalMenuOrderId, g.getTitle());

			goalMenuItem.setCheckable(true).setChecked(false).setActionView(R.layout.drawer_right_more_button);

			// Set icon to coloured circle (with goal colour)
			Drawable icon = DrawableCompat.wrap(ResourcesCompat.getDrawable(getResources(), R.drawable.circle_24dp, null));
			DrawableCompat.setTint(icon, g.getColourOpaque());
			goalMenuItem.setIcon(icon);

			// Get ActionMenuView - this holds the small "info" button to the right of each goal menu item
			LinearLayout drawerItemActionView = (LinearLayout) goalMenuItem.getActionView();
			ImageButton goalInfoButton = (ImageButton)drawerItemActionView.findViewById(R.id.drawer_right_more_button);

			handleGoalInfoNavigationItemSelected(goalInfoButton, Goal.TAG, g.getId());
			handleGoalNavigationItemSelected(goalMenuItem, g.getId());

			// Clones icon drawable - so tint doesn't not re-write all icons
			icon = icon.mutate().getConstantState().newDrawable();

			currentGoalMenuOrderId++;
		}

		Drawable newGoalIcon = DrawableCompat.wrap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_add_black_24dp, null));
		DrawableCompat.setTint(newGoalIcon, ResourcesCompat.getColor(getResources(), R.color.darkGreyTint, null));

		drawerMenu.add(R.id.drawer_menu_group_goals, R.id.drawer_menu_new_goal, 1000+currentGoalMenuOrderId, R.string.drawer_new_goal)
				.setCheckable(true).setChecked(false).setIcon(newGoalIcon);
	}

	// ------------------------------------------------------------------------
	// UI METHODS
	// ------------------------------------------------------------------------

	/**
	 *  Deletes and recreates goal menu items in navigation drawer
	 */
	public void refreshNavDrawer() {
		mNavigationView.getMenu().removeGroup(R.id.drawer_menu_group_goals);
		createGoalDrawerMenuItems();
	}

	/**
	 *  This is needed because of the transparent status bar
	 *  which increases the height of the application by the height of the status bar
	 */
	static int getStatusBarHeight(Resources res) {
		int result = 0;
		int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = res.getDimensionPixelSize(resourceId);
		}
		return result;
	}

	/**
	 *  Creates and displays a new TaskViewDialogFragment
	 */
	static void newTaskViewDialog(FragmentManager fragmentManager, String invokedBy, String itemType, int itemId) {
		TaskViewDialogFragment newFragment = TaskViewDialogFragment.newInstance(invokedBy, itemType, itemId);
		fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.add(android.R.id.content, newFragment, TaskViewDialogFragment.TAG).addToBackStack(null).commit();
	}

	/**
	 *  Creates and displays a new TaskEditDialogFragment
	 */
	static void newTaskEditDialog(FragmentManager fragmentManager, String invokedBy, String itemType) {
		newTaskEditDialog(fragmentManager, invokedBy, itemType, -1);
	}

	static void newTaskEditDialog(FragmentManager fragmentManager, String invokedBy, String itemType, int itemId) {
		newTaskEditDialog(fragmentManager, invokedBy, itemType, itemId, -1);
	}

	static void newTaskEditDialog(FragmentManager fragmentManager, String invokedBy, String itemType, int itemId, int parentid) {
		TaskEditDialogFragment newFragment = TaskEditDialogFragment.newInstance(invokedBy, itemType, itemId, parentid);
		fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.add(android.R.id.content, newFragment, TaskEditDialogFragment.TAG).addToBackStack(null).commit();
	}

	/**
	 *  Replaces taskListFragment
	 */
	public void updateTaskListFragment(TaskListFragment newFragment) {
		getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.replace(R.id.fragment_container, newFragment).commit();
		mTaskListFragment = newFragment;
	}

	public void resetPrimaryCol() {
		updatePrimaryCol(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null)+0xFF000000);
	}

	@Override
	public void gotoAppHome() {
		initNewTaskFAB();
		updateTaskListFragment(TaskListFragment.newInstance());
		resetPrimaryCol();
		mNavigationView.setCheckedItem(R.id.drawer_menu_all_tasks);
	}

	@Override
	public void updatePrimaryCol(int col) {
		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		mToolbar.setBackgroundColor(col);
		mNewTaskFab.setBackgroundTintList(ColorStateList.valueOf(col));
	}

	@Override
	public void taskListChanged(int itemId, String invokedBy) {
		// If TaskListFragment exists -> Update Task list
		updateTaskListFragment();
		updateTaskViewDialog();
	}

	@Override
	public void goalListChanged(int itemId, String invokedBy) {
		refreshNavDrawer();
		updateTaskListFragment();
		updateTaskViewDialog();
		if (invokedBy.equals(TaskEditDialogFragment.TAG))
			updateTaskEditDialog(itemId);
	}

	public void updateTaskListFragment() {
		TaskListFragment taskListFragment = (TaskListFragment)
				getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		if (taskListFragment != null) {
			taskListFragment.refreshTaskListAdapter();
		}
	}

	public void updateTaskViewDialog() {
		TaskViewDialogFragment taskViewFragment = (TaskViewDialogFragment)
				getSupportFragmentManager().findFragmentByTag(TaskViewDialogFragment.TAG);

		if (taskViewFragment != null) {
			taskViewFragment.refresh();
		}
	}

	public void updateTaskEditDialog(int itemId) {
		TaskEditDialogFragment taskEditFragment = (TaskEditDialogFragment)
				getSupportFragmentManager().findFragmentByTag(TaskEditDialogFragment.TAG);

		if (taskEditFragment != null) {
			//taskEditFragment.setTaskGoal(itemId);
		}
	}

}

interface TodoFragmentListener {
	void gotoAppHome();
	void goalListChanged(int itemId, String invokedBy);
	void taskListChanged(int id, String invokedBy);
	void resetPrimaryCol();
	void updatePrimaryCol(int col);

}
