package com.ttwishing.stretchablelistview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import com.ttwishing.stretchablelistview.library.StretchableListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StretchableListView listView = (StretchableListView) findViewById(R.id.list_view);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, getData());
        listView.setAdapter(adapter);

        View headerView = LayoutInflater.from(this).inflate(R.layout.list_header, null);
        listView.addHeaderView(headerView);
        if (headerView instanceof StretchableListView.StretchListener) {
            stretchListeners.add((StretchableListView.StretchListener) headerView);
        }

        View footerView = LayoutInflater.from(this).inflate(R.layout.list_footer, null);
        listView.addFooterView(footerView);
        if (footerView instanceof StretchableListView.StretchListener) {
            stretchListeners.add((StretchableListView.StretchListener) footerView);
        }

        setObservableListView(listView);
    }

    private List<String> getData() {

        List<String> data = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            data.add("item-" + i);
        }

        return data;
    }


    private final List<StretchableListView.StretchListener> stretchListeners = new CopyOnWriteArrayList();

    public void setObservableListView(StretchableListView listView) {
        listView.setStretchListener(new StretchableListView.StretchListener() {

            @Override
            public void onStretchHeightChanged(StretchableListView observableListView, int oldHeight, int height, boolean paramBoolean) {
                for (StretchableListView.StretchListener listener : stretchListeners) {
                    listener.onStretchHeightChanged(observableListView, oldHeight, height, paramBoolean);
                }
            }

            @Override
            public void onStretchReleaseComplete(StretchableListView observableListView, int height, boolean paramBoolean) {
                for (StretchableListView.StretchListener listener : stretchListeners) {
                    listener.onStretchReleaseComplete(observableListView, height, paramBoolean);
                }
            }

            @Override
            public void onStretchStart(StretchableListView observableListView, int paramInt1, int paramInt2, boolean paramBoolean) {
                for (StretchableListView.StretchListener listener : stretchListeners) {
                    listener.onStretchStart(observableListView, paramInt1, paramInt2, paramBoolean);
                }
            }

            @Override
            public void onStretchReleaseStart(StretchableListView observableListView, int height, boolean paramBoolean) {
                for (StretchableListView.StretchListener listener : stretchListeners) {
                    listener.onStretchReleaseStart(observableListView, height, paramBoolean);
                }
            }
        });
    }
}
