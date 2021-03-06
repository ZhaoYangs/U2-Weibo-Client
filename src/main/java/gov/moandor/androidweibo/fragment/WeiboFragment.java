package gov.moandor.androidweibo.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayout;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import gov.moandor.androidweibo.R;
import gov.moandor.androidweibo.activity.WeiboActivity;
import gov.moandor.androidweibo.bean.WeiboGeo;
import gov.moandor.androidweibo.bean.WeiboStatus;
import gov.moandor.androidweibo.bean.WeiboUser;
import gov.moandor.androidweibo.concurrency.ImageDownloader;
import gov.moandor.androidweibo.concurrency.MyAsyncTask;
import gov.moandor.androidweibo.concurrency.WeiboDetailPictureReadTask;
import gov.moandor.androidweibo.dao.MapImageDao;
import gov.moandor.androidweibo.util.ActivityUtils;
import gov.moandor.androidweibo.util.GlobalContext;
import gov.moandor.androidweibo.util.Logger;
import gov.moandor.androidweibo.util.OnWeiboTextTouchListener;
import gov.moandor.androidweibo.util.TextUtils;
import gov.moandor.androidweibo.util.TimeUtils;
import gov.moandor.androidweibo.util.Utilities;
import gov.moandor.androidweibo.util.WeiboException;
import gov.moandor.androidweibo.widget.WeiboDetailPicView;

public class WeiboFragment extends Fragment implements UserDialogFragment.OnUserChangedListener {
    private static final String TAG_USER_DIALOG = "user_dialog";

    private WeiboStatus mWeiboStatus;
    private ImageDownloader.ImageType mAvatarType = Utilities.getAvatarType();
    private ImageDownloader.ImageType mPictureType = Utilities.getDetailPictureType();
    private ImageView mAvatar;
    private ImageView mMap;
    private WeiboDetailPicView mPicture;
    private WeiboDetailPicView mRetweetPicture;
    private GridLayout mPictureMulti;
    private GridLayout mRetweetPictureMulti;
    private RelativeLayout mRetweetLayout;
    private LinearLayout mPicLayout;
    private TextView mUserName;
    private TextView mTime;
    private TextView mSource;
    private TextView mText;
    private TextView mRetweetText;
    private TextView mCoordinate;
    private float mFontSize = Utilities.getFontSize();
    private float mSmallFontSize = mFontSize - 3;
    private OnWeiboTextTouchListener mTextOnTouchListener = new OnWeiboTextTouchListener();
    private View.OnClickListener mTextOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = ActivityUtils.weiboActivity(mWeiboStatus.retweetStatus);
            getActivity().startActivity(intent);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weibo, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mAvatar = (ImageView) view.findViewById(R.id.avatar);
        mMap = (ImageView) view.findViewById(R.id.map);
        mUserName = (TextView) view.findViewById(R.id.user_name);
        mTime = (TextView) view.findViewById(R.id.time);
        mSource = (TextView) view.findViewById(R.id.source);
        mText = (TextView) view.findViewById(R.id.text);
        mRetweetText = (TextView) view.findViewById(R.id.retweet_text);
        mCoordinate = (TextView) view.findViewById(R.id.coordinate);
        mPicture = (WeiboDetailPicView) view.findViewById(R.id.pic);
        mRetweetPicture = (WeiboDetailPicView) view.findViewById(R.id.retweet_pic);
        mPictureMulti = (GridLayout) view.findViewById(R.id.pic_multi);
        mRetweetPictureMulti = (GridLayout) view.findViewById(R.id.retweet_pic_multi);
        mRetweetLayout = (RelativeLayout) view.findViewById(R.id.retweet);
        mPicLayout = (LinearLayout) view.findViewById(R.id.pic_layout);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWeiboStatus = ((WeiboActivity) getActivity()).getWeiboStatus();
        initLayout();
        buildLayout();
    }

    @Override
    public void onUserChanged(WeiboUser user) {
        mWeiboStatus.weiboUser = user;
        buildLayout();
    }

    private void initLayout() {
        mUserName.setTextSize(mFontSize);
        mTime.setTextSize(mSmallFontSize);
        mSource.setTextSize(mSmallFontSize);
        mText.setTextSize(mFontSize);
        mRetweetText.setTextSize(mFontSize);
        mUserName.getPaint().setFakeBoldText(true);
        mText.setOnTouchListener(mTextOnTouchListener);
        mRetweetText.setOnTouchListener(mTextOnTouchListener);
        mRetweetText.setOnClickListener(mTextOnClickListener);
    }

    private void buildLayout() {
        WeiboUser user = mWeiboStatus.weiboUser;
        if (user != null) {
            mUserName.setText(user.name);
            mAvatar.setOnClickListener(new OnAvatarClickListener(user));
            mAvatar.setOnLongClickListener(new OnAvatarLongClickListener());
        }
        String time = TimeUtils.getListTime(mWeiboStatus);
        mTime.setText(time);
        if (!TextUtils.isEmpty(mWeiboStatus.source)) {
            mSource.setText(Html.fromHtml(mWeiboStatus.source).toString());
        }
        if (TextUtils.isEmpty(mWeiboStatus.textSpannable)) {
            mWeiboStatus.textSpannable = TextUtils.addWeiboLinks(mWeiboStatus.text);
        }
        mText.setText(mWeiboStatus.textSpannable);
        ImageDownloader.downloadAvatar(mAvatar, mWeiboStatus.weiboUser, false, mAvatarType);
        if (mWeiboStatus.thumbnailPic != null && mWeiboStatus.picCount > 0) {
            mPicLayout.setVisibility(View.VISIBLE);
            if (mWeiboStatus.picCount == 1) {
                mPicture.setVisibility(View.VISIBLE);
                buildPicture(mPicture, mWeiboStatus);
            } else {
                mPictureMulti.setVisibility(View.VISIBLE);
                buildMultiPicture(mPictureMulti, mWeiboStatus);
            }
        } else {
            if (mWeiboStatus.retweetStatus != null) {
                mRetweetLayout.setVisibility(View.VISIBLE);
                if (TextUtils.isEmpty(mWeiboStatus.retweetStatus.textSpannable)) {
                    user = mWeiboStatus.retweetStatus.weiboUser;
                    String text;
                    if (user != null) {
                        text = "@" + user.name + " : " + mWeiboStatus.retweetStatus.text;
                    } else {
                        text = mWeiboStatus.retweetStatus.text;
                    }
                    mWeiboStatus.retweetStatus.textSpannable = TextUtils.addWeiboLinks(text);
                }
                mRetweetText.setText(mWeiboStatus.retweetStatus.textSpannable);
                if (mWeiboStatus.retweetStatus.thumbnailPic != null && mWeiboStatus.retweetStatus.picCount > 0) {
                    if (mWeiboStatus.retweetStatus.picCount == 1) {
                        mRetweetPicture.setVisibility(View.VISIBLE);
                        buildPicture(mRetweetPicture, mWeiboStatus.retweetStatus);
                    } else {
                        mRetweetPictureMulti.setVisibility(View.VISIBLE);
                        buildMultiPicture(mRetweetPictureMulti, mWeiboStatus.retweetStatus);
                    }
                }
            }
        }
        if (mWeiboStatus.weiboGeo != null && GlobalContext.isInWifi()) {
            mPicLayout.setVisibility(View.VISIBLE);
            buildCoordinate();
        }
    }

    private void buildPicture(WeiboDetailPicView view, WeiboStatus status) {
        String url;
        switch (mPictureType) {
            case PICTURE_MEDIUM:
                url = status.bmiddlePic[0];
                break;
            case PICTURE_LARGE:
                url = status.originalPic[0];
                break;
            default:
                throw new IllegalStateException("Illegal image type");
        }
        new WeiboDetailPictureReadTask(url, mPictureType, view).execute();
        view.setOnClickListener(new OnPictureClickListener(0, status));
    }

    private void buildMultiPicture(GridLayout grid, WeiboStatus status) {
        for (int i = 0; i < status.picCount; i++) {
            ImageView view = (ImageView) grid.getChildAt(i);
            view.setVisibility(View.VISIBLE);
            ImageDownloader.downloadMultiPicture(view, status, false, mPictureType, i);
            view.setOnClickListener(new OnPictureClickListener(i, status));
        }
        if (status.picCount < 9) {
            ImageView view;
            switch (status.picCount) {
                case 8:
                    view = (ImageView) grid.getChildAt(8);
                    view.setVisibility(View.INVISIBLE);
                    break;
                case 7:
                    for (int i = 8; i > 6; i--) {
                        view = (ImageView) grid.getChildAt(i);
                        view.setVisibility(View.INVISIBLE);
                    }
                    break;
                case 6:
                    for (int i = 8; i > 5; i--) {
                        view = (ImageView) grid.getChildAt(i);
                        view.setVisibility(View.GONE);
                    }
                    break;
                case 5:
                    for (int i = 8; i > 5; i--) {
                        view = (ImageView) grid.getChildAt(i);
                        view.setVisibility(View.GONE);
                    }
                    view = (ImageView) grid.getChildAt(5);
                    view.setVisibility(View.INVISIBLE);
                    break;
                case 4:
                    for (int i = 8; i > 5; i--) {
                        view = (ImageView) grid.getChildAt(i);
                        view.setVisibility(View.GONE);
                    }
                    for (int i = 5; i > 3; i--) {
                        view = (ImageView) grid.getChildAt(i);
                        view.setVisibility(View.INVISIBLE);
                    }
                    break;
                case 3:
                    for (int i = 8; i > 2; i--) {
                        view = (ImageView) grid.getChildAt(i);
                        view.setVisibility(View.GONE);
                    }
                    break;
                case 2:
                    for (int i = 8; i > 2; i--) {
                        view = (ImageView) grid.getChildAt(i);
                        view.setVisibility(View.GONE);
                    }
                    view = (ImageView) grid.getChildAt(2);
                    view.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }

    private void buildCoordinate() {
        final String token = GlobalContext.getCurrentAccount().token;
        MyAsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                MapImageDao dao = new MapImageDao();
                dao.setToken(token);
                dao.setLatitude(mWeiboStatus.weiboGeo.coordinate[0]);
                dao.setLongitude(mWeiboStatus.weiboGeo.coordinate[1]);
                try {
                    final Bitmap bitmap = dao.execute();
                    if (bitmap != null) {
                        GlobalContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayMap(bitmap);
                            }
                        });
                    }
                } catch (WeiboException e) {
                    Logger.logException(e);
                }
            }
        });
    }

    private void displayMap(Bitmap map) {
        if (!isAdded()) {
            return;
        }
        mMap.setVisibility(View.VISIBLE);
        mMap.setImageBitmap(map);
        mCoordinate.setVisibility(View.VISIBLE);
        mCoordinate.setText(getCoordinate(mWeiboStatus.weiboGeo));
    }

    private String getCoordinate(WeiboGeo geo) {
        double latitude = geo.coordinate[0];
        double longitude = geo.coordinate[1];
        StringBuilder builder = new StringBuilder();
        builder.append(decimalToSexagesimal(latitude));
        if (latitude > 0) {
            builder.append("N");
        } else if (latitude < 0) {
            builder.append("S");
        }
        builder.append(", ");
        builder.append(decimalToSexagesimal(longitude));
        if (longitude > 0) {
            builder.append("E");
        } else if (latitude < 0) {
            builder.append("W");
        }
        return builder.toString();
    }

    private String decimalToSexagesimal(double decimal) {
        int degrees = (int) Math.floor(decimal);
        decimal -= degrees;
        decimal *= 60;
        int minutes = (int) Math.floor(decimal);
        decimal -= minutes;
        double seconds = decimal * 60;
        return degrees + "°" + String.format("%02d", minutes) + "′" + String.format("%.1f", seconds) + "″";
    }

    private class OnPictureClickListener implements View.OnClickListener {
        private int mPosition;
        private WeiboStatus mStatus;

        public OnPictureClickListener(int position, WeiboStatus status) {
            mPosition = position;
            mStatus = status;
        }

        @Override
        public void onClick(View v) {
            startActivity(ActivityUtils.imageViewerActivity(mStatus, mPosition));
        }
    }

    private class OnAvatarClickListener implements View.OnClickListener {
        private WeiboUser mUser;

        public OnAvatarClickListener(WeiboUser user) {
            mUser = user;
        }

        @Override
        public void onClick(View v) {
            getActivity().startActivity(ActivityUtils.userActivity(mUser));
        }
    }

    private class OnAvatarLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            UserDialogFragment dialog = new UserDialogFragment();
            dialog.setOnUserChangedListener(WeiboFragment.this);
            Bundle args = new Bundle();
            args.putParcelable(UserDialogFragment.USER, mWeiboStatus.weiboUser);
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), TAG_USER_DIALOG);
            return true;
        }
    }
}
