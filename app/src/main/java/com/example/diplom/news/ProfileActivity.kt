package com.example.diplom.news

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.diplom.R
import com.example.diplom.api.NewsApi
import com.example.diplom.auth.LoginActivity
import com.example.diplom.database.AppDatabase
import com.example.diplom.databinding.ActivityProfileBinding
import com.example.diplom.news.adapter.NewsViewModel
import com.example.diplom.repository.NewsRepository
import com.example.diplom.utils.showToast
import com.example.diplom.viewmodel.NewsViewModelFactory
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: NewsViewModel by viewModels {
        NewsViewModelFactory(
            NewsRepository(
                AppDatabase.getDatabase(this).userDao(),
                AppDatabase.getDatabase(this).newsDao(),
                NewsApi.service
            )
        )
    }
    private var userId: Int = -1
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adView: AdView
    private val adRefreshHandler = Handler(Looper.getMainLooper())
    private var lastRefreshTime = 0L
    private var adClosedTime = 0L
    private var isAdManuallyClosed = false
    private val adReshowDelay = 5 * 60 * 1000L

    private val adRefreshRunnable = object : Runnable {
        override fun run() {
            Log.d("AdRefresh", "Refreshing ad in ProfileActivity")
            adView.loadAd(AdRequest.Builder().build())
            lastRefreshTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed(this, adReshowDelay)
        }
    }

    private val paymentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                updateSubscriptionStatus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        binding.btnSelectCategories.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }

        getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .edit()
            .putInt("last_user_id", userId)
            .apply()

        adView = binding.adView
        setupAdListener()
        val adRequest = AdRequest.Builder().build()
        if (!isSubscribed()) {
            adView.loadAd(adRequest)
            lastRefreshTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
        }

        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        setupViews()
        loadUserData()
        setupNavigation()
        setupLanguageSpinner()

        binding.btnCloseAd.setOnClickListener {
            adView.visibility = View.GONE
            binding.btnCloseAd.visibility = View.GONE
            adRefreshHandler.removeCallbacks(adRefreshRunnable)
            isAdManuallyClosed = true
            adClosedTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed({
                if (isAdManuallyClosed) {
                    reloadAd()
                }
            }, adReshowDelay)
        }

        updateSubscriptionStatus()
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_russian)
        )
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.languages_array,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentLanguage = prefs.getString("language_$userId", "en") ?: "en"
        val position = if (currentLanguage == "ru") 1 else 0
        binding.spinnerLanguage.setSelection(position)

        binding.spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLanguage = if (position == 0) "en" else "ru"
                    val currentLanguage = getCurrentLanguage()
                    if (selectedLanguage != currentLanguage) {
                        setLocale(selectedLanguage)
                        recreate()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun getCurrentLanguage(): String {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return prefs.getString("language_$userId", "en") ?: "en"
    }

    override fun setLocale(language: String, notify: Boolean) {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putString("language_$userId", language)
            apply()
        }
        super.setLocale(language, notify)
        Log.d("ProfileActivity", "Locale set to: $language for userId: $userId")
    }

    private fun setupAdListener() {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                if (!isSubscribed()) {
                    binding.adView.visibility = View.VISIBLE
                    binding.btnCloseAd.visibility = View.VISIBLE
                }
                isAdManuallyClosed = false
                Log.d("AdListener", "Ad loaded successfully in ProfileActivity")
            }

            override fun onAdClosed() {
                super.onAdClosed()
                binding.adView.visibility = View.GONE
                binding.btnCloseAd.visibility = View.GONE
            }
        }
    }

    private fun reloadAd() {
        if (!isSubscribed()) {
            binding.adView.visibility = View.VISIBLE
            adView.loadAd(AdRequest.Builder().build())
            lastRefreshTime = System.currentTimeMillis()
            adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
        }
        isAdManuallyClosed = false
    }

    private fun setupViews() {
        binding.btnSaveChanges.setOnClickListener {
            val newName = binding.etName.text.toString()
            val newEmail = binding.etEmail.text.toString()

            if (validateInput(newName, newEmail)) {
                viewModel.updateUser(userId, newName, newEmail)
                showToast("Changes saved")
            }
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnSubscribeProfile.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                val intent = Intent(this@ProfileActivity, PaymentActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                }
                paymentLauncher.launch(intent)
            }
        }
    }

    private fun updateSubscriptionStatus() {
        if (isSubscribed()) {
            binding.btnSubscribeProfile.visibility = View.GONE
            binding.tvSubscriptionStatus.visibility = View.VISIBLE
            binding.tvSubscriptionStatus.text = getString(R.string.premium_subscription_active)
            binding.adView.visibility = View.GONE
            binding.btnCloseAd.visibility = View.GONE
            adRefreshHandler.removeCallbacks(adRefreshRunnable)
        } else {
            binding.btnSubscribeProfile.visibility = View.VISIBLE
            binding.tvSubscriptionStatus.visibility = View.GONE
            reloadAd()
        }
    }

    private fun isSubscribed(): Boolean {
        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return sharedPrefs.getBoolean("isSubscribed_$userId", false)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        adView.resume()
        val currentTime = System.currentTimeMillis()

        if (!isSubscribed()) {
            if (isAdManuallyClosed && currentTime - adClosedTime >= adReshowDelay) {
                reloadAd()
            } else if (!isAdManuallyClosed && currentTime - lastRefreshTime > adReshowDelay && binding.adView.visibility == View.VISIBLE) {
                adView.loadAd(AdRequest.Builder().build())
                lastRefreshTime = currentTime
                adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
            } else if (binding.adView.visibility == View.VISIBLE) {
                adRefreshHandler.postDelayed(adRefreshRunnable, adReshowDelay)
            }
        }
        loadUserData()
        updateSubscriptionStatus()
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
        adRefreshHandler.removeCallbacks(adRefreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        adView.destroy()
        adRefreshHandler.removeCallbacks(adRefreshRunnable)
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.etPassword)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_account))
            .setMessage(getString(R.string.confirm_deletion))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    deleteUser(password)
                } else {
                    passwordInput.error = getString(R.string.password_required)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteUser(password: String) {
        val user = auth.currentUser
        val email = user?.email ?: run {
            showToast("User not authenticated")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    user.delete()
                        .addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                viewModel.deleteUser(userId)
                                logoutUser()
                            } else {
                                showToast("Deletion failed: ${deleteTask.exception?.message}")
                            }
                        }
                } else {
                    showToast("Authentication failed: ${authTask.exception?.message}")
                }
            }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    private fun loadUserData() {
        viewModel.user.observe(this) { user ->
            user?.let {
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)
            }
        }
        viewModel.loadUser(userId)
    }

    private fun validateInput(name: String, email: String): Boolean {
        var isValid = true
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_name_required)
            isValid = false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            isValid = false
        }
        return isValid
    }

    private fun setupNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }

                R.id.navigation_saved -> {
                    startActivity(
                        Intent(this, SavedNewsActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }

                R.id.navigation_recommend -> {
                    startActivity(
                        Intent(this, RecommendActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    applyTransition()
                    true
                }

                R.id.navigation_profile -> true
                else -> false
            }
        }
    }

    private fun applyTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}