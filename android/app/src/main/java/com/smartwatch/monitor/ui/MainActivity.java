package com.smartwatch.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.ui.settings.SettingsActivity;

/**
 * 主Activity，包含底部导航栏和Navigation组件
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private FrameLayout btnNotification;
    private View viewUnread;
    private ImageView ivAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigation();
        setupToolbar();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.menu_main);
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }
                return false;
            });

            // 通知铃铛
            btnNotification = toolbar.findViewById(R.id.btn_toolbar_notification);
            viewUnread = toolbar.findViewById(R.id.view_toolbar_unread);
            if (btnNotification != null) {
                btnNotification.setOnClickListener(v -> {
                    if (navController != null) {
                        navController.navigate(R.id.alarmListFragment);
                    }
                });
            }

            // 用户头像
            ivAvatar = toolbar.findViewById(R.id.iv_toolbar_avatar);
            if (ivAvatar != null) {
                ivAvatar.setOnClickListener(v -> {
                    startActivity(new Intent(this, SettingsActivity.class));
                });
            }
        }
    }

    /**
     * 更新通知未读徽章
     * @param hasUnread 是否有未读通知
     */
    public void updateNotificationBadge(boolean hasUnread) {
        if (viewUnread != null) {
            viewUnread.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
}
