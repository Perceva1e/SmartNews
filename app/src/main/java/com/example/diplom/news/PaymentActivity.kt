package com.example.diplom.news

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.diplom.R
import com.example.diplom.databinding.ActivityPaymentBinding
import com.example.diplom.utils.showToast

class PaymentActivity : BaseActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private var userId: Int = -1

    private val languageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("PaymentActivity", "Received language change broadcast")
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        binding.btnSubscribe.setOnClickListener {
            if (validateInput()) {
                saveSubscriptionStatus(true)
                showToast(getString(R.string.premium_subscription_active))
                setResult(RESULT_OK)
                finish()
            }
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(languageChangeReceiver, IntentFilter(ACTION_LANGUAGE_CHANGED))
    }

    override fun onResume() {
        super.onResume()
        loadLocale()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(languageChangeReceiver)
    }

    private fun validateInput(): Boolean {
        val name = binding.etCardholderName.text.toString()
        val cardNumber = binding.etCardNumber.text.toString()
        val expiryDate = binding.etExpiryDate.text.toString()
        val cvv = binding.etCvv.text.toString()

        var isValid = true

        if (name.isEmpty()) {
            binding.tilCardholderName.error = getString(R.string.error_name_required)
            isValid = false
        } else {
            binding.tilCardholderName.error = null
        }

        if (cardNumber.isEmpty() || cardNumber.length < 16) {
            binding.tilCardNumber.error = getString(R.string.error_card_number_required)
            isValid = false
        } else {
            binding.tilCardNumber.error = null
        }

        if (expiryDate.isEmpty() || !expiryDate.matches(Regex("\\d{2}/\\d{2}"))) {
            binding.tilExpiryDate.error = getString(R.string.error_expiry_date_required)
            isValid = false
        } else {
            binding.tilExpiryDate.error = null
        }

        if (cvv.isEmpty() || cvv.length < 3) {
            binding.tilCvv.error = getString(R.string.error_cvv_required)
            isValid = false
        } else {
            binding.tilCvv.error = null
        }

        return isValid
    }

    private fun saveSubscriptionStatus(isSubscribed: Boolean) {
        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("isSubscribed_$userId", isSubscribed).apply()
    }
}