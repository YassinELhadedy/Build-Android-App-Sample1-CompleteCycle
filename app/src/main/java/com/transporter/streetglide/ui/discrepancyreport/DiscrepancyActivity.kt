package com.transporter.streetglide.ui.discrepancyreport

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.transporter.streetglide.R

/**
 * plz don't review this package is not actual implementation because it's depend on the next story (consolidation shipments)
 *
 */

class DiscrepancyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discrepancy)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_discrepancy_toolbar_custom)
        setSupportActionBar(toolbar)

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        val viewPager = findViewById<ViewPager>(R.id.viewpager_discrepancy_discrepancy_content)
        setupViewPager(viewPager)

        // Give the TabLayout the ViewPager
        val tabLayout = findViewById<TabLayout>(R.id.tablayout_discrepancy_discrepancy_category)
        tabLayout.setupWithViewPager(viewPager)
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(DiscrepancyFragment(), "غير موجوده لديك")
        adapter.addFragment(DiscrepancyFragment(), "غير موجوده في الشيت")
        viewPager.adapter = adapter
    }

    public override fun onResume() {
        super.onResume()
    }

    internal inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
        private val mFragmentList = ArrayList<Fragment>()
        private val mFragmentTitleList = ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }
    }
}