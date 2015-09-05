package zhexian.learn.cnblogs.news;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

import zhexian.learn.cnblogs.base.BaseActivity;
import zhexian.learn.cnblogs.base.BaseSwipeListFragment;
import zhexian.learn.cnblogs.image.ZImage;
import zhexian.learn.cnblogs.lib.ZDate;
import zhexian.learn.cnblogs.ui.TabActionBarView;
import zhexian.learn.cnblogs.util.ConfigConstant;
import zhexian.learn.cnblogs.util.DBHelper;


/**
 * 新闻列表的UI
 */
public class NewsListFragment extends BaseSwipeListFragment<NewsListEntity> implements TabActionBarView.ITabActionCallback {

    private ConfigConstant.NewsCategory mCategory;

    public static NewsListFragment newInstance() {
        return new NewsListFragment();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabActionBarView actionBarView = new TabActionBarView((BaseActivity) getActivity());
        actionBarView.bindTab(this, "精选", "最新");
    }

    @Override
    protected RecyclerView.Adapter<RecyclerView.ViewHolder> bindArrayAdapter(List<NewsListEntity> list) {
        return new NewsListAdapter(mBaseActivity, list);
    }

    @Override
    protected List<NewsListEntity> loadData(int pageIndex, int pageSize) {
        List<NewsListEntity> list = NewsDal.getNewsList(mBaseApp, mCategory, pageIndex, pageSize);

        if (mBaseApp == null)
            return null;

        if (list != null && mCategory == ConfigConstant.NewsCategory.Recommend && mBaseApp.isNetworkWifi() && mBaseApp.isAutoLoadRecommend())
            new AsyncCacheNews().execute(list);

        return list;
    }

    @Override
    protected NewsListEntity getLoadMorePlaceHolder() {
        NewsListEntity entity = new NewsListEntity();
        entity.setEntityType(ConfigConstant.ENTITY_TYPE_LOAD_MORE_PLACE_HOLDER);
        return entity;
    }

    @Override
    public void onLeftTabClick() {
        mCategory = ConfigConstant.NewsCategory.Recommend;
        onRefresh();
    }

    @Override
    public void onMiddleTabClick() {

    }

    @Override
    public void onRightClick() {
        mCategory = ConfigConstant.NewsCategory.Recent;
        onRefresh();
    }

    private class AsyncCacheNews extends AsyncTask<List<NewsListEntity>, Void, Void> {

        @Override
        protected Void doInBackground(List<NewsListEntity>... lists) {
            List<NewsListEntity> list = lists[0];

            for (NewsListEntity entity : list) {

                if (mBaseApp == null || mBaseApp.isNetworkWifi() == false)
                    break;

                if (entity == null)
                    continue;


                String key = String.format("news_content_%d", entity.getNewsID());

                if (DBHelper.cache().exist(key))
                    continue;

                ZImage.ready().want(entity.getIconUrl()).lowPriority().save();

                //自动缓存三天内的新闻
                boolean isNeedCache = entity.getPublishDate().equals(ZDate.TODAY_STRING) || entity.getPublishDate().equals(ZDate.YESTERDAY_STRING);

                if (isNeedCache)
                    NewsDal.CacheNews(entity.getNewsID(), key);
            }
            return null;
        }
    }
}
