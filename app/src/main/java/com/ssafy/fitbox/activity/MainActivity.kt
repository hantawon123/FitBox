package com.ssafy.fitbox.activity

import android.app.AlertDialog
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.ActivityMainBinding
import com.ssafy.fitbox.fragment.AdminDashboardFragment
import com.ssafy.fitbox.fragment.AdminNotificationFragment
import com.ssafy.fitbox.fragment.AccountRecoveryFragment
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.fragment.ChatFragment
import com.ssafy.fitbox.fragment.HomeFragment
import com.ssafy.fitbox.fragment.LoginFragment
import com.ssafy.fitbox.fragment.MyPageFragment
import com.ssafy.fitbox.fragment.NotificationFragment
import com.ssafy.fitbox.fragment.OrderListFragment
import com.ssafy.fitbox.fragment.RegisterFragment
import com.ssafy.fitbox.fragment.SubscriptionOrderFragment
import com.ssafy.fitbox.fragment.WelcomeFragment
import com.ssafy.fitbox.notification.FcmTokenRegistrar
import com.ssafy.fitbox.notification.FitBoxMessagingService
import com.ssafy.fitbox.notification.FitBoxNotificationChannel
import com.ssafy.fitbox.repository.MealRepository
import com.ssafy.fitbox.util.LoginRequiredDialog
import com.ssafy.fitbox.util.MealLocalCache
import com.ssafy.fitbox.viewmodel.CartViewModel
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import com.kakao.sdk.common.util.Utility
import com.ssafy.fitbox.fragment.UserProfileEditFragment

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var sessionManager: SessionManager

    private var exitDialog: AlertDialog? = null
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                FcmTokenRegistrar.registerCurrentUser(applicationContext)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        Log.d("KAKAO_KEY_HASH", Utility.getKeyHash(this))

        sessionManager = SessionManager(this)
        if ((sessionManager.getUser()?.id ?: 1) <= 0) {
            sessionManager.clearSession()
        }
        FitBoxNotificationChannel.create(applicationContext)
        requestNotificationPermission()
        FcmTokenRegistrar.registerCurrentUser(applicationContext)
        MealLocalCache.initialize(this)
        lifecycleScope.launch {
            MealRepository().warmHomeCache(sessionManager.getUser()?.id)
        }

        setBottomNavigationView()
        setupBackPressedHandler()
        setupFragmentLifecycleCallback()

        if (savedInstanceState == null) {
            if (sessionManager.getUser()?.purpose.equals("SELLER", true)) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, AdminDashboardFragment())
                    .commit()
            } else {
                binding.bottomNavigationView.selectedItemId = R.id.fragment_home
            }
        }
        openNotificationsIfRequested(intent)
        hideStartupLoadingAfterDelay()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openNotificationsIfRequested(intent)
    }

    override fun onStart() {
        super.onStart()
        FcmTokenRegistrar.registerCurrentUser(applicationContext)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hideStartupLoadingAfterDelay() {
        lifecycleScope.launch {
            delay(850L)
            binding.layoutStartupLoading.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction {
                    binding.layoutStartupLoading.visibility = View.GONE
                    binding.layoutStartupLoading.alpha = 1f
                }
                .start()
        }
    }

    private fun openNotificationsIfRequested(intent: Intent?) {
        if (intent?.getBooleanExtra(FitBoxMessagingService.EXTRA_OPEN_NOTIFICATIONS, false) != true) {
            return
        }
        intent.removeExtra(FitBoxMessagingService.EXTRA_OPEN_NOTIFICATIONS)
        val destination =
            if (sessionManager.getUser()?.purpose.equals("SELLER", true)) {
                AdminNotificationFragment()
            } else {
                NotificationFragment()
            }
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, destination)
            .addToBackStack(null)
            .commit()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentFragment = getCurrentFragment()

                    if (currentFragment is HomeFragment ||
                        currentFragment is AdminDashboardFragment
                    ) {
                        showExitDialog()
                        return
                    }

                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                        return
                    }

                    moveToHomeFragment()
                }
            }
        )
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.main_container)
    }

    private fun moveToHomeFragment() {
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        if (binding.bottomNavigationView.selectedItemId != R.id.fragment_home) {
            binding.bottomNavigationView.selectedItemId = R.id.fragment_home
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, HomeFragment())
                .commit()
        }
    }

    fun moveToOrderListFragment() {
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        binding.bottomNavigationView.visibility = View.VISIBLE

        if (binding.bottomNavigationView.selectedItemId != R.id.fragment_orderList) {
            binding.bottomNavigationView.selectedItemId = R.id.fragment_orderList
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, OrderListFragment())
                .commit()
        }
    }

    private fun showExitDialog() {
        if (exitDialog?.isShowing == true) {
            return
        }

        exitDialog = AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                finish()
            }
            .setNegativeButton("아니오", null)
            .create()

        exitDialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP
            ) {
                finish()
                true
            } else {
                false
            }
        }

        exitDialog?.show()
    }

    private fun setupFragmentLifecycleCallback() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    v: View,
                    savedInstanceState: Bundle?
                ) {
                    super.onFragmentViewCreated(fm, f, v, savedInstanceState)

                    when (f) {
                        is AccountRecoveryFragment,
                        is LoginFragment,
                        is RegisterFragment,
                        is WelcomeFragment,
                        is AdminDashboardFragment,
                        is AdminNotificationFragment -> {
                            binding.bottomNavigationView.visibility = View.GONE
                        }

                        else -> {
                            binding.bottomNavigationView.visibility = View.VISIBLE
                        }
                    }
                }
            },
            true
        )
    }

    fun showWelcomeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, WelcomeFragment())
            .addToBackStack(null)
            .commit()
    }

    fun onLoginSuccess(user: User? = sessionManager.getUser()) {
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        if (user?.purpose.equals("SELLER", true)) {
            binding.bottomNavigationView.visibility = View.GONE
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, AdminDashboardFragment())
                .commit()
        } else {
            binding.bottomNavigationView.visibility = View.VISIBLE
            if (binding.bottomNavigationView.selectedItemId != R.id.fragment_myPage) {
                binding.bottomNavigationView.selectedItemId = R.id.fragment_myPage
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, MyPageFragment())
                    .commit()
            }
        }
    }

    fun onKakaoLoginSuccess() {
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        binding.bottomNavigationView.selectedItemId = R.id.fragment_myPage

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                UserProfileEditFragment.newKakaoLoginInstance()
            )
            .commit()
    }

    fun onLogoutSuccess() {
        sessionManager.clearSession()
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        binding.bottomNavigationView.visibility = View.VISIBLE

        if (binding.bottomNavigationView.selectedItemId != R.id.fragment_myPage) {
            binding.bottomNavigationView.selectedItemId = R.id.fragment_myPage
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, MyPageFragment())
                .commit()
        }
    }

    fun setBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, HomeFragment())
                        .commit()
                    true
                }

                R.id.fragment_chat -> {
                    if (!sessionManager.isLoggedIn()) {
                        LoginRequiredDialog.show(this)
                        return@setOnItemSelectedListener false
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, ChatFragment())
                        .commit()
                    true
                }

                R.id.fragment_subscription -> {
                    if (!sessionManager.isLoggedIn()) {
                        LoginRequiredDialog.show(this)
                        return@setOnItemSelectedListener false
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, SubscriptionOrderFragment())
                        .commit()
                    true
                }

                R.id.fragment_orderList -> {
                    if (!sessionManager.isLoggedIn()) {
                        LoginRequiredDialog.show(this)
                        return@setOnItemSelectedListener false
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, OrderListFragment())
                        .commit()
                    true
                }

                R.id.fragment_myPage -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, MyPageFragment())
                        .commit()
                    true
                }

                else -> false
            }
        }
    }

    fun onKakaoLoginNeedsProfileEdit() {
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                UserProfileEditFragment.newKakaoLoginInstance()
            )
            .commit()
    }

    fun onKakaoLoginNeedsRegistration() {
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        binding.bottomNavigationView.visibility = View.GONE

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                RegisterFragment.newKakaoLoginInstance()
            )
            .commit()
    }

    fun onKakaoLoginSuccessGoHome() {
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.bottomNavigationView.selectedItemId = R.id.fragment_home
    }

    override fun onDestroy() {
        exitDialog?.dismiss()
        exitDialog = null
        super.onDestroy()
    }
}
