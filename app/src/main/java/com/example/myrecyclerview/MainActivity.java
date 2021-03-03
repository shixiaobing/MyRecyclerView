package com.example.myrecyclerview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MyRecyclerView myRecyclerView;
    private int count = 5000000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myRecyclerView = findViewById(R.id.table);
        myRecyclerView.setAdapter(new MyAdapter(this));

        // jump RecyclerView source code
        RecyclerView recyclerView = new RecyclerView(this);
    }

    class MyAdapter implements MyRecyclerView.Adapter {
        private final LayoutInflater inflater;
        private final int height;

        public MyAdapter(Context context) {
            Resources resources = context.getResources();
            height = resources.getDimensionPixelOffset(R.dimen.item_height);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (position % 2 == 0) {
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.item_table, parent, false);

                }
                TextView textView = convertView.findViewById(R.id.item_text);
                textView.setText("第" + position + "行");
            }
            else {
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.item_table2, parent, false);
                }
            }
            Log.i(TAG, "onCreateViewHolder:" + convertView.hashCode());
            return convertView;

        }


        @Override
        public int getItemViewType(int row) {
            if (row % 2 == 0) return 0;
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemCount() {
            return 30;
        }

        @Override
        public int getHeight(int index) {
            return height;
        }
    }
}