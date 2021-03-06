package com.supercilex.robotscouter.ui.teamlist;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

import de.hdodenhof.circleimageview.CircleImageView;

public class TeamViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {
    private Team mTeam;
    private TeamMenuManager mMenuManager;
    private boolean mIsItemSelected;
    private boolean mCouldItemBeSelected;
    private boolean mIsScouting;

    private CircleImageView mLogo;
    private TextView mNumber;
    private TextView mName;
    private ImageButton mNewScout;

    @Keep
    public TeamViewHolder(View itemView) {
        super(itemView);
        mLogo = (CircleImageView) itemView.findViewById(R.id.logo);
        mNumber = (TextView) itemView.findViewById(R.id.number);
        mName = (TextView) itemView.findViewById(R.id.name);
        mNewScout = (ImageButton) itemView.findViewById(R.id.new_scout);
    }

    public boolean isScouting() {
        return mIsScouting;
    }

    public void bind(Team team,
                     TeamMenuManager menuManager,
                     boolean isItemSelected,
                     boolean couldItemBeSelected,
                     boolean isScouting) {
        mTeam = team;
        mMenuManager = menuManager;
        mIsItemSelected = isItemSelected;
        mCouldItemBeSelected = couldItemBeSelected;
        mIsScouting = isScouting;

        mNewScout.setBackground(getRippleDrawable());
        setTeamNumber();
        setTeamName();
        updateItemStatus();

        mLogo.setOnClickListener(this);
        mNewScout.setOnClickListener(this);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    private void updateItemStatus() {
        itemView.setBackground(getRippleDrawable());
        if (mIsItemSelected) {
            Glide.with(itemView.getContext())
                    .load("")
                    .placeholder(ContextCompat.getDrawable(itemView.getContext(),
                                                           R.drawable.ic_check_circle_grey_144dp))
                    .into(mLogo);
            itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), R.color.selected_item));
        } else {
            setTeamLogo();
        }
        mNewScout.setVisibility(mCouldItemBeSelected ? View.GONE : View.VISIBLE);

        if (!mIsItemSelected && !mCouldItemBeSelected && mIsScouting) {
            itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), R.color.grey));
        }
    }

    private Drawable getRippleDrawable() {
        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        TypedArray typedArray = itemView.getContext().obtainStyledAttributes(attrs);
        try {
            return typedArray.getDrawable(0);
        } finally {
            typedArray.recycle();
        }
    }

    private void setTeamNumber() {
        mNumber.setText(mTeam.getNumber());
    }

    private void setTeamName() {
        if (TextUtils.isEmpty(mTeam.getName())) {
            mName.setText(itemView.getContext().getString(R.string.unknown_team));
        } else {
            mName.setText(mTeam.getName());
        }
    }

    private void setTeamLogo() {
        Glide.with(itemView.getContext())
                .load(mTeam.getMedia())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(R.drawable.ic_android_black_144dp)
                .into(mLogo);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.logo || mIsItemSelected || mCouldItemBeSelected) {
            onTeamContextMenuRequested();
        } else {
            ((TeamSelectionListener) itemView.getContext())
                    .onTeamSelected(mTeam, v.getId() == R.id.new_scout);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        onTeamContextMenuRequested();
        return true;
    }

    private void onTeamContextMenuRequested() {
        mIsItemSelected = !mIsItemSelected;
        updateItemStatus();
        mMenuManager.onTeamContextMenuRequested(mTeam.getHelper());
    }

    @Override
    public String toString() {
        return mTeam.toString();
    }
}
