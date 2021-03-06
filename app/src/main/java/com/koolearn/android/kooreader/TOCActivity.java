package com.koolearn.android.kooreader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.koolearn.android.kooreader.api.KooReaderIntents;
import com.koolearn.android.kooreader.bookmark.EditBookmarkActivity;
import com.koolearn.android.kooreader.libraryService.BookCollectionShadow;
import com.koolearn.android.util.OrientationUtil;
import com.koolearn.android.util.UIMessageUtil;
import com.koolearn.android.util.ViewUtil;
import com.koolearn.klibrary.core.application.ZLApplication;
import com.koolearn.klibrary.core.tree.ZLTree;
import com.koolearn.klibrary.core.util.ZLColor;
import com.koolearn.klibrary.ui.android.R;
import com.koolearn.klibrary.ui.android.library.UncaughtExceptionHandler;
import com.koolearn.klibrary.ui.android.util.ZLAndroidColorUtil;
import com.koolearn.kooreader.book.Book;
import com.koolearn.kooreader.book.BookEvent;
import com.koolearn.kooreader.book.Bookmark;
import com.koolearn.kooreader.book.BookmarkQuery;
import com.koolearn.kooreader.book.HighlightingStyle;
import com.koolearn.kooreader.book.IBookCollection;
import com.koolearn.kooreader.bookmodel.TOCTree;
import com.koolearn.kooreader.kooreader.KooReaderApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView;

public class TOCActivity extends Activity implements IBookCollection.Listener<Book> {

    private List<String> mTitleList = new ArrayList<>(); // 页卡标题集合
    private List<View> mViewList = new ArrayList<>(); // 页卡视图集合

    private static final int OPEN_ITEM_ID = 0;
    private static final int EDIT_ITEM_ID = 1;
    private static final int DELETE_ITEM_ID = 2;

    private final Map<Integer, HighlightingStyle> myStyles =
            Collections.synchronizedMap(new HashMap<Integer, HighlightingStyle>());

    private final BookCollectionShadow myCollection = new BookCollectionShadow();
    private volatile Book myBook;
    private volatile Bookmark myBookmark;

    private final Comparator<Bookmark> myComparator = new Bookmark.ByTimeComparator();

    private volatile BookmarksAdapter myThisBookAdapter;
    private ListView thisBookListView;
    private String bgValue;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        setContentView(R.layout.listview_toc);
        TextView tvBook = (TextView) findViewById(R.id.tv_book);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.vp_view);
        TabLayout mTabLayout = (TabLayout) findViewById(R.id.tabs);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View tocView = mInflater.inflate(R.layout.list_toc, null);
        final ListView tocListView = (ListView) tocView.findViewById(R.id.listView);


        final KooReaderApp kooreader = (KooReaderApp) ZLApplication.Instance();
        final TOCTree root = kooreader.Model.TOCTree;
        tvBook.setText(kooreader.getCurrentBook().getTitle());

        final TOCAdapter tocAdapter = new TOCAdapter(tocListView, root);
        TOCTree treeToSelect = kooreader.getCurrentTOCElement();
        tocAdapter.selectItem(treeToSelect); // 设置当前位置

        View bookmarkView = mInflater.inflate(R.layout.list_bookmark, null);
        thisBookListView = (ListView) bookmarkView.findViewById(R.id.bookmark_this_book);

        myBook = KooReaderIntents.getBookExtra(getIntent(), myCollection);
        myBookmark = KooReaderIntents.getBookmarkExtra(getIntent());

        myCollection.bindToService(this, new Runnable() {
            public void run() {
                myThisBookAdapter =
                        new BookmarksAdapter(thisBookListView, myBookmark != null);
                myCollection.addListener(TOCActivity.this);
                updateStyles();
                loadBookmarks();
            }
        });

        // 页卡视图
        View noteView = mInflater.inflate(R.layout.list_note, null);

        /**
         * 设置背景与阅读背景一致
         */
        bgValue = kooreader.ViewOptions.getColorProfile().WallpaperOption.getValue();

        //添加页卡视图
        mViewList.add(tocView);
        mViewList.add(bookmarkView);
        mViewList.add(noteView);
        //添加页卡标题
        mTitleList.add("目录");
        mTitleList.add("笔记");
        mTitleList.add("书签");

        mTabLayout.setTabMode(TabLayout.MODE_FIXED);//设置tab模式，当前为系统默认模式
        mTabLayout.addTab(mTabLayout.newTab().setText(mTitleList.get(0)));//添加tab选项卡
        mTabLayout.addTab(mTabLayout.newTab().setText(mTitleList.get(1)));
        mTabLayout.addTab(mTabLayout.newTab().setText(mTitleList.get(2)));

        MyPagerAdapter mAdapter = new MyPagerAdapter(mViewList);
        mViewPager.setAdapter(mAdapter); // 给ViewPager设置适配器
        mTabLayout.setupWithViewPager(mViewPager); // 将TabLayout和ViewPager关联起来。
        mTabLayout.setTabsFromPagerAdapter(mAdapter); // 给Tabs设置适配器

        findViewById(R.id.btn_prev_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tocAdapter.prevPage();
            }
        });

        findViewById(R.id.btn_next_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tocAdapter.nextPage();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        OrientationUtil.setOrientation(this, getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        OrientationUtil.setOrientation(this, intent);
    }
    
    private final class TOCAdapter extends ZLTreeAdapter {
        TOCAdapter(ListView listView, TOCTree root) {
            super(listView, root);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = (convertView != null) ? convertView : LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.toc_tree_item, parent, false);
            final TOCTree tree = (TOCTree) getItem(position);
    
            TextView textView = ViewUtil.findTextView(view, R.id.toc_tree_item_text);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
            int leftMargin = dp2px(TOCActivity.this, 25);
            lp.leftMargin = leftMargin * tree.level;
    
            String flag = "•  ";
            if (tree.hasChildren()) {
                flag = isOpen(tree) ? "▼  " : "▶︎  ";
            }
            
            textView.setText(flag + tree.getText());
            return view;
        }

        void openBookText(TOCTree tree) {
            final TOCTree.Reference reference = tree.getReference();
            if (reference != null) {
                finish();
                final KooReaderApp kooreader = (KooReaderApp) ZLApplication.Instance();
                kooreader.addInvisibleBookmark();
                kooreader.readerView.gotoPosition(reference.ParagraphIndex, 0, 0);
                kooreader.showBookTextView();
                kooreader.storePosition();
            }
        }

        @Override
        protected boolean runTreeItem(ZLTree<?> tree) {
            if (super.runTreeItem(tree)) {
                return true;
            }
            openBookText((TOCTree) tree);
            return true;
        }
    }


    //ViewPager适配器
    class MyPagerAdapter extends PagerAdapter {
        private List<View> mViewList;

        public MyPagerAdapter(List<View> mViewList) {
            this.mViewList = mViewList;
        }

        @Override
        public int getCount() {
            return mViewList.size();//页卡数
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;//官方推荐写法
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mViewList.get(position));//添加页卡
            return mViewList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mViewList.get(position));//删除页卡
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitleList.get(position);//页卡标题
        }

    }


    private void updateStyles() {
        synchronized (myStyles) {
            myStyles.clear();
            for (HighlightingStyle style : myCollection.highlightingStyles()) {
                myStyles.put(style.Id, style);
            }
        }
    }

    private final Object myBookmarksLock = new Object();

    private void loadBookmarks() {
        new Thread(new Runnable() {
            public void run() {
                synchronized (myBookmarksLock) {
                    for (BookmarkQuery query = new BookmarkQuery(myBook, 50); ; query = query.next()) {
                        final List<Bookmark> thisBookBookmarks = myCollection.bookmarks(query);
                        if (thisBookBookmarks.isEmpty()) {
                            break;
                        }
                        myThisBookAdapter.addAll(thisBookBookmarks);
//                        myAllBooksAdapter.addAll(thisBookBookmarks);
                    }
//                    for (BookmarkQuery query = new BookmarkQuery(50); ; query = query.next()) {
//                        final List<Bookmark> allBookmarks = myCollection.bookmarks(query);
//                        if (allBookmarks.isEmpty()) {
//                            break;
//                        }
//                        myAllBooksAdapter.addAll(allBookmarks);
//                    }
                }
            }
        }).start();
    }

    private void updateBookmarks(final Book book) {
        new Thread(new Runnable() {
            public void run() {
                synchronized (myBookmarksLock) {
                    final boolean flagThisBookTab = book.getId() == myBook.getId();

                    final Map<String, Bookmark> oldBookmarks = new HashMap<String, Bookmark>();
                    if (flagThisBookTab) {
                        for (Bookmark b : myThisBookAdapter.bookmarks()) {
                            oldBookmarks.put(b.Uid, b);
                        }
                    }

                    for (BookmarkQuery query = new BookmarkQuery(book, 50); ; query = query.next()) {
                        final List<Bookmark> loaded = myCollection.bookmarks(query);
                        if (loaded.isEmpty()) {
                            break;
                        }
                        for (Bookmark b : loaded) {
                            final Bookmark old = oldBookmarks.remove(b.Uid);
//                            myAllBooksAdapter.replace(old, b);
                            if (flagThisBookTab) {
                                myThisBookAdapter.replace(old, b);
                            }
                        }
                    }
                    if (flagThisBookTab) {
                        myThisBookAdapter.removeAll(oldBookmarks.values());
                    }
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        myCollection.unbind();
        super.onDestroy();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        final BookmarksAdapter adapter = myThisBookAdapter;

        final Bookmark bookmark = adapter.getItem(position);
        switch (item.getItemId()) {
            case OPEN_ITEM_ID:
                gotoBookmark(bookmark);
                return true;
            case EDIT_ITEM_ID:
                final Intent intent = new Intent(this, EditBookmarkActivity.class);
                intent.putExtra("bgColor", bgValue);
                KooReaderIntents.putBookmarkExtra(intent, bookmark);
                OrientationUtil.startActivity(this, intent);
                return true;
            case DELETE_ITEM_ID:
                myCollection.deleteBookmark(bookmark);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void gotoBookmark(Bookmark bookmark) {
        bookmark.markAsAccessed();
        myCollection.saveBookmark(bookmark);
        final Book book = myCollection.getBookById(bookmark.BookId);
        if (book != null) {
            ReaderActivity.openBookActivity(this, book, bookmark);
        } else {
            UIMessageUtil.showErrorMessage(this, "cannotOpenBook");
        }
    }

    private final class BookmarksAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, View.OnCreateContextMenuListener {
        private final List<Bookmark> myBookmarksList =
                Collections.synchronizedList(new LinkedList<Bookmark>());
        private volatile boolean myShowAddBookmarkItem;

        BookmarksAdapter(ListView listView, boolean showAddBookmarkItem) {
            myShowAddBookmarkItem = showAddBookmarkItem;
            listView.setAdapter(this);
            listView.setOnItemClickListener(this);
            listView.setOnCreateContextMenuListener(this);
        }

        public List<Bookmark> bookmarks() {
            return Collections.unmodifiableList(myBookmarksList);
        }

        public void addAll(final List<Bookmark> bookmarks) {
            runOnUiThread(new Runnable() {
                public void run() {
                    synchronized (myBookmarksList) {
                        for (Bookmark b : bookmarks) {
                            final int position = Collections.binarySearch(myBookmarksList, b, myComparator);
                            if (position < 0) {
                                myBookmarksList.add(-position - 1, b);
                            }
                        }
                    }
                    notifyDataSetChanged();
                }
            });
        }

        private boolean areEqualsForView(Bookmark b0, Bookmark b1) {
            return
                    b0.getStyleId() == b1.getStyleId() &&
                            b0.getText().equals(b1.getText()) &&
                            b0.getTimestamp(Bookmark.DateType.Latest).equals(b1.getTimestamp(Bookmark.DateType.Latest));
        }

        public void replace(final Bookmark old, final Bookmark b) {
            if (old != null && areEqualsForView(old, b)) {
                return;
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    synchronized (myBookmarksList) {
                        if (old != null) {
                            myBookmarksList.remove(old);
                        }
                        final int position = Collections.binarySearch(myBookmarksList, b, myComparator);
                        if (position < 0) {
                            myBookmarksList.add(-position - 1, b);
                        }
                    }
                    notifyDataSetChanged();
                }
            });
        }

        public void removeAll(final Collection<Bookmark> bookmarks) {
            if (bookmarks.isEmpty()) {
                return;
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    myBookmarksList.removeAll(bookmarks);
                    notifyDataSetChanged();
                }
            });
        }

        public void clear() {
            runOnUiThread(new Runnable() {
                public void run() {
                    myBookmarksList.clear();
                    notifyDataSetChanged();
                }
            });
        }

        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
            final int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
            if (getItem(position) != null) {
                menu.add(0, OPEN_ITEM_ID, 0, "打开笔记");
                menu.add(0, EDIT_ITEM_ID, 0, "编辑笔记");
                menu.add(0, DELETE_ITEM_ID, 0, "删除笔记");
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = (convertView != null) ? convertView :
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_item, parent, false);
            final ImageView imageView = ViewUtil.findImageView(view, R.id.bookmark_item_icon);
            final View colorContainer = ViewUtil.findView(view, R.id.bookmark_item_color_container);
            final AmbilWarnaPrefWidgetView colorView =
                    (AmbilWarnaPrefWidgetView) ViewUtil.findView(view, R.id.bookmark_item_color);
            final TextView textView = ViewUtil.findTextView(view, R.id.bookmark_item_text);
            final TextView bookTitleView = ViewUtil.findTextView(view, R.id.bookmark_item_booktitle);

            final Bookmark bookmark = getItem(position);
            if (bookmark == null) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageResource(R.drawable.ic_list_plus);
                colorContainer.setVisibility(View.GONE);
                textView.setText("添加笔记");
                bookTitleView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.GONE);
                colorContainer.setVisibility(View.VISIBLE);
                setupColorView(colorView, myStyles.get(bookmark.getStyleId()));
                textView.setText(bookmark.getText());
                if (myShowAddBookmarkItem) {
                    bookTitleView.setVisibility(View.GONE);
                } else {
                    bookTitleView.setVisibility(View.VISIBLE);
                    bookTitleView.setText(bookmark.BookTitle);
                }
            }
            return view;
        }

        @Override
        public final boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public final boolean isEnabled(int position) {
            return true;
        }

        @Override
        public final long getItemId(int position) {
            final Bookmark item = getItem(position);
            return item != null ? item.getId() : -1;
        }

        @Override
        public final Bookmark getItem(int position) {
            if (myShowAddBookmarkItem) {
                --position;
            }
            return position >= 0 ? myBookmarksList.get(position) : null;
        }

        @Override
        public final int getCount() {
            return myShowAddBookmarkItem ? myBookmarksList.size() + 1 : myBookmarksList.size();
        }

        public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Bookmark bookmark = getItem(position);
            if (bookmark != null) {
                gotoBookmark(bookmark);
            } else if (myShowAddBookmarkItem) {
                myShowAddBookmarkItem = false;
                myCollection.saveBookmark(myBookmark);
            }
        }
    }

    // method from IBookCollection.Listener
    public void onBookEvent(BookEvent event, Book book) {
        switch (event) {
            default:
                break;
            case BookmarkStyleChanged:
                runOnUiThread(new Runnable() {
                    public void run() {
                        updateStyles();
                        myThisBookAdapter.notifyDataSetChanged();
                    }
                });
                break;
            case BookmarksUpdated:
                updateBookmarks(book);
                break;
        }
    }

    // method from IBookCollection.Listener
    public void onBuildEvent(IBookCollection.Status status) {
    }

    static void setupColorView(AmbilWarnaPrefWidgetView colorView, HighlightingStyle style) {
        Integer rgb = null;
        if (style != null) {
            final ZLColor color = style.getBackgroundColor();
            if (color != null) {
                rgb = ZLAndroidColorUtil.rgb(color);
            }
        }

        if (rgb != null) {
            colorView.setBackgroundColor(rgb);
        } else {
            colorView.setBackgroundColor(0);
        }
    }
    
    public static int dp2px(Context context, float dip) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (dip * displayMetrics.density + 0.5f);
    }
}
