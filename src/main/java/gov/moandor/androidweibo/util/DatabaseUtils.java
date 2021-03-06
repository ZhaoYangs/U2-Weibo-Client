package gov.moandor.androidweibo.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import gov.moandor.androidweibo.bean.AbsDraftBean;
import gov.moandor.androidweibo.bean.Account;
import gov.moandor.androidweibo.bean.CommentDraft;
import gov.moandor.androidweibo.bean.DirectMessage;
import gov.moandor.androidweibo.bean.DirectMessagesUser;
import gov.moandor.androidweibo.bean.TimelinePosition;
import gov.moandor.androidweibo.bean.WeiboComment;
import gov.moandor.androidweibo.bean.WeiboDraft;
import gov.moandor.androidweibo.bean.WeiboGroup;
import gov.moandor.androidweibo.bean.WeiboStatus;
import gov.moandor.androidweibo.bean.WeiboUser;
import gov.moandor.androidweibo.util.filter.WeiboFilter;

public class DatabaseUtils extends SQLiteOpenHelper {
    private static final String NAME = "weibo.db";
    private static final int VERSION = 19;

    private static final DatabaseUtils sInstance = new DatabaseUtils();
    private static final Gson sGson = new Gson();

    private static final String CREATE_ACCOUNT_TABLE = "create table " + Table.Account.TABLE_NAME + "("
            + Table.Account.ID + " integer primary key, " + Table.Account.TOKEN + " text, "
            + Table.Account.USER_INFO_DATA + " text)";

    private static final String CREATE_WEIBO_GROUP_TABLE = "create table " + Table.WeiboGroup.TABLE_NAME + "("
            + Table.WeiboGroup.ACCOUNT_ID + " integer primary key, " + Table.WeiboGroup.CONTENT_DATA + " text)";

    private static final String CREATE_WEIBO_STATUS_TABLE = "create table if not exists %s("
            + Table.WeiboStatus.POSITION + " integer primary key, " + Table.WeiboStatus.CONTENT_DATA + " text)";

    private static final String CREATE_ATME_STATUS_TABLE = "create table if not exists %s(" + Table.AtmeStatus.POSITION
            + " integer primary key, " + Table.AtmeStatus.CONTENT_DATA + " text)";

    private static final String CREATE_COMMENT_TABLE = "create table if not exists %s(" + Table.Comment.POSITION
            + " integer primary key, " + Table.Comment.CONTENT_DATA + " text)";

    private static final String CREATE_TIMELINE_POSITION_TABLE = "create table " + Table.TimelinePosition.TABLE_NAME
            + "(" + Table.TimelinePosition.ACCOUNT_ID + " integer, " + Table.TimelinePosition.FRAGMENT_INDEX
            + " integer, " + Table.TimelinePosition.SPINNER_POSITION + " integer, " + Table.TimelinePosition.POSITION
            + " integer, " + Table.TimelinePosition.TOP + " integer)";

    private static final String CREATE_DRAFT_TABLE = "create table " + Table.Draft.TABLE_NAME + "(" + Table.Draft.ID
            + " integer primary key autoincrement, " + Table.Draft.ACCOUNT_ID + " integer, " + Table.Draft.TYPE
            + " integer, " + Table.Draft.CONTENT_DATA + " text)";

    private static final String CREATE_FOLLOWING_AVATAR_PATH_TABLE = "create table "
            + Table.FollowingAvatarPath.TABLE_NAME + "(" + Table.FollowingAvatarPath.CONTENT_DATA + " text)";

    private static final String CREATE_DM_USER_TABLE = "create table " + Table.DmUser.TABLE_NAME + "("
            + Table.DmUser.ACCOUNT_ID + " integer, " + Table.DmUser.CONTENT_DATA + " text)";

    private static final String CREATE_FOLLOWING_ID_TABLE = "create table " + Table.FollowingId.TABLE_NAME + "("
            + Table.FollowingId.ACCOUNT_ID + " integer, " + Table.FollowingId.CONTENT_DATA + " text)";

    private static final String CREATE_DM_CONVERSATION_TABLE = "create table " + Table.DmConversation.TABLE_NAME + "("
            + Table.DmConversation.ACCOUNT_ID + " integer, " + Table.DmConversation.USER_ID + " integer, "
            + Table.DmConversation.CONTENT_DATA + " text)";

    private static final String CREATE_WEIBO_FILTER_TABLE = "create table " + Table.WeiboFilter.TABLE_NAME + "("
            + Table.WeiboFilter.ID + " integer primary key autoincrement, " + Table.WeiboFilter.CLASS_NAME + " text, "
            + Table.WeiboFilter.CONTENT_DATA + " text)";

    private DatabaseUtils() {
        super(GlobalContext.getInstance(), NAME, null, VERSION);
    }

    public static synchronized void insertOrUpdateAccount(Account account) {
        ContentValues cv = new ContentValues();
        cv.put(Table.Account.ID, account.user.id);
        cv.put(Table.Account.TOKEN, account.token);
        cv.put(Table.Account.USER_INFO_DATA, sGson.toJson(account.user));
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor =
                database.query(Table.Account.TABLE_NAME, new String[]{Table.Account.ID}, Table.Account.ID + "="
                        + account.user.id, null, null, null, null);
        if (cursor.getCount() > 0) {
            database.close();
            database = sInstance.getWritableDatabase();
            database.update(Table.Account.TABLE_NAME, cv, Table.Account.ID + "=" + account.user.id, null);
        } else {
            database.close();
            database = sInstance.getWritableDatabase();
            database.insert(Table.Account.TABLE_NAME, null, cv);
        }
        cursor.close();
        database.close();
    }

    public static synchronized List<Account> getAccounts() {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor = database.rawQuery("select * from " + Table.Account.TABLE_NAME, null);
        List<Account> result = new ArrayList<Account>();
        while (cursor.moveToNext()) {
            Account account = new Account();
            account.token = cursor.getString(cursor.getColumnIndex(Table.Account.TOKEN));
            account.user =
                    sGson.fromJson(cursor.getString(cursor.getColumnIndex(Table.Account.USER_INFO_DATA)),
                            WeiboUser.class);
            result.add(account);
        }
        cursor.close();
        database.close();
        return result;
    }

    public static synchronized void removeAccount(long id) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.delete(Table.Account.TABLE_NAME, Table.Account.ID + "=" + id, null);
        database.close();
    }

    public static void insertWeiboStatuses(List<WeiboStatus> statuses, long accountId, int group) {
        insertWeiboStatuses(Utilities.toSparseArray(statuses), accountId, group);
    }

    public static synchronized void insertWeiboStatuses(SparseArray<WeiboStatus> statuses, long accountId, int group) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.WeiboStatus.TABLE_NAME, accountId, group);
        database.execSQL(String.format(CREATE_WEIBO_STATUS_TABLE, tableName));
        int count = statuses.size();
        for (int i = 0; i < count; i++) {
            WeiboStatus status = statuses.valueAt(i);
            int position = statuses.keyAt(i);
            ContentValues values = new ContentValues();
            values.put(Table.WeiboStatus.POSITION, position);
            values.put(Table.WeiboStatus.CONTENT_DATA, sGson.toJson(status));
            database.insert(tableName, null, values);
        }
        database.close();
    }

    public static synchronized void updateWeiboStatus(WeiboStatus status, int position, long accountId, int group) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.WeiboStatus.TABLE_NAME, accountId, group);
        ContentValues values = new ContentValues();
        values.put(Table.WeiboStatus.POSITION, position);
        values.put(Table.WeiboStatus.CONTENT_DATA, sGson.toJson(status));
        database.update(tableName, values, Table.WeiboStatus.POSITION + "=" + position, null);
        database.close();
    }

    public static synchronized void removeWeiboStatus(int position, long accountId, int group) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.WeiboStatus.TABLE_NAME, accountId, group);
        Cursor cursor =
                database.rawQuery("select * from " + tableName + " where " + Table.WeiboStatus.POSITION + ">"
                        + position, null);
        database.delete(tableName, Table.WeiboStatus.POSITION + ">=" + position, null);
        while (cursor.moveToNext()) {
            ContentValues values = new ContentValues();
            values.put(Table.WeiboStatus.POSITION, cursor.getInt(cursor.getColumnIndex(Table.WeiboStatus.POSITION)) - 1);
            values.put(Table.WeiboStatus.CONTENT_DATA, cursor.getString(cursor
                    .getColumnIndex(Table.WeiboStatus.CONTENT_DATA)));
            database.insert(tableName, null, values);
        }
        cursor.close();
        database.close();
    }

    public static synchronized void removeWeiboStatuses(long accountId, int group) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.WeiboStatus.TABLE_NAME, accountId, group);
        database.execSQL(String.format(CREATE_WEIBO_STATUS_TABLE, tableName));
        database.delete(tableName, null, null);
        database.close();
    }

    public static synchronized List<WeiboStatus> getWeiboStatuses(long accountId, int group) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.WeiboStatus.TABLE_NAME, accountId, group);
        database.execSQL(String.format(CREATE_WEIBO_STATUS_TABLE, tableName));
        Cursor cursor = database.rawQuery("select * from " + tableName, null);
        SparseArray<WeiboStatus> statuses = new SparseArray<WeiboStatus>();
        while (cursor.moveToNext()) {
            String contentData = cursor.getString(cursor.getColumnIndex(Table.WeiboStatus.CONTENT_DATA));
            int position = cursor.getInt(cursor.getColumnIndex(Table.WeiboStatus.POSITION));
            statuses.append(position, sGson.fromJson(contentData, WeiboStatus.class));
        }
        cursor.close();
        database.close();
        List<WeiboStatus> result = new ArrayList<WeiboStatus>();
        int count = statuses.size();
        for (int i = 0; i < count; i++) {
            result.add(statuses.get(i));
        }
        return result;
    }

    public static void insertAtmeStatuses(List<WeiboStatus> statuses, long accountId, int filter) {
        insertAtmeStatuses(Utilities.toSparseArray(statuses), accountId, filter);
    }

    public static synchronized void insertAtmeStatuses(SparseArray<WeiboStatus> statuses, long accountId, int filter) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.AtmeStatus.TABLE_NAME, accountId, filter);
        database.execSQL(String.format(CREATE_ATME_STATUS_TABLE, tableName));
        int count = statuses.size();
        for (int i = 0; i < count; i++) {
            WeiboStatus status = statuses.valueAt(i);
            int position = statuses.keyAt(i);
            ContentValues values = new ContentValues();
            values.put(Table.AtmeStatus.POSITION, position);
            values.put(Table.AtmeStatus.CONTENT_DATA, sGson.toJson(status));
            database.insert(tableName, null, values);
        }
        database.close();
    }

    public static synchronized void updateAtmeStatus(WeiboStatus status, int position, long accountId, int group) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.AtmeStatus.TABLE_NAME, accountId, group);
        ContentValues values = new ContentValues();
        values.put(Table.AtmeStatus.POSITION, position);
        values.put(Table.AtmeStatus.CONTENT_DATA, sGson.toJson(status));
        database.update(tableName, values, Table.AtmeStatus.POSITION + "=" + position, null);
        database.close();
    }

    public static synchronized void removeAtmeStatus(int position, long accountId, int group) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.AtmeStatus.TABLE_NAME, accountId, group);
        Cursor cursor =
                database.rawQuery(
                        "select * from " + tableName + " where " + Table.AtmeStatus.POSITION + ">" + position, null);
        database.delete(tableName, Table.AtmeStatus.POSITION + ">=" + position, null);
        while (cursor.moveToNext()) {
            ContentValues values = new ContentValues();
            values.put(Table.AtmeStatus.POSITION, cursor.getInt(cursor.getColumnIndex(Table.AtmeStatus.POSITION)) - 1);
            values.put(Table.AtmeStatus.CONTENT_DATA, cursor.getString(cursor
                    .getColumnIndex(Table.AtmeStatus.CONTENT_DATA)));
            database.insert(tableName, null, values);
        }
        cursor.close();
        database.close();
    }

    public static synchronized void removeAtmeStatuses(long accountId, int filter) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.AtmeStatus.TABLE_NAME, accountId, filter);
        database.execSQL(String.format(CREATE_ATME_STATUS_TABLE, tableName));
        database.delete(tableName, null, null);
        database.close();
    }

    public static synchronized List<WeiboStatus> getAtmeStatuses(long accountId, int filter) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.AtmeStatus.TABLE_NAME, accountId, filter);
        database.execSQL(String.format(CREATE_ATME_STATUS_TABLE, tableName));
        Cursor cursor = database.rawQuery("select * from " + tableName, null);
        SparseArray<WeiboStatus> statuses = new SparseArray<WeiboStatus>();
        while (cursor.moveToNext()) {
            String contentData = cursor.getString(cursor.getColumnIndex(Table.AtmeStatus.CONTENT_DATA));
            int position = cursor.getInt(cursor.getColumnIndex(Table.AtmeStatus.POSITION));
            statuses.append(position, sGson.fromJson(contentData, WeiboStatus.class));
        }
        cursor.close();
        database.close();
        List<WeiboStatus> result = new ArrayList<WeiboStatus>();
        int count = statuses.size();
        for (int i = 0; i < count; i++) {
            result.add(statuses.get(i));
        }
        return result;
    }

    public static void insertComments(List<WeiboComment> comments, long accountId, int filter) {
        insertComments(Utilities.toSparseArray(comments), accountId, filter);
    }

    public static synchronized void insertComments(SparseArray<WeiboComment> comments, long accountId, int filter) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.Comment.TABLE_NAME, accountId, filter);
        database.execSQL(String.format(CREATE_COMMENT_TABLE, tableName));
        int count = comments.size();
        for (int i = 0; i < count; i++) {
            WeiboComment comment = comments.valueAt(i);
            int position = comments.keyAt(i);
            ContentValues values = new ContentValues();
            values.put(Table.Comment.POSITION, position);
            values.put(Table.Comment.CONTENT_DATA, sGson.toJson(comment));
            database.insert(tableName, null, values);
        }
        database.close();
    }

    public static synchronized void removeComment(int position, long accountId, int group) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.Comment.TABLE_NAME, accountId, group);
        Cursor cursor =
                database.rawQuery("select * from " + tableName + " where " + Table.Comment.POSITION + ">" + position,
                        null);
        database.delete(tableName, Table.Comment.POSITION + ">=" + position, null);
        while (cursor.moveToNext()) {
            ContentValues values = new ContentValues();
            values.put(Table.Comment.POSITION, cursor.getInt(cursor.getColumnIndex(Table.Comment.POSITION)) - 1);
            values.put(Table.Comment.CONTENT_DATA, cursor.getString(cursor.getColumnIndex(Table.Comment.CONTENT_DATA)));
            database.insert(tableName, null, values);
        }
        cursor.close();
        database.close();
    }

    public static synchronized void removeComments(long accountId, int filter) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.Comment.TABLE_NAME, accountId, filter);
        database.execSQL(String.format(CREATE_COMMENT_TABLE, tableName));
        database.delete(tableName, null, null);
        database.close();
    }

    public static synchronized List<WeiboComment> getComments(long accountId, int filter) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        String tableName = String.format(Locale.ENGLISH, Table.Comment.TABLE_NAME, accountId, filter);
        database.execSQL(String.format(CREATE_COMMENT_TABLE, tableName));
        Cursor cursor = database.rawQuery("select * from " + tableName, null);
        SparseArray<WeiboComment> comments = new SparseArray<WeiboComment>();
        while (cursor.moveToNext()) {
            String contentData = cursor.getString(cursor.getColumnIndex(Table.Comment.CONTENT_DATA));
            int position = cursor.getInt(cursor.getColumnIndex(Table.Comment.POSITION));
            comments.append(position, sGson.fromJson(contentData, WeiboComment.class));
        }
        cursor.close();
        database.close();
        List<WeiboComment> result = new ArrayList<WeiboComment>();
        int count = comments.size();
        for (int i = 0; i < count; i++) {
            result.add(comments.get(i));
        }
        return result;
    }

    public static synchronized void updateWeiboGroups(long accountId, WeiboGroup[] groups) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.delete(Table.WeiboGroup.TABLE_NAME, Table.WeiboGroup.ACCOUNT_ID + "=" + accountId, null);
        ContentValues values = new ContentValues();
        values.put(Table.WeiboGroup.ACCOUNT_ID, accountId);
        values.put(Table.WeiboGroup.CONTENT_DATA, sGson.toJson(groups));
        database.insert(Table.WeiboGroup.TABLE_NAME, null, values);
        database.close();
    }

    public static synchronized WeiboGroup[] getWeiboGroups(long accountId) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor =
                database.rawQuery("select * from " + Table.WeiboGroup.TABLE_NAME + " where "
                        + Table.WeiboGroup.ACCOUNT_ID + "=" + accountId, null);
        try {
            if (cursor.moveToNext()) {
                return sGson.fromJson(cursor.getString(cursor.getColumnIndex(Table.WeiboGroup.CONTENT_DATA)),
                        WeiboGroup[].class);
            }
        } finally {
            cursor.close();
            database.close();
        }
        return null;
    }

    public static synchronized TimelinePosition getTimelinePosition(long accountId, int fragmentIndex,
                                                                    int spinnerPosition) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor =
                database.rawQuery("select " + Table.TimelinePosition.POSITION + ", " + Table.TimelinePosition.TOP
                        + " from " + Table.TimelinePosition.TABLE_NAME + " where " + Table.TimelinePosition.ACCOUNT_ID
                        + "=? and " + Table.TimelinePosition.FRAGMENT_INDEX + "=? and "
                        + Table.TimelinePosition.SPINNER_POSITION + "=?", new String[]{String.valueOf(accountId),
                        String.valueOf(fragmentIndex), String.valueOf(spinnerPosition)});
        TimelinePosition result = new TimelinePosition();
        if (cursor.moveToNext()) {
            result.position = cursor.getInt(cursor.getColumnIndex(Table.TimelinePosition.POSITION));
            result.top = cursor.getInt(cursor.getColumnIndex(Table.TimelinePosition.TOP));
        }
        cursor.close();
        database.close();
        return result;
    }

    public static synchronized void insertOrUpdateTimelinePosition(TimelinePosition position, int fragmentIndex,
                                                                   int spinnerPosition, long accountId) {
        ContentValues values = new ContentValues();
        values.put(Table.TimelinePosition.ACCOUNT_ID, accountId);
        values.put(Table.TimelinePosition.FRAGMENT_INDEX, fragmentIndex);
        values.put(Table.TimelinePosition.SPINNER_POSITION, spinnerPosition);
        values.put(Table.TimelinePosition.POSITION, position.position);
        values.put(Table.TimelinePosition.TOP, position.top);
        String where =
                Table.TimelinePosition.ACCOUNT_ID + "=" + String.valueOf(accountId) + " and "
                        + Table.TimelinePosition.FRAGMENT_INDEX + "=" + String.valueOf(fragmentIndex) + " and "
                        + Table.TimelinePosition.SPINNER_POSITION + "=" + String.valueOf(spinnerPosition);
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor =
                database.query(Table.TimelinePosition.TABLE_NAME, new String[]{Table.TimelinePosition.POSITION}, where,
                        null, null, null, null);
        if (cursor.getCount() > 0) {
            database.close();
            database = sInstance.getWritableDatabase();
            database.update(Table.TimelinePosition.TABLE_NAME, values, where, null);
        } else {
            database.close();
            database = sInstance.getWritableDatabase();
            database.insert(Table.TimelinePosition.TABLE_NAME, null, values);
        }
        cursor.close();
        database.close();
    }

    public static synchronized void insertDraft(AbsDraftBean bean) {
        ContentValues values = new ContentValues();
        values.put(Table.Draft.ACCOUNT_ID, bean.accountId);
        int type = 0;
        if (bean instanceof WeiboDraft) {
            type = Table.Draft.TYPE_WEIBO;
        } else if (bean instanceof CommentDraft) {
            type = Table.Draft.TYPE_COMMENT;
        }
        values.put(Table.Draft.TYPE, type);
        values.put(Table.Draft.CONTENT_DATA, sGson.toJson(bean));
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.insert(Table.Draft.TABLE_NAME, null, values);
        database.close();
    }

    public static synchronized List<AbsDraftBean> getDrafts(long accountId) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor =
                database.rawQuery("select * from " + Table.Draft.TABLE_NAME + " where " + Table.Draft.ACCOUNT_ID + "="
                        + accountId + " order by " + Table.Draft.ID + " desc", null);
        List<AbsDraftBean> result = new ArrayList<AbsDraftBean>();
        while (cursor.moveToNext()) {
            String contentData = cursor.getString(cursor.getColumnIndex(Table.Draft.CONTENT_DATA));
            int type = cursor.getInt(cursor.getColumnIndex(Table.Draft.TYPE));
            AbsDraftBean bean = null;
            switch (type) {
                case Table.Draft.TYPE_WEIBO:
                    bean = sGson.fromJson(contentData, WeiboDraft.class);
                    break;
                case Table.Draft.TYPE_COMMENT:
                    bean = sGson.fromJson(contentData, CommentDraft.class);
                    break;
            }
            bean.id = cursor.getInt(cursor.getColumnIndex(Table.Draft.ID));
            result.add(bean);
        }
        cursor.close();
        database.close();
        return result;
    }

    public static synchronized void removeDrafts(int[] ids) {
        String idsString = Arrays.toString(ids);
        idsString = idsString.replace('[', '(');
        idsString = idsString.replace(']', ')');
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.delete(Table.Draft.TABLE_NAME, Table.Draft.ID + " in " + idsString, null);
        database.close();
    }

    public static synchronized List<String> getFollowingAvatarPaths() {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor = database.rawQuery("select * from " + Table.FollowingAvatarPath.TABLE_NAME, null);
        List<String> result = null;
        if (cursor.moveToNext()) {
            String json = cursor.getString(cursor.getColumnIndex(Table.FollowingAvatarPath.CONTENT_DATA));
            result = sGson.fromJson(json, new TypeToken<List<String>>() {
            }.getType());
        }
        cursor.close();
        database.close();
        return result;
    }

    public static synchronized void updateFollowingAvatarPaths(List<String> paths) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from " + Table.FollowingAvatarPath.TABLE_NAME, null);
        ContentValues values = new ContentValues();
        values.put(Table.FollowingAvatarPath.CONTENT_DATA, sGson.toJson(paths, new TypeToken<List<String>>() {
        }
                .getType()));
        if (cursor.getCount() > 0) {
            database.update(Table.FollowingAvatarPath.TABLE_NAME, values, null, null);
        } else {
            database.insert(Table.FollowingAvatarPath.TABLE_NAME, null, values);
        }
        cursor.close();
        database.close();
    }

    public static synchronized void updateDmUsers(DmUsers users, long accountId) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.delete(Table.DmUser.TABLE_NAME, Table.DmUser.ACCOUNT_ID + "=" + accountId, null);
        ContentValues values = new ContentValues();
        values.put(Table.DmUser.ACCOUNT_ID, accountId);
        values.put(Table.DmUser.CONTENT_DATA, sGson.toJson(users));
        database.insert(Table.DmUser.TABLE_NAME, null, values);
        database.close();
    }

    public static synchronized DmUsers getDmUsers(long accountId) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        String sql = "select * from " + Table.DmUser.TABLE_NAME + " where " + Table.DmUser.ACCOUNT_ID + "=" + accountId;
        Cursor cursor = database.rawQuery(sql, null);
        try {
            if (cursor.moveToNext()) {
                String json = cursor.getString(cursor.getColumnIndex(Table.DmUser.CONTENT_DATA));
                return sGson.fromJson(json, DmUsers.class);
            }
        } finally {
            cursor.close();
            database.close();
        }
        return null;
    }

    public static synchronized void updateFollowingIds(long[] ids, long accountId) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.delete(Table.FollowingId.TABLE_NAME, Table.FollowingId.ACCOUNT_ID + "=" + accountId, null);
        ContentValues values = new ContentValues();
        values.put(Table.FollowingId.ACCOUNT_ID, accountId);
        values.put(Table.FollowingId.CONTENT_DATA, sGson.toJson(ids));
        database.insert(Table.FollowingId.TABLE_NAME, null, values);
        database.close();
    }

    public static synchronized long[] getFollowingIds(long accountId) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        String sql =
                "select * from " + Table.FollowingId.TABLE_NAME + " where " + Table.FollowingId.ACCOUNT_ID + "="
                        + accountId;
        Cursor cursor = database.rawQuery(sql, null);
        try {
            if (cursor.moveToNext()) {
                String json = cursor.getString(cursor.getColumnIndex(Table.FollowingId.CONTENT_DATA));
                return sGson.fromJson(json, long[].class);
            }
        } finally {
            cursor.close();
            database.close();
        }
        return null;
    }

    public static synchronized void updateDmConversation(long accountId, long userId, DirectMessage[] messages) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.delete(Table.DmConversation.TABLE_NAME, Table.DmConversation.ACCOUNT_ID + "=" + accountId + " and "
                + Table.DmConversation.USER_ID + "=" + userId, null);
        ContentValues values = new ContentValues();
        values.put(Table.DmConversation.ACCOUNT_ID, accountId);
        values.put(Table.DmConversation.USER_ID, userId);
        values.put(Table.DmConversation.CONTENT_DATA, sGson.toJson(messages));
        database.insert(Table.DmConversation.TABLE_NAME, null, values);
        database.close();
    }

    public static synchronized DirectMessage[] getDmConversation(long accountId, long userId) {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        String sql =
                "select * from " + Table.DmConversation.TABLE_NAME + " where " + Table.DmConversation.ACCOUNT_ID + "="
                        + accountId + " and " + Table.DmConversation.USER_ID + "=" + userId;
        Cursor cursor = database.rawQuery(sql, null);
        try {
            if (cursor.moveToNext()) {
                String json = cursor.getString(cursor.getColumnIndex(Table.DmConversation.CONTENT_DATA));
                return sGson.fromJson(json, DirectMessage[].class);
            }
        } finally {
            cursor.close();
            database.close();
        }
        return null;
    }

    public static synchronized void insertOrUpdateWeiboFilter(WeiboFilter filter) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Table.WeiboFilter.CLASS_NAME, filter.getClass().getName());
        values.put(Table.WeiboFilter.CONTENT_DATA, sGson.toJson(filter));
        Cursor cursor =
                database.rawQuery("select " + Table.WeiboFilter.ID + " from " + Table.WeiboFilter.TABLE_NAME
                        + " where " + Table.WeiboFilter.ID + "=" + filter.getId(), null);
        if (cursor.moveToNext()) {
            values.put(Table.WeiboFilter.ID, filter.getId());
            database.update(Table.WeiboFilter.TABLE_NAME, values, Table.WeiboFilter.ID + "=" + filter.getId(), null);
        } else {
            database.insert(Table.WeiboFilter.TABLE_NAME, null, values);
        }
        cursor.close();
        database.close();
    }

    public static synchronized void removeWeiboFilter(int id) {
        SQLiteDatabase database = sInstance.getWritableDatabase();
        database.delete(Table.WeiboFilter.TABLE_NAME, Table.WeiboFilter.ID + "=" + id, null);
        database.close();
    }

    public static synchronized WeiboFilter[] getWeiboFilters() {
        SQLiteDatabase database = sInstance.getReadableDatabase();
        Cursor cursor = database.rawQuery("select * from " + Table.WeiboFilter.TABLE_NAME, null);
        List<WeiboFilter> result = new ArrayList<WeiboFilter>();
        while (cursor.moveToNext()) {
            String className = cursor.getString(cursor.getColumnIndex(Table.WeiboFilter.CLASS_NAME));
            String json = cursor.getString(cursor.getColumnIndex(Table.WeiboFilter.CONTENT_DATA));
            try {
                WeiboFilter filter = (WeiboFilter) sGson.fromJson(json, Class.forName(className));
                filter.setId(cursor.getInt(cursor.getColumnIndex(Table.WeiboFilter.ID)));
                result.add(filter);
            } catch (ClassNotFoundException e) {
                Logger.logException(e);
            }
        }
        cursor.close();
        database.close();
        return result.toArray(new WeiboFilter[0]);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ACCOUNT_TABLE);
        db.execSQL(CREATE_WEIBO_GROUP_TABLE);
        db.execSQL(CREATE_TIMELINE_POSITION_TABLE);
        db.execSQL(CREATE_DRAFT_TABLE);
        db.execSQL(CREATE_FOLLOWING_AVATAR_PATH_TABLE);
        db.execSQL(CREATE_DM_USER_TABLE);
        db.execSQL(CREATE_FOLLOWING_ID_TABLE);
        db.execSQL(CREATE_DM_CONVERSATION_TABLE);
        db.execSQL(CREATE_WEIBO_FILTER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 9:
                db.execSQL(CREATE_DRAFT_TABLE);
            case 10:
                db.execSQL(CREATE_FOLLOWING_AVATAR_PATH_TABLE);
            case 11:
                db.execSQL(CREATE_DM_USER_TABLE);
            case 12:
                db.execSQL("drop table if exists " + Table.DmUser.TABLE_NAME);
                db.execSQL(CREATE_DM_USER_TABLE);
            case 13:
                db.execSQL(CREATE_FOLLOWING_ID_TABLE);
            case 14:
                db.execSQL(CREATE_DM_CONVERSATION_TABLE);
            case 15:
                db.delete(Table.DmUser.TABLE_NAME, null, null);
            case 16:
            case 17:
                db.execSQL("drop table if exists " + Table.WeiboGroup.TABLE_NAME);
                db.execSQL(CREATE_WEIBO_GROUP_TABLE);
            case 18:
                db.execSQL(CREATE_WEIBO_FILTER_TABLE);
        }
    }

    public static class DmUsers {
        public DirectMessagesUser[] users;
        public int nextCursor;
    }

    private static final class Table {
        public static final class Account {
            public static final String TABLE_NAME = "account_table";
            public static final String ID = "id";
            public static final String TOKEN = "token";
            public static final String USER_INFO_DATA = "user_info_data";
        }

        public static final class WeiboGroup {
            public static final String TABLE_NAME = "weibo_group_table";
            public static final String ACCOUNT_ID = "account_id";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class WeiboStatus {
            public static final String TABLE_NAME = "weibo_status_%d_%d";
            public static final String POSITION = "position";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class AtmeStatus {
            public static final String TABLE_NAME = "atme_status_%d_%d";
            public static final String POSITION = "position";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class Comment {
            public static final String TABLE_NAME = "comment_%d_%d";
            public static final String POSITION = "position";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class TimelinePosition {
            public static final String TABLE_NAME = "timeline_position";
            public static final String ACCOUNT_ID = "account_id";
            public static final String FRAGMENT_INDEX = "fragment_index";
            public static final String SPINNER_POSITION = "spinner_position";
            public static final String POSITION = "position";
            public static final String TOP = "top";
        }

        public static final class Draft {
            public static final int TYPE_WEIBO = 0;
            public static final int TYPE_COMMENT = 1;
            public static final String TABLE_NAME = "draft_table";
            public static final String ID = "id";
            public static final String ACCOUNT_ID = "account_id";
            public static final String TYPE = "type";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class FollowingAvatarPath {
            public static final String TABLE_NAME = "following_avatar_path";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class DmUser {
            public static final String TABLE_NAME = "dm_user";
            public static final String ACCOUNT_ID = "account_id";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class FollowingId {
            public static final String TABLE_NAME = "following_ids";
            public static final String ACCOUNT_ID = "account_id";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class DmConversation {
            public static final String TABLE_NAME = "dm_conversation";
            public static final String ACCOUNT_ID = "account_id";
            public static final String USER_ID = "user_id";
            public static final String CONTENT_DATA = "content_data";
        }

        public static final class WeiboFilter {
            public static final String TABLE_NAME = "weibo_filter";
            public static final String ID = "id";
            public static final String CLASS_NAME = "class_name";
            public static final String CONTENT_DATA = "content_data";
        }
    }
}
