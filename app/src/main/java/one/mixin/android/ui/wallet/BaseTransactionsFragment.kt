package one.mixin.android.ui.wallet

import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_transaction_filters.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.R
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CheckedFlowLayout
import javax.inject.Inject

abstract class BaseTransactionsFragment<C> : BaseFragment() {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    protected lateinit var dataObserver: Observer<C>

    protected fun showFiltersSheet() {
        filtersView.sort_flow.setCheckedById(currentOrder)
        filtersView.filter_flow.setCheckedById(currentType)
        filtersSheet.show()
    }

    protected val filtersSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomSheet = builder.create()
        builder.setCustomView(filtersView)
        bottomSheet
    }

    private val filtersView: View by lazy {
        val view = View.inflate(ContextThemeWrapper(context, R.style.Custom), R.layout.fragment_transaction_filters, null)
        view.filters_title.right_iv.setOnClickListener { filtersSheet.dismiss() }
        view.apply_tv.setOnClickListener { onApplyClick() }
        view.filter_flow.setOnCheckedListener(object : CheckedFlowLayout.OnCheckedListener {
            override fun onChecked(id: Int) {
                currentType = id
            }
        })
        view.sort_flow.setOnCheckedListener(object : CheckedFlowLayout.OnCheckedListener {
            override fun onChecked(id: Int) {
                currentOrder = id
            }
        })
        view
    }

    private var currentLiveData: LiveData<C>? = null

    protected fun bindLiveData(liveData: LiveData<C>) {
        currentLiveData?.removeObserver(dataObserver)
        currentLiveData = liveData
        currentLiveData?.observe(this, dataObserver)
    }

    protected var currentType = R.id.filters_radio_all
    protected var currentOrder = R.id.sort_time

    abstract fun onApplyClick()
}