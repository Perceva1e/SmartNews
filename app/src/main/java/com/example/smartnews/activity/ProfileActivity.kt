package com.example.smartnews.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        dbHelper = DatabaseHelper(this)

        val currentLang = sharedPref.getString("app_language", "ru")
        val currentCurrency = sharedPref.getString("app_currency", "RUB")

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val spLanguage = findViewById<Spinner>(R.id.spLanguage)
        val spCurrency = findViewById<Spinner>(R.id.spCurrency)
        val ivLanguageIcon = findViewById<ImageView>(R.id.ivLanguageIcon)
        val ivCurrencyIcon = findViewById<ImageView>(R.id.ivCurrencyIcon)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)
        val btnBuyVip = findViewById<Button>(R.id.btnBuyVip)
        val btnNewsFilter = findViewById<Button>(R.id.btnNewsFilter)

        val user = dbHelper.getUser()
        if (user != null) {
            etName.setText(user.name)
            etEmail.setText(user.email)
        }

        val languages = resources.getStringArray(R.array.languages)
        val currencies = resources.getStringArray(R.array.currencies)

        val languageAdapter = ArrayAdapter(this, R.layout.spinner_item, languages)
        languageAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spLanguage.adapter = languageAdapter

        val currencyAdapter = ArrayAdapter(this, R.layout.spinner_item, currencies)
        currencyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spCurrency.adapter = currencyAdapter

        val languageIndex = languages.indexOfFirst { it.contains(currentLang ?: "ru", ignoreCase = true) }
        spLanguage.setSelection(if (languageIndex != -1) languageIndex else 0)

        val currencyIndex = currencies.indexOfFirst { it.startsWith(currentCurrency ?: "RUB") }
        spCurrency.setSelection(if (currencyIndex != -1) currencyIndex else 0)

        updateLanguageIcon(spLanguage.selectedItemPosition, ivLanguageIcon)
        updateCurrencyIcon(spCurrency.selectedItemPosition, ivCurrencyIcon)

        spLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateLanguageIcon(position, ivLanguageIcon)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCurrencyIcon(position, ivCurrencyIcon)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val language = if (spLanguage.selectedItemPosition == 0) "ru" else "en"
            val currency = currencies[spCurrency.selectedItemPosition].substringBefore(" ")

            if (name.isNotEmpty() && email.isNotEmpty()) {
                lifecycleScope.launch {
                    val existingUser = dbHelper.getUser()
                    if (existingUser != null) {
                        dbHelper.updateUser(existingUser.id, name, email, existingUser.password, existingUser.newsCategories, existingUser.isVip)
                    } else {
                        dbHelper.addUser(name, email, "")
                    }
                    with(sharedPref.edit()) {
                        putString("app_language", language)
                        putString("app_currency", currency)
                        apply()
                    }
                    setLocale(language)
                    showCustomDialog(getString(R.string.success_title), getString(R.string.saved), R.layout.custom_dialog_success)
                }
            } else {
                showCustomDialog(getString(R.string.error_title), getString(R.string.error_empty_fields), R.layout.custom_dialog_error)
            }
        }

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
                        finish()
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

        btnNewsFilter.setOnClickListener {
            startActivity(Intent(this, NewsFilterActivity::class.java).apply {
                putExtra("USER_ID", userId)
            })
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

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
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

    private fun updateLanguageIcon(position: Int, imageView: ImageView) {
        val drawableId = when (position) {
            0 -> R.drawable.ic_flag_ru
            1 -> R.drawable.ic_flag_en
            else -> R.drawable.ic_flag_ru
        }
        imageView.setImageResource(drawableId)
    }

    private fun updateCurrencyIcon(position: Int, imageView: ImageView) {
        val currency = resources.getStringArray(R.array.currencies)[position].substringBefore(" ")
        val drawableId = when (currency) {
            "RUB" -> R.drawable.ic_rub
            "USD" -> R.drawable.ic_usd
            else -> R.drawable.ic_rub
        }
        imageView.setImageResource(drawableId)
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
}