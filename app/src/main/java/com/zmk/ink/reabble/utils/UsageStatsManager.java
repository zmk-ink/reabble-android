package com.zmk.ink.reabble.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsageStatsManager {
    private static final String STATS_FILE_NAME = "usage_stats.json";
    private static final long SAVE_INTERVAL_MS = 60 * 1000; // 每分钟保存一次

    private final Context context;
    private final Handler handler;
    private final SimpleDateFormat dateFormat;

    private boolean isTracking = false;
    private long sessionStartTime = 0;
    private String sessionStartDate = null;
    private Map<String, Integer> pendingMinutes = new HashMap<>();

    private final Runnable periodicSaveRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTracking) {
                checkDayChangeAndSave();
                handler.postDelayed(this, SAVE_INTERVAL_MS);
            }
        }
    };

    public UsageStatsManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    public synchronized void startTracking() {
        if (isTracking) {
            return;
        }
        isTracking = true;
        sessionStartTime = System.currentTimeMillis();
        sessionStartDate = dateFormat.format(new Date(sessionStartTime));
        pendingMinutes.clear();

        handler.postDelayed(periodicSaveRunnable, SAVE_INTERVAL_MS);
    }

    public synchronized void stopTracking() {
        if (!isTracking) {
            return;
        }
        handler.removeCallbacks(periodicSaveRunnable);

        long now = System.currentTimeMillis();
        accumulateTime(sessionStartTime, now);
        savePendingMinutes();

        isTracking = false;
        sessionStartTime = 0;
        sessionStartDate = null;
        pendingMinutes.clear();
    }

    private void checkDayChangeAndSave() {
        long now = System.currentTimeMillis();
        String currentDate = dateFormat.format(new Date(now));

        if (sessionStartDate != null && !sessionStartDate.equals(currentDate)) {
            long midnightTime = getMidnightTime(now);
            accumulateTime(sessionStartTime, midnightTime);
            savePendingMinutes();
            pendingMinutes.clear();

            sessionStartTime = midnightTime;
            sessionStartDate = currentDate;
        } else {
            accumulateTime(sessionStartTime, now);
            savePendingMinutes();
            pendingMinutes.clear();
            sessionStartTime = now;
        }
    }

    private void accumulateTime(long startTime, long endTime) {
        if (startTime >= endTime) {
            return;
        }

        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(startTime);

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endTime);

        while (startCal.before(endCal)) {
            String date = dateFormat.format(startCal.getTime());

            Calendar dayEnd = (Calendar) startCal.clone();
            dayEnd.set(Calendar.HOUR_OF_DAY, 23);
            dayEnd.set(Calendar.MINUTE, 59);
            dayEnd.set(Calendar.SECOND, 59);
            dayEnd.set(Calendar.MILLISECOND, 999);

            long segmentEnd = Math.min(dayEnd.getTimeInMillis(), endTime);
            long durationMs = segmentEnd - startCal.getTimeInMillis();
            int minutes = (int) (durationMs / (60 * 1000));

            if (minutes > 0) {
                int existing = pendingMinutes.getOrDefault(date, 0);
                pendingMinutes.put(date, existing + minutes);
            }

            startCal.add(Calendar.DAY_OF_MONTH, 1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        }
    }

    private long getMidnightTime(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private synchronized void savePendingMinutes() {
        if (pendingMinutes.isEmpty()) {
            return;
        }

        try {
            JSONObject root = loadStatsFile();
            JSONArray records = root.optJSONArray("records");
            if (records == null) {
                records = new JSONArray();
                root.put("records", records);
            }

            Map<String, Integer> existingRecords = new HashMap<>();
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                String date = record.getString("date");
                int minutes = record.getInt("minutes");
                existingRecords.put(date, minutes);
            }

            for (Map.Entry<String, Integer> entry : pendingMinutes.entrySet()) {
                String date = entry.getKey();
                int addMinutes = entry.getValue();
                int existing = existingRecords.getOrDefault(date, 0);
                existingRecords.put(date, existing + addMinutes);
            }

            JSONArray newRecords = new JSONArray();
            List<String> sortedDates = new ArrayList<>(existingRecords.keySet());
            java.util.Collections.sort(sortedDates);

            for (String date : sortedDates) {
                JSONObject record = new JSONObject();
                record.put("date", date);
                record.put("minutes", existingRecords.get(date));
                newRecords.put(record);
            }

            root.put("records", newRecords);
            saveStatsFile(root);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject loadStatsFile() {
        File file = new File(context.getFilesDir(), STATS_FILE_NAME);
        if (!file.exists()) {
            try {
                JSONObject root = new JSONObject();
                root.put("records", new JSONArray());
                return root;
            } catch (JSONException e) {
                return new JSONObject();
            }
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return new JSONObject(sb.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            try {
                JSONObject root = new JSONObject();
                root.put("records", new JSONArray());
                return root;
            } catch (JSONException ex) {
                return new JSONObject();
            }
        }
    }

    private void saveStatsFile(JSONObject root) {
        File file = new File(context.getFilesDir(), STATS_FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getStatsJson(int days) {
        try {
            JSONObject root = loadStatsFile();
            JSONArray records = root.optJSONArray("records");
            if (records == null) {
                records = new JSONArray();
            }

            Map<String, Integer> recordMap = new HashMap<>();
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                recordMap.put(record.getString("date"), record.getInt("minutes"));
            }

            JSONArray result = new JSONArray();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -(days - 1));

            for (int i = 0; i < days; i++) {
                String date = dateFormat.format(cal.getTime());
                int minutes = recordMap.getOrDefault(date, 0);

                JSONObject dayRecord = new JSONObject();
                dayRecord.put("date", date);
                dayRecord.put("minutes", minutes);
                result.put(dayRecord);

                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            return result.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "[]";
        }
    }

    public boolean isTracking() {
        return isTracking;
    }

    public int getTotalAllTimeMinutes() {
        try {
            JSONObject root = loadStatsFile();
            JSONArray records = root.optJSONArray("records");
            if (records == null) {
                return 0;
            }

            int total = 0;
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                total += record.getInt("minutes");
            }
            return total;
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getStartDate() {
        try {
            JSONObject root = loadStatsFile();
            JSONArray records = root.optJSONArray("records");
            if (records == null || records.length() == 0) {
                return dateFormat.format(new Date());
            }

            String earliestDate = records.getJSONObject(0).getString("date");
            for (int i = 1; i < records.length(); i++) {
                String date = records.getJSONObject(i).getString("date");
                if (date.compareTo(earliestDate) < 0) {
                    earliestDate = date;
                }
            }
            return earliestDate;
        } catch (JSONException e) {
            e.printStackTrace();
            return dateFormat.format(new Date());
        }
    }

    public int getTotalDays() {
        try {
            String startDateStr = getStartDate();
            Date startDate = dateFormat.parse(startDateStr);
            if (startDate == null) {
                return 1;
            }

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            long diffMs = todayCal.getTimeInMillis() - startCal.getTimeInMillis();
            int days = (int) (diffMs / (24 * 60 * 60 * 1000)) + 1;
            return Math.max(days, 1);
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}
