package com.example.smartnews.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnews.R
import com.example.smartnews.bd.DatabaseHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.example.smartnews.auth.RegisterActivity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1
    private lateinit var adContainer: FrameLayout
    private lateinit var adView: AdView
    private lateinit var ivCloseAd: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adRequest: AdRequest
    private val showAdRunnable = Runnable {
        adContainer.visibility = View.VISIBLE
        adView.loadAd(adRequest)
    }
    private val sharedPref by lazy { getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) }

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
        Log.d("ProfileActivity", "attachBaseContext: Language set to $language")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e("ProfileActivity", "Invalid USER_ID, finishing activity")
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvLanguage = findViewById<TextView>(R.id.tvLanguage)
        val tvCurrency = findViewById<TextView>(R.id.tvCurrency)
        val btnEditName = findViewById<ImageButton>(R.id.btnEditName)
        val btnEditEmail = findViewById<ImageButton>(R.id.btnEditEmail)
        val btnEditLanguage = findViewById<ImageButton>(R.id.btnEditLanguage)
        val btnEditCurrency = findViewById<ImageButton>(R.id.btnEditCurrency)
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)
        val btnBuyVip = findViewById<Button>(R.id.btnBuyVip)

        refreshUI()

        btnEditName.setOnClickListener { startEditActivity("name") }
        btnEditEmail.setOnClickListener { startEditActivity("email") }
        btnEditLanguage.setOnClickListener { startEditActivity("language") }
        btnEditCurrency.setOnClickListener { startEditActivity("currency") }

        btnDeleteAccount.setOnClickListener {
            val existingUser = dbHelper.getUser()
            if (existingUser != null) {
                lifecycleScope.launch {
                    dbHelper.deleteUser(existingUser.id)
                    with(sharedPref.edit()) {
                        clear()
                        apply()
                    }
                    showCustomDialog(
                        getString(R.string.success_title),
                        getString(R.string.success_deleted_desc),
                        R.layout.custom_dialog_success
                    ) {
                        val intent = Intent(this@ProfileActivity, RegisterActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finishAffinity()
                    }
                }
            } else {
                showCustomDialog(getString(R.string.error_title), getString(R.string.error_user_not_found), R.layout.custom_dialog_error)
            }
        }

        btnBuyVip.setOnClickListener {
            val user = dbHelper.getUser()
            if (user != null && !user.isVip) {
                startActivity(Intent(this, PaymentActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                })
            }
        }

        updateVipButtonState(btnBuyVip)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    applyTransition()
                    true
                }
                R.id.navigation_saved -> {
                    startActivity(Intent(this, SavedNewsActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    applyTransition()
                    true
                }
                R.id.navigation_recommend -> true
                R.id.navigation_profile -> true
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.navigation_profile

        adContainer = findViewById(R.id.adContainer)
        adView = findViewById(R.id.adView)
        ivCloseAd = findViewById(R.id.ivCloseAd)
        adRequest = AdRequest.Builder().build()

        updateAdVisibility()
    }

    override fun onResume() {
        super.onResume()
        updateAdVisibility()
        updateVipButtonState(findViewById(R.id.btnBuyVip))
        adView.resume()
        refreshUI()
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
    }

    override fun onDestroy() {
        adView.destroy()
        handler.removeCallbacks(showAdRunnable)
        super.onDestroy()
    }

    private fun startEditActivity(field: String) {
        startActivity(Intent(this, EditProfileActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("EDIT_FIELD", field)
        })
    }

    private fun applyTransition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun showCustomDialog(title: String, message: String, layoutResId: Int, onOk: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)?.setOnClickListener {
            onOk?.invoke()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateAdVisibility() {
        val user = dbHelper.getUser()
        if (user != null && user.isVip) {
            adContainer.visibility = View.GONE
            handler.removeCallbacks(showAdRunnable)
        } else {
            MobileAds.initialize(this) {}
            ivCloseAd.visibility = View.GONE
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    ivCloseAd.visibility = View.VISIBLE
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    ivCloseAd.visibility = View.GONE
                    adContainer.visibility = View.GONE
                }
            }
            adView.loadAd(adRequest)
            ivCloseAd.setOnClickListener {
                adContainer.visibility = View.GONE
                ivCloseAd.visibility = View.GONE
                handler.postDelayed(showAdRunnable, 10000)
            }
        }
    }

    private fun updateVipButtonState(btnBuyVip: Button?) {
        val user = dbHelper.getUser()
        btnBuyVip?.apply {
            if (user != null && user.isVip) {
                isEnabled = false
                text = getString(R.string.vip_activated)
            } else {
                isEnabled = true
                text = getString(R.string.buy_vip)
            }
        }
    }

    private fun refreshUI() {
        val user = dbHelper.getUser()
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvLanguage = findViewById<TextView>(R.id.tvLanguage)
        val tvCurrency = findViewById<TextView>(R.id.tvCurrency)
        if (user != null) {
            tvName.text = user.name ?: getString(R.string.not_set)
            tvEmail.text = user.email ?: getString(R.string.not_set)
            tvLanguage.text = when (sharedPref.getString("app_language", "ru")) {
                "ru" -> getString(R.string.language_russian)
                "en" -> getString(R.string.language_english)
                else -> getString(R.string.not_set)
            }
            tvCurrency.text = sharedPref.getString("app_currency", "RUB") ?: getString(R.string.not_set)
        }
        Log.d("ProfileActivity", "refreshUI: Updated with language ${sharedPref.getString("app_language", "ru")}")
    }
}