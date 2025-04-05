package com.example.diplom.news

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.databinding.ActivityPaymentBinding
import com.example.diplom.utils.showToast

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) finish()

        binding.btnSubscribe.setOnClickListener {
            if (validateInput()) {
                saveSubscriptionStatus(true)
                showToast("Premium subscription activated!")
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun validateInput(): Boolean {
        val name = binding.etCardholderName.text.toString()
        val cardNumber = binding.etCardNumber.text.toString()
        val expiryDate = binding.etExpiryDate.text.toString()
        val cvv = binding.etCvv.text.toString()

        var isValid = true

        if (name.isEmpty()) {
            binding.tilCardholderName.error = "Name required"
            isValid = false
        } else {
            binding.tilCardholderName.error = null
        }

        if (cardNumber.isEmpty() || cardNumber.length < 16) {
            binding.tilCardNumber.error = "Valid card number required (16 digits)"
            isValid = false
        } else {
            binding.tilCardNumber.error = null
        }

        if (expiryDate.isEmpty() || !expiryDate.matches(Regex("\\d{2}/\\d{2}"))) {
            binding.tilExpiryDate.error = "Valid expiry date required (MM/YY)"
            isValid = false
        } else {
            binding.tilExpiryDate.error = null
        }

        if (cvv.isEmpty() || cvv.length < 3) {
            binding.tilCvv.error = "Valid CVV required (3 digits)"
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