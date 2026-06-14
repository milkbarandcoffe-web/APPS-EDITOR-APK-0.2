package com.danielz.bollaviewer;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private static final String SEP = "|||";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        prefs = getSharedPreferences("bollaviewer", MODE_PRIVATE);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(32));
        root.setBackgroundColor(0xFF111111);

        // Titolo
        TextView tit = new TextView(this);
        tit.setText("BollaViewer Settings");
        tit.setTextColor(0xFFFFFFFF); tit.setTextSize(20);
        tit.setTypeface(null, android.graphics.Typeface.BOLD);
        tit.setPadding(0, 0, 0, dp(20));
        root.addView(tit);

        // --- URL WEBVIEW ---
        root.addView(_h("URL WebView"));
        root.addView(_l("URL pagina web da caricare"));
        EditText eWU = _i(prefs.getString("url_webview", ""));
        root.addView(eWU);

        // --- URL BOLLA ---
        root.addView(_h("URL Bolla (POST)"));
        root.addView(_l("Endpoint GAS per invio clipboard"));
        EditText eBU = _i(prefs.getString("url_bolla", ""));
        root.addView(eBU);
        root.addView(_l("Token bolla"));
        EditText eBT = _i(prefs.getString("token_bolla", ""));
        root.addView(eBT);

        Button bSave = _b("SALVA URL");
        bSave.setOnClickListener(v -> {
            prefs.edit()
                .putString("url_webview", eWU.getText().toString().trim())
                .putString("url_bolla", eBU.getText().toString().trim())
                .putString("token_bolla", eBT.getText().toString().trim())
                .apply();
            Toast.makeText(this, "URL salvati", Toast.LENGTH_SHORT).show();
        });
        root.addView(bSave);

        root.addView(_sep());

        // --- BOLLA ASPETTO ---
        root.addView(_h("Aspetto Bolla"));
        root.addView(_l("Testo bolla"));
        EditText eT = _i(prefs.getString("bolla_testo", "B")); root.addView(eT);
        root.addView(_l("Colore bolla (hex es. #1a73e8)"));
        EditText eC = _i(String.format("#%06X", 0xFFFFFF & prefs.getInt("bolla_colore", 0xFF1a73e8))); root.addView(eC);
        root.addView(_l("Trasparenza bolla (0-255)"));
        SeekBar sB = _sb(prefs.getInt("bolla_alpha", 230)); root.addView(sB);

        root.addView(_sep());

        // --- X BUTTON ---
        root.addView(_h("Pulsante X"));
        root.addView(_l("Trasparenza X (0-255)"));
        SeekBar sX = _sb(prefs.getInt("x_alpha", 180)); root.addView(sX);

        Button bApply = _b("APPLICA");
        bApply.setOnClickListener(v -> {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString("bolla_testo", eT.getText().toString().trim());
            try { ed.putInt("bolla_colore", Color.parseColor(eC.getText().toString().trim())); }
            catch (Exception ignored) {}
            ed.putInt("bolla_alpha", sB.getProgress());
            ed.putInt("x_alpha", sX.getProgress());
            ed.apply();
            Toast.makeText(this, "Salvato.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
        root.addView(bApply);

        root.addView(_sep());

        // --- BACKUP / RIPRISTINO ---
        root.addView(_h("Backup / Ripristino"));
        root.addView(_l("Incolla qui il backup JSON e premi Ripristina, oppure premi Esporta per copiarlo."));
        EditText eBkp = _i("{ ... }");
        eBkp.setMinLines(3); eBkp.setMaxLines(6);
        root.addView(eBkp);

        LinearLayout bkpRow = new LinearLayout(this); bkpRow.setOrientation(LinearLayout.HORIZONTAL);
        Button bExport = _bHalf("ESPORTA");
        bExport.setOnClickListener(v -> {
            String json = exportJson();
            eBkp.setText(json);
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("backup", json));
            Toast.makeText(this, "Copiato negli appunti", Toast.LENGTH_SHORT).show();
        });
        Button bImport = _bHalf("RIPRISTINA");
        bImport.setOnClickListener(v -> {
            try {
                importJson(eBkp.getText().toString().trim());
                // Ricarica i campi
                eWU.setText(prefs.getString("url_webview", ""));
                eBU.setText(prefs.getString("url_bolla", ""));
                eBT.setText(prefs.getString("token_bolla", ""));
                eT.setText(prefs.getString("bolla_testo", "B"));
                eC.setText(String.format("#%06X", 0xFFFFFF & prefs.getInt("bolla_colore", 0xFF1a73e8)));
                sB.setProgress(prefs.getInt("bolla_alpha", 230));
                sX.setProgress(prefs.getInt("x_alpha", 180));
                Toast.makeText(this, "Ripristinato!", Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Toast.makeText(this, "JSON non valido: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        bkpRow.addView(bExport); bkpRow.addView(bImport);
        root.addView(bkpRow);

        sv.addView(root); setContentView(sv);
    }

    private String exportJson() {
        return "{"
            + "\"url_webview\":\"" + esc(prefs.getString("url_webview","")) + "\","
            + "\"url_bolla\":\"" + esc(prefs.getString("url_bolla","")) + "\","
            + "\"token_bolla\":\"" + esc(prefs.getString("token_bolla","")) + "\","
            + "\"bolla_testo\":\"" + esc(prefs.getString("bolla_testo","B")) + "\","
            + "\"bolla_colore\":" + prefs.getInt("bolla_colore", 0xFF1a73e8) + ","
            + "\"bolla_alpha\":" + prefs.getInt("bolla_alpha", 230) + ","
            + "\"x_alpha\":" + prefs.getInt("x_alpha", 180)
            + "}";
    }

    private void importJson(String json) {
        // Parser minimale senza librerie
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("url_webview", jstr(json, "url_webview"));
        ed.putString("url_bolla", jstr(json, "url_bolla"));
        ed.putString("token_bolla", jstr(json, "token_bolla"));
        ed.putString("bolla_testo", jstr(json, "bolla_testo"));
        String col = jval(json, "bolla_colore");
        if (!col.isEmpty()) try { ed.putInt("bolla_colore", Integer.parseInt(col.trim())); } catch (Exception ignored) {}
        String ba = jval(json, "bolla_alpha");
        if (!ba.isEmpty()) try { ed.putInt("bolla_alpha", Integer.parseInt(ba.trim())); } catch (Exception ignored) {}
        String xa = jval(json, "x_alpha");
        if (!xa.isEmpty()) try { ed.putInt("x_alpha", Integer.parseInt(xa.trim())); } catch (Exception ignored) {}
        ed.apply();
    }

    private String jstr(String json, String key) {
        String k = "\"" + key + "\":\"";
        int i = json.indexOf(k);
        if (i < 0) return "";
        int start = i + k.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }

    private String jval(String json, String key) {
        String k = "\"" + key + "\":";
        int i = json.indexOf(k);
        if (i < 0) return "";
        int start = i + k.length();
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        return end > start ? json.substring(start, end).trim() : "";
    }

    private String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private TextView _h(String t) { TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(0xFFFFFFFF); tv.setTextSize(15); tv.setTypeface(null, android.graphics.Typeface.BOLD); tv.setPadding(0, dp(16), 0, dp(4)); return tv; }
    private TextView _l(String t) { TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(0xFF888888); tv.setTextSize(12); tv.setPadding(0, dp(4), 0, dp(3)); return tv; }
    private EditText _i(String val) { EditText et = new EditText(this); et.setText(val); et.setHintTextColor(0xFF555555); et.setTextColor(0xFFFFFFFF); et.setBackgroundColor(0xFF222222); et.setPadding(dp(10), dp(8), dp(10), dp(8)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.bottomMargin = dp(4); et.setLayoutParams(lp); return et; }
    private Button _b(String t) { Button b = new Button(this); b.setText(t); b.setTextColor(0xFF000000); b.setBackgroundColor(0xFFFFFFFF); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.topMargin = dp(10); b.setLayoutParams(lp); return b; }
    private Button _bHalf(String t) { Button b = new Button(this); b.setText(t); b.setTextColor(0xFF000000); b.setBackgroundColor(0xFFCCCCCC); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); lp.topMargin = dp(8); lp.rightMargin = dp(4); b.setLayoutParams(lp); return b; }
    private SeekBar _sb(int p) { SeekBar sb = new SeekBar(this); sb.setMax(255); sb.setProgress(p); return sb; }
    private View _sep() { View v = new View(this); v.setBackgroundColor(0xFF333333); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)); lp.topMargin = dp(20); lp.bottomMargin = dp(8); v.setLayoutParams(lp); return v; }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
