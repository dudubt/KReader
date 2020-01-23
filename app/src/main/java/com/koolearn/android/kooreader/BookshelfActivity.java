package com.koolearn.android.kooreader;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import com.koolearn.android.kooreader.fragment.LocalBookAdapter;
import com.koolearn.android.kooreader.library.LibraryActivity;
import com.koolearn.android.kooreader.libraryService.BookCollectionShadow;
import com.koolearn.android.util.OrientationUtil;
import com.koolearn.klibrary.ui.android.R;
import com.koolearn.kooreader.book.Book;

import java.util.ArrayList;
import java.util.List;

public class BookshelfActivity extends AppCompatActivity implements RecyclerItemClickListener.OnItemClickListener {

    private RecyclerView mRecyclerView;

    private List<Book> bookshelf = new ArrayList<>();
    private LocalBookAdapter mLocalBookAdapter;
    private final BookCollectionShadow bookCollectionShadow = new BookCollectionShadow();

    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_shelf);
        activity = this;

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(activity, mRecyclerView, this));

        mLocalBookAdapter = new LocalBookAdapter(activity);
        mRecyclerView.setAdapter(mLocalBookAdapter);

        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OrientationUtil.startActivity(activity, new Intent(activity, LibraryActivity.class));
            }
        });
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission_group.STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 111);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getBooks();
    }

    private void getBooks() {
        bookCollectionShadow.bindToService(activity, new Runnable() {
            public void run() {
                bookshelf.clear();
                bookshelf = bookCollectionShadow.recentlyOpenedBooks(15);
                mLocalBookAdapter.clearItems();
                mLocalBookAdapter.updateItems(bookshelf, false);
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {
        ReaderActivity.openBookActivity(activity, mLocalBookAdapter.getBook(position), null);
    }

    @Override
    public void onItemLongClick(final View view, final int position) {
        new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_error_outline_black)
                .setTitle("删除书籍?")
                .setNeutralButton("稍后", null)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        bookCollectionShadow.removeBook(mLocalBookAdapter.getBook(position), true);
                        mLocalBookAdapter.removeItems(position);

                    }
                }).show();
    }

    @Override
    public void onDestroy() {
        bookCollectionShadow.unbind();
        super.onDestroy();
    }
}
