package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.findNavController
import one.mixin.android.R
import one.mixin.android.extension.notNullElse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletPasswordFragment.Companion.ARGS_CHANGE
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import javax.inject.Inject

class WalletActivity : BlazeBaseActivity() {

    @Inject
    lateinit var jobManager: MixinJobManager

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        val account = Session.getAccount()
        if (account == null) {
            finish()
            return
        }
        navController = findNavController(R.id.wallet_nav_fragment)
        val navGraph = navController.navInflater.inflate(R.navigation.nav_wallet)
        notNullElse(asset, {
            navGraph.startDestination = R.id.transactions_fragment
            navGraph.addArgument(ARGS_ASSET, NavArgument.Builder().setDefaultValue(it).build())
        }, {
            if (account.hasPin) {
                navGraph.startDestination = R.id.wallet_fragment
            } else {
                navGraph.startDestination = R.id.wallet_password_fragment
                navGraph.addArgument(ARGS_CHANGE, NavArgument.Builder().setDefaultValue(false).build())
            }
        })
        navController.graph = navGraph
        jobManager.addJobInBackground(RefreshAssetsJob())
    }

    private val asset: AssetItem? by lazy {
        intent.extras?.getParcelable<AssetItem>(ASSET)
    }

    companion object {
        const val ASSET = "ASSET"
        fun show(activity: Activity, assetItem: AssetItem? = null) {
            val myIntent = Intent(activity, WalletActivity::class.java)
            assetItem?.let {
                val bundle = Bundle()
                bundle.putParcelable(ASSET, assetItem)
                myIntent.putExtras(bundle)
            }
            activity.startActivity(myIntent)
        }
    }
}
