package com.example.anas.techienews;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ArrayAdapter<String> adapter;
    Map<Integer,String> articleTitles= new HashMap<Integer, String>();
    ArrayList<Integer> articleInfo=new ArrayList<Integer>();
    Map<Integer,String> articleUrls=new HashMap<Integer, String>();
    SQLiteDatabase articlesDb;
    public class  gettingNewsList extends AsyncTask<String,Void,String>
    {
        @Override
        protected String doInBackground(String... strings)
        {
            URL urli;
            String result="";
            try
            {
                char ch;

                urli = new URL(strings[0]);
                HttpURLConnection connection=(HttpURLConnection) urli.openConnection();
                connection.connect();
                InputStream is =connection.getInputStream();
                InputStreamReader reader=new InputStreamReader(is);
                int data=reader.read();
                while(data!=-1)
                {

                    data=reader.read();
                    ch=(char)data;
                    result+=ch;

                }
                int count=20;
                articlesDb.execSQL("Delete from articles");
                JSONArray array=new JSONArray("["+result);
                for (int i = 1; i < result.length() && count >= 0; i++)
                {

                    String articleid=array.getString(i);

                    urli=new URL("https://hacker-news.firebaseio.com/v0/item/" + articleid + ".json?print=pretty");
                    String news="";
                    connection=(HttpURLConnection) urli.openConnection();
                    connection.connect();
                    is=connection.getInputStream();
                    reader=new InputStreamReader(is);
                    data=reader.read();
                    while (data!=-1)
                    {
                        data=reader.read();
                        ch=(char)data;
                        news+=ch;
                    }
                    JSONObject jsonobject = new JSONObject("{" + news);



                    if(jsonobject.has("url") && jsonobject.has("title"))
                    {
                        String url = jsonobject.getString("url");
                        String title = jsonobject.getString("title");


                        articleInfo.add(Integer.valueOf(articleid));
                        articleTitles.put(Integer.valueOf(articleid),title);
                        articleUrls.put(Integer.valueOf(articleid),url);

                        String sql=" insert into articles(articleId,url,title) values(?,?,?)  ";
                        SQLiteStatement statement=articlesDb.compileStatement(sql);
                        statement.bindString(1,articleid);
                        statement.bindString(2,url);
                        statement.bindString(3,title);
                        statement.execute();
                        count--;
                    }
                    else
                    {
                    }

                }





            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            UpdateUI();
        }
    }
    ArrayList<String> urls;
    ArrayList<String> newsArrayList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        articlesDb=this.openOrCreateDatabase("articles",Context.MODE_PRIVATE,null);
        ListView newsList =(ListView)findViewById(R.id.listView);
        gettingNewsList newsitems=new gettingNewsList();
        newsArrayList=new ArrayList<String>();
        urls=new ArrayList<>();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, newsArrayList);
        newsList.setAdapter(adapter);
        newsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent in = new Intent(getApplicationContext(), Main2Activity.class);
                in.putExtra("url", urls.get(i));
                startActivity(in);
            }
        });
        articlesDb.execSQL("create table  if not exists  articles (id Integer primary key,articleId int ,url  varchar,title varchar,content varchar )");

        UpdateUI();


            try
            {

                newsitems.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
            }
            catch (Exception e)
            {
                Toast.makeText(getApplicationContext(),"Kindly connect your device to internet",Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

    }

    public void UpdateUI()
    {
        Cursor c =articlesDb.rawQuery("select * from articles order by articleId desc",null);

        int articleidIndex=c.getColumnIndex("articleId");
        int articleTitleIndex=c.getColumnIndex("title");
        int articleUrlIndex=c.getColumnIndex("url");
        c.moveToFirst();
        while(!c.isAfterLast())
        {

            urls.add(c.getString(articleUrlIndex));
            newsArrayList.add(c.getString(articleTitleIndex));
            Log.i("Info-",c.getString(articleidIndex));
            Log.i("title-",c.getString(articleTitleIndex));
            Log.i("url-",c.getString(articleUrlIndex));
            c.moveToNext();
        }
        adapter.notifyDataSetChanged();

    }


}
