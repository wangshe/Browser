package acr.browser.lightning.settings.fragment

import acr.browser.lightning.R
import acr.browser.lightning.settings.activity.SettingsActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceHeaderFragmentCompat
import timber.log.Timber


/**
 * Give us single pane on narrow screens and two pane settings on wider screens.
 */
class ResponsiveSettingsFragment : PreferenceHeaderFragmentCompat() {

    private val iRootSettingsFragment = RootSettingsFragment()
    // Keep track of the settings fragment we are currently showing
    // Notably used to remain in the proper settings page after screen rotation
    var iPreference: Preference? = null

    // Breadcrumbs management
    private var iBreadcrumbs: ArrayList<String> = ArrayList<String>()
    //
    private var iTitleStack: ArrayList<String> = ArrayList<String>()



    override fun onCreatePreferenceHeader(): PreferenceFragmentCompat {
        iTitleStack.add(requireContext().getString(R.string.settings))
        iBreadcrumbs.add(requireContext().getString(R.string.settings))
        // Provide left pane headers fragment
        return iRootSettingsFragment
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {

        Timber.d("onPreferenceStartFragment")

        iPreference = pref

        // We currently do not support more than two breadcrumbs
        // Make sure you pop the last one then otherwise they are accumulating
        // TODO: Do support more breadcrumbs maybe using either AbstractSettingsFragment or addOnBackStackChangedListener
        popBreadcrumbs()
        // Ugly specific case for DomainSettingsFragment, then again that whole thing was a mess before
        (if (caller is DomainSettingsFragment || caller is DomainsSettingsFragment) pref.summary else pref.title)?.let {
            iTitleStack.add(it.toString())
            iBreadcrumbs.add(it.toString())
        }

        // Trigger title update on next layout
        (activity as? SettingsActivity)?.updateTitleOnLayout()

        return super.onPreferenceStartFragment(caller,pref)
    }

    /**
     * Called by the activity whenever we go back
     */
    fun popBreadcrumbs(aGoBack: Boolean = false) {
        if (iBreadcrumbs.count()>1) {
            iBreadcrumbs.removeLast()
        }

        if (aGoBack) {
            iTitleStack.removeLast()
        }

        // Needed to restore previous page title when coming back from Portrait and Landscape settings
        if (aGoBack && iRootSettingsFragment.isVisible && !slidingPaneLayout.isSlideable) {
            iTitleStack.last().let {
                iBreadcrumbs.add(it)
            }
        }
    }

    /**
     * Provide proper title according to current mode:
     * - Split screen mode shows breadcrumbs
     * - Full screen mode shows only current page title
     */
    fun title(): String {

        Timber.d("titles: $iTitleStack")
        Timber.d("breadcrumbs: $iBreadcrumbs")

        return if (iRootSettingsFragment.isVisible && !slidingPaneLayout.isSlideable && iBreadcrumbs.count()>1) {

            var title = iBreadcrumbs.first()

            for (i in 1 until iBreadcrumbs.count()) {
                title += " > " + iBreadcrumbs.elementAt(i)
            }
            title
        } else {
            iTitleStack.last()
        }
    }
}
