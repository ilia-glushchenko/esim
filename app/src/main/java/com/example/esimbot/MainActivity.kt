package com.example.esimbot

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccInfo
import android.telephony.euicc.EuiccManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.esimbot.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var euiccManager: EuiccManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //----------------------------------------------
        // Activity initialization
        //----------------------------------------------
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        //----------------------------------------------
        // eSim initialization
        //----------------------------------------------

        // Get EuiccManager
        euiccManager = getSystemService(Context.EUICC_SERVICE) as EuiccManager
        val isEnabled: Boolean = euiccManager.isEnabled()
        if (!isEnabled) return
        val info = euiccManager.euiccInfo
        Log.d("eSim", info?.osVersion.toString())

        val eSimActivationCode = "LPA:1\$smdp.io$4Y-1PAS75-GNHZLK"
        val downloadSubscriptionAction = "download_subscription"
        val downloadSubscriptionIntent = Intent(downloadSubscriptionAction)
        val downloadSubscriptionPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            downloadSubscriptionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Register broadcast receiver
        var broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR) {
                    euiccManager.startResolutionActivity(
                        this@MainActivity, 0, intent, downloadSubscriptionPendingIntent
                    )
                } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
                    Log.d(
                        "eSim", intent?.getIntExtra(
                            EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 999
                        ).toString()
                    )
                } else {
                    Log.d("eSim", resultCode.toString())
                }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter(downloadSubscriptionAction))

        // Download subscription asynchronously
        euiccManager.downloadSubscription(
            DownloadableSubscription.forActivationCode(eSimActivationCode),
            true,
            downloadSubscriptionPendingIntent
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}