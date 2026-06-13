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
    private LinearLayout listaUrlLayout;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        prefs = getSharedPreferences("bollaviewer", MODE_PRIVATE);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(16),dp(16),dp(32));
        root.setBackgroundColor(0xFF111111);
        TextView tit = new TextView(this);
        tit.setText("\u2699 BollaViewer Settings");
        tit.setTextColor(0xFFFFFFFF); tit.setTextSize(20);
        tit.setTypeface(null,android.graphics.Typeface.BOLD);
        tit.setPadding(0,0,0,dp(20));
        root.addView(tit);
        root.addView(_l("URL webapp"));
        EditText euU=_i("https://script.google.com/macros/s/.../exec"); root.addView(euU);
        root.addView(_l("Token"));
        EditText euT=_i("token"); root.addView(euT);
        Button bS=_b("\ud83d\udcbe SALVA URL");
        bS.setOnClickListener(v->{
            String u=euU.getText().toString().trim(),t=euT.getText().toString().trim();
            if(u.isEmpty()){Toast.makeText(this,"URL vuoto",Toast.LENGTH_SHORT).show();return;}
            _saveUrl(u,t);euU.setText("");euT.setText("");_updList(listaUrlLayout);
            Toast.makeText(this,"Salvato",Toast.LENGTH_SHORT).show();
        });
        root.addView(bS);
        root.addView(_l("URL memorizzati"));
        listaUrlLayout=new LinearLayout(this); listaUrlLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(listaUrlLayout); _updList(listaUrlLayout);
        root.addView(_sep());
        root.addView(_l("Testo bolla"));
        EditText eT=_i(prefs.getString("bolla_testo","B")); root.addView(eT);
        root.addView(_l("Colore bolla (hex es. #1a73e8)"));
        EditText eC=_i(String.format("#%06X",0xFFFFFF&prefs.getInt("bolla_colore",0xFF1a73e8))); root.addView(eC);
        root.addView(_l("Trasparenza bolla")); SeekBar sB=_sb(prefs.getInt("bolla_alpha",230)); root.addView(sB);
        root.addView(_sep());
        root.addView(_l("Trasparenza X")); SeekBar sX=_sb(prefs.getInt("x_alpha",180)); root.addView(sX);
        Button bA=_b("\u2705 APPLICA");
        bA.setOnClickListener(v->{
            SharedPreferences.Editor ed=prefs.edit();
            ed.putString("bolla_testo",eT.getText().toString().trim());
            try{ed.putInt("bolla_colore",Color.parseColor(eC.getText().toString().trim()));}catch(Exception ignored){}
            ed.putInt("bolla_alpha",sB.getProgress()); ed.putInt("x_alpha",sX.getProgress()); ed.apply();
            Toast.makeText(this,"Salvato. Riavvia bolla.",Toast.LENGTH_SHORT).show(); finish();
        });
        root.addView(bA); sv.addView(root); setContentView(sv);
    }

    private void _saveUrl(String url,String tok){
        Set<String> l=new LinkedHashSet<>(prefs.getStringSet("url_lista",new LinkedHashSet<>()));
        l.add(url+"|||"+tok);
        prefs.edit().putStringSet("url_lista",l).putString("url_attivo",url).putString("token_attivo",tok).apply();
    }
    private void _updList(LinearLayout c){
        c.removeAllViews();
        Set<String> l=prefs.getStringSet("url_lista",new LinkedHashSet<>());
        String au=prefs.getString("url_attivo","");
        for(String e:l){
            String[]p=e.split("\|\|\|",2);String u=p[0],t=p.length>1?p[1]:"";
            LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
            TextView tv=new TextView(this);
            String lb=u.length()>38?"\u2026"+u.substring(u.length()-36):u;
            tv.setText((u.equals(au)?"\u25b6 ":"   ")+lb);
            tv.setTextColor(u.equals(au)?0xFF34a853:0xFFAAAAAA);tv.setTextSize(12);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
            r.addView(tv);
            Button bU=new Button(this);bU.setText("USA");bU.setTextSize(10);
            bU.setOnClickListener(v->{ prefs.edit().putString("url_attivo",u).putString("token_attivo",t).apply();_updList(c);});
            r.addView(bU);
            Button bD=new Button(this);bD.setText("\u2715");bD.setTextColor(0xFFea4335);bD.setTextSize(10);
            bD.setOnClickListener(v->{Set<String>n=new LinkedHashSet<>(prefs.getStringSet("url_lista",new LinkedHashSet<>()));n.remove(e);prefs.edit().putStringSet("url_lista",n).apply();if(u.equals(au))prefs.edit().putString("url_attivo","").putString("token_attivo","").apply();_updList(c);});
            r.addView(bD);c.addView(r);
        }
    }
    private TextView _l(String t){TextView tv=new TextView(this);tv.setText(t);tv.setTextColor(0xFF888888);tv.setTextSize(12);tv.setPadding(0,dp(10),0,dp(3));return tv;}
    private EditText _i(String h){EditText et=new EditText(this);et.setHint(h);et.setHintTextColor(0xFF555555);et.setTextColor(0xFFFFFFFF);et.setBackgroundColor(0xFF222222);et.setPadding(dp(10),dp(8),dp(10),dp(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(4);et.setLayoutParams(lp);return et;}
    private Button _b(String t){Button b=new Button(this);b.setText(t);b.setTextColor(0xFF000000);b.setBackgroundColor(0xFFFFFFFF);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=dp(10);b.setLayoutParams(lp);return b;}
    private SeekBar _sb(int p){SeekBar sb=new SeekBar(this);sb.setMax(255);sb.setProgress(p);return sb;}
    private View _sep(){View v=new View(this);v.setBackgroundColor(0xFF333333);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1));lp.topMargin=dp(16);lp.bottomMargin=dp(6);v.setLayoutParams(lp);return v;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
}
