package com.example.newsapp.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.NewsActivity
import com.example.newsapp.R
import com.example.newsapp.adapters.NewsAdapter
import com.example.newsapp.util.Constants
import com.example.newsapp.util.Constants.Companion.DURATION
import com.example.newsapp.util.Resource
import com.example.newsapp.viewModel.NewsViewModel
import kotlinx.android.synthetic.main.fragment_breaking_news.*
import kotlinx.android.synthetic.main.fragment_search_news.*
import kotlinx.android.synthetic.main.fragment_search_news.paginationProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchNewsFragment : Fragment() {


    lateinit var newsViewModel:NewsViewModel
    lateinit var newsAdapter: NewsAdapter
    val TAG ="Search News Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsViewModel = (activity as NewsActivity).newsViewModel
        setupRecyclerView()


        newsAdapter.setOnItemClickListener {
            val bundle = Bundle().apply {
                putSerializable("article",it)
            }
            findNavController().navigate(R.id.action_searchNewsFragment_to_articleFragment,bundle)
        }

        etSearch.addTextChangedListener{
            editable->
            GlobalScope.launch {
                delay(DURATION)
                editable?.let {
                    if(editable.toString().isNotEmpty()){
                        newsViewModel.searchNews(editable.toString())
                    }
                }
            }

        }

        newsViewModel.searchNews.observe(viewLifecycleOwner, Observer {
                response->
            when(response){
                is Resource.Success->{
                    hideProgressBar()
                    response.data?.let {
                            newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles)
                        val totalPages = newsResponse.totalResults / Constants.QUERY_PAGE_SIZE + 2
                        isLastPage = newsViewModel.breakingNewsPage ==totalPages

                        if(isLastPage){
                            rvSearchNews.setPadding(0,0,0,0)
                        }
                    }
                }
                is Resource.Error ->{
                    hideProgressBar()
                    response.message?.let { message ->
                        Log.e(TAG, "An error: $message")
                    }
                }
                is Resource.Loading ->{
                    showProgressBar()
                }
            }
        })
    }

    var isLoading = false
    var isLastPage = false
    var isScrolling = false

    val scrollListner = object : RecyclerView.OnScrollListener(){

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalIteamCount = layoutManager.itemCount

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalIteamCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalIteamCount >= Constants.QUERY_PAGE_SIZE

            val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning
                    &&isTotalMoreThanVisible && isScrolling

            if(shouldPaginate){
                newsViewModel.searchNews(etSearch.text.toString())
                isScrolling = false
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                isScrolling = true
            }
        }
    }


    private fun setupRecyclerView(){
        newsAdapter= NewsAdapter()
        rvSearchNews.apply {
            adapter= newsAdapter
            addOnScrollListener(scrollListner)
        }
    }

    private fun hideProgressBar(){
        paginationProgressBar.visibility = View.GONE
        isLoading = false
    }

    private fun showProgressBar(){
        paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }
}