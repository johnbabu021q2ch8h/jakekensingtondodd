package com.droideep.indexbar.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.droideep.indexbar.IndexBar;
import com.droideep.indexbar.sample.common.dummydata.Cheeses;

import java.util.List;


public class IndexBarActivity extends AppCompatActivity {

    private ListView mListView;
    private IndexBar mIndexBar;

    private final List<String> dummyData = Cheeses.asList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_bar);

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, dummyData));
        mIndexBar = (IndexBar) findViewById(R.id.index_bar);
        mIndexBar.setSections(sections());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_index_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String[] sections() {
        final int length = 26;
        final String[] sections = new String[length];
        char c = 'A';
        for (int i = 0; i < length; i++) {
            sections[i] = String.valueOf(c++);
        }
        return sections;
    }
}
