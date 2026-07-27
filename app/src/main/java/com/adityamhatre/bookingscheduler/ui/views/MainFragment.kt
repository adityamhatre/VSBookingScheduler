package com.adityamhatre.bookingscheduler.ui.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.adityamhatre.bookingscheduler.Application
import com.adityamhatre.bookingscheduler.BuildConfig
import com.adityamhatre.bookingscheduler.R
import com.adityamhatre.bookingscheduler.customViews.MonthView
import com.adityamhatre.bookingscheduler.ui.viewmodels.MainFragmentViewModel
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainFragmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireActivity().packageManager.canRequestPackageInstalls()) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Please click \"Allow from this source\" on the next screen to allow auto updates")
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        )
                    )
                    Toast.makeText(
                        requireContext(),
                        "Please click \"Allow from this source\" to enable auto updates",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            val alert = builder.create()
            alert.show()
            return
        }
        Application.getInstance().checkForUpdates()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
        loadMonthlyBookingsCount(view)
    }

    // Adds rolling months dynamically and controls visibility for a 2-year window
    private fun setupRollingMonths(monthsList: LinearLayout) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.systemDefault()))
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1-indexed

        // Show a rolling window of 2 years (24 months) from the current month
        val maxCalendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.systemDefault()))
        maxCalendar.add(Calendar.MONTH, 24)
        val maxYear = maxCalendar.get(Calendar.YEAR)
        val maxMonth = maxCalendar.get(Calendar.MONTH) + 1

        // Show past months starting from 4 months ago
        val minCalendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.systemDefault()))
        minCalendar.add(Calendar.MONTH, -4)
        val minYear = minCalendar.get(Calendar.YEAR)
        val minMonth = minCalendar.get(Calendar.MONTH) + 1

        // 1. Appends new MonthViews programmatically if they are not yet in the layout
        var (lastMonth, lastYear) = (monthsList[monthsList.childCount - 1] as MonthView).getMonthYear()
        while (lastYear < maxYear || (lastYear == maxYear && lastMonth < maxMonth)) {
            lastMonth++
            if (lastMonth > 12) {
                lastMonth = 1
                lastYear++
            }
            val newMonthView = MonthView(requireContext(), lastMonth, lastYear)
            monthsList.addView(newMonthView)
        }

        // 2. Control visibility: show months from 4 months ago up to 24 months in the future
        monthsList.children.forEach { child ->
            val monthView = child as MonthView
            val (m, y) = monthView.getMonthYear()

            val isAfterMin = y > minYear || (y == minYear && m >= minMonth)
            val isBeforeMax = y < maxYear || (y == maxYear && m <= maxMonth)

            if (isAfterMin && isBeforeMax) {
                monthView.visibility = View.VISIBLE
            } else {
                monthView.visibility = View.GONE
            }
        }
    }

    private fun printMonths(newMonth: Int, newYear: Int) {
        val xml = """
             <com.adityamhatre.bookingscheduler.customViews.MonthView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:layout_marginTop="16dp"
                month_view:month="${MonthView.monthName(newMonth)}"
                month_view:year="$newYear" />
        """.trimIndent()
        println(xml)
    }


    private fun loadMonthlyBookingsCount(view: View) {
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                Application.getInstance().getRenderService()
                    .getBookingSummary { bookingSummaryJsonObject ->
                        timer.cancel()
                        bookingSummaryJsonObject.keys().forEach {
                            val monthYear = it
                            val month = it.substring(0, 2).toInt()
                            val year = it.substring(2).toInt()
                            val count = bookingSummaryJsonObject[it].toString().toInt()

                            val index = 12 * (year - 2021) + month - 1
                            val monthView =
                                view.findViewById<LinearLayout>(R.id.yearList)[index] as MonthView

                            monthView.setBookingsCount(count)
                        }
                    }
            }
        }

        timer.scheduleAtFixedRate(timerTask, 0, 3000)
    }

    private fun setupView(view: View) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.systemDefault()));
        val year = calendar.get(Calendar.YEAR);
        val month = calendar.get(Calendar.MONTH)
        val scrollToIndex = 12 * (year - 2021) + month

        viewModel.wasViewLoaded().observe(viewLifecycleOwner, {
            if (!it) {
                viewModel.viewDidLoad()
                view.findViewById<ScrollView>(R.id.scrollLayout).postDelayed({
                    view.findViewById<ScrollView>(R.id.scrollLayout)
                        .smoothScrollTo(
                            0,
                            view.findViewById<LinearLayout>(R.id.yearList)[scrollToIndex].top
                        )
                }, 500)
            }
        })

        setupRollingMonths(view.findViewById<LinearLayout>(R.id.yearList))

        view.findViewById<LinearLayout>(R.id.yearList).children.forEachIndexed { i, it ->
            val monthView = it as MonthView
            monthView.dateClickedListener =
                MonthView.DateClickedListener { date, month, year ->
                    viewBookings(
                        date,
                        month,
                        year
                    )
                }

            monthView.monthClickedListener =
                MonthView.MonthClickedListener { month, year ->
                    viewBookings(
                        month = month,
                        year = year
                    )
                }
            monthView.setOnClickListener { }
            monthView.addBookingInfo()
        }
    }

    private fun viewBookings(date: Int = -1, month: Int, year: Int) {
        if (activity == null) return
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.container, ListOfBookingsFragment.newInstance(date, month, year))
            .addToBackStack(null)
            .commit()
    }

}