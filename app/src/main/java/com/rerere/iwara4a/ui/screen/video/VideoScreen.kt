package com.rerere.iwara4a.ui.screen.video

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.rerere.iwara4a.R
import com.rerere.iwara4a.model.detail.video.VideoDetail
import com.rerere.iwara4a.ui.component.RandomLoadingAnim
import com.rerere.iwara4a.ui.component.basic.Centered
import com.rerere.iwara4a.ui.component.pagerTabIndicatorOffset
import com.rerere.iwara4a.ui.component.player.PlayerController
import com.rerere.iwara4a.ui.component.player.VideoPlayer
import com.rerere.iwara4a.ui.component.player.adaptiveVideoSize
import com.rerere.iwara4a.ui.component.player.rememberPlayerState
import com.rerere.iwara4a.ui.local.LocalDarkMode
import com.rerere.iwara4a.ui.local.LocalPIPMode
import com.rerere.iwara4a.ui.modifier.noRippleClickable
import com.rerere.iwara4a.ui.screen.video.tabs.VideoScreenCommentTab
import com.rerere.iwara4a.ui.screen.video.tabs.VideoScreenDetailTab
import com.rerere.iwara4a.ui.screen.video.tabs.VideoScreenSimilarVideoTab
import com.rerere.iwara4a.ui.states.rememberWindowDpSize
import com.rerere.iwara4a.util.DataState
import com.rerere.iwara4a.util.isActiveNetworkMetered
import com.rerere.iwara4a.util.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rerere.compose_setting.preference.rememberBooleanPreference
import me.rerere.compose_setting.preference.rememberStringPreference

@Composable
fun VideoScreen(
    navController: NavController,
    videoId: String,
    videoViewModel: VideoViewModel = hiltViewModel()
) {
    val videoDetail by videoViewModel.videoDetailState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val darkMode = LocalDarkMode.current

    // 判断视频是否加载了
    fun isVideoLoaded() = videoDetail is DataState.Success

    fun getTitle() = when {
        isVideoLoaded() -> videoDetail.read().title
        videoDetail is DataState.Loading -> context.stringResource(id = R.string.loading)
        videoDetail is DataState.Error -> context.stringResource(id = R.string.load_error)
        else -> context.stringResource(id = R.string.screen_video_title_video_page)
    }

    // 加载视频
    LaunchedEffect(Unit) {
        if (!isVideoLoaded()) {
            videoViewModel.loadVideo(videoId)
        }
    }

    val autoPlayVideo by rememberBooleanPreference(
        key = "setting.autoPlayVideo",
        default = true
    )
    val autoPlayOnWifi by rememberBooleanPreference(
        key = "setting.autoPlayVideoOnWifi",
        default = false
    )

    val playerState = rememberPlayerState {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    val scope = rememberCoroutineScope()
    val windowSize = rememberWindowDpSize()

    // Ensure the statusbar color
    DisposableEffect(Unit) {
        scope.launch {
            delay(500)
            WindowCompat.getInsetsController((context as Activity).window, view).apply {
                isAppearanceLightStatusBars = false
            }
        }
        onDispose {
            WindowCompat.getInsetsController((context as Activity).window, view).apply {
                isAppearanceLightStatusBars = !darkMode
            }
        }
    }

    var videoQuality by rememberStringPreference(
        key = "setting.videoQuality",
        default = "Source"
    )
    val videoLinkState by videoViewModel.videoLink.collectAsState()
    LaunchedEffect(videoLinkState) {
        playerState.handleMediaItem(
            items = videoLinkState
                .readSafely()
                ?.toLinkMap()
                ?.mapValues {
                    MediaItem.fromUri(it.value)
                } ?: emptyMap(),
            autoPlay = if (autoPlayVideo) {
                if (autoPlayOnWifi) {
                    !context.isActiveNetworkMetered
                } else {
                    true
                }
            } else {
                false
            },
            quality = videoQuality
        )
    }

    if (windowSize.width <= windowSize.height) {
        Column {
            VideoPlayer(
                modifier = Modifier
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .adaptiveVideoSize(playerState),
                state = playerState
            ) {
                PlayerController(
                    state = playerState,
                    title = getTitle(),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (playerState.fullScreen.value) {
                                    playerState.exitFullScreen(context as Activity)
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    },
                    onChangeVideoQuality = {
                        videoQuality = it
                    }
                )
            }
            when (videoDetail) {
                is DataState.Empty,
                is DataState.Loading -> {
                    RandomLoadingAnim()
                }
                is DataState.Success -> {
                    if (videoDetail.read() == VideoDetail.PRIVATE) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.screen_video_detail_private),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (videoDetail.read() == VideoDetail.DELETED) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.screen_video_detail_not_found),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            VideoInfo(navController, videoViewModel, videoDetail.read())
                        }
                    }
                }
                is DataState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .noRippleClickable { videoViewModel.loadVideo(videoId) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .padding(10.dp)
                                    .clip(CircleShape)
                            ) {
                                Image(
                                    modifier = Modifier.fillMaxSize(),
                                    painter = painterResource(R.drawable.anime_4),
                                    contentDescription = null
                                )
                            }
                            Text(
                                text = "${stringResource(id = R.string.load_error)}~ （${
                                    stringResource(
                                        id = R.string.screen_video_detail_error_daily_potato
                                    )
                                }）", fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        Row {
            Centered(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black)
            ) {
                VideoPlayer(
                    modifier = Modifier
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .adaptiveVideoSize(playerState),
                    state = playerState
                ) {
                    PlayerController(
                        state = playerState,
                        title = getTitle(),
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (playerState.fullScreen.value) {
                                        playerState.exitFullScreen(context as Activity)
                                    } else {
                                        navController.popBackStack()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        },
                        onChangeVideoQuality = {
                            videoQuality = it
                        }
                    )
                }
            }
            if (!playerState.fullScreen.value && !LocalPIPMode.current) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(Color.Black)
                    )
                    when (videoDetail) {
                        is DataState.Empty,
                        is DataState.Loading -> {
                            RandomLoadingAnim()
                        }
                        is DataState.Success -> {
                            if (videoDetail.read() == VideoDetail.PRIVATE) {
                                Centered(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.screen_video_detail_private),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (videoDetail.read() == VideoDetail.DELETED) {
                                Centered(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.screen_video_detail_not_found),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    VideoInfo(navController, videoViewModel, videoDetail.read())
                                }
                            }
                        }
                        is DataState.Error -> {
                            Centered(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .noRippleClickable { videoViewModel.loadVideo(videoId) }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(160.dp)
                                            .padding(10.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Image(
                                            modifier = Modifier.fillMaxSize(),
                                            painter = painterResource(R.drawable.anime_4),
                                            contentDescription = null
                                        )
                                    }
                                    Text(
                                        text = "${stringResource(id = R.string.load_error)}~ （${
                                            stringResource(
                                                id = R.string.screen_video_detail_error_daily_potato
                                            )
                                        }）", fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoInfo(
    navController: NavController,
    videoViewModel: VideoViewModel,
    videoDetail: VideoDetail
) {
    val pagerState = rememberPagerState(0)
    val coroutineScope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        TabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = pagerState.currentPage,
            indicator = {
                TabRowDefaults.Indicator(Modifier.pagerTabIndicatorOffset(pagerState, it))
            }
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                text = {
                    Text(text = stringResource(R.string.introduction))
                }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                text = {
                    Text(text = stringResource(R.string.comment))
                }
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                },
                text = {
                    Text(text = stringResource(R.string.similar_video))
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize(),
                state = pagerState,
                count = 3
            ) {
                when (it) {
                    0 -> VideoScreenDetailTab(videoViewModel, videoDetail)
                    1 -> VideoScreenCommentTab(navController, videoViewModel)
                    2 -> VideoScreenSimilarVideoTab(videoDetail)
                }
            }
        }
    }
}

