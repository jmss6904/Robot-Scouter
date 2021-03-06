package com.supercilex.robotscouter.ui.scout.template;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.model.metrics.SpinnerMetric;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseAdapterHelper;

import java.util.Collections;

public class ScoutTemplateSheet extends BottomSheetDialogFragment
        implements View.OnClickListener, DialogInterface.OnShowListener, RecyclerView.OnItemTouchListener {
    private static final String TAG = "ScoutTemplateSheet";

    private View mRootView;
    private View mResetAllButton;
    private View mResetTeamButton;
    private FloatingActionMenu mFam;

    private RecyclerView mRecyclerView;
    private ScoutTemplateAdapter mAdapter;
    private LinearLayoutManager mManager;
    private ScoutTemplateItemTouchCallback<ScoutMetric, ScoutViewHolderBase> mItemTouchCallback;
    private boolean mHasAddedItem;

    private String mTemplateKey;

    public static void show(FragmentManager manager, TeamHelper teamHelper) {
        ScoutTemplateSheet sheet = new ScoutTemplateSheet();
        sheet.setArguments(teamHelper.toBundle());
        sheet.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new BottomSheetDialog(getContext(), getTheme()) {
            @Override
            public void onBackPressed() {
                if (mFam.isOpened()) {
                    mFam.close(true);
                } else {
                    super.onBackPressed();
                }
            }
        };
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        BottomSheetDialog d = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = (FrameLayout) d.findViewById(android.support.design.R.id.design_bottom_sheet);
        int parentHeight = getActivity().findViewById(android.R.id.content).getHeight();
        BottomSheetBehavior.from(bottomSheet).setPeekHeight(parentHeight);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_scout_template, container, false);
        mResetAllButton = mRootView.findViewById(R.id.reset_template_all);
        mResetTeamButton = mRootView.findViewById(R.id.reset_template_team);

        getTemplateKey();
        setupRecyclerView(savedInstanceState);
        initFabMenu();
        mResetAllButton.setOnClickListener(this);
        mResetTeamButton.setOnClickListener(this);

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        FirebaseAdapterHelper.saveRecyclerViewState(outState, mAdapter, mManager);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.cleanup();
        mRecyclerView.clearFocus(); // Needed to ensure template is saved if user taps outside sheet
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    private void getTemplateKey() {
        final TeamHelper teamHelper = TeamHelper.get(getArguments());
        mTemplateKey = teamHelper.getTeam().getTemplateKey();
        if (TextUtils.isEmpty(mTemplateKey)) {
            DatabaseReference newTemplateRef = Constants.FIREBASE_SCOUT_TEMPLATES.push();
            mTemplateKey = newTemplateRef.getKey();
            final Context appContext = getContext().getApplicationContext();
            new FirebaseCopier(Constants.FIREBASE_DEFAULT_TEMPLATE, newTemplateRef) {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    super.onDataChange(snapshot);
                    teamHelper.updateTemplateKey(mTemplateKey, appContext);
                    showResetButtons();
                }
            }.performTransformation();
        } else {
            showResetButtons();
            String storedTemplateKey = getContext().getSharedPreferences(Constants.SCOUT_TEMPLATE,
                                                                         Context.MODE_PRIVATE)
                    .getString(Constants.SCOUT_TEMPLATE, null);
            if (TextUtils.isEmpty(storedTemplateKey)) {
                getContext().getSharedPreferences(Constants.SCOUT_TEMPLATE, Context.MODE_PRIVATE)
                        .edit()
                        .putString(Constants.SCOUT_TEMPLATE, mTemplateKey)
                        .apply();
            }
        }
    }

    private void showResetButtons() {
        mResetAllButton.setVisibility(View.VISIBLE);
        mResetTeamButton.setVisibility(View.VISIBLE);
    }

    private void setupRecyclerView(Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.list);
        mManager = new LinearLayoutManager(getContext());

        mRecyclerView.setLayoutManager(mManager);
        mItemTouchCallback = new ScoutTemplateItemTouchCallback<>(mRootView);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mItemTouchCallback);
        mItemTouchCallback.setItemTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // User scrolled down -> hide the FAB
                    mFam.hideMenuButton(true);
                } else if (dy < 0) {
                    mFam.showMenuButton(true);
                } else if (mHasAddedItem
                        && (mManager.findFirstCompletelyVisibleItemPosition() != 0
                        || mManager.findLastCompletelyVisibleItemPosition() != mAdapter.getItemCount() - 1)) {
                    mFam.hideMenuButton(true);
                }

                mHasAddedItem = false;
            }
        });

        mAdapter = new ScoutTemplateAdapter(
                Constants.FIREBASE_SCOUT_TEMPLATES.child(mTemplateKey),
                getChildFragmentManager(),
                (SimpleItemAnimator) mRecyclerView.getItemAnimator(),
                mItemTouchCallback);
        mRecyclerView.setAdapter(mAdapter);
        mItemTouchCallback.setAdapter(mAdapter);
        FirebaseAdapterHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);
    }

    private void initFabMenu() {
        mFam = (FloatingActionMenu) mRootView.findViewById(R.id.fab_menu);

        mRootView.findViewById(R.id.add_checkbox).setOnClickListener(this);
        mRootView.findViewById(R.id.add_counter).setOnClickListener(this);
        mRootView.findViewById(R.id.add_spinner).setOnClickListener(this);
        mRootView.findViewById(R.id.add_note).setOnClickListener(this);
        mRootView.findViewById(R.id.add_stopwatch).setOnClickListener(this);
        mRootView.findViewById(R.id.add_header).setOnClickListener(this);

        // This lets us close the fam when the recyclerview it touched
        mRecyclerView.addOnItemTouchListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        mFam.close(true);
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.reset_template_all || id == R.id.reset_template_team) {
            mRecyclerView.clearFocus();
            ResetTemplateDialog.show(getChildFragmentManager(),
                                     TeamHelper.get(getArguments()),
                                     id == R.id.reset_template_all);
            return;
        }

        int priority = FirebaseAdapterHelper.getHighestIntPriority(mAdapter.getSnapshots()) + 1;
        DatabaseReference metricRef = Constants.FIREBASE_SCOUT_TEMPLATES.child(mTemplateKey).push();
        switch (id) {
            case R.id.add_checkbox:
                metricRef.setValue(new ScoutMetric<>("", false, MetricType.CHECKBOX), priority);
                break;
            case R.id.add_counter:
                metricRef.setValue(new ScoutMetric<>("", 0, MetricType.COUNTER), priority);
                break;
            case R.id.add_spinner:
                metricRef.setValue(
                        new SpinnerMetric("", Collections.singletonMap("0", "item 1")),
                        priority);
                break;
            case R.id.add_note:
                metricRef.setValue(new ScoutMetric<>("", "", MetricType.NOTE), priority);
                break;
            case R.id.add_stopwatch:
                metricRef.setValue(
                        new ScoutMetric<>("", Collections.<Long>emptyList(), MetricType.STOPWATCH),
                        priority);
                break;
            case R.id.add_header:
                metricRef.setValue(new ScoutMetric<Void>("", null, MetricType.HEADER), priority);
                break;
        }

        mItemTouchCallback.addItemToScrollQueue(mAdapter.getItemCount());
        mFam.close(true);
        mHasAddedItem = true;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        // We only care about onInterceptTouchEvent
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // We only care about onInterceptTouchEvent
    }
}
