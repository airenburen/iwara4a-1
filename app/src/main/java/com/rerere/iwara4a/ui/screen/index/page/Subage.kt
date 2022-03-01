package com.rerere.iwara4a.ui.screen.index.page

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.rerere.iwara4a.ui.component.MediaPreviewCard
import com.rerere.iwara4a.ui.component.PageList
import com.rerere.iwara4a.ui.component.rememberPageListPage
import com.rerere.iwara4a.ui.screen.index.IndexViewModel

@Composable
fun SubPage(navController: NavController, indexViewModel: IndexViewModel) {
    val pageListState = rememberPageListPage()
    PageList(
        state = pageListState,
        provider = indexViewModel.subscriptionsProvider
    ) {
        MediaPreviewCard(navController, it)
    }
}