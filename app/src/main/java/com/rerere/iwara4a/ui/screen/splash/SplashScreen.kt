package com.rerere.iwara4a.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rerere.iwara4a.R
import com.rerere.iwara4a.ui.theme.uiBackGroundColor

@Composable
fun SplashScreen(navController: NavController, splashViewModel: SplashViewModel = hiltViewModel()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.uiBackGroundColor), contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null
                )
            }
            Text(text = "IWARA", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground)
            Text(text = "ecchi.iwara.tv", fontSize = 20.sp, color = MaterialTheme.colors.onBackground)
            Spacer(modifier = Modifier.height(120.dp))
            if(splashViewModel.checkingCookkie){
                LinearProgressIndicator(
                    modifier = Modifier.width(120.dp)
                )
                Text(text = "检查登录信息是否过期...")
            }
        }
    }
    LaunchedEffect(splashViewModel.checked, splashViewModel.cookieValid, splashViewModel.checkingCookkie) {
        if(splashViewModel.checked && !splashViewModel.checkingCookkie){
            // 前往主页
            if (splashViewModel.cookieValid) {
                navController.navigate("index") {
                    popUpTo("splash") {
                        inclusive = true
                    }
                }
            } else {
                // 登录
                navController.navigate("login") {
                    popUpTo("splash") {
                        inclusive = true
                    }
                }
            }
        }
    }
}