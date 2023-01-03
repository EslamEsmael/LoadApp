package com.eslam.loadApp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eslam.loadApp.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDetailBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        intent.extras.let {
            binding.contentDetails.fileNameValueTextView.text = it?.getString("fileName")
            binding.contentDetails.fileStatusValueTextView.text = it?.getString("status")
        }

        binding.contentDetails.downloadButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

}
