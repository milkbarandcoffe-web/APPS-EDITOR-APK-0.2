package com.danielz.bollaviewer;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class BubbleService extends Service {
    public static final String CH_ID = "bolla_ch";
    public static final int NOTIF_ID = 77;
    private WindowManager wm;
    private TextView bolla;
    private WindowManager.LayoutParams params;
    private SharedPreferences prefs;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("bollaviewer", MODE_PRIVATE);
        _createChannel();
        startForeground(NOTIF_ID, _buildNotif());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        _creaBolla();
    }

    private void _creaBolla() {
        bolla = new TextView(this);
        _aggiornaBolla();
        int size = _dp(58);
        params = new WindowManager.LayoutParams(size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = prefs.getInt("bolla_x", 20);
        params.y = prefs.getInt("bolla_y", 400);
        wm.addView(bolla, params);
        _attachTouch();
    }

    private void _aggiornaBolla() {
        bolla.setText(prefs.getString("bolla_testo", "B"));
        bolla.setTextSize(14);
        bolla.setTextColor(Color.WHITE);
        bolla.setGravity(Gravity.CENTER);
        bolla.setTypeface(null, Typeface.BOLD);
        bolla.setAlpha(prefs.getInt("bolla_alpha", 230) / 255f);
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(prefs.getInt("bolla_colore", 0xFF1a73e8));
        d.setStroke(_dp(2), 0x44000000);
        bolla.setBackground(d);
    }

    private void _attachTouch() {
        bolla.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy; float itx, ity; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ix = params.x; iy = params.y;
                        itx = e.getRawX(); ity = e.getRawY(); moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(e.getRawX()-itx), dy = (int)(e.getRawY()-ity);
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true;
                        if (moved) { params.x = ix+dx; params.y = iy+dy; wm.updateViewLayout(bolla, params); }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) prefs.edit().putInt("bolla_x", params.x).putInt("bolla_y", params.y).apply();
                        else _invia();
                        return true;
                }
                return false;
            }
        });
    }

    private void _invia() {
        String urlBase = prefs.getString("url_bolla", "");
        String token = prefs.getString("token_bolla", "");
        if (urlBase.isEmpty()) { _showText("NO URL"); return; }
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) { _showText("CLIP VUOTA"); return; }
        CharSequence cs = cm.getPrimaryClip().getItemAt(0).getText();
        if (cs == null || cs.toString().trim().isEmpty()) { _showText("CLIP VUOTA"); return; }
        final String testo = cs.toString();
        final String urlFinal = token.isEmpty() ? urlBase : urlBase + "?token=" + token;

        _showText("...");
        new Thread(() -> {
            String risposta = "";
            int code = -1;
            try {
                URL url = new URL(urlFinal);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(true);
                byte[] body = testo.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(body.length);
                try (OutputStream os = conn.getOutputStream()) { os.write(body); }
                code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    risposta = sb.toString();
                }
                conn.disconnect();
            } catch (Exception ex) {
                risposta = "ERR:" + ex.getMessage();
            }
            final String risp = risposta;
            final int c = code;
            handler.post(() -> _mostraRisposta(risp, c));
        }).start();
    }

    private void _mostraRisposta(String json, int httpCode) {
        // Mostra risposta reale: cerca "ok":true/false e "error":"..."
        boolean ok = json.contains("\"ok\":true");
        String msg;
        if (json.isEmpty()) {
            msg = "HTTP " + httpCode;
        } else if (ok) {
            msg = "OK";
        } else {
            // Estrai campo error se presente
            String err = "";
            int ei = json.indexOf("\"error\":\"");
            if (ei >= 0) {
                int start = ei + 9;
                int end = json.indexOf("\"", start);
                if (end > start) err = json.substring(start, end);
            }
            msg = err.isEmpty() ? "ERR " + httpCode : err;
        }
        // Mostra sulla bolla per 3 secondi
        String testoOriginale = prefs.getString("bolla_testo", "B");
        bolla.setText(msg);
        bolla.setTextSize(10);
        _setC(ok ? 0xFF34a853 : 0xFFea4335);
        handler.postDelayed(() -> {
            bolla.setText(testoOriginale);
            bolla.setTextSize(14);
            _setC(prefs.getInt("bolla_colore", 0xFF1a73e8));
        }, 3000);
    }

    private void _showText(String msg) {
        bolla.setText(msg);
        bolla.setTextSize(10);
    }

    private void _setC(int c) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(c);
        bolla.setBackground(d);
    }

    private void _createChannel() {
        NotificationChannel ch = new NotificationChannel(CH_ID, "Bolla", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }

    private Notification _buildNotif() {
        return new Notification.Builder(this, CH_ID)
            .setContentTitle("Bolla attiva")
            .setContentText("Tap = invia clipboard")
            .setSmallIcon(android.R.drawable.ic_menu_send).build();
    }

    private int _dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() {
        super.onDestroy();
        if (bolla != null) try { wm.removeView(bolla); } catch (Exception ignored) {}
    }
}
