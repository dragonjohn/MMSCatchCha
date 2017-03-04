package dragonjohn.mmscatchcha;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class main extends Activity {

    final String [] programs = {"baa","lta","ada"};
    final String [] programsName = {"空中英語教室","大家說英語","彭蒙惠英語"};


    int defaultIndex = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultIndexSetting = preference.getString("default_index","0");
        defaultIndex = Integer.parseInt(defaultIndexSetting);
        Toast.makeText(this,defaultIndexSetting,Toast.LENGTH_LONG).show();
        Spinner programSpinner = (Spinner)findViewById(R.id.spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,programsName);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        programSpinner.setAdapter(dataAdapter);

    }

    public void catchUrls(View view){
        //must check network connection

        final ArrayList<String> urls = new ArrayList<String>();

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites()
                .detectNetwork().penaltyLog().build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyLog().penaltyDeath().build());

        //TextView mainView = (TextView)findViewById(R.id.url);
        //String url = mainView.getText().toString();
        Spinner programSpinner = (Spinner)findViewById(R.id.spinner);
        String program = programs[programSpinner.getSelectedItemPosition()];
        DatePicker datePicker = (DatePicker)findViewById(R.id.datePicker);
        String year = String.valueOf(datePicker.getYear());
        String month = datePicker.getMonth()+1<10?"0"+(datePicker.getMonth()+1):String.valueOf(datePicker.getMonth()+1);
        String dayOfMonth = String.valueOf(datePicker.getDayOfMonth());
        String date = year+"-"+month+"-"+dayOfMonth;

        BufferedReader in = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            //2013-08-11 the source change to send correct URL (contains url and header parameters)
            //String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

            //url params: mag = baa 空中英語教室, day = 2013-08-10 日期
            request.setURI(new URI("http://www.studioclassroom.com/index_play.php?mag="+program+"&day=" + date));

            //設定header
            request.setHeader("Host", "www.studioclassroom.com");
            request.setHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
            request.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            request.setHeader("Accept-Language","zh-tw,zh;q=0.8,en-us;q=0.5,en;q=0.3");
            request.setHeader("Accept-Encoding", "gzip, deflate");
            request.setHeader("Connection", "keep-alive");
            //加入此參數讓對方伺服器確認是從以下網指連入的
            request.setHeader("Referer", "http://www.studioclassroom.com/default.php");
            request.getAllHeaders();
            HttpResponse response = client.execute(request);
            response.getAllHeaders();
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String page = sb.toString();
            //2013-08-11
            String[] wmaCut = page.split(".wma");
            String[] httpCut;
            for (int i = 0; i < wmaCut.length - 1; i++) {
                httpCut = wmaCut[i].split("http");
                // 0:url with domain name, 1:url with ip
                urls.add("mms" + httpCut[httpCut.length - 1] + ".wma");
                if (i == defaultIndex) {
                    //Toast.makeText(view.getContext(), "http" + httpCut[httpCut.length - 1] + ".wma", Toast.LENGTH_LONG).show();
                    startMMS(i, urls);
                }
            }
            if(urls.size() != 0 && defaultIndex >= urls.size()){
                startMMS(0, urls);
            }
            ArrayAdapter<String> arrayData = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,urls);
            ListView urlListView = (ListView)findViewById(R.id.listView);
            urlListView.setAdapter(arrayData);

            urlListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
                    //Toast.makeText(arg0.getContext(), "you got No." + arg3+" "+arg0.getContext().toString()+arg1.getContext().toString()+arg2, Toast.LENGTH_LONG).show();
                    startMMS(arg2, urls);
                }
            });
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, "Please connect to the network!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NetworkOnMainThreadException e){
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startMMS(int position, ArrayList<String> urls) {
        Uri mmsUrl = Uri.parse(urls.get(position));
        Intent mediaIntent = new Intent(Intent.ACTION_VIEW,mmsUrl);
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(mediaIntent, 0);
        boolean isIntentSafe = activities.size() > 0;
        if(isIntentSafe){
            //mediaIntent.setComponent(new ComponentName("org.videolan.vlc.betav7neon.gui.video","org.videolan.vlc.betav7neon.gui.video.VideoPlayerActivity"));
            startActivity(mediaIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            //Toast.makeText(this,"In Setting",Toast.LENGTH_LONG).show();
            Intent settingsIntent = new Intent(this,SettingsActivity.class);
            settingsIntent.putExtra("Index", defaultIndex);
            startActivity(settingsIntent);
            return true;
        }
        if (id == R.id.action_about) {
            new AlertDialog.Builder(this).setTitle("About Me").setMessage("Product by John Wu \n2014/08")
                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            return;
                        }
                    }).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
