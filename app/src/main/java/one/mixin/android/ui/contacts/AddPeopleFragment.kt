package one.mixin.android.ui.contacts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_add_people.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.toast
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.landing.MobileFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.widget.Keyboard
import org.jetbrains.anko.textColor
import java.util.Locale
import javax.inject.Inject

class AddPeopleFragment : BaseFragment() {

    companion object {
        const val TAG = "AddPeopleFragment"
        val keys = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "0", "")
        const val POS_SEARCH = 0
        const val POS_PROGRESS = 1

        fun newInstance(): AddPeopleFragment {
            return AddPeopleFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val contactsViewModel: ContactViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ContactViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_add_people, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        val account = Session.getAccount()
        if (account != null) {
            tip_tv.text = getString(R.string.add_people_tip, account.identity_number)
        }
        search_et.addTextChangedListener(mWatcher)
        search_et.showSoftInputOnFocus = false
        search_et.post { search_et.isFocusable = false }
        keyboard.setKeyboardKeys(keys)
        keyboard.setOnClickKeyboardListener(mKeyboardListener)
        keyboard.animate().translationY(0f).start()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        search_animator.setOnClickListener {
            search_animator.displayedChild = POS_PROGRESS
            search_animator.isEnabled = false
            contactsViewModel.search(search_et.text.toString()).autoDisposable(scopeProvider).subscribe({ r ->
                search_animator.displayedChild = POS_SEARCH
                search_animator.isEnabled = true
                when {
                    r.isSuccess -> r.data?.let { data ->
                        if (data.userId == Session.getAccountId()) {
                            activity?.addFragment(this@AddPeopleFragment,
                                ProfileFragment.newInstance(), ProfileFragment.TAG)
                        } else {
                            contactsViewModel.insertUser(user = data)
                            UserBottomSheetDialogFragment.newInstance(data).showNow(requireFragmentManager(), UserBottomSheetDialogFragment.TAG)
                        }
                    }
                    r.errorCode == ErrorHandler.NOT_FOUND -> context?.toast(R.string.error_user_not_found)
                    else -> ErrorHandler.handleMixinError(r.errorCode)
                }
            }, { t: Throwable ->
                search_animator.displayedChild = POS_SEARCH
                search_animator.isEnabled = true
                ErrorHandler.handleError(t)
            })
        }
    }

    private fun valid(number: String): Boolean {
        if (number.startsWith("+")) {
            val phone = MobileFragment.Phone(number)
            val phoneUtil = PhoneNumberUtil.getInstance()
            return try {
                val phoneNumber = phoneUtil.parse(phone.phone, Locale.getDefault().country)
                phoneUtil.isValidNumber(phoneNumber)
            } catch (e: NumberParseException) {
                false
            }
        }
        if (number.length >= 2) {
            return true
        }
        return false
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (!isAdded) {
                return
            }
            val editable = search_et.text
            val start = search_et.selectionStart
            val end = search_et.selectionEnd
            if (position == 11) {
                if (editable.isEmpty()) return

                if (start == end) {
                    if (start == 0) {
                        search_et.text.delete(0, end)
                    } else {
                        search_et.text.delete(start - 1, end)
                    }
                    if (start > 0) {
                        search_et.setSelection(start - 1)
                    }
                } else {
                    search_et.text.delete(start, end)
                    search_et.setSelection(start)
                }
            } else {
                search_et.text = editable.insert(start, value)
                search_et.setSelection(start + 1)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (!isAdded) {
                return
            }
            val editable = search_et.text
            if (position == 11) {
                if (editable.isEmpty()) return

                search_et.text.clear()
            } else {
                val start = search_et.selectionStart
                search_et.text = editable.insert(start, value)
                search_et.setSelection(start + 1)
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (!isAdded) return

            if (valid(s.toString())) {
                search_animator.isEnabled = true
                search_animator.background = resources.getDrawable(R.drawable.bg_wallet_blue_btn, null)
                search_animator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = requireContext().dpToPx(72f)
                    bottomMargin = 0
                }
                search_tv.textColor = requireContext().getColor(R.color.white)
            } else {
                search_animator.isEnabled = false
                search_animator.background = resources.getDrawable(R.drawable.bg_gray_btn, null)
                search_animator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = requireContext().dpToPx(40f)
                    bottomMargin = requireContext().dpToPx(16f)
                }
                search_tv.textColor = requireContext().getColor(R.color.wallet_text_gray)

            }
        }
    }
}